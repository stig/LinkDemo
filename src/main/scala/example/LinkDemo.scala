package example

import akka.actor.{ Props, ActorSystem }
import akka.actor.ActorDSL._
import spray.routing.HttpServiceActor
import spray.can.Http
import akka.io.IO
import spray.json._
import spray.routing._
import spray.http._
import DefaultJsonProtocol._
import akka.util.Timeout
import scala.concurrent.duration._
import spray.httpx.SprayJsonSupport._

object LinkDemo extends App {

  implicit val system = ActorSystem("demo")

  val service = system.actorOf(Props[Service])

  IO(Http) ! Http.Bind(service, interface = "localhost", port = 8080)
}

class Service extends HttpServiceActor {
  import context.dispatcher

  implicit val timeout = Timeout(1.second)

  def receive = runRoute(
    get {
      path("paging") {
        parameters('offset ? 0, 'limit ? 3) { (offset: Int, limit: Int) =>
          complete {
            Range(offset, offset + limit).toList
          }
        }
      }
    })
}
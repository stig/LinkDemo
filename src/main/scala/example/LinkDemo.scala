package example

import akka.actor.{ Props, ActorSystem }
import spray.can.Http
import akka.io.IO
import spray.http._

object LinkDemo extends App {

  implicit val system = ActorSystem("demo")

  val model = system.actorOf(Props[Model])
  val service = system.actorOf(Props(new Service(model)))

  IO(Http) ! Http.Bind(service, interface = "localhost", port = 8080)
}


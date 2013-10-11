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
import spray.http.StatusCodes._
import spray.http.HttpHeaders._
import spray.http.CacheDirectives.`max-age`
import spray.http.Uri.Query

/*
 * A Demo of different strategies for Pagination, using the HTTP Link header
 * as described in http://www.rfc-editor.org/rfc/rfc5988.txt
 * 
 * By always relying on the next/prev/last/first rel links different API endpoints
 * can use different types of arguments for pagination, yet can still be treated
 * the same by clients. 
 */

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
          ctx =>
            val cache = `Cache-Control`(`max-age`(offset * 60))

            val q = Query("offset" -> (offset + limit).toString, "limit" -> limit.toString)
            val next = Uri(path = ctx.request.uri.path, query = q)
            val link = RawHeader("Link", s"""<$next>; rel="next"""")

            ctx.complete(OK, link :: cache :: Nil, Range(offset, offset + limit).toList)
        }
      } ~
        path("after") {
          parameters('after.as[Int] ? 0, 'limit ? 3) { (after: Int, limit: Int) =>
            ctx =>
              val cache = `Cache-Control`(`max-age`(60))

              val data = Range(after + 1, after + 1 + limit).toList

              val q = Query("after" -> data.last.toString, "limit" -> limit.toString)
              val next = ctx.request.uri.copy(query = q)
              val link = RawHeader("Link", s"""<$next>; rel="next"""")

              ctx.complete(OK, link :: cache :: Nil, data)
          }
        }

    })
}
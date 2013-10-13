package example

import spray.routing.HttpServiceActor
import spray.routing._
import akka.actor.{ Actor, ActorRef }
import akka.util.Timeout
import scala.concurrent.duration._
import spray.http.HttpHeaders._
import spray.http.StatusCodes._
import java.util.UUID
import org.joda.time.DateTime
import scala.util.Random
import spray.http.CacheDirectives.`max-age`
import spray.http.Uri
import spray.httpx.SprayJsonSupport._
import akka.pattern.ask

class Service(model: ActorRef) extends HttpServiceActor with ItemJsonProtocol {
  import context.dispatcher

  implicit val timeout = Timeout(10.seconds)

  def cacheHeader(slice: Iterable[Item]) =
    `Cache-Control`(`max-age`(slice.map(_.cache).min * 60))

  def linkHeader(next: Uri, prev: Option[Uri]) = {
    prev match {
      case Some(prev) => RawHeader("Link", s"""<$next>; rel="next", <$prev>; rel="prev"""")
      case None => RawHeader("Link", s"""<$next>; rel="next"""")
    }
  }

  def receive = runRoute(
    get {
      path("by-offset") {
        parameters('offset ? 0, 'limit ? 3) { (offset: Int, limit: Int) =>
          ctx =>
            (model ? ItemsWithOffset(offset, limit)) map {
              case Items(slice) =>
                val q = Uri.Query("offset" -> (offset + limit).toString, "limit" -> limit.toString)
                val prevOffset = (offset - limit)
                val prev = prevOffset match {
                  case x if x < 0 => None
                  case x => Option(ctx.request.uri.copy(query = Uri.Query("offset" -> (prevOffset).toString, "limit" -> limit.toString)))
                }

                val next = ctx.request.uri.copy(query = q)
                ctx.complete(OK, linkHeader(next, prev) :: cacheHeader(slice) :: Nil, slice)
            }
        }
      } ~
        path("by-date") {
          parameters('since ? 0L, 'limit ? 3) { (since, limit) =>
            ctx =>
              (model ? ItemsSinceEpoch(since, limit)) map {
                case Items(slice) =>
                  val q = Uri.Query("since" -> slice.toList.last.created.getMillis.toString, "limit" -> limit.toString)
                  val next = ctx.request.uri.copy(query = q)
                  ctx.complete(OK, linkHeader(next, None) :: cacheHeader(slice) :: Nil, slice)
              }
          }
        } ~
        path("by-id") {
          parameters('since ? "", 'limit ? 3) { (since, limit) =>
            ctx =>
              val msg = (try {
                ItemsSinceId(UUID.fromString(since), limit)
              } catch {
                case _: Exception => ItemsWithOffset(0, limit)
              })

              (model ? msg) map {
                case Items(slice) =>
                  val q = Uri.Query("since" -> slice.toList.last.id.toString, "limit" -> limit.toString)
                  val next = ctx.request.uri.copy(query = q)
                  ctx.complete(OK, linkHeader(next, None) :: cacheHeader(slice) :: Nil, slice)
              }
          }
        }

    })
}

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
import spray.routing.directives.CachingDirectives._

class Service(model: ActorRef) extends HttpServiceActor with ItemJsonProtocol {
  import context.dispatcher

  implicit val timeout = Timeout(10.seconds)

  def cacheHeader(slice: Iterable[Item]): `Cache-Control` =
    cacheHeader(slice.map(_.cache).min)

  def cacheHeader(cache: Int) =
    `Cache-Control`(`max-age`(cache * 60))

  def linkHeader(next: Uri) =
    RawHeader("Link", s"""<$next>; rel="next"""")

  // A 5-second cache for 1000 most recent GET requests
  val simpleCache = routeCache(maxCapacity = 1000, timeToLive = 5.seconds)

  def receive = runRoute(
    cache(simpleCache) {
      get {
        path(JavaUUID) { id =>
          onSuccess(model ? id) {
            case i: Item => complete(OK, cacheHeader(i.cache) :: Nil, i)
            case None => complete(NotFound)
          }
        } ~
          path("by-offset") {
            parameters('offset ? 0, 'limit ? 3) { (offset: Int, limit: Int) =>
              ctx =>
                (model ? ItemsWithOffset(offset, limit)) map {
                  case Items(slice) =>
                    val q = Uri.Query("offset" -> (offset + limit).toString, "limit" -> limit.toString)
                    val next = ctx.request.uri.copy(query = q)
                    ctx.complete(OK, linkHeader(next) :: cacheHeader(slice) :: Nil, slice)
                }
            }
          }
      } ~
        path("by-date") {
          parameters('since ? 0L, 'limit ? 3) { (since, limit) =>
            ctx =>
              val msg = if (since == 0) ItemsWithOffset(0, limit) else ItemsSinceDateTime(new DateTime(since), limit)
              (model ? msg) map {
                case Items(slice) =>
                  val q = Uri.Query("since" -> slice.toList.last.created.getMillis.toString, "limit" -> limit.toString)
                  val next = ctx.request.uri.copy(query = q)
                  ctx.complete(OK, linkHeader(next) :: cacheHeader(slice) :: Nil, slice)
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
                  ctx.complete(OK, linkHeader(next) :: cacheHeader(slice) :: Nil, slice)
              }
          }
        }
    })
}
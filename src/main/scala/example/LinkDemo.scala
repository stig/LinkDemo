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
import org.joda.time.DateTime
import scala.util.Random
import java.util.UUID

case class Item(id: UUID, created: DateTime, cache: Int)

object ItemJsonFormat {
  implicit object UuidFormat extends JsonFormat[UUID] {
    def write(x: UUID) = JsString(x toString ())
    def read(value: JsValue) = value match {
      case JsString(x) => UUID.fromString(x)
      case x => deserializationError("Expected UUID as JsString, but got " + x)
    }
  }

  implicit object DateTimeJsonFormat extends JsonFormat[DateTime] {
    def write(x: DateTime) = JsNumber(x.getMillis)
    def read(value: JsValue) = value match {
      case JsNumber(x) => new DateTime(x.toLong)
      case x => deserializationError("Expected DateTime as JsNumber, but got " + x)
    }
  }

  implicit val itemFmt = jsonFormat3(Item)
}

object LinkDemo extends App {

  implicit val system = ActorSystem("demo")

  val service = system.actorOf(Props[Service])

  IO(Http) ! Http.Bind(service, interface = "localhost", port = 8080)
}

class Service extends HttpServiceActor {
  import context.dispatcher
  import ItemJsonFormat._

  implicit val timeout = Timeout(1.second)

  // Dummy data for illustration purposes, in ascending order by date
  val data = (for {
    x <- 0 to 100
  } yield Item(UUID.randomUUID(), new DateTime().minusDays(x), Random.nextInt(20))).reverse

  def cacheHeader(slice: Iterable[Item]) =
    `Cache-Control`(`max-age`(slice.map(_.cache).min * 60))

  def linkHeader(next: Uri) =
    RawHeader("Link", s"""<$next>; rel="next"""")

  def receive = runRoute(
    get {
      path("by-offset") {
        parameters('offset ? 0, 'limit ? 3) { (offset: Int, limit: Int) =>
          ctx =>
            val slice = data.drop(offset).take(limit)
            val q = Query("offset" -> (offset + limit).toString, "limit" -> limit.toString)
            val next = ctx.request.uri.copy(query = q)
            ctx.complete(OK, linkHeader(next) :: cacheHeader(slice) :: Nil, slice)
        }
      } ~
        path("by-date") {
          parameters('since ? 0L, 'limit ? 3) { (since, limit) =>
            ctx =>
              val slice = data.filter(_.created.isAfter(since)).take(limit)
              val q = Query("since" -> slice.last.created.getMillis.toString, "limit" -> limit.toString)
              val next = ctx.request.uri.copy(query = q)
              ctx.complete(OK, linkHeader(next) :: cacheHeader(slice) :: Nil, slice)
          }
        } ~
        path("by-id") {
          parameters('since ? "", 'limit ? 3) { (since, limit) =>
            ctx =>

              val slice = (try {
                val uuid = UUID.fromString(since)
                data.dropWhile(_.id != uuid)
              } catch {
                case _: Exception =>
                  data
              }).take(limit)

              val q = Query("since" -> slice.last.id.toString, "limit" -> limit.toString)
              val next = ctx.request.uri.copy(query = q)
              ctx.complete(OK, linkHeader(next) :: cacheHeader(slice) :: Nil, slice)
          }
        }

    })
}
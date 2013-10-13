package example

import spray.json._
import DefaultJsonProtocol._
import java.util.UUID
import org.joda.time.DateTime

trait ItemJsonProtocol extends DefaultJsonProtocol {
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

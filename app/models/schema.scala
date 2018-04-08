package models

import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import reactivemongo.bson.{BSONDateTime, BSONHandler, Macros}
import reactivemongo.play.json.BSONFormats._

object Handlers {

  DateTimeZone.setDefault(DateTimeZone.UTC)

  implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
    val fmt: DateTimeFormatter = ISODateTimeFormat.dateTime()
    def read(time: BSONDateTime): DateTime = new DateTime(time.value)
    def write(jdtime: DateTime): BSONDateTime = BSONDateTime(jdtime.getMillis)
  }

  implicit val coordinateHandler = Macros.handler[Coordinate]
  implicit val extraHandler = Macros.handler[Extra]
  implicit val priceHandler = Macros.handler[Price]
  implicit val propertyHandler = Macros.handler[Property]
}

object Formats {
  implicit val coordinateFormat = Json.format[Coordinate]
  implicit val extraFormat = Json.format[Extra]
  implicit val priceFormat = Json.format[Price]
  implicit val propertyFormat = Json.format[Property]
}
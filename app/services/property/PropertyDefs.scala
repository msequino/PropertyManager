package services.property

import models.{Coordinate, Extra, Price}
import play.api.libs.json.Json
import models.Formats._

object PropertyDefs {

  case class PropertyId( id: String )
  case class PropertyRes( id: Option[String], city: String, address: String, postcode: String, coordinate: Coordinate, extra: List[Extra], prices: List[Price])
  case class Properties( property: Seq[PropertyRes] )

  implicit val propertyIdFormF = Json.format[PropertyId]
  implicit val propertyFormF = Json.format[PropertyRes]
  implicit val propertiesFormF = Json.format[Properties]
}

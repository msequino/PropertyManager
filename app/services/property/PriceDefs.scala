package services.property

import play.api.libs.json.Json

object PriceDefs {

  case class PriceDTO(date: String, price: Double)

  implicit val pricesFormF = Json.format[PriceDTO]
}

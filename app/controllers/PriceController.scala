package controllers

import java.text.SimpleDateFormat

import javax.inject.{Inject, Singleton}
import models.Price
import models.Formats._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.property.PriceDefs.PriceDTO
import services.property.PropertyService
import utils.FutureUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import services.property._
import play.api.Logger

@Singleton
class PriceController @Inject()(cc: ControllerComponents,
                                propertyService: PropertyService)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

  def addToProperty(id: String) = Action.async(parse.json) { request =>

    Logger.info(s"addToProperty - propertyId[$id], request[${request}]")
    (for {
      newPrice <- Future.fromTry(Try(request.body.validate[PriceDTO].asOpt))
      if FutureUtils.notSatisfy(newPrice.isDefined) { new Exception("data not valid") }
      prop <- propertyService.getById(id)
      if FutureUtils.notSatisfy(prop.isDefined) { new Exception("data not valid") }
      res <- propertyService.edit(id, prop.map { x => x.copy( prices = Some(x.prices.getOrElse(List[Price]()) :+ new Price(dateFormatter.parse(newPrice.get.date), newPrice.get.price)) )}.get)
    } yield {
      Ok(Json.toJson(res))
    }) recover {
      case ex: Exception => {
        Logger.error(s"addToProperty exception - $ex")
        BadRequest(s"Cannot add the properties ${ex}")
      }
    }
  }
}

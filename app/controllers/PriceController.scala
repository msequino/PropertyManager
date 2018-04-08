package controllers

import javax.inject.{Inject, Singleton}
import models.{Price, Property}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.property.PropertyService
import utils.FutureUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class PriceController @Inject()(cc: ControllerComponents,
                                propertyService: PropertyService)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  def addToProperty(id: String) = Action.async(parse.json) { request =>

    (for {
      newPrice <- Future.fromTry(Try(request.body.validate[Price].asOpt))
      if FutureUtils.notSatisfy(newPrice.isDefined) { new Exception("data not valid") }
      prop <- propertyService.getById(id)
      if FutureUtils.notSatisfy(prop.isDefined) { new Exception("data not valid") }
      res <- propertyService.edit(prop.map { x => x.copy( prices = Some(x.prices.getOrElse(List[Price]()) :+ newPrice.get) )}.get)
    } yield {
      Ok(Json.toJson(res))
    }) recover {
      case ex: Exception => BadRequest(s"Cannot add the properties ${ex}")
    }
  }
}

package controllers

import javax.inject.{Inject, Singleton}
import models.Property
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.property.PropertyService
import utils.FutureUtils

import services.property.PropertyDefs._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import models.Formats._

@Singleton
class PropertyController @Inject()(cc: ControllerComponents, propertyService: PropertyService)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def get() = Action.async { request =>

    (for {
      properties <- propertyService.getAll()
    } yield {
      Ok(Json.toJson(properties.map(x => PropertyRes(Some(x.id), x.city, x.address, x.postcode, x.coordinates, x.extra.getOrElse(Nil), x.prices.getOrElse(Nil)))))
    }) recover {
      case ex: Exception => BadRequest("Cannot recover the properties")
    }
  }

  def add() = Action.async(parse.json) { request =>

    (for {
      prop <- Future.fromTry(Try(request.body.validate[PropertyRes].asOpt))
      if FutureUtils.notSatisfy(prop.isDefined) { new Exception("data not valid") }
      res <- propertyService.add(prop.map(x => Property(city = x.city, address = x.address, postcode = x.postcode, coordinates = x.coordinate, extra = Some(x.extra), prices = Some(x.prices))).get)
      if FutureUtils.notSatisfy(res.isDefined) { new Exception("property did not add") }
    } yield {
      Ok(Json.toJson(res.map(x => PropertyRes(Some(x.id), x.city, x.address, x.postcode, x.coordinates, x.extra.getOrElse(Nil), x.prices.getOrElse(Nil)))))
    }) recover {
      case ex: Exception => BadRequest(s"Cannot add the properties ${ex}")
    }
  }

  def edit() = Action.async(parse.json) { request =>

    (for {
      prop <- Future.fromTry(Try(request.body.validate[PropertyRes].asOpt))
      if FutureUtils.notSatisfy(prop.isDefined) { new Exception("data not valid") }
      res <- propertyService.edit(prop.get.id.get, prop.map(x => Property(city = x.city, address = x.address, postcode = x.postcode, coordinates = x.coordinate, extra = Some(x.extra), prices = Some(x.prices))).get)
    } yield {
      Ok(Json.toJson(res))
    }) recover {
      case ex: Exception => BadRequest(s"Cannot edit the property ${request.body} ${ex}")
    }
  }

  def delete() = Action.async(parse.json) { request =>

    (for {
      prop <- Future.fromTry(Try(request.body.validate[PropertyId].asOpt))
      if FutureUtils.notSatisfy(prop.isDefined) { new Exception("data not valid") }
      res <- propertyService.delete(prop.get.id)
    } yield {
      Ok(Json.toJson(res))
    }) recover {
      case ex: Exception => BadRequest(s"Cannot delete the property ${request.body}")
    }
  }
}

package controllers

import javax.inject.{Inject, Singleton}
import models.Property
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.property.PropertyService
import utils.FutureUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import models.Formats._

@Singleton
class PropertyController @Inject()(cc: ControllerComponents, propertyService: PropertyService)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def get() = Action.async { request =>

    (for {
      properties <- propertyService.getAll()
    } yield {
      Ok(Json.toJson(properties))
    }) recover {
      case ex: Exception => BadRequest("Cannot recover the properties")
    }
  }

  def add() = Action.async(parse.json) { request =>

    (for {
      prop <- Future.fromTry(Try(request.body.validate[Property].asOpt))
      if FutureUtils.notSatisfy(prop.isDefined) { new Exception("data not valid") }
      res <- propertyService.add(prop.get)
      if FutureUtils.notSatisfy(res.isDefined) { new Exception("property did not add") }
    } yield {
      Ok(Json.toJson(res.get))
    }) recover {
      case ex: Exception => BadRequest(s"Cannot add the properties ${ex}")
    }
  }

  def edit() = Action.async(parse.json) { request =>

    (for {
      prop <- Future.fromTry(Try(request.body.validate[Property].asOpt))
      if FutureUtils.notSatisfy(prop.isDefined) { new Exception("data not valid") }
      res <- propertyService.edit(prop.get)
    } yield {
      Ok(Json.toJson(res))
    }) recover {
      case ex: Exception => BadRequest(s"Cannot edit the property ${request.body}")
    }
  }

  def delete() = Action.async(parse.json) { request =>

    (for {
      prop <- Future.fromTry(Try(request.body.validate[Property].asOpt))
      if FutureUtils.notSatisfy(prop.isDefined) { new Exception("data not valid") }
      res <- propertyService.delete(prop.get)
    } yield {
      Ok(Json.toJson(res))
    }) recover {
      case ex: Exception => BadRequest(s"Cannot delete the property ${request.body}")
    }
  }
}

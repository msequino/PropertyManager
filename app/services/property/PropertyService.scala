package services.property

import models.Property

import scala.concurrent.Future

trait PropertyService {

  def getAll() : Future[List[Property]]
  def getById(id: String) : Future[Option[Property]]
  def add(property: Property) : Future[Option[Property]]
  def edit(property: Property) : Future[Property]
  def delete(property: Property) : Future[Boolean]
}

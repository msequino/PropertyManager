package services.property

import javax.inject.Singleton
import models.{Handlers, Property}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import utils.db.mongo.helpers.MongoHelper
import utils.db.mongo.{DefaultMongoDB, MongoGenericHandler}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.runtime.universe._

@Singleton
class PropertyServiceImpl extends PropertyService with DefaultMongoDB with MongoHelper{

  override implicit val handlerTable: Map[Type, MongoGenericHandler] = Map(
    typeTag[Property].tpe -> Handlers.propertyHandler
  )

  override def indexes(coll: BSONCollection, collType: Type): Unit = {
    // TODO manse put indexes
  }

  override def getAll() : Future[List[Property]] = getAll[Property]()

  override def getById(id: String) : Future[Option[Property]] = getOptById[Property](BSONObjectID.parse(id).toOption.get)

  override def add(property: Property) : Future[Option[Property]] = {
    val pr = property.copy(_id = Some(BSONObjectID.generate))
    insert[Property](property).map( x => if( x.ok ) Some(pr) else None)
  }

  override def edit(id: String, property: Property) : Future[Property] = findAndUpdate[Property](BSONDocument("_id" -> BSONObjectID(id)), property.copy(_id = BSONObjectID.parse(id).toOption))

  override def delete(id: String) : Future[Boolean] = deleteById[Property](BSONObjectID.parse(id).toOption.get).map(_ > 0)
}


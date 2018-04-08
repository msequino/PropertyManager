package utils.db.mongo.helpers

import java.util.Date

import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json._
import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.bson.BSONCountCommand.Count
import reactivemongo.api.commands.bson.BSONCountCommandImplicits._
import reactivemongo.api.commands.{Command, MultiBulkWriteResult, WriteResult}
import utils.db.mongo._
import reactivemongo.bson.{BSONDocument, BSONString, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}
import scala.reflect.runtime.universe._

import reactivemongo.bson.{BSONDocument, BSONString, _}
import reactivemongo.play.json.ImplicitBSONHandlers._

trait MongoHelper {
  this: MongoDB =>

  /// must override
  protected val handlerTable: Map[Type, MongoGenericHandler]

  /// should override
  protected def strategy(collectionName: String): FailoverStrategy = defaultStrategy
  protected def postInitCollection(collectionName: String): Unit = {}
  protected def indexes(coll: BSONCollection, collType: Type): Unit = {}

  def getCollection[T: TypeTag](implicit t: TypeTag[T]): BSONCollection = lookupTable(t.tpe)

  def insert[T](insertValue: T)(implicit typeTag: TypeTag[T]): Future[WriteResult] = context[T, WriteResult] { collection => implicit handler =>
    val value = handler.write(insertValue)
    println(s"insert document in ${collection.name}")
    collection.insert(value)
  }

  def insertBulk[T](insertValues: Seq[T])(implicit typeTag: TypeTag[T]): Future[MultiBulkWriteResult] = context[T, MultiBulkWriteResult] { collection => implicit handler =>
    println(s"insert bulk documents in ${collection.name}")
    val bulkDocs = insertValues.map(implicitly[collection.ImplicitlyDocumentProducer](_))
    collection.bulkInsert(ordered = false)(bulkDocs:_*)
  }

  def updateById[T](keyValue: BSONObjectID, updateValue: T)(implicit typeTag: TypeTag[T]): Future[T] = context[T, T] { collection => implicit handler =>
    val selector = BSONDocument("_id" -> keyValue)
    println(s"update document by ${pretty(selector)} in collection ${collection.name}")
    val time = BSONDocument("creation_time" -> true, "update_time" -> true)
    val value = handler.write(updateValue)
    val update = BSONDocument("$currentDate" -> time, "$set" -> value)
    collection.findAndUpdate(selector, update, fetchNewObject = true, upsert = true) flatMap { res =>
      res.result[T]
        .map { Future.successful }
        .getOrElse {
          println(s"update document by id in collection ${collection.name} success, but return value mapping failed")
          println(s"raw updated value: ${res.value}")
          Future.failed(new Exception("failed mapping document to required type"))
        }
    }
  }

  def updateByKey[T](keyName: String, keyValue: String, updateValue: T)(implicit typeTag: TypeTag[T]): Future[T] = context[T, T] { collection => implicit handler =>
    val selector = BSONDocument(keyName -> keyValue)
    println(s"update document by ${pretty(selector)} in collection ${collection.name}")
    val time = BSONDocument("creation_time" -> true, "update_time" -> true)
    val value = handler.write(updateValue)
    val update = BSONDocument("$currentDate" -> time, "$set" -> value)
    collection.findAndUpdate(selector, update, fetchNewObject = true, upsert = true) flatMap { res =>
      res.result[T]
        .map { Future.successful }
        .getOrElse {
          println(s"update document by key in collection ${collection.name} success, but return value mapping failed")
          println(s"raw updated value: ${res.value}")
          Future.failed(new Exception("failed mapping document to required type"))
        }
    }
  }

  def update[T](criteria: Seq[(String, Any)], modifier: Seq[(String, Any)])(implicit typeTag: TypeTag[T]): Future[Int] = context[T, Int] { collection => implicit handler =>
    val bcriteria = bsondocFromSeq(criteria)
    val bmodifier = bsondocsetFromSeq(modifier)
    println(s"update document by ${pretty(bcriteria)} in collection ${collection.name}")
    collection.update(bcriteria, bmodifier, multi = true).map(_.n)
  }

  def upsert[T](keyValue: BSONObjectID, updateValue: T)(implicit typeTag: TypeTag[T]): Future[Int] = context[T, Int] { collection => implicit handler =>
    val selector = BSONDocument("_id" -> keyValue)
    println(s"upsert document by ${pretty(selector)} in collection ${collection.name}")
    collection.update(selector, updateValue, upsert = true).map(_.n)
  }

  def upsert[T](criteria: BSONDocument, updateValue: T)(implicit typeTag: TypeTag[T]): Future[Int] = context[T, Int] { collection => implicit handler =>
    println(s"upsert document by ${pretty(criteria)} in collection ${collection.name}")
    collection.update(criteria, updateValue, upsert = true).map(_.n)
  }

  def findAndUpdate[T](criteria: BSONDocument, updateValue: T)(implicit typeTag: TypeTag[T]): Future[T] = context[T, T] { collection => implicit handler =>
    println(s"findAndUpdate document by ${pretty(criteria)} in collection ${collection.name}")
    val value: BSONDocument = BSONDocument("$set" -> bsonvalue[T](updateValue))
    collection.findAndUpdate(criteria, value, fetchNewObject = true, upsert = true) flatMap { res =>
      res.result[T]
        .map { Future.successful }
        .getOrElse {
          println(s"findAndUpdate document in collection ${collection.name} success, but return value mapping failed")
          println(s"raw updated value: ${res.value}")
          Future.failed(new Exception("failed mapping document to required type"))
        }
    }
  }

  def updateSeqById[T](id: BSONObjectID, modifier: Seq[(String, Any)])(implicit tableTag: TypeTag[T]): Future[Int] = context[T, Int] { collection => implicit handler =>
    val selector = BSONDocument("_id" -> id)
    println(s"update sequence by ${pretty(selector)} in collection ${collection.name}")
    val time = BSONDocument("update_time" -> true)
    val bmodifier = bsondocsetFromSeq(modifier)
    val update = BSONDocument("$currentDate" -> time)
    collection.update(selector, update ++ bmodifier).map(_.n)
  }

  def updateFieldByKey[T, V](keyName: String, keyValue: String, fieldName: String, fieldValue: V)(implicit tableTag: TypeTag[T], valueTag: TypeTag[V]): Future[Int] = context[T, Int] { collection => implicit handler =>
    val selector = BSONDocument(keyName -> keyValue)
    println(s"update field $fieldName by ${pretty(selector)} in collection ${collection.name}")
    val time = BSONDocument("update_time" -> true)
    val value = bsonvalue[V](fieldValue)
    val update = BSONDocument("$currentDate" -> time, "$set" -> BSONDocument(fieldName -> value))
    collection.update(selector, update).map(_.n)
  }

  def updateFieldById[T, V](id: BSONObjectID, fieldName: String, fieldValue: V)(implicit tableTag: TypeTag[T], valueTag: TypeTag[V]): Future[Int] = context[T, Int] { collection => implicit handler =>
    val selector = BSONDocument("_id" -> id)
    println(s"update field $fieldName by ${pretty(selector)} in collection ${collection.name}")
    val time = BSONDocument("update_time" -> true)
    val value = bsonvalue[V](fieldValue)
    val update = BSONDocument("$currentDate" -> time, "$set" -> BSONDocument(fieldName -> value))
    collection.update(selector, update).map(_.n)
  }

  def get[T](selector: Seq[(String, Any)])(implicit typeTag: TypeTag[T]): Future[T] = get(bsondocFromSeq(selector))
  def getById[T](keyValue: BSONObjectID)(implicit typeTag: TypeTag[T]): Future[T] = get(BSONDocument("_id" -> keyValue))
  def getByKey[T](keyName: String, keyValue: String)(implicit typeTag: TypeTag[T]): Future[T] = get(BSONDocument(keyName -> keyValue))

  def getOpt[T](selector: Seq[(String, Any)])(implicit typeTag: TypeTag[T]): Future[Option[T]] = getOpt(bsondocFromSeq(selector))
  def getOptById[T](keyValue: BSONObjectID)(implicit typeTag: TypeTag[T]): Future[Option[T]] = getOpt(BSONDocument("_id" -> keyValue))
  def getOptByKey[T](keyName: String, keyValue: String)(implicit typeTag: TypeTag[T]): Future[Option[T]] = getOpt(BSONDocument(keyName -> keyValue))

  def getOpt[T](query: BSONDocument)(implicit typeTag: TypeTag[T]): Future[Option[T]] = context[T, Option[T]] { collection => implicit handler =>
    println(s"get opt document by ${pretty(query)} in collection ${collection.name}")
    collection.find(query).one[T]
  }
  def get[T](query: BSONDocument)(implicit typeTag: TypeTag[T]): Future[T] = context[T, T] { collection => implicit handler =>
    println(s"get document by ${pretty(query)} in collection ${collection.name}")
    collection.find(query).one[T].flatMap {
      case Some(x) => Future.successful(x)
      case _ =>
        println(s"get document by key in collection ${collection.name} success, but value not found or return value mapping failed")
        Future.failed(new Exception("value not found or failed mapping document to required type"))
    }
  }

  def search[T](searchCondition: BSONDocument)(implicit typeTag: TypeTag[T]): Future[List[T]] = search[T](searchCondition, None, None)
  def searchWithSort[T](searchCondition: BSONDocument, sortCondition: BSONDocument)(implicit typeTag: TypeTag[T]): Future[List[T]] = search[T](searchCondition, Some(sortCondition), None)
  def searchWithPag[T](searchCondition: BSONDocument, pagination: (Int, Int))(implicit typeTag: TypeTag[T]): Future[List[T]] = search[T](searchCondition, None, Some(pagination))
  def searchWithSortAndPag[T](searchCondition: BSONDocument, sortCondition: BSONDocument, pagination: (Int, Int))(implicit typeTag: TypeTag[T]): Future[List[T]] = search[T](searchCondition, Some(sortCondition), Some(pagination))
  def getAll[T]()(implicit typeTag: TypeTag[T]): Future[List[T]] = search[T](BSONDocument(), None, None)
  def getAllWithSort[T](sortCondition: BSONDocument)(implicit typeTag: TypeTag[T]): Future[List[T]] = search[T](BSONDocument(), Some(sortCondition), None)
  def getAllWithPag[T](pagination: (Int, Int))(implicit typeTag: TypeTag[T]): Future[List[T]] = search[T](BSONDocument(), None, Some(pagination))
  def getAllWithSortAndPag[T](sortCondition: BSONDocument, pagination: (Int, Int))(implicit typeTag: TypeTag[T]): Future[List[T]] = search[T](BSONDocument(), Some(sortCondition), Some(pagination))
  def search[T](searchCondition: BSONDocument, sortCondition: Option[BSONDocument], pagination: Option[(Int, Int)])(implicit typeTag: TypeTag[T]): Future[List[T]] = context[T, List[T]] { collection => implicit handler =>
    println(s"search document by condition ${Json.toJson[BSONDocument](searchCondition)} in collection ${collection.name}")
    val query =  sortCondition map { cond => collection.find(searchCondition).sort(cond) } getOrElse collection.find(searchCondition)
    pagination map { case(num, size) =>
      query.options(QueryOpts(skipN = num * size)).cursor[T]().collect[List](size)
    } getOrElse {
      query.cursor[T]().collect[List]()
    }
  }

  def exists[T](keyName: String, keyValue: String)(implicit typeTag: TypeTag[T]): Future[Boolean] = context[T, Boolean] { collection => implicit handler =>
    val query = BSONDocument(keyName -> keyValue)
    println(s"find document by ${pretty(query)} in collection ${collection.name}")
    collection.find(query).one[T].flatMap {
      case Some(_) => Future.successful(true)
      case _ =>
        println(s"find document by key in collection ${collection.name} success, but value not found or return value mapping failed")
        Future.successful(false)
    }
  }

  def deleteByKey[T](keyName: String, keyValue: String)(implicit typeTag: TypeTag[T]): Future[Int] = context[T, Int] { collection => implicit handler =>
    val query = BSONDocument(keyName -> keyValue)
    println(s"delete document by ${pretty(query)} in collection ${collection.name}")
    collection.remove(query).map(_.n)
  }

  def deleteById[T](keyValue: BSONObjectID)(implicit typeTag: TypeTag[T]): Future[Int] = context[T, Int] { collection => implicit handler =>
    val query = BSONDocument("_id" -> keyValue)
    println(s"delete document by ${pretty(query)} in collection ${collection.name}")
    collection.remove(query).map(_.n)
  }

  def deleteByQuery[T](searchCondition: BSONDocument)(implicit typeTag: TypeTag[T]): Future[Int] = context[T, Int] { collection => implicit handler =>
    val query = searchCondition
    println(s"delete document by ${pretty(query)} in collection ${collection.name}")
    collection.remove(query).map(_.n)
  }

  def count[T](searchCondition: BSONDocument)(implicit tableTag: TypeTag[T]): Future[Int] = context[T, Int] { collection => implicit handler =>
    println(s"count condition ${pretty(searchCondition)} in collection ${collection.name}")
    collection.runCommand(Count(searchCondition)).map(x => x.count)
  }

  def command(command:BSONDocument): Future[BSONDocument] = {
    val runner = Command.run(BSONSerializationPack)
    runner(db, runner.rawCommand(command)).one[BSONDocument]
  }

  object lang {
    val IT = "it"
    val EN = "en"
    val NONE = "none"
  }

  //  NOTE: to create a text index, for mongodb 3.2 follow this:
  //  https://docs.mongodb.org/manual/tutorial/create-text-index-on-multiple-fields/#index-all-fields
  //  otherwise the mSearch and mLike doesn't work.
  def mSearch(search: String, language: String = lang.IT) = BSONDocument("$text" -> BSONDocument("$search" -> search, "$language" -> language))
  def mLike(field: String, search: String, insensitive: Boolean = true) = BSONDocument(field -> BSONDocument("$regex" -> BSONString(".*" + search + ".*"), "$options" -> (if(insensitive) "i" else "")))

  def mNot(field: String, value: String) = BSONDocument(field -> BSONDocument("$ne" -> value))
  def mNot(field: String, value: Int) = BSONDocument(field -> BSONDocument("$ne" -> value))
  def mNot(field: String, value: BSONObjectID) = BSONDocument(field -> BSONDocument("$ne" -> value))
  def mOr(fields: BSONDocument*) = BSONDocument("$or" -> fields)
  def mAnd(fields: BSONDocument*) = BSONDocument("$and" -> fields)
  def mExists(field: String) = BSONDocument(field -> BSONDocument("$exists" -> true))
  def mNotExists(field: String) = BSONDocument(field -> BSONDocument("$exists" -> false))
  def mSort(typingSort: String, fields: String*) = BSONDocument(fields.map(f => f -> BSONInteger(if (typingSort == "asc") 1 else -1)))

  protected def bsonvalue[T](value: Any)(implicit typeTag: TypeTag[T]): BSONValue = {
    value match {
      case value: String => BSONString(value).asInstanceOf[BSONValue]
      case value: Boolean => BSONBoolean(value).asInstanceOf[BSONValue]
      case value: Int => BSONInteger(value).asInstanceOf[BSONValue]
      case value: Long => BSONLong(value).asInstanceOf[BSONValue]
      case value: Double => BSONDouble(value).asInstanceOf[BSONValue]
      case value: Date => BSONDateTime(value.getTime).asInstanceOf[BSONValue]
      case _ => handlerTable(typeTag.tpe).asInstanceOf[MongoHandler[Any]].write(value)
    }
  }

  protected def bsondocFromSeq(selector: Seq[(String, Any)]): BSONDocument = BSONDocument(selector.map(x=>x._1->bsonvalue(x._2)))
  protected def bsondocsetFromSeq(selector: Seq[(String, Any)]): BSONDocument = BSONDocument("$set" -> BSONDocument(selector.map(x => x._1 -> bsonvalue(x._2))))

  protected def pretty(doc: BSONDocument): String = BSONDocument.pretty(doc).replace('\n', ' ')
  protected def pretty(doca: BSONArray): String = BSONArray.pretty(doca).replace('\n', ' ')
  protected def pretty(id: BSONObjectID): String = id.stringify

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  protected val defaultStrategy = FailoverStrategy(initialDelay = new FiniteDuration(1000, java.util.concurrent.TimeUnit.MILLISECONDS),
                                                   retries = 3, delayFactor = attemptNumber => 1 + attemptNumber * 0.5)

  private lazy val lookupTable: Map[Type, BSONCollection] = handlerTable.map(x => (x._1, initializeCollection(x._1.typeSymbol.asClass.name.toString, x._1)))

  private def context[T, RES](fun: BSONCollection => MongoHandler[T] => Future[RES])(implicit typeTag: TypeTag[T]): Future[RES] = {
    implicit val handler = handlerTable(typeTag.tpe).asInstanceOf[MongoHandler[T]]
    val collection: BSONCollection = lookupTable(typeTag.tpe)

    fun(collection)(handler) recoverWith { case ex =>
      println(s"database failure, stack trace:")
      ex.printStackTrace()
      Future.failed(ex)
    }
  }

  private def initializeCollection(collectionName: String, collType: Type): BSONCollection = {
    logger.info(s"initializing collection $collectionName")
    val coll: BSONCollection = db.collection(collectionName, strategy(collectionName))
    val outcome = coll.stats()
      .recoverWith { case ex =>
        println(s"failed stats on collection, creating and initializing [${ex.getMessage}]")
        coll.create().map { _ => postInitCollection(collectionName) }
      }
      .map { _ =>  indexes(coll, collType); coll }

    outcome map { _ =>
      logger.info(s"collection ${coll.name} successfully created")
    } recover { case ex =>
      println(s"collection ${coll.name} creation failed", ex.getLocalizedMessage)
    }

    Await.result(outcome, 15.seconds)
  }
}

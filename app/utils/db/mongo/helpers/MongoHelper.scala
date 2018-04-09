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
    logger.debug(s"insert document in ${collection.name}")
    collection.insert(value)
  }

  def findAndUpdate[T](criteria: BSONDocument, updateValue: T)(implicit typeTag: TypeTag[T]): Future[T] = context[T, T] { collection => implicit handler =>
    logger.debug(s"findAndUpdate document by ${pretty(criteria)} in collection ${collection.name}")
    val value: BSONDocument = BSONDocument("$set" -> bsonvalue[T](updateValue))
    collection.findAndUpdate(criteria, value, fetchNewObject = true, upsert = true) flatMap { res =>
      res.result[T]
        .map { Future.successful }
        .getOrElse {
          logger.debug(s"findAndUpdate document in collection ${collection.name} success, but return value mapping failed")
          logger.debug(s"raw updated value: ${res.value}")
          Future.failed(new Exception("failed mapping document to required type"))
        }
    }
  }

  def getAll[T]()(implicit typeTag: TypeTag[T]): Future[List[T]] = search[T](BSONDocument(), None, None)

  def getOptById[T](keyValue: BSONObjectID)(implicit typeTag: TypeTag[T]): Future[Option[T]] = getOpt(BSONDocument("_id" -> keyValue))

  def getOpt[T](query: BSONDocument)(implicit typeTag: TypeTag[T]): Future[Option[T]] = context[T, Option[T]] { collection => implicit handler =>
    logger.debug(s"get opt document by ${pretty(query)} in collection ${collection.name}")
    collection.find(query).one[T]
  }

  def search[T](searchCondition: BSONDocument, sortCondition: Option[BSONDocument], pagination: Option[(Int, Int)])(implicit typeTag: TypeTag[T]): Future[List[T]] = context[T, List[T]] { collection => implicit handler =>
    println(s"search document by condition ${Json.toJson[BSONDocument](searchCondition)} in collection ${collection.name}")
    val query =  sortCondition map { cond => collection.find(searchCondition).sort(cond) } getOrElse collection.find(searchCondition)
    pagination map { case(num, size) =>
      query.options(QueryOpts(skipN = num * size)).cursor[T]().collect[List](size)
    } getOrElse {
      query.cursor[T]().collect[List]()
    }
  }

  def deleteById[T](keyValue: BSONObjectID)(implicit typeTag: TypeTag[T]): Future[Int] = context[T, Int] { collection => implicit handler =>
    val query = BSONDocument("_id" -> keyValue)
    logger.debug(s"delete document by ${pretty(query)} in collection ${collection.name}")
    collection.remove(query).map(_.n)
  }

  def command(command:BSONDocument): Future[BSONDocument] = {
    val runner = Command.run(BSONSerializationPack, FailoverStrategy())
    runner(db, runner.rawCommand(command)).one[BSONDocument](ReadPreference.Primary)
  }

  object lang {
    val IT = "it"
    val EN = "en"
    val NONE = "none"
  }

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
      logger.debug(s"database failure, stack trace:")
      ex.printStackTrace()
      Future.failed(ex)
    }
  }

  private def initializeCollection(collectionName: String, collType: Type): BSONCollection = {
    logger.info(s"initializing collection $collectionName")
    val coll: BSONCollection = db.collection(collectionName, strategy(collectionName))
    val outcome = coll.stats()
      .recoverWith { case ex =>
        logger.debug(s"failed stats on collection, creating and initializing [${ex.getMessage}]")
        coll.create().map { _ => postInitCollection(collectionName) }
      }
      .map { _ =>  indexes(coll, collType); coll }

    outcome map { _ =>
      logger.info(s"collection ${coll.name} successfully created")
    } recover { case ex =>
      logger.debug(s"collection ${coll.name} creation failed", ex.getLocalizedMessage)
    }

    Await.result(outcome, 15.seconds)
  }
}

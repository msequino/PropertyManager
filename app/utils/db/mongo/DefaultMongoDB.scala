package utils.db.mongo
import reactivemongo.api.{DefaultDB, MongoConnectionOptions, MongoDriver}
import reactivemongo.core.nodeset.Authenticate

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait DefaultMongoDB extends MongoDB {
  override def driver: MongoDriver = DefaultMongoDB.driver
  override def db: DefaultDB = DefaultMongoDB.db
}

object DefaultMongoDB extends DefaultMongoDBConnection {
  lazy val driver: MongoDriver = new MongoDriver
  lazy val credentials = Seq(Authenticate(name, user, pass))
  lazy val db: DefaultDB = Await.result(driver.connection(Seq(s"$host:$port"), authentications = credentials).database(name), 30.seconds)
}

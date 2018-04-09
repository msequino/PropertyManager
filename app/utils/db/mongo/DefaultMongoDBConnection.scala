package utils.db.mongo

import com.typesafe.config.ConfigFactory

class DefaultMongoDBConnection extends MongoDBConnection {
  override def user: String = Option(ConfigFactory.load.getString("mongodb.user")) getOrElse { throw new IllegalArgumentException("missing configuration 'mongodb.user'") }
  override def name: String = Option(ConfigFactory.load.getString("mongodb.name")) getOrElse { throw new IllegalArgumentException("missing configuration 'mongodb.name'") }
  override def host: String = Option(ConfigFactory.load.getString("mongodb.host")) getOrElse { throw new IllegalArgumentException("missing configuration 'mongodb.name'") }
  override def port: Int = Option(ConfigFactory.load.getInt("mongodb.port")) getOrElse { throw new IllegalArgumentException("missing configuration 'mongodb.name'") }
}

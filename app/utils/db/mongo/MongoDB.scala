package utils.db.mongo

import reactivemongo.api.{DefaultDB, MongoDriver}

trait MongoDB {
  def driver: MongoDriver
  def db: DefaultDB
}

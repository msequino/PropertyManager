package utils.db.mongo

trait MongoDBConnection {
  def name:String
  def host:String
  def port:Int
}

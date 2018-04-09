package utils.db.mongo

trait MongoDBConnection {
  def user:String
  def name:String
  def host:String
  def port:Int
}

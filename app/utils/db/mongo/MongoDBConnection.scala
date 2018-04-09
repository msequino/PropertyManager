package utils.db.mongo

trait MongoDBConnection {
  def pass:String
  def user:String
  def name:String
  def host:String
  def port:Int
}

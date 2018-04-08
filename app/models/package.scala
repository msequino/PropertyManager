import reactivemongo.bson.BSONObjectID

package object models {
  def newId: Option[BSONObjectID] = Some(BSONObjectID.generate)
}
package models

import reactivemongo.bson.BSONObjectID

trait BaseModel {
  def _id: Option[BSONObjectID]

  def id: String = _id.map(i => i.stringify).getOrElse("")
}
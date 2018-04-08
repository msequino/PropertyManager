package models

import reactivemongo.bson.BSONObjectID

case class Property (override val _id: Option[BSONObjectID] = newId,
                     city: String,
                     address: String,
                     postcode: String,
                     coordinates: Coordinate,
                     extra : Option[List[Extra]],
                     prices: Option[List[Price]]
                    )  extends BaseModel
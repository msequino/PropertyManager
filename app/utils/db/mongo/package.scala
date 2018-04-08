package utils.db

import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONHandler, BSONInteger, BSONObjectID, BSONValue}

package object mongo {
  type MongoHandler[T] = BSONHandler[BSONDocument, T] with BSONDocumentReader[T] with BSONDocumentWriter[T]
  type MongoGenericHandler = BSONHandler[BSONDocument, _] with BSONDocumentReader[_] with BSONDocumentWriter[_]

  object lang {
    val IT = "it"
    val EN = "en"
    val NONE = "none"
  }

  sealed trait Sort
  case object ASC extends Sort
  case object DESC extends Sort

  implicit class Field(name: String) {
    def neq(value: String): BSONDocument = BSONDocument(name -> BSONDocument("$ne" -> value))
    def neq(value: Int): BSONDocument = BSONDocument(name -> BSONDocument("$ne" -> value))
    def neq(value: Boolean): BSONDocument = BSONDocument(name -> BSONDocument("$ne" -> value))
    def neq(value: BSONObjectID): BSONDocument = BSONDocument(name -> BSONDocument("$ne" -> value))
    def neq(value: BSONValue): BSONDocument = BSONDocument(name -> BSONDocument("$ne" -> value))

    def equ(value: String): BSONDocument = BSONDocument(name -> BSONDocument("$eq" -> value))
    def equ(value: Int): BSONDocument = BSONDocument(name -> BSONDocument("$eq" -> value))
    def equ(value: Boolean): BSONDocument = BSONDocument(name -> BSONDocument("$eq" -> value))
    def equ(value: BSONObjectID): BSONDocument = BSONDocument(name -> BSONDocument("$eq" -> value))
    def equ(value: BSONValue): BSONDocument = BSONDocument(name -> BSONDocument("$eq" -> value))

    def exists = BSONDocument(name -> BSONDocument("$exists" -> true))
    def notExists = BSONDocument(name -> BSONDocument("$exists" -> false))
  }

  implicit class BSONDocumentExtended(document: BSONDocument) {
    def and(other: BSONDocument*): BSONDocument = {
      // TODO: optimize to concat consecutive and, avoid generating and trees
      val fields = document +: other
      BSONDocument("$and" -> fields)
    }

    def or(other: BSONDocument*): BSONDocument = {
      // TODO: optimize to concat consecutive or, avoid generating or trees
      val fields = document +: other
      BSONDocument("$or" -> fields)
    }
  }

  implicit class StringExtended(string: String) {
    def search(language: String = lang.IT): BSONDocument = BSONDocument("$text" -> BSONDocument("$search" -> string, "$language" -> language))
  }

  implicit class StringSeqExtended(seq: Seq[String]) {
    def sort(sort: Sort = ASC): BSONDocument = BSONDocument(seq.map(f => f -> BSONInteger(if (sort == ASC) 1 else -1)))
  }
}

package scredis.protocol.requests

import scredis.protocol._
import scredis.serialization.{ Reader, Writer }

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.ListBuffer

object HashRequests {
  
  import scredis.serialization.Implicits.stringReader
  import scredis.serialization.Implicits.doubleReader
  
  private object HDel extends Command("HDEL")
  private object HExists extends Command("HEXISTS")
  private object HGet extends Command("HGET")
  private object HGetAll extends Command("HGETALL")
  private object HIncrBy extends Command("HINCRBY")
  private object HIncrByFloat extends Command("HINCRBYFLOAT")
  private object HKeys extends Command("HKEYS")
  private object HLen extends Command("HLEN")
  private object HMGet extends Command("HMGET")
  private object HMSet extends Command("HMSET")
  private object HScan extends Command("HSCAN")
  private object HSet extends Command("HSET")
  private object HSetNX extends Command("HSETNX")
  private object HVals extends Command("HVALS")
  
  case class HDel(keys: String*) extends Request[Long](HDel, keys: _*) {
    override def decode = {
      case IntegerResponse(value) => value
    }
  }
  
  case class HExists(key: String) extends Request[Boolean](HExists, key) {
    override def decode = {
      case i: IntegerResponse => i.toBoolean
    }
  }
  
  case class HGet[R: Reader](key: String, field: String) extends Request[Option[R]](
    HGet, key, field
  ) {
    override def decode = {
      case b: BulkStringResponse => b.parsed[R]
    }
  }
  
  case class HGetAll[R: Reader](key: String) extends Request[Map[String, R]](HGetAll, key) {
    override def decode = {
      case a: ArrayResponse => a.parsedAsPairsMap[String, R, Map] {
        case b: BulkStringResponse => b.flattened[String]
      } {
        case b: BulkStringResponse => b.flattened[R]
      }
    }
  }
  
  case class HIncrBy(key: String, field: String, value: Long) extends Request[Long](
    HIncrBy, key, field, value
  ) {
    override def decode = {
      case IntegerResponse(value) => value
    }
  }
  
  case class HIncrByFloat(key: String, field: String, value: Double) extends Request[Double](
    HIncrByFloat, key, field, value
  ) {
    override def decode = {
      case b: BulkStringResponse => b.flattened[Double]
    }
  }
  
  case class HKeys(key: String) extends Request[Set[String]](HKeys, key) {
    override def decode = {
      case a: ArrayResponse => a.parsed[String, Set] {
        case b: BulkStringResponse => b.flattened[String]
      }
    }
  }
  
  case class HLen(key: String) extends Request[Long](HLen, key) {
    override def decode = {
      case IntegerResponse(value) => value
    }
  }
  
  case class HMGet[R: Reader, CC[X] <: Traversable[X]](key: String, fields: String*)(
    implicit cbf: CanBuildFrom[Nothing, Option[R], CC[Option[R]]]
  ) extends Request[CC[Option[R]]](HMGet, key, fields: _*) {
    override def decode = {
      case a: ArrayResponse => a.parsed[Option[R], CC] {
        case b: BulkStringResponse => b.parsed[R]
      }
    }
  }
  
  case class HMGetAsMap[R: Reader, CC[X] <: Traversable[X]](key: String, fields: String*)(
    implicit cbf: CanBuildFrom[Nothing, Option[R], CC[Option[R]]]
  ) extends Request[Map[String, R]](HMGet, key, fields: _*) {
    override def decode = {
      case a: ArrayResponse => {
        val values = a.parsed[Option[R], List] {
          case b: BulkStringResponse => b.parsed[R]
        }
        fields.zip(values).flatMap {
          case (key, Some(value)) => Some((key, value))
          case _ => None
        }.toMap
      }
    }
  }
  
  case class HMSet[W: Writer](key: String, fieldValuePairs: (String, W)*) extends Request[Unit](
    HMSet,
    key,
    unpair(
      fieldValuePairs.map {
        case (field, value) => (field, implicitly[Writer[W]].write(value))
      }
    ): _*
  ) {
    override def decode = {
      case SimpleStringResponse(_) => ()
    }
  }
  
  case class HScan[R: Reader, CC[X] <: Traversable[X]](
    key: String,
    cursor: Long,
    matchOpt: Option[String],
    countOpt: Option[Int]
  )(
    implicit cbf: CanBuildFrom[Nothing, (String, R), CC[(String, R)]]
  ) extends Request[(Long, CC[(String, R)])](
    HScan,
    generateScanLikeArgs(
      keyOpt = Some(key),
      cursor = cursor,
      matchOpt = matchOpt,
      countOpt = countOpt
    )
  ) {
    override def decode = {
      case a: ArrayResponse => a.parsedAsScanResponse[(String, R), CC] {
        case a: ArrayResponse => a.parsedAsPairs[String, R, CC] {
          case b: BulkStringResponse => b.flattened[String]
        } {
          case b: BulkStringResponse => b.flattened[R]
        }
      }
    }
  }
  
  case class HSet[W: Writer](key: String, field: String, value: W) extends Request[Boolean](
    HSet, key, field, implicitly[Writer[W]].write(value)
  ) {
    override def decode = {
      case i: IntegerResponse => i.toBoolean
    }
  }
  
  case class HSetNX[W: Writer](key: String, field: String, value: W) extends Request[Boolean](
    HSetNX, key, field, implicitly[Writer[W]].write(value)
  ) {
    override def decode = {
      case i: IntegerResponse => i.toBoolean
    }
  }
  
  case class HVals[R: Reader, CC[X] <: Traversable[X]](key: String)(
    implicit cbf: CanBuildFrom[Nothing, R, CC[R]]
  ) extends Request[CC[R]](HVals, key) {
    override def decode = {
      case a: ArrayResponse => a.parsed[R, CC] {
        case b: BulkStringResponse => b.flattened[R]
      }
    }
  }
  
}
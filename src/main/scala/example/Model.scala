package example

import java.util.UUID
import org.joda.time.DateTime
import scala.util.Random
import akka.actor.Actor

class Model extends Actor {

  // Dummy data for illustration purposes, in ascending order by date
  val data = (for {
    x <- 0 to 100
  } yield Item(UUID.randomUUID(), new DateTime().minusDays(x), Random.nextInt(20))).reverse

  def receive = {
    case ItemsWithOffset(offset, limit) =>
      sender ! Items(data.drop(offset).take(limit))

    case ItemsSinceEpoch(since, limit) =>
      sender ! Items(data.filter(_.created.isAfter(since)).take(limit))

    case ItemsSinceId(id, limit) =>
      sender ! Items(data.dropWhile(_.id != id).take(limit))

  }

}
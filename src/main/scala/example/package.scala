
import java.util.UUID
import org.joda.time.DateTime

package example {

  case class Item(id: UUID, created: DateTime, cache: Int)

  case class ItemsWithOffset(offset: Int, limit: Int)

  case class ItemsSinceDateTime(dt: DateTime, limit: Int)

  case class ItemsSinceId(id: UUID, limit: Int)

  case class Items(slice: Iterable[Item])

}
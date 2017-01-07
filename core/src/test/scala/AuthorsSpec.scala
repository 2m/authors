package lt.dvim.authors

import akka.stream.scaladsl.Flow
import org.scalatest.WordSpec
import play.api.libs.streams.Streams

class AuthorsSpec extends WordSpec {

  "authors" should {
    "parse" in {
      //val enumeratee = io.circe.streaming.byteParser
      //Flow.fromProcessor(() => Streams.enumerateeToProcessor(enumeratee))
    }
  }

}

package lt.dvim.authors

import cats.syntax.either._ // not needed from scala 2.12
import io.circe.Decoder

final case class Author(name: String, login: String, url: String)
final case class Commit(sha: String, author: Author)

object Commit {
  implicit val decodeCommit: Decoder[Commit] = Decoder.instance(c =>
    for {
      sha <- c.downField("sha").as[String]
      name <- c.downField("commit").downField("author").downField("name").as[String]
      login <- c.downField("author").downField("login").as[String]
      url <- c.downField("author").downField("url").as[String]
    } yield Commit(sha, Author(name, login, url))
  )
}

package lt.dvim.authors

import com.tradeshift.reaktive.json.JSONProtocol._
import com.tradeshift.reaktive.marshal.Protocol._
import lt.dvim.scala.compat.javaslang.FunctionConverters._
import lt.dvim.scala.compat.javaslang.OptionConverters._

import javaslang.control.{Option => JsOption}

object GithubProtocol {
  final case class CommitUrl(url: String)

  final case class GithubAuthor(login: String, url: String)
  final case class GitAuthor(name: String, email: String)
  final case class Stats(additions: Int, deletions: Int)
  final case class Commit(githubAuthor: Option[GithubAuthor], gitAuthor: GitAuthor, stats: Stats)

  val compareProto =
    `object`(
      field(
        "commits",
        array(
          `object`(
            field("url", stringValue),
            CommitUrl
          )
        )
      )
    )

  val commitProto =
    `object`(
      option(
        field("author", githubAuthorProto)
      ),
      field("commit",
            `object`(
              field("author", gitAuthorProto)
            )),
      field("stats", statsProto),
      (githubAuthor: JsOption[GithubAuthor], gitAuthor: GitAuthor, stats: Stats) =>
        Commit(githubAuthor, gitAuthor, stats)
    )

  lazy val gitAuthorProto =
    `object`(
      field("name", stringValue),
      field("email", stringValue),
      GitAuthor
    )

  lazy val githubAuthorProto =
    `object`(
      field("login", stringValue),
      field("html_url", stringValue),
      GithubAuthor
    )

  lazy val statsProto =
    `object`(
      field("additions", integerValue),
      field("deletions", integerValue),
      (a: Integer, d: Integer) => Stats(a.intValue, d.intValue)
    )
}

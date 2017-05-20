package lt.dvim.authors

import com.tradeshift.reaktive.json.JSONProtocol._
import com.tradeshift.reaktive.marshal.Protocol._
import lt.dvim.scala.compat.javaslang.FunctionConverters._
import lt.dvim.scala.compat.javaslang.OptionConverters._

import javaslang.control.{Option => VavrOption}

object GithubProtocol {
  final case class GithubAuthor(login: String, url: String, avatar: String)
  final case class GitAuthor(name: String, email: String)
  final case class Stats(additions: Int, deletions: Int, commits: Int)
  final case class Commit(sha: String, message: String, gitAuthor: GitAuthor, githubAuthor: Option[GithubAuthor])
  final case class AuthorStats(gitAuthor: GitAuthor, githubAuthor: Option[GithubAuthor], stats: Stats)

  val compareProto =
    `object`(
      field(
        "commits",
        array(commitProto)
      )
    )

  lazy val commitProto =
    `object`(
      option(
        field("author", githubAuthorProto)
      ),
      field("commit",
            `object`(
              field("author", gitAuthorProto),
              field("message", stringValue),
              (gitAuthor: GitAuthor, message: String) =>
                (gitAuthor, message)
            )),
      field("sha", stringValue),
      (githubAuthor: VavrOption[GithubAuthor], gitAuthorAndMessage: (GitAuthor, String), sha: String) =>
        Commit(sha, gitAuthorAndMessage._2, gitAuthorAndMessage._1, githubAuthor)
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
      field("avatar_url", stringValue),
      GithubAuthor
    )
}

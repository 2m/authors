/*
 * Copyright 2016 Martynas Mickevičius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lt.dvim.authors

import com.tradeshift.reaktive.json.JSONProtocol._
import com.tradeshift.reaktive.marshal.Protocol._
import io.vavr.control.{Option => VavrOption}
import lt.dvim.scala.compat.vavr.OptionConverters._

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
      field(
        "commit",
        `object`(
          field("author", gitAuthorProto),
          field("message", stringValue),
          (gitAuthor: GitAuthor, message: String) => (gitAuthor, message)
        )
      ),
      field("sha", stringValue),
      (githubAuthor: VavrOption[GithubAuthor], gitAuthorAndMessage: (GitAuthor, String), sha: String) =>
        Commit(sha, gitAuthorAndMessage._2, gitAuthorAndMessage._1, githubAuthor)
    )

  lazy val gitAuthorProto =
    `object`(
      field("name", stringValue),
      field("email", stringValue),
      GitAuthor.apply(_, _)
    )

  lazy val githubAuthorProto =
    `object`(
      field("login", stringValue),
      field("html_url", stringValue),
      field("avatar_url", stringValue),
      GithubAuthor.apply(_, _, _)
    )
}

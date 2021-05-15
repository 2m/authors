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

import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}

object GithubProtocol {
  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames.withSnakeCaseConstructorNames

  @ConfiguredJsonCodec final case class GithubAuthor(login: String, htmlUrl: String, avatarUrl: String)
  @ConfiguredJsonCodec final case class GitAuthor(name: String, email: String)
  @ConfiguredJsonCodec final case class GitCommit(message: String, author: GitAuthor)
  @ConfiguredJsonCodec final case class Commit(sha: String, commit: GitCommit, author: Option[GithubAuthor])
}

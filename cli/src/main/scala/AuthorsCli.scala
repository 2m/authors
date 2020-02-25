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

import scala.concurrent.Await
import scala.concurrent.duration._

import org.rogach.scallop._

object AuthorsCli {
  import ScallopOpts._

  class Config(args: Seq[String]) extends ScallopConf(args) {
    banner("""|Fetches a summary of authors that contributed to a project between two points in git history.
              |Usage: authors [-p <path>] [-r <repo>] <from-ref> <to-ref>
              |
              |From and to git references can be:
              |  * commit short hash
              |  * tag name
              |  * HEAD
              |
              |If <path> is not specified, current directory "." is used by default.
              |
              |If <repo> is not specified, it is parsed from the origin url.
    """.stripMargin)

    val path = opt[String](default = Some("."), descr = "Path to the local project directory")
    val repo = opt[String](default = None, descr = "org/repo of the project")
    val timeout = opt[FiniteDuration](default = Some(30.seconds), descr = "Timeout for the command")
    val fromRef = trailArg[String]()
    val toRef = trailArg[String]()

    verify()
  }

  def main(args: Array[String]) = {
    val config = new Config(args.toIndexedSeq)

    val future =
      Authors.summary(
        config.repo.toOption,
        config.fromRef.toOption.get,
        config.toRef.toOption.get,
        config.path.toOption.get
      )

    println(Await.result(future, config.timeout.toOption.get))
  }
}

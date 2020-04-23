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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ParseRepoSpec extends AnyWordSpec with Matchers {

  "repo parser" must {
    "parse https url" in {
      Authors.parseRepo("https://github.com/2m/authors.git") shouldBe "2m/authors"
    }

    "parse ssh url" in {
      Authors.parseRepo("git@github.com:2m/authors.git") shouldBe "2m/authors"
    }
  }

}

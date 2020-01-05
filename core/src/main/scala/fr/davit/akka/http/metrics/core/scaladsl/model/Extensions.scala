/*
 * Copyright 2019 Michel Davit
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

package fr.davit.akka.http.metrics.core.scaladsl.model

import akka.http.scaladsl.model.Uri

import scala.annotation.tailrec

object Extensions {

  implicit class RichPath(path: Uri.Path) {
    import Uri.Path._

    @tailrec private def dropRec(n: Int, builder: Uri.Path): Uri.Path = {
      if (n <= 0) builder
      else builder match {
        case Empty => Empty
        case Slash(tail) => dropRec(n -1, tail)
        case Segment(_, tail) => dropRec(n - 1, tail)
      }
    }

    def take(n: Int): Uri.Path = {
      dropRec(path.length - n, path.reverse).reverse
    }


    def drop(n: Int): Uri.Path = {
      dropRec(n, path)
    }
  }
}

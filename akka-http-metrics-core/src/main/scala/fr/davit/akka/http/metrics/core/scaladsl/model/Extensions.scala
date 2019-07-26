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

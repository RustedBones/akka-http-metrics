package fr.davit.akka.http.metrics.core.scaladsl.model

import akka.http.scaladsl.model.Uri
import org.scalatest.{FlatSpec, Matchers}

class ExtensionsSpec extends FlatSpec with Matchers {

  import Extensions._

  "Extensions" should "Enrich Uri.Path with take" in {
    val empty = Uri.Path("")
    empty.take(0) shouldBe empty
    empty.take(3) shouldBe empty

    val singleSlash = Uri.Path("/")
    singleSlash.take(0) shouldBe empty
    singleSlash.take(3) shouldBe singleSlash

    val longPath = Uri.Path("1/3/5")
    longPath.take(0) shouldBe empty
    longPath.take(3) shouldBe Uri.Path("1/3")
  }

  it should "Enrich Uri.Path with drop" in {
    val empty = Uri.Path("")
    empty.drop(0) shouldBe empty
    empty.drop(3) shouldBe empty

    val singleSlash = Uri.Path("/")
    singleSlash.drop(0) shouldBe singleSlash
    singleSlash.drop(3) shouldBe empty

    val longPath = Uri.Path("1/3/5")
    longPath.drop(0) shouldBe longPath
    longPath.drop(3) shouldBe Uri.Path("/5")
  }

}

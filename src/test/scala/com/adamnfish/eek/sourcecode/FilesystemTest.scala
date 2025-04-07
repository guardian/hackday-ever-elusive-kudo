package com.adamnfish.eek.sourcecode

import cats.effect.IO
import fs2.io.file.{Files, Path}
import munit.{FunSuite, CatsEffectSuite}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

class FilesystemTest extends CatsEffectSuite {
  given LoggerFactory[IO] = Slf4jFactory.create[IO]

  // NOTE: this will fail if more docs get added to this repository
  // in that case, please update the assertion to match the new repository state
  test("Check it discovers the README from this project's own source code") {
    for {
      result <- new Filesystem[IO](".").repoDocs
      paths = result.map(_.path)
    } yield assertEquals(paths, List("README.md"))
  }
}

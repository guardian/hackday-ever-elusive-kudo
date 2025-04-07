package com.adamnfish.eek.sourcecode

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.*

class SourceCodeTest extends ScalaCheckSuite {
  val docsExtensions = List(".md", ".rst", ".txt", ".adoc")

  // allow bare file for "repo root" files like the README
  val repoRootExtensions = "" :: docsExtensions

  // repository root files (e.g. README)

  test("the root is not a documentation path") {
    assertEquals(SourceCode.isDocPath("/"), false)
  }

  property(
    "the README file is a documentation path (with any supported extension"
  ) {
    forAllNoShrink(Gen.oneOf(repoRootExtensions)) { extension =>
      assert(SourceCode.isDocPath(clue(s"README$extension")))
    }
  }

  property(
    "The FAQ file is a documentation path (with any supported extension)"
  ) {
    forAllNoShrink(Gen.oneOf(repoRootExtensions)) { extension =>
      assert(SourceCode.isDocPath(s"FAQ$extension"))
    }
  }

  // docs directory files

  test("docs dir example is considered a documentation path") {
    assert(SourceCode.isDocPath("docs/running-locally.md"))
  }

  property(
    "docs dir example is considered a documentation path (with any supported extension)"
  ) {
    forAllNoShrink(Gen.oneOf(docsExtensions)) { extension =>
      assert(SourceCode.isDocPath(clue(s"docs/browser-support$extension")))
    }
  }

  // other cases

  test("non-docs dir example is not considered a documentation path") {
    assertEquals(
      SourceCode.isDocPath("src/main/scala/com/adamnfish/Main.scala"),
      false
    )
  }

  property("non-docs file at the root is not considered a documentation path") {
    forAllNoShrink(Gen.oneOf(docsExtensions)) { extension =>
      assertEquals(SourceCode.isDocPath(s"LICENCE$extension"), false)
    }
  }

  test("project file in the root is not considered a documentation path") {
    assertEquals(SourceCode.isDocPath("build.sbt"), false)
  }
}

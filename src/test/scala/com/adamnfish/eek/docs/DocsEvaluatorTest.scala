package com.adamnfish.eek.docs

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.*
import com.adamnfish.eek.docs.DocsEvaluator.DocsQuality
import com.adamnfish.eek.docs.DocsEvaluator.DocsEvaluation
import com.adamnfish.eek.docs.DocsEvaluator.DocsEvaluation.{
  formatDocsEvaluation,
  formatDocsQuality
}
import com.adamnfish.eek.docs.DocsEvaluator.DocsQuality.{
  MayNeedImprovement,
  Missing
}

import scala.Console.{CYAN, RESET}

class DocsEvaluatorTest extends ScalaCheckSuite {
  test("example 'good' result is correct") {
    val label = "label"
    val expected = s"🟢 $label"
    assert(clue(formatDocsQuality(label, DocsQuality.Good)) == expected)
  }

  test("formatDocsQuality starts with 🟢 for 'good' results") {
    assert(clue(formatDocsQuality("label", DocsQuality.Good)).startsWith("🟢"))
  }

  test("formatDocsQuality contains the label for 'good' results") {
    forAll { (label: String) =>
      assert(clue(formatDocsQuality(label, DocsQuality.Good)).contains(label))
    }
  }

  test("example 'may need improvement' result is correct") {
    val label = "label"
    val summary = "summary"
    val expected = s"🟡 $label - ${CYAN}$summary${RESET}"
    assert(
      clue(
        formatDocsQuality(
          label,
          DocsQuality.MayNeedImprovement(summary)
        )
      ) == expected
    )
  }

  test("formatDocsQuality starts with 🟡 for 'may need improvement' results") {
    forAll { (summary: String) =>
      assert(
        clue(
          formatDocsQuality("label", DocsQuality.MayNeedImprovement(summary))
        ).startsWith("🟡")
      )
    }
  }

  test(
    "formatDocsQuality contains the label for 'may need improvement' results"
  ) {
    forAll { (label: String) =>
      assert(
        clue(
          formatDocsQuality(label, DocsQuality.MayNeedImprovement("summary"))
        ).startsWith("🟡")
      )
    }
  }

  test(
    "formatDocsQuality contains the summary for may need improvement results"
  ) {
    forAll { (summary: String) =>
      assert(
        clue(
          formatDocsQuality("label", DocsQuality.MayNeedImprovement(summary))
        ).contains(summary)
      )
    }
  }

  test("example 'missing' result is correct") {
    val label = "label"
    val expected = s"🔴 $label - ${CYAN}Not found${RESET}"
    assert(clue(formatDocsQuality(label, DocsQuality.Missing)) == expected)
  }

  test("formatDocsQuality starts with 🔴 for 'missing' results") {
    assert(
      clue(formatDocsQuality("label", DocsQuality.Missing)).startsWith("🔴")
    )
  }

  test("formatDocsQuality contains the label for 'missing' results") {
    forAll { (label: String) =>
      assert(
        clue(formatDocsQuality(label, DocsQuality.Missing)).contains(label)
      )
    }
  }

  property("formatDocsEvaluation contains the provided summary") {
    forAll { (owner: String, repo: String) =>
      assert(
        formatDocsEvaluation(s"$owner/$repo", DocsEvaluation.empty).contains(
          s"$owner/$repo"
        )
      )
    }
  }
}

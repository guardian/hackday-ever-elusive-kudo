package com.adamnfish.tdc.docs

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.*
import com.adamnfish.tdc.docs.DocsEvaluator.DocsQuality
import com.adamnfish.tdc.docs.DocsEvaluator.DocsEvaluation
import com.adamnfish.tdc.docs.DocsEvaluator.DocsEvaluation.{
  formatDocsEvaluation,
  formatDocsQuality
}
import com.adamnfish.tdc.docs.DocsEvaluator.DocsQuality.{
  MayNeedImprovement,
  Missing
}

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
    val expected = s"🟡 $label - $summary"
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
    val expected = s"🔴 $label - not found"
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

  property("formatDocsEvaluation contains the owner and repo") {
    forAll { (owner: String, repo: String) =>
      assert(
        formatDocsEvaluation(owner, repo, DocsEvaluation.empty).contains(
          s"$owner/$repo"
        )
      )
    }
  }
}

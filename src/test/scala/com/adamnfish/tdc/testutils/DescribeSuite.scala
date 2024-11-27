package com.adamnfish.tdc.testutils

import munit.*
import munit.internal.PlatformCompat

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.control.NonFatal

trait DescribeSuite
    extends munit.Suite
    with Assertions
    with TestOptionsConversions
    with DescribeTestTransforms
    with DescribeSuiteTransforms
    with DescribeValueTransforms {

  // for now require our test tree to be explicitly rooted here
  // once this is proven, we can consider the mutable "registration" approach that munit uses
  val tests: TestGroup

  // tree structure for defined tests
  enum TestGroup {
    case Group(groupOptions: TestOptions, testGroup: TestGroup*)
    case Test(test: munit.Test)
  }

  def describe(name: String)(testGroups: TestGroup*)(implicit
      loc: Location
  ): TestGroup.Group = {
    describe(new TestOptions(name))(testGroups*)
  }
  def describe(testOptions: TestOptions)(testGroups: TestGroup*)(implicit
      loc: Location
  ): TestGroup.Group = {
    TestGroup.Group(testOptions, testGroups*)
  }

  def test(
      name: String
  )(body: => Any)(implicit loc: Location): TestGroup.Test = {
    test(new TestOptions(name))(body)
  }
  def test(
      options: TestOptions
  )(body: => Any)(implicit loc: Location): TestGroup.Test = {
    TestGroup.Test {

      new Test(
        options.name,
        { () =>
          try {
            waitForCompletion(() => munitValueTransform(body))
          } catch {
            case NonFatal(e) =>
              Future.failed(e)
          }
        },
        options.tags,
        loc
      )
    }
  }

  def munitTimeout: Duration = new FiniteDuration(30, TimeUnit.SECONDS)
  private final def waitForCompletion[T](f: () => Future[T]) =
    PlatformCompat.waitAtMost(f, munitTimeout, munitExecutionContext)

  // Convert our tree of test groups into munit's list of tagged tests
  final override def munitTests(): Seq[Test] = {

    /** Traverse our tree of tests. Collects all the tests, but changes their
      * name and tags based on the groups it is in.
      */
    def loop(
        currentGroup: List[TestGroup],
        nestedGroupOptions: Seq[TestOptions],
        acc: Seq[Test]
    ): Seq[Test] = {
      currentGroup match
        case TestGroup.Test(test) :: tail =>
          // update the test name to include the groups
          // also applies the parent group's tags to this test
          val groupedTestName =
            if (nestedGroupOptions.isEmpty) test.name
            else nestedGroupOptions.map(_.name).mkString("", " > ", " > ")
          val groupedTestTags =
            nestedGroupOptions.flatMap(_.tags).toSet ++ test.tags
          acc :+ test
            .withName(groupedTestName)
            .withTags(groupedTestTags)
        case TestGroup.Group(thisGroupOptions, thisGroup*) :: tail =>
          // continue through the current group
          loop(
            tail,
            nestedGroupOptions,
            // but first go into this subtree, and use the result as the accumulator for subsequent steps
            loop(thisGroup.toList, nestedGroupOptions :+ thisGroupOptions, acc)
          )
        case Nil =>
          acc
    }

    munitSuiteTransform(
      loop(List(tests), Seq.empty, Seq.empty)
        .map(munitTestTransform)
        .toList
    )
  }
}

package org.scastie.sbt

import org.scastie.api._
import org.scastie.sbt.FormatActor
import org.scalatest.Assertions._
import org.scalatest.funsuite.AnyFunSuite

class FormatActorTest extends AnyFunSuite {
  test("format should accept scala 2 code") {
    val code = """
                 |class A {
                 |val a: Int=3
                 |}""".stripMargin
    val output = """class A {
                   |  val a: Int = 3
                   |}
                   |""".stripMargin
    assert(FormatActor.format(code, false, Jvm.default) == Right(output))
  }

  test("format should accept scala 2 worksheets") {
    val code = "val x:Int=41+1"
    val output = "val x: Int = 41 + 1\n"

    assert(Jvm.default.hasWorksheetMode)
    assert(FormatActor.format(code, true, Jvm.default) == Right(output))
  }

  test("format should accept scala 3 code") {
    val code = """
                 |class A {
                 |val a: Int=3
                 |}""".stripMargin
    val output = """class A {
                   |  val a: Int = 3
                   |}
                   |""".stripMargin
    assert(FormatActor.format(code, false, Scala3.default) == Right(output))
  }

  test("format should accept scala 3 worksheets") {
    val code = "val x:Int=41+1"
    val output = "val x: Int = 41 + 1\n"

    assert(Scala3.default.hasWorksheetMode)
    assert(FormatActor.format(code, true, Scala3.default) == Right(output))
  }

  test("Longer Scala 3 snippet is accepted (from Issue #511)") {
    val longerSnippet = """
                          |enum T:
                          |    case A
                          |    case B
                          |
                          |import T.*
                          |
                          |class C {
                          |  inline def get(using inline t: T): String =
                          |    inline t match
                          |      case A => "A"
                          |      case B => "B"
                          |}
                          |
                          |@main
                          |def main =
                          |  inline given t: T = A
                          |  val c = C()
                          |  test(c.get, "A")
                          |  test(c.get(using A), "A")
                          |  test(c.get(using B), "B")
                          |
                          |def test(actual: String, expected: String): Unit =
                          |  println(actual)
                          |  assert(actual == expected)
                          |""".stripMargin
    val res = FormatActor.format(longerSnippet, true, Scala3.default)
    assert(res.isRight, res)
  }
}
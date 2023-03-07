import core.{StringLiteral, Ancestors}
import graph.CallGraph.CallGraph
import graph.{CallGraph, Graph}

import org.scalatest.flatspec.AnyFlatSpec

class AncestorsTests extends AnyFlatSpec {
    behavior of "Ancestors flag"

    it should "no defs, returns an empty graph" in {
        val code = StringLiteral("""
                                   |object EmptyObject {
                                   |}
                                   |""".stripMargin)
        val result = Ancestors.ancestors(code, "defName").toDot("graphit", CallGraph.nodeLabeller).filterNot(_.isWhitespace)
        val expResult =
            """digraph graphit {
              |
              |}""".stripMargin.filterNot(_.isWhitespace)

        assert(result == expResult)
    }

    it should "single ancestor" in {
        val code = StringLiteral("""
                                   |object anObject {
                                   |    def A() = {
                                   |        B()
                                   |    }
                                   |    def B() = Unit
                                   |}
                                   |""".stripMargin)
        val result = Ancestors.ancestors(code, "B").toDot("graphit", CallGraph.nodeLabeller).filterNot(_.isWhitespace)
        val expResult =
            """digraph graphit {
              |A -> B
              |}""".stripMargin.filterNot(_.isWhitespace)

        assert(result == expResult)
    }

    it should "multiple ancestors" in {
        val code = StringLiteral("""
                                   |object anObject {
                                   |
                                   |    def A() = {
                                   |        B1()
                                   |        B2()
                                   |    }
                                   |
                                   |    def B1() = C()
                                   |
                                   |    def C() = Unit
                                   |
                                   |}
                                   |""".stripMargin)
        val result = Ancestors.ancestors(code, "C").toDot("graphit", CallGraph.nodeLabeller).filterNot(_.isWhitespace)
        val expResult =
            """digraph graphit {
              |B1 -> C
              |A -> B1
              |}""".stripMargin.filterNot(_.isWhitespace)

        assert(result == expResult)
    }

    it should "return whole diamond when bottom of diamond" in {
        val code = StringLiteral("""
                                    |object anObject {
                                    |    def A() = {
                                    |        B1()
                                    |        B2()
                                    |    }
                                    |    def B2() = C()
                                    |    def B1() = C()
                                    |    def C() = Unit
                                    |}
                                    |""".stripMargin)
        val result: Graph[String] = Ancestors.ancestors(code, "C").map(_.name)
        val expResult: Graph[String] = Graph.from(
            List("A", "B1", "B2", "C"),
            List(("A", "B1"), ("A", "B2"), ("B1", "C"), ("B2", "C")))

        assert(result.equals(expResult))
    }

    it should "still return a diamond when there's stuff below the root node" in {
        val code = StringLiteral("""
                                   |object anObject {
                                   |    def A() = {
                                   |        B1()
                                   |        B2()
                                   |    }
                                   |    def B2() = C()
                                   |    def B1() = C()
                                   |    def C() = D()
                                   |    def D() = Unit
                                   |}
                                   |""".stripMargin)
        val result: Graph[String] = Ancestors.ancestors(code, "C").map(_.name)
        val expResult: Graph[String] = Graph.from(
            List("A", "B1", "B2", "C"),
            List(("A", "B1"), ("A", "B2"), ("B1", "C"), ("B2", "C")))

        assert(result.equals(expResult))
    }
}

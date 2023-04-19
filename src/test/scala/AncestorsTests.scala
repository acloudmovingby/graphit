import java.io.File
import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

import scala.util.{Success, Try}
import core.{Ancestors, StringLiteral}
import graph.CallGraph.CallGraph
import graph.{CallGraph, Graph}
import subcommands.AncestorsMultiFile


class AncestorsTests extends AnyFlatSpec with should.Matchers {

    behavior of "single file ancestors"

    it should "no defs, returns an empty graph" in {
        val code = StringLiteral("""
                                   |object EmptyObject {
                                   |}
                                   |""".stripMargin)
        val result = Ancestors.ancestors(code, Seq("defName")).toDot("graphit", CallGraph.nodeLabeller).filterNot(_.isWhitespace)
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
        val result = Ancestors.ancestors(code, Seq("B")).toDot("graphit", CallGraph.nodeLabeller).filterNot(_.isWhitespace)
        val expResult =
            """digraph graphit {
              |A -> B
              |}""".stripMargin.filterNot(_.isWhitespace)

        assert(result == expResult)
    }

    /**
     * A calls B1 and B2, B1 calls C
     *
     *      A                                               A
     *     / \                                             /
     *    B1 B2                                           B1
     *     \             if C is root, returns:            \
     *      C                                               C
     *
     */
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
        val result = Ancestors.ancestors(code, Seq("C")).toDot("graphit", CallGraph.nodeLabeller).filterNot(_.isWhitespace)
        val expResult =
            """digraph graphit {
              |B1 -> C
              |A -> B1
              |}""".stripMargin.filterNot(_.isWhitespace)

        assert(result == expResult)
    }

    /***
     *  A calls B1 and B2, which both call C.
     *
     *      A                                               A
     *     / \                                             / \
     *    B1 B2                                           B1 B2
     *     \ /            if C is root, returns:           \ /
     *      C                                               C
     *
     */
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
        val result: Graph[String] = Ancestors.ancestors(code, Seq("C")).map(_.name)
        val expResult: Graph[String] = Graph.from(
            nodes = List("A", "B1", "B2", "C"),
            edges = List(("A", "B1"), ("A", "B2"), ("B1", "C"), ("B2", "C")))

        assert(result.equals(expResult))
    }

    /**
     * A calls B1 and B2, which both call C, which calls D
     *
     *      A                                               A
     *     / \                                             / \
     *    B1 B2                                           B1 B2
     *     \ /            if C is root, returns:           \ /
     *      C                                               C
     *      |
     *      D
     *
     *      If C is root, should return subgraph {A, B1, B2, C}
     */
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
        val result: Graph[String] = Ancestors.ancestors(code, Seq("C")).map(_.name)
        val expResult: Graph[String] = Graph.from(
            nodes = List("A", "B1", "B2", "C"),
            edges = List(("A", "B1"), ("A", "B2"), ("B1", "C"), ("B2", "C")))

        assert(result.equals(expResult))
    }

    behavior of "multi-file ancestors"

    /**
     * A
     *  \
     *   B
     *    \
     *     C
     */
    it should "work on multiple files with simple chain" in {
        val result: Try[CallGraph] = AncestorsMultiFile.ancestorGraph(new File("./src/test/resources/ancestor-tests/dir1/File3.scala"), "C", Seq("./src/test/resources/ancestor-tests"))
        val simplifiedResult: Graph[String] = result.get.map(_.name)

        val expResult: Graph[String] = Graph.from(
            nodes = List("A", "B", "C"),
            edges = List(("A", "B"), ("B", "C"))
        )

        simplifiedResult.nodes should be (expResult.nodes)
        simplifiedResult.edges should be (expResult.edges)
    }

    it should "doesn't explore cycles" in {
        val result: Try[CallGraph] = AncestorsMultiFile.ancestorGraph(new File("./src/test/resources/ancestor-tests/dir1/File3.scala"), "Z", Seq("./src/test/resources/ancestor-tests"))
        val simplifiedResult: Graph[String] = result.get.map(_.name)

        val expResult: Graph[String] = Graph.from(
            nodes = List("X", "Y", "Z"),
            edges = List(("X", "Y"), ("Y", "Z")) // note: it doesn't have edge ("X", "Z") even though that exists in the code
        )

        simplifiedResult.nodes should be (expResult.nodes)
        simplifiedResult.edges should be (expResult.edges)
    }
}

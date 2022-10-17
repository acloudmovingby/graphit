import org.scalatest.flatspec.AnyFlatSpec

import graph.Graph

class PathTests extends AnyFlatSpec {

    behavior of "path"

    it should "NOT find a path on an empty graph" in {
        val code = StringLiteral("""
                     |object EmptyObject {
                     |}
                     |""".stripMargin)
        val result = Path(code, "methodName1", "methodName2").generateDot().filterNot(_.isWhitespace)
        val expResult =
            """digraph graphit {
              |
              |}""".stripMargin.filterNot(_.isWhitespace)

        assert(result == expResult)
    }

    it should "NOT find a path between two unconnected nodes" in {
        val code = StringLiteral("""
                                   |object Test {
                                   |    def A() = 3
                                   |    def B() = 2
                                   |}
                                   |""".stripMargin)
        val result = Path(code, "A", "B").generateDot().filterNot(_.isWhitespace)
        val expResult =
            """digraph graphit {}""".filterNot(_.isWhitespace)

        assert(result == expResult)
    }

    it should "NOT find a path between two nodes even if those nodes are connected to others" in {
        val code = StringLiteral("""
                                   |object Test {
                                   |    def A() = 3
                                   |    def B() = { C() }
                                   |    def C() = { foo(); 5 }
                                   |}
                                   |""".stripMargin)
        val result = Path(code, "A", "B").generateDot().filterNot(_.isWhitespace)
        val expResult =
            """digraph graphit {}""".filterNot(_.isWhitespace)

        assert(result == expResult)
    }

    it should "find one path when A -> B" in {
        val code = StringLiteral("""
                                   |object Test {
                                   |    def A() = { B() }
                                   |    def B() = 3
                                   |}
                                   |""".stripMargin)
        val result = Path(code, "A", "B").generateDot().filterNot(_.isWhitespace)
        val expResult =
            """digraph graphit {
              |A -> B
              |}""".stripMargin.filterNot(_.isWhitespace)

        assert(result == expResult)
    }

    it should "find two paths when we have two methods calling each other (A -> B, B -> A)" in {
        val code = StringLiteral("""
                                   |object Test {
                                   |    def A() = { B() }
                                   |    def B() = { A() }
                                   |}
                                   |""".stripMargin)
        val result = Path(code, "A", "B").generateDot().filterNot(_.isWhitespace)
        val expResult =
            """digraph graphit {
              |A -> B
              |B -> A
              |}""".stripMargin.filterNot(_.isWhitespace)

        assert(result == expResult)
    }

    it should "find the path down a tree" in {
        val code = StringLiteral("""
                                   |object Test {
                                   |    def A() = { B(); foo() }
                                   |    def B() = { C() }
                                   |    def C() = 3
                                   |}
                                   |""".stripMargin)
        val result = Path(code, "A", "C").generateDot().filterNot(_.isWhitespace)
        val expResult =
            """digraph graphit {
              |A -> B
              |B -> C
              |}""".stripMargin.filterNot(_.isWhitespace)

        assert(result == expResult)
    }

}

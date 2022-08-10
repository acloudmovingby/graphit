import org.scalatest.flatspec.AnyFlatSpec

import GraphGenerators.graphFromString

class GraphBuilderTests extends AnyFlatSpec {

    behavior of "createGraph"

    it should "collect zero methods in code with no defs" in {
        val result = graphFromString("""object Foo { val x: Int = 5 }""")
        val nodes = List.empty[Def]
        val edges = List.empty[(Def, Def)]
        val expResult = Graph.from(nodes, edges)
        assert(result.isEmpty)
        assert(result == expResult)
    }

    it should "collect one method if there is one def" in {
        val result = graphFromString("""object Foo { def foo() = "hello" }""")
        val nodes = List(DefinedDef("foo", Vector("Foo")))
        val edges = List.empty[(Def, Def)]
        val expResult = Graph.from(nodes, edges)
        assert(result.size == 1)
        assert(result.edges.isEmpty)
        assert(result == expResult)
    }

    it should "make an edge when a call is made to a def within the file parsed" in {
        val result = graphFromString(
            """
              |object Foo {
              | def foo() = { bar() }
              | def bar() = ()
              |}
              |""".stripMargin)
        val (m1,  m2) = (
            DefinedDef("foo", Vector("Foo")),
            DefinedDef("bar", Vector("Foo"))
        )
        val nodes = List(m1, m2)
        val edges = List((m1, m2))
        val expResult = Graph.from(nodes, edges)
        assert(result.nodes.size == 2)
        assert(result.edges.size == 1)
        assert(result == expResult)
    }

    it should "make an edge even if the method isn't within the file parsed" in {
        val result = graphFromString(
            """
              |object Foo {
              | def foo() = { bar() }
              |}
              |""".stripMargin)
        val (m1, m2) = (
            DefinedDef("foo", Vector("Foo")),
            UnknownDef("bar")
        )
        val nodes = List(m1, m2)
        val edges = List((m1, m2))
        val expResult = Graph.from(nodes, edges)
        assert(result.nodes.size == 2)
        assert(result.edges.size == 1)
        assert(result == expResult)
    }

}

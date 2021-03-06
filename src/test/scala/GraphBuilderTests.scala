import org.scalatest.flatspec.AnyFlatSpec
import scala.meta._
import scalax.collection.Graph
import scalax.collection.GraphEdge._
import scalax.collection.GraphPredef._

import GraphBuilder.{CallGraph, createGraph, collectMethods}

class GraphBuilderTests extends AnyFlatSpec {

    def graphFromString(code: String): CallGraph = {
        val syntaxTree: Source = code.parse[Source].get
        createGraph(collectMethods(syntaxTree))
    }

    behavior of "createGraph"

    it should "collect zero methods in code with no defs" in {
        val result = graphFromString("""object Foo { val x: Int = 5 }""")
        val nodes = List.empty[Method]
        val edges = List.empty[DiEdge[Method]]
        val expResult = Graph.from(nodes, edges)
        assert(result.isEmpty)
        assert(result == expResult)
    }

    it should "collect one method if there is one def" in {
        val result = graphFromString("""object Foo { def foo() = "hello" }""")
        val nodes = List(DefinedMethod("foo", Vector("Foo")))
        val edges = List.empty[DiEdge[Method]]
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
            DefinedMethod("foo", Vector("Foo")),
            DefinedMethod("bar", Vector("Foo"))
        )
        val nodes = List(m1, m2)
        val edges = List(m1 ~> m2)
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
            DefinedMethod("foo", Vector("Foo")),
            UnknownMethod("bar")
        )
        val nodes = List(m1, m2)
        val edges = List(m1 ~> m2)
        val expResult = Graph.from(nodes, edges)
        assert(result.nodes.size == 2)
        assert(result.edges.size == 1)
        assert(result == expResult)
    }

}

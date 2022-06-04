import scala.meta._
import GraphBuilder.{CallGraph, collectMethods, createGraph}
import org.openide.util.NotImplementedException
import org.scalatest.flatspec.AnyFlatSpec

import java.io.File
import scala.meta.Source

class FilterTests extends AnyFlatSpec {

    def graphFromFile(fileName: String): CallGraph = {
        val file = new File(s"./src/test/scala/resources/$fileName.scala")
        val syntaxTree: Source = FileParser.toSyntaxTree(file)
        createGraph(collectMethods(syntaxTree))
    }

    def graphFromString(code: String): CallGraph = {
        val syntaxTree: Source = code.parse[Source].get
        createGraph(collectMethods(syntaxTree))
    }

    behavior of "matchesExcludedWord"

    it should "return false if the exclusion list is empty" in {
        assert(! Filters.matchesExcludedWord(List.empty)("test"))
    }

    it should "return false if it doesn't match anything in the exclusion list" in {
        assert(! Filters.matchesExcludedWord(List("foo"))("food"))
    }

    it should "return true if it does match something in the exclusion list" in {
        assert(Filters.matchesExcludedWord(List("foo*"))("food"))
        assert(Filters.matchesExcludedWord(List("whatever", "food"))("food"))
        assert(Filters.matchesExcludedWord(List("*oo*"))("food"))
        assert(Filters.matchesExcludedWord(List("*"))("food"))
        assert(Filters.matchesExcludedWord(List("*ood"))("food"))
    }

    behavior of "exclude"

    it should "change nothing if the exclusion list is empty" in {
        TestHelper.shouldPrint = true
        val graph = graphFromString(
            """
              |object Foo {
              | def foo() = bar()
              |}
              |""".stripMargin)
        val newGraph = Filters.exclude(List.empty)(graph)
        assert(graph == newGraph)
        TestHelper.shouldPrint = false
    }

    it should "change nothing if the entity doesn't exist" in {
        val graph = graphFromString(
            """
              |object Foo {
              | def foo() = bar()
              |}
              |""".stripMargin)
        val newGraph2 = Filters.exclude(List("apoenraora"))(graph)
        assert(graph == newGraph2)
    }

    it should "remove a method but not the ones it's attached to" in {
        val graph = graphFromString(
            """
              |class Foo {
              | def f1() = ()
              | def f2() = f1()
              | def f3() = f2()
              |}
              |""".stripMargin
        )
        val newGraph = Filters.exclude(List("f2"))(graph)
        assert(graph != newGraph)
        assert(newGraph.contains(DefinedMethod("f1", Vector("Foo"))))
        assert(! newGraph.contains(DefinedMethod("f2", Vector("Foo"))))
        assert(newGraph.contains(DefinedMethod("f3", Vector("Foo"))))
    }

    it should "remove all methods in the class if a class is named" in {
        val graph = graphFromString(
            """
              |class notDelete {
              | def foo() = ()
              |}
              |
              |class delete {
              | def foo() = ()
              |}
              |""".stripMargin)
        val newGraph = Filters.exclude(List("delete"))(graph)
        assert(graph != newGraph)
        assert(! newGraph.contains(DefinedMethod("foo", Vector("delete"))))
        assert(newGraph.contains(DefinedMethod("foo", Vector("notDelete"))))
    }

    it should "remove all methods in the trait if a trait is named" in {
        throw new NotImplementedException()

    }

    it should "remove all methods in the object if the object is named" in {
        throw new NotImplementedException()

    }

    it should "remove multiple methods" in {
        throw new NotImplementedException()

    }

    it should "be able to remove a variety of methods, classes, objects" in {
        throw new NotImplementedException()

    }

    it should "remove things with wildcards" in {
        throw new NotImplementedException()

    }

    behavior of "islands"

    it should "not change an empty graph" in {
        val graph = graphFromString(
            """
              |object Foo {
              |}
              |""".stripMargin)
        val newGraph = Filters.removeIslands(graph)
        assert(graph == newGraph)
        assert(newGraph.isEmpty)
    }

    it should "remove the one method in a single method graph" in {
        val graph = graphFromString(
            """
              |object Foo {
              | def foo() = ()
              |}
              |""".stripMargin)
        val newGraph = Filters.removeIslands(graph)
        assert(graph != newGraph)
        assert(newGraph.isEmpty)
    }

    it should "not remove a method even if there is only one edge" in {
        val graph = graphFromString(
            """
              |object Foo {
              | def foo() = bar()
              |}
              |""".stripMargin)
        val newGraph = Filters.removeIslands(graph)
        assert(graph == newGraph)
        assert(newGraph.size == 3)
    }

    it should "remove nodes that are isolates (i.e. no neighbors, degree zero)" in {
        val graph = graphFromFile("islands-test")
        val newGraph = Filters.removeIslands(graph)
        println(s"graph=$graph")
        assert(graph.nodes.distinct.size == 4)
        assert(newGraph.nodes.size == 3)
        assert(!newGraph.nodes.map(_.toOuter.name).contains("island"))
    }

    behavior of "fileOnly"

    it should "only remove methods that were just called but were never found in parsed code" in {
        val graph = graphFromString(
            """
              |object Foo {
              | def foo() = bar()
              |}
              |""".stripMargin)
        val newGraph = Filters.inFileOnly(graph)
        assert(graph.size == 3) // note: size is nodes + edges
        assert(newGraph.size == 1)
        assert(newGraph.nodes.map(_.toOuter.name).contains("foo"))
    }
}

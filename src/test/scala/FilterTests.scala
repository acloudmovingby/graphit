import java.io.File
import org.openide.util.NotImplementedException
import org.scalatest.flatspec.AnyFlatSpec
import scala.meta.Source
import scala.meta._
import scalax.collection.Graph
import scalax.collection.GraphPredef.{EdgeAssoc, edgeSetToSeq}

import GraphBuilder.{CallGraph, collectMethods, createGraph}

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

    behavior of "matches"

    it should "return false if the exclusion list is empty" in {
        assert(! Filters.matches(List.empty)("test"))
    }

    it should "return false if it doesn't match anything in the exclusion list" in {
        assert(! Filters.matches(List("foo"))("food"))
    }

    it should "return true if it matches something in the exclusion list, including wildcards" in {
        assert(Filters.matches(List("foo*"))("food"))
        assert(Filters.matches(List("whatever", "food"))("food"))
        assert(Filters.matches(List("*oo*"))("food"))
        assert(Filters.matches(List("*"))("food"))
        assert(Filters.matches(List("*ood"))("food"))
    }

    behavior of "remove"

    it should "change nothing if the exclusion list is empty" in {
        val graph = graphFromString(
            """
              |object Foo {
              | def foo() = bar()
              |}
              |""".stripMargin)
        val newGraph = Filters.remove(List.empty)(graph)
        assert(graph == newGraph)
    }

    it should "change nothing if the entity doesn't exist" in {
        val graph = graphFromString(
            """
              |object Foo {
              | def foo() = bar()
              |}
              |""".stripMargin)
        val newGraph2 = Filters.remove(List("apoenraora"))(graph)
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
        val newGraph = Filters.remove(List("f2"))(graph)
        assert(graph != newGraph)
        assert(newGraph.contains(DefinedMethod("f1", Vector("Foo"))))
        assert(! newGraph.contains(DefinedMethod("f2", Vector("Foo"))))
        assert(newGraph.contains(DefinedMethod("f3", Vector("Foo"))))
    }

    it should "works with classes" in {
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
        val newGraph = Filters.remove(List("del*te"))(graph)
        assert(graph != newGraph)
        assert(! newGraph.contains(DefinedMethod("foo", Vector("delete"))))
        assert(newGraph.contains(DefinedMethod("foo", Vector("notDelete"))))
    }

    it should "works with traits" in {
        val graph = graphFromString(
            """
              |class notDelete {
              | def foo() = ()
              |}
              |
              |trait delete {
              | def foo() = ()
              |}
              |""".stripMargin)
        val newGraph = Filters.remove(List("del*te"))(graph)
        assert(graph != newGraph)
        assert(! newGraph.contains(DefinedMethod("foo", Vector("delete"))))
        assert(newGraph.contains(DefinedMethod("foo", Vector("notDelete"))))
    }

    it should "works with objects" in {
        val graph = graphFromString(
            """
              |class notDelete {
              | def foo() = ()
              |}
              |
              |object delete {
              | def foo() = ()
              |}
              |""".stripMargin)
        val newGraph = Filters.remove(List("del*te"))(graph)
        assert(graph != newGraph)
        newGraph.nodes.map(_.toOuter.name)
        assert(! newGraph.contains(DefinedMethod("foo", Vector("delete"))))
        assert(newGraph.contains(DefinedMethod("foo", Vector("notDelete"))))
    }

    it should "be able to work with several removed words" in {
        val graph = graphFromString(
            """
              |class notDelete {
              | def foo() = ()
              | def bar() = rock()
              |}
              |
              |object delete {
              | def foo() = rabbit()
              | def rabbit() = dragon()
              |}
              |""".stripMargin)
        val newGraph = Filters.remove(List("fo*", "ra**it", "rock"))(graph)
        assert(graph != newGraph)
        assert(newGraph.size == 2)
        val nodeNames = newGraph.nodes.map(_.toOuter.name)
        assert(nodeNames.contains("bar"))
        assert(nodeNames.contains("dragon"))
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

    behavior of "exclude"

    def graphForExcludeTests: CallGraph = {
        val edges = {
            List("A" ->"B", "B" ->"C", "C" ->"D") ++ // A -> B -> C -> D
                List("X" ->"Y", "Y" ->"Z") ++ // X -> Y -> Z
                List("B" ->"Y", "Y" ->"D") // Both 'Y' and 'D' are connected to X, so they won't be excluded
        } map { case (src, target) => UnknownMethod(src) ~> UnknownMethod(target)} // I think it's fine for them all to be Unknown?
        Graph.from(List.empty, edges)
    }


    it should "remove nothing if the name given doesn't exist" in {
        val graph = graphForExcludeTests
        val excluded = Filters.exclude(List("whatever", "not there"))(graph)
        assert(excluded.size == 14)
        assert(graph == excluded)
    }

    it should "remove C if C is excluded" in {
        val graph = graphForExcludeTests
        val excludedA = Filters.exclude(List("C"))(graph)
        assert(excludedA.size == 11)
        assert(! excludedA.nodes.exists(_.toOuter.name == "C")) // C should no longer exist
        assert(excludedA.nodes.exists(_.toOuter.name == "D")) // keeps D
    }

    it should "remove X if X is excluded" in {
        val graph = graphForExcludeTests
        val excludedA = Filters.exclude(List("X"))(graph)
        assert(excludedA.size == 12)
        assert(! excludedA.nodes.exists(_.toOuter.name == "X")) // X should no longer exist
        assert(excludedA.nodes.exists(_.toOuter.name == "Y")) // keeps Y
    }

    it should "remove Z if Z is excluded" in {
        val graph = graphForExcludeTests
        val excludedA = Filters.exclude(List("Z"))(graph)
        assert(excludedA.size == 12)
        assert(! excludedA.nodes.exists(_.toOuter.name == "Z")) // Z should no longer exist
    }

    it should "remove D if D is excluded" in {
        val graph = graphForExcludeTests
        val excludedA = Filters.exclude(List("D"))(graph)
        assert(excludedA.size == 11)
        assert(! excludedA.nodes.exists(_.toOuter.name == "D")) // D should no longer exist
    }

    it should "remove B, C, and A if A is excluded" in {
        val graph = graphForExcludeTests
        val excludedA = Filters.exclude(List("A"))(graph)
        assert(excludedA.size == 7)
        assert(! excludedA.nodes.exists(_.toOuter.name == "A")) // A should no longer exist
        assert(! excludedA.nodes.exists(_.toOuter.name == "B")) // nor B
        assert(! excludedA.nodes.exists(_.toOuter.name == "C")) // nor C
    }

    it should "remove C, D, Y and Z if both C and Y are excluded" in {
        val graph = graphForExcludeTests
        val excludedA = Filters.exclude(List("C", "Y"))(graph)
        assert(excludedA.size == 4)
        assert(! excludedA.nodes.exists(_.toOuter.name == "C"))
        assert(! excludedA.nodes.exists(_.toOuter.name == "D"))
        assert(! excludedA.nodes.exists(_.toOuter.name == "Y"))
        assert(! excludedA.nodes.exists(_.toOuter.name == "Z"))
        // order shouldn't matter
        val excludedB = Filters.exclude(List("Y", "C"))(graph)
        assert(excludedB.size == 4)
        assert(! excludedB.nodes.exists(_.toOuter.name == "C"))
        assert(! excludedB.nodes.exists(_.toOuter.name == "D"))
        assert(! excludedB.nodes.exists(_.toOuter.name == "Y"))
        assert(! excludedB.nodes.exists(_.toOuter.name == "Z"))
    }
/*
    it should "remove descendants who have the excluded method as their unique and only shared ancestor" in {
        val graph = graphForExcludeTests
        assert(graph.size == 14) // 7 nodes + 7 edges
        assert(graph.nodes.exists(_.toOuter.name == "A")) // assert A exists first
        val excludedA = Filters.exclude(List("A"))(graph)
        assert(excludedA.size == 7) // now 4 nodes + 3 edges
        assert(! excludedA.nodes.exists(_.toOuter.name == "A")) // A should no longer exist
        assert(! excludedA.nodes.exists(_.toOuter.name == "B")) // nor B
        assert(! excludedA.nodes.exists(_.toOuter.name == "C")) // nor C

        val excludedB = Filters.exclude(List("B"))(graph)
        assert(excludedB.size == 8)
        assert(! excludedB.nodes.exists(_.toOuter.name == "B"))
        assert(! excludedB.nodes.exists(_.toOuter.name == "C"))
    }*/

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

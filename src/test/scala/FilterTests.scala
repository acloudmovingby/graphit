import java.io.File
import org.openide.util.NotImplementedException
import org.scalatest.flatspec.AnyFlatSpec

import scala.meta.Source
import scala.meta._

import GraphBuilder.{collectMethods, createGraph}
import CallGraph.CallGraph

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

    /** These helper methods cut way down on the line count of the tests */
    def existsInGraph(graph: CallGraph, name: String): Boolean = graph.nodes.exists(_.name == name)

    def shouldInclude(graph: CallGraph, nodeNames: List[String]): Boolean = {
        nodeNames
            .map { name => (name, existsInGraph(graph, name)) }
            .foldLeft(true) { case (result, (name, exists)) =>
                if (! exists) println(s"node $name should be in the graph but isn't. graph=$graph")
                result && exists
            }
    }

    def shouldNotInclude(graph: CallGraph, nodeNames: List[String]): Boolean =
        nodeNames
            .map { name => (name, existsInGraph(graph, name)) }
            .foldLeft(true) { case (result, (name, exists)) =>
                if (exists) println(s"node $name shouldn't be in the graph but is. graph=$graph")
                result && (!exists)
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
        val graph1 = graphFromString(
            """
              |object Foo {
              | def foo() = bar()
              |}
              |""".stripMargin)
        val graph2 = graph1.filter(Filters.remove(List.empty))
        assert(graph1.equals(graph2))
    }

    it should "change nothing if the entity doesn't exist" in {
        val graph1 = graphFromString(
            """
              |object Foo {
              | def foo() = bar()
              |}
              |""".stripMargin)
        val graph2 = graph1.filter(Filters.remove(List("apoenraora")))
        assert(graph1.equals(graph2))
    }

    it should "remove a method but not the ones it's attached to" in {
        val graph1 = graphFromString(
            """
              |class Foo {
              | def f1() = ()
              | def f2() = f1()
              | def f3() = f2()
              |}
              |""".stripMargin
        )
        val graph2 = graph1.filter(Filters.remove(List("f2")))
        assert(!graph1.equals(graph2))
        assert(graph2.nodes.contains(DefinedDef("f1", Vector("Foo"))))
        assert(! graph2.nodes.contains(DefinedDef("f2", Vector("Foo"))))
        assert(graph2.nodes.contains(DefinedDef("f3", Vector("Foo"))))
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
        val graph2 = graph.filter(Filters.remove(List("del*te")))
        assert(! graph.equals(graph2))
        assert(! graph2.nodes.contains(DefinedDef("foo", Vector("delete"))))
        assert(graph2.nodes.contains(DefinedDef("foo", Vector("notDelete"))))
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
        val graph2 = graph.filter(Filters.remove(List("del*te")))
        assert(! graph.equals(graph2))
        assert(! graph2.nodes.contains(DefinedDef("foo", Vector("delete"))))
        assert(graph2.nodes.contains(DefinedDef("foo", Vector("notDelete"))))
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
        val graph2 = graph.filter(Filters.remove(List("del*te")))
        assert(! graph.equals(graph2))
        assert(! graph2.nodes.contains(DefinedDef("foo", Vector("delete"))))
        assert(graph2.nodes.contains(DefinedDef("foo", Vector("notDelete"))))
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
        val graph2 = graph.filter(Filters.remove(List("fo*", "ra**it", "rock")))
        assert(! graph.equals(graph2))
        assert(graph2.size == 2)
        val nodeNames = graph2.nodes.map(_.name)
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
        val graph2 = graph.transform(Transformers.removeIslands)
        assert(graph == graph2)
        assert(graph2.isEmpty)
    }

    it should "remove the one method in a single method graph" in {
        val graph = graphFromString(
            """
              |object Foo {
              | def foo() = ()
              |}
              |""".stripMargin)
        val graph2 = graph.transform(Transformers.removeIslands)
        assert(! graph.equals(graph2))
        assert(graph2.isEmpty)
    }

    it should "not remove a method even if there is only one edge" in {
        val graph = graphFromString(
            """
              |object Foo {
              | def foo() = bar()
              |}
              |""".stripMargin)
        val graph2 = graph.transform(Transformers.removeIslands)
        assert(graph == graph2)
        assert(graph2.size == 3)
    }

    it should "remove nodes that are isolates (i.e. no neighbors, degree zero)" in {
        val graph = graphFromFile("islands-test")
        val graph2 = graph.transform(Transformers.removeIslands)
        println(s"graph=$graph")
        assert(graph.nodes.size == 4)
        assert(graph2.nodes.size == 3)
        assert(!graph2.nodes.map(_.name).contains("island"))
    }

    behavior of "exclude"

    def graphForExcludeTests: CallGraph = {
        val edges = {
            List("A" ->"B", "B" ->"C", "C" ->"D") ++
                List("X" ->"Y", "Y" ->"Z") ++
                List("B" ->"Y", "Y" ->"D")
        } map { case (src, target) => (UnknownDef(src), UnknownDef(target))} // For this test it's fine for them all to be Unknown
        Graph.from(List.empty, edges)
    }


    it should "remove nothing if the name given doesn't exist" in {
        val graph = graphForExcludeTests
        val excluded = graph.transform(Transformers.exclude(List("whatever", "not there")))
        assert(excluded.size == 14)
        assert(graph == excluded)
    }

    it should "remove C if C is excluded" in {
        val graph = graphForExcludeTests
        val afterExcluding = graph.transform(Transformers.exclude(List("C")))
        assert(afterExcluding.size == 11)
        assert(! afterExcluding.nodes.exists(_.name == "C")) // C should no longer exist
        assert(afterExcluding.nodes.exists(_.name == "D")) // keeps D
    }

    it should "remove X if X is excluded" in {
        val graph = graphForExcludeTests
        val afterExcluding = graph.transform(Transformers.exclude(List("X")))
        assert(afterExcluding.size == 12)
        assert(! afterExcluding.nodes.exists(_.name == "X")) // X should no longer exist
        assert(afterExcluding.nodes.exists(_.name == "Y")) // keeps Y
    }

    it should "remove Z if Z is excluded" in {
        val graph = graphForExcludeTests
        val afterExcluding = graph.transform(Transformers.exclude(List("Z")))
        assert(afterExcluding.size == 12)
        assert(! afterExcluding.nodes.exists(_.name == "Z")) // Z should no longer exist
    }

    it should "remove D if D is excluded" in {
        val graph = graphForExcludeTests
        val afterExcluding = graph.transform(Transformers.exclude(List("D")))
        assert(afterExcluding.size == 11)
        assert(! existsInGraph(afterExcluding, "D"))
    }

    it should "remove B, C, and A if A is excluded" in {
        val graph = graphForExcludeTests
        val afterExcluding = graph.transform(Transformers.exclude(List("A")))
        assert(afterExcluding.size == 7)
        assert(shouldInclude(afterExcluding, List("D")))
        assert(shouldNotInclude(afterExcluding, List("A", "B", "C")))
    }

    it should "remove C, D, Y and Z if both C and Y are excluded" in {
        val graph = graphForExcludeTests
        val afterExcludingA = graph.transform(Transformers.exclude(List("C", "Y")))
        assert(afterExcludingA.size == 4)
        assert(shouldNotInclude(afterExcludingA, List("C", "D", "Y", "Z")))

        // order shouldn't matter
        val afterExcludingB = graph.transform(Transformers.exclude(List("Y", "C")))
        assert(afterExcludingB.size == 4)
        assert(shouldNotInclude(afterExcludingB, List("C", "D", "Y", "Z")))
    }
/*
    it should "remove descendants who have the excluded method as their unique and only shared ancestor" in {
        val graph = graphForExcludeTests
        assert(graph.size == 14) // 7 nodes + 7 edges
        assert(graph.nodes.exists(_.name == "A")) // assert A exists first
        val afterExcluding = graph.transform(Transformers.exclude(List("A")))
        assert(afterExcluding.size == 7) // now 4 nodes + 3 edges
        assert(! afterExcluding.nodes.exists(_.name == "A")) // A should no longer exist
        assert(! afterExcluding.nodes.exists(_.name == "B")) // nor B
        assert(! afterExcluding.nodes.exists(_.name == "C")) // nor C

        val excludedB = graph.transform(Transformers.exclude(List("B")))
        assert(excludedB.size == 8)
        assert(! excludedB.nodes.exists(_.name == "B"))
        assert(! excludedB.nodes.exists(_.name == "C"))
    }*/

    behavior of "fileOnly"

    it should "only remove methods that were just called but were never found in parsed code" in {
        val graph = graphFromString(
            """
              |object Foo {
              | def foo() = bar()
              |}
              |""".stripMargin)
        val graph2 = graph.filter(Filters.inFileOnly)
        assert(graph.size == 3) // note: size is nodes + edges
        assert(graph2.size == 1)
        assert(existsInGraph(graph2, "foo"))
    }
}

import org.scalatest.flatspec.AnyFlatSpec

import graph.Graph

class GraphTests extends AnyFlatSpec {

    /** Did these tests to sanity check myself after I saw some weird behavior after refactoring */

    behavior of "equals"

    it should "return true if both graphs are empty" in {
        val g1 = Graph.from(List.empty, List.empty)
        val g2 = Graph.from(List.empty, List.empty)
        assert(g1 == g2)
    }

    it should "return true if both graphs have one element and its the same" in {
        val g1 = Graph.from(List("A"), List.empty)
        val g2 = Graph.from(List("A"), List.empty)
        assert(g1 == g2)
    }

    it should "return false if they both have one element but it's different" in {
        val g1 = Graph.from(List("A"), List.empty)
        val g2 = Graph.from(List("B"), List.empty)
        assert(g1 != g2)
    }

    it should "return true if they both the same edge" in {
        val g1 = Graph.from(List("A", "B"), List(("A", "B")))
        val g2 = Graph.from(List("A", "B"), List(("A", "B")))
        assert(g1 == g2)
    }

    it should "return false if they have same edge but opposite direction" in {
        val g1 = Graph.from(List("A", "B"), List(("A", "B")))
        val g2 = Graph.from(List("A", "B"), List(("B", "A")))
        assert(g1 != g2)
    }

    it should "return false if they have different edges" in {
        val g1 = Graph.from(List("A", "B"), List(("A", "B")))
        val g2 = Graph.from(List("A", "B"), List.empty)
        assert(g1 != g2)
    }
}

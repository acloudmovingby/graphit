package graph

import scalax.collection.GraphEdge._
import scalax.collection.GraphPredef._
import scalax.collection.io.dot._
import scalax.collection.{Graph => ScalaXGraph}

/**
 * A more ergonomic wrapper around the open source graph library (scalax) beecause I got tired of copying the same scalax
 * boilerplate everywhere. I know that there will not be duplicate values entered in the graph, which makes the graph
 * API much simpler.
 *
 * To use:
 * - has directed edges, but they're not weighted
 * - graph.Graph[A] means the node value is of type A. Node values must be unique and implement eq/hash. In other words,
 *     you can make a graph.Graph of Strings, with one node being "A" but not two.
 * being A
 *
 * */
class Graph[A]() {

    private var g: ScalaXGraph[A, DiEdge] =  ScalaXGraph()

    def isEmpty: Boolean = g.isEmpty

    def size: Int = g.size

    def nodes: Set[A] = g.nodes.toOuter

    def edges: Set[(A, A)] = g.edges.map((e: ScalaXGraph[A, DiEdge]#EdgeT) => (e.toOuter.head, e.toOuter.tail.head)).toSet

    /** Keeps nodes matching the predicate */
    def filter(p: A => Boolean): Graph[A] = Graph.fromScalaX(g filter g.having(p(_)))

    def find(p: A => Boolean): Option[A] = g.nodes.find(n => p(n.value)).map(_.toOuter)

    /** Simply applies f to the graph. Allows you to chain calls together along with filter */
    def transform[B](f: Graph[A] => Graph[B]): Graph[B] = f(this)

    def map[B](f: A => B): Graph[B] =
        Graph.from(this.nodes.map(f(_)).toSeq, this.edges.map(e => (f(e._1), f(e._2))).toSeq)

    def parentsOf(a: A): Seq[A] = g.get(a).diPredecessors.map(_.value).toSeq

    def childrenOf(a: A): Seq[A] = g.get(a).diSuccessors.map(_.value).toSeq

    def -(a: A): Graph[A] = Graph.fromScalaX(g - a)

    def ++(other: Graph[A]): Graph[A] = Graph.fromScalaX(g ++ other.g)

    override def equals(obj: Any): Boolean = obj match {
        case gr: Graph[_] => nodes.toSet.equals(gr.nodes.toSet) && edges.toSet.equals(gr.edges.toSet)
        case _ => false
    }

    /**
     * Encodes the graph into the DOT format for use with GraphViz or other programs.
     *
     * Note that if your nodeLabel method would cause distinct nodes to produce the same label, then those duplicates will be
     * combined into a single node in the output DOT graph.
     *
     * nodeLabel probably shouldn't produce Strings with DOT syntax (e.g. '->') but I haven't tested it much...
     * */
    def toDot(title: String, nodeLabel: A => String): String = {
        val root: DotRootGraph = DotRootGraph(
            directed = true,
            id = Some(Id(title)))

        def edgeTransformer(innerEdge: ScalaXGraph[A, DiEdge]#EdgeT): Option[(DotGraph, DotEdgeStmt)] = {
            innerEdge.edge match {
                case DiEdge(src, target) => Some((root, DotEdgeStmt(NodeId(nodeLabel(src.value)), NodeId(nodeLabel(target.value)), Nil)))
            }
        }

        def nodeTransformer(innerNode: ScalaXGraph[A, DiEdge]#NodeT): Option[(DotGraph, DotNodeStmt)] =
            Some((root, DotNodeStmt(NodeId(nodeLabel(innerNode.value)))))

        g.toDot(root, edgeTransformer, iNodeTransformer = Some(nodeTransformer))
    }
}

object Graph {
    def empty[A](): Graph[A] = from(Seq.empty, Seq.empty)

    def from[A](nodes: Seq[A], edges: Seq[(A, A)]): Graph[A] = fromScalaX(
        ScalaXGraph.from(nodes, edges.map { case (a, b) => a ~> b})
    )

    // After I use the operations provided by the library's graph, I then put my wrapper around it
    // this wrapper graph.Graph clas is a little silly, I should probably just implement this myself
    def fromScalaX[A](g: ScalaXGraph[A,DiEdge]): Graph[A] = {
        val graph = new Graph[A]
        graph.g = g
        graph
    }
}

import scalax.collection.{Graph => ScalaXGraph}
import scalax.collection.GraphEdge._
import scalax.collection.GraphPredef._
import scalax.collection.io.dot._

/**
 * A more ergonomic wrapper around the open source graph library (scalax) beecause I got tired of copying the same scalax
 * boilerplate everywhere. Graph libraries tend to have complex APIs due to the fact that 'graph' is encompasses a wide variety of structures (multigraphs,
 * hypergraphs, etc.) all with their own constraints and implementation details and forcing it all into one type is a bit ridiculous.
 *
 * Will anyone ever read these comments? Maybe? Well, if you care, one reason I can simplify the API greatly is that I know
 * that there will not be duplicate values entered in the graph, which gets away with all the nonsense with wrapper Node
 * objects and node indices and whatnot.
 *
 * The specs:
 * - directed edges, but not-weighted
 * - nodes (type A) must be unique and implement eq/hash
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

    /** Simply applies f to the graph. Allows you to chain calls together along with filter. */
    def transform[B](f: Graph[A] => Graph[B]): Graph[B] = f(this)

    def parentsOf(a: A): Seq[A] = g.get(a).diPredecessors.map(_.value).toSeq

    def childrenOf(a: A): Seq[A] = g.get(a).diSuccessors.map(_.value).toSeq

    def -(a: A): Graph[A] = Graph.fromScalaX(g - a)

    override def equals(obj: Any): Boolean = obj match {
        case gr: Graph[_] => nodes.equals(gr.nodes) && edges.equals(gr.edges)
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
    def from[A](nodes: Seq[A], edges: Seq[(A, A)]): Graph[A] = fromScalaX(
        ScalaXGraph.from(nodes, edges.map { case (a, b) => a ~> b})
    )

    def fromScalaX[A](g: ScalaXGraph[A,DiEdge]): Graph[A] = {
        val graph = new Graph[A]
        graph.g = g
        graph
    }
}

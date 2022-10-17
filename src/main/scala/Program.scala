import graph.{CallGraph, Def, Graph}
import graph.CallGraph.CallGraph

import java.io.File

trait ScalaSource
case class Files(files: Seq[File]) extends ScalaSource
case class StringLiteral(str: String) extends ScalaSource

trait RunnableProgram {
    val code: ScalaSource

    /** Returns DOT formatted string from a given source. We use this method for tests, but other methods for output
     * to console or opening in the browser. */
    def generateDot(): String

    def run(): Unit = println(generateDot())
    def runWeb(): Unit = Web.open(generateDot())
}

case class Default(
    code: ScalaSource,
    removedMethods: List[String],
    excludedMethods: List[String],
    keepIslands: Boolean
) extends RunnableProgram {
    override def generateDot(): String = {
        val fullGraph: CallGraph = GraphGenerators.graphFromSource(code)
        Filters.basicFilterSequence(
            fullGraph,
            removedMethods = removedMethods,
            excludedMethods = excludedMethods,
            keepIslands = keepIslands
        ).toDot("graphit", CallGraph.nodeLabeller)
    }
}

case class Path(
    code: ScalaSource,
    def1: String,
    def2: String
) extends RunnableProgram {
    override def generateDot(): String = {
        GraphGenerators.graphFromSource(code)
            .toDot("graphit", CallGraph.nodeLabeller)
    }
}

object Ancestors {

    def ancestors(scalaSource: ScalaSource, rootName: String): CallGraph = {

        val graph: CallGraph = GraphGenerators.graphFromSource(scalaSource)

        println(s"graph is: ${graph.toDot("graphit", CallGraph.nodeLabeller)}")

        val rootNode: Option[Def] = graph.find(_.name == rootName)

        println(s"rootNode=$rootNode")

        var visited: Set[Def] = Set.empty[Def]
        def parentsBFS(graph: CallGraph, root: Def): Set[String] = {
            println(s"root=$root, visited=$visited")
            visited = visited + root
            val recurOnParents: Set[String] = graph
                .parentsOf(root).toSet
                .diff(visited)
                .flatMap(parentsBFS(graph, _))

            println(s"value being sent back up: ${recurOnParents + root.name}")
            recurOnParents + root.name
        }

        rootNode match {
            case None => Graph.empty()
            case Some(rootDef: Def) =>
                val allAncestorNames: Set[String] = parentsBFS(graph, rootDef)
                println(s"allAncestorNames=$allAncestorNames")
                val temp = graph.filter(node => allAncestorNames.contains(node.name))
                println(s"temp nodes=${temp.nodes.map(_.name)}")
                println(s"temp edges=${temp.edges.map(e => s"${e._1.name} -> ${e._2.name}")}")
                temp
        }
    }

}
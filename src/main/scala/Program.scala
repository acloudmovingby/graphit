import graph.{CallGraph, Def, Graph}
import graph.CallGraph.CallGraph

import java.io.File

trait ScalaSource
case class Files(files: Seq[File]) extends ScalaSource
case class StringLiteral(str: String) extends ScalaSource

case class Path(
    code: ScalaSource,
    def1: String,
    def2: String
) {
    def generateDot(): String = {
        GraphGenerators.graphFromSource(code)
            .toDot("graphit", CallGraph.nodeLabeller)
    }
}

object Ancestors {

    def ancestors(scalaSource: ScalaSource, rootName: String): CallGraph = {

        val graph: CallGraph = GraphGenerators.graphFromSource(scalaSource)

        val rootNode: Option[Def] = graph.find(_.name == rootName)

        var visited: Set[Def] = Set.empty[Def]
        def parentsBFS(graph: CallGraph, root: Def): Set[String] = {
            visited = visited + root
            val recurOnParents: Set[String] = graph
                .parentsOf(root).toSet
                .diff(visited)
                .flatMap(parentsBFS(graph, _))

            recurOnParents + root.name
        }

        rootNode match {
            case None => Graph.empty()
            case Some(rootDef: Def) =>
                val allAncestorNames: Set[String] = parentsBFS(graph, rootDef)
                val temp = graph.filter(node => allAncestorNames.contains(node.name))
                temp
        }
    }

}
package core

import java.io.File

import graph.{CallGraph, Def, Graph}
import graph.CallGraph.CallGraph

trait ScalaSource
case class Files(files: Seq[File]) extends ScalaSource
case class StringLiteral(str: String) extends ScalaSource

/** Not implemented yet */
case class Path(
    code: ScalaSource,
    def1: String,
    def2: String
) {
    def generateDot(): String = ???
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
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

    def ancestors(scalaSource: ScalaSource, rootDefs: Seq[String]): CallGraph = ancestors(scalaSource, rootDefs, Int.MaxValue)

    def ancestors(scalaSource: ScalaSource, rootDefs: Seq[String], depth: Int): CallGraph = {

        val graph: CallGraph = GraphGenerators.graphFromSource(scalaSource)
        val rootNodes: Set[Def] = graph.nodes.filter(node => rootDefs.contains(node.name))

        var visited: Set[Def] = Set.empty[Def]

        def parentsBFS(graph: CallGraph, roots: Set[Def], depth: Int): Set[String] = {
            visited = visited ++ roots
            def recurOnParents: Set[String] = {
                val parents = roots.flatMap(graph.parentsOf)
                val notYetVisited = parents.diff(visited)
                parentsBFS(graph, notYetVisited, depth - 1)
            }

            if (depth > 0) recurOnParents ++ roots.map(_.name) else roots.map(_.name)
        }

        if (rootNodes.isEmpty) Graph.empty() else {
            val allAncestorNames: Set[String] = parentsBFS(graph, rootNodes, depth)
            val temp = graph.filter(node => allAncestorNames.contains(node.name))
            temp
        }
    }

}
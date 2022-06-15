import Filters.{exclude, inFileOnly, remove}
import GraphBuilder.CallGraph
import scalax.collection.Graph

import java.io.File

trait RunnableProgram {
    def run(): Unit
}

case class DefaultProgram(
    files: Seq[File] = List.empty,
    web: Boolean = false,
    removedMethods: List[String] = List.empty,
    excludedMethods: List[String] = List.empty,
    hadDirectoryArgs: Boolean = false, // were there any directories amongst the arguments?
    noIslands: Boolean = true
) extends RunnableProgram {
    def run(): Unit = {
        val graph: CallGraph = Some(GraphBuilder.buildGraph(files))
            .map(remove(removedMethods))
            .map(exclude(excludedMethods))
            .map(inFileOnly)
            .map(if (noIslands) Filters.removeIslands else identity)
            .getOrElse(Graph.empty)
        val dot = GraphBuilder.toDot(graph)
        if (web) Web.open(dot) else println(dot)
    }
}
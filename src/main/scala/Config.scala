import java.io.File
import scalax.collection.Graph

import scopt.OParser

import GraphBuilder.CallGraph

/** As arguments/flags are parsed, this gets mutated, then validated. Any validation that happens before
 * the files are actually parsed should have already happened. */
case class Config(
    files: Seq[File] = List.empty,
    web: Boolean = false,
    excludedMethods: List[String] = List.empty,
    path: Option[(String, String)] = None,
    showParents: List[String] = List.empty,
    showChildren: List[String] = List.empty,
    hadDirectoryArgs: Boolean = false, // were there any directories amongst the arguments?
    noIslands: Boolean = true
) {
    def run(): Unit = {
        val graph: CallGraph = Some(GraphBuilder.buildGraph(files))
            .map(Filters.exclude(excludedMethods))
            .map(Filters.inFileOnly)
            .map(if (noIslands) Filters.removeIslands else identity)
            .getOrElse(Graph.empty)
        val dot = GraphBuilder.toDot(graph)
        if (web) Web.open(dot)
        else println(dot)
    }
}

object Config {
    val genericError = "An error has occurred. Contact me through github?"
}
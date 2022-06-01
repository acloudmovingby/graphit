import java.io.File
import scalax.collection.Graph

import GraphBuilder.CallGraph

case class Config(
    files: Seq[File] = List.empty,
    web: Boolean = false,
    excludedMethods: List[String] = List.empty,
    path: Option[(String, String)] = None,
    showParents: List[String] = List.empty,
    showChildren: List[String] = List.empty,
    hadDirectoryArgs: Boolean = false, // were there any directories amongst the arguments?
    keepIslands: Boolean = true
)

object Config {
    val genericError = "An error has occurred. Contact me through github?"
}

trait Program {
    def run(): Unit
}

case class Basic(
    files: Seq[File],
    web: Boolean = false,
    exclude: List[String],
    keepIslands: Boolean
) extends Program {
    def run(): Unit = {
        val graph: CallGraph = Some(GraphBuilder.buildGraph(files))
            .map(Filters.exclude(exclude))
            .map(Filters.islands(keepIslands))
            .map(Filters.inFileOnly())
            .getOrElse(Graph.empty)
        val dot = GraphBuilder.toDot(graph)
        if (web) Web.open(dot)
        else println(dot)
    }
}

case class Path(
    files: Seq[File],
    web: Boolean,
    method1: String,
    method2: String
) extends Program {
    def run(): Unit = {
        println("Path not yet implemented")
    }
}


/** Abstraction of every run of program if it's not lazily evaluating file reading?
 * (1) read files and read in as ASTs: File => Tree/Source --> error handling, arg validation, etc.
 * (2) turn AST into graph structure: Tree/Source => CallGraph
 * (3) filter CallGraph
 * (4) convert to dot format
 * */

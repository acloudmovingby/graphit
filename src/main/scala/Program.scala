import CallGraph.CallGraph

import java.io.File

case class Program(
    files: Seq[File],
    web: Boolean,
    removedMethods: List[String],
    excludedMethods: List[String],
    keepIslands: Boolean
) {
    def run(): Unit = {
        val graph: CallGraph = GraphBuilder.buildGraph(files)
            .filter(Filters.remove(removedMethods))
            .transform(Transformers.exclude(excludedMethods))
            .filter(Filters.inFileOnly)
            .transform(if (keepIslands) identity else Transformers.removeIslands)

        val dot = graph.toDot("graphit", CallGraph.nodeLabeller)
        if (web) Web.open(dot) else println(dot)
    }
}
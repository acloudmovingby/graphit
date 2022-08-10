import CallGraph.CallGraph

import java.io.File

trait ScalaSource

case class Files(files: Seq[File]) extends ScalaSource

case class StringLiteral(str: String) extends ScalaSource

trait RunnableProgram {
    val code: ScalaSource
    /** Returns DOT formatted string from a given source. Allows testing at a high level */
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
        val graph: CallGraph = GraphGenerators.graphFromSource(code)
            .filter(Filters.remove(removedMethods))
            .transform(Transformers.exclude(excludedMethods))
            .filter(Filters.inFileOnly)
            .transform(if (keepIslands) identity else Transformers.removeIslands)
        graph.toDot("graphit", CallGraph.nodeLabeller)
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
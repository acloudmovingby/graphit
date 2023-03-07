package core

import graph.CallGraph.CallGraph
import GraphBuilder.{collectMethods, createGraph}

import java.io.File
import scala.meta._

object GraphGenerators {

    def graphFromSource(source: ScalaSource): CallGraph = {
        source match {
            case Files(files) => graphFromFiles(files)
            case StringLiteral(str) => graphFromString(str)
        }
    }

    def graphFromFiles(files: Seq[File]): CallGraph = {
        val methods = files.map(FileParser.toSyntaxTree).flatMap(collectMethods)
        createGraph(methods)
    }

    def graphFromString(code: String): CallGraph = {
        val syntaxTree: Source = code.parse[Source].get
        createGraph(collectMethods(syntaxTree))
    }

    // e.g. ./src/test/scala/resources/my-test-file.scala
    def graphFromTestFile(repoPath: String): CallGraph = {
        val file = new File(repoPath)
        val syntaxTree: Source = FileParser.toSyntaxTree(file)
        createGraph(collectMethods(syntaxTree))
    }

}

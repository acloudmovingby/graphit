package core

import graph.CallGraph.CallGraph
import GraphBuilder.{collectDefs, createGraph}

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
        val defs: Seq[DefWithCallsites] = files.flatMap { f =>
            val syntaxTree = FileParser.toSyntaxTree(f)
            val filename = f.getName
            collectDefs(Some(filename), syntaxTree)
        }
        createGraph(defs)
    }

    def graphFromString(code: String): CallGraph = {
        val syntaxTree: Source = code.parse[Source].get
        createGraph(collectDefs(syntaxTree))
    }

    // e.g. ./src/test/resources/my-test-file.scala
    def graphFromTestFile(repoPath: String): CallGraph = {
        val file = new File(repoPath)
        val syntaxTree: Source = FileParser.toSyntaxTree(file)
        createGraph(collectDefs(syntaxTree))
    }

}

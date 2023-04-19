import java.io.File
import scala.io.{Source => IOSource}
import scala.util.{Failure, Success, Try}

import graph.Graph


def contains(strs: Seq[String], files: Seq[File]): Try[Seq[File]] = Try {
    def searchFile(str: Seq[String], file: File): Boolean = {
        val bufferedSource = IOSource.fromFile(file)
        val isInFile = bufferedSource.getLines().exists(line => strs.exists(line.contains(_)))
        bufferedSource.close()
        isInFile
    }
    files.map(f => (f, searchFile(strs, f))).filter(_._2).map(_._1)
}



val c = contains(Seq("def", "bar"), Seq(new File("/Users/coates/Documents/code/graphit/src/main/scala/subcommands/worksheet.sc")))
println(c)

val f = new java.io.File("src/test/resources/ancestor-tests/dir1/File3.scala")


println(s"f exists? -> ${f.exists()}")
val c2 = contains(Seq("whatever"), Seq(new File("./src/test/resources/ancestor-tests/dir1/File3.scala")))
println(s"c2=$c2")
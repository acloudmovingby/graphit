import java.io.File
import scala.meta._

object FileParser {
    def parse(path: String): Source = {
        val filePath = java.nio.file.Paths.get(path)
        val bytes = java.nio.file.Files.readAllBytes(filePath)
        val text = new String(bytes, "UTF-8")
        val input = Input.VirtualFile(filePath.toString, text)
        input.parse[Source].get
    }

    def toSyntaxTree(file: File): Source = {
        val bytes = java.nio.file.Files.readAllBytes(file.toPath)
        val text = new String(bytes, "UTF-8")
        val input = Input.VirtualFile(file.toPath.toString, text)
        input.parse[Source].get
    }

    def collectScalaFiles(root: File): Seq[File] = {
        def getFiles(theRoot: File, extensions: Seq[String]): List[File] = {
            Option(theRoot) match {
                case Some(f) if f.isFile && extensions.exists(f.getName.endsWith(_)) => List(f)
                case Some(d) if d.isDirectory =>
                    d.listFiles().toList.flatMap(child => getFiles(child, extensions))
                case _ => Nil
            }
        }

        getFiles(root, List("scala"))
    }
}
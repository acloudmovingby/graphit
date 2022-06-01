import java.io.File
import scala.language.postfixOps
import scala.meta._
import scopt.OParser

object Main extends App {

    /** The parsing process works as follows:
     * - with each flag/argument found, changes are made to the Config object: StdIn => Config
     * - at each flag/argument, you can validate that argument in isolation (e.g. is it a File?): Arg/Flag => Boolean
     * - at the end, you run checkConfig for consistency of the whole Config (e.g. they don't use two contradictory flags): Config => Boolean
     * - then we run the program according to how the Config was, uh, configured. Program.run(Config) (or whatever) */
    val builder = OParser.builder[Config]

    val parser1 = {
        import builder._
        OParser.sequence(
            programName("graphit"),
            head("graphit", "0.1.0"),
            arg[File]("<arg1>,<arg2>...")
                .unbounded()
                .minOccurs(1)
                .action((root, c: Config) => c.copy(
                    files = c.files ++ FileParser.collectScalaFiles(root)
                ))
                .text("Args are absolute paths to '.scala' files or directories that contain '.scala' files (will search recursively).")
                .validate(f =>
                    if (f.isDirectory || f.getName.endsWith("scala")) success
                    else failure("Arguments must be .scala files, or directories containing .scala files (directories searched recursively).")),
            opt[Seq[String]]('e', "exclude")
                .valueName("<def1>,<def2>...")
                .unbounded()
                .optional()
                .action((pattern, c) => c.copy(excludedMethods = c.excludedMethods ++ pattern))
            .text("""Exclude any method matching <value>. Accepts * character as wildcard. For example '--exclude f*b*' would exclude method "foobar" from the graph"""),
            opt[Unit]("no-islands")
                .optional()
                .action((_, c) => c.copy(keepIslands = false))
                .text("excludes methods that are isolates, i.e. not connected to any other method in the files searched."),
            opt[Unit]("web")
                .action((_, c) => c.copy(web = true))
                .text("opens website to visualize graph (website is NOT affiliated with graphit so maybe don't upload source code.)"),
            opt[Seq[String]]('p', "path")
                .optional()
                .validate(f =>
                    if (f.size == 2) success
                    else failure("path flag requires exactly two scala def's. For example, 'graphit -p getId fetchIds <dir>' searches the given directory to find any method/function named 'getId' and 'fetchIds, and then shows all paths between those two methods."))
                .text("shows any paths between the two methods, i.e. is one a descendant call of the other."),
            /* opt[String]('a', "ancestor")
                 .minOccurs(2)
                 .maxOccurs(2)
                 .optional()
                 .text("""shows any method that is a predecessor (an "ancestor") of both arguments ."""),*/
            checkConfig {
                case c if c.path.isDefined && (c.showChildren.nonEmpty || c.showParents.nonEmpty) =>
                    failure("Cannot use children or parent flags when finding a path.")
                case c if c.files.isEmpty =>
                    failure("Must provide at least one .scala file, or directories containing at least one .scala file (directories searched recursively).")
                case _ => success
            }
        )
    }

    OParser.parse(parser1, args, Config()) match {
        case None => println(s"There was an error during parsing of the arguments. Sorry...")
        case Some(config) => Basic(config.files, config.web, config.excludedMethods, config.keepIslands).run()
    }

    //    println(s"this was the file: ${config.file}")
    //    val file1 = "/Users/coates/Documents/code/scalameta/hello-world/src/main/scala/SimpleScalaFile.scala"
    //    val file2 = "/Users/coates/Documents/code/scalameta/hello-world/src/main/scala/ScalaFile.scala"
//    val file3 = "/Users/coates/px/server/server/api/common/src/main/scala/com/paytronix/server/api/common/RemoteServiceProxy.scala"
//    val file4 = "/Users/coates/px/server/server/api/common/src/main/scala/com/paytronix/server/api/common/ServiceLocator.scala"
//    val file5 = "/Users/coates/px/server/server/misczio/src/main/scala/com/paytronix/server/misczio/effects.scala"
//    val file6 = "/Users/coates/px/server/server/dataobject/src/main/scala/com/paytronix/server/dataobject/GuestWebPropertyConfiguration.scala"
//    val file7 = "/Users/coates/px/server/server/service/notificationenqueuer/src/main/scala/com/paytronix/server/service/notificationenqueuer/NotificationEnqueuerServiceImpl.scala"
//    val file8 = "/Users/coates/px/server/server/service/enrollment/src/main/scala/com/paytronix/server/service/enrollment/Logic.scala"
//
//    def getListOfFiles(dir: File, extensions: List[String]): List[File] = {
//        val thisDirectoryFiles = dir.listFiles.filter(_.isFile).toList
//            .filter { file =>
//                extensions.exists(file.getName.endsWith(_))
//            }
//        val subdirectoryFiles = dir.listFiles.filter(_.isDirectory).toList
//            .flatMap(directory =>
//                getListOfFiles(directory, extensions)
//            )
//
//        thisDirectoryFiles ++ subdirectoryFiles
//    }
//
//    val allScalaFiles: Seq[String] = getListOfFiles(new File(args(0)), List("scala")).map(_.getAbsolutePath)
//    val graph = GraphBuilder.buildGraph(allScalaFiles) //args.toList)
//
//    val dot = GraphBuilder.toDot(graph)
//    println("******DOT FORMAT*****")
//    println(dot)
    /*val exampleTree = FileParser.parse(args(0))

    /*exampleTree.collect {
      case app: Term.Apply => {
        defNames.find(_)
        app.fun.toString()
      }
    }*/

    //println(s"defNames:\n$defNames")
    //GraphBuilder.collectMethods(exampleTree)
    val methods = GraphBuilder.collectMethods(exampleTree)
    //println("methods found:\n\t" + methods.mkString("\n\t"))
    val graph = GraphBuilder.createGraph(methods)
    //println(s"graph: \n\t$graph")
    val dot = GraphBuilder.toDot(graph)
    println("******DOT FORMAT*****")
    println(URLEncoder.encode(dot, StandardCharsets.UTF_8.toString))
    "ls -al" !*/
}

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
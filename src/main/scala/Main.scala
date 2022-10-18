import graph.CallGraph

import java.io.File
import scala.language.postfixOps
import scala.meta._
import scopt.OParser

object Main extends App {
    ArgParser.parse(args) match {
        case None => println(s"There was an error during parsing of the arguments. Sorry...")
        case Some(program) => program.run()
    }
}

case class Program(
    files: Seq[File] = List.empty,
    web: Boolean = false,
    removedMethods: List[String] = List.empty,
    excludedMethods: List[String] = List.empty,
    keepIslands: Boolean = true,
    ancestorRoot: Option[String] = None
) {

    def run(): Unit = {
        val graph = ancestorRoot match {
            case None => GraphGenerators.graphFromSource(Files(files))
            case Some(root) => Ancestors.ancestors(Files(files), root)
        }
        val dot = Filters.basicFilterSequence(graph, removedMethods, excludedMethods, keepIslands)
            .toDot("graphit", CallGraph.nodeLabeller)

        if (web) Web.open(dot) else println(dot)
    }
}

object ArgParser {

    /** used the scopt library which has its documentation at github here: https://github.com/scopt/scopt */
    def parse(args: Array[String]): Option[Program] = {

        // Note that "Program" is a data structure that gets replaced with new info as each flag/argument is parsed
        val builder = OParser.builder[Program]

        val parser1 = {
            import builder._
            OParser.sequence(
                programName("graphit"),
                head("graphit", "0.1.0"),
                // In each argument/option defined below, the action method modifies the Program
                arg[File]("<arg1>,<arg2>...")
                    .unbounded()
                    .minOccurs(1)
                    .action((root, c: Program) => c.copy(files = c.files ++ FileParser.collectScalaFiles(root)))
                    .text("Args are absolute paths to *.scala' files or directories that contain *.scala' files (will search recursively).")
                    // at each flag/argument, you can validate that argument in isolation (e.g. is it a File?)
                    .validate(f =>
                        if (f.isDirectory || f.getName.endsWith("scala")) success
                        else failure("Arguments must be *.scala files, or directories containing *.scala files (directories searched recursively).")),
                opt[Seq[String]]('r', "remove")
                    .valueName("<s1>,<s2>,...")
                    .unbounded()
                    .optional()
                    .action((pattern, c) => c.copy(removedMethods = c.removedMethods ++ pattern))
                    .text("""If s1 is "foo", then this removes from the final graph any methods named "foo", or methods which live in a class/object/trait named "foo". Accepts the `*` character as wildcard. Also see -e/--exclude."""),
                opt[Seq[String]]('e', "exclude")
                    .valueName("<s1>,<s2>...")
                    .unbounded()
                    .optional()
                    .action((pattern, c) => c.copy(excludedMethods = c.excludedMethods ++ pattern))
                    .text("""Removes all the nodes that -r/--remove would, as well as all their descendants. Good for cleaning up overly messy graphs."""),
                opt[Unit]("no-islands")
                    .optional()
                    .action((_, c) => c.copy(keepIslands = false))
                    .text("""Removes any `isolates` from the graph, i.e. methods which neither (1) make calls to other methods, nor (2) are called by other methods (within the file searched)."""),
                opt[Unit]('w',"web")
                    .action((_, c) => c.copy(web = true))
                    .text("Opens the website GraphvizOnline (https://github.com/dreampuf/GraphvizOnline) to show a visualization of your graph. Graphit is unaffiliated with ."),
                opt[Seq[String]]('p', "path")
                    .optional()
                    .validate(f =>
                        if (f.size == 2) success
                        else failure("Path flag requires exactly two scala def's. For example, 'graphit -c getId fetchIds <dir>' searches the given directory to find any method/function named 'getId' and 'fetchIds', and then shows all paths between those two methods."))
                    .text("Shows any paths between the two methods, i.e. is one a descendant call of the other."),
                opt[String]('a', "ancestors")
                    .optional()
                    .action((pattern, c) => c.copy(ancestorRoot = Some(pattern)))
                    .text("Show the defs that lead this def being called (the callers higher up in the calling hierarchy."),
                note(sys.props("line.separator")),
                help("help").text("prints this usage text"),
                // checkConfig checks for consistency of the whole Program (e.g. to prevent contradictory flags)
                checkConfig {
                    case c if c.files.isEmpty =>
                        failure("Must provide at least one .scala file, or directories containing at least one .scala file (directories searched recursively).")
                    case _ => success
                }
            )
        }

        // This is where we actually run our parser on the args. If successful, returns Some(config) and we can use
        // the collected information in our program
        OParser.parse(parser1, args, Program())
    }
}
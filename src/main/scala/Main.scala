import java.io.File
import scala.language.postfixOps
import scala.meta._
import scopt.OParser

object Main extends App {

    // used scopt library below which has its documentation at github here: https://github.com/scopt/scopt
    // for the most part followed syntax described in that documentation

    // Note that Program is the object that we mutate as each flag/argument is parsed
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
                .valueName("<name1>,<name2>...")
                .unbounded()
                .optional()
                .action((pattern, p) => p.copy(removedMethods = p.removedMethods ++ pattern))
            .text("""Removes from the graph anything matching the given names. This includes methods, classes, traits, or objects. Accepts the `*` character as wildcard. To remove connected nodes, see -e/--exclude."""),
            opt[Seq[String]]('e', "exclude")
            .valueName("<name1>,<name2>...")
            .unbounded()
            .optional()
            .action((pattern, p) => p.copy(excludedMethods = p.excludedMethods ++ pattern)),
            opt[Unit]("no-islands")
                .optional()
                .action((_, p) => p.copy(noIslands = true))
                .text("""Removes any `isolates` from the graph, i.e. methods which neither (1) make calls to other methods, nor (2) are called by other methods (within the file searched)."""),
            opt[Unit]('w',"web")
                .action((_, p) => p.copy(web = true))
                .text("Automatically opens your web browser to show a visualization of your graph using an open source website version of Graphviz (website is unaffiliated with graphit and I accept no liability for whatever you upload to it. https://github.com/dreampuf/GraphvizOnline)"),
            opt[Seq[String]]('p', "path")
                .optional()
                .validate(f =>
                    if (f.size == 2) success
                    else failure("Path flag requires exactly two scala def's. For example, 'graphit -p getId fetchIds <dir>' searches the given directory to find any method/function named 'getId' and 'fetchIds', and then shows all paths between those two methods."))
                .text("Shows any paths between the two methods, i.e. is one a descendant call of the other."),
            // checkConfig checks for consistency of the whole Program (e.g. to prevent contradictory flags)
            checkConfig {
                case p if p.path.isDefined && (p.showChildren.nonEmpty || p.showParents.nonEmpty) =>
                    failure("Cannot use children or parent flags when finding a path.")
                case p if p.files.isEmpty =>
                    failure("Must provide at least one .scala file, or directories containing at least one .scala file (directories searched recursively).")
                case _ => success
            }
        )
    }

    // This is where we actually run our parser on the args. If successful, returns Some(program) and we then run our
    // program with that program, which is hopefully in a valid state
    OParser.parse(parser1, args, Program()) match {
        case None => println(s"There was an error during parsing of the arguments. Sorry...")
        case Some(program) => program.run()
    }
}
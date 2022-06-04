import java.io.File
import scala.language.postfixOps
import scala.meta._
import scopt.OParser

object Main extends App {

    // used scopt library below which has its documentation at github here: https://github.com/scopt/scopt
    // for the most part followed syntax described in that documentation

    // Note that Config is the object that we mutate as each flag/argument is parsed
    val builder = OParser.builder[Config]

    val parser1 = {
        import builder._
        OParser.sequence(
            programName("graphit"),
            head("graphit", "0.1.0"),
            // In each argument/option defined below, the action method modifies the Config
            arg[File]("<arg1>,<arg2>...")
                .unbounded()
                .minOccurs(1)
                .action((root, c: Config) => c.copy(
                    files = c.files ++ FileParser.collectScalaFiles(root)
                ))
                .text("Args are absolute paths to '.scala' files or directories that contain '.scala' files (will search recursively).")
                // at each flag/argument, you can validate that argument in isolation (e.g. is it a File?)
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
                .action((_, c) => {
                    println("no-islands flag happened")
                    c.copy(noIslands = true)
                })
                .text("excludes methods that are not connected to any other method in the files searched."),
            opt[Unit]("web")
                .action((_, c) => c.copy(web = true))
                .text("opens website to visualize graph (website is NOT affiliated with graphit so maybe don't upload source code.)"),
            opt[Seq[String]]('p', "path")
                .optional()
                .validate(f =>
                    if (f.size == 2) success
                    else failure("path flag requires exactly two scala def's. For example, 'graphit -p getId fetchIds <dir>' searches the given directory to find any method/function named 'getId' and 'fetchIds', and then shows all paths between those two methods."))
                .text("shows any paths between the two methods, i.e. is one a descendant call of the other."),
            // check for consistency of the whole Config (e.g. they to prevent using contradictory flags)
            checkConfig {
                case c if c.path.isDefined && (c.showChildren.nonEmpty || c.showParents.nonEmpty) =>
                    failure("Cannot use children or parent flags when finding a path.")
                case c if c.files.isEmpty =>
                    failure("Must provide at least one .scala file, or directories containing at least one .scala file (directories searched recursively).")
                case _ => success
            }
        )
    }

    // This is where we actually run our parser on the args, starting with an inintial Config which is then mutated
    // as args are read in. If successful, returns Some(config) and we then run our program with that config, which is
    // hopefully valid
    OParser.parse(parser1, args, Config()) match {
        case None => println(s"There was an error during parsing of the arguments. Sorry...")
        case Some(config) =>
            println(s"config.keepIslands=${config.noIslands}")
            config.run()
    }
}
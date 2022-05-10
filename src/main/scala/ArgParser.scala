import java.io.File
import scopt.OParser

/**
 * Side-effectful top-level class to parse arguments
 */
object ArgParser {
    def parse(args: Array[String]) = {
        val parser = getParser()
        OParser.parse(parser, args, Config(List.empty)) match {
            case Some(config) =>
                run(config)
            case _ =>
            // arguments are bad, error message will have been displayed
        }
    }

    case class Config(
        fileNames: List[String],

    )

    private def getParser(): OParser[Unit, Config] = {

        val builder = OParser.builder[Config]
        import builder._    // TODO understand why this is the way it is...?

        /*
        TWO OPTIONS:
        (1) do everything in .action() blocks or at least delegate to other functions that do all the work
        (2) use the config model and have it parse the args into a case class that I then use to run the program
        Pros/cons:
        - with config we can clearly see the input "state" of the program, which might make it easier to unit test later
        - more elaborate than it needs to be?
         */

        OParser.sequence(
            programName("graphit"),
            head("graphit", "1.0"),
            arg[File]("<file>...")
                .unbounded()
                .optional()
                .text("Enter a path to one or more Scala files. Type -h to learn more.")
                .action { case (file, config) =>
                    config.copy(fileNames = config.fileNames ++ List(file.getName))
                }
        )
    }

    private def run(c: Config): Unit = {
        println(s"Filenames were: ${c.fileNames}")
    }
}
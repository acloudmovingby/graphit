import java.io.File

/**
 * As arguments/flags are parsed, this gets mutated, then validated by the parser.
 */
case class ParsedArgs(
    files: Seq[File] = List.empty,
    web: Boolean = false,
    removedMethods: List[String] = List.empty,
    excludedMethods: List[String] = List.empty,
    keepIslands: Boolean = true
) {
    def generateProgram(): RunnableProgram = Default(Files(files), removedMethods, excludedMethods, keepIslands)
}
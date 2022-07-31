import java.io.File

/**
 * As arguments/flags are parsed, this gets mutated, then validated by the parser.
 */
case class Config(
    files: Seq[File] = List.empty,
    web: Boolean = false,
    removedMethods: List[String] = List.empty,
    excludedMethods: List[String] = List.empty,
    keepIslands: Boolean = true
) {
    /** TODO Return as Either? Left if not possible to make a valid program from the args/flags*/
    def getProgram(): Program = Program(files, web, removedMethods, excludedMethods, keepIslands)

    // run program and pattern match on output and web,
}
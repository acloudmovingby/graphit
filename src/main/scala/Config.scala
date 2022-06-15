import java.io.File

/** As arguments/flags are parsed, this gets mutated, then validated. Any validation that happens before
 * the files are actually parsed should have already happened. */
case class Config(
    files: Seq[File] = List.empty,
    web: Boolean = false,
    removedMethods: List[String] = List.empty,
    excludedMethods: List[String] = List.empty,
    path: Option[(String, String)] = None,
    parents: Option[String] = None,
    hadDirectoryArgs: Boolean = false, // were there any directories amongst the arguments?
    noIslands: Boolean = true
) {
    /** TODO Return as Either? Left if not possible to make a valid program from the args/flags*/
    def getProgram(): RunnableProgram = DefaultProgram(files, web, removedMethods, excludedMethods, hadDirectoryArgs, noIslands)
}
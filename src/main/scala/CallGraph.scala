object CallGraph {
    /**
     * The core data structure of graphit
     */
    type CallGraph = Graph[Def]
    def nodeLabeller: Def => String = _.name
}

/**
 * "Method" is a slightly inaccurate name, as it really refers to any "def" found in the files
 */
trait Def {
    val name: String
}

case class UnknownDef(
    name: String
) extends Def

case class DefinedDef(
    name: String,
    parents: Vector[String]
    // TODO: file name
) extends Def

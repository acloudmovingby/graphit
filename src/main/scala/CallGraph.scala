object CallGraph {
    /**
     * The core data structure of graphit
     */
    type CallGraph = Graph[Def]
    def nodeLabeller: Def => String = _.name
}

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

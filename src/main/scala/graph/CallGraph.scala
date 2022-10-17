package graph

object CallGraph {
    /**
     * Each node is a def; an edge e from def A to def B means B is called in the body of A:
     *     def A() = {
     *         ...
     *         B()
     *         ...
     *     }
     *
     *     def B() = { ... }
     */
    type CallGraph = Graph[Def]
    def nodeLabeller: Def => String = _.name
}

//"def foo(...) = ..." => name is "foo"
trait Def {
    val name: String
}

// Found places where the def is called, but it's actual definition was never found in the scala code searched.
case class UnknownDef(name: String) extends Def

// Defined in the files
case class DefinedDef(
    name: String,
    parents: Vector[String]
    // TODO: file name?
) extends Def

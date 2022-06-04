import scala.util.matching.Regex

import GraphBuilder.CallGraph

/** Functions of type CallGraph => CallGraph */
object Filters {

    def wildcardsToRegex(glob: String): Regex = glob.replace("*","[^/]*").r

    /** For example,'sc*a' would match 'scala' */
    def matchesExcludedWord(excludedMethods: List[String])(str: String): Boolean =
        excludedMethods
            .map(wildcardsToRegex)
            .exists(_.matches(str))

    def exclude(excludedMethods: List[String]): CallGraph => CallGraph =
        graph => {
            graph filter graph.having(node => {
                val method = node.toOuter
                val isExcluded: String => Boolean = matchesExcludedWord(excludedMethods)
                // exclude matching methods and anything in the parents
                method match {
                    case UnknownMethod(name) => !isExcluded(name)
                    case DefinedMethod(name, parents) =>
                        val excluded = isExcluded(name) || parents.exists(isExcluded)
                        !excluded // if its not excluded, then return true so we keep this node in the graph
                }
        })
    }

    val removeIslands: CallGraph => CallGraph =
        graph => graph filter graph.having(n => {
            val p = !n.isIsolated
            if (TestHelper.shouldPrint) println(s"p=$p, n=${n.toOuter},")
            p
        })

    def inFileOnly: CallGraph => CallGraph = graph => graph filter graph.having(_.toOuter match {
        case _: DefinedMethod => true
        case _: UnknownMethod => false
    })
}

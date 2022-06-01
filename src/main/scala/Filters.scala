import scala.util.matching.Regex

import GraphBuilder.CallGraph

/** Functions of type CallGraph => CallGraph */
object Filters {

    def toRegex(glob: String): Regex = glob.replace("*","[^/]*").r

    def exclude(excludedMethods: List[String]): CallGraph => CallGraph =
        graph => {
            graph filter graph.having(node => {
            val name = node.toOuter.name
            ! excludedMethods.map(toRegex).exists(_.findFirstIn(name).isDefined)
        })
    }

    def islands(keepIslands: Boolean): CallGraph => CallGraph = {
        graph => {
            if (keepIslands)
                graph
            else
                graph filter graph.having(! _.isIsolated)
        }

    }

    def inFileOnly(): CallGraph => CallGraph = {
        graph => {
            graph filter graph.having(_.toOuter.context.isDefined)
        }
    }
}

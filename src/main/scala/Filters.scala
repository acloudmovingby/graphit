import scala.util.matching.Regex
import scala.collection.mutable.Queue
import scalax.collection.GraphTraversal.{BreadthFirst, DepthFirst}

import scala.collection.mutable
import graph.CallGraph.CallGraph
import graph.{Def, DefinedDef, UnknownDef}

object Filters {

    def wildcardsToRegex(glob: String): Regex = glob.replace("*", "[^/]*").r

    /** For example, List("sc*a")("scala") would return true because * is a wildcard, but List("scalla")("scala") would return false */
    def matches(strs: List[String])(s: String): Boolean =
        strs
            .map(wildcardsToRegex)
            .exists(_.matches(s))

    def remove(removedMethods: List[String]): Def => Boolean =
        method => {
            def isRemoved(name: String): Boolean = matches(removedMethods)(name)
            // remove methods whose names match or whose parent object/class/trait match
            method match {
                case UnknownDef(name) => !isRemoved(name)
                case DefinedDef(name, parents) =>
                    val removed = isRemoved(name) || parents.exists(isRemoved)
                    !removed // if its not removed, then return true so we keep this node in the graph
            }
        }

    def inFileOnly: Def => Boolean = {
        case _: DefinedDef => true
        case _: UnknownDef => false
    }

    // an ordering of the various filters, haven't thought that much about the order in which they take affect, but
    // it will matter (they're not commutative)
    def basicFilterSequence(
        g: CallGraph,
        removedMethods: List[String],
        excludedMethods: List[String],
        keepIslands: Boolean
    ): CallGraph =
        g.filter(Filters.remove(removedMethods))
            .transform(Transformers.exclude(excludedMethods))
            .filter(Filters.inFileOnly)
            .transform(if (keepIslands) identity else Transformers.removeIslands)
}

/**
 * Unlike Filters, these functions need access to the graph as a whole to get more context about whether to remove a
 * node or not (e.g. to see how many parents a node has, etc.)
 */
object Transformers {
    val removeIslands: CallGraph => CallGraph =
        graph => graph.filter(node => graph.childrenOf(node).nonEmpty || graph.parentsOf(node).nonEmpty)


    /** Similar to Filters.remove but removes descendants of the removed nodes as well (if they don't have non-removed ancestors) */
    def exclude(strs: List[String]): CallGraph => CallGraph =
        graph => {
            var queue = Queue.from {
                graph.nodes.filter(n => strs.contains(n.name))
            }

            var g = graph
            while (queue.nonEmpty) {
                val head = queue.dequeue()
                // we do two things: (1) remove the method that is the head of the queue,
                // then, (2) any children of that method with no other parents get added to the queue (and later will be removed as well)
                queue = queue ++ g.childrenOf(head) // children
                    .filter(g.parentsOf(_).size == 1) // that only have one parent
                g = g - head
            }

            g
        }

}

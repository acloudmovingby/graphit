import scala.util.matching.Regex
import scala.collection.mutable.{Set => MutableSet, Queue => MutableQueue}
import scalax.collection.GraphTraversal.{BreadthFirst, DepthFirst}
import GraphBuilder.{CallGraph, toDot}

import scala.collection.mutable

/** Functions of type CallGraph => CallGraph */
object Filters {

    def wildcardsToRegex(glob: String): Regex = glob.replace("*", "[^/]*").r

    /** For example, List("sc*a")("scala") would return true because * is a wildcard, but List("scalla")("scala") would return false */
    def matches(strs: List[String])(s: String): Boolean =
        strs
            .map(wildcardsToRegex)
            .exists(_.matches(s))

    def remove(removedMethods: List[String]): CallGraph => CallGraph =
        graph => graph filter graph.having { node =>
            val method = node.toOuter

            def isRemoved(name: String): Boolean = matches(removedMethods)(name)
            // remove methods whose names match or whose parent object/class/trait match
            method match {
                case UnknownMethod(name) => !isRemoved(name)
                case DefinedMethod(name, parents) =>
                    val removed = isRemoved(name) || parents.exists(isRemoved)
                    !removed // if its not removed, then return true so we keep this node in the graph
            }
        }

    // oof, don't ask me how this works, I somehow got the unit tests to pass
    def exclude2(strs: List[String]): CallGraph => CallGraph =
        graph => {
            val excluded: Seq[Method] = strs.foldLeft(MutableSet.empty[Method]) { case (set, str) =>
                val regex = wildcardsToRegex(str)
                val root = graph.nodes.find(n => regex.matches(n.toOuter.name))
                val toExclude = MutableSet.empty[Method]
                root.foreach { r =>
                    r.withKind(BreadthFirst).foreach { node =>
                        if (node.diPredecessors.forall(toExclude.contains)) toExclude.add(node)
                    }
                }
                set ++ toExclude ++ root.map(_.toOuter).map(MutableSet(_)).getOrElse(MutableSet.empty)
            }.toSeq
            graph -- excluded
        }

    def exclude(strs: List[String]): CallGraph => CallGraph =
        graph => {
            var queue = MutableQueue.from {
                strs.map(name => graph find (graph having (node = _.toOuter.name == name)))
                    .filter(_.isDefined)
                    .map(_.get.asInstanceOf[graph.NodeT].toOuter)
            }

            var newGraph = graph
            while (queue.nonEmpty) {
                val head = newGraph.get(queue.dequeue())
                queue = queue ++ head.diSuccessors // children
                    .filter(_.diPredecessors.size == 1) // that only have one parent
                    .map(_.toOuter)
                newGraph = newGraph - head.toOuter
            }
            
            newGraph
        }

    val removeIslands: CallGraph => CallGraph =
        graph => graph filter graph.having(!_.isIsolated)

    def inFileOnly: CallGraph => CallGraph =
        graph => graph filter graph.having(n => {
            n.toOuter match {
                case _: DefinedMethod => true
                case _: UnknownMethod => false
            }
        })
}

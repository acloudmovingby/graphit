import java.io.File
import scala.annotation.tailrec
import scala.meta.{Defn, Source, Term, Tree}
import scala.collection.mutable.{Map => MutableMap}

import CallGraph.CallGraph

object GraphBuilder {

    def buildGraph(files: Seq[File]): CallGraph = {
        val methods = files.map(FileParser.toSyntaxTree).flatMap(collectMethods)
        createGraph(methods)
    }

    /** As the DFS search through the tree retreats back up to the higher levels, it needs to keep of information
     * that it has found while traversing the lower levels.*/
    case class RecurContainer(methods: List[MethodWithCallsites], callsites: List[CallSite]) {
        def ++(rc: RecurContainer): RecurContainer = {
            RecurContainer(
                methods = this.methods ++ rc.methods,
                callsites = this.callsites ++ rc.callsites
            )
        }
    }

    object RecurContainer {
        val empty: RecurContainer = RecurContainer(List.empty, List.empty)
    }

    def collectMethods(tree: Source): List[MethodWithCallsites] = {
        def recur(context: Vector[String], tree: Tree): RecurContainer = {

            def recurOnChildren(context: Vector[String], children: List[Tree]): RecurContainer =
                children
                    .map(recur(context, _))
                    .fold(RecurContainer.empty)(_ ++ _)

            @tailrec
            def getCallSiteName(termApply: Term.Apply): String = termApply.fun match {
                case s: Term.Select => s.name.value // x.methodName(...)
                case n: Term.Name => n.value // methodName(...)
                case a: Term.ApplyType => a.fun match {
                    // a little silly to repeat what's above, but the Scalameta types make it awkward to avoid that
                    case s: Term.Select => s.name.value // x.methodName(...)
                    case n: Term.Name => n.value // methodName(...)
                }
                case ta: Term.Apply => getCallSiteName(ta)
                case other => {
                    println(s"hmmm... didn't expect that a Term.Apply had something else in it. The 'fun' attribute in the Term.Apply is: $other whose type is ${other.getClass}")
                    "uhhhh, what?"
                }
            }

            tree match {
                case method: Defn.Def =>
                    val recurResult = recurOnChildren(context :+ method.name.value, method.children)
                    // maybe useful later?: val position = (method.pos.start, method.pos.end)
                    val newMethod = MethodWithCallsites(
                        DefinedDef(method.name.value, context),
                        recurResult.callsites.distinct)
                    RecurContainer(recurResult.methods ++ List(newMethod), List.empty)
                case callSite: Term.Apply =>
                    val methodName = getCallSiteName(callSite)
                    RecurContainer(List.empty, List(CallSite(methodName))) ++ recurOnChildren(context, callSite.children)
                case obj: Defn.Object => recurOnChildren(context :+ obj.name.value, obj.children)
                case c: Defn.Class => recurOnChildren(context :+ c.name.value, c.children)
                case t: Defn.Trait => recurOnChildren(context :+ t.name.value, t.children)
                case _ => recurOnChildren(context, tree.children)
            }
        }

        recur(Vector.empty, tree).methods
    }

    def createGraph(methods: Seq[MethodWithCallsites]): CallGraph = {
        val immutMap = methods.map(_.method).groupBy(m => m.name)
        val nameMap: MutableMap[String, Seq[Def]] = MutableMap(immutMap.toSeq: _*)
        val edges = methods.flatMap { mc =>
            mc.callSites.flatMap { c =>
                if (!nameMap.contains(c.name)) {
                    nameMap.put(c.name, Seq(UnknownDef(c.name)))
                }
                nameMap.get(c.name).get.map((mc.method, _)) // TODO avoid using .get here
            }
        }

        val nodes: Seq[Def] = nameMap.values.toSeq.flatten

        Graph.from(nodes, edges)
    }
}

object TestHelper {
    var shouldPrint: Boolean = false
}

case class MethodWithCallsites(
    method: Def,
    callSites: List[CallSite] // calls to other methods within this method
)

case class CallSite(name: String)

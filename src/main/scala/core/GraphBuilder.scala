package core

import scala.annotation.tailrec
import scala.meta.{Defn, Source, Term, Tree}
import scala.collection.mutable.{Map => MutableMap}

import graph.CallGraph.CallGraph
import graph.{Def, DefinedDef, UnknownDef, Graph}

object GraphBuilder {

    /**
     * It's just state information being passed in and out of recursive calls. As the depth-first search retreats back up
     * the AST, it needs to keep track of information that it has found while traversing lower levels?
     * */
    case class RecurContainer(
        methods: List[DefWithCallsites],
        callsites: List[CallSite]
    ) {
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

    def collectDefs(tree: Source): List[DefWithCallsites] = collectDefs(None, tree)

    /** Recursively searches the syntax tree, looking for two main things: (1) any def declarations and (2) any
     * callsites in the subtree formed by that declaration (i.e. in this def, which other defs are called?) These
     * are put stored as a DefWithCallsites which can be further processed to build the call graph  */
    def collectDefs(fileName: Option[String], tree: Source): List[DefWithCallsites] = {
        val mentionFile = fileName.map(f => s" in $f").getOrElse("")

        def recur(context: Vector[String], tree: Tree): RecurContainer = {

            def recurOnChildren(context: Vector[String], children: List[Tree]): RecurContainer =
                children
                    .map(recur(context, _))
                    .fold(RecurContainer.empty)(_ ++ _)

            @tailrec
            def getCallSiteName(termApply: Term): String = termApply match {
                case s: Term.Select => getCallSiteName(s.name) // x.methodName(...)
                case n: Term.Name => n.value // methodName(...)
                case a: Term.ApplyType => a.fun match {
                    // a little silly to repeat what's above, but the Scalameta types make it awkward to avoid that
                    case s: Term.Select => s.name.value // x.methodName(...)
                    case n: Term.Name => n.value // methodName(...)
                    case _ => println(s"AST parsing error$mentionFile: unclear type for fun of the Term.ApplyType: type=${a.fun.getClass.getTypeName} fun=${a.fun}"); "???"
                }
                case ta: Term.Apply => getCallSiteName(ta.fun)
                case other => {
                    println(s"AST parsing error$mentionFile: unexpected term type: ${other.getClass}, term=$other")
                    ""
                }
            }

            tree match {
                case method: Defn.Def =>
                    val recurResult = recurOnChildren(context :+ method.name.value, method.children)
                    // maybe useful later?: val position = (method.pos.start, method.pos.end)
                    val newMethod = DefWithCallsites(
                        DefinedDef(method.name.value, context),
                        recurResult.callsites.distinct)
                    RecurContainer(recurResult.methods ++ List(newMethod), List.empty)
                case callSite: Term.Apply =>
                    val methodName = getCallSiteName(callSite)
                    if (methodName == "") println(s"AST parsing error$mentionFile: no name found for Term.Apply: $callSite")
                    RecurContainer(List.empty, List(CallSite(methodName))) ++ recurOnChildren(context, callSite.children)
                case obj: Defn.Object => recurOnChildren(context :+ obj.name.value, obj.children)
                case c: Defn.Class => recurOnChildren(context :+ c.name.value, c.children)
                case t: Defn.Trait => recurOnChildren(context :+ t.name.value, t.children)
                case _ => recurOnChildren(context, tree.children)
            }
        }

        recur(Vector.empty, tree).methods
    }

    def createGraph(defs: Seq[DefWithCallsites]): CallGraph = {
        val immutMap = defs.map(_.theDef).groupBy(m => m.name)
        val nameMap: MutableMap[String, Seq[Def]] = MutableMap(immutMap.toSeq: _*)
        val edges = defs.flatMap { mc =>
            mc.callSites.flatMap { c =>
                if (!nameMap.contains(c.name)) {
                    nameMap.put(c.name, Seq(UnknownDef(c.name)))
                }
                nameMap.get(c.name).get.map((mc.theDef, _)) // TODO avoid using .get here
            }
        }

        val nodes: Seq[Def] = nameMap.values.toSeq.flatten

        Graph.from(nodes, edges)
    }
}

object TestHelper {
    var shouldPrint: Boolean = false
}

case class DefWithCallsites(
    theDef: Def,
    callSites: List[CallSite] // calls to other things within this def
)

/**
 * These are examples of callsites.
 *      foo.name(bar)
 *      name(bar)
 *      foo name bar
 * @param name The name of the method/function called.
 */
case class CallSite(name: String)

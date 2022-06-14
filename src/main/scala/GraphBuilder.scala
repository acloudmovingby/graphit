import scalax.collection.Graph
import scalax.collection.GraphEdge._
import scalax.collection.GraphPredef._
import scalax.collection.io.dot._

import java.io.File
import scala.annotation.tailrec
import scala.meta.{Defn, Source, Term, Tree}
import scala.collection.mutable.{Map => MutableMap}

object GraphBuilder {

    type CallGraph = Graph[Method, DiEdge]

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
                        DefinedMethod(method.name.value, context),
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
        val nameMap: MutableMap[String, Seq[Method]] = MutableMap(immutMap.toSeq: _*)
        val edges = methods.flatMap { mc =>
            mc.callSites.flatMap { c =>
                if (!nameMap.contains(c.name)) {
                    nameMap.put(c.name, Seq(UnknownMethod(c.name)))
                }
                nameMap.get(c.name).get.map(mc.method ~> _) // TODO avoid using .get here
            }
        }

        val nodes: Seq[Method] = nameMap.values.toSeq.flatten

        Graph.from(nodes, edges)
    }

    def toDot(g: CallGraph): String = {
        val root: DotRootGraph = DotRootGraph(
            directed = true,
            id = Some(Id("Graphit"))) // TODO maybe make this the argument/filename?

        // I somehow got this to work...
        def edgeTransformer(innerEdge: Graph[Method, DiEdge]#EdgeT): Option[(DotGraph, DotEdgeStmt)] = {
            innerEdge.edge match {
                case DiEdge(src, target) => Some((root, DotEdgeStmt(NodeId(src.value.name), NodeId(target.value.name), Nil)))
            }
        }

        def nodeTransformer(innerNode: Graph[Method, DiEdge]#NodeT): Option[(DotGraph, DotNodeStmt)] =
            Some((root, DotNodeStmt(NodeId(innerNode.value.name))))

        g.toDot(root, edgeTransformer, iNodeTransformer = Some(nodeTransformer))
    }
}

object TestHelper {
    var shouldPrint: Boolean = false
}

/** EXAMPLE **
 *
 object Foo {
    def foo() = {
        def bar() = run()
        bar()
    }
 }
 *
 * *
/** In the example snippet of code above, one `Method` could be Method("bar", List("Foo", "foo")) */
case class Method(
    name: String, // name of def
    /** Name of the def/object/class/trait this def is defined. If no definition can be found for it, it is emtpy*/
    parents: Vector[String]
)*/

trait Method {
    val name: String
}

case class UnknownMethod(
    name: String
) extends Method

case class DefinedMethod(
    name: String,
    parents: Vector[String]
    // TODO: file name
) extends Method

case class MethodWithCallsites(
    method: Method,
    callSites: List[CallSite] // calls to other methods within this method
)

case class CallSite(name: String)

/*

each recursion call is happening in one of these contexts:
(1) outside of object/class (Source)
(2) inside of object/class but not yet inside a method --> returns methods with their callsites
(3) inside a basic method --> returns callsites
(4) inside a nested method
(5) inside a nested object/class

So, at each match, you do a recursive call and either:
  (a) call a different recursive function that returns the needed type
  (b) call the same recursive function and pattern match to the type
  (c) call the same recursive function and partial function directly to the type

(ignoring 4 and 5) At each recursive call, we know the type returned so
we know at the top level the recursive function returns type List[TreeElem].
We know we won't have 4 and 5 so we know at each recursive call exactly which
TreeElem is being returned and we can just build the full TreeElem easily
After collecting the top level List[ObjectClass], we do the following:
- de-dupe names (?)
- make a hashmap of name -> full method name (or list of such if there are dupes)
- add all full method names to graph as nodes
- for each Method, get all Callsites and, using the hashmap add edge between nodes

(not ignoring 4 and 5) at each recursive call, we get a list and we have to filter
that list so that we add the callsites to the method BUT what do we then return if
there was another def or object in that list? What would happen if we still
return the..

object Yo = {
  def blah() = {
    def nah() = ???
    nah()
  }
}

Yo.blah --> Yo.blah.nah

 */

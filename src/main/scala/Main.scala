import scala.meta._
import scopt.OParser
import scalax.collection.Graph
import scalax.collection.GraphPredef._
import scalax.collection.GraphEdge._
import scalax.collection.io.dot._

import sys.process._
import java.lang.ProcessBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.language.postfixOps

object Main extends App {
/*
// USE THIS TO PARSE ARGS:
  ArgParser.parse(args)

  println(s"args are ${args.mkString(",")}")

  val a: Array[String] = args

  println("What files do you want to graph?")
  val builder = OParser.builder[Config]
  val parser1 = {
    import builder._
    OParser.sequence(
      programName("scopt"),
      head("scopt", "4.x"),
      // option -f, --foo
      opt[Int]('f', "foo")
        .action((x, c) => c.copy(stuff = c.stuff + x))
        .text("foo is an integer property"),
      // more options here...
    )
  }

  OParser.parse(parser1, args, Config(0)) match {
    case Some(config) =>
      println(s"stuff is: $config")
    // do something
    case _ =>
    // arguments are bad, error message will have been displayed
  }
*/
  val file1 = "/Users/coates/Documents/code/scalameta/hello-world/src/main/scala/SimpleScalaFile.scala"
  val file2 = "/Users/coates/Documents/code/scalameta/hello-world/src/main/scala/ScalaFile.scala"
  val file3 = "/Users/coates/px/server/server/api/common/src/main/scala/com/paytronix/server/api/common/RemoteServiceProxy.scala"
  val file4 = "/Users/coates/px/server/server/api/common/src/main/scala/com/paytronix/server/api/common/ServiceLocator.scala"
  val file5 = "/Users/coates/px/server/server/misczio/src/main/scala/com/paytronix/server/misczio/effects.scala"
  val file6 = "/Users/coates/px/server/server/dataobject/src/main/scala/com/paytronix/server/dataobject/GuestWebPropertyConfiguration.scala"
  val file7 = "/Users/coates/px/server/server/service/notificationenqueuer/src/main/scala/com/paytronix/server/service/notificationenqueuer/NotificationEnqueuerServiceImpl.scala"
  val file8 = "/Users/coates/px/server/server/service/enrollment/src/main/scala/com/paytronix/server/service/enrollment/Logic.scala"

  val graph = GraphBuilder.buildGraph(args.toList)

  val dot = GraphBuilder.toDot(graph)
  println("******DOT FORMAT*****")
  println(dot)
  /*val exampleTree = FileParser.parse(args(0))

  /*exampleTree.collect {
    case app: Term.Apply => {
      defNames.find(_)
      app.fun.toString()
    }
  }*/

  //println(s"defNames:\n$defNames")
  //GraphBuilder.collectMethods(exampleTree)
  val methods = GraphBuilder.collectMethods(exampleTree)
  //println("methods found:\n\t" + methods.mkString("\n\t"))
  val graph = GraphBuilder.createGraph(methods)
  //println(s"graph: \n\t$graph")
  val dot = GraphBuilder.toDot(graph)
  println("******DOT FORMAT*****")
  println(URLEncoder.encode(dot, StandardCharsets.UTF_8.toString))
  "ls -al" !*/
}

object FileParser {
  def parse(path: String): Source = {
    val filePath = java.nio.file.Paths.get(path)
    val bytes = java.nio.file.Files.readAllBytes(filePath)
    val text = new String(bytes, "UTF-8")
    val input = Input.VirtualFile(filePath.toString, text)
    input.parse[Source].get
  }
}

object GraphBuilder {

  def buildGraph(filepaths: List[String]): Graph[String, DiEdge] = {
    val methods = filepaths.map(FileParser.parse(_))
        .flatMap(file => collectMethods(file))
    createGraph(methods)
  }

  case class RecurContainer(methods: List[Method], callsites:List[CallSite]) {
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

  def collectMethods(tree: Source): List[Method] = {
    def recur(context: Option[String], tree: Tree): RecurContainer = {

      /** I dunno why I named this flatMap but it feels like flatMap somehow */
      def flatMap(context: Option[String], children: List[Tree]): RecurContainer = children.foldLeft(RecurContainer.empty)((container: RecurContainer, child: Tree) => {
        val result = recur(context, child)
        container ++ result
      })

      def addToContext(context: Option[String], newThing: String): Option[String] =
        Some(context.map(_ + ".").getOrElse("") + newThing)

      def handleObjClassTrait(objClassTraitName: String, children: List[Tree]): RecurContainer = {
        val newContext = addToContext(context, objClassTraitName)
        flatMap(newContext, children)
      }

      def getCallSiteName(termApply: Term.Apply): String = termApply.fun match {
        case s: Term.Select => s.name.value // x.methodName(...)
        case n: Term.Name => n.value // methodName(...)
        case a: Term.ApplyType => a.fun match {
          // a little silly to repeat what's above, but the Scalameta types make it awkward to avoid that
          case s: Term.Select => s.name.value // x.methodName(...)
          case n: Term.Name => n.value // methodName(...)
        }
//          println(s"fun is: ${a.fun} and its class is ${a.fun.getClass}")
//          "so it was an ApplyType..."
        case ta: Term.Apply => getCallSiteName(ta)
        case other => {
          println(s"hmmm... didn't expect that a Term.Apply had something else in it. The fun in the Term.Apply is: $other whose type is ${other.getClass}")

          "uhhhh, what?"
        }
      }

      /** Where the magic happens */
      tree match {
        case method: Defn.Def =>
          val name = method.name.value
          val newContext = Some(context.map(_ + ".").getOrElse("") + name)
          val recurResult = flatMap(newContext, method.children)
          val fullName = context.map(_ + ".").getOrElse("") + method.name.value
          // might need this later: val position = (method.pos.start, method.pos.end)
          val newMethod = Method( method.name.value, fullName,recurResult.callsites.distinct)
          RecurContainer(recurResult.methods ++ List(newMethod), List.empty)
        case callSite: Term.Apply =>
          val methodName = getCallSiteName(callSite)
          RecurContainer(List.empty, List(new CallSite(methodName))) ++ flatMap(context, callSite.children)
        case obj :Defn.Object => handleObjClassTrait(obj.name.value, obj.children)
        case c: Defn.Class => handleObjClassTrait(c.name.value, c.children)
        case t: Defn.Trait => handleObjClassTrait(t.name.value, t.children)
        case _ => flatMap(context, tree.children)
      }
    }
    recur(None, tree).methods
  }

  def createGraph(methods: List[Method]): Graph[String, DiEdge] = {
    val nameMap: Map[String, List[String]] = methods.groupMap(_.name)(_.fullName)

    // there's duplicated code between these two choices, so later I can abstract better
    // CHOICE 1: foo -> bar
    def buildEdgesWithSimpleNames(m: Method): List[DiEdge[String]] = {
      val thisMethod = m.name
      val callsToOtherMethods: List[String] = m.callSites.map(_.methodCalled).filter(nameMap.contains(_))
      val edgesAsTuples: List[(String, String)] = callsToOtherMethods.map(otherMethod => (thisMethod, otherMethod))
      edgesAsTuples.map{ case (src, target) => src ~> target } // The "~>" symbol forms a directed edge object
    }

    // CHOICE 2: Object1.foo -> Object2.bar AND Object1.foo -> Object3.bar
    def buildEdgesWithFullName(m: Method): List[DiEdge[String]] = {
      val thisMethod: String = m.fullName
      val callsToOtherMethods: List[String] = m.callSites.map(_.methodCalled)
      // if multiple methods have that name, rather than reinventing the wheel of what a compiler does,
      // we naively assume that this method could be callying any of them (hence the flatMap below this)
      val callsAsFullNames: List[String] = callsToOtherMethods.flatMap(nameMap.get(_).getOrElse(List.empty)) // note that if a method  is not mentioned in one of the files we ignore it
      val edgesAsTuples: List[(String, String)] = callsAsFullNames.map(otherMethod => (thisMethod, otherMethod))
      edgesAsTuples.map{ case (src, target) => src ~> target } // The "~>" symbol forms a directed edge object
    }

    val nodes = methods.map(_.fullName).distinct
    val edges = methods
      .filter(!_.callSites.isEmpty) // ignore methods that don't make calls to other methods
      .flatMap(buildEdgesWithSimpleNames(_))
      .distinct
    Graph.from(nodes, edges)
  }

  def toDot(g: Graph[String, DiEdge]): String = {
    val root: DotRootGraph = DotRootGraph (
      directed = true,
      id        = Some(Id("Graphit")))
    def edgeTransformer(innerEdge: Graph[String, DiEdge]#EdgeT): Option[(DotGraph, DotEdgeStmt)] = {
      innerEdge.edge match {
        case DiEdge(src, target) => Some((root, DotEdgeStmt(NodeId(src.value), NodeId(target.value), Nil)))
      }
    }
    g.toDot(root, edgeTransformer)
  }
  def nameMap(methods: List[Method]): Map[String, String] = methods.map { m =>
    (m.name -> m.fullName)
  }.toMap
}

case class Config(
  val stuff: Int
)

case class MethodNode(
  name: String,
  position: (Int, Int),
  children: List[MethodNode]
)

trait TreeElem

case class ObjectClass(name: String, methods: List[Method]) extends TreeElem

/** fullname is built by passing a stack into recursive call */
case class Method(name: String, fullName: String, callSites: List[CallSite]) extends TreeElem

case class CallSite(methodCalled: String) extends TreeElem

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
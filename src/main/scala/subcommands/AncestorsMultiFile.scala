package subcommands


import java.io.{File, FileNotFoundException}
import scala.io.{Source => IOSource}
import scala.util.{Failure, Success, Try}
import core.{Ancestors, Files}
import core.FileParser.collectScalaFiles
import graph.CallGraph.CallGraph
import graph.{CallGraph, Def, Graph}

object AncestorsMultiFile {
    /**
     *
     * @param rootFile path to file
     * @param rootDef name of the def whose ancestor graph you want to see
     * @param dirs
     * @return .dot graph string
     */
    def run(rootFile: File, rootDef: String, dirs: Seq[String]): Try[String] =
        ancestorGraph(rootFile, rootDef, dirs)
            .map(_.toDot(s"ancestors-of-$rootDef", CallGraph.nodeLabeller))

    def ancestorGraph(rootFile: File, rootDef: String, dirs: Seq[String]): Try[CallGraph] = {


        /**
         * A calls B; B calls D; C calls D and E.
         * (The letters higher up represent functions higher in the call heierarchy)
         *
         * A
         *  \
         *   B    C
         *    \ /  \
         *     D    E
         *
         * The ancestor graph of D is the subgraph formed by nodes {A, B, C, D}
         */
        val firstFileGraph: Try[CallGraph] = for {
//            fileExists <- // Why doesn't this work???
//                if (rootFile.exists()) {
//                    Failure(new FileNotFoundException(s"Couldn't find file: $rootFile"))
//                } else Success(())
            graph <- Try(Ancestors.ancestors(core.Files(Seq(rootFile)), Seq(rootDef)))
            _ <- if (graph.isEmpty)
                    Failure(new IllegalArgumentException(s"The file didn't contain the specified def: def=$rootDef, file=$rootFile"))
                else Success(())
        } yield graph

        val allScalaFiles: Set[File] = dirs.flatMap(dirPath => collectScalaFiles(new File(dirPath))).toSet

        // search all files for new roots
        // for each file, do ancestors of it
        // take all nodes from the ancestor graphs and make those the new roots
        // combine graphs

        def findAncestorsRecursion(roots: Seq[Def], files: Seq[File], visited: Set[Def]): Try[CallGraph] = {
            (for {
                filesWithRoots: Seq[File] <- contains(roots.map(_.name), files)
                graphFromThoseFiles: CallGraph <- Try(Ancestors.ancestors(Files(filesWithRoots), roots.map(_.name)))
                newRoots = graphFromThoseFiles.nodes -- roots.toSet
            } yield {
                newRoots.diff(visited).toSeq match {
                    case Nil => Success(Graph.empty[Def]())
                    case unvisited => findAncestorsRecursion(unvisited, files, visited ++ roots).map(_ ++ graphFromThoseFiles)
                }
            }).flatten
        }

        for {
            firstGraph <- firstFileGraph
            newRoots = firstGraph.nodes
            ancestorGraph <- findAncestorsRecursion(newRoots.toSeq, allScalaFiles.toSeq, Set.empty[Def])
        } yield {
            firstGraph ++ ancestorGraph
        }

                /**
                 * TODO:
                 *  (1) use rg or some internal Java methods to find files with a name
                 *  (2) modify ancestors method so that it can take multiple roots and finds ancestors of all of them
                 *  (3) add method to combine graphs, i.e. wherever a node has the same value, those nodes are combined with edges as union of the edges of the two
                 *  (4) if we do 1, then apply 2 to each file it finds, then 3 to combine all results together
                 *  (5) add depth so we can limit how far we search
                 */

                /**
                 * Start with all ancestor words, use rg to find all file with them in it
                 * For each file, parse it and create the callsites (not the graph). Look for any callsites mentioning the words, if so select the surrounding DefinedDefs
                 * Make a new ancestor graph from those DefinedDefs
                 * Merge/attach to the old graph
                 */

    }

    def contains(strs: Seq[String], files: Seq[File]): Try[Seq[File]] = Try {
        def searchFile(str: Seq[String], file: File): Boolean = {
            val bufferedSource = IOSource.fromFile(file)
            val isInFile = bufferedSource.getLines().exists(line => strs.exists(line.contains(_)))
            bufferedSource.close()
            isInFile
        }
        files.map(f => (f, searchFile(strs, f))).filter(_._2).map(_._1)
    }
}

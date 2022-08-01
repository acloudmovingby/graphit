# graphit
A command line tool to extract and visualize call graphs from Scala source code. 

Written in Scala, for exploring large Scala codebases.

## Introduction

### Basic usage: the DOT format
Let's run `graphit` on a simple Scala file (file located [here](https://github.com/acloudmovingby/graphit/blob/main/examples/example1.scala)) as follows:
```sh
graphit example1.scala
```
This outputs:
```
digraph graphit {
	sendMessage -> sendRequest
	sendRequest -> getClient
	sendMessage -> makePolite
	sendMessage -> makeHttpRequest
}
```
This represents the call graph of that code using the [DOT format](https://en.wikipedia.org/wiki/DOT_graph_description_language). The DOT format is a standardized format to represent graph data structures. The `graphit` user can then pipe the above output to other CLI programs that rely on this format, such as [Graphviz](https://graphviz.org/). 

### Visualizing the graph
While you could do as described above, it would be *way* cooler to see it immediately! Now run graphit with the `--web` flag:
```
graphit --web example1.scala
```
Which then will open the following image in your browser:

![Visualization of the call graph of a Scala file](https://github.com/acloudmovingby/graphit/blob/main/examples/example1.png?raw=true)

Wow! This site will automatically run Graphviz for you in the browser. This site is not affilated with graphit, so be careful with using it with sensitive source code, but yeah, it's an awesome tool. ([site](https://dreampuf.github.io/GraphvizOnline/), [github](https://github.com/dreampuf/GraphvizOnline))

### Other Stuff Graphit Can Do

* You can run it on one or more `.scala` files or directories that contain `.scala` files
* You can exclude certain parts of the graph (see `--remove` and `--exclude` flags in help text). Accepts `*` as a wildcard. Useful for cleaning up large graphs, or for taking screenshots.
* Currently explores defs, but I hope to include val in the future (see TODO section)
* Ignores "nested" def's, (i.e. a def within a def), since those tend to just be helper methods. Maybe a flag could be added to turn that off, but I honestly didn't find it useful.
* Run with `--help` to see more!


## How to Install / Use:

### Option 1: sbt shell
If you're familiar with sbt, this should be pretty straightforward:
1. clone repo
2. start sbt shell by running `sbt`
3. Enter `run` followed by the args/flags
4. Enter `test` to run the test suite.

### Option 2: single command
1. clone repo
2. Build all dependencies and bundle with program by running the command `sbt assembly`
3. go into `target/scala-2.13 directory` and get then absolute path leading to the assembly .jar (maybe something like `graphit-assembly-*.*.jar`)
4. Then make an alias in your .bashrc/.zshrc using the path to the assembly .jar:
      ```alias graphit='(){ java -jar /path/to/graphit-assembly-1.0.jar $1 ;}'```
5. Then you can run the program with simply the word `graphit` as shown in the examples above.
(Note if you make changes to the repo, you'd have re-run sbt assembly, so it's recommended to use the sbt shell above if you're editing the repo)
  
## Development

### Motivation

*acloudmovingby*: `graphit` was born in the first year of my career as a software engineer where I was fortunate enough to be working full-time with Scala. As blessed an existence as that was, real world production code--even code written in Scala--can get a bit *narsty*. I had to spend a lot of time trying to figure out how files worked, how they talked to each other, how things could get triggered...From the beginning. I've been interested in adding features that help in comprehending large Scala codebases. 

If you'd like to contribute (and please do!), **it will help to be using `graphit` while working on other Scala projects**, especially new ones that you're just trying to figure out*

### Third party tools
These are some great tools that help graphit work: 
* [Scopt](https://github.com/scopt/scopt) to parse the arguments/flags
* [Scalameta](https://scalameta.org/docs/trees/guide.html) to parse Scala code into abstract syntax trees (ASTs).
* [Graphs for Scala](https://www.scala-graph.org/) (i.e. scalax) to handle graph operations. I (@acloudmovingby) used graph libraries in Rust, Java, and Scala, and I think this is my favorite so far. That being said, like all graph libraries, it's API is quite complex because graphs are very general mathematical structures. I had a lot of boilerplate scalax code in graphit and I knew it might discourage others/myself from contributing, so I made a light wrapper Graph class around the scalax library. Might discard the scalax library entirely in the future.

### POSSIBLE TO-DO:

In no particular priority: 
* Don't just look at `def`'s but also at `val`'s. 
* (HARD) make into an IDE plugin, maybe with a GUI?
* Make this tool available via apt-get, homebrew, etc.?
* Make flag to show all method/functions in graph, including those whose definitions were never found in the files. (It's very verbose to do this because it would show all the .map and .toString and such, but it might be useful)
* Make flag to show class names with methods. Surprisingly, even though people mention this a lot, I have not found it useful since it's usually not hard to figure out what method a node is referring to. Adding the full classname clutters the image, but people might want it...
* Make flag to find path(s) between two methods.
* Make flag to show all paths between two files.
* Make flag to show all parent callers, up to a certain depth or something so it's reasonable to run on a huge codebase.
* Make flag to show all descendants, up to a certain depth.
* Make output title of graph be file or directory names?
* Improve efficiency when looking at large codebases by reading files and running algorithms in a more `online` way, i.e. not waiting to read all files and build the whole graph if a given flag command doesn't need it (???)
* (MEDIUM) Make flag to group methods by their classes and/or files. In DOT format terms, I'd probably use the `subgraph` keyword to group methods that are all in the same class/object/trait. The trick is getting this to work
* **(HARD but arguably most important)** Make this actually use a compiler to check types rather than just using reflection and string equality (currently graphit cannot know the difference between two methods with the same name, e.g. it will think `ObjectA.foo()` is calling the same method as `ObjectB.foo()`. Heard that we'd possibly need to write my own compiler plugin. 



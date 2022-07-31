# graphit
Command line tool to help analyze call graphs in Scala files.

## Basic Usage
### Your first call graph using the DOT Format
Let's run `graphit` on a simple Scala file (see the file itself [here](https://github.com/acloudmovingby/graphit/blob/main/examples/example1.scala)
```sh
graphit /examples/example1.scala
```
Which outputs:
```
digraph graphit {
	sendMessage -> sendRequest
	sendRequest -> getClient
	sendMessage -> makePolite
	sendMessage -> makeHttpRequest
}
```
This is a representation of the call graph of that Scala code using the [DOT format](https://en.wikipedia.org/wiki/DOT_(graph_description_language). You can then pipe this output to other programs such as the CLI for [Graphviz](https://graphviz.org/). 

### Visualizing the graph
While you could pipe the output to another program, it would be *way* cooler to see it immediately! Let's now run graphit with the `--web` flag:
```
graphit --web /examples/example1.scala
```
Which then will open the following image n your browser:

![Visualization of the call graph of a Scala file](https://github.com/acloudmovingby/graphit/blob/main/examples/example1.png?raw=true)

Wow! Note, the GraphvizOnline tool is not affiliated with graphit, but because it's so useful, it's been bundled into graphit (please give thanks to the people over there maintaining that site!)

### Other Things You Can Do

* You can run it on any .scala file or directories that contain .scala files
* You can exclude certain parts of the graph if it gets cluttered (see --remove and --exclude flags)
* Currently explores defs, but I hope to include val in the future (see TODO section)
* Ignores "nested" def's, (i.e. a def within a def), since those tend to just be helper methods
* Run with `--help` to learn more


## How to Install / Use:
1. clone repo
2. Option 1: use sbt (must have sbt shell running)
    - `cd` into repo
    - run the command `sbt`
    - Once in sbt shell, you can run graphit by doing `run ...`
3. Option 2: make an alias (you can run with a single command)
    - `cd` into repo
    - Build all dependencies and bundle with program by doing `sbt assembly`
    - go into target/scala-2.13 directory and get then absolute path leading to the assembly .jar (maybe something like `graphit-assembly-*.*.jar`)
    - then make an alias in your .bashrc/.zshrc using the path to the assembly .jar
      ```alias graphit='(){ java -jar /Users/coates/Documents/code/graphit/graphit/target/scala-2.13/graphit-assembly-1.0.jar $1 ;}'```
    - run the program simply using the command `graphit <arg1>, <arg2>....`
  
## Development
### Third party tools
These are some of the awesome tools that help graphit work: 
* [Scopt] to parse arguments
* [Scalameta] to parse Scala code into abstract syntax trees (ASTs).
* [Graphs for Scala] (i.e. scalax) to handle graph operations. I (Chris) have used graph libraries in Rust, Java, and Scala, and this one is the best! That being said, like all graph libraries, it's API is quite complex because graphs are very general mathematical structures. I had a lot of boilerplate scalax code in graphit and I knew it might discourage others/myself from contributing, so I made a light wrapper Graph class around the scalax library, and I may discard the scalax library entirely in the future.

### POSSIBLE TO-DO:
In no particular priority: 
* Don't just look at `def`'s but also at `val`'s. 
* Make this tool available via apt-get, homebrew, etc.
* Make flag to show all method/functions in graph, including those whose definitions were never found in the files. (It's very verbose to do this because it would show all the .map and .toString and such, but it might be useful)
* Make flag to show class names with methods
* Make flag to find path(s) between two methods.
* Make flag to show all paths between two files.
* Make flag to show all parent callers, up to a certain depth or something so it's reasonable to run on a huge codebase.
* Make flag to show all descendants, up to a certain depth.
* Improve efficiency when looking at large codebases by reading files and running algorithms in a more `online` way, i.e. not waiting to read all files and build the whole graph if a given flag command doesn't need it (???)
* (HARD) Make flag to group methods by their classes and/or files. In DOT format terms, I'd probably use the `subgraph` keyword to group methods that are all in the same class/object/trait. The trick is getting this to work ;) 
* (HARD) Make this actually use a compiler to check types rather than just using reflection and string equality (currently graphit cannot know the diffrence between two methods with the same name, e.g. it will think `ObjectA.foo()` is calling the same method as `ObjectB.foo()`. Obviously people have devised ways to avoid this, but in discussion with others it appears I may need to write my own compiler plugin. 

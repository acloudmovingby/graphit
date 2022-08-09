# graphit
A command line tool to extract and visualize call graphs from Scala source code. 

Written in Scala, for exploring large Scala codebases.

## Introduction

### Basic usage: the DOT format
Let's say we've set up `graphit` on the command line (instructions below) and we run `graphit` on the Scala file `example1.scala` as follows:
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
Each arrow (`A -> B`) indicates that within the code block of function/method `A`, there is a call to function/method `B`. If you look at the example file [here](https://github.com/acloudmovingby/graphit/blob/main/examples/example1.scala), you'll see that the method `sendMessage` makes a call to the method `sendRequest`, which in turn makes a call to `getClient` and so on. 

This so-called 'call graph' of the code, is represented in the [DOT format](https://en.wikipedia.org/wiki/DOT_graph_description_language), a standardized way to represent graph data structures. The `graphit` user can then pipe the above output to other CLI programs that rely on this format, such as [Graphviz](https://graphviz.org/). 

### Visualizing the graph
But wait...seeing a graph as text still feels a little unintuitive. It would be *way* cooler to see it visually (and immediately)! So let's run graphit with the `--web` flag:
```
graphit --web example1.scala
```
This then opens the following image in your browser:

![Visualization of the call graph of a Scala file](https://github.com/acloudmovingby/graphit/blob/main/examples/example1.png?raw=true)

Neat! What graphit is doing here is sending your graph information to an open-source site called GraphvizOnline ([site](https://dreampuf.github.io/GraphvizOnline/), [github](https://github.com/dreampuf/GraphvizOnline)). The site automatically runs the Graphviz CLI program mentioned above, but does it in the browser.

Note: Because this site is not affilated with graphit, be mindful of using it with confidential source code. To be honest, though, it's highly unlikely anyone is using that site to skim production code call graphs from Scala programmers...

### Other Stuff Graphit Can Do

* You can run it on one or more `.scala` files or directories that contain `.scala` files
* You can exclude certain parts of the graph (see `--remove` and `--exclude` flags in help text). Accepts `*` as a wildcard. Useful for cleaning up large graphs, or for taking screenshots.
* Currently explores defs, but I hope to include val in the future (see TODO section)
* Ignores "nested" def's, (i.e. a def within a def), since those tend to just be helper methods. Maybe a flag could be added to turn that off, but I honestly didn't find it useful.
* Run with `--help` to see more!


## How to Install / Use:
You need to have sbt installed.

### Option 1: sbt shell
This is probably the best way (meaning, it will work), but it's a little more clunky. If you're familiar with sbt, this should be pretty straightforward:
1. clone repo
2. start sbt shell by running `sbt`
3. Enter `run` followed by the args/flags
4. Enter `test` to run the test suite.

### Option 2: single command
If you do this, you can run graphit as shown in the examples above with just the command 'graphit'
1. clone repo; cd into it
2. Build all dependencies and bundle with program by running the command `sbt assembly`
3. go into `target/scala-2.13 directory` and get then absolute path leading to the assembly .jar (maybe something like `graphit-assembly-*.*.jar`)
4. Then make an alias in your .bashrc/.zshrc using the path to the assembly .jar as follows:
      ```alias graphit='(){ java -jar /path/to/graphit-assembly-1.0.jar $1 ;}'```
5. Then you can run the program with simply the word `graphit` as shown in the examples above.
(Note if you make changes to the repo, you'd have to re-run `sbt assembly`, so it's easier to use the sbt shell above if you're working on edits to the repo)
  
## Development

CONTRIBUTORS WELCOME

For any comments/suggestions, feel free to open an issue or send a message to Github user *acloudmovingby*

If you'd like to contribute, **it will help to be using `graphit` while working on other Scala projects**, especially new ones that you're just trying to figure out*

### POSSIBLE TO-DO:
This is just a random mix of ideas, probably shouldn't / won't do a lot of them: 
* Don't just look at `def`'s but also at `val`'s. 
* (HARD) make into an IDE plugin, maybe with a GUI?
* Make this tool available via apt-get, homebrew, etc.?
* Make flag to show all method/functions in graph, including those whose definitions were never found in the files. (It's very verbose to do this because it would show all the .map and .toString and such, but it might be useful)
* Make flag to show class names with methods. Surprisingly, even though people mention this a lot, I have not found it useful since it's usually not hard to figure out what method a node is referring to. Adding the full classname clutters the image, but people might want it...
* Make flag to find path(s) between two methods.
* Make flag to show all paths between two files.
* Make flag to show all *parent* callers, up to a certain depth or something so it's reasonable to run on a huge codebase.
* Same as the previous bullet point, but make flag to show all *descendants*, up to a certain depth.
* Make output title of graph be file or directory names?
* Improve efficiency when looking at large codebases by reading files and running algorithms in a more `online` way, i.e. not waiting to read all files and build the whole graph if a given flag command doesn't need it (???)
* (HARD) Make flag to group methods by their classes and/or files. In DOT format terms, I'd probably use the `subgraph` keyword to group methods that are all in the same class/object/trait. The trick is getting this to work
* (MEDIUM) Same as previous bullet point, but instead of grouping all methods from a class in one box, I think it's easier to just color them. Perhaps different hues represent different files and shades of that hue represent different classes within that file??
* **(HARD but arguably most important)** Make this actually use a compiler to check types rather than just using reflection and string equality (currently graphit cannot know the difference between two methods with the same name, e.g. it will think `ObjectA.foo()` is calling the same method as `ObjectB.foo()`. Heard that we'd possibly need to write my own compiler plugin. 

### Motivation
This project was started by *acloudmovingby* who is a software engineer working full-time with Scala. As blessed an existence as that is, real world production code--even in Scala--can get darn complicated. The hope for this tool is spend less time trying to figure out how files worked, how they talk to each other, how things can get triggered, etc.


### Third party tools
These are some great tools that help graphit work: 
* [Scopt](https://github.com/scopt/scopt) to parse the arguments/flags
* [Scalameta](https://scalameta.org/docs/trees/guide.html) to parse Scala code into abstract syntax trees (ASTs).
* [Graphs for Scala](https://www.scala-graph.org/) (i.e. scalax) to handle graph operations. I (@acloudmovingby) used graph libraries in Rust, Java, and Scala, and I think this is my favorite so far. That being said, like all graph libraries, it's API is quite complex because graphs are very general mathematical structures. I had a lot of boilerplate scalax code in graphit and I knew it might discourage others/myself from contributing, so I made a light wrapper Graph class around the scalax library. Might discard the scalax library entirely in the future.



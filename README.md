# graphit
Command line tool to help analyze call graphs in Scala files.

Here is how you can run `graphit` on a Scala file:
```sh
graphit --web /examples/example1.scala
```
This will then open your web browser and display this! 

![alt text](https://https://github.com/acloudmovingby/graphit/blob/main/examples/example1.png?raw=true)

This image is in fact the call graph of that Scala code! (See the Scala file [here](https://https://github.com/acloudmovingby/graphit/blob/main/examples/example1.png?raw=true))

The online tool that it opens, GraphvizOnline, is unaffiliated with graphit. If you run without the `--web` flag, it outputs the graph in standard DOT format, which you can then pipe to GraphViz on the command line or use in other software accepting that format.

And that's graphit in a nutshell! Some other facts that didn't fit in the nutshell:
* You can run it on any .scala file or directories that contain .scala files
* You can exclude certain parts of the graph if it gets cluttered (see --remove and --exclude flags)
* Currently explores defs, but I hope to include val in the future (see TODO section)
* Ignores "nested" def's, (i.e. a def within a def), since those tend to just be helper methods
* Run with `--help` to learn more


# How to Install / Use:
1. clone repo
2. Option 1: use sbt (must have sbt shell running)
    - `cd` into repo
    - run the command `sbt`
    - Once in sbt shell, you can run graphit by doing `run <flag>... <arg>...` (see below on the many other things you can do)
3. Option 2: make an alias (you can run with a single command)
    - `cd` into repo
    - Build all dependencies and bundle with program by doing `sbt assembly`
    - go into target/scala-2.13 directory and get then absolute path leading to the assembly .jar (maybe something like `graphit-assembly-*.*.jar`)
    - then make an alias in your .bashrc/.zshrc using the path to the assembly .jar
    - `alias graphit='(){ java -jar /Users/coates/Documents/code/graphit/graphit/target/scala-2.13/graphit-assembly-1.0.jar $1 ;}'`
    - run the program simply using the command `graphit`:
        - `graphit <flag>... <arg>...`
  
# POSSIBLE TODO:
In no particular priority: 
* Don't just look at `def`'s but also at `val`'s. 
* Make flag to show all method/functions in graph, including those whose definitions were never found in the files. (It's very verbose to do this because it would show all the .map and .toString and such, but it might be useful)
* Make flag to show class names with methods
* Make flag to find path(s) between two methods.
* Make flag to show all paths between two files.
* Make flag to show all parent callers, up to a certain depth or something so it's reasonable to run on a huge codebase.
* Make flag to show all descendants, up to a certain depth.
* Improve efficiency when looking at large codebases by reading files and running algorithms in a more `online` way, i.e. not waiting to read all files and build the whole graph if a given flag command doesn't need it (???)
* (HARD) Make flag to group methods by their classes and/or files. In DOT format terms, I'd probably use the `subgraph` keyword to group methods that are all in the same class/object/trait. The trick is getting this to work ;) 
* (VERY HARD) Make this actually use a compiler to check types rather than just using reflection and string equality (currently graphit cannot know the diffrence between two methods with the same name, e.g. it will think `ObjectA.foo()` is calling the same method as `ObjectB.foo()`
* Make 

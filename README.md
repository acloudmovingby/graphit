# graphit
Command line tool to help analyze call graphs in Scala files.

# Installation/Setup
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
  
  
# Basic Use
  
Most important: 
1. args --> args are either .scala files or directories that contain .scala files
2. -w flag --> visualizes this graph using an open-source Graphviz website

# Examples

These examples analyze the Schedule.scala file in the open-source scala Zio library. Let's get started!

First, let's just look at the whole file:
```
graphit -w /absolute/path/to/Schedule.scala
```


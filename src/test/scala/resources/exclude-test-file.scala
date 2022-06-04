package resources

class ClassAAA {
    def def1(): Unit = ()
}

object ClassAAA {
    def def2(): Unit = ObjectAAA.def3()

}

object ObjectAAA {
    def def3(): Unit = ()

}

trait TraitAAA {
    def def4(): Unit = ()
}

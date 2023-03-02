package core

import java.net.URLEncoder
import sys.process._

object Web {
    def open(dot: String): Unit = {
        val encoded = URLEncoder.encode(dot,"UTF-8").replace("+", "%20").replace("+", "%20")
        val url = s"https://dreampuf.github.io/GraphvizOnline/#$encoded"
        val bash = """open """ + url
        bash.! // runs bash command
    }
}
import org.scalatest.flatspec.AnyFlatSpec
import java.io.File

import core.FileParser.collectScalaFiles

class FileUtilTests extends AnyFlatSpec {

    behavior of "collectScalaFiles"

    it should "find 3 scala files in has-scala-files directory" in {
        val result = collectScalaFiles(new File("./src/test/resources/file-util-tests/has-scala-files"))
        assert(result.size == 3)
    }

    it should "find 0 scala files in the no-scala-files directory" in {
        val result = collectScalaFiles(new File("./src/test/resources/file-util-tests/no-scala-files"))
        assert(result.size == 0)
    }
}

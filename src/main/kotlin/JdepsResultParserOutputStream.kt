import java.io.OutputStream
import java.io.PrintWriter
import java.util.regex.Pattern
import com.sun.tools.jdeps.Main
import net.lingala.zip4j.core.ZipFile
import org.apache.commons.io.FileUtils
import java.io.Closeable
import java.io.File

class JdepsResultParserOutputStream : OutputStream() {
    private val line = StringBuilder()
    private val parser = JdepsLineParser()
    val result = JdepsResult()

    override fun write(character: Int) {

        val char = character.toChar()

        if (char == '\n') {
            add(line)
            line.clear()
        } else {
            line.append(char)
        }
    }

    private fun add(line: StringBuilder) {
        val classDependency = parser.parse(line.toString())

        if (classDependency != null) {
            result.add(classDependency.first, classDependency.second)
        }
    }

}

class JdepsLineParser {
    private val patternClassDependency: Pattern = Pattern.compile("\\s+(\\S+)\\s+->\\s+(\\S+)\\s+not found\\s*")
    private val patternClass: Pattern = Pattern.compile("\\s+(\\S+)\\s+->\\s+(\\S+)\\s+\\s*")

    fun parse(line: String): Pair<String, String?>? {
        var matcher = patternClassDependency.matcher(line)

        if (matcher.matches()) {
            return Pair(matcher.group(1), matcher.group(2))
        }

        matcher = patternClass.matcher(line)
        if (matcher.matches()) {
            return Pair(matcher.group(1), null)
        }

        return null
    }
}

class JdepsResult(val resultMap: MutableMap<String, MutableSet<String>> = mutableMapOf()) {


    fun add(className: String, dependendClassName: String?) {
        var classDependencies = resultMap[className]

        if (classDependencies == null) {
            classDependencies = HashSet()
            resultMap[className] = classDependencies
        }
        if (dependendClassName != null) {
            classDependencies.add(dependendClassName)
        }
    }
}

object AnalyseJavaDependencies
{
    fun read(jarFile: File): JdepsResult {
        val resultParserOutputStream = JdepsResultParserOutputStream()
        val printWriter = PrintWriter(resultParserOutputStream)

        Main.run(
            arrayOf(
                "-version",
                "-v",
                "-filter:none",
                jarFile.absolutePath
            )
            , printWriter
        )

        println("lib ${jarFile.name} dependencies: ${resultParserOutputStream.result.resultMap.size}")
        return resultParserOutputStream.result
    }
}

class Expander(private val workdir: String, private val zipFile: File) : Closeable {
    private val tempDir = File(workdir, zipFile.name)

    fun expand(): File {
        tempDir.mkdirs()
        tempDir.deleteOnExit()
        ZipFile(zipFile).extractAll(tempDir.absolutePath)

        return tempDir
    }

    override fun close() {
        FileUtils.deleteDirectory(tempDir)
    }
}
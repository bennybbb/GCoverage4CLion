package gcov.data

import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.cpp.toolchains.CPPToolSet
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import gcov.notification.GCovNotification
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class CoverageThreadHelper {

    fun generateGCDAFor(gcda: File, project: Project, hashSet: HashSet<List<String>>, toolchain1: CPPToolchains.Toolchain, path: String?): Boolean {
        try {
            var lines = mutableListOf<String>()
            val nullFile = if (System.getProperty("os.name").startsWith("Windows")) {
                "NUL"
            } else {
                "/dev/zero"
            }
            if (path == null) {
                val notification = GCovNotification.GROUP_DISPLAY_ID_INFO
                        .createNotification("No GCov specified for toolchain $toolchain1", NotificationType.ERROR)
                Notifications.Bus.notify(notification, project)
                return false
            }
            val builder = if (toolchain1.toolSetKind == CPPToolSet.Kind.WSL) {
                ProcessBuilder(toolchain1.toolSetPath, "run", path, "-i", "-m", "-b", gcda.name.toString())
            } else {
                ProcessBuilder(path, "-i", "-m", "-b", gcda.name.toString())
            }.run {
                directory(gcda.parentFile)
                redirectOutput(File(nullFile))
                redirectErrorStream(false)
            }
            val p = builder.start()
            val reader = BufferedReader(InputStreamReader(p.errorStream))
            do {
                val line = reader.readLine() ?: break
                lines.add(line)
            } while (true)
            val retCode = p.waitFor()
            if (retCode != 0) {
                CoverageThreadHelper.log.warn("\"gcov\" returned with error code $retCode")
            }
            if (lines.isNotEmpty()) {
                lines = lines.map {
                    val pathConverted = it.replace('\\', '/')
                    pathConverted.substring(1 + pathConverted.indexOf(':', pathConverted.indexOf('/')))
                }.toMutableList()
                if (hashSet.add(lines)) {
                    val notification = GCovNotification.GROUP_DISPLAY_ID_INFO
                            .createNotification("gcov returned following warning:\n" + lines.joinToString("\n"), NotificationType.WARNING)
                    Notifications.Bus.notify(notification, project)
                }
            }
        } catch (e: IOException) {
            val notification = GCovNotification.GROUP_DISPLAY_ID_INFO
                    .createNotification("\"gcov\" was not found in system path", NotificationType.ERROR)
            Notifications.Bus.notify(notification, project)
        } catch (e: InterruptedException) {
            val notification = GCovNotification.GROUP_DISPLAY_ID_INFO
                    .createNotification("Process timed out", NotificationType.ERROR)
            Notifications.Bus.notify(notification, project)
        }
        return true
    }

    companion object {

        private val log = Logger.getInstance(CoverageThreadHelper::class.java)
    }
}

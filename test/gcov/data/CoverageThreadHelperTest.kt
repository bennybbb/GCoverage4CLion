package gcov.data

import com.intellij.mock.MockApplicationEx
import com.intellij.mock.MockProjectEx
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.Disposer
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cidr.toolchains.OSType
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse


class CoverageThreadHelperTest {
    @Test
    fun generateGCDAFor_WhenInvalidPath_ThenStopExecution() {

        Extensions.registerAreaClass("IDEA_PROJECT", null)
        val mockDisposable = Disposer.newDisposable()

        val application = MockApplicationEx(mockDisposable)
        ApplicationManager.setApplication(application, mockDisposable)

        val project = MockProjectEx(mockDisposable)

        assertFalse(CoverageThreadHelper().generateGCDAFor(File(""), project, HashSet<List<String>>(), CPPToolchains.Toolchain(OSType.MAC), null))
    }

    @Test
    fun foo() {

        System.setProperty("os.name", "Windows")
        assertEquals("Windows", System.getProperty("os.name"))
    }

}

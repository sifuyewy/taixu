package top.wkbin.taixu.iteration.engine

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 纯 JVM 单测：校验自举引擎的路径推导与常量契约，不依赖 Android Context。
 */
class CustomIterationBootstrapPathsTest {

    private val home = File("/root")

    @Test
    fun `workspace path uses isolated custom_taixu directory`() {
        val paths = CustomIterationBootstrap.resolvePaths(home)
        assertEquals("/root/${CustomIterationBootstrap.WORKSPACE_NAME}", paths.workspace.absolutePath)
        assertEquals("custom_taixu", CustomIterationBootstrap.WORKSPACE_NAME)
    }

    @Test
    fun `skill is deployed under taixu skills directory`() {
        val paths = CustomIterationBootstrap.resolvePaths(home)
        assertEquals("/root/.taixu/skills/taixu-custom-iteration/SKILL.md", paths.skill.absolutePath)
    }

    @Test
    fun `workflow is cached and preset into repository path`() {
        val paths = CustomIterationBootstrap.resolvePaths(home)
        assertEquals(
            "/root/.taixu/templates/workflows/taixudev-build.yml",
            paths.workflowCache.absolutePath,
        )
        assertEquals(
            "/root/custom_taixu/.github/workflows/taixudev-build.yml",
            paths.workflowInRepo.absolutePath,
        )
    }

    @Test
    fun `bootstrap prompt pins official repo and device-flow security rule`() {
        val prompt = CustomIterationBootstrap.BOOTSTRAP_PROMPT
        assertTrue(prompt.contains(CustomIterationBootstrap.OFFICIAL_REPO))
        assertTrue(prompt.contains("gh auth login"))
        assertTrue(prompt.contains("top.wkbin.taixu.dev"))
        assertTrue(prompt.contains("taixudev-build.yml"))
    }
}

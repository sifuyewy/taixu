package top.wkbin.taixu.iteration.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 纯 JVM 单测：校验 CI 调度指令模板与产物契约常量。
 */
class TaiXuDevBuildCoordinatorTest {

    @Test
    fun `workflow and artifact names stay stable`() {
        assertEquals("taixudev-build.yml", TaiXuDevBuildCoordinator.WORKFLOW_NAME)
        assertEquals("taixudev-apk", TaiXuDevBuildCoordinator.ARTIFACT_NAME)
    }

    @Test
    fun `dev build keeps separate application id`() {
        assertEquals("top.wkbin.taixu.dev", TaiXuDevBuildCoordinator.DEV_APPLICATION_ID)
        assertEquals("TaiXuDev", TaiXuDevBuildCoordinator.DEV_APP_LABEL)
    }

    @Test
    fun `trigger command targets workflow on given ref`() {
        val cmd = TaiXuDevBuildCoordinator.CliCommands.triggerWorkflow(branch = "feature/custom_iteration")
        assertTrue(cmd.startsWith("gh workflow run taixudev-build.yml"))
        assertTrue(cmd.contains("--ref feature/custom_iteration"))
    }

    @Test
    fun `watch and download commands pin run id and artifact`() {
        val runId = "1234567890"
        assertTrue(TaiXuDevBuildCoordinator.CliCommands.watchWorkflow(runId).contains(runId))
        val download = TaiXuDevBuildCoordinator.CliCommands.downloadArtifact(runId)
        assertTrue(download.contains(runId))
        assertTrue(download.contains(TaiXuDevBuildCoordinator.ARTIFACT_NAME))
    }

    @Test
    fun `verification commands cover package name and digest`() {
        val apk = "/storage/emulated/0/Download/TaiXuDev-arm64-debug.apk"
        assertTrue(TaiXuDevBuildCoordinator.CliCommands.verifyApkPackage(apk).contains(apk))
        assertTrue(TaiXuDevBuildCoordinator.CliCommands.verifyApkDigest(apk).contains(apk))
    }

    @Test
    fun `build status machine avoids legacy typo`() {
        val statuses: List<TaiXuDevBuildCoordinator.BuildStatus> = listOf(
            TaiXuDevBuildCoordinator.BuildStatus.Idle,
            TaiXuDevBuildCoordinator.BuildStatus.Dispatching("main"),
            TaiXuDevBuildCoordinator.BuildStatus.Running("1", "queued"),
            TaiXuDevBuildCoordinator.BuildStatus.Downloading("1", 50),
            TaiXuDevBuildCoordinator.BuildStatus.Success("/tmp/a.apk", "deadbeef"),
            TaiXuDevBuildCoordinator.BuildStatus.Failed("boom"),
        )
        assertEquals(6, statuses.size)
        assertTrue(statuses[1] is TaiXuDevBuildCoordinator.BuildStatus.Dispatching)
    }
}

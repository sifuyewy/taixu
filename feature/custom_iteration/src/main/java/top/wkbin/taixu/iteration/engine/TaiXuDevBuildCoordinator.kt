package top.wkbin.taixu.iteration.engine

/**
 * TaiXuDevBuildCoordinator — 调度 GitHub Actions 云端构建与本地产物校验。
 *
 * 本对象只负责生成 gh CLI 指令与描述构建状态机；真正的进程执行交给终端 / Harness 侧，
 * 避免 UI 层直接持有 shell 会话，也保证手机端不需要安装 Android SDK/NDK。
 */
object TaiXuDevBuildCoordinator {

    /** CI 工作流文件名（位于 .github/workflows/）。 */
    const val WORKFLOW_NAME = "taixudev-build.yml"

    /** workflow 中上传的 artifact 名称，下载时必须与之完全一致。 */
    const val ARTIFACT_NAME = "taixudev-apk"

    /** 产物 APK 的期望包名，用于校验双包共存是否生效。 */
    const val DEV_APPLICATION_ID = "top.wkbin.taixu.dev"

    /** 产物 APK 的期望应用名。 */
    const val DEV_APP_LABEL = "TaiXuDev"

    /** 手机端默认下载目录。 */
    const val DEFAULT_DOWNLOAD_DIR = "/storage/emulated/0/Download"

    sealed interface BuildStatus {
        data object Idle : BuildStatus
        data class Dispatching(val branch: String) : BuildStatus
        data class Running(val runId: String, val statusText: String) : BuildStatus
        data class Downloading(val runId: String, val progress: Int) : BuildStatus
        data class Success(val apkPath: String, val sha256: String) : BuildStatus
        data class Failed(val error: String, val logs: String? = null) : BuildStatus
    }

    /**
     * 构建相关的核心 CLI 指令模板。
     */
    object CliCommands {
        fun triggerWorkflow(
            branch: String = "main",
            workflowName: String = WORKFLOW_NAME,
        ): String = "gh workflow run $workflowName --ref $branch"

        fun watchWorkflow(runId: String): String =
            "gh run watch $runId --exit-status"

        fun downloadArtifact(
            runId: String,
            outputDir: String = DEFAULT_DOWNLOAD_DIR,
        ): String = "gh run download $runId -n $ARTIFACT_NAME -D $outputDir"

        fun checkFailedLog(runId: String): String =
            "gh run view $runId --log-failed"

        /** 安装前校验 APK 实际包名，确认是 top.wkbin.taixu.dev 而非正式包。 */
        fun verifyApkPackage(apkPath: String): String =
            "aapt dump badging $apkPath | head -n 1"

        /** 与 CI 产出的 .sha256 文件比对，确认下载未被截断或篡改。 */
        fun verifyApkDigest(apkPath: String): String =
            "sha256sum $apkPath"
    }
}

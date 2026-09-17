package top.wkbin.taixu.iteration.engine

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * CustomIterationBootstrap — 太墟自定义迭代环境自举引擎。
 *
 * 职责：
 * 1. 准备沙盒内隔离的源码工作区 `~/custom_taixu`；
 * 2. 将预置的 `taixu-custom-iteration` Skill 真实落盘到 Agent 技能目录；
 * 3. 将 GitHub Actions 工作流模板落盘到沙盒缓存目录，并预置到工作区仓库路径；
 * 4. 生成引导 Agent 执行自迭代开发的规范提示词。
 *
 * 说明：本引擎只做文件部署，不启动任何网络或构建行为；APK 构建统一交给 GitHub Actions。
 */
object CustomIterationBootstrap {

    const val WORKSPACE_NAME = "custom_taixu"
    const val OFFICIAL_REPO = "https://github.com/wkbin/taixu"

    /** 预置 Skill 在 APK assets 中的路径。 */
    const val SKILL_ASSET_PATH = "skills/taixu-custom-iteration/SKILL.md"

    /** 预置 GitHub Actions 工作流模板在 APK assets 中的路径。 */
    const val WORKFLOW_ASSET_PATH = "templates/workflows/taixudev-build.yml"

    /** Skill 在沙盒家目录下的落盘相对路径。 */
    const val SKILL_DEPLOY_RELATIVE = ".taixu/skills/taixu-custom-iteration/SKILL.md"

    /** 工作流模板在沙盒家目录下的缓存相对路径。 */
    const val WORKFLOW_CACHE_RELATIVE = ".taixu/templates/workflows/taixudev-build.yml"

    /** 工作流在克隆后的仓库内的相对路径。 */
    const val WORKFLOW_REPO_RELATIVE = ".github/workflows/taixudev-build.yml"

    const val BOOTSTRAP_PROMPT = """我准备在太墟（TaiXu）的手机 Linux 虚拟沙盒中进行 TaiXu 自定义迭代。

请按以下步骤引导我：
1. 检查本地开发环境：
   - 确认当前命令在 Linux PRoot 沙盒中执行；
   - 确认独立工作区为 ~/custom_taixu；
   - 优先使用 GitHub Actions 构建 TaiXuDev APK，无需在手机本地安装庞大的 Android SDK/NDK。

2. 检查并配置 GitHub 认证：
   - 检查 gh CLI 与 Git 配置；
   - 引导我使用 gh auth login（设备码流程）或 SSH Key 完成登录验证；
   - 不要把 Token、私钥写入命令历史或日志中。

3. 验证与克隆仓库：
   - 为官方仓库 $OFFICIAL_REPO 点星；
   - Fork main 分支到我自己的 GitHub 账户；
   - 将 Fork 后的仓库克隆到 ~/custom_taixu。

4. 遵循 taixu-custom-iteration Skill 开发规范：
   - 按照太墟的 Jetpack Compose、Hilt 和多模块规范进行修改；
   - 编写或调整功能后运行单元测试验证；
   - 提交修改并推送到 Fork 仓库的特性分支。

5. 通过 GitHub Actions 构建独立的 TaiXuDev APK：
   - 触发 .github/workflows/taixudev-build.yml 编译；
   - 实时监控构建进度并在成功后下载 APK 至手机；
   - 校验包名 top.wkbin.taixu.dev 和应用名 TaiXuDev，与正式版独立共存。

6. 若体验满意，协助我生成标准 PR 提交到 $OFFICIAL_REPO。"""

    /** 由沙盒家目录推导出的全部部署目标路径（纯函数，便于单元测试）。 */
    data class DeployPaths(
        val workspace: File,
        val skill: File,
        val workflowCache: File,
        val workflowInRepo: File,
    )

    fun resolvePaths(rootfsHomeDir: File): DeployPaths = DeployPaths(
        workspace = File(rootfsHomeDir, WORKSPACE_NAME),
        skill = File(rootfsHomeDir, SKILL_DEPLOY_RELATIVE),
        workflowCache = File(rootfsHomeDir, WORKFLOW_CACHE_RELATIVE),
        workflowInRepo = File(rootfsHomeDir, "$WORKSPACE_NAME/$WORKFLOW_REPO_RELATIVE"),
    )

    /**
     * 初始化自定义迭代环境：创建隔离工作区并落盘 Skill 与 CI 工作流模板。
     *
     * @param rootfsHomeDir 沙盒家目录（通常为 /root），工作区与 .taixu 均在其下。
     */
    fun bootstrap(context: Context, rootfsHomeDir: File): BootstrapResult {
        val paths = resolvePaths(rootfsHomeDir)
        val deployed = mutableListOf<String>()
        return try {
            paths.workspace.mkdirs()
            paths.workflowInRepo.parentFile?.mkdirs()

            copyAsset(context, SKILL_ASSET_PATH, paths.skill)
            deployed += paths.skill.absolutePath

            copyAsset(context, WORKFLOW_ASSET_PATH, paths.workflowCache)
            deployed += paths.workflowCache.absolutePath

            // 预置到工作区仓库路径；若用户已克隆仓库，则由 git 决定是否覆盖。
            if (!paths.workflowInRepo.exists()) {
                copyAsset(context, WORKFLOW_ASSET_PATH, paths.workflowInRepo)
                deployed += paths.workflowInRepo.absolutePath
            }

            BootstrapResult(
                success = true,
                workspacePath = paths.workspace.absolutePath,
                prompt = BOOTSTRAP_PROMPT,
                deployedFiles = deployed,
            )
        } catch (e: IOException) {
            BootstrapResult(
                success = false,
                workspacePath = paths.workspace.absolutePath,
                prompt = "",
                deployedFiles = deployed,
                errorMessage = e.message ?: "Bootstrap failed while deploying assets",
            )
        }
    }

    private fun copyAsset(context: Context, assetPath: String, target: File) {
        target.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }

    data class BootstrapResult(
        val success: Boolean,
        val workspacePath: String,
        val prompt: String,
        val deployedFiles: List<String> = emptyList(),
        val errorMessage: String? = null,
    )
}

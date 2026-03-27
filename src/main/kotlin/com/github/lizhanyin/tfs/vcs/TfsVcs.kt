package com.github.lizhanyin.tfs.vcs

import com.github.lizhanyin.tfs.changes.TfsChangeProvider
import com.github.lizhanyin.tfs.services.TfsService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.ChangeProvider
import com.intellij.openapi.vcs.history.VcsHistoryProvider
import org.jetbrains.annotations.NonNls

/**
 * TFS VCS 实现类
 * 继承 AbstractVcs 作为与 IntelliJ VCS 框架的桥梁
 */
class TfsVcs(project: Project) : AbstractVcs(project, TFS_VCS_NAME) {

    companion object {
        @NonNls
        const val TFS_VCS_NAME = "TFS"

        @JvmStatic
        fun getInstance(project: Project): TfsVcs? {
            val vcsManager = ProjectLevelVcsManager.getInstance(project)
            return vcsManager.findVcsByName(TFS_VCS_NAME) as? TfsVcs
        }
    }

    private val tfsService: TfsService = TfsService.getInstance(project)
    private var changeProvider: TfsChangeProvider? = null

    override fun getDisplayName(): String = "Team Foundation Server"

    fun getMimeType(): String? = null

    override fun getChangeProvider(): ChangeProvider? {
        if (changeProvider == null) {
            changeProvider = TfsChangeProvider(project, this)
        }
        return changeProvider
    }

    override fun getVcsHistoryProvider(): VcsHistoryProvider? {
        // TODO: 实现 TfsHistoryProvider
        return null
    }

    // ============== 工具方法 ==============

    fun getService(): TfsService = tfsService

    fun isConfigured(): Boolean = tfsService.isConfigured()

    fun isInitialized(): Boolean = tfsService.isInitialized

    /**
     * TFS 修订版本号
     */
    class Revision(val changesetId: Int) : com.intellij.openapi.vcs.history.VcsRevisionNumber {
        override fun asString(): String = "C$changesetId"

        override fun compareTo(other: com.intellij.openapi.vcs.history.VcsRevisionNumber?): Int {
            if (other !is Revision) return -1
            return changesetId.compareTo(other.changesetId)
        }
    }
}

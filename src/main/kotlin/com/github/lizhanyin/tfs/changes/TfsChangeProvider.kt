package com.github.lizhanyin.tfs.changes

import com.github.lizhanyin.tfs.model.PendingChange
import com.github.lizhanyin.tfs.services.TfsService
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsKey
import com.intellij.openapi.vcs.changes.*
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil
import org.jetbrains.annotations.NotNull
import java.util.concurrent.ConcurrentHashMap

/**
 * TFS 变更提供者
 * 负责追踪工作区中的文件变更
 */
class TfsChangeProvider(
    private val project: Project,
    private val vcs: com.github.lizhanyin.tfs.vcs.TfsVcs
) : ChangeProvider {

    private val LOG = Logger.getInstance(TfsChangeProvider::class.java)

    private val tfsService: TfsService = TfsService.getInstance(project)
    private val pendingChangesCache = ConcurrentHashMap<String, PendingChange>()
    private var lastRefreshTime: Long = 0
    private val cacheValidityMs = 5000L

    @OptIn(IntellijInternalApi::class)
    override fun getChanges(
        @NotNull dirtyScope: VcsDirtyScope,
        @NotNull builder: ChangelistBuilder,
        @NotNull progress: com.intellij.openapi.progress.ProgressIndicator,
        @NotNull gate: ChangeListManagerGate
    ) {
        if (!tfsService.isInitialized) {
            return
        }

        val commandClient = tfsService.commandClient ?: return

        try {
            // 获取待提交的变更
            val pendingChanges = commandClient.pendingChanges.get()

            // 更新缓存
            pendingChangesCache.clear()
            for (change in pendingChanges) {
                change.localItem()?.let { pendingChangesCache[it] = change }
            }
            lastRefreshTime = System.currentTimeMillis()

            // 构建变更列表
            for (change in pendingChanges) {
                val localPath = change.localItem() ?: continue
                val file = VcsUtil.getVirtualFile(localPath) ?: continue

                val ideaChange = when (change.changeType()) {
                    PendingChange.ChangeType.ADD -> {
                        // 新增文件
                        Change(null, createCurrentContentRevision(file))
                    }
                    PendingChange.ChangeType.DELETE -> {
                        // 删除文件
                        Change(createCurrentContentRevision(file), null)
                    }
                    else -> {
                        // 修改文件
                        Change(createCurrentContentRevision(file), createCurrentContentRevision(file))
                    }
                }

                builder.processChange(ideaChange, VcsKey(vcs.name))
            }

            // 处理脏范围内的未跟踪文件
            // processUntrackedFiles(dirtyScope, builder) // 已弃用，暂时禁用

        } catch (e: Exception) {
            LOG.error("Failed to get TFS changes", e)
        }
    }

    override fun isModifiedDocumentTrackingRequired(): Boolean = true

    override fun doCleanup(modifiedFiles: MutableList<out VirtualFile>) {
        // 清理已不存在的变更
        modifiedFiles.removeIf { file ->
            !pendingChangesCache.containsKey(file.path)
        }
    }

    /**
     * 处理未跟踪的文件
     */
    @Suppress("DEPRECATION", "removal")
    private fun processUntrackedFiles(
        dirtyScope: VcsDirtyScope,
        builder: ChangelistBuilder
    ) {
        val untrackedFiles = mutableListOf<VirtualFile>()

        // 遍历脏范围内的文件
        for (filePath in dirtyScope.dirtyFiles) {
            val file = filePath.virtualFile
            if (file != null && file.isValid && !file.isDirectory) {
                // 检查是否在 TFS 中被跟踪
                if (!isFileTracked(file) && !isIgnored(file)) {
                    untrackedFiles.add(file)
                }
            }
        }

        // 添加未跟踪文件
        for (file in untrackedFiles) {
            builder.processUnversionedFile(file)
        }
    }

    /**
     * 检查文件是否被 TFS 跟踪
     */
    private fun isFileTracked(file: VirtualFile): Boolean {
        // 如果在缓存中，说明被跟踪
        if (pendingChangesCache.containsKey(file.path)) {
            return true
        }

        // 检查是否存在 $tf 目录（TFS 元数据）
        val tfDir = file.parent?.findChild("\$tf")
        if (tfDir != null && tfDir.isDirectory) {
            // TODO: 更精确的跟踪检查
            return true
        }

        return false
    }

    /**
     * 检查文件是否被忽略
     */
    private fun isIgnored(file: VirtualFile): Boolean {
        // 检查 .tfignore 文件
        val parent = file.parent ?: return false
        val tfignore = parent.findChild(".tfignore")
        if (tfignore != null && !tfignore.isDirectory) {
            // TODO: 解析 .tfignore 规则
            return false
        }

        // 默认忽略的文件
        val fileName = file.name
        return fileName.endsWith(".class") ||
                fileName.endsWith(".jar") ||
                fileName.startsWith(".") ||
                fileName == "Thumbs.db"
    }

    /**
     * 创建当前内容修订
     */
    private fun createCurrentContentRevision(file: VirtualFile): ContentRevision {
        val filePath = VcsUtil.getFilePath(file)
        return object : ContentRevision {
            override fun getFile(): FilePath = filePath
            override fun getContent(): String? {
                return try {
                    String(file.contentsToByteArray(), file.charset)
                } catch (e: Exception) {
                    LOG.warn("Failed to read file content: ${file.path}", e)
                    null
                }
            }
            override fun getRevisionNumber(): com.intellij.openapi.vcs.history.VcsRevisionNumber {
                return com.intellij.openapi.vcs.history.VcsRevisionNumber.NULL
            }
        }
    }

    /**
     * 刷新变更缓存
     */
    fun refreshCache() {
        pendingChangesCache.clear()
        lastRefreshTime = 0
    }

    /**
     * 检查缓存是否有效
     */
    fun isCacheValid(): Boolean {
        return System.currentTimeMillis() - lastRefreshTime < cacheValidityMs
    }

}

package com.github.lizhanyin.tfs.actions

import com.github.lizhanyin.tfs.wizard.ImportProjectWizard
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

/**
 * 从 TFS 导入项目操作
 */
class ImportProjectAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project

        // 显示导入项目向导
        val wizard = ImportProjectWizard(project)
        wizard.show()

        if (wizard.isOK) {
            // 用户完成了向导，执行导入操作
            val context = wizard.context

            // TODO: 执行实际的导入操作
            println("导入配置:")
            println("  服务器: ${context.serverUrl}")
            println(context.collection?.let { "  集合: ${it.collectionName}" })
            println("  本地路径: ${context.localPath}")
            println("  选中项目: ${context.selectedProjects}")
        }
    }

    override fun update(e: AnActionEvent) {
        // 始终启用此操作
        e.presentation.isEnabledAndVisible = true
    }
}

package com.github.lizhanyin.tfs.settings

import com.github.lizhanyin.tfs.services.TfsService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.FlowLayout
import javax.swing.*

/**
 * TFS 设置界面
 */
class TfsSettingsConfigurable(private val project: Project) : Configurable {

    private val settings: TfsSettings = TfsSettings.getInstance(project)

    init {

    }

    override fun getDisplayName(): String = "TFS"

    override fun createComponent(): JComponent {
        val panel = FormBuilder.createFormBuilder()

            .panel

        panel.border = JBUI.Borders.empty(10)
        return panel
    }


    private fun updateAuthFieldsVisibility() {

    }

    override fun isModified(): Boolean {
        return false
    }

    override fun apply() {
        applyToSettings()

        // 重新初始化服务
        val tfsService = TfsService.getInstance(project)
        tfsService.reinitialize()
    }

    private fun applyToSettings() {

    }

    override fun reset() {

        updateAuthFieldsVisibility()
    }
}

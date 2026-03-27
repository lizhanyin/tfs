package com.github.lizhanyin.tfs.wizard.step

import com.github.lizhanyin.tfs.wizard.ImportProjectContext
import com.intellij.ide.wizard.AbstractWizardStepEx
import org.jetbrains.annotations.NotNull
import javax.swing.JComponent

/**
 * 向导步骤抽象基类
 */
abstract class AbstractWizardStep(
    private val stepId: String,
    title: String,
    protected val context: ImportProjectContext
) : AbstractWizardStepEx(title) {

    private var component: JComponent? = null

    override fun getStepId(): String = stepId

    override fun getComponent(): JComponent {
        if (component == null) {
            component = buildComponent()
        }
        return component!!
    }

    /**
     * 构建步骤组件
     */
    protected abstract fun buildComponent(): JComponent

    override fun isComplete(): Boolean = true

    override fun getPreferredFocusedComponent(): JComponent? = component

    /**
     * 完成时调用，用于保存步骤数据
     * @return 是否可以继续
     */
    open fun onFinish(): Boolean = true

    override fun getNextStepId(): String? = null // 由 ImportProjectWizard 自行管理步骤流转

    override fun getPreviousStepId(): String? = null // 由 ImportProjectWizard 自行管理步骤流转

    override fun commit(@NotNull commitType: AbstractWizardStepEx.CommitType) {
        // 由 onFinish() 处理数据保存，此处无需操作
    }
}

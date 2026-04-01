package com.github.lizhanyin.tfs.wizard

import com.github.lizhanyin.tfs.TfsBundle
import com.github.lizhanyin.tfs.client.ui.framework.UIContext
import com.github.lizhanyin.tfs.wizard.step.ProjectSelectionStep
import com.github.lizhanyin.tfs.wizard.step.ServerSelectionStep
import com.github.lizhanyin.tfs.wizard.step.CollectionSelectionStep
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.*

/**
 * 导入项目向导
 */
class ImportProjectWizard(@field:Nullable private val project: Project?) : DialogWrapper(project, false) {

    companion object {
        const val TITLE = "ImportProjectWizard.title"
    }

    val context = ImportProjectContext()

    // 向导步骤
    private val steps = listOf(
        ServerSelectionStep(context),
        CollectionSelectionStep(context),
        ProjectSelectionStep(context)
    )
    private var currentStepIndex = 0

    // UI 组件
    private lateinit var contentPanel: JPanel
    private lateinit var stepContainer: JPanel
    private var currentStepComponent: JComponent? = null
    private lateinit var stepTitleLabel: JBLabel
    private lateinit var previousButton: JButton
    private lateinit var nextButton: JButton

    init {
        setTitle(TfsBundle.message(TITLE))
        setOKButtonText(TfsBundle.message("ImportProjectWizard.button.import"))
        init()
        // window 在 init() 之后才可用
        context.uiContext = UIContext.of(project, window)
    }

    override fun createCenterPanel(): JComponent {
        contentPanel = JPanel(BorderLayout())

        // 顶部步骤标题
        stepTitleLabel = JBLabel().apply {
            font = font.deriveFont(Font.BOLD, 14f)
            border = JBUI.Borders.emptyBottom(10)
        }
        contentPanel.add(stepTitleLabel, BorderLayout.NORTH)

        // 步骤内容容器
        stepContainer = JPanel(BorderLayout())
        contentPanel.add(stepContainer, BorderLayout.CENTER)

        // 初始化显示第一个步骤
        updateStepContent()

        return contentPanel
    }

    override fun createSouthPanel(): JComponent {
        val southPanel = JPanel(BorderLayout())

        // 导航按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0))

        previousButton = JButton(TfsBundle.message("ImportProjectWizard.button.previous")).apply {
            addActionListener { goToPreviousStep() }
            isEnabled = false
        }
        buttonPanel.add(previousButton)

        nextButton = JButton(TfsBundle.message("ImportProjectWizard.button.next")).apply {
            addActionListener { goToNextStep() }
        }
        buttonPanel.add(nextButton)

        // 完成按钮
        val finishButton = createJButtonForAction(okAction)
        finishButton.isEnabled = false
        buttonPanel.add(finishButton)

        // 放弃按钮
        setCancelButtonText(TfsBundle.message("ImportProjectWizard.button.cancel"))
        val cancelButton = createJButtonForAction(cancelAction)
        buttonPanel.add(cancelButton)

        southPanel.add(buttonPanel, BorderLayout.EAST)
        southPanel.border = JBUI.Borders.emptyTop(10)
        return southPanel
    }

    /**
     * 更新步骤内容
     */
    private fun updateStepContent() {
        currentStepComponent?.let { stepContainer.remove(it) }

        val step = steps[currentStepIndex]
        currentStepComponent = step.component
        stepContainer.add(currentStepComponent, BorderLayout.CENTER)

        // 更新步骤标题
        stepTitleLabel.text = TfsBundle.message("ImportProjectWizard.step.progress", currentStepIndex + 1, steps.size, step.title ?: "")

        // 更新按钮状态
        updateButtonState()

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    /**
     * 更新按钮状态
     */
    private fun updateButtonState() {
        if (!::previousButton.isInitialized || !::nextButton.isInitialized) {
            return
        }

        // 上一步按钮：第一步时禁用
        previousButton.isEnabled = currentStepIndex > 0

        val isLastStep = currentStepIndex == steps.size - 1

        // 下一步按钮：最后一步时禁用
        nextButton.isEnabled = !isLastStep

        // 完成按钮：只在最后一步启用
        setOKActionEnabled(isLastStep)
        setOKButtonText(TfsBundle.message("ImportProjectWizard.button.finish"))
    }

    /**
     * 跳转到上一步
     */
    private fun goToPreviousStep() {
        if (currentStepIndex > 0) {
            currentStepIndex--
            updateStepContent()
        }
    }

    /**
     * 跳转到下一步
     */
    private fun goToNextStep() {
        val currentStep = steps[currentStepIndex]
        if (!currentStep.isComplete) {
            Messages.showWarningDialog(
                contentPanel,
                TfsBundle.message("ImportProjectWizard.error.incompleteStep"),
                TfsBundle.message("ImportProjectWizard.error.cannotContinue")
            )
            return
        }

        // 保存当前步骤数据
        if (!currentStep.onFinish()) {
            return
        }

        if (currentStepIndex < steps.size - 1) {
            currentStepIndex++
            updateStepContent()
        }
    }

    override fun doOKAction() {
        // 完成按钮只在最后一步启用，直接执行导入
        val currentStep = steps[currentStepIndex]

        if (!currentStep.isComplete) {
            Messages.showWarningDialog(
                contentPanel,
                TfsBundle.message("ImportProjectWizard.error.incompleteStep"),
                TfsBundle.message("ImportProjectWizard.error.cannotImport")
            )
            return
        }

        // 保存当前步骤数据
        if (!currentStep.onFinish()) {
            return
        }

        // 执行导入
        executeImport()
    }

    /**
     * 执行项目导入
     */
    private fun executeImport() {
        ProgressManager.getInstance().run(object : Task.Modal(project, TfsBundle.message("ImportProjectWizard.progress.importing"), true) {
            override fun run(@NotNull indicator: ProgressIndicator) {
                indicator.text = TfsBundle.message("ImportProjectWizard.progress.importingFromTfs")
                indicator.isIndeterminate = false
                indicator.fraction = 0.0

                try {
                    // TODO: 实现实际的导入逻辑
                    // 1. 创建工作区
                    // 2. 映射文件夹
                    // 3. 获取文件
                    // 4. 创建 IDEA 项目

                    for (i in 0..100) {
                        indicator.checkCanceled()
                        indicator.fraction = i / 100.0
                        Thread.sleep(20)
                    }

                    SwingUtilities.invokeLater {
                        // 先关闭对话框
                        close(OK_EXIT_CODE)
                        // 再显示成功消息
                        Messages.showInfoMessage(
                            TfsBundle.message("ImportProjectWizard.success.message", context.serverUrl ?: "", context.teamProject ?: "", context.localPath ?: ""),
                            TfsBundle.message("ImportProjectWizard.success.title")
                        )
                    }

                } catch (e: Exception) {
                    if (e is com.intellij.openapi.progress.ProcessCanceledException) {
                        throw e
                    }
                    SwingUtilities.invokeLater {
                        Messages.showErrorDialog(
                            contentPanel,
                            TfsBundle.message("ImportProjectWizard.error.importFailed", e.message ?: ""),
                            TfsBundle.message("ImportProjectWizard.error.importFailed.title")
                        )
                    }
                }
            }
        })
    }

    override fun getDimensionServiceKey(): String = "TfsImportProjectWizard"

    override fun getPreferredFocusedComponent(): JComponent? {
        return if (currentStepIndex in steps.indices) {
            steps[currentStepIndex].preferredFocusedComponent
        } else null
    }
}

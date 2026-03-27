package com.github.lizhanyin.tfs.wizard;

import com.github.lizhanyin.tfs.wizard.step.*;
import com.intellij.ide.wizard.AbstractWizardStepEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入项目向导
 */
public class ImportProjectWizard extends DialogWrapper {

    public static final String TITLE = "从 TFS 导入项目";

    @Nullable
    private final Project project;
    private final ImportProjectContext context;

    // 向导步骤
    private final List<AbstractWizardStep> steps = new ArrayList<>();
    private int currentStepIndex = 0;

    // UI 组件
    private JPanel contentPanel;
    private JPanel stepContainer;
    private JComponent currentStepComponent;
    private JBLabel stepTitleLabel;
    private JButton previousButton;
    private JButton nextButton;

    public ImportProjectWizard(@Nullable Project project) {
        super(project, false);
        this.project = project;
        this.context = new ImportProjectContext();

        // 添加向导步骤
        steps.add(new ServerSelectionStep(context));
        steps.add(new TeamProjectSelectionStep(context));
        steps.add(new ProjectSelectionStep(context));

        setTitle(TITLE);
        setOKButtonText("导入");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        contentPanel = new JPanel(new BorderLayout());

        // 顶部步骤标题
        stepTitleLabel = new JBLabel();
        stepTitleLabel.setFont(stepTitleLabel.getFont().deriveFont(Font.BOLD, 14f));
        stepTitleLabel.setBorder(JBUI.Borders.emptyBottom(10));
        contentPanel.add(stepTitleLabel, BorderLayout.NORTH);

        // 步骤内容容器
        stepContainer = new JPanel(new BorderLayout());
        contentPanel.add(stepContainer, BorderLayout.CENTER);

        // 初始化显示第一个步骤
        updateStepContent();

        return contentPanel;
    }

    @Override
    protected JComponent createSouthPanel() {
        JPanel southPanel = new JPanel(new BorderLayout());

        // 左侧导航按钮
        JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        previousButton = new JButton("上一步");
        previousButton.addActionListener(e -> goToPreviousStep());
        previousButton.setEnabled(false);
        navigationPanel.add(previousButton);

        nextButton = new JButton("下一步");
        nextButton.addActionListener(e -> goToNextStep());
        navigationPanel.add(nextButton);

        southPanel.add(navigationPanel, BorderLayout.WEST);

        // 右侧标准按钮
        JPanel standardButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        standardButtons.add(createJButtonForAction(getOKAction()));
        standardButtons.add(createJButtonForAction(getCancelAction()));
        southPanel.add(standardButtons, BorderLayout.EAST);

        southPanel.setBorder(JBUI.Borders.emptyTop(10));
        return southPanel;
    }

    /**
     * 更新步骤内容
     */
    private void updateStepContent() {
        if (currentStepComponent != null) {
            stepContainer.remove(currentStepComponent);
        }

        AbstractWizardStep step = steps.get(currentStepIndex);
        currentStepComponent = step.getComponent();
        stepContainer.add(currentStepComponent, BorderLayout.CENTER);

        // 更新步骤标题
        stepTitleLabel.setText("步骤 " + (currentStepIndex + 1) + "/" + steps.size() + ": " + step.getTitle());

        // 更新按钮状态
        updateButtonState();

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonState() {
        if (previousButton == null || nextButton == null) {
            return;
        }

        previousButton.setEnabled(currentStepIndex > 0);

        boolean isLastStep = currentStepIndex == steps.size() - 1;
        nextButton.setEnabled(!isLastStep);
        nextButton.setVisible(!isLastStep);

        if (isLastStep) {
            setOKButtonText("导入");
        } else {
            setOKButtonText("下一步");
        }
    }

    /**
     * 跳转到上一步
     */
    private void goToPreviousStep() {
        if (currentStepIndex > 0) {
            currentStepIndex--;
            updateStepContent();
        }
    }

    /**
     * 跳转到下一步
     */
    private void goToNextStep() {
        AbstractWizardStep currentStep = (AbstractWizardStep) steps.get(currentStepIndex);
        if (!currentStep.isComplete()) {
            Messages.showWarningDialog(
                    contentPanel,
                    "请完成当前步骤的必填项",
                    "无法继续"
            );
            return;
        }

        // 保存当前步骤数据
        if (!currentStep.onFinish()) {
            return;
        }

        if (currentStepIndex < steps.size() - 1) {
            currentStepIndex++;
            updateStepContent();
        }
    }

    @Override
    protected void doOKAction() {
        AbstractWizardStep currentStep = steps.get(currentStepIndex);

        if (!currentStep.isComplete()) {
            if (currentStepIndex == steps.size() - 1) {
                Messages.showWarningDialog(
                        contentPanel,
                        "请完成当前步骤的必填项",
                        "无法导入"
                );
            } else {
                goToNextStep();
            }
            return;
        }

        // 保存当前步骤数据
        if (!currentStep.onFinish()) {
            return;
        }

        if (currentStepIndex < steps.size() - 1) {
            // 不是最后一步，执行下一步
            goToNextStep();
        } else {
            // 最后一步，执行导入
            executeImport();
        }
    }

    /**
     * 执行项目导入
     */
    private void executeImport() {
        ProgressManager.getInstance().run(new Task.Modal(project, "正在导入项目", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("正在从 TFS 导入项目...");
                indicator.setIndeterminate(false);
                indicator.setFraction(0.0);

                try {
                    // TODO: 实现实际的导入逻辑
                    // 1. 创建工作区
                    // 2. 映射文件夹
                    // 3. 获取文件
                    // 4. 创建 IDEA 项目

                    for (int i = 0; i <= 100; i++) {
                        indicator.checkCanceled();
                        indicator.setFraction(i / 100.0);
                        Thread.sleep(20);
                    }

                    SwingUtilities.invokeLater(() -> {
                        ImportProjectWizard.this.doOKAction();
                        Messages.showInfoMessage(
                                contentPanel,
                                "项目导入成功！\n\n服务器: " + context.getServerUrl() +
                                        "\n团队项目: " + context.getTeamProject() +
                                        "\n本地路径: " + context.getLocalPath(),
                                "导入完成"
                        );
                    });

                } catch (ProcessCanceledException e) {
                    // 用户取消，直接重新抛出
                    throw e;
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        Messages.showErrorDialog(
                                contentPanel,
                                "项目导入失败: " + e.getMessage(),
                                "导入失败"
                        );
                    });
                }
            }
        });
    }

    /**
     * 获取向导上下文
     */
    @NotNull
    public ImportProjectContext getContext() {
        return context;
    }

    @Override
    protected String getDimensionServiceKey() {
        return "TfsImportProjectWizard";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        if (currentStepIndex >= 0 && currentStepIndex < steps.size()) {
            return steps.get(currentStepIndex).getPreferredFocusedComponent();
        }
        return null;
    }
}

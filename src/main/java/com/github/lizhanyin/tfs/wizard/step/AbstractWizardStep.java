package com.github.lizhanyin.tfs.wizard.step;

import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.intellij.ide.wizard.AbstractWizardStepEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 向导步骤抽象基类
 */
public abstract class AbstractWizardStep extends AbstractWizardStepEx {

    protected final ImportProjectContext context;
    private JComponent component;
    private String stepId;

    public AbstractWizardStep(@NotNull String stepId, @NotNull String title, @NotNull ImportProjectContext context) {
        super(title);
        this.stepId = stepId;
        this.context = context;
    }

    public @NotNull String getStepId(){
        return this.stepId;
    }

    @Override
    public JComponent getComponent() {
        if (component == null) {
            component = buildComponent();
        }
        return component;
    }

    /**
     * 构建步骤组件
     */
    protected abstract JComponent buildComponent();

    /**
     * 获取上下文
     */
    protected ImportProjectContext getContext() {
        return context;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return component;
    }

    public boolean onFinish() {
        return true;
    }

    @Nullable
    @Override
    public String getNextStepId() {
        return null; // 由 ImportProjectWizard 自行管理步骤流转
    }

    @Nullable
    @Override
    public String getPreviousStepId() {
        return null; // 由 ImportProjectWizard 自行管理步骤流转
    }

    @Override
    public void commit(@NotNull CommitType commitType) {
        // 由 onFinish() 处理数据保存，此处无需操作
    }
}

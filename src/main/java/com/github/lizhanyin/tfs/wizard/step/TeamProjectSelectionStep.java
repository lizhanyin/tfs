package com.github.lizhanyin.tfs.wizard.step;

import com.github.lizhanyin.tfs.services.TfsConnectionService;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 团队项目选择步骤
 */
public class TeamProjectSelectionStep extends AbstractWizardStep {

    private static final String STEP_ID = "team-project-selection";

    // UI 组件
    private JComboBox<String> teamProjectComboBox;
    private JBLabel statusLabel;
    private JButton refreshButton;
    private JLabel serverInfoLabel;

    // 团队项目列表
    private final List<String> teamProjects = new ArrayList<>();

    public TeamProjectSelectionStep(@NotNull ImportProjectContext context) {
        super(STEP_ID, "选择团队项目", context);
    }

    @Override
    protected JComponent buildComponent() {
        FormBuilder builder = FormBuilder.createFormBuilder();

        // 服务器信息
        serverInfoLabel = new JLabel(" ");
        updateServerInfo();
        builder.addLabeledComponent("已连接到:", serverInfoLabel);
        builder.addSeparator();
        builder.addLabeledComponent("团队项目:", createTeamProjectPanel());
        builder.addSeparator();
        builder.addComponent(createStatusPanel());

        JPanel panel = builder.getPanel();
        panel.setBorder(JBUI.Borders.empty(10));

        return panel;
    }

    /**
     * 更新服务器信息显示
     */
    private void updateServerInfo() {
        StringBuilder info = new StringBuilder("<html><b>");
        info.append(context.getServerUrl());
        if (context.getCollectionName() != null && !context.getCollectionName().isEmpty()) {
            info.append("/").append(context.getCollectionName());
        }
        info.append("</b></html>");
        serverInfoLabel.setText(info.toString());
    }

    /**
     * 创建团队项目选择面板
     */
    private JComponent createTeamProjectPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));

        teamProjectComboBox = new JComboBox<>();
        teamProjectComboBox.setEnabled(false);
        updateTeamProjectComboBox();
        panel.add(teamProjectComboBox, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> loadTeamProjects());
        buttonPanel.add(refreshButton);

        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 创建状态面板
     */
    private JComponent createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        statusLabel = new JBLabel("点击\"刷新\"加载团队项目列表");
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 加载团队项目列表
     */
    private void loadTeamProjects() {
        if (!context.isServerConfigured()) {
            statusLabel.setText("请先配置服务器连接");
            statusLabel.setForeground(java.awt.Color.RED);
            return;
        }

        statusLabel.setText("正在加载团队项目...");
        statusLabel.setForeground(java.awt.Color.BLACK);
        refreshButton.setEnabled(false);
        teamProjectComboBox.setEnabled(false);

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "加载团队项目", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("正在连接到 TFS 服务器...");
                indicator.setIndeterminate(true);

                try {
                    // 使用 TFS SDK 加载团队项目列表
                    TfsConnectionService connectionService =
                            ApplicationManager.getApplication().getService(TfsConnectionService.class);

                    List<String> projects = connectionService.getTeamProjects(context);

                    // 更新 UI
                    SwingUtilities.invokeLater(() -> {
                        teamProjects.clear();
                        teamProjects.addAll(projects);
                        updateTeamProjectComboBox();

                        if (projects.isEmpty()) {
                            statusLabel.setText("未找到团队项目");
                            statusLabel.setForeground(java.awt.Color.ORANGE);
                        } else {
                            statusLabel.setText("已加载 " + projects.size() + " 个团队项目");
                            statusLabel.setForeground(new java.awt.Color(0, 128, 0));
                        }

                        refreshButton.setEnabled(true);
                        teamProjectComboBox.setEnabled(true);
                    });

                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("加载失败: " + e.getMessage());
                        statusLabel.setForeground(java.awt.Color.RED);
                        refreshButton.setEnabled(true);
                    });
                }
            }
        });
    }

    /**
     * 更新团队项目下拉框
     */
    private void updateTeamProjectComboBox() {
        teamProjectComboBox.setModel(new CollectionComboBoxModel<>(teamProjects));
        if (!teamProjects.isEmpty()) {
            teamProjectComboBox.setSelectedIndex(0);
        }
    }

    @Override
    public boolean isComplete() {
        return teamProjectComboBox.getSelectedItem() != null;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return teamProjectComboBox;
    }

    @Override
    public boolean onFinish() {
        String selected = (String) teamProjectComboBox.getSelectedItem();
        if (selected != null) {
            context.setTeamProject(selected);
        }
        return true;
    }
}

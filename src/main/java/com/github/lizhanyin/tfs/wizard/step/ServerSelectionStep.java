package com.github.lizhanyin.tfs.wizard.step;

import com.github.lizhanyin.tfs.settings.TfsServerConfiguration;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.github.lizhanyin.tfs.wizard.dialog.ManageServersDialog;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;

/**
 * 服务器选择步骤
 * 第一步：选择要连接的服务器
 */
public class ServerSelectionStep extends AbstractWizardStep {

    private static final String STEP_ID = "server-selection";

    // UI 组件
    private ComboBox<TfsServerConfiguration.ServerConfig> serverComboBox;
    private JLabel statusLabel;

    public ServerSelectionStep(@NotNull ImportProjectContext context) {
        super(STEP_ID, "服务器选择", context);
    }

    @Override
    protected JComponent buildComponent() {
        FormBuilder builder = FormBuilder.createFormBuilder();

        // 步骤说明
        builder.addComponent(new JLabel("选择要连接到的服务器"));
        builder.addSeparator();

        // 服务器选择面板
        builder.addLabeledComponent("TFS服务器:", createServerPanel());

        // 状态标签
        statusLabel = new JLabel(" ");
        builder.addComponent(statusLabel);

        JPanel formPanel = builder.getPanel();
        formPanel.setBorder(JBUI.Borders.empty(10));

        // 使用 BorderLayout 将内容放在顶部
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(formPanel, BorderLayout.NORTH);

        return panel;
    }

    /**
     * 创建服务器选择面板
     */
    private JPanel createServerPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));

        // 服务器下拉框
        serverComboBox = new ComboBox<>();
        serverComboBox.setPreferredSize(new Dimension(300, serverComboBox.getPreferredSize().height));
        loadServers();

        serverComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                onServerSelected();
            }
        });

        panel.add(serverComboBox, BorderLayout.CENTER);

        // 服务器管理按钮
        JButton manageButton = new JButton("服务器...");
        manageButton.addActionListener(e -> showManageServersDialog());
        panel.add(manageButton, BorderLayout.EAST);

        return panel;
    }

    /**
     * 加载已保存的服务器
     */
    private void loadServers() {
        List<TfsServerConfiguration.ServerConfig> servers = TfsServerConfiguration.getInstance().getServers();
        serverComboBox.setModel(new CollectionComboBoxModel<>(servers));

        // 如果有服务器，默认选中第一个
        if (!servers.isEmpty()) {
            serverComboBox.setSelectedIndex(0);
            onServerSelected();
        }
    }

    /**
     * 服务器选中时
     */
    private void onServerSelected() {
        TfsServerConfiguration.ServerConfig selected = getSelectedServer();
        if (selected != null) {
            // 保存到上下文
            context.setServerUrl(selected.getUrl());

            // 恢复凭证信息
            if (selected.getAuthType() != null) {
                try {
                    ImportProjectContext.AuthType authType = ImportProjectContext.AuthType.valueOf(selected.getAuthType());
                    context.setAuthType(authType);
                } catch (IllegalArgumentException ignored) {
                    context.setAuthType(ImportProjectContext.AuthType.NTLM);
                }
            }

            context.setUsername(selected.getUsername());
            context.setPassword(selected.getPassword());
            context.setDomain(selected.getDomain());

            // 显示连接信息（statusLabel 可能还未初始化）
            if (statusLabel != null) {
                String credStatus = selected.getPassword() != null && !selected.getPassword().isEmpty()
                        ? "（已保存凭证）" : "";
                statusLabel.setText(credStatus);
                statusLabel.setForeground(JBColor.GRAY);
            }
        } else if (statusLabel != null) {
            statusLabel.setText(" ");
        }
    }

    /**
     * 显示服务器管理对话框
     */
    private void showManageServersDialog() {
        // 获取按钮所在的窗口作为父组件
        Window parentWindow = SwingUtilities.getWindowAncestor(serverComboBox);
        ManageServersDialog dialog = new ManageServersDialog(parentWindow);
        dialog.showAndGet();

        // 重新加载服务器列表
        TfsServerConfiguration.ServerConfig previousSelected = getSelectedServer();
        loadServers();

        // 尝试恢复之前选中的服务器
        if (previousSelected != null) {
            for (int i = 0; i < serverComboBox.getItemCount(); i++) {
                TfsServerConfiguration.ServerConfig server = serverComboBox.getItemAt(i);
                if (server.getUrl().equals(previousSelected.getUrl())) {
                    serverComboBox.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    /**
     * 获取选中的服务器
     */
    @Nullable
    private TfsServerConfiguration.ServerConfig getSelectedServer() {
        return (TfsServerConfiguration.ServerConfig) serverComboBox.getSelectedItem();
    }

    @Override
    public boolean isComplete() {
        TfsServerConfiguration.ServerConfig selected = getSelectedServer();
        return selected != null && !selected.getUrl().isEmpty();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return serverComboBox;
    }

    @Override
    public boolean onFinish() {
        TfsServerConfiguration.ServerConfig selected = getSelectedServer();
        if (selected != null) {
            context.setServerUrl(selected.getUrl());
            context.setCollectionName(selected.getCollection());

            if (selected.getAuthType() != null) {
                try {
                    ImportProjectContext.AuthType authType = ImportProjectContext.AuthType.valueOf(selected.getAuthType());
                    context.setAuthType(authType);
                } catch (IllegalArgumentException ignored) {
                }
            }

            context.setUsername(selected.getUsername());
            context.setPassword(selected.getPassword());
            context.setDomain(selected.getDomain());
        }
        return true;
    }
}

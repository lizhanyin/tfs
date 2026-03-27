package com.github.lizhanyin.tfs.wizard.step;

import com.github.lizhanyin.tfs.settings.TfsServerConfiguration;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务器选择步骤
 */
public class ServerSelectionStep extends AbstractWizardStep {

    private static final String STEP_ID = "server-selection";

    // UI 组件
    private ComboBox<TfsServerConfiguration.ServerConfig> serverComboBox;
    private JBTextField usernameField;
    private JBPasswordField passwordField;
    private JBTextField domainField;
    private ComboBox<ImportProjectContext.AuthType> authTypeComboBox;
    private JButton testConnectionButton;
    private JLabel connectionStatusLabel;

    public ServerSelectionStep(@NotNull ImportProjectContext context) {
        super(STEP_ID, "选择 TFS 服务器", context);
    }

    @Override
    protected JComponent buildComponent() {
        FormBuilder builder = FormBuilder.createFormBuilder();

        // 服务器选择
        builder.addLabeledComponent("TFS 服务器:", createServerPanel());
        builder.addSeparator();
        builder.addLabeledComponent("认证方式:", createAuthPanel());
        builder.addSeparator();
        builder.addComponent(createStatusPanel());

        JPanel panel = builder.getPanel();
        panel.setBorder(JBUI.Borders.empty(10));
        return panel;
    }

    /**
     * 创建服务器选择面板
     */
    private JPanel createServerPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));

        serverComboBox = new ComboBox<>();
        loadServers();
        serverComboBox.addActionListener(e -> {
            TfsServerConfiguration.ServerConfig selected = (TfsServerConfiguration.ServerConfig) serverComboBox.getSelectedItem();
            if (selected != null) {
                fillCredentials(selected);
            }
        });
        panel.add(serverComboBox, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        JButton addButton = new JButton("添加...");
        addButton.addActionListener(e -> showAddServerDialog());
        buttonPanel.add(addButton);

        JButton removeButton = new JButton("移除");
        removeButton.addActionListener(e -> removeSelectedServer());
        buttonPanel.add(removeButton);

        JButton editButton = new JButton("编辑凭据...");
        editButton.addActionListener(e -> showEditCredentialsDialog());
        buttonPanel.add(editButton);

        panel.add(buttonPanel, BorderLayout.EAST);
        return panel;
    }

    /**
     * 创建认证面板
     */
    private JPanel createAuthPanel() {
        FormBuilder builder = FormBuilder.createFormBuilder();

        authTypeComboBox = new ComboBox<>(new DefaultComboBoxModel<>(
                ImportProjectContext.AuthType.values()
        ));
        authTypeComboBox.setSelectedItem(ImportProjectContext.AuthType.NTLM);
        authTypeComboBox.addActionListener(e -> updateAuthFieldsVisibility());
        builder.addLabeledComponent("认证方式:", authTypeComboBox);

        domainField = new JBTextField();
        builder.addLabeledComponent("域:", domainField);

        usernameField = new JBTextField();
        builder.addLabeledComponent("用户名:", usernameField);

        passwordField = new JBPasswordField();
        builder.addLabeledComponent("密码:", passwordField);

        JPanel panel = builder.getPanel();
        updateAuthFieldsVisibility();
        return panel;
    }

    /**
     * 创建状态面板
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));

        testConnectionButton = new JButton("测试连接");
        testConnectionButton.addActionListener(e -> testConnection());
        panel.add(testConnectionButton, BorderLayout.WEST);

        connectionStatusLabel = new JLabel(" ");
        panel.add(connectionStatusLabel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 更新认证字段的可见性
     */
    private void updateAuthFieldsVisibility() {
        ImportProjectContext.AuthType authType = (ImportProjectContext.AuthType) authTypeComboBox.getSelectedItem();
        boolean showDomain = authType == ImportProjectContext.AuthType.NTLM;
        domainField.setVisible(showDomain);
        if (domainField.getParent() != null) {
            domainField.getParent().setVisible(showDomain);
        }
    }

    /**
     * 填充凭据信息
     */
    private void fillCredentials(TfsServerConfiguration.ServerConfig server) {
        if (server.getUsername() != null) {
            usernameField.setText(server.getUsername());
        } else {
            usernameField.setText("");
        }
        if (server.getPassword() != null) {
            passwordField.setText(server.getPassword());
        } else {
            passwordField.setText("");
        }
        if (server.getDomain() != null) {
            domainField.setText(server.getDomain());
        } else {
            domainField.setText("");
        }
        if (server.getAuthType() != null) {
            try {
                ImportProjectContext.AuthType authType = ImportProjectContext.AuthType.valueOf(server.getAuthType());
                authTypeComboBox.setSelectedItem(authType);
            } catch (IllegalArgumentException ignored) {
                authTypeComboBox.setSelectedItem(ImportProjectContext.AuthType.NTLM);
            }
        }
    }

    /**
     * 加载已保存的服务器
     */
    private void loadServers() {
        List<TfsServerConfiguration.ServerConfig> servers = TfsServerConfiguration.getInstance().getServers();
        serverComboBox.setModel(new CollectionComboBoxModel<>(servers));
    }

    /**
     * 显示添加服务器对话框
     */
    private void showAddServerDialog() {
        AddServerDialog dialog = new AddServerDialog(null);
        if (dialog.showAndGet()) {
            TfsServerConfiguration.ServerConfig newServer = dialog.getServerConfig();
            TfsServerConfiguration.getInstance().addServer(newServer);
            loadServers();
            serverComboBox.setSelectedItem(newServer);
        }
    }

    /**
     * 移除选中的服务器
     */
    private void removeSelectedServer() {
        TfsServerConfiguration.ServerConfig selected = (TfsServerConfiguration.ServerConfig) serverComboBox.getSelectedItem();
        if (selected != null) {
            int result = JOptionPane.showConfirmDialog(
                    null,
                    "确定要移除服务器 \"" + selected.getUrl() + "\" 吗？",
                    "确认移除",
                    JOptionPane.YES_NO_OPTION
            );
            if (result == JOptionPane.YES_OPTION) {
                TfsServerConfiguration.getInstance().removeServer(selected);
                loadServers();
            }
        }
    }

    /**
     * 显示编辑凭据对话框
     */
    private void showEditCredentialsDialog() {
        TfsServerConfiguration.ServerConfig selected = (TfsServerConfiguration.ServerConfig) serverComboBox.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(null, "请先选择一个服务器");
            return;
        }

        AddServerDialog dialog = new AddServerDialog(selected);
        if (dialog.showAndGet()) {
            TfsServerConfiguration.ServerConfig updatedServer = dialog.getServerConfig();
            TfsServerConfiguration.getInstance().updateServer(selected, updatedServer);
            loadServers();
            serverComboBox.setSelectedItem(updatedServer);
        }
    }

    /**
     * 测试连接
     */
    private void testConnection() {
        TfsServerConfiguration.ServerConfig selected = (TfsServerConfiguration.ServerConfig) serverComboBox.getSelectedItem();
        if (selected == null) {
            connectionStatusLabel.setText("请先选择一个服务器");
            return;
        }

        // 保存当前配置到上下文
        saveToContext();

        connectionStatusLabel.setText("正在测试连接...");
        testConnectionButton.setEnabled(false);

        // 在后台线程中测试连接
        com.intellij.openapi.progress.ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                com.github.lizhanyin.tfs.services.TfsConnectionService connectionService =
                        com.intellij.openapi.application.ApplicationManager.getApplication()
                                .getService(com.github.lizhanyin.tfs.services.TfsConnectionService.class);

                boolean success = connectionService.testConnection(context);

                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        connectionStatusLabel.setText("连接成功!");
                        connectionStatusLabel.setForeground(new java.awt.Color(0, 128, 0));
                    } else {
                        connectionStatusLabel.setText("连接失败，请检查配置");
                        connectionStatusLabel.setForeground(java.awt.Color.RED);
                    }
                    testConnectionButton.setEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    connectionStatusLabel.setText("连接失败: " + e.getMessage());
                    connectionStatusLabel.setForeground(java.awt.Color.RED);
                    testConnectionButton.setEnabled(true);
                });
            }
        }, "测试 TFS 连接", true, null);
    }

    /**
     * 保存当前配置到上下文
     */
    private void saveToContext() {
        TfsServerConfiguration.ServerConfig selected = (TfsServerConfiguration.ServerConfig) serverComboBox.getSelectedItem();
        if (selected != null) {
            context.setServerUrl(selected.getUrl());
            context.setCollectionName(selected.getCollection());
        }

        context.setAuthType((ImportProjectContext.AuthType) authTypeComboBox.getSelectedItem());
        context.setUsername(usernameField.getText());
        context.setPassword(new String(passwordField.getPassword()));
        context.setDomain(domainField.getText());
    }

    @Override
    public boolean isComplete() {
        TfsServerConfiguration.ServerConfig selected = (TfsServerConfiguration.ServerConfig) serverComboBox.getSelectedItem();
        return selected != null && !selected.getUrl().isEmpty();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return serverComboBox;
    }

    @Override
    public boolean onFinish() {
        saveToContext();

        // 保存凭据到服务器配置
        TfsServerConfiguration.ServerConfig selected = (TfsServerConfiguration.ServerConfig) serverComboBox.getSelectedItem();
        if (selected != null) {
            selected.setUsername(usernameField.getText());
            selected.setPassword(new String(passwordField.getPassword()));
            selected.setDomain(domainField.getText());
            selected.setAuthType(((ImportProjectContext.AuthType) authTypeComboBox.getSelectedItem()).name());
            TfsServerConfiguration.getInstance().updateServer(selected, selected);
        }

        return true;
    }

    /**
     * 添加服务器对话框
     */
    private static class AddServerDialog extends com.intellij.openapi.ui.DialogWrapper {
        private JBTextField urlField;
        private JBTextField collectionField;
        private JBTextField nameField;
        private final TfsServerConfiguration.ServerConfig editingItem;

        protected AddServerDialog(@Nullable TfsServerConfiguration.ServerConfig editingItem) {
            super(false);
            this.editingItem = editingItem;
            setTitle(editingItem == null ? "添加 TFS 服务器" : "编辑 TFS 服务器");
            init();
        }

        @Override
        protected JComponent createCenterPanel() {
            FormBuilder builder = FormBuilder.createFormBuilder();

            urlField = new JBTextField();
            urlField.setText(editingItem != null && editingItem.getUrl() != null ? editingItem.getUrl() : "http://");
            builder.addLabeledComponent("服务器 URL:", urlField);

            collectionField = new JBTextField();
            collectionField.setText(editingItem != null && editingItem.getCollection() != null ? editingItem.getCollection() : "DefaultCollection");
            builder.addLabeledComponent("集合名称:", collectionField);

            nameField = new JBTextField();
            nameField.setText(editingItem != null && editingItem.getName() != null ? editingItem.getName() : "");
            builder.addLabeledComponent("显示名称:", nameField);

            JPanel panel = builder.getPanel();
            panel.setBorder(JBUI.Borders.empty(10));
            return panel;
        }

        public TfsServerConfiguration.ServerConfig getServerConfig() {
            String url = urlField.getText().trim();
            String collection = collectionField.getText().trim();
            String name = nameField.getText().trim();

            if (name.isEmpty()) {
                name = url;
            }

            TfsServerConfiguration.ServerConfig config = new TfsServerConfiguration.ServerConfig(url, collection, name);

            // 保留原有的凭据信息
            if (editingItem != null) {
                config.setUsername(editingItem.getUsername());
                config.setPassword(editingItem.getPassword());
                config.setDomain(editingItem.getDomain());
                config.setAuthType(editingItem.getAuthType());
            }

            return config;
        }

        @Override
        protected void doOKAction() {
            if (urlField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "请输入服务器 URL");
                return;
            }
            super.doOKAction();
        }
    }
}

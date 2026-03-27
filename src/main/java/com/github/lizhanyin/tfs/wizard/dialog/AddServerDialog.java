package com.github.lizhanyin.tfs.wizard.dialog;

import com.github.lizhanyin.tfs.services.TfsConnectionService;
import com.github.lizhanyin.tfs.settings.TfsServerConfiguration;
import com.github.lizhanyin.tfs.startup.TfsNativeLibraryInitializer;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;

/**
 * 添加 Team Foundation Server 对话框
 */
public class AddServerDialog extends DialogWrapper {

    private JBTextField nameOrUrlField;
    private JBTextField pathField;
    private JBTextField portField;
    private JRadioButton httpRadio;
    private JRadioButton httpsRadio;
    private JBTextField previewField;
    private JButton testButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private volatile boolean testingInProgress = false;

    private final TfsServerConfiguration.ServerConfig editingItem;
    private String savedUsername;
    private String savedPassword;
    private CredentialsDialog.CredentialType savedCredentialType = CredentialsDialog.CredentialType.USER_PASSWORD;

    private boolean connectionTested = false;

    public AddServerDialog(Component parent, @Nullable TfsServerConfiguration.ServerConfig editingItem) {
        super(parent, false);
        // 确保本地库已初始化
        TfsNativeLibraryInitializer.INSTANCE.init();
        this.editingItem = editingItem;
        setTitle(editingItem == null ? "添加 Team Foundation Server" : "编辑 Team Foundation Server");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        FormBuilder builder = FormBuilder.createFormBuilder();

        // 名称或URL输入 - label 和 input 分两行显示
        builder.addComponent(new JLabel("Team Foundation Server 的名称或URL:"));
        nameOrUrlField = new JBTextField();
        nameOrUrlField.getEmptyText().setText("例如: tfs.example.com 或 192.168.100.1");
        builder.addComponent(nameOrUrlField);

        // 连接信息面板（fieldset 效果）
        JPanel connectionInfoPanel = new JPanel();
        connectionInfoPanel.setBorder(BorderFactory.createTitledBorder("连接信息"));
        connectionInfoPanel.setLayout(new BorderLayout(5, 5));

        FormBuilder connectionBuilder = FormBuilder.createFormBuilder();

        // 路径
        pathField = new JBTextField("tfs");
        connectionBuilder.addLabeledComponent("路径:", pathField);

        // 端口
        portField = new JBTextField("8080");
        connectionBuilder.addLabeledComponent("端口号:", portField);

        // 协议选择
        JPanel protocolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        httpRadio = new JRadioButton("HTTP");
        httpRadio.setSelected(true);
        httpsRadio = new JRadioButton("HTTPS");
        ButtonGroup protocolGroup = new ButtonGroup();
        protocolGroup.add(httpRadio);
        protocolGroup.add(httpsRadio);
        protocolPanel.add(httpRadio);
        protocolPanel.add(httpsRadio);
        connectionBuilder.addLabeledComponent("协议:", protocolPanel);

        JPanel innerPanel = connectionBuilder.getPanel();
        innerPanel.setBorder(JBUI.Borders.empty(0, 10)); // 上下0，左右10像素间距
        connectionInfoPanel.add(innerPanel, BorderLayout.CENTER);
        builder.addComponent(connectionInfoPanel);

        // 预览URL
        JPanel previewPanel = new JPanel(new BorderLayout(5, 0));
        previewField = new JBTextField();
        previewField.setEditable(false);
        previewPanel.add(previewField, BorderLayout.CENTER);

        // 测试和停止按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        testButton = new JButton("测试");
        testButton.addActionListener(e -> testConnection());
        stopButton = new JButton("停止");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopTesting());
        buttonPanel.add(testButton);
        buttonPanel.add(stopButton);
        previewPanel.add(buttonPanel, BorderLayout.EAST);
        builder.addLabeledComponent("预览:", previewPanel);

        // 状态标签
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(JBColor.GRAY);
        builder.addComponent(statusLabel);

        // 如果是编辑模式，填充数据
        if (editingItem != null) {
            fillFromConfig(editingItem);
        }

        // 添加监听器更新预览
        DocumentListener docListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePreview();
                connectionTested = false;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePreview();
                connectionTested = false;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePreview();
                connectionTested = false;
            }
        };

        nameOrUrlField.getDocument().addDocumentListener(docListener);
        pathField.getDocument().addDocumentListener(docListener);
        portField.getDocument().addDocumentListener(docListener);

        httpRadio.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updatePreview();
                connectionTested = false;
            }
        });
        httpsRadio.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updatePreview();
                connectionTested = false;
            }
        });

        // 初始更新预览
        updatePreview();

        JPanel panel = builder.getPanel();
        panel.setBorder(JBUI.Borders.empty(10));
        panel.setPreferredSize(new Dimension(500, panel.getPreferredSize().height));
        return panel;
    }

    @Override
    protected JComponent createSouthPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton credentialsButton = new JButton("输入凭证");
        credentialsButton.addActionListener(e -> showCredentialsDialog());
        leftPanel.add(credentialsButton);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.add(createJButtonForAction(getOKAction()));
        rightPanel.add(createJButtonForAction(getCancelAction()));

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 从配置填充数据
     */
    private void fillFromConfig(@NotNull TfsServerConfiguration.ServerConfig config) {
        String url = config.getUrl();
        if (url == null || url.isEmpty()) {
            return;
        }

        // 解析 URL
        try {
            if (url.startsWith("https://")) {
                httpsRadio.setSelected(true);
                httpRadio.setSelected(false);
                url = url.substring(8);
            } else if (url.startsWith("http://")) {
                httpRadio.setSelected(true);
                httpsRadio.setSelected(false);
                url = url.substring(7);
            }

            // 分离端口
            int portIndex = url.indexOf(':');
            int pathIndex = url.indexOf('/');

            if (portIndex > 0) {
                String host = url.substring(0, portIndex);
                nameOrUrlField.setText(host);

                if (pathIndex > portIndex) {
                    portField.setText(url.substring(portIndex + 1, pathIndex));
                    pathField.setText(url.substring(pathIndex + 1));
                } else {
                    portField.setText(url.substring(portIndex + 1));
                }
            } else if (pathIndex > 0) {
                nameOrUrlField.setText(url.substring(0, pathIndex));
                pathField.setText(url.substring(pathIndex + 1));
                portField.setText(httpRadio.isSelected() ? "80" : "443");
            } else {
                nameOrUrlField.setText(url);
            }

            // 恢复凭证信息
            if (config.getAuthType() != null) {
                try {
                    savedCredentialType = CredentialsDialog.CredentialType.valueOf(config.getAuthType());
                } catch (IllegalArgumentException ignored) {
                }
            }
            savedUsername = config.getUsername();
            savedPassword = config.getPassword();

        } catch (Exception e) {
            // 解析失败，直接填入原始值
            nameOrUrlField.setText(config.getUrl());
        }
    }

    /**
     * 更新预览URL
     */
    private void updatePreview() {
        String url = buildUrl();
        previewField.setText(url);
    }

    /**
     * 构建完整URL
     */
    @NotNull
    private String buildUrl() {
        String nameOrUrl = nameOrUrlField.getText().trim();

        // 如果已经是完整URL，直接返回
        if (nameOrUrl.startsWith("http://") || nameOrUrl.startsWith("https://")) {
            return nameOrUrl;
        }

        String protocol = httpsRadio.isSelected() ? "https" : "http";
        String port = portField.getText().trim();
        String path = pathField.getText().trim();

        StringBuilder url = new StringBuilder();
        url.append(protocol).append("://");
        url.append(nameOrUrl);

        // 只有非标准端口才添加端口号
        if (!port.isEmpty()) {
            boolean isStandardPort = (httpsRadio.isSelected() && "443".equals(port))
                    || (httpRadio.isSelected() && "80".equals(port));
            if (!isStandardPort) {
                url.append(":").append(port);
            }
        }

        if (!path.isEmpty()) {
            if (!path.startsWith("/")) {
                url.append("/");
            }
            url.append(path);
        }

        return url.toString();
    }

    /**
     * 显示凭证对话框
     */
    private void showCredentialsDialog() {
        CredentialsDialog dialog = new CredentialsDialog(getContentPane(), buildUrl());
        dialog.setCredentials(savedCredentialType, savedUsername, savedPassword);
        if (dialog.showAndGet()) {
            savedCredentialType = dialog.getCredentialType();
            savedUsername = dialog.getUsername();
            savedPassword = dialog.getPassword();
        }
    }

    /**
     * 测试连接
     */
    private void testConnection() {
        String url = buildUrl();
        if (url.isEmpty() || url.equals("http://") || url.equals("https://")) {
            statusLabel.setText("请输入服务器地址");
            statusLabel.setForeground(JBColor.RED);
            return;
        }

        // 检查是否已输入凭证
        if (savedPassword == null || savedPassword.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(
                    getContentPane(),
                    "尚未输入凭证，可能导致连接失败。是否继续测试？",
                    "确认",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        statusLabel.setText("正在测试连接...");
        statusLabel.setForeground(JBColor.GRAY);
        testButton.setEnabled(false);
        stopButton.setEnabled(true);
        testingInProgress = true;

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "测试 TFS 连接", true) {
            private boolean success = false;
            private String errorMessage = null;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("正在连接到 " + url);
                indicator.setIndeterminate(true);

                try {
                    TfsConnectionService connectionService = ApplicationManager.getApplication().getService(TfsConnectionService.class);

                    // 创建临时上下文
                    ImportProjectContext testContext = new ImportProjectContext();
                    testContext.setServerUrl(url);
                    testContext.setCollectionName("DefaultCollection");
                    if (savedCredentialType == CredentialsDialog.CredentialType.PERSONAL_ACCESS_TOKEN) {
                        testContext.setAuthType(ImportProjectContext.AuthType.PAT);
                        testContext.setPassword(savedPassword);
                    } else {
                        testContext.setAuthType(ImportProjectContext.AuthType.NTLM);
                        testContext.setUsername(savedUsername);
                        testContext.setPassword(savedPassword);
                    }

                    success = connectionService.testConnection(testContext);
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    // 输出异常到控制台
                    e.printStackTrace();
                }
            }

            @Override
            public void onSuccess() {
                finishTesting();
                if (success) {
                    statusLabel.setText("连接成功!");
                    statusLabel.setForeground(new Color(0, 128, 0));
                    connectionTested = true;
                } else {
                    statusLabel.setText("连接失败，请检查配置和凭证");
                    statusLabel.setForeground(JBColor.RED);
                }
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                finishTesting();
                // 输出异常到控制台
                error.printStackTrace();
                statusLabel.setText("连接失败: " + error.getMessage());
                statusLabel.setForeground(JBColor.RED);
            }

            @Override
            public void onFinished() {
                finishTesting();
            }
        });
    }

    /**
     * 停止测试
     */
    private void stopTesting() {
        testingInProgress = false;
        ProgressManager.getInstance().getProgressIndicator().cancel();
        finishTesting();
        statusLabel.setText("已取消测试");
        statusLabel.setForeground(JBColor.GRAY);
    }

    /**
     * 完成测试后恢复按钮状态
     */
    private void finishTesting() {
        SwingUtilities.invokeLater(() -> {
            testButton.setEnabled(true);
            stopButton.setEnabled(false);
            testingInProgress = false;
        });
    }

    @Override
    protected void doOKAction() {
        String url = buildUrl();
        if (url.isEmpty() || url.equals("http://") || url.equals("https://")) {
            JOptionPane.showMessageDialog(getContentPane(), "请输入服务器地址", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!connectionTested) {
            int result = JOptionPane.showConfirmDialog(
                    getContentPane(),
                    "尚未测试连接，确定要保存吗？",
                    "确认",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        super.doOKAction();
    }

    /**
     * 获取服务器配置
     */
    @NotNull
    public TfsServerConfiguration.ServerConfig getServerConfig() {
        String url = buildUrl();
        String name = nameOrUrlField.getText().trim();

        TfsServerConfiguration.ServerConfig config = new TfsServerConfiguration.ServerConfig();
        config.setUrl(url);
        config.setName(name.isEmpty() ? url : name);
        config.setCollection("DefaultCollection");
        config.setAuthType(savedCredentialType.name());
        config.setUsername(savedUsername);
        config.setPassword(savedPassword);

        return config;
    }
}

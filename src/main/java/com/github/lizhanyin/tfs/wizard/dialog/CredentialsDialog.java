package com.github.lizhanyin.tfs.wizard.dialog;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * 输入凭证对话框
 */
public class CredentialsDialog extends DialogWrapper {

    public enum CredentialType {
        USER_PASSWORD("用户/密码"),
        PERSONAL_ACCESS_TOKEN("个人访问令牌-所有访问范围");

        private final String displayName;

        CredentialType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final String serverUrl;
    private JRadioButton userPasswordRadio;
    private JRadioButton patRadio;
    private JBTextField usernameField;
    private JBPasswordField passwordField;
    private JLabel usernameLabel;
    private JLabel passwordLabel;

    private CredentialType credentialType = CredentialType.USER_PASSWORD;
    private String username;
    private String password;

    public CredentialsDialog(@Nullable Component parent, @NotNull String serverUrl) {
        super(parent, false);
        this.serverUrl = serverUrl;
        setTitle("输入密码");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        FormBuilder builder = FormBuilder.createFormBuilder();

        // 提示信息
        builder.addComponent(new JLabel("输入 " + serverUrl + " 的密码"));
        builder.addSeparator();

        // 凭证类型选择
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        userPasswordRadio = new JRadioButton(CredentialType.USER_PASSWORD.toString());
        userPasswordRadio.setSelected(true);
        userPasswordRadio.addActionListener(e -> updateCredentialType());

        patRadio = new JRadioButton(CredentialType.PERSONAL_ACCESS_TOKEN.toString());
        patRadio.addActionListener(e -> updateCredentialType());

        ButtonGroup typeGroup = new ButtonGroup();
        typeGroup.add(userPasswordRadio);
        typeGroup.add(patRadio);

        typePanel.add(userPasswordRadio);
        typePanel.add(patRadio);
        builder.addLabeledComponent("凭证类型:", typePanel);

        // 用户名
        usernameLabel = new JLabel("用户名:");
        usernameField = new JBTextField();
        builder.addLabeledComponent(usernameLabel, usernameField);

        // 密码
        passwordLabel = new JLabel("密码:");
        passwordField = new JBPasswordField();
        builder.addLabeledComponent(passwordLabel, passwordField);

        JPanel panel = builder.getPanel();
        panel.setBorder(JBUI.Borders.empty(10));
        panel.setPreferredSize(new Dimension(450, panel.getPreferredSize().height));
        return panel;
    }

    /**
     * 更新凭证类型
     */
    private void updateCredentialType() {
        if (patRadio.isSelected()) {
            credentialType = CredentialType.PERSONAL_ACCESS_TOKEN;
            usernameField.setEnabled(false);
            usernameField.setText("");
        } else {
            credentialType = CredentialType.USER_PASSWORD;
            usernameField.setEnabled(true);
        }
    }

    @Override
    protected void doOKAction() {
        username = usernameField.getText().trim();
        password = new String(passwordField.getPassword());

        if (credentialType == CredentialType.USER_PASSWORD && username.isEmpty()) {
            JOptionPane.showMessageDialog(getContentPane(), "请输入用户名", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (password.isEmpty()) {
            String fieldName = credentialType == CredentialType.USER_PASSWORD ? "密码" : "令牌";
            JOptionPane.showMessageDialog(getContentPane(), "请输入" + fieldName, "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        super.doOKAction();
    }

    @Override
    protected String getHelpId() {
        return null;
    }

    /**
     * 获取凭证类型
     */
    @NotNull
    public CredentialType getCredentialType() {
        return credentialType;
    }

    /**
     * 获取用户名
     */
    @Nullable
    public String getUsername() {
        return credentialType == CredentialType.USER_PASSWORD ? username : null;
    }

    /**
     * 获取密码/令牌
     */
    @NotNull
    public String getPassword() {
        return password;
    }

    /**
     * 设置初始值
     */
    public void setCredentials(@Nullable CredentialType type, @Nullable String username, @Nullable String password) {
        if (type != null) {
            this.credentialType = type;
            if (type == CredentialType.PERSONAL_ACCESS_TOKEN) {
                patRadio.setSelected(true);
                userPasswordRadio.setSelected(false);
            } else {
                userPasswordRadio.setSelected(true);
                patRadio.setSelected(false);
            }
            updateCredentialType();
        }
        if (username != null) {
            this.usernameField.setText(username);
        }
        if (password != null) {
            this.passwordField.setText(password);
        }
    }
}

package com.github.lizhanyin.tfs.wizard.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.net.URI
import javax.swing.*

/**
 * 输入凭证对话框
 */
class CredentialsDialog(
    @Nullable parent: Component,
    @NotNull private val serverUrl: String
) : DialogWrapper(parent, false) {

    enum class CredentialType(val displayName: String) {
        USER_PASSWORD("用户/密码"),
        PERSONAL_ACCESS_TOKEN("个人访问令牌-所有访问范围");

        override fun toString(): String = displayName
    }

    private lateinit var userPasswordRadio: JRadioButton
    private lateinit var patRadio: JRadioButton
    private lateinit var usernameField: JBTextField
    private lateinit var passwordField: JBPasswordField

    private var credentialType: CredentialType = CredentialType.USER_PASSWORD
    private var username: String? = null
    private var password: String? = null

    init {
        title = "输入密码"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val builder = FormBuilder.createFormBuilder()

        // 提示信息
        builder.addComponent(JLabel("输入 $serverUrl 的密码"))
        builder.addSeparator()

        // 凭证类型选择
        val typePanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        userPasswordRadio = JRadioButton(CredentialType.USER_PASSWORD.displayName).apply {
            isSelected = true
            addActionListener { updateCredentialType() }
        }

        patRadio = JRadioButton(CredentialType.PERSONAL_ACCESS_TOKEN.displayName).apply {
            addActionListener { updateCredentialType() }
        }

        val typeGroup = ButtonGroup()
        typeGroup.add(userPasswordRadio)
        typeGroup.add(patRadio)

        typePanel.add(userPasswordRadio)
        typePanel.add(patRadio)
        builder.addLabeledComponent("凭证类型:", typePanel)

        // 用户名
        usernameField = JBTextField()
        builder.addLabeledComponent("用户名:", usernameField)

        // 密码
        passwordField = JBPasswordField()
        builder.addLabeledComponent("密码:", passwordField)

        val panel = builder.panel
        panel.border = JBUI.Borders.empty(10)
        panel.preferredSize = Dimension(450, panel.preferredSize.height)
        return panel
    }

    /**
     * 更新凭证类型
     */
    private fun updateCredentialType() {
        if (patRadio.isSelected) {
            credentialType = CredentialType.PERSONAL_ACCESS_TOKEN
            usernameField.isEnabled = false
            usernameField.text = ""
        } else {
            credentialType = CredentialType.USER_PASSWORD
            usernameField.isEnabled = true
        }
    }

    override fun doOKAction() {
        username = usernameField.text.trim()
        password = String(passwordField.password)

        if (credentialType == CredentialType.USER_PASSWORD && username.isNullOrEmpty()) {
            JOptionPane.showMessageDialog(contentPane, "请输入用户名", "提示", JOptionPane.WARNING_MESSAGE)
            return
        }

        if (password.isNullOrEmpty()) {
            val fieldName = if (credentialType == CredentialType.USER_PASSWORD) "密码" else "令牌"
            JOptionPane.showMessageDialog(contentPane, "请输入$fieldName", "提示", JOptionPane.WARNING_MESSAGE)
            return
        }

        super.doOKAction()
    }

    /**
     * 获取凭证类型
     */
    @NotNull
    fun getCredentialType(): CredentialType = credentialType

    /**
     * 获取用户名
     */
    @Nullable
    fun getUsername(): String? = if (credentialType == CredentialType.USER_PASSWORD) username else null

    /**
     * 获取密码/令牌
     */
    @NotNull
    fun getPassword(): String = password ?: ""

    /**
     * 设置初始值
     */
    fun setCredentials(type: CredentialType?, username: String?, password: String?) {
        type?.let {
            credentialType = it
            when (it) {
                CredentialType.PERSONAL_ACCESS_TOKEN -> {
                    patRadio.isSelected = true
                    userPasswordRadio.isSelected = false
                }
                CredentialType.USER_PASSWORD -> {
                    userPasswordRadio.isSelected = true
                    patRadio.isSelected = false
                }
            }
            updateCredentialType()
        }
        username?.let { usernameField.text = it }
        password?.let { passwordField.text = it }
    }

    /**
     * 设置初始凭证 (从 Credentials 对象)
     */
    fun setCredentials(credentials: com.microsoft.tfs.core.httpclient.Credentials?) {
        credentials?.let {
            when (it) {
                is com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials -> {
                    credentialType = CredentialType.USER_PASSWORD
                    userPasswordRadio.isSelected = true
                    patRadio.isSelected = false
                    usernameField.text = it.username ?: ""
                    // 密码无法从 Credentials 对象中获取，用户需要重新输入
                }
                is com.microsoft.tfs.core.httpclient.DefaultNTCredentials -> {
                    credentialType = CredentialType.USER_PASSWORD
                    userPasswordRadio.isSelected = true
                    patRadio.isSelected = false
                    usernameField.text = ""
                }
                else -> {
                    // 其他类型的凭证，默认使用 PAT
                    credentialType = CredentialType.PERSONAL_ACCESS_TOKEN
                    patRadio.isSelected = true
                    userPasswordRadio.isSelected = false
                }
            }
            updateCredentialType()
        }
    }

    /**
     * 设置是否允许保存密码
     */
    var allowSavePassword: Boolean = true

    /**
     * 设置错误消息
     */
    var errorMessage: String? = null
        set(value) {
            field = value
            // 可以在这里更新 UI 显示错误消息
        }
}

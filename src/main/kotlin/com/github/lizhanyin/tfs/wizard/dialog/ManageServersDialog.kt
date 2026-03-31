package com.github.lizhanyin.tfs.wizard.dialog

import com.github.lizhanyin.tfs.TfsBundle
import com.github.lizhanyin.tfs.settings.TfsServerConfiguration
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.AbstractTableModel

/**
 * 添加/移除 Team Foundation Server 对话框
 */
class ManageServersDialog(parent: Component) : DialogWrapper(parent, false) {

    private lateinit var tableModel: ServerTableModel
    private lateinit var serverTable: JBTable
    private lateinit var removeButton: JButton
    private lateinit var credentialsButton: JButton
    private lateinit var clearCredentialsButton: JButton

    private var selectedServer: TfsServerConfiguration.ServerConfig? = null

    init {
        title = TfsBundle.message("ManageServersDialog.title")
        setOKButtonText(TfsBundle.message("ManageServersDialog.button.close"))
        setCancelButtonText(TfsBundle.message("ManageServersDialog.button.close"))
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(0, 10))
        panel.border = JBUI.Borders.empty(10)
        panel.preferredSize = Dimension(600, 300)

        // 上方：标签独占一行
        val label = JLabel(TfsBundle.message("ManageServersDialog.listLabel"))
        panel.add(label, BorderLayout.NORTH)

        // 中间：左侧表格 + 右侧按钮
        val contentPanel = JPanel(BorderLayout(10, 0))

        // 创建表格
        tableModel = ServerTableModel()
        loadServers()

        serverTable = JBTable(tableModel)
        serverTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        serverTable.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                updateButtonState()
            }
        }

        // 双击编辑
        serverTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val row = serverTable.rowAtPoint(e.point)
                    if (row >= 0) {
                        editServer(row)
                    }
                }
            }
        })

        // 设置列宽
        serverTable.columnModel.getColumn(0).preferredWidth = 150
        serverTable.columnModel.getColumn(1).preferredWidth = 350

        val scrollPane = JBScrollPane(serverTable)
        contentPanel.add(scrollPane, BorderLayout.CENTER)

        // 右侧：按钮纵向排列
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.emptyLeft(10)
        }

        val addButton = JButton(TfsBundle.message("ManageServersDialog.button.add")).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener { addServer() }
        }
        buttonPanel.add(addButton)
        buttonPanel.add(Box.createVerticalStrut(5))

        credentialsButton = JButton(TfsBundle.message("ManageServersDialog.button.credentials")).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener { inputCredentials() }
            isEnabled = false
        }
        buttonPanel.add(credentialsButton)
        buttonPanel.add(Box.createVerticalStrut(5))

        clearCredentialsButton = JButton(TfsBundle.message("ManageServersDialog.button.clearCredentials")).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener { clearCredentials() }
            isEnabled = false
        }
        buttonPanel.add(clearCredentialsButton)
        buttonPanel.add(Box.createVerticalStrut(5))

        removeButton = JButton(TfsBundle.message("ManageServersDialog.button.remove")).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener { removeServer() }
            isEnabled = false
        }
        buttonPanel.add(removeButton)

        contentPanel.add(buttonPanel, BorderLayout.EAST)
        panel.add(contentPanel, BorderLayout.CENTER)

        return panel
    }

    override fun createSouthPanel(): JComponent {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT))
        panel.border = JBUI.Borders.emptyTop(10)

        val closeButton = JButton(TfsBundle.message("ManageServersDialog.button.close"))
        closeButton.addActionListener { doCancelAction() }
        panel.add(closeButton)

        return panel
    }

    override fun createActions(): Array<Action> = emptyArray()

    /**
     * 加载服务器列表
     */
    private fun loadServers() {
        val servers = TfsServerConfiguration.getInstance().servers
        tableModel.setServers(ArrayList(servers))
    }

    /**
     * 更新按钮状态
     */
    private fun updateButtonState() {
        val selectedRow = serverTable.selectedRow
        val hasSelection = selectedRow >= 0 && selectedRow < tableModel.rowCount

        removeButton.isEnabled = hasSelection
        credentialsButton.isEnabled = hasSelection
        clearCredentialsButton.isEnabled = hasSelection && hasCredentials(selectedRow)
    }

    /**
     * 检查是否有保存的凭证
     */
    private fun hasCredentials(row: Int): Boolean {
        if (row < 0 || row >= tableModel.rowCount) {
            return false
        }
        val server = tableModel.getServer(row)
        return server?.password?.isNotEmpty() == true
    }

    /**
     * 添加服务器
     */
    private fun addServer() {
        val dialog = AddServerDialog(contentPane, null)
        if (dialog.showAndGet()) {
            val newServer = dialog.getServerConfig()
            TfsServerConfiguration.getInstance().addServer(newServer)
            loadServers()

            // 选中新添加的服务器
            for (i in 0 until tableModel.rowCount) {
                if (tableModel.getServer(i)?.url == newServer.url) {
                    serverTable.setRowSelectionInterval(i, i)
                    break
                }
            }
        }
    }

    /**
     * 编辑服务器
     */
    private fun editServer(row: Int) {
        val server = tableModel.getServer(row) ?: return
        val dialog = AddServerDialog(contentPane, server)
        if (dialog.showAndGet()) {
            val updatedServer = dialog.getServerConfig()
            TfsServerConfiguration.getInstance().updateServer(server, updatedServer)
            loadServers()
        }
    }

    /**
     * 输入凭证
     */
    private fun inputCredentials() {
        val selectedRow = serverTable.selectedRow
        if (selectedRow < 0) return

        val server = tableModel.getServer(selectedRow) ?: return

        val dialog = CredentialsDialog(contentPane, server.url)
        var type = CredentialsDialog.CredentialType.USER_PASSWORD
        if (server.authType.isNotEmpty()) {
            try {
                type = CredentialsDialog.CredentialType.valueOf(server.authType)
            } catch (e: IllegalArgumentException) { }
        }

        dialog.setCredentials(type, server.username, server.password)

        if (dialog.showAndGet()) {
            server.authType = dialog.getCredentialType().name
            server.username = dialog.getUsername()
            server.password = dialog.getPassword()
            TfsServerConfiguration.getInstance().updateServer(server, server)
            loadServers()
            serverTable.setRowSelectionInterval(selectedRow, selectedRow)
        }
    }

    /**
     * 清除凭证
     */
    private fun clearCredentials() {
        val selectedRow = serverTable.selectedRow
        if (selectedRow < 0) return

        val server = tableModel.getServer(selectedRow) ?: return

        val result = JOptionPane.showConfirmDialog(
            contentPane,
            TfsBundle.message("ManageServersDialog.confirm.clearCredentials", server.name),
            TfsBundle.message("ManageServersDialog.confirm.clearCredentials.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        )

        if (result == JOptionPane.YES_OPTION) {
            server.username = null
            server.password = null
            server.authType = "NTLM"
            TfsServerConfiguration.getInstance().updateServer(server, server)
            loadServers()
            serverTable.setRowSelectionInterval(selectedRow, selectedRow)
            updateButtonState()
        }
    }

    /**
     * 移除服务器
     */
    private fun removeServer() {
        val selectedRow = serverTable.selectedRow
        if (selectedRow < 0) return

        val server = tableModel.getServer(selectedRow) ?: return

        val result = JOptionPane.showConfirmDialog(
            contentPane,
            TfsBundle.message("ManageServersDialog.confirm.remove", server.name),
            TfsBundle.message("ManageServersDialog.confirm.remove.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )

        if (result == JOptionPane.YES_OPTION) {
            TfsServerConfiguration.getInstance().removeServer(server)
            loadServers()
            updateButtonState()
        }
    }

    /**
     * 获取选中的服务器
     */
    @Nullable
    fun getSelectedServer(): TfsServerConfiguration.ServerConfig? = selectedServer

    /**
     * 服务器表格模型
     */
    private class ServerTableModel : AbstractTableModel() {
        private val columnNames = arrayOf(
            TfsBundle.message("ManageServersDialog.column.name"),
            TfsBundle.message("ManageServersDialog.column.server"),
            TfsBundle.message("ManageServersDialog.column.credentials")
        )
        private var servers: List<TfsServerConfiguration.ServerConfig> = emptyList()

        fun setServers(servers: List<TfsServerConfiguration.ServerConfig>) {
            this.servers = servers
            fireTableDataChanged()
        }

        fun getServer(row: Int): TfsServerConfiguration.ServerConfig? =
            if (row in servers.indices) servers[row] else null

        override fun getRowCount(): Int = servers.size

        override fun getColumnCount(): Int = columnNames.size

        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
            val server = servers[rowIndex]
            return when (columnIndex) {
                0 -> server.name.ifEmpty { server.url }
                1 -> server.url
                2 -> if (!server.password.isNullOrEmpty()) TfsBundle.message("ManageServersDialog.status.saved") else "-"
                else -> null
            }
        }
    }
}

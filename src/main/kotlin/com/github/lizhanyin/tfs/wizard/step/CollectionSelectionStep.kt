package com.github.lizhanyin.tfs.wizard.step

import com.github.lizhanyin.tfs.TfsBundle
import com.github.lizhanyin.tfs.client.catalog.CrossCollectionProjectInfo
import com.github.lizhanyin.tfs.services.TfsConnectionService
import com.github.lizhanyin.tfs.wizard.ImportProjectContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.microsoft.tfs.core.util.ServerURIUtils
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer


/**
 * 团队项目选择步骤（第二步）
 *
 * UI 布局（参考 CLAUDE.md）：
 * ------------------------------------------
 * │  从 Team Foundation Server 导入项目  [X ]│
 * │----------------------------------------│
 * │  团队项目选择                             │
 * │    从列表中选择一个团队项目                 │
 * │----------------------------------------│
 * │  [input                    ] (tip: 键入内容以筛选列表) │
 * │  [table  head               ]          │
 * │  [table  row1               ]          │
 * │  [table  row2               ]          │
 * │  [table  row3               ]          │
 * │  [img_2x2 ]  [a ] (tfs url)            │
 * │             [label ] (tfs username)     │
 * │  [label ] (tip: 若要更改服务器，请使用‘返回’按钮)│
 * │  [进度条 ]                               │
 * │----------------------------------------│
 * │ [上一步 ] [下一步 ]     [完成](禁用) [放弃 ]│
 * -----------------------------------------│
 */
class CollectionSelectionStep(context: ImportProjectContext) :
    AbstractWizardStep(STEP_ID, TfsBundle.message("CollectionSelectionStep.title"), context, TfsBundle.message("CollectionSelectionStep.description")) {

    companion object {
        private const val STEP_ID = "team-project-selection"
    }

    private val VSTS_IMAGE_LOC: String = "/images/common/vso-account.png" //$NON-NLS-1$
    private val TFS_IMAGE_LOC: String = "/images/common/windows-account.png" //$NON-NLS-1$

    private lateinit var filterField: com.intellij.ui.components.JBTextField
    private lateinit var table: JBTable
    private lateinit var tableModel: TeamProjectTableModel
    private lateinit var statusLabel: JBLabel
    private lateinit var serverUrlLabel: JLabel
    private lateinit var userNameLabel: JLabel
    private lateinit var tipLabel: JBLabel

    private val allProjects = mutableListOf<CrossCollectionProjectInfo>()

    override fun buildComponent(): JComponent {

        // 标题区域（FormBuilder）
        val builder = FormBuilder.createFormBuilder()
        builder.addSeparator()
        val formPanel = builder.panel

        // 内容区域（BorderLayout，可自动填充）
        val contentPanel = JPanel(BorderLayout(0, JBUI.scale(8)))
        contentPanel.border = JBUI.Borders.empty(10)

        // 顶部：筛选输入框
        contentPanel.add(createFilterPanel(), BorderLayout.NORTH)

        // 中间：项目表格
        contentPanel.add(createTablePanel(), BorderLayout.CENTER)

        // 底部：服务器信息 + 提示
        contentPanel.add(createBottomPanel(), BorderLayout.SOUTH)

        // 整体：标题在顶部，内容填充剩余空间
        val panel = JPanel(BorderLayout())
        panel.add(formPanel, BorderLayout.NORTH)
        panel.add(contentPanel, BorderLayout.CENTER)

        // 自动加载团队项目
        loadTeamProjects()

        return panel
    }

    // ==================== UI 构建 ====================

    /**
     * 创建筛选输入框面板
     */
    private fun createFilterPanel(): JComponent {
        val panel = JPanel(BorderLayout(4, 0))

        filterField = com.intellij.ui.components.JBTextField()
        filterField.emptyText.text = TfsBundle.message("CollectionSelectionStep.filter.placeholder")
        filterField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                tableModel.filter(filterField.text.trim())
            }
        })

        panel.add(filterField, BorderLayout.CENTER)
        return panel
    }

    /**
     * 创建项目表格面板
     */
    private fun createTablePanel(): JComponent {
        tableModel = TeamProjectTableModel()
        table = JBTable(tableModel)

        // 基本设置
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        table.setShowGrid(false)
        table.intercellSpacing = Dimension(0, 0)
        table.rowHeight = JBUI.scale(24)
        table.columnModel.getColumn(0).preferredWidth = 250
        table.columnModel.getColumn(1).preferredWidth = 150

        // 单元格渲染器
        table.setDefaultRenderer(Any::class.java, object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable?, value: Any?, selected: Boolean, focus: Boolean, row: Int, column: Int
            ): Component {
                val c = super.getTableCellRendererComponent(table, value, selected, focus, row, column)
                border = JBUI.Borders.empty(2, 6)
                return c
            }
        })

        // 双击选择
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && table.selectedRow >= 0) {
                    // 双击可触发向导的"下一步"
                }
            }
        })

        // 状态标签（覆盖在表格区域）
        statusLabel = JBLabel(TfsBundle.message("CollectionSelectionStep.status.loading"))
        statusLabel.foreground = JBColor.gray

        val scrollPane = JBScrollPane(table)
        scrollPane.preferredSize = Dimension(400, 200)

        // 使用叠加面板，加载中时显示状态，有数据时显示表格
        val panel = JPanel(BorderLayout())
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    /**
     * 创建底部信息面板（服务器URL、用户名、提示）
     */
    private fun createBottomPanel(): JComponent {
        val panel = JPanel(BorderLayout(0, JBUI.scale(4)))
        panel.border = JBUI.Borders.emptyTop(8)

        // 服务器信息区域
        val infoPanel = JPanel(BorderLayout(4, 2))

        val uri: URI = URI(context.serverUrl ?: "");
        val isVSTS = ServerURIUtils.isHosted(uri)
        val imageLocation: String = if (isVSTS) VSTS_IMAGE_LOC else TFS_IMAGE_LOC

        // 左侧：2x2 图标
        val iconUrl = javaClass.getResource(imageLocation)
        if (iconUrl != null) {
            val icon = javax.swing.ImageIcon(iconUrl)
            val scaledIcon = icon.image.getScaledInstance(JBUI.scale(32), JBUI.scale(32), java.awt.Image.SCALE_SMOOTH)
            val iconLabel = JLabel(javax.swing.ImageIcon(scaledIcon))
            iconLabel.border = JBUI.Borders.emptyRight(4)
            infoPanel.add(iconLabel, BorderLayout.WEST)
        }

        serverUrlLabel = JLabel()
        serverUrlLabel.foreground = JBColor.GRAY
        userNameLabel = JLabel()
        userNameLabel.foreground = JBColor.GRAY
        val infoContent = JPanel(BorderLayout(0, 0))
        // 缩进对齐图标
        infoContent.add(serverUrlLabel, BorderLayout.NORTH)
        infoContent.add(userNameLabel, BorderLayout.SOUTH)

        infoPanel.add(infoContent, BorderLayout.CENTER)
        panel.add(infoPanel, BorderLayout.NORTH)

        // 提示标签
        tipLabel = JBLabel(TfsBundle.message("CollectionSelectionStep.tip.changeServer"))
        tipLabel.foreground = JBColor.gray
        panel.add(tipLabel, BorderLayout.SOUTH)

        // 初始化显示
        updateServerInfo()

        return panel
    }

    // ==================== 数据加载 ====================

    /**
     * 更新底部服务器信息
     */
    private fun updateServerInfo() {
        val serverText = buildString {
            append(TfsBundle.message("CollectionSelectionStep.label.serverUrl"))
            append(" ")
            append(context.serverUrl ?: "")
            val collection = context.collection
            if (collection != null) {
                append("/").append(collection.collectionName)
            }
        }
        if (::serverUrlLabel.isInitialized) {
            serverUrlLabel.text = serverText
        }

        val userText = buildString {
            append(TfsBundle.message("CollectionSelectionStep.label.userName"))
            append(" ")
            val user = context.username
            if (!user.isNullOrEmpty()) {
                val domain = context.domain
                if (!domain.isNullOrEmpty()) {
                    append(domain).append("\\")
                }
                append(user)
            } else {
                append("-")
            }
        }
        if (::userNameLabel.isInitialized) {
            userNameLabel.text = userText
        }
    }

    /**
     * 加载团队项目列表（进入步骤时自动调用）
     */
    fun loadTeamProjects() {
        context.wizardController?.showProgress("正在加载团队项目...");

        if (!context.isServerConfigured()) {
            statusLabel.text = TfsBundle.message("CollectionSelectionStep.error.configureServerFirst")
            statusLabel.foreground = JBColor.RED
            context.wizardController?.hideProgress()
            return
        }

        statusLabel.text = TfsBundle.message("CollectionSelectionStep.status.loading")
        statusLabel.foreground = JBColor.gray
        table.isEnabled = false
        filterField.isEnabled = false

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            null, TfsBundle.message("CollectionSelectionStep.progress.loading"), true
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = TfsBundle.message("CollectionSelectionStep.progress.connecting")
                indicator.isIndeterminate = true

                try {
                    val connectionService = ApplicationManager.getApplication()
                        .getService(TfsConnectionService::class.java)
                    val projects = connectionService.getTeamProjects(context)

                    SwingUtilities.invokeLater {
                        allProjects.clear()
                        allProjects.addAll(projects.sortedBy { it.name })
                        tableModel.setData(allProjects)
                        tableModel.filter(filterField.text.trim())

                        if (projects.isEmpty()) {
                            statusLabel.text = TfsBundle.message("CollectionSelectionStep.status.noResults")
                            statusLabel.foreground = JBColor.ORANGE
                        } else {
                            statusLabel.text = TfsBundle.message(
                                "CollectionSelectionStep.status.loaded", projects.size
                            )
                            statusLabel.foreground = JBColor.GRAY
                        }

                        table.isEnabled = true
                        filterField.isEnabled = true
                        // 默认选中第一行
                        if (table.rowCount > 0) {
                            table.setRowSelectionInterval(0, 0)
                        }
                    }

                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        statusLabel.text = TfsBundle.message("CollectionSelectionStep.error.loadingFailed", e.message ?: "")
                        statusLabel.foreground = JBColor.RED
                        table.isEnabled = true
                        filterField.isEnabled = true
                    }
                } finally {
                    context.wizardController?.hideProgress()
                }
            }
        })
    }

    // ==================== 表格模型 ====================

    /**
     * 团队项目表格模型，支持筛选
     */
    private inner class TeamProjectTableModel : AbstractTableModel() {
        private val columnNames = arrayOf(
            TfsBundle.message("CollectionSelectionStep.table.column.name"),
            TfsBundle.message("CollectionSelectionStep.table.column.collection")
        )
        private var allData: List<CrossCollectionProjectInfo> = emptyList()
        private var filteredData: List<CrossCollectionProjectInfo> = emptyList()

        fun setData(data: List<CrossCollectionProjectInfo>) {
            allData = data.toList()
            filteredData = allData
            fireTableDataChanged()
        }

        fun filter(text: String) {
            filteredData = if (text.isEmpty()) {
                allData
            } else {
                allData.filter { it.name.contains(text, ignoreCase = true) }
            }
            fireTableDataChanged()
        }

        fun getSelectedItem(): CrossCollectionProjectInfo? {
            val row = table.selectedRow
            return if (row in filteredData.indices) filteredData[row] else null
        }

        override fun getRowCount(): Int = filteredData.size

        override fun getColumnCount(): Int = columnNames.size

        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val project = filteredData[rowIndex]
            return when (columnIndex) {
                0 -> project.name
                1 -> project.collectionName
                else -> ""
            }
        }
    }

    // ==================== 步骤接口 ====================

    override fun isComplete(): Boolean = table.selectedRow >= 0

    override fun getPreferredFocusedComponent(): JComponent = if (::filterField.isInitialized) filterField else table

    override fun onFinish(): Boolean {
        val selected = tableModel.getSelectedItem()
        if (selected != null) {
            context.collection = selected
        }
        return true
    }
}

package com.github.lizhanyin.tfs.wizard.step

import com.github.lizhanyin.tfs.TfsBundle
import com.github.lizhanyin.tfs.client.codemarker.CodeMarker
import com.github.lizhanyin.tfs.client.codemarker.CodeMarkerDispatch
import com.github.lizhanyin.tfs.client.ui.vc.serveritem.TypedServerItem
import com.github.lizhanyin.tfs.services.TfsConnectionService
import com.github.lizhanyin.tfs.vc.serveritem.ServerItemLabelProvider
import com.github.lizhanyin.tfs.wizard.ImportProjectContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

/**
 * 项目选择步骤（第四步）
 *
 * UI 布局（参考 CLAUDE.md）：
 * ------------------------------------------
 * |  项目选择                                |
 * |    从 Team Foundation Server 导入项目    |
 * |----------------------------------------|
 * | 导入项目:                                |
 * | [tree] (ip\集合名称）                    |
 * |    root  (项目名）                       |
 * |      文件夹1                            |
 * |      文件夹2                            |
 * | [label] (已选择 {0} 个项目)              |
 * | [checkbox] 对选定项目中的文件执行强制获取  |
 * |----------------------------------------|
 * | [上一步] [下一步]  [完成](禁用) [放弃]    |
 * ------------------------------------------
 */
class ProjectSelectionStep(context: ImportProjectContext) :
    AbstractWizardStep(STEP_ID, TfsBundle.message("ProjectSelectionStep.title"), context, TfsBundle.message("ProjectSelectionStep.description")) {

    companion object {
        private const val STEP_ID = "project-selection"
        private val CODEMARKER_CHILD_NODES_FETCH_START =
        CodeMarker("com.github.lizhanyin.tfs.wizard.step.ProjectSelectionStep#childNodesFetchStart"); //$NON-NLS-1$
        private val CODEMARKER_CHILD_NODES_FETCH_COMPLETE =
            CodeMarker("com.github.lizhanyin.tfs.wizard.step.ProjectSelectionStep#childNodesFetchComplete"); //$NON-NLS-1$
    }

    private lateinit var projectTree: Tree
    private lateinit var rootTreeNode: DefaultMutableTreeNode
    private lateinit var selectedCountLabel: JBLabel
    private lateinit var forceGetCheckbox: JBCheckBox
    private lateinit var statusLabel: JBLabel
    private lateinit var refreshButton: JButton

    private var labelProvider: ServerItemLabelProvider = ServerItemLabelProvider()

    private val projects = mutableListOf<ProjectItem>()

    override fun buildComponent(): JComponent {
        val contentPanel = JPanel(BorderLayout(0, JBUI.scale(8)))
        contentPanel.border = JBUI.Borders.empty(10)

        // 中间：项目树 + 状态
        contentPanel.add(createTreePanel(), BorderLayout.CENTER)

        // 底部：选择计数 + 强制获取复选框
        contentPanel.add(createBottomPanel(), BorderLayout.SOUTH)

        // 初始加载
        loadProjects()

        return contentPanel
    }

    // ==================== UI 构建 ====================

    private fun createTreePanel(): JComponent {
        val panel = JPanel(BorderLayout(0, JBUI.scale(4)))

        // 树标题 + 刷新按钮
        val headerPanel = JPanel(BorderLayout())
        headerPanel.add(JBLabel(TfsBundle.message("ProjectSelectionStep.label.selectProjects")), BorderLayout.WEST)

        refreshButton = JButton(TfsBundle.message("ProjectSelectionStep.button.refresh"))
        refreshButton.addActionListener { loadProjects() }
        headerPanel.add(refreshButton, BorderLayout.EAST)
        panel.add(headerPanel, BorderLayout.NORTH)

        // 创建项目树
        createProjectTree()
        val treeScrollPane = JBScrollPane(projectTree)
        treeScrollPane.preferredSize = Dimension(500, 300)
        panel.add(treeScrollPane, BorderLayout.CENTER)

        // 状态标签
        statusLabel = JBLabel(TfsBundle.message("ProjectSelectionStep.status.loading"))
        statusLabel.foreground = JBColor.gray
        panel.add(statusLabel, BorderLayout.SOUTH)

        return panel
    }

    private fun createBottomPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.emptyTop(8)

        selectedCountLabel = JBLabel(TfsBundle.message("ProjectSelectionStep.status.selectedNone"))
        panel.add(selectedCountLabel, BorderLayout.WEST)

        forceGetCheckbox = JBCheckBox(TfsBundle.message("ProjectSelectionStep.checkbox.forceGet"))
        panel.add(forceGetCheckbox, BorderLayout.EAST)

        return panel
    }

    private fun createProjectTree() {
        rootTreeNode = DefaultMutableTreeNode()

        projectTree = Tree(rootTreeNode)
        projectTree.isRootVisible = false
        projectTree.setShowsRootHandles(true)
        projectTree.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
        projectTree.setCellRenderer { tree, value, selected, expanded, leaf, row, hasFocus ->
            val label = javax.swing.JLabel()
            if (value is DefaultMutableTreeNode) {
                val userObject = value.userObject
                if (userObject is ProjectItem) {
                    label.icon = labelProvider.getImage(userObject.node)
                    label.text = labelProvider.getText(userObject.node)
                }
            }
            label
        }

        projectTree.addTreeSelectionListener(TreeSelectionListener { updateSelectedCount() })
    }

    // ==================== 数据加载 ====================

    private fun loadProjects() {
        if (!context.isTeamProjectSelected()) {
            statusLabel.text = TfsBundle.message("ProjectSelectionStep.error.selectTeamProjectFirst")
            statusLabel.foreground = JBColor.RED
            return
        }

        statusLabel.text = TfsBundle.message("ProjectSelectionStep.status.loading")
        statusLabel.foreground = JBColor.gray
        refreshButton.isEnabled = false

        ProgressManager.getInstance().run(object : Task.Backgroundable(null, TfsBundle.message("ProjectSelectionStep.progress.loading"), false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = TfsBundle.message("ProjectSelectionStep.progress.fetching")
                indicator.isIndeterminate = true

                try {
                    val projectItems = mutableListOf<ProjectItem>()

                    val connectionService = ApplicationManager.getApplication().getService(TfsConnectionService::class.java)
                    // 根节点 ServerItemType
                    val itemSource = connectionService.getChildItems(context)
                    labelProvider.setServerItemSource(itemSource)

                    val root = ProjectItem(TypedServerItem.ROOT, labelProvider);
                    projectItems.add(root)

                    // 子节点 TypedServerItem
                    CodeMarkerDispatch.dispatch(CODEMARKER_CHILD_NODES_FETCH_START)
                    val children = itemSource.getChildren(root.node);

                    CodeMarkerDispatch.dispatch(CODEMARKER_CHILD_NODES_FETCH_COMPLETE)

                    if (children.size == 0) {
                        return
                    }

                    loadChildren(root, indicator, root.node.serverPath)

                    SwingUtilities.invokeLater {
                        projects.clear()
                        projects.addAll(projectItems)
                        buildProjectTree()

                        if (projects.isEmpty()) {
                            statusLabel.text = TfsBundle.message("ProjectSelectionStep.status.noResults")
                            statusLabel.foreground = JBColor.ORANGE
                        } else {
                            statusLabel.text = TfsBundle.message("ProjectSelectionStep.status.loaded", projects.size)
                            statusLabel.foreground = JBColor.GRAY
                        }
                        refreshButton.isEnabled = true
                        updateSelectedCount()
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        Messages.showErrorDialog(
                            TfsBundle.message("ProjectSelectionStep.error.loadingFailed", e.message ?: ""),
                            TfsBundle.message("ProjectSelectionStep.title")
                        )
                        refreshButton.isEnabled = true
                    }
                }
            }
        })
    }

    private fun loadChildren(
        parent: ProjectItem,
        indicator: ProgressIndicator,
        rootPath: String
    ) {
        if (indicator.isCanceled) return

        try {
            val children = labelProvider.getServerItemSource().getChildren(parent.node)

            for (child in children) {
                val childItem = ProjectItem(child, labelProvider)
                parent.addChild(childItem)
                // 限制递归深度（相对于根路径最多 4 层）
                val depth = child.serverPath.removePrefix(rootPath).count { it == '/' }
                if (depth < 1) {
                    loadChildren(childItem, indicator, rootPath)
                }
            }
        } catch (_: Exception) {
            // 忽略单个文件夹加载失败
        }
    }

    // ==================== 树操作 ====================

    private fun buildProjectTree() {
        rootTreeNode.removeAllChildren()

        for (item in projects) {
            val node = createTreeNode(item)
            rootTreeNode.add(node)
        }

        (projectTree.model as DefaultTreeModel).reload()
    }

    private fun createTreeNode(item: ProjectItem): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode(item)

        item.children?.forEach { child ->
            node.add(createTreeNode(child))
        }

        return node
    }

    private fun updateSelectedCount() {
        val count = getSelectedPaths().size
        selectedCountLabel.text = if (count == 0) {
            TfsBundle.message("ProjectSelectionStep.status.selectedNone")
        } else {
            TfsBundle.message("ProjectSelectionStep.status.selectedCount", count)
        }
    }

    // ==================== 选择相关 ====================

    private fun getSelectedPaths(): List<String> {
        return projectTree.selectionPaths?.mapNotNull { path ->
            val node = path.lastPathComponent as? DefaultMutableTreeNode
            val item = node?.userObject as? ProjectItem
            item?.node?.serverPath
        } ?: emptyList()
    }

    // ==================== 步骤接口 ====================

    override fun isComplete(): Boolean = getSelectedPaths().isNotEmpty()

    override fun getPreferredFocusedComponent(): JComponent = projectTree

    override fun onFinish(): Boolean {
        val selectedPaths = getSelectedPaths()
        context.selectedProjects.clear()
        context.selectedProjects.addAll(selectedPaths)
        context.forceGetLatest = forceGetCheckbox.isSelected
        return true
    }

    /**
     * 项目项
     */
    private class ProjectItem(
        val node: TypedServerItem,
        val labelProvider: ServerItemLabelProvider
    ) {
        var children: MutableList<ProjectItem>? = null

        fun addChild(child: ProjectItem) {
            if (children == null) {
                children = mutableListOf()
            }
            children!!.add(child)
        }

        override fun toString(): String = labelProvider.getText(node)
    }
}

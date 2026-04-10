package com.github.lizhanyin.tfs.wizard.step

import com.github.lizhanyin.tfs.TfsBundle
import com.github.lizhanyin.tfs.client.codemarker.CodeMarker
import com.github.lizhanyin.tfs.client.codemarker.CodeMarkerDispatch
import com.github.lizhanyin.tfs.client.ui.vc.serveritem.ServerItemSource
import com.github.lizhanyin.tfs.client.ui.vc.serveritem.ServerItemType
import com.github.lizhanyin.tfs.client.ui.vc.serveritem.TypedServerItem
import com.github.lizhanyin.tfs.services.TfsConnectionService
import com.github.lizhanyin.tfs.vc.serveritem.ServerItemLabelProvider
import com.github.lizhanyin.tfs.wizard.ImportProjectContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
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
 * | [ tree] (ip\集合名称）                    |
 * |    root  (项目名）                       |
 * |      文件夹1                            |
 * |      文件夹2                            |
 * | [ label] (已选择 {0} 个项目)              |
 * | [ checkbox] 对选定项目中的文件执行强制获取  |
 * |----------------------------------------|
 * | [ 上一步] [ 下一步]  [完成](禁用) [ 放弃]    |
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
    private lateinit var statusLabel: JBLabel
    private lateinit var refreshButton: JButton

    // 参数
    private lateinit var forceGetCheckbox: JBCheckBox

    //
    private var labelProvider: ServerItemLabelProvider = ServerItemLabelProvider()
    private var contentProvider: ContentProvider = ContentProvider()

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

        projectTree.addTreeSelectionListener(TreeSelectionListener { validateSelection() })

        // 展开事件：懒加载子节点
        projectTree.addTreeExpansionListener(object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent) {
                val treeNode = event.path.lastPathComponent as? DefaultMutableTreeNode ?: return
                val projectItem = treeNode.userObject as? ProjectItem ?: return
                if (!projectItem.loaded) {
                    loadChildrenAsync(treeNode, projectItem, event.path)
                }
            }
            override fun treeCollapsed(event: TreeExpansionEvent) {}
        })

        // 双击事件：展开/加载子节点
        projectTree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount != 2) return
                val path = projectTree.getPathForLocation(e.x, e.y) ?: return
                val treeNode = path.lastPathComponent as? DefaultMutableTreeNode ?: return
                val projectItem = treeNode.userObject as? ProjectItem ?: return
                if (!projectItem.loaded && projectItem.node.type != ServerItemType.FILE) {
                    loadChildrenAsync(treeNode, projectItem, path)
                }
            }
        })
    }

    private fun cleanStatus(){
        showStatus(JBColor.gray, null)
    }

    private fun showStatus(color: JBColor, key: String?, vararg params: Any){
        if (key == null) {
            statusLabel.text = ""
        } else {
            val text = TfsBundle.message(key, *params)
            statusLabel.text = "<html><body style='width:480px'>$text</body></html>"
        }
        statusLabel.foreground = color
    }

    // ==================== 数据加载 ====================

    private fun loadProjects() {
        if (!context.isTeamProjectSelected()) {
            showStatus(JBColor.RED, "ProjectSelectionStep.error.selectTeamProjectFirst")
            return
        }
        showStatus(JBColor.gray, "ProjectSelectionStep.status.loading")
        refreshButton.isEnabled = false

        ProgressManager.getInstance().run(object : Task.Backgroundable(null, TfsBundle.message("ProjectSelectionStep.progress.loading"), false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = TfsBundle.message("ProjectSelectionStep.progress.fetching")
                indicator.isIndeterminate = true

                try {
                    val connectionService = ApplicationManager.getApplication().getService(TfsConnectionService::class.java)
                    val itemSource = connectionService.getChildItems(context)
                    labelProvider.setServerItemSource(itemSource)

                    val rootNodes = contentProvider.getElements(itemSource)
                    if (rootNodes.isEmpty()) return

                    val rootItem = ProjectItem(rootNodes[0], labelProvider)

                    // 仅加载根节点的直接子项（团队项目），不递归
                    CodeMarkerDispatch.dispatch(CODEMARKER_CHILD_NODES_FETCH_START)
                    val rootChildren = itemSource.getChildren(rootItem.node)
                    CodeMarkerDispatch.dispatch(CODEMARKER_CHILD_NODES_FETCH_COMPLETE)

                    for (child in rootChildren) {
                        if (contentProvider.hasChildren(child)) {
                            rootItem.addChild(ProjectItem(child, labelProvider))
                        }
                    }
                    rootItem.loaded = true

                    val projectItems = mutableListOf(rootItem)

                    SwingUtilities.invokeLater {
                        projects.clear()
                        projects.addAll(projectItems)
                        buildProjectTree()

                        if (projects.isEmpty()) {
                            showStatus(JBColor.ORANGE, "ProjectSelectionStep.status.noResults")
                        } else {
                            showStatus(JBColor.GRAY, "ProjectSelectionStep.status.loaded", projects.size)
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

    /**
     * 异步加载子节点（懒加载）
     */
    private fun loadChildrenAsync(treeNode: DefaultMutableTreeNode, projectItem: ProjectItem, treePath: TreePath) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(null, TfsBundle.message("ProjectSelectionStep.progress.loading"), true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = TfsBundle.message("ProjectSelectionStep.progress.fetching")
                indicator.isIndeterminate = true

                try {
                    CodeMarkerDispatch.dispatch(CODEMARKER_CHILD_NODES_FETCH_START)
                    val children = contentProvider.getChildren(projectItem.node)
                    CodeMarkerDispatch.dispatch(CODEMARKER_CHILD_NODES_FETCH_COMPLETE)

                    if (children.isNullOrEmpty()) {
                        projectItem.loaded = true
                        SwingUtilities.invokeLater {
                            treeNode.removeAllChildren()
                            (projectTree.model as DefaultTreeModel).nodeStructureChanged(treeNode)
                        }
                        return
                    }

                    val childItems = children
                        .filter { contentProvider.hasChildren(it) }
                        .map { ProjectItem(it, labelProvider) }
                        .toMutableList()

                    SwingUtilities.invokeLater {
                        treeNode.removeAllChildren()
                        for (childItem in childItems) {
                            treeNode.add(createTreeNode(childItem))
                        }
                        projectItem.children = childItems
                        projectItem.loaded = true
                        (projectTree.model as DefaultTreeModel).nodeStructureChanged(treeNode)
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        Messages.showErrorDialog(
                            TfsBundle.message("ProjectSelectionStep.error.loadingFailed", e.message ?: ""),
                            TfsBundle.message("ProjectSelectionStep.title")
                        )
                    }
                }
            }
        })
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

        if (item.loaded) {
            // 已加载：用实际子节点填充
            item.children?.forEach { child ->
                node.add(createTreeNode(child))
            }
        } else if (item.node.type != ServerItemType.FILE) {
            // 未加载的文件夹：添加占位节点以显示展开箭头
            node.add(DefaultMutableTreeNode())
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

    private var validating = false

    private fun validateSelection() {
        cleanStatus()

        if (validating) return
        validating = true
        try {
            val selectionPaths = projectTree.selectionPaths ?: run {
                updateSelectedCount()
                return
            }

            val pathsToKeep = selectionPaths.toMutableList()
            var error: Boolean = false

            // 检查1: 不能选择根节点
            val iter = pathsToKeep.iterator()
            while (iter.hasNext()) {
                val item = getProjectItem(iter.next())
                if (item != null && item.node.type == ServerItemType.ROOT) {
                    showStatus(JBColor.RED, "ImportFolderValidator.CannotImportServerRoot")
                    error = true
                }
            }

            // 检查2: 已选节点的祖先不能被选中
            if (!error && pathsToKeep.size > 1) {
                val toRemove = mutableSetOf<TreePath>()
                for (i in pathsToKeep.indices) {
                    for (j in pathsToKeep.indices) {
                        if (i == j || toRemove.contains(pathsToKeep[i])) continue
                        // pathsToKeep[i] 是 pathsToKeep[j] 的祖先，需要移除
                        if (pathsToKeep[i].isDescendant(pathsToKeep[j])) {
                            toRemove.add(pathsToKeep[i])
                        }
                    }
                }
                if (toRemove.isNotEmpty()) {
                    val ancestorItem = getProjectItem(toRemove.first())
                    showStatus(JBColor.RED, "ImportFolderCollection.SelectedPathIsAncestorFormat", ancestorItem?.node?.serverPath ?: "", toRemove)
                }
            }

            if (!error) {
                projectTree.selectionPaths = pathsToKeep.toTypedArray()
            }

            updateSelectedCount()
        } finally {
            validating = false
        }
    }

    private fun getProjectItem(path: TreePath): ProjectItem? {
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return null
        return node.userObject as? ProjectItem
    }

    private fun getSelectedPaths(): List<String> {
        return projectTree.selectionPaths?.mapNotNull { path ->
            val item = getProjectItem(path)
            if (item != null && item.node.type != ServerItemType.ROOT) {
                item.node.serverPath
            } else null
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
        var loaded: Boolean = false

        fun addChild(child: ProjectItem) {
            if (children == null) {
                children = mutableListOf()
            }
            children!!.add(child)
        }

        override fun toString(): String = labelProvider.getText(node)
    }

    private class ContentProvider {
        private var source: ServerItemSource? = null
        var visibleTypeFlags: Int = ServerItemType.ROOT.flag or
                ServerItemType.TEAM_PROJECT.flag or
                ServerItemType.FOLDER.flag

        fun getChildren(parentElement: TypedServerItem): Array<TypedServerItem>? {
            CodeMarkerDispatch.dispatch(CODEMARKER_CHILD_NODES_FETCH_START)

            val parent = parentElement as TypedServerItem
            val children = source!!.getChildren(parent)

            CodeMarkerDispatch.dispatch(CODEMARKER_CHILD_NODES_FETCH_COMPLETE)

            if (children.isEmpty()) {
                return null
            }

            return children
        }

        fun hasChildren(element: TypedServerItem): Boolean {
            val item = element as TypedServerItem

            if (item.type == ServerItemType.FILE) {
                return false
            }

            if (item.type == ServerItemType.GIT_REPOSITORY) {
                return visibleTypeFlags and ServerItemType.GIT_BRANCH.flag != 0
            }

            return true
        }

        fun getElements(inputElement: ServerItemSource?): Array<TypedServerItem> {
            source = inputElement as ServerItemSource?

            return if (source == null) {
                emptyArray()
            } else {
                arrayOf(TypedServerItem.ROOT)
            }
        }

        fun getParent(element: TypedServerItem): Any {
            val node = element as TypedServerItem
            return node.parent
        }
    }
}

package com.github.lizhanyin.tfs.wizard.step

import com.github.lizhanyin.tfs.services.TfsConnectionService
import com.github.lizhanyin.tfs.wizard.ImportProjectContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

/**
 * 项目选择步骤
 */
class ProjectSelectionStep(context: ImportProjectContext) :
    AbstractWizardStep(STEP_ID, "选择项目", context) {

    companion object {
        private const val STEP_ID = "project-selection"
    }

    private lateinit var projectTree: CheckboxTree
    private lateinit var rootTreeNode: CheckedTreeNode
    private lateinit var localPathField: TextFieldWithBrowseButton
    private lateinit var statusLabel: JBLabel
    private lateinit var refreshButton: JButton

    private val projects = mutableListOf<ProjectItem>()

    override fun buildComponent(): JComponent {
        val builder = FormBuilder.createFormBuilder()

        // 本地路径选择
        localPathField = TextFieldWithBrowseButton()
        localPathField.text = context.localPath ?: System.getProperty("user.home")
        localPathField.addBrowseFolderListener(
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
        builder.addLabeledComponent("本地路径:", localPathField)

        builder.addSeparator()

        // 项目树标签
        val headerPanel = JPanel(BorderLayout())
        headerPanel.add(JBLabel("选择要导入的项目/文件夹:"), BorderLayout.WEST)

        refreshButton = JButton("刷新")
        refreshButton.addActionListener { loadProjects() }
        headerPanel.add(refreshButton, BorderLayout.EAST)

        builder.addComponent(headerPanel)

        // 创建项目树
        createProjectTree()
        val treeScrollPane = JBScrollPane(projectTree)
        treeScrollPane.preferredSize = Dimension(400, 300)
        builder.addComponentFillVertically(treeScrollPane, 0)

        builder.addSeparator()

        // 状态标签
        statusLabel = JBLabel("正在加载项目列表...")
        builder.addComponent(statusLabel)

        val panel = builder.panel
        panel.border = JBUI.Borders.empty(10)

        // 初始加载
        loadProjects()

        return panel
    }

    /**
     * 创建项目树
     */
    private fun createProjectTree() {
        rootTreeNode = CheckedTreeNode(null)

        projectTree = CheckboxTree(
            object : CheckboxTree.CheckboxTreeCellRenderer() {
                fun customizeCellRenderer(
                    tree: JTree,
                    value: Any?,
                    selected: Boolean,
                    expanded: Boolean,
                    leaf: Boolean,
                    row: Int,
                    hasFocus: Boolean
                ) {
                    if (value is CheckedTreeNode) {
                        val userObject = value.userObject
                        if (userObject is ProjectItem) {
                            textRenderer.append(userObject.name)
                        }
                    }
                }
            }, rootTreeNode
        )
        projectTree.isRootVisible = false
        projectTree.setShowsRootHandles(true)
    }

    /**
     * 加载项目列表
     */
    private fun loadProjects() {
        if (!context.isTeamProjectSelected()) {
            statusLabel.text = "请先选择团队项目"
            statusLabel.foreground = JBColor.RED
            return
        }

        statusLabel.text = "正在加载项目列表..."
        statusLabel.foreground = Color.BLACK
        refreshButton.isEnabled = false

        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "加载项目", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "正在从 TFS 服务器获取项目列表..."
                indicator.isIndeterminate = true

                try {
                    val connectionService = ApplicationManager.getApplication().getService(TfsConnectionService::class.java)
                    val teamProject = context.teamProject ?: return

                    val serverItems = connectionService.getProjectItems(context, teamProject)

                    // 转换为本地项目项
                    val projectItems = mutableListOf<ProjectItem>()
                    for (item in serverItems) {
                        if (item.isFolder) {
                            val projectItem = ProjectItem(item.serverPath, item.name)
                            // 如果是文件夹，递归加载子项
                            loadChildren(projectItem, connectionService, indicator)
                            projectItems.add(projectItem)
                        }
                    }

                    // 更新 UI
                    SwingUtilities.invokeLater {
                        projects.clear()
                        projects.addAll(projectItems)
                        buildProjectTree()

                        val itemCount = countLeafNodes(rootTreeNode)
                        if (itemCount == 0) {
                            statusLabel.text = "未找到可导入的项目"
                            statusLabel.foreground = JBColor.ORANGE
                        } else {
                            statusLabel.text = "已加载 $itemCount 个项目/文件夹"
                            statusLabel.foreground = Color(0, 128, 0)
                        }
                        refreshButton.isEnabled = true
                    }

                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        statusLabel.text = "加载失败: ${e.message}"
                        statusLabel.foreground = JBColor.RED
                        refreshButton.isEnabled = true
                    }
                }
            }
        })
    }

    /**
     * 递归加载子项
     */
    private fun loadChildren(
        parent: ProjectItem,
        connectionService: TfsConnectionService,
        indicator: ProgressIndicator
    ) {
        try {
            val children = connectionService.getChildItems(context, parent.serverPath)

            for (child in children) {
                if (child.isFolder) {
                    val childItem = ProjectItem(child.serverPath, child.name)
                    parent.addChild(childItem)
                    // 限制递归深度
                    if (parent.serverPath.split("/").size < 6) {
                        loadChildren(childItem, connectionService, indicator)
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略单个文件夹加载失败
        }
    }

    /**
     * 构建项目树
     */
    private fun buildProjectTree() {
        rootTreeNode.removeAllChildren()

        for (item in projects) {
            val node = createTreeNode(item)
            rootTreeNode.add(node)
        }

        (projectTree.model as DefaultTreeModel).reload()
    }

    /**
     * 创建树节点
     */
    private fun createTreeNode(item: ProjectItem): CheckedTreeNode {
        val node = CheckedTreeNode(item)

        item.children?.forEach { child ->
            node.add(createTreeNode(child))
        }

        return node
    }

    /**
     * 统计叶子节点数量
     */
    private fun countLeafNodes(node: TreeNode): Int {
        if (node.isLeaf) {
            return 1
        }

        var count = 0
        for (i in 0 until node.childCount) {
            count += countLeafNodes(node.getChildAt(i))
        }
        return count
    }

    /**
     * 获取选中的项目路径
     */
    private fun getSelectedPaths(): List<String> {
        val paths = mutableListOf<String>()
        collectCheckedPaths(rootTreeNode, paths)
        return paths
    }

    /**
     * 递归收集选中的路径
     */
    private fun collectCheckedPaths(node: CheckedTreeNode, paths: MutableList<String>) {
        if (node.isLeaf && node.isChecked) {
            val userObject = node.userObject
            if (userObject is ProjectItem) {
                paths.add(userObject.serverPath)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            if (child is CheckedTreeNode) {
                collectCheckedPaths(child, paths)
            }
        }
    }

    override fun isComplete(): Boolean {
        val selected = getSelectedPaths()
        return selected.isNotEmpty() && localPathField.text.isNotEmpty()
    }

    override fun getPreferredFocusedComponent(): JComponent = projectTree

    override fun onFinish(): Boolean {
        val selectedPaths = getSelectedPaths()
        context.selectedProjects.clear()
        context.selectedProjects.addAll(selectedPaths)
        context.localPath = localPathField.text
        return true
    }

    /**
     * 项目项
     */
    private class ProjectItem(
        val serverPath: String,
        val name: String
    ) {
        var children: MutableList<ProjectItem>? = null

        fun addChild(child: ProjectItem) {
            if (children == null) {
                children = mutableListOf()
            }
            children!!.add(child)
        }

        override fun toString(): String = name
    }
}

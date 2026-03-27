package com.github.lizhanyin.tfs.wizard.step;

import com.github.lizhanyin.tfs.services.TfsConnectionService;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 项目选择步骤
 */
public class ProjectSelectionStep extends AbstractWizardStep {

    private static final String STEP_ID = "project-selection";

    // UI 组件
    private CheckboxTree projectTree;
    private CheckedTreeNode rootTreeNode;
    private TextFieldWithBrowseButton localPathField;
    private JBLabel statusLabel;
    private JButton refreshButton;

    // 项目列表
    private final List<ProjectItem> projects = new ArrayList<>();

    public ProjectSelectionStep(@NotNull ImportProjectContext context) {
        super(STEP_ID, "选择项目", context);
    }

    @Override
    protected JComponent buildComponent() {
        FormBuilder builder = FormBuilder.createFormBuilder();

        // 本地路径选择
        localPathField = new TextFieldWithBrowseButton();
        localPathField.getTextField().setText(context.getLocalPath() != null ? context.getLocalPath() : System.getProperty("user.home"));
        localPathField.addBrowseFolderListener(
                null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
        );
        builder.addLabeledComponent("本地路径:", localPathField);

        builder.addSeparator();

        // 项目树标签
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(new JBLabel("选择要导入的项目/文件夹:"), BorderLayout.WEST);

        refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> loadProjects());
        headerPanel.add(refreshButton, BorderLayout.EAST);

        builder.addComponent(headerPanel);

        // 创建项目树
        createProjectTree();
        JBScrollPane treeScrollPane = new JBScrollPane(projectTree);
        treeScrollPane.setPreferredSize(new Dimension(400, 300));
        builder.addComponentFillVertically(treeScrollPane, 0);

        builder.addSeparator();

        // 状态标签
        statusLabel = new JBLabel("正在加载项目列表...");
        builder.addComponent(statusLabel);

        JPanel panel = builder.getPanel();
        panel.setBorder(JBUI.Borders.empty(10));

        // 初始加载
        loadProjects();

        return panel;
    }

    /**
     * 创建项目树
     */
    private void createProjectTree() {
        rootTreeNode = new CheckedTreeNode(null);

        projectTree = new CheckboxTree(new CheckboxTree.CheckboxTreeCellRenderer() {
            public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (value instanceof CheckedTreeNode) {
                    Object userObject = ((CheckedTreeNode) value).getUserObject();
                    if (userObject instanceof ProjectItem) {
                        getTextRenderer().append(((ProjectItem) userObject).getName());
                    }
                }
            }
        }, rootTreeNode);
        projectTree.setRootVisible(false);
        projectTree.setShowsRootHandles(true);
    }

    /**
     * 加载项目列表
     */
    private void loadProjects() {
        if (!context.isTeamProjectSelected()) {
            statusLabel.setText("请先选择团队项目");
            statusLabel.setForeground(java.awt.Color.RED);
            return;
        }

        statusLabel.setText("正在加载项目列表...");
        statusLabel.setForeground(java.awt.Color.BLACK);
        refreshButton.setEnabled(false);

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "加载项目", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("正在从 TFS 服务器获取项目列表...");
                indicator.setIndeterminate(true);

                try {
                    TfsConnectionService connectionService =
                            ApplicationManager.getApplication().getService(TfsConnectionService.class);

                    List<TfsConnectionService.ProjectItemInfo> serverItems =
                            connectionService.getProjectItems(context, context.getTeamProject());

                    // 转换为本地项目项
                    List<ProjectItem> projectItems = new ArrayList<>();
                    for (TfsConnectionService.ProjectItemInfo item : serverItems) {
                        if (item.isFolder()) {
                            ProjectItem projectItem = new ProjectItem(item.getServerPath(), item.getName());
                            // 如果是文件夹，递归加载子项
                            loadChildren(projectItem, connectionService, indicator);
                            projectItems.add(projectItem);
                        }
                    }

                    // 更新 UI
                    SwingUtilities.invokeLater(() -> {
                        projects.clear();
                        projects.addAll(projectItems);
                        buildProjectTree();

                        int itemCount = countLeafNodes(rootTreeNode);
                        if (itemCount == 0) {
                            statusLabel.setText("未找到可导入的项目");
                            statusLabel.setForeground(java.awt.Color.ORANGE);
                        } else {
                            statusLabel.setText("已加载 " + itemCount + " 个项目/文件夹");
                            statusLabel.setForeground(new java.awt.Color(0, 128, 0));
                        }
                        refreshButton.setEnabled(true);
                    });

                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("加载失败: " + e.getMessage());
                        statusLabel.setForeground(java.awt.Color.RED);
                        refreshButton.setEnabled(true);
                    });
                }
            }
        });
    }

    /**
     * 递归加载子项
     */
    private void loadChildren(ProjectItem parent, TfsConnectionService connectionService, ProgressIndicator indicator) {
        try {
            List<TfsConnectionService.ProjectItemInfo> children =
                    connectionService.getChildItems(context, parent.getServerPath());

            for (TfsConnectionService.ProjectItemInfo child : children) {
                if (child.isFolder()) {
                    ProjectItem childItem = new ProjectItem(child.getServerPath(), child.getName());
                    parent.addChild(childItem);
                    // 限制递归深度
                    if (parent.getServerPath().split("/").length < 6) {
                        loadChildren(childItem, connectionService, indicator);
                    }
                }
            }
        } catch (Exception e) {
            // 忽略单个文件夹加载失败
        }
    }

    /**
     * 构建项目树
     */
    private void buildProjectTree() {
        rootTreeNode.removeAllChildren();

        for (ProjectItem item : projects) {
            CheckedTreeNode node = createTreeNode(item);
            rootTreeNode.add(node);
        }

        ((DefaultTreeModel) projectTree.getModel()).reload();
    }

    /**
     * 创建树节点
     */
    private CheckedTreeNode createTreeNode(ProjectItem item) {
        CheckedTreeNode node = new CheckedTreeNode(item);

        if (item.getChildren() != null) {
            for (ProjectItem child : item.getChildren()) {
                node.add(createTreeNode(child));
            }
        }

        return node;
    }

    /**
     * 统计叶子节点数量
     */
    private int countLeafNodes(TreeNode node) {
        if (node.isLeaf()) {
            return 1;
        }

        int count = 0;
        Enumeration<TreeNode> children = (Enumeration<TreeNode>) node.children();
        while (children.hasMoreElements()) {
            count += countLeafNodes(children.nextElement());
        }
        return count;
    }

    /**
     * 获取选中的项目路径
     */
    private List<String> getSelectedPaths() {
        List<String> paths = new ArrayList<>();
        collectCheckedPaths(rootTreeNode, paths);
        return paths;
    }

    /**
     * 递归收集选中的路径
     */
    private void collectCheckedPaths(CheckedTreeNode node, List<String> paths) {
        if (node.isLeaf() && node.isChecked()) {
            Object userObject = node.getUserObject();
            if (userObject instanceof ProjectItem) {
                paths.add(((ProjectItem) userObject).getServerPath());
            }
        }

        Enumeration<TreeNode> children = node.children();
        while (children.hasMoreElements()) {
            TreeNode child = children.nextElement();
            if (child instanceof CheckedTreeNode) {
                collectCheckedPaths((CheckedTreeNode) child, paths);
            }
        }
    }

    @Override
    public boolean isComplete() {
        List<String> selected = getSelectedPaths();
        return !selected.isEmpty() && !localPathField.getText().isEmpty();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return projectTree;
    }

    @Override
    public boolean onFinish() {
        List<String> selectedPaths = getSelectedPaths();
        context.setSelectedProjects(selectedPaths);
        context.setLocalPath(localPathField.getText());
        return true;
    }

    /**
     * 项目项
     */
    private static class ProjectItem {
        private final String serverPath;
        private final String name;
        private List<ProjectItem> children;

        public ProjectItem(String serverPath, String name) {
            this.serverPath = serverPath;
            this.name = name;
        }

        public String getServerPath() { return serverPath; }
        public String getName() { return name; }
        public List<ProjectItem> getChildren() { return children; }

        public void addChild(ProjectItem child) {
            if (children == null) {
                children = new ArrayList<>();
            }
            children.add(child);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

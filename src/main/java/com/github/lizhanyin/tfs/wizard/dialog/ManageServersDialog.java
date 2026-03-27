package com.github.lizhanyin.tfs.wizard.dialog;

import com.github.lizhanyin.tfs.settings.TfsServerConfiguration;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 添加/移除 Team Foundation Server 对话框
 */
public class ManageServersDialog extends DialogWrapper {

    private ServerTableModel tableModel;
    private JBTable serverTable;
    private JButton removeButton;
    private JButton credentialsButton;
    private JButton clearCredentialsButton;

    private TfsServerConfiguration.ServerConfig selectedServer;

    public ManageServersDialog(Component parent) {
        super(parent, false);
        setTitle("添加/移除 Team Foundation Server");
        // 隐藏 OK 按钮，只显示关闭按钮
        setOKButtonText("关闭");
        setCancelButtonText("关闭");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(JBUI.Borders.empty(10));
        panel.setPreferredSize(new Dimension(600, 300));

        // 上方：标签独占一行
        JLabel label = new JLabel("Team Foundation Server 列表");
        panel.add(label, BorderLayout.NORTH);

        // 中间：左侧表格 + 右侧按钮
        JPanel contentPanel = new JPanel(new BorderLayout(10, 0));

        // 创建表格
        tableModel = new ServerTableModel();
        loadServers();

        serverTable = new JBTable(tableModel);
        serverTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serverTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonState();
            }
        });

        // 双击编辑
        serverTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = serverTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        editServer(row);
                    }
                }
            }
        });

        // 设置列宽
        serverTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        serverTable.getColumnModel().getColumn(1).setPreferredWidth(350);

        // 居中显示
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        serverTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        JBScrollPane scrollPane = new JBScrollPane(serverTable);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // 右侧：按钮纵向排列
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(JBUI.Borders.emptyLeft(10));

        JButton addButton = new JButton("添加...");
        addButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        addButton.addActionListener(e -> addServer());
        buttonPanel.add(addButton);
        buttonPanel.add(Box.createVerticalStrut(5));

        credentialsButton = new JButton("输入凭证");
        credentialsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        credentialsButton.addActionListener(e -> inputCredentials());
        credentialsButton.setEnabled(false);
        buttonPanel.add(credentialsButton);
        buttonPanel.add(Box.createVerticalStrut(5));

        clearCredentialsButton = new JButton("清除凭证");
        clearCredentialsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        clearCredentialsButton.addActionListener(e -> clearCredentials());
        clearCredentialsButton.setEnabled(false);
        buttonPanel.add(clearCredentialsButton);
        buttonPanel.add(Box.createVerticalStrut(5));

        removeButton = new JButton("移除");
        removeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        removeButton.addActionListener(e -> removeServer());
        removeButton.setEnabled(false);
        buttonPanel.add(removeButton);

        contentPanel.add(buttonPanel, BorderLayout.EAST);

        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    @Override
    protected JComponent createSouthPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(JBUI.Borders.emptyTop(10));

        // 关闭按钮
        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(e -> doCancelAction());
        panel.add(closeButton);

        return panel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        // 不显示默认的 OK/Cancel 按钮
        return new Action[0];
    }

    /**
     * 加载服务器列表
     */
    private void loadServers() {
        List<TfsServerConfiguration.ServerConfig> servers = TfsServerConfiguration.getInstance().getServers();
        tableModel.setServers(new ArrayList<>(servers));
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonState() {
        int selectedRow = serverTable.getSelectedRow();
        boolean hasSelection = selectedRow >= 0 && selectedRow < tableModel.getRowCount();

        removeButton.setEnabled(hasSelection);
        credentialsButton.setEnabled(hasSelection);
        clearCredentialsButton.setEnabled(hasSelection && hasCredentials(selectedRow));
    }

    /**
     * 检查是否有保存的凭证
     */
    private boolean hasCredentials(int row) {
        if (row < 0 || row >= tableModel.getRowCount()) {
            return false;
        }
        TfsServerConfiguration.ServerConfig server = tableModel.getServer(row);
        return server != null && server.getPassword() != null && !server.getPassword().isEmpty();
    }

    /**
     * 添加服务器
     */
    private void addServer() {
        AddServerDialog dialog = new AddServerDialog(getContentPane(), null);
        if (dialog.showAndGet()) {
            TfsServerConfiguration.ServerConfig newServer = dialog.getServerConfig();
            TfsServerConfiguration.getInstance().addServer(newServer);
            loadServers();

            // 选中新添加的服务器
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (Objects.requireNonNull(tableModel.getServer(i)).getUrl().equals(newServer.getUrl())) {
                    serverTable.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    /**
     * 编辑服务器
     */
    private void editServer(int row) {
        TfsServerConfiguration.ServerConfig server = tableModel.getServer(row);
        AddServerDialog dialog = new AddServerDialog(getContentPane(), server);
        if (dialog.showAndGet()) {
            TfsServerConfiguration.ServerConfig updatedServer = dialog.getServerConfig();
            TfsServerConfiguration.getInstance().updateServer(server, updatedServer);
            loadServers();
        }
    }

    /**
     * 输入凭证
     */
    private void inputCredentials() {
        int selectedRow = serverTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        TfsServerConfiguration.ServerConfig server = tableModel.getServer(selectedRow);

        CredentialsDialog.CredentialType type = CredentialsDialog.CredentialType.USER_PASSWORD;
        if (server.getAuthType() != null) {
            try {
                type = CredentialsDialog.CredentialType.valueOf(server.getAuthType());
            } catch (IllegalArgumentException ignored) {
            }
        }

        CredentialsDialog dialog = new CredentialsDialog(getContentPane(), server.getUrl());
        dialog.setCredentials(type, server.getUsername(), server.getPassword());

        if (dialog.showAndGet()) {
            server.setAuthType(dialog.getCredentialType().name());
            server.setUsername(dialog.getUsername());
            server.setPassword(dialog.getPassword());
            TfsServerConfiguration.getInstance().updateServer(server, server);
            loadServers();
            serverTable.setRowSelectionInterval(selectedRow, selectedRow);
        }
    }

    /**
     * 清除凭证
     */
    private void clearCredentials() {
        int selectedRow = serverTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        TfsServerConfiguration.ServerConfig server = tableModel.getServer(selectedRow);

        int result = JOptionPane.showConfirmDialog(
                getContentPane(),
                "是否要清除 Team Foundation Server " + server.getName() + " 的已保存凭据？",
                "清除保存的凭据",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            server.setUsername(null);
            server.setPassword(null);
            server.setAuthType("NTLM");
            TfsServerConfiguration.getInstance().updateServer(server, server);
            loadServers();
            serverTable.setRowSelectionInterval(selectedRow, selectedRow);
            updateButtonState();
        }
    }

    /**
     * 移除服务器
     */
    private void removeServer() {
        int selectedRow = serverTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        TfsServerConfiguration.ServerConfig server = tableModel.getServer(selectedRow);

        int result = JOptionPane.showConfirmDialog(
                getContentPane(),
                "是否要移除 Team Foundation Server " + server.getName() + "？",
                "移除 Team Foundation Server",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            TfsServerConfiguration.getInstance().removeServer(server);
            loadServers();
            updateButtonState();
        }
    }

    /**
     * 获取选中的服务器
     */
    @Nullable
    public TfsServerConfiguration.ServerConfig getSelectedServer() {
        return selectedServer;
    }

    /**
     * 服务器表格模型
     */
    private static class ServerTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"名称", "服务器", "凭证"};

        private List<TfsServerConfiguration.ServerConfig> servers = new ArrayList<>();

        public void setServers(List<TfsServerConfiguration.ServerConfig> servers) {
            this.servers = servers;
            fireTableDataChanged();
        }

        public TfsServerConfiguration.ServerConfig getServer(int row) {
            return servers.get(row);
        }

        @Override
        public int getRowCount() {
            return servers.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            TfsServerConfiguration.ServerConfig server = servers.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> server.getName() != null ? server.getName() : server.getUrl();
                case 1 -> server.getUrl();
                case 2 -> server.getPassword() != null && !server.getPassword().isEmpty() ? "已保存" : "-";
                default -> null;
            };
        }
    }
}

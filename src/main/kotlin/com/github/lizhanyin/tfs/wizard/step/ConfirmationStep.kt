package com.github.lizhanyin.tfs.wizard.step

import com.github.lizhanyin.tfs.TfsBundle
import com.github.lizhanyin.tfs.wizard.ImportProjectContext
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

/**
 * 确认步骤（第五步）
 *
 * UI 布局：
 * ------------------------------------------
 * |  确认                                   |
 * |    将从 Team Foundation Server 导入以下项目|
 * |----------------------------------------|
 * | 导入项目:                                |
 * |  [table  head               ]          |
 * |  [table  row1               ]          |
 * |  [table  row2               ]          |
 * |  [table  row3               ]          |
 * |----------------------------------------|
 * | [上一步] [下一步](禁用)        [完成] [放弃]|
 * -----------------------------------------
 *
 * table 列: 项目|服务器路径
 */
class ConfirmationStep(context: ImportProjectContext) :
    AbstractWizardStep(STEP_ID, TfsBundle.message("ImportWizardConfirmationPage.PageName"), context, TfsBundle.message("ImportWizardConfirmationPage.MultiProjectImportLabelText")) {

    companion object {
        private const val STEP_ID = "confirmation"
    }

    private lateinit var table: JTable

    override fun buildComponent(): JComponent {
        val panel = JPanel(BorderLayout(0, JBUI.scale(8)))
        panel.border = JBUI.Borders.empty(10)

        table = createTable()
        val scrollPane = JBScrollPane(table)
        scrollPane.preferredSize = Dimension(500, 300)
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    private fun createTable(): JTable {
        val columnNames = arrayOf(
            TfsBundle.message("ImportWizardConfirmationPage.ColumnNameProject"),
            TfsBundle.message("ImportWizardConfirmationPage.ColumnNameServerPath")
        )

        val model = object : DefaultTableModel(columnNames, 0) {
            override fun isCellEditable(row: Int, column: Int): Boolean = false
        }

        return JTable(model)
    }

    /**
     * 填充表格数据，在进入此步骤时调用
     */
    private fun refreshTable() {
        val model = table.model as DefaultTableModel
        model.rowCount = 0

        for (path in context.selectedProjects) {
            // 从服务器路径提取项目名，如 $/ProjectName/src/main → src/main
            val projectName = extractProjectName(path)
            model.addRow(arrayOf(projectName, path))
        }
    }

    /**
     * 从服务器路径提取项目名
     * $/TeamProject/folder/subfolder → folder/subfolder
     * $/TeamProject → TeamProject
     */
    private fun extractProjectName(serverPath: String): String {
        // 去掉 $/ 前缀
        val withoutPrefix = serverPath.removePrefix("$/")
        val segments = withoutPrefix.split("/")
        // 第一个段是团队项目名，之后的是项目路径
        return if (segments.size > 1) {
            segments.drop(1).joinToString("/")
        } else {
            withoutPrefix
        }
    }

    override fun isComplete(): Boolean = context.selectedProjects.isNotEmpty()

    override fun onFinish(): Boolean {
        // 确认步骤不需要额外保存数据
        return true
    }

    override fun getComponent(): JComponent {
        val comp = super.getComponent()
        // 每次显示时刷新表格
        refreshTable()
        return comp
    }
}

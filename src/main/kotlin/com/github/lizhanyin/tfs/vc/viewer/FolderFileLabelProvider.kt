package com.github.lizhanyin.tfs.vc.viewer

import com.github.lizhanyin.tfs.TFSClientPlugin
import com.github.lizhanyin.tfs.client.ui.framework.helper.ImageHelper
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import java.text.DecimalFormat
import java.text.MessageFormat
import javax.swing.Icon


/**
 * FolderFileLabelProvider 是一个抽象类，
 * 用于构建显示文件夹和文件元素的标签提供器。
 *
 * 子类实现 getText 和 getImage 方法。
 * 需要文件夹图标时调用 getImageForFolder()，
 * 需要文件图标时调用 getImageForFile(String)。
 */
abstract class FolderFileLabelProvider {

    /**
     * The [ImageHelper] used by this [FolderFileLabelProvider] -
     * never `null` and cleaned up in [.dispose].
     */
    var imageHelper: ImageHelper = ImageHelper(TFSClientPlugin.PLUGIN_ID)

    private val sizeFormat = arrayOf(
        DecimalFormat("#0.00"),
        DecimalFormat("#0.0"),
        DecimalFormat("#0")
    )

    val symbolicLinkDescription: String
        get() = "Symbolic Link"

    val folderDescription: String
        get() = "File Folder"

    fun getFileTypeDescription(filename: String): String {
        val fileType = FileTypeManager.getInstance().getFileTypeByFileName(filename)
        return fileType.description
    }

    fun getFileSize(size: Long): String {
        var idx = 0
        var value = size.toFloat()

        val formats = arrayOf(
            "{0} bytes",
            "{0} KB",
            "{0} MB",
            "{0} GB",
            "{0} TB",
            "{0} PB"
        )

        while (value > 1024 && idx < formats.size - 1) {
            value /= 1024
            idx++
        }

        val format = when {
            idx > 0 && value < 10 -> sizeFormat[0]
            idx > 0 && value < 100 -> sizeFormat[1]
            else -> sizeFormat[2]
        }

        return MessageFormat.format(formats[idx], format.format(value))
    }

    /**
     * 获取文件夹图标
     */
    fun getImageForFolder(): Icon = AllIcons.Nodes.Folder

    /**
     * 获取文件图标，根据文件名匹配对应的文件类型图标
     *
     * @param filename 文件名，为 null 时返回通用文件图标
     * @return 文件图标
     */
    fun getImageForFile(filename: String?): Icon = getImageForFile(filename, true)

    /**
     * 获取文件图标
     *
     * @param filename 文件名，为 null 时返回通用文件图标
     * @param useFileTypeIcons 是否根据文件类型匹配图标
     * @return 文件图标
     */
    fun getImageForFile(filename: String?, useFileTypeIcons: Boolean): Icon {
        if (useFileTypeIcons && filename != null) {
            val fileType = FileTypeManager.getInstance().getFileTypeByFileName(filename)
            return fileType.icon ?: AllIcons.FileTypes.Any_type
        }
        return AllIcons.FileTypes.Any_type
    }
}

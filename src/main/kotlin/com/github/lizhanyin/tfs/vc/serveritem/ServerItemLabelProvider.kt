package com.github.lizhanyin.tfs.vc.serveritem

import com.github.lizhanyin.tfs.client.ui.vc.serveritem.ServerItemSource
import com.github.lizhanyin.tfs.client.ui.vc.serveritem.ServerItemType
import com.github.lizhanyin.tfs.client.ui.vc.serveritem.TypedServerItem
import com.github.lizhanyin.tfs.vc.viewer.FolderFileLabelProvider
import javax.swing.Icon

class ServerItemLabelProvider : FolderFileLabelProvider() {

    private var serverImage: Icon? = null
    private var teamProjectImage: Icon? = null
    private var branchImage: Icon? = null
    private var gitBranchImage: Icon? = null
    private var gitRepositoryImage: Icon? = null

    private var serverItemSource: ServerItemSource? = null

    fun setServerItemSource(serverItemSource: ServerItemSource) {
        this.serverItemSource = serverItemSource
    }

    fun getServerItemSource(): ServerItemSource {return serverItemSource!!}

    fun getText(element: Any): String {
        val node = element as TypedServerItem

        if (node.type === ServerItemType.ROOT && serverItemSource != null) {
            return serverItemSource!!.serverName
        }

        return node.name
    }

    fun getImage(element: Any): Icon {
        val node = element as TypedServerItem

        if (node.type === ServerItemType.ROOT) {
            if (serverImage == null) {
                serverImage = imageHelper.getImage("icons/TeamFoundationServer.gif")
            }
            return serverImage!!
        }

        if (node.type === ServerItemType.TEAM_PROJECT) {
            if (teamProjectImage == null) {
                teamProjectImage = imageHelper.getImage("icons/TeamProject.gif")
            }
            return teamProjectImage!!
        }

        if (node.type === ServerItemType.FOLDER && node.isBranch) {
            if (branchImage == null) {
                branchImage = imageHelper.getImage("images/vc/folder_branch.gif")
            }
            return branchImage!!
        }

        if (node.type === ServerItemType.GIT_REPOSITORY) {
            if (gitRepositoryImage == null) {
                gitRepositoryImage = imageHelper.getImage("images/common/git_repo.png")
            }
            return gitRepositoryImage!!
        }

        if (node.type === ServerItemType.GIT_BRANCH) {
            if (gitBranchImage == null) {
                gitBranchImage = imageHelper.getImage("images/common/git_branch.png")
            }
            return gitBranchImage!!
        }

        return if (node.type === ServerItemType.FOLDER) {
            getImageForFolder()
        } else {
            getImageForFile(node.name)
        }
    }
}

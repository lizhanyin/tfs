package com.github.lizhanyin.tfs.services.impl;

import com.github.lizhanyin.tfs.services.TfsConnectionService;
import com.github.lizhanyin.tfs.wizard.ImportProjectContext;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.URIUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * TFS 连接服务实现
 */
public class TfsConnectionServiceImpl implements TfsConnectionService {

    @Override
    public boolean testConnection(@NotNull ImportProjectContext context) {
        TFSTeamProjectCollection tpc = null;
        try {
            tpc = connect(context);
            // 尝试获取工作项客户端来验证连接
            tpc.getWorkItemClient();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (tpc != null) {
                try {
                    tpc.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    @NotNull
    public List<String> getTeamProjects(@NotNull ImportProjectContext context) throws Exception {
        TFSTeamProjectCollection tpc = null;
        try {
            tpc = connect(context);
            List<String> projects = new ArrayList<>();
            for (com.microsoft.tfs.core.clients.workitem.project.Project project : tpc.getWorkItemClient().getProjects()) {
                projects.add(project.getName());
            }
            return projects;
        } finally {
            if (tpc != null) {
                try {
                    tpc.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    @NotNull
    public List<ProjectItemInfo> getProjectItems(@NotNull ImportProjectContext context, @NotNull String teamProject) throws Exception {
        TFSTeamProjectCollection tpc = null;
        try {
            tpc = connect(context);
            VersionControlClient vcClient = tpc.getVersionControlClient();
            List<ProjectItemInfo> items = new ArrayList<>();

            // 获取团队项目根路径下的项目
            String serverPath = "$/" + teamProject;
            Item[] tfsItems = vcClient.getItems(serverPath, RecursionType.ONE_LEVEL).getItems();

            if (tfsItems != null) {
                for (Item item : tfsItems) {
                    // 排除根目录本身
                    if (!item.getServerItem().equals(serverPath)) {
                        items.add(new ProjectItemInfo(
                                item.getServerItem(),
                                item.getItemType() == com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType.FOLDER
                                        ? TfsConnectionService.ItemType.FOLDER : TfsConnectionService.ItemType.FILE
                        ));
                    }
                }
            }

            return items;
        } finally {
            if (tpc != null) {
                try {
                    tpc.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    @NotNull
    public List<ProjectItemInfo> getChildItems(@NotNull ImportProjectContext context, @NotNull String parentPath) throws Exception {
        TFSTeamProjectCollection tpc = null;
        try {
            tpc = connect(context);
            VersionControlClient vcClient = tpc.getVersionControlClient();
            List<ProjectItemInfo> items = new ArrayList<>();

            Item[] tfsItems = vcClient.getItems(parentPath, RecursionType.ONE_LEVEL).getItems();

            if (tfsItems != null) {
                for (Item item : tfsItems) {
                    // 排除当前目录本身
                    if (!item.getServerItem().equals(parentPath)) {
                        items.add(new ProjectItemInfo(
                                item.getServerItem(),
                                item.getItemType() == com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType.FOLDER
                                        ? TfsConnectionService.ItemType.FOLDER : TfsConnectionService.ItemType.FILE
                        ));
                    }
                }
            }

            return items;
        } finally {
            if (tpc != null) {
                try {
                    tpc.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 连接到 TFS 服务器
     */
    private TFSTeamProjectCollection connect(@NotNull ImportProjectContext context) throws Exception {
        Credentials credentials = createCredentials(context);
        String collectionUrl = context.getCollectionUrl();

        return new TFSTeamProjectCollection(
                URIUtils.newURI(collectionUrl),
                credentials
        );
    }

    /**
     * 创建凭据
     */
    private Credentials createCredentials(@NotNull ImportProjectContext context) {
        switch (context.getAuthType()) {
            case NTLM:
                return new DefaultNTCredentials();
            case BASIC:
            case PAT:
            default:
                String username = context.getUsername();
                String password = context.getPassword();
                String domain = context.getDomain();

                /* 如果有域名，组合为 DOMAIN\\username 格式 */
                if (domain != null && !domain.isEmpty()) {
                    username = domain + "\\" + username;
                }

                return new UsernamePasswordCredentials(username, password);
        }
    }
}

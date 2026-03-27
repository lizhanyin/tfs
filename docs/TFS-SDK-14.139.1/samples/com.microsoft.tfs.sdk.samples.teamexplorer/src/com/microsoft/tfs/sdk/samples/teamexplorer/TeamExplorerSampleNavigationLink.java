// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.teamexplorer;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.swt.widgets.Shell;

import com.github.lizhanyin.tfs.client.ui.ui.browser.BrowserFacade;
import com.github.lizhanyin.tfs.client.ui.ui.browser.BrowserFacade.LaunchMode;
import com.github.lizhanyin.tfs.client.ui.ui.teamexplorer.TeamExplorerContext;
import com.github.lizhanyin.tfs.client.ui.ui.teamexplorer.TeamExplorerNavigator;
import com.github.lizhanyin.tfs.client.ui.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;
import com.github.lizhanyin.tfs.client.ui.ui.teamexplorer.link.TeamExplorerBaseNavigationLink;

public class TeamExplorerSampleNavigationLink extends TeamExplorerBaseNavigationLink {

    @Override
    public void onClick(
        final Shell shell,
        final TeamExplorerContext context,
        final TeamExplorerNavigator navigator,
        final TeamExplorerNavigationItemConfig parentNavigationItem) {
        final String uriText = TeamExplorerSettings.SAMPLE_LINK_URL;
        URI uri = null;
        try {
            uri = new URI(uriText);
        } catch (final URISyntaxException e) {

        }

        BrowserFacade.launchURL(uri, null, null, null, LaunchMode.EXTERNAL);

    }

}

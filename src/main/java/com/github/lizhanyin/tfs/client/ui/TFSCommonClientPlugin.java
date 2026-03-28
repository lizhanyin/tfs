package com.github.lizhanyin.tfs.client.ui;

import com.github.lizhanyin.tfs.runtime.IStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TFSCommonClientPlugin {

    private static final Log log = LogFactory.getLog(TFSCommonClientPlugin.class);

    public static final String PLUGIN_ID = "com.microsoft.tfs.client.common"; //$NON-NLS-1$

    public static void log(IStatus status) {
        log.info(status);
    }
}

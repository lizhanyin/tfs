// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.ui.framework.helper;

import com.intellij.openapi.ui.Messages;

import javax.swing.Icon;

/**
 * A class to assist with retrieving IDEA system icons. (For example, warning
 * icons, etc.)
 *
 * NOTE: DO NOT dispose system icons.
 */
public class SystemImageHelper {
    /**
     * Return the <code>Icon</code> to be used when displaying an error.
     *
     * @return icon the error icon
     */
    public static Icon getErrorImage() {
        return Messages.getErrorIcon();
    }

    /**
     * Return the <code>Icon</code> to be used when displaying a warning.
     *
     * @return icon the warning icon
     */
    public static Icon getWarningImage() {
        return Messages.getWarningIcon();
    }

    /**
     * Return the <code>Icon</code> to be used when displaying information.
     *
     * @return icon the information icon
     */
    public static Icon getInfoImage() {
        return Messages.getInformationIcon();
    }

    /**
     * Return the <code>Icon</code> to be used when displaying a question.
     *
     * @return icon the question icon
     */
    public static Icon getQuestionImage() {
        return Messages.getQuestionIcon();
    }
}

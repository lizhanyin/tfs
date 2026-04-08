// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.codemarker;

/**
 * The extension point mechanism allowing plugins to contribute
 * {@link CodeMarkerListener}s.
 */
public interface CodeMarkerListenerProvider {
    public CodeMarkerListener getCodeMarkerListener();
}

// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.github.lizhanyin.tfs.client.codemarker;

/**
 * A code marker listener will be notified when {@link CodeMarker}s are reached.
 */
public interface CodeMarkerListener {
    public void onCodeMarker(CodeMarker codeMarker);
}

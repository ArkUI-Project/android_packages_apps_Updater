/*
 * SPDX-FileCopyrightText: 2026 The ArkUI Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.data.source.network

import android.content.Context
import org.lineageos.updater.R
import org.lineageos.updater.deviceinfo.DeviceInfoUtils

/** ArkUI OTA configuration. A blank endpoint deliberately disables online update checks. */
object UpdateServiceConfig {
    fun endpoint(context: Context): String? =
        DeviceInfoUtils.updaterUri.trim()
            .ifEmpty { context.getString(R.string.updater_server_url).trim() }
            .takeIf { it.isNotEmpty() }

    fun isConfigured(context: Context): Boolean = endpoint(context) != null
}

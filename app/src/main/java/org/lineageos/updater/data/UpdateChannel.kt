/*
 * SPDX-FileCopyrightText: 2026 The ArkUI Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.data

import androidx.annotation.StringRes
import org.lineageos.updater.R

enum class UpdateChannel(val storageValue: String, @StringRes val label: Int) {
    STABLE("stable", R.string.update_channel_stable),
    BETA("beta", R.string.update_channel_beta),
    ALPHA("alpha", R.string.update_channel_alpha);

    fun releaseLabel(number: Int?): String? = when (this) {
        STABLE -> if (number == null) "Stable" else null
        BETA -> number?.takeIf { it > 0 }?.let { "Beta$it" }
        ALPHA -> number?.takeIf { it > 0 }?.let { "Alpha$it" }
    }

    companion object {
        fun fromStorageValue(value: String?): UpdateChannel? =
            entries.find { it.storageValue == value }
    }
}

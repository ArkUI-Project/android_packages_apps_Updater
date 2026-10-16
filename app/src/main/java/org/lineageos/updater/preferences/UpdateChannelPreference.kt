/*
 * SPDX-FileCopyrightText: 2026 The ArkUI Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settingslib.spa.widget.preference.ListPreference
import com.android.settingslib.spa.widget.preference.ListPreferenceModel
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import kotlinx.coroutines.launch
import org.lineageos.updater.R
import org.lineageos.updater.data.UpdateChannel
import org.lineageos.updater.data.UserPreferencesRepository
import org.lineageos.updater.deviceinfo.DeviceInfoUtils

@Composable
fun UpdateChannelPreference(repository: UserPreferencesRepository) {
    val installed = DeviceInfoUtils.installedChannel ?: return
    val selectedChannel by repository.updateChannelFlow.collectAsStateWithLifecycle(installed)
    val channel = selectedChannel ?: return
    val coroutineScope = rememberCoroutineScope()
    val preferenceTitle = stringResource(R.string.update_channel_title)
    val channelOptions = UpdateChannel.entries.map { option ->
        ListPreferenceOption(
            id = option.ordinal,
            text = stringResource(option.label),
        )
    }
    val selected = remember(channel) {
        object : IntState {
            override val intValue = channel.ordinal
        }
    }
    ListPreference(object : ListPreferenceModel {
        override val title = preferenceTitle
        override val options = channelOptions
        override val selectedId = selected
        override val onIdSelected: (Int) -> Unit = { id ->
            val newChannel = UpdateChannel.entries.getOrNull(id)
            if (newChannel != null) {
                coroutineScope.launch { repository.setUpdateChannel(newChannel) }
            }
        }
    })
}

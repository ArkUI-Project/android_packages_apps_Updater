/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The ArkUI Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import org.lineageos.updater.data.source.local.UpdatesLocalDataSource
import org.lineageos.updater.data.source.network.UpdatesNetworkDataSource
import org.lineageos.updater.data.source.network.toUpdate
import org.lineageos.updater.deviceinfo.DeviceInfoUtils
import org.lineageos.updater.notifications.NotificationHelper
import org.lineageos.updater.util.NetworkMonitor
import java.io.IOException

private const val TAG = "UpdatesRepository"

class UpdatesRepository(
    private val networkMonitor: NetworkMonitor,
    private val notificationHelper: NotificationHelper,
    private val networkDataSource: UpdatesNetworkDataSource,
    private val localDataSource: UpdatesLocalDataSource,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    private val fetchMutex = Mutex()
    val isUpdateServiceConfigured: Boolean
        get() = networkDataSource.isConfigured

    fun observeLocalUpdates(): Flow<List<Update>> = combine(
        localDataSource.observeUpdates(), userPreferencesRepository.updateChannelFlow
    ) { updates, channel ->
        updates.filter {
            it.downloadId == Update.LOCAL_ID || it.status.persistentStatus > 0 ||
                (channel != null && it.type == "OFFICIAL" && it.channel == channel.storageValue)
        }
    }

    /**
     * Fetches available updates from the server, syncs the local database, and posts a
     * notification if new updates are found. Callers observe [observeLocalUpdates]
     * for results from the selected channel and any ongoing local operations.
     *
     * @return the timestamp of the fetch, or null if the service or network is unavailable.
     * @throws IOException on network or HTTP errors.
     * @throws SerializationException if the response cannot be parsed.
     */
    suspend fun fetchUpdates(): Long? = fetchMutex.withLock {
        // Return before touching cached packages or recording a successful check.
        if (!isUpdateServiceConfigured) return@withLock null
        if (!networkMonitor.currentNetworkState.isOnline) return@withLock null

        val channel = userPreferencesRepository.getUpdateChannel() ?: return@withLock null
        val networkUpdates = withContext(Dispatchers.IO) {
            networkDataSource.fetchUpdates(channel)
                .map { it.toUpdate() }.filter { filterUpdates(it, channel) }
        }
        // A response from the previous channel must not replace the new selection.
        if (userPreferencesRepository.getUpdateChannel() != channel) return@withLock null

        val localUpdates = withContext(Dispatchers.IO) {
            localDataSource.getUpdates()
        }.associateBy { it.downloadId }

        val networkIds = networkUpdates.map { it.downloadId }.toSet()

        if (localUpdates.isNotEmpty() && networkUpdates.any { it.downloadId !in localUpdates }) {
            notificationHelper.showNewUpdatesNotification()
        }

        withContext(Dispatchers.IO) {
            // Merge local state into each network update and upsert into the DB.
            // Room's observeUpdates() Flow will emit automatically if anything changed.
            networkUpdates.forEach { networkUpdate ->
                val local = localUpdates[networkUpdate.downloadId]
                val update = if (local != null && local.status.persistentStatus > 0) {
                    networkUpdate.copy(status = local.status, file = local.file)
                } else {
                    networkUpdate
                }
                localDataSource.addUpdate(update)
            }

            // Retain downloaded packages and other channels when refreshing this channel.
            localUpdates.values.filter {
                it.downloadId !in networkIds && it.downloadId != Update.LOCAL_ID &&
                    it.type == "OFFICIAL" && it.channel == channel.storageValue &&
                    it.status.persistentStatus == 0 && it.downloadUrl != null
            }.forEach {
                localDataSource.removeUpdate(it.downloadId)
            }
        }

        if (userPreferencesRepository.getUpdateChannel() == channel) System.currentTimeMillis() else null
    }

    private fun filterUpdates(update: Update, channel: UpdateChannel): Boolean {
        val isCurrentBuild = update.timestamp == DeviceInfoUtils.buildDateTimestamp
        val isOlderBuild = update.timestamp < DeviceInfoUtils.buildDateTimestamp

        if (!DeviceInfoUtils.isDowngradingAllowed && (isOlderBuild || isCurrentBuild)) {
            Log.d(TAG, "${update.name} is not newer than the current build")
            return false
        }

        if (update.osSdkLevel < DeviceInfoUtils.sdkLevel) {
            Log.d(TAG, "${update.name} is older than current Android version")
            return false
        }

        if (update.type != "OFFICIAL" || update.channel != channel.storageValue) {
            Log.d(TAG, "${update.name} has type ${update.type} and channel ${update.channel}")
            return false
        }
        return true
    }
}

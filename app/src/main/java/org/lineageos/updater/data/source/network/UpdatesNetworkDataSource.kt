/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The ArkUI Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.data.source.network

import android.content.Context
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.lineageos.updater.data.UpdateChannel
import org.lineageos.updater.deviceinfo.DeviceInfoUtils
import java.io.IOException
import java.util.concurrent.TimeUnit

class UpdatesNetworkDataSource(private val context: Context) {
    val isConfigured: Boolean
        get() = UpdateServiceConfig.isConfigured(context)

    private fun serverUrl(channel: UpdateChannel): String {
        val base = UpdateServiceConfig.endpoint(context)
            ?: throw IOException("ArkUI update service is not configured")
        require(base.startsWith("https://")) {
            "Update server URL must use HTTPS: $base"
        }
        val url = base
            .replace("{device}", DeviceInfoUtils.device)
            .replace("{channel}", channel.storageValue)
            .replace("{incr}", DeviceInfoUtils.buildVersionIncremental)
        require('{' !in url && '}' !in url) { "Unsupported update URL placeholder" }
        return url
    }

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    fun fetchUpdates(channel: UpdateChannel): List<NetworkUpdate> {
        val request = Request.Builder()
            .url(serverUrl(channel))
            .header("X-ArkUI-Release-Type", DeviceInfoUtils.releaseType)
            .header("X-ArkUI-Channel", channel.storageValue)
            .header("X-ArkUI-Installed-Channel", DeviceInfoUtils.installedChannel?.storageValue.orEmpty())
            .header("X-ArkUI-Build-Version", DeviceInfoUtils.buildVersion)
            .header("X-ArkUI-SDK", DeviceInfoUtils.sdkLevel.toString())
            .build()

        val responseBody = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP status: ${response.code}")
            }

            response.body?.string() ?: throw IOException("Empty response body")
        }

        val updates = Json.decodeFromString<List<NetworkUpdate>>(responseBody)
        if (updates.any { it.type != "OFFICIAL" || it.channel != channel.storageValue }) {
            throw SerializationException("Updates must match the requested official channel")
        }
        if (updates.any { it.files.size != 1 }) {
            throw SerializationException("An update must contain exactly one OTA file")
        }
        if (updates.any { update ->
                val file = update.files.single()
                update.datetime <= 0 ||
                    channel.releaseLabel(update.channelNumber) == null ||
                    !update.version.matches(Regex("[0-9]+(?:\\.[0-9]+){1,3}(?:[-+][A-Za-z0-9.-]+)?")) ||
                    !update.buildNumber.matches(Regex("(?!eng\\.)[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) ||
                    file.filename != "${update.buildName}.zip" || file.size <= 0 ||
                    file.osSdkLevel <= 0 || !file.sha256.matches(Regex("[0-9a-f]{64}")) ||
                    !file.osPatchLevel.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) ||
                    !file.url.startsWith("https://")
            }) {
            throw SerializationException("Invalid OTA metadata")
        }
        return updates
    }
}

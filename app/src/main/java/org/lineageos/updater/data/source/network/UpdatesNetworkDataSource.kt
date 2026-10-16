/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The ArkUI Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.updater.data.source.network

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.lineageos.updater.deviceinfo.DeviceInfoUtils
import java.io.IOException
import java.util.concurrent.TimeUnit

class UpdatesNetworkDataSource(private val context: Context) {
    val isConfigured: Boolean
        get() = UpdateServiceConfig.isConfigured(context)

    private val serverUrl: String
        get() {
            val base = UpdateServiceConfig.endpoint(context)
                ?: throw IOException("ArkUI update service is not configured")
            require(base.startsWith("https://")) {
                "Update server URL must use HTTPS: $base"
            }
            return base
                .replace("{device}", DeviceInfoUtils.device)
                .replace("{type}", DeviceInfoUtils.releaseType.lowercase())
                .replace("{incr}", DeviceInfoUtils.buildVersionIncremental)
        }

    private val client = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    fun fetchUpdates(): List<NetworkUpdate> {
        val request = Request.Builder()
            .url(serverUrl)
            .header("X-ArkUI-Release-Type", DeviceInfoUtils.releaseType)
            .header("X-ArkUI-Build-Version", DeviceInfoUtils.buildVersion)
            .header("X-ArkUI-SDK", DeviceInfoUtils.sdkLevel.toString())
            .build()

        val responseBody = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP status: ${response.code}")
            }

            response.body?.string() ?: throw IOException("Empty response body")
        }

        return Json.decodeFromString<List<NetworkUpdate>>(responseBody)
    }
}

/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The ArkUI Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.data.source.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.lineageos.updater.data.Update
import org.lineageos.updater.data.UpdateChannel
import org.lineageos.updater.misc.Constants

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class NetworkUpdate(
    @SerialName("datetime") val datetime: Long,
    @SerialName("files") val files: List<NetworkUpdateFile>,
    @SerialName("type") val type: String,
    @SerialName("channel") val channel: String,
    @SerialName("channel_number") val channelNumber: Int?,
    @SerialName("build_number") val buildNumber: String,
    @SerialName("version") val version: String,
) {
    val buildName: String
        get() {
            val label = UpdateChannel.fromStorageValue(channel)?.releaseLabel(channelNumber)
            return "ArkUI-$version-$type-$label-$buildNumber"
        }
}

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class NetworkUpdateFile(
    @SerialName("filename") val filename: String,
    @SerialName("os_patch_level") val osPatchLevel: String,
    @SerialName("os_sdk_level") val osSdkLevel: Int,
    @SerialName("ota_property_files") val otaPropertyFiles: String? = null,
    @SerialName("sha256") val sha256: String,
    @SerialName("size") val size: Long,
    @SerialName("url") val url: String,
)

private data class PackageFileRange(
    val offset: Long,
    val size: Long,
)

private fun String.parsePackageFileRanges() =
    split(",").associate { token ->
        val parts = token.trim().split(":")
        parts[0].trim() to PackageFileRange(
            offset = parts[1].trim().toLong(),
            size = parts[2].trim().toLong(),
        )
    }

fun NetworkUpdate.toUpdate(): Update {
    val file = files[0]
    val packageFileRanges = file.otaPropertyFiles?.parsePackageFileRanges().orEmpty()
    val payloadMetadataRange = packageFileRanges[Constants.AB_PAYLOAD_METADATA_PATH]
    val payloadRange = packageFileRanges[Constants.AB_PAYLOAD_BIN_PATH]
    val payloadPropertiesRange = packageFileRanges[Constants.AB_PAYLOAD_PROPERTIES_PATH]

    return Update(
        downloadId = file.sha256,
        name = file.filename,
        timestamp = datetime,
        type = type,
        channel = channel,
        fileSize = file.size,
        downloadUrl = file.url,
        version = buildName,
        osPatchLevel = file.osPatchLevel,
        osSdkLevel = file.osSdkLevel,
        payloadMetadataOffset = payloadMetadataRange?.offset,
        payloadMetadataSize = payloadMetadataRange?.size,
        payloadOffset = payloadRange?.offset,
        payloadSize = payloadRange?.size,
        payloadPropertiesOffset = payloadPropertiesRange?.offset,
        payloadPropertiesSize = payloadPropertiesRange?.size,
        isAvailableOnline = true,
    )
}

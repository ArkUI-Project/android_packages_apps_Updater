/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The ArkUI Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.deviceinfo

import android.os.Build
import android.os.SystemProperties
import org.lineageos.updater.data.UpdateChannel
import com.android.settingslib.DeviceInfoUtils as SettingsLibDeviceInfoUtils

object DeviceInfoUtils : SettingsLibDeviceInfoUtils() {

    private const val PROP_AB_DEVICE = "ro.build.ab_update"
    private const val PROP_ALLOW_MAJOR_UPGRADES = "lineage.updater.allow_major_upgrades"
    private const val PROP_BUILD_DATE = "ro.build.date.utc"
    private const val PROP_BUILD_VERSION = "org.arkui.build.version"
    private const val PROP_BUILD_VERSION_INCREMENTAL = "ro.build.version.incremental"
    private const val PROP_DEVICE = "ro.lineage.device"
    private const val PROP_RELEASE_TYPE = "org.arkui.releasetype"
    private const val PROP_CHANNEL = "org.arkui.channel"
    private const val PROP_CHANNEL_NUMBER = "org.arkui.channel.number"
    private const val PROP_UPDATER_ALLOW_DOWNGRADING = "lineage.updater.allow_downgrading"
    private const val PROP_UPDATER_URI = "ro.arkui.updater.uri"
    private const val PROP_UPDATE_RECOVERY = "persist.vendor.recovery_update"

    // Read-only
    val androidVersion: String = Build.VERSION.RELEASE

    val buildSecurityPatch: String = Build.VERSION.SECURITY_PATCH

    val sdkLevel: Int = Build.VERSION.SDK_INT

    @JvmStatic
    val buildDateTimestamp: Long = SystemProperties.getLong(PROP_BUILD_DATE, 0)

    @JvmStatic
    val buildVersion: String = SystemProperties.get(PROP_BUILD_VERSION, "")

    @JvmStatic
    val buildVersionIncremental: String = SystemProperties.get(PROP_BUILD_VERSION_INCREMENTAL, "")

    @JvmStatic
    val device: String = SystemProperties.get(PROP_DEVICE)

    @JvmStatic
    val isABDevice: Boolean = SystemProperties.getBoolean(PROP_AB_DEVICE, false)

    @JvmStatic
    val releaseType: String = SystemProperties.get(PROP_RELEASE_TYPE)

    val isOfficialBuild: Boolean = releaseType == "OFFICIAL"

    val romVersion: String = SystemProperties.get("ro.arkui.version")

    @JvmStatic
    val buildName: String = SystemProperties.get("ro.lineage.version")

    private val channelNumberValue = SystemProperties.get(PROP_CHANNEL_NUMBER)
    val installedChannelNumber: Int? = channelNumberValue.toIntOrNull()

    val installedChannel: UpdateChannel? = if (isOfficialBuild) {
        UpdateChannel.fromStorageValue(SystemProperties.get(PROP_CHANNEL))?.takeIf {
            (channelNumberValue.isEmpty() || installedChannelNumber != null) &&
                it.releaseLabel(installedChannelNumber) != null
        }
    } else {
        null
    }

    val releaseLabel: String = if (isOfficialBuild) {
        installedChannel?.releaseLabel(installedChannelNumber).orEmpty()
    } else {
        releaseType
    }

    // Mutable at runtime
    @JvmStatic
    val isDowngradingAllowed: Boolean
        get() = SystemProperties.getBoolean(PROP_UPDATER_ALLOW_DOWNGRADING, false)

    @JvmStatic
    val isMajorUpdateAllowed: Boolean
        get() = SystemProperties.getBoolean(PROP_ALLOW_MAJOR_UPGRADES, false)

    @JvmStatic
    var isRecoveryUpdateEnabled: Boolean
        get() = SystemProperties.getBoolean(PROP_UPDATE_RECOVERY, false)
        set(value) = SystemProperties.set(PROP_UPDATE_RECOVERY, value.toString())

    @JvmStatic
    val updaterUri: String
        get() = SystemProperties.get(PROP_UPDATER_URI, "")
}

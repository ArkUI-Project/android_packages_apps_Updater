/*
 * SPDX-FileCopyrightText: 2026 The ArkUI Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.ui

import android.app.Activity
import android.os.Build
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lineageos.updater.R
import org.lineageos.updater.UpdaterApplication
import org.lineageos.updater.UpdatesViewModel
import org.lineageos.updater.deviceinfo.DeviceInfoUtils
import org.lineageos.updater.preferences.UpdateChannelPreference
import org.lineageos.updater.updates.UpdateList
import org.lineageos.updater.updates.action.UpdateAction
import org.lineageos.updater.updates.action.UpdateActionType
import org.lineageos.updater.updates.state.ProgressState
import org.lineageos.updater.updates.state.UpdateItemState
import org.lineageos.updater.util.StringUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArkuiUpdatesScreen(
    uiState: UpdatesViewModel.UiState,
    items: List<UpdateItemState>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLocalUpdate: () -> Unit,
    onPreferences: () -> Unit,
    onAction: (UpdateAction, String) -> Unit,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val featured = items.firstOrNull {
        it.progress != null || it.actions.primary.type == UpdateActionType.REBOOT
    } ?: items.firstOrNull()
    var menuOpen by remember { mutableStateOf(false) }
    var moreExpanded by rememberSaveable { mutableStateOf(false) }
    var otherExpanded by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(R.string.display_name)
    val pending = !uiState.isUpdateServiceConfigured
    LaunchedEffect(title) { (context as? Activity)?.title = title }

    Scaffold(
        containerColor = colors.surface,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface),
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.arkui_navigate_back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Outlined.MoreVert, stringResource(R.string.arkui_update_options))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.local_update_import)) },
                                onClick = { menuOpen = false; onLocalUpdate() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_preferences)) },
                                onClick = { menuOpen = false; onPreferences() },
                            )
                            if (uiState.isUpdateServiceConfigured) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.check_for_updates)) },
                                    enabled = uiState.updatesCheckModel.canCheckForUpdates,
                                    onClick = { menuOpen = false; onRefresh() },
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = colors.surface, tonalElevation = 3.dp) {
                Box(
                    Modifier.fillMaxWidth().navigationBarsPadding()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        Modifier.widthIn(max = 680.dp).fillMaxWidth().padding(24.dp, 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalIconButton(onClick = onLocalUpdate, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Outlined.Archive, stringResource(R.string.local_update_import))
                        }
                        Button(
                            onClick = {
                                if (featured != null) onAction(featured.actions.primary, featured.downloadId)
                                else onRefresh()
                            },
                            enabled = featured?.actions?.primary?.enabled
                                ?: uiState.updatesCheckModel.canCheckForUpdates,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) {
                            Text(
                                text = featured?.actions?.primary?.type?.title(context)
                                    ?: stringResource(when {
                                        !DeviceInfoUtils.isOfficialBuild -> R.string.arkui_unofficial_update
                                        pending -> R.string.arkui_update_service_pending
                                        uiState.isCheckingForUpdates -> R.string.checking_for_updates
                                        else -> R.string.check_for_updates
                                    }),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 680.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                UpdateHero()
                UpdateChannelPreference(
                    (context.applicationContext as UpdaterApplication).userPreferencesRepository
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(if (featured == null) R.string.arkui_current_version else R.string.arkui_update_ready),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primary,
                        )
                        Text(
                            featured?.buildVersion ?: DeviceInfoUtils.buildName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            featured?.androidUpdateInfo?.takeIf { it.isNotEmpty() }
                                ?: stringResource(R.string.header_android_version, DeviceInfoUtils.androidVersion),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Surface(shape = CircleShape, color = colors.secondaryContainer) {
                        Icon(
                            Icons.Outlined.SystemUpdate, null,
                            Modifier.padding(18.dp).size(28.dp),
                            tint = colors.onSecondaryContainer,
                        )
                    }
                }

                if (featured == null) {
                    UpdateServiceStatus(uiState)
                } else {
                    FeaturedUpdateDetails(featured, onAction)
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(stringResource(R.string.arkui_before_updating))
                    Text(
                        stringResource(R.string.arkui_update_reminder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                }

                Column(
                    Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = 0.85f, stiffness = 450f)),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionTitle(stringResource(R.string.arkui_version_details))
                    InfoRow(stringResource(R.string.build_date), StringUtil.formatBuildDate(context, DeviceInfoUtils.buildDateTimestamp))
                    InfoRow(stringResource(R.string.security_update), StringUtil.formatSecurityPatch(context, DeviceInfoUtils.buildSecurityPatch))
                    if (moreExpanded) {
                        InfoRow(stringResource(R.string.arkui_build_number), DeviceInfoUtils.buildVersionIncremental)
                        InfoRow(stringResource(R.string.arkui_device), Build.MODEL)
                    }
                    TextButton(
                        onClick = { moreExpanded = !moreExpanded },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(if (moreExpanded) R.string.arkui_show_less else R.string.arkui_show_more))
                        Spacer(Modifier.width(6.dp))
                        Icon(if (moreExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null)
                    }
                }
                if (items.size > 1) {
                    Column {
                        TextButton(onClick = { otherExpanded = !otherExpanded }) {
                            Text(stringResource(R.string.arkui_other_updates))
                            Icon(if (otherExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null)
                        }
                        AnimatedVisibility(otherExpanded) {
                            UpdateList(
                                items = items.filter { it.downloadId != featured?.downloadId },
                                isUpdatesCheckStatusVisible = true,
                                onAction = onAction,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Original vector artwork, using the system's dynamic Material colors. */
@Composable
private fun UpdateHero() {
    val colors = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val ink = colors.onPrimaryContainer
    Box(Modifier.fillMaxWidth().heightIn(min = 220.dp).clip(RoundedCornerShape(28.dp))) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawRect(Brush.linearGradient(
                listOf(colors.primaryContainer, colors.secondaryContainer, colors.surfaceContainerHigh),
                start = Offset.Zero, end = Offset(w, h),
            ))
            // Broad curved glass ribbons, with a soft highlight along each fold.
            for (index in 0..7) {
                val x = w * (-0.25f + index * 0.22f)
                val ribbon = Path().apply {
                    moveTo(x, -h * 0.2f)
                    cubicTo(x + w * 0.36f, h * 0.1f, x - w * 0.22f, h * 0.75f, x + w * 0.16f, h * 1.2f)
                    lineTo(x + w * 0.38f, h * 1.2f)
                    cubicTo(x, h * 0.75f, x + w * 0.58f, h * 0.1f, x + w * 0.22f, -h * 0.2f)
                    close()
                }
                drawPath(ribbon, Brush.linearGradient(
                    listOf(
                        colors.primary.copy(alpha = if (dark) 0.14f else 0.07f),
                        colors.onPrimaryContainer.copy(alpha = 0.025f),
                        colors.surfaceBright.copy(alpha = if (dark) 0.10f else 0.38f),
                    ),
                    start = Offset(x, 0f), end = Offset(x + w * 0.22f, h * 0.3f),
                ))
            }
            drawRect(Brush.radialGradient(
                listOf(colors.primaryContainer.copy(alpha = 0.7f), colors.primaryContainer.copy(alpha = 0f)),
                center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.6f,
            ))
        }
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                painterResource(R.drawable.arkui_logo), stringResource(R.string.brand_name),
                Modifier.width(132.dp).height(44.dp),
                colorFilter = ColorFilter.tint(ink),
            )
            Text(
                DeviceInfoUtils.romVersion, fontSize = 64.sp,
                lineHeight = 68.sp, letterSpacing = (-2).sp,
                fontWeight = FontWeight.Medium, color = ink,
            )
            Text(DeviceInfoUtils.releaseLabel, style = MaterialTheme.typography.titleMedium, color = ink)
            Surface(shape = CircleShape, color = colors.surface.copy(alpha = 0.45f)) {
                Text(
                    stringResource(R.string.header_android_version, DeviceInfoUtils.androidVersion),
                    Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium, color = ink,
                )
            }
        }
    }
}

@Composable
private fun UpdateServiceStatus(state: UpdatesViewModel.UiState) {
    val colors = MaterialTheme.colorScheme
    val pending = !state.isUpdateServiceConfigured
    val title = stringResource(when {
        !DeviceInfoUtils.isOfficialBuild -> R.string.arkui_unofficial_update
        pending -> R.string.arkui_update_service_pending
        state.isCheckingForUpdates -> R.string.checking_for_updates
        !state.isOnline -> R.string.check_your_internet_connection
        state.hasUpdateCheckFailed -> R.string.updates_check_failed
        state.lastCheckedTimestamp > 0 -> R.string.updates_zero_state_title
        else -> R.string.arkui_update_not_checked
    })
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp), color = colors.surfaceContainerLow,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.isCheckingForUpdates) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                } else {
                    Icon(if (pending || !state.isOnline || state.hasUpdateCheckFailed) Icons.Outlined.CloudOff else Icons.Outlined.VerifiedUser, null, tint = colors.primary)
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                stringResource(when {
                    !DeviceInfoUtils.isOfficialBuild -> R.string.arkui_unofficial_update_summary
                    pending -> R.string.arkui_update_service_pending_summary
                    else -> R.string.updates_zero_state_description
                }),
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
            )
            if (!pending && state.lastCheckedTimestamp > 0) {
                val date = DateUtils.formatDateTime(LocalContext.current, state.lastCheckedTimestamp, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
                Text(date, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FeaturedUpdateDetails(item: UpdateItemState, onAction: (UpdateAction, String) -> Unit) {
    val context = LocalContext.current
    var optionsOpen by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.arkui_update_details))
        Text(item.buildDate, style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.list_update_size, item.fileSize), style = MaterialTheme.typography.bodyLarge)
        if (item.securityUpdate.isNotEmpty()) {
            Text(stringResource(R.string.list_security_update, item.securityUpdate), style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            if (item.installNote == R.string.list_major_upgrade_recovery_install) {
                stringResource(item.installNote, stringResource(R.string.brand_name))
            } else stringResource(item.installNote),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (item.status.isNotEmpty()) Text(item.status, style = MaterialTheme.typography.titleMedium)
        when (val progress = item.progress) {
            is ProgressState.Determinate -> {
                LinearProgressIndicator(progress = { (progress.percent / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text(listOf(progress.downloadedSize, progress.eta).filter { it.isNotEmpty() }.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
            }
            ProgressState.Indeterminate -> LinearProgressIndicator(Modifier.fillMaxWidth())
            null -> Unit
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            item.actions.secondary?.let { action ->
                TextButton(onClick = { onAction(action, item.downloadId) }, enabled = action.enabled) {
                    Text(action.type.title(context))
                }
            }
            if (item.actions.overflow.isNotEmpty()) {
                Box {
                    TextButton(onClick = { optionsOpen = true }) {
                        Text(stringResource(R.string.arkui_update_options))
                        Icon(Icons.Outlined.KeyboardArrowDown, null)
                    }
                    DropdownMenu(expanded = optionsOpen, onDismissRequest = { optionsOpen = false }) {
                        item.actions.overflow.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.type.title(context)) }, enabled = action.enabled,
                                onClick = { optionsOpen = false; onAction(action, item.downloadId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

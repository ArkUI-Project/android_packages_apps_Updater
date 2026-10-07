/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The ArkUI Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.framework.theme.SettingsTheme
import org.lineageos.updater.controller.UpdaterController
import org.lineageos.updater.data.Update
import org.lineageos.updater.preferences.PreferencesActivity
import org.lineageos.updater.ui.ArkuiUpdatesScreen
import org.lineageos.updater.updates.action.AlertDialogState
import org.lineageos.updater.updates.action.UpdateActionDialog
import org.lineageos.updater.updates.action.UpdateActionHandler
import org.lineageos.updater.updates.state.UpdateItemStateMapper

abstract class UpdatesScaffoldActivity : ComponentActivity() {
    private val viewModel by viewModels<UpdatesViewModel>()
    private var activeUpdaterController: UpdaterController? by mutableStateOf(null)
    private var controllerStateVersion: Int by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    protected fun setupCompose() {
        setContent {
            val navController = remember {
                object : NavControllerWrapper {
                    override fun navigate(route: String, popUpCurrent: Boolean) {}
                    override fun navigateBack() = finish()
                }
            }
            CompositionLocalProvider(LocalNavController provides navController) {
                SettingsTheme {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val application = application as UpdaterApplication
                    val networkState by application.networkMonitor.networkState
                        .collectAsStateWithLifecycle(application.networkMonitor.currentNetworkState)
                    val streamUpdates by application.userPreferencesRepository.streamUpdatesFlow
                        .collectAsStateWithLifecycle(true)
                    val controller = activeUpdaterController
                    val items = remember(
                        uiState.updates, controller, networkState, streamUpdates, controllerStateVersion,
                    ) {
                        val mapper = controller?.let {
                            UpdateItemStateMapper(this@UpdatesScaffoldActivity, it, streamUpdates)
                        }
                        uiState.updates.mapNotNull { update ->
                            controller?.getUpdate(update.downloadId)?.let { mapper?.map(it, networkState) }
                        }
                    }
                    val dialogState = remember { mutableStateOf<AlertDialogState?>(null) }
                    dialogState.value?.let {
                        UpdateActionDialog(dialog = it, onDismiss = { dialogState.value = null })
                    }
                    val actionHandler = remember(controller) {
                        controller?.let {
                            UpdateActionHandler(
                                activity = this@UpdatesScaffoldActivity,
                                updaterController = it,
                                exportUpdate = { update -> exportUpdate(update) },
                                showDialog = { dialogState.value = it },
                            )
                        }
                    }
                    ArkuiUpdatesScreen(
                        uiState = uiState,
                        items = items,
                        onBack = { finish() },
                        onRefresh = { onRefreshClick() },
                        onLocalUpdate = { onLocalUpdateClick() },
                        onPreferences = {
                            startActivity(Intent(this@UpdatesScaffoldActivity, PreferencesActivity::class.java))
                        },
                        onAction = { action, id ->
                            controller?.getUpdate(id)?.let { actionHandler?.perform(action, it) }
                            notifyControllerStateChanged()
                        },
                    )
                }
            }
        }
    }

    protected fun setUpdaterController(controller: UpdaterController?) {
        activeUpdaterController = controller
        notifyControllerStateChanged()
    }

    protected fun notifyControllerStateChanged() {
        controllerStateVersion++
    }

    open fun onRefreshClick() {}
    open fun onLocalUpdateClick() {}
    open fun exportUpdate(update: Update) {}
}

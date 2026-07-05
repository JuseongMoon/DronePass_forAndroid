package com.ScienceFiction.DronePassAndroid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.ScienceFiction.DronePassAndroid.core.data.NotificationPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.storedLaunchNotificationPermissionRequested
import com.ScienceFiction.DronePassAndroid.feature.settings.localizedAppLanguageContext
import com.ScienceFiction.DronePassAndroid.feature.settings.storedKeepScreenAwake
import com.ScienceFiction.DronePassAndroid.service.AppForegroundState
import com.ScienceFiction.DronePassAndroid.service.ForegroundNotification
import com.ScienceFiction.DronePassAndroid.service.extractForegroundNotification
import com.ScienceFiction.DronePassAndroid.service.extractNotificationShapeId
import com.ScienceFiction.DronePassAndroid.ui.navigation.MainScreen
import com.ScienceFiction.DronePassAndroid.ui.theme.DronePassAndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val LaunchNotificationPermissionRequestCode = 7301

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var dataStore: DataStore<Preferences>

    private val initialFocusShapeId = mutableStateOf<String?>(null)
    private val notificationForPopup = mutableStateOf<ForegroundNotification?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(localizedAppLanguageContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_DronePassAndroid)
        super.onCreate(savedInstanceState)
        initialFocusShapeId.value = resolveNotificationLaunchFocusShapeId(
            notificationShapeId = extractNotificationShapeId(intent),
        )
        notificationForPopup.value = extractForegroundNotification(intent)
        enableEdgeToEdge()
        observeKeepScreenAwakeSetting()
        requestLaunchNotificationPermissionIfNeeded()
        setContent {
            DronePassAndroidTheme {
                MainScreen(
                    initialFocusShapeId = initialFocusShapeId.value,
                    initialForegroundNotification = notificationForPopup.value,
                    onInitialFocusShapeConsumed = {
                        initialFocusShapeId.value = null
                    },
                    onInitialForegroundNotificationConsumed = {
                        notificationForPopup.value = null
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        initialFocusShapeId.value = resolveNotificationLaunchFocusShapeId(
            notificationShapeId = extractNotificationShapeId(intent),
        )
        notificationForPopup.value = extractForegroundNotification(intent)
    }

    override fun onStart() {
        super.onStart()
        AppForegroundState.onActivityStarted()
    }

    override fun onStop() {
        AppForegroundState.onActivityStopped()
        super.onStop()
    }

    private fun requestLaunchNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        lifecycleScope.launch {
            val alreadyRequested = dataStore.data
                .map(::storedLaunchNotificationPermissionRequested)
                .first()
            val permissionGranted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

            if (
                shouldRequestLaunchNotificationPermission(
                    sdkInt = Build.VERSION.SDK_INT,
                    permissionGranted = permissionGranted,
                    alreadyRequested = alreadyRequested,
                )
            ) {
                dataStore.edit { preferences ->
                    preferences[NotificationPreferenceKeys.LAUNCH_NOTIFICATION_PERMISSION_REQUESTED] = true
                }
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    LaunchNotificationPermissionRequestCode,
                )
            }
        }
    }

    private fun observeKeepScreenAwakeSetting() {
        lifecycleScope.launch {
            dataStore.data
                .map { preferences -> storedKeepScreenAwake(preferences) }
                .distinctUntilChanged()
                .collect { keepScreenAwake ->
                    when (resolveKeepScreenAwakeFlagUpdate(keepScreenAwake)) {
                        KeepScreenAwakeFlagUpdate.ADD -> {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                        KeepScreenAwakeFlagUpdate.CLEAR -> {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                }
        }
    }
}

internal fun shouldRequestLaunchNotificationPermission(
    sdkInt: Int,
    permissionGranted: Boolean,
    alreadyRequested: Boolean,
): Boolean {
    return sdkInt >= Build.VERSION_CODES.TIRAMISU && !permissionGranted && !alreadyRequested
}

@Suppress("UNUSED_PARAMETER")
internal fun resolveNotificationLaunchFocusShapeId(
    notificationShapeId: String?,
): String? {
    // iOS PushNotificationManager.didReceive only restores the title/body popup.
    // Shape map focus remains limited to in-app saved-list and overlay taps.
    return null
}

internal enum class KeepScreenAwakeFlagUpdate {
    ADD,
    CLEAR,
}

internal fun resolveKeepScreenAwakeFlagUpdate(
    keepScreenAwake: Boolean,
): KeepScreenAwakeFlagUpdate {
    return if (keepScreenAwake) {
        KeepScreenAwakeFlagUpdate.ADD
    } else {
        KeepScreenAwakeFlagUpdate.CLEAR
    }
}

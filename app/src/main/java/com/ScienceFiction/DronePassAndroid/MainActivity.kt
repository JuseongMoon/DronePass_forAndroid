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
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.ScienceFiction.DronePassAndroid.app.LaunchPermissionSequence
import com.ScienceFiction.DronePassAndroid.core.data.NotificationPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.data.storedLaunchNotificationPermissionRequested
import com.ScienceFiction.DronePassAndroid.core.analytics.UserActivityTracker
import com.ScienceFiction.DronePassAndroid.feature.settings.localizedAppLanguageContext
import com.ScienceFiction.DronePassAndroid.feature.settings.storedKeepScreenAwake
import com.ScienceFiction.DronePassAndroid.service.AppForegroundState
import com.ScienceFiction.DronePassAndroid.subscription.PaywallRequest
import com.ScienceFiction.DronePassAndroid.subscription.SubscriptionManager
import com.ScienceFiction.DronePassAndroid.subscription.SubscriptionPaywall
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
    @Inject lateinit var userActivityTracker: UserActivityTracker
    @Inject lateinit var subscriptionManager: SubscriptionManager

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
        subscriptionManager.start()
        setContent {
            var paywallRequest by remember { mutableStateOf<PaywallRequest?>(null) }
            var subscriptionMessage by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(subscriptionManager) {
                subscriptionManager.paywallRequests.collect { paywallRequest = it }
            }
            LaunchedEffect(subscriptionManager) {
                subscriptionManager.messages.collect { subscriptionMessage = it }
            }
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
                paywallRequest?.let { request ->
                    SubscriptionPaywall(subscriptionManager, request, this, onDismiss = { paywallRequest = null })
                }
                subscriptionMessage?.let { message ->
                    AlertDialog(
                        onDismissRequest = { subscriptionMessage = null },
                        text = { Text(message) },
                        confirmButton = { TextButton(onClick = { subscriptionMessage = null }) { Text(getString(R.string.common_confirm)) } },
                    )
                }
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
        lifecycleScope.launch {
            userActivityTracker.recordIfNeeded()
        }
    }

    override fun onResume() {
        super.onResume()
        subscriptionManager.onForeground()
    }

    override fun onStop() {
        AppForegroundState.onActivityStopped()
        super.onStop()
    }

    private fun requestLaunchNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        lifecycleScope.launch {
            // 지도 화면의 위치 권한 요청이 끝난 뒤에 묻는다. 동시에 요청하면 이 요청이 버려진다.
            LaunchPermissionSequence.locationRequestSettled.first { it }
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

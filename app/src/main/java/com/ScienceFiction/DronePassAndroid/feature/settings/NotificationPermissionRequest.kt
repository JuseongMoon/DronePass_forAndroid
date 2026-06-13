package com.ScienceFiction.DronePassAndroid.feature.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ScienceFiction.DronePassAndroid.R

/**
 * 알림 관련 권한 요청 안내 카드.
 *
 * 두 가지 권한을 다룬다:
 * 1. **POST_NOTIFICATIONS** (Android 13+) — 알림 표시 자체에 필요.
 *    런타임 권한이라 [ActivityResultContracts.RequestPermission]으로 요청.
 * 2. **SCHEDULE_EXACT_ALARM** (Android 12+) — 정확한 시각 알람.
 *    사용자가 시스템 설정에서 직접 허용해야 하므로 안내 + Intent 만 제공.
 *
 * 카드는 권한이 부족할 때만 노출되며, 권한이 부여되면 자동으로 숨겨진다.
 * 앱 시작 자동 권한 요청은 MainActivity 의 1회 요청 흐름이 담당하므로,
 * 설정 화면에서는 사용자가 버튼을 눌렀을 때만 권한을 다시 요청한다.
 */
@Composable
fun NotificationPermissionRequest(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // POST_NOTIFICATIONS 권한 상태 추적
    var notificationPermissionGranted by remember {
        mutableStateOf(hasNotificationPermission(context))
    }
    // SCHEDULE_EXACT_ALARM 권한 상태 추적
    var exactAlarmGranted by remember {
        mutableStateOf(canScheduleExactAlarms(context))
    }

    // 시스템 설정에서 돌아오면 권한 재확인 (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationPermissionGranted = hasNotificationPermission(context)
                exactAlarmGranted = canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // POST_NOTIFICATIONS 런타임 권한 요청 launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
    }

    // 두 권한 모두 OK 면 카드 자체를 그리지 않음
    if (!shouldRenderNotificationPermissionRequest(notificationPermissionGranted, exactAlarmGranted)) return

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (!notificationPermissionGranted) {
            PermissionCard(
                icon = Icons.Default.NotificationsActive,
                title = stringResource(R.string.notification_permission_title),
                description = stringResource(R.string.notification_permission_description),
                buttonText = stringResource(R.string.notification_permission_request),
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!exactAlarmGranted) {
            PermissionCard(
                icon = Icons.Default.Schedule,
                title = stringResource(R.string.exact_alarm_permission_title),
                description = stringResource(R.string.exact_alarm_permission_description),
                buttonText = stringResource(R.string.exact_alarm_permission_open_settings),
                onClick = { openExactAlarmSettings(context) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

}

@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onClick) {
                Text(buttonText)
            }
        }
    }
}

/** Android 13+ 에서 POST_NOTIFICATIONS 권한 보유 여부. 이전 버전은 항상 true. */
private fun hasNotificationPermission(context: Context): Boolean {
    val permissionGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return resolveNotificationPermissionGranted(
        sdkInt = Build.VERSION.SDK_INT,
        permissionGranted = permissionGranted,
    )
}

/** Android 12+ 에서 SCHEDULE_EXACT_ALARM 권한 부여 여부. 이전 버전은 항상 true. */
private fun canScheduleExactAlarms(context: Context): Boolean {
    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    val canScheduleExactAlarms =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am?.canScheduleExactAlarms()
        } else {
            null
        }
    return resolveExactAlarmPermissionGranted(
        sdkInt = Build.VERSION.SDK_INT,
        canScheduleExactAlarms = canScheduleExactAlarms,
    )
}

/** 시스템의 "정확한 알람 권한" 설정 화면으로 이동. */
private fun openExactAlarmSettings(context: Context) {
    if (!shouldOpenExactAlarmSettings(Build.VERSION.SDK_INT)) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

internal fun resolveNotificationPermissionGranted(
    sdkInt: Int,
    permissionGranted: Boolean,
): Boolean {
    return sdkInt < Build.VERSION_CODES.TIRAMISU || permissionGranted
}

internal fun resolveExactAlarmPermissionGranted(
    sdkInt: Int,
    canScheduleExactAlarms: Boolean?,
): Boolean {
    return sdkInt < Build.VERSION_CODES.S || canScheduleExactAlarms == true
}

internal fun shouldRenderNotificationPermissionRequest(
    notificationPermissionGranted: Boolean,
    exactAlarmGranted: Boolean,
): Boolean {
    return !notificationPermissionGranted || !exactAlarmGranted
}

internal fun shouldOpenExactAlarmSettings(sdkInt: Int): Boolean {
    return sdkInt >= Build.VERSION_CODES.S
}

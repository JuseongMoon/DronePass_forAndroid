package com.ScienceFiction.DronePassAndroid.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.local.EncryptedPrefsHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

internal const val FCM_DEVICE_ID_PREFERENCE_KEY = "DeviceUUID"
internal const val LEGACY_FCM_DEVICE_ID_PREFERENCE_KEY = "fcm_device_id"

internal fun buildFcmDeviceData(
    token: String,
    appVersion: String,
): HashMap<String, Any> = hashMapOf<String, Any>(
    "fcmToken" to token,
    "platform" to "android",
    "appVersion" to appVersion,
    "isActive" to true,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
)

internal fun buildFcmDeactivateData(): HashMap<String, Any> = hashMapOf(
    "isActive" to false,
    "updatedAt" to FieldValue.serverTimestamp(),
)

internal fun selectStoredFcmDeviceId(primary: String?, legacy: String?): String? =
    primary?.takeIf { it.isNotBlank() } ?: legacy?.takeIf { it.isNotBlank() }

internal fun shouldMigrateLegacyFcmDeviceId(primary: String?, legacy: String?): Boolean =
    primary.isNullOrBlank() && !legacy.isNullOrBlank()

internal fun formatFcmAppVersion(versionName: String?, versionCode: Long?): String {
    val version = versionName?.trim()?.takeIf { it.isNotEmpty() } ?: "Unknown"
    val build = versionCode?.toString() ?: "Unknown"
    return "$version ($build)"
}

internal fun fcmTokenUpdatedLogMessage(token: String): String =
    "FCM 토큰 갱신: length=${token.length}"

internal fun fcmDeviceIdCreatedLogMessage(deviceId: String): String =
    "새 디바이스 ID 생성: length=${deviceId.length}"

internal fun fcmTokenStoredLogMessage(deviceId: String): String =
    "FCM 토큰 저장 성공: deviceIdLength=${deviceId.length}"

internal fun fcmTokenDeactivatedLogMessage(deviceId: String): String =
    "FCM 토큰 비활성화 성공: deviceIdLength=${deviceId.length}"

/**
 * FCM 푸시 알림 서비스
 * Firebase Cloud Messaging을 통해 수신된 메시지를 처리하고 알림을 표시합니다.
 *
 * @AndroidEntryPoint 로 Hilt 통합 — 인스턴스 메서드에서는 @Inject 필드를 사용하고
 * 정적 헬퍼([deactivateToken] 등)는 [EncryptedPrefsHelper.createEncryptedPrefs]
 * 등 명시적 헬퍼만 사용한다.
 */
@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject lateinit var firebaseAuth: FirebaseAuth
    @Inject lateinit var firestore: FirebaseFirestore

    companion object {
        private const val TAG = "FcmService"

        /** 일반 정보성 알림(FCM 푸시 등) */
        const val CHANNEL_ID = "dronepass_notifications"

        /** 시간 민감 알림(일출/일몰/비행 종료 등 — 잠금화면+소리+진동) */
        const val CHANNEL_ID_TIME_SENSITIVE = "dronepass_time_sensitive"

        /**
         * 알림 채널 생성 (Android 8.0+ 필수). 두 채널을 모두 등록한다:
         *  - CHANNEL_ID: 일반 정보 (IMPORTANCE_DEFAULT)
         *  - CHANNEL_ID_TIME_SENSITIVE: 시간 민감 (IMPORTANCE_HIGH + sound/vibration)
         */
        fun createNotificationChannel(context: Context) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: run {
                    Log.w(TAG, "NotificationManager를 가져올 수 없어 알림 채널 생성을 건너뜁니다.")
                    return
                }

            val defaultChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_default_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_default_description)
            }

            val timeSensitiveChannel = NotificationChannel(
                CHANNEL_ID_TIME_SENSITIVE,
                context.getString(R.string.notification_channel_time_sensitive_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_time_sensitive_description)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(defaultChannel, timeSensitiveChannel)
            )
        }

        /**
         * 로그아웃 시 FCM 토큰 비활성화
         * Firestore에서 해당 디바이스를 비활성 상태로 표시한다.
         */
        fun deactivateToken(context: Context) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                Log.d(TAG, "로그인 상태가 아니므로 FCM 토큰 비활성화를 건너뜁니다.")
                return
            }

            val deviceId = getDeviceId(context) ?: run {
                Log.d(TAG, "디바이스 ID가 없으므로 FCM 토큰 비활성화를 건너뜁니다.")
                return
            }

            val firestore = FirebaseFirestore.getInstance()
            val deviceRef = firestore
                .collection("users")
                .document(userId)
                .collection("devices")
                .document(deviceId)

            val deactivateData = buildFcmDeactivateData()

            deviceRef.update(deactivateData)
                .addOnSuccessListener {
                    Log.d(TAG, fcmTokenDeactivatedLogMessage(deviceId))
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FCM 토큰 비활성화 실패", e)
                }
        }

        /**
         * 로그인 성공 직후 현재 FCM 토큰을 요청하여 Firestore에 저장한다.
         * iOS PushNotificationManager.requestFCMToken 정합.
         */
        fun requestAndSaveToken(context: Context) {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (token.isNullOrBlank()) {
                        Log.d(TAG, "FCM 토큰이 비어 있어 저장을 건너뜁니다.")
                        return@addOnSuccessListener
                    }
                    saveTokenToFirestore(context.applicationContext, token)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FCM 토큰 요청 실패", e)
                }
        }

        /**
         * 저장된 디바이스 ID 조회 (없으면 null)
         *
         * EncryptedPrefs 생성/손상 복구 로직은 [EncryptedPrefsHelper.createEncryptedPrefs]가 담당.
         */
        private fun getDeviceId(context: Context): String? {
            val prefs = EncryptedPrefsHelper.createEncryptedPrefs(context)
            return getStoredDeviceId(prefs)
        }

        private fun getOrCreateDeviceId(context: Context): String {
            val prefs = EncryptedPrefsHelper.createEncryptedPrefs(context)
            val existing = getStoredDeviceId(prefs)
            if (existing != null) return existing

            val deviceId = UUID.randomUUID().toString()
            prefs.edit {
                putString(FCM_DEVICE_ID_PREFERENCE_KEY, deviceId)
            }
            Log.d(TAG, fcmDeviceIdCreatedLogMessage(deviceId))
            return deviceId
        }

        private fun getStoredDeviceId(prefs: SharedPreferences): String? {
            val primary = prefs.getString(FCM_DEVICE_ID_PREFERENCE_KEY, null)
            val legacy = prefs.getString(LEGACY_FCM_DEVICE_ID_PREFERENCE_KEY, null)
            val selected = selectStoredFcmDeviceId(primary, legacy) ?: return null
            if (shouldMigrateLegacyFcmDeviceId(primary, legacy)) {
                prefs.edit {
                    putString(FCM_DEVICE_ID_PREFERENCE_KEY, selected)
                    remove(LEGACY_FCM_DEVICE_ID_PREFERENCE_KEY)
                }
            }
            return selected
        }

        private fun saveTokenToFirestore(context: Context, token: String) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                Log.d(TAG, "로그인 상태가 아니므로 FCM 토큰 저장을 건너뜁니다.")
                return
            }

            val deviceId = getOrCreateDeviceId(context)
            val appVersion = getAppVersion(context)
            val firestore = FirebaseFirestore.getInstance()
            val deviceRef = firestore
                .collection("users")
                .document(userId)
                .collection("devices")
                .document(deviceId)

            val deviceData = buildFcmDeviceData(
                token = token,
                appVersion = appVersion,
            )
            deviceRef.set(deviceData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, fcmTokenStoredLogMessage(deviceId))
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FCM 토큰 저장 실패", e)
                }
        }

        private fun getAppVersion(context: Context): String {
            val packageInfo = try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (e: Exception) {
                null
            }
            return formatFcmAppVersion(
                versionName = packageInfo?.versionName,
                versionCode = packageInfo?.let(::packageVersionCode),
            )
        }

        private fun packageVersionCode(packageInfo: PackageInfo): Long {
            return packageInfo.longVersionCode
        }
    }

    /**
     * FCM 토큰이 갱신되었을 때 호출
     * Firestore의 users/{userId}/devices/{deviceId}에 토큰 정보 저장
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, fcmTokenUpdatedLogMessage(token))
        saveTokenToFirestore(applicationContext, token)
    }

    /**
     * FCM 메시지 수신 시 호출
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM 메시지 수신: from=${message.from}")

        // 알림 채널 생성
        createNotificationChannel(this)

        // 알림 페이로드가 있는 경우
        val shapeId = extractNotificationShapeId(message.data)
        message.notification?.let { notification ->
            val title = notification.title ?: getString(R.string.app_name)
            val body = notification.body ?: ""
            publishForegroundNotification(title = title, body = body, shapeId = shapeId)
            showNotification(title = title, body = body, shapeId = shapeId)
        }

        // 데이터 페이로드가 있는 경우
        if (message.data.isNotEmpty()) {
            val title = extractNotificationTitle(message.data) ?: getString(R.string.app_name)
            val body = extractNotificationBody(message.data) ?: ""
            if (
                message.notification == null &&
                shouldDisplayRemoteDataOnlyNotification()
            ) {
                publishForegroundNotification(title = title, body = body, shapeId = shapeId)
                showNotification(title = title, body = body, shapeId = shapeId)
            }
        }
    }

    private fun publishForegroundNotification(title: String, body: String, shapeId: String?) {
        foregroundNotificationForRemoteDelivery(
            title = title,
            body = body,
            shapeId = shapeId,
            appInForeground = AppForegroundState.isForeground,
        )?.let(ForegroundNotificationBus::publish)
    }

    /**
     * 알림 표시
     */
    private fun showNotification(title: String, body: String, shapeId: String? = null) {
        val notificationId = System.currentTimeMillis().toInt()
        val intent = buildNotificationClickIntent(
            context = this,
            shapeId = shapeId,
            title = title,
            body = body,
        )
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: run {
                Log.w(TAG, "NotificationManager를 가져올 수 없어 FCM 알림 표시를 건너뜁니다.")
                return
            }
        notificationManager.notify(notificationId, notification)
    }

}

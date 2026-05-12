package com.ScienceFiction.DronePassAndroid.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ScienceFiction.DronePassAndroid.MainActivity
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.local.EncryptedPrefsHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

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
        private const val CHANNEL_NAME = "DronePass 알림"
        private const val CHANNEL_DESCRIPTION = "DronePass 앱의 일반 알림을 수신합니다"

        /** 시간 민감 알림(일출/일몰/비행 종료 등 — 잠금화면+소리+진동) */
        const val CHANNEL_ID_TIME_SENSITIVE = "dronepass_time_sensitive"
        private const val CHANNEL_NAME_TIME_SENSITIVE = "비행 시각 알림"
        private const val CHANNEL_DESCRIPTION_TIME_SENSITIVE =
            "일출/일몰 및 비행 종료 등 정해진 시각에 도착해야 하는 알림"

        private const val KEY_DEVICE_ID = "fcm_device_id"

        /**
         * 알림 채널 생성 (Android 8.0+ 필수). 두 채널을 모두 등록한다:
         *  - CHANNEL_ID: 일반 정보 (IMPORTANCE_DEFAULT)
         *  - CHANNEL_ID_TIME_SENSITIVE: 시간 민감 (IMPORTANCE_HIGH + sound/vibration)
         */
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val defaultChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = CHANNEL_DESCRIPTION }

            val timeSensitiveChannel = NotificationChannel(
                CHANNEL_ID_TIME_SENSITIVE,
                CHANNEL_NAME_TIME_SENSITIVE,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION_TIME_SENSITIVE
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(defaultChannel, timeSensitiveChannel)
            )
        }

        /**
         * 로그아웃 시 FCM 토큰 비활성화
         * Firestore에서 해당 디바이스의 fcmToken을 null로 설정
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

            val deactivateData = hashMapOf<String, Any?>(
                "fcmToken" to null,
                "isActive" to false,
                "lastUpdated" to FieldValue.serverTimestamp()
            )

            deviceRef.update(deactivateData)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM 토큰 비활성화 성공: deviceId=$deviceId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FCM 토큰 비활성화 실패", e)
                }
        }

        /**
         * 저장된 디바이스 ID 조회 (없으면 null)
         *
         * EncryptedPrefs 생성/손상 복구 로직은 [EncryptedPrefsHelper.createEncryptedPrefs]가 담당.
         */
        private fun getDeviceId(context: Context): String? {
            val prefs = EncryptedPrefsHelper.createEncryptedPrefs(context)
            return prefs.getString(KEY_DEVICE_ID, null)
        }
    }

    /**
     * FCM 토큰이 갱신되었을 때 호출
     * Firestore의 users/{userId}/devices/{deviceId}에 토큰 정보 저장
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM 토큰 갱신: $token")
        saveTokenToFirestore(token)
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
        message.notification?.let { notification ->
            showNotification(
                title = notification.title ?: getString(R.string.app_name),
                body = notification.body ?: ""
            )
        }

        // 데이터 페이로드가 있는 경우
        if (message.data.isNotEmpty()) {
            val title = message.data["title"] ?: getString(R.string.app_name)
            val body = message.data["body"] ?: ""
            if (message.notification == null) {
                showNotification(title = title, body = body)
            }
        }
    }

    /**
     * 알림 표시
     */
    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
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
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    /**
     * FCM 토큰을 Firestore에 저장
     * FieldValue.serverTimestamp()를 사용하여 서버 시간으로 lastUpdated 기록
     */
    private fun saveTokenToFirestore(token: String) {
        val userId = firebaseAuth.currentUser?.uid ?: run {
            Log.d(TAG, "로그인 상태가 아니므로 FCM 토큰 저장을 건너뜁니다.")
            return
        }

        val deviceId = getOrCreateDeviceId()

        val packageInfo = try {
            packageManager.getPackageInfo(packageName, 0)
        } catch (e: Exception) {
            null
        }
        val appVersion = packageInfo?.versionName ?: "1.0"

        val deviceData = hashMapOf<String, Any>(
            "fcmToken" to token,
            "platform" to "android",
            "appVersion" to appVersion,
            "isActive" to true,
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        val deviceRef = firestore
            .collection("users")
            .document(userId)
            .collection("devices")
            .document(deviceId)

        // 먼저 문서가 존재하는지 확인 후 createdAt 설정
        deviceRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    deviceData["createdAt"] = FieldValue.serverTimestamp()
                }
                deviceRef.set(deviceData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "FCM 토큰 저장 성공: deviceId=$deviceId")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "FCM 토큰 저장 실패", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "디바이스 문서 확인 실패", e)
            }
    }

    /**
     * 고유 디바이스 ID를 EncryptedSharedPreferences에서 가져오거나 새로 생성
     *
     * EncryptedPrefs 생성/손상 복구 로직은 [EncryptedPrefsHelper.createEncryptedPrefs]가 담당.
     */
    private fun getOrCreateDeviceId(): String {
        val prefs = EncryptedPrefsHelper.createEncryptedPrefs(this)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
            Log.d(TAG, "새 디바이스 ID 생성: $deviceId")
        }
        return deviceId
    }
}

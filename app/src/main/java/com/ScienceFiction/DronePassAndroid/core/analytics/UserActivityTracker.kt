package com.ScienceFiction.DronePassAndroid.core.analytics

import android.util.Log
import com.ScienceFiction.DronePassAndroid.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

internal const val USER_ACTIVITY_MIN_INTERVAL_MILLIS = 15 * 60 * 1_000L

internal fun shouldRecordUserActivity(
    lastRecordedAtMillis: Long?,
    nowMillis: Long,
    minimumIntervalMillis: Long = USER_ACTIVITY_MIN_INTERVAL_MILLIS,
): Boolean {
    return lastRecordedAtMillis == null ||
        nowMillis - lastRecordedAtMillis >= minimumIntervalMillis
}

@Singleton
class UserActivityTracker @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {
    private val writeMutex = Mutex()
    private val lastRecordedAtByUser = mutableMapOf<String, Long>()

    suspend fun recordIfNeeded(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return writeMutex.withLock {
            val user = firebaseAuth.currentUser ?: return@withLock false
            if (!shouldRecordUserActivity(lastRecordedAtByUser[user.uid], nowMillis)) {
                return@withLock false
            }

            runCatching {
                firestore.collection("users")
                    .document(user.uid)
                    .update(
                        mapOf(
                            "lastActiveAt" to FieldValue.serverTimestamp(),
                            "lastActivePlatform" to "android",
                            "lastActiveAppVersion" to BuildConfig.VERSION_NAME,
                        )
                    )
                    .await()
            }.fold(
                onSuccess = {
                    lastRecordedAtByUser[user.uid] = nowMillis
                    true
                },
                onFailure = { error ->
                    Log.w(TAG, "사용자 활동 시각 기록 실패", error)
                    false
                },
            )
        }
    }

    private companion object {
        const val TAG = "UserActivityTracker"
    }
}

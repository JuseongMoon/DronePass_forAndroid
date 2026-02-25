package com.ScienceFiction.DronePassAndroid.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로컬 알림 스케줄러
 * AlarmManager를 사용하여 일출/일몰 알림과 비행 종료일 알림을 예약합니다.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "NotificationScheduler"

        // 일출/일몰 알림 request code 범위
        private const val RC_SUNRISE_30 = 10001
        private const val RC_SUNRISE_10 = 10002
        private const val RC_SUNSET_30 = 10003
        private const val RC_SUNSET_10 = 10004

        // 비행 종료일 알림 request code offset
        private const val RC_END_DATE_OFFSET = 20000

        // Intent extras
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_NOTIFICATION_TITLE = "notification_title"
        const val EXTRA_NOTIFICATION_BODY = "notification_body"
        const val EXTRA_SHAPE_ID = "shape_id"

        // 알림 타입
        const val TYPE_SUNRISE = "sunrise"
        const val TYPE_SUNSET = "sunset"
        const val TYPE_END_DATE = "end_date"
    }

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // ===== 일출 알림 =====

    /**
     * 일출 알림 예약 (30분 전, 10분 전)
     * @param sunriseTimeString ISO 형식 일출 시간 문자열 (예: "2026-02-24T07:15")
     */
    fun scheduleSunriseAlarms(sunriseTimeString: String?) {
        if (sunriseTimeString == null) {
            Log.w(TAG, "일출 시간이 null이므로 알림을 예약할 수 없습니다.")
            return
        }

        val sunriseTime = parseSunTime(sunriseTimeString) ?: return

        val now = LocalDateTime.now()

        // 30분 전 알림
        val before30 = sunriseTime.minusMinutes(30)
        if (before30.isAfter(now)) {
            scheduleAlarm(
                requestCode = RC_SUNRISE_30,
                triggerTime = before30,
                type = TYPE_SUNRISE,
                title = context.getString(com.ScienceFiction.DronePassAndroid.R.string.notification_sunrise_title),
                body = context.getString(com.ScienceFiction.DronePassAndroid.R.string.notification_sunrise_30min_body)
            )
            Log.d(TAG, "일출 30분 전 알림 예약: $before30")
        }

        // 10분 전 알림
        val before10 = sunriseTime.minusMinutes(10)
        if (before10.isAfter(now)) {
            scheduleAlarm(
                requestCode = RC_SUNRISE_10,
                triggerTime = before10,
                type = TYPE_SUNRISE,
                title = context.getString(com.ScienceFiction.DronePassAndroid.R.string.notification_sunrise_title),
                body = context.getString(com.ScienceFiction.DronePassAndroid.R.string.notification_sunrise_10min_body)
            )
            Log.d(TAG, "일출 10분 전 알림 예약: $before10")
        }
    }

    /**
     * 일출 알림 취소
     */
    fun cancelSunriseAlarms() {
        cancelAlarm(RC_SUNRISE_30)
        cancelAlarm(RC_SUNRISE_10)
        Log.d(TAG, "일출 알림 취소됨")
    }

    // ===== 일몰 알림 =====

    /**
     * 일몰 알림 예약 (30분 전, 10분 전)
     * @param sunsetTimeString ISO 형식 일몰 시간 문자열 (예: "2026-02-24T18:00")
     */
    fun scheduleSunsetAlarms(sunsetTimeString: String?) {
        if (sunsetTimeString == null) {
            Log.w(TAG, "일몰 시간이 null이므로 알림을 예약할 수 없습니다.")
            return
        }

        val sunsetTime = parseSunTime(sunsetTimeString) ?: return

        val now = LocalDateTime.now()

        // 30분 전 알림
        val before30 = sunsetTime.minusMinutes(30)
        if (before30.isAfter(now)) {
            scheduleAlarm(
                requestCode = RC_SUNSET_30,
                triggerTime = before30,
                type = TYPE_SUNSET,
                title = context.getString(com.ScienceFiction.DronePassAndroid.R.string.notification_sunset_title),
                body = context.getString(com.ScienceFiction.DronePassAndroid.R.string.notification_sunset_30min_body)
            )
            Log.d(TAG, "일몰 30분 전 알림 예약: $before30")
        }

        // 10분 전 알림
        val before10 = sunsetTime.minusMinutes(10)
        if (before10.isAfter(now)) {
            scheduleAlarm(
                requestCode = RC_SUNSET_10,
                triggerTime = before10,
                type = TYPE_SUNSET,
                title = context.getString(com.ScienceFiction.DronePassAndroid.R.string.notification_sunset_title),
                body = context.getString(com.ScienceFiction.DronePassAndroid.R.string.notification_sunset_10min_body)
            )
            Log.d(TAG, "일몰 10분 전 알림 예약: $before10")
        }
    }

    /**
     * 일몰 알림 취소
     */
    fun cancelSunsetAlarms() {
        cancelAlarm(RC_SUNSET_30)
        cancelAlarm(RC_SUNSET_10)
        Log.d(TAG, "일몰 알림 취소됨")
    }

    // ===== 비행 종료일 알림 =====

    /**
     * 비행 종료일 7일 전 알림 예약
     * @param shapeId 도형 ID
     * @param flightEndDate 비행 종료일 (epoch millis)
     */
    fun scheduleEndDateAlarm(shapeId: String, flightEndDate: Long) {
        val endDateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(flightEndDate),
            ZoneId.systemDefault()
        )

        // 7일 전 오전 9시에 알림
        val notifyTime = endDateTime.minusDays(7)
            .withHour(9)
            .withMinute(0)
            .withSecond(0)

        val now = LocalDateTime.now()
        if (notifyTime.isBefore(now)) {
            Log.d(TAG, "비행 종료일 알림 시간이 이미 지났습니다: shapeId=$shapeId")
            return
        }

        val requestCode = getEndDateRequestCode(shapeId)

        scheduleAlarm(
            requestCode = requestCode,
            triggerTime = notifyTime,
            type = TYPE_END_DATE,
            title = context.getString(com.ScienceFiction.DronePassAndroid.R.string.notification_end_date_title),
            body = context.getString(com.ScienceFiction.DronePassAndroid.R.string.notification_end_date_body),
            shapeId = shapeId
        )
        Log.d(TAG, "비행 종료일 알림 예약: shapeId=$shapeId, notifyTime=$notifyTime")
    }

    /**
     * 비행 종료일 알림 취소
     * @param shapeId 도형 ID
     */
    fun cancelEndDateAlarm(shapeId: String) {
        val requestCode = getEndDateRequestCode(shapeId)
        cancelAlarm(requestCode)
        Log.d(TAG, "비행 종료일 알림 취소: shapeId=$shapeId")
    }

    // ===== 내부 메서드 =====

    /**
     * AlarmManager를 사용하여 정확한 알림 예약
     */
    private fun scheduleAlarm(
        requestCode: Int,
        triggerTime: LocalDateTime,
        type: String,
        title: String,
        body: String,
        shapeId: String? = null
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_TYPE, type)
            putExtra(EXTRA_NOTIFICATION_TITLE, title)
            putExtra(EXTRA_NOTIFICATION_BODY, body)
            if (shapeId != null) {
                putExtra(EXTRA_SHAPE_ID, shapeId)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = triggerTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ : canScheduleExactAlarms() 확인
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    // 정확한 알림 권한이 없으면 일반 알림 사용
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.w(TAG, "정확한 알림 권한이 없어 일반 알림으로 예약합니다.")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "알림 예약 실패: 권한 부족", e)
            // Fallback: 비정확한 알림 사용
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /**
     * 예약된 알림 취소
     */
    private fun cancelAlarm(requestCode: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /**
     * 일출/일몰 시간 문자열 파싱
     * Open-Meteo API는 "2026-02-24T07:15" 형식을 반환
     */
    private fun parseSunTime(timeString: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (e: Exception) {
            try {
                // "HH:mm" 형식인 경우 오늘 날짜와 결합
                val time = LocalTime.parse(timeString)
                LocalDateTime.of(LocalDate.now(), time)
            } catch (e2: Exception) {
                Log.e(TAG, "시간 파싱 실패: $timeString", e2)
                null
            }
        }
    }

    /**
     * shapeId에서 고유한 request code 생성
     * hashCode를 사용하여 양수 범위로 변환
     */
    private fun getEndDateRequestCode(shapeId: String): Int {
        return RC_END_DATE_OFFSET + (shapeId.hashCode() and 0x7FFFFFFF) % 10000
    }
}

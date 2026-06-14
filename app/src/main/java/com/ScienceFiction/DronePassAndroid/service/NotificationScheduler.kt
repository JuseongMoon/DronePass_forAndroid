package com.ScienceFiction.DronePassAndroid.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ScienceFiction.DronePassAndroid.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

internal fun calculateEndDateNotificationTime(
    flightEndDate: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): LocalDateTime {
    return truncateNotificationTriggerToMinute(
        LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(flightEndDate),
            zoneId,
        ).minusDays(7),
    )
}

internal fun truncateNotificationTriggerToMinute(triggerTime: LocalDateTime): LocalDateTime {
    return triggerTime.withSecond(0).withNano(0)
}

internal fun resolveNotificationSunZone(utcOffsetSeconds: Int?): ZoneId {
    return runCatching {
        utcOffsetSeconds?.let(ZoneOffset::ofTotalSeconds)
    }.getOrNull() ?: ZoneId.systemDefault()
}

internal fun calculateNotificationTriggerAtMillis(
    triggerTime: LocalDateTime,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Long {
    return truncateNotificationTriggerToMinute(triggerTime)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()
}

internal fun parseNotificationSunTime(
    timeString: String,
    fallbackDate: LocalDate = LocalDate.now(),
): LocalDateTime? {
    return try {
        LocalDateTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    } catch (e: Exception) {
        try {
            LocalDateTime.of(fallbackDate, LocalTime.parse(timeString))
        } catch (e2: Exception) {
            null
        }
    }
}

internal fun selectNextSunEventTime(
    timeStrings: List<String>?,
    now: LocalDateTime = LocalDateTime.now(),
): LocalDateTime? {
    return timeStrings
        .orEmpty()
        .mapNotNull { parseNotificationSunTime(it, fallbackDate = now.toLocalDate()) }
        .filter { it.isAfter(now) }
        .minOrNull()
        ?.let(::truncateNotificationTriggerToMinute)
}

internal data class SunAlarmSchedulePlan(
    val shouldCancelExisting: Boolean,
    val nextEventTime: LocalDateTime?,
)

internal fun resolveSunAlarmSchedulePlan(
    timeStrings: List<String>?,
    now: LocalDateTime = LocalDateTime.now(),
): SunAlarmSchedulePlan {
    return SunAlarmSchedulePlan(
        shouldCancelExisting = true,
        nextEventTime = selectNextSunEventTime(timeStrings, now),
    )
}

internal data class NotificationContentResourceIds(
    val title: Int,
    val body: Int,
)

internal data class EndDateNotificationBodyResource(
    val body: Int,
    val shapeTitle: String? = null,
)

internal fun endDateNotificationBodyResource(shapeTitle: String?): EndDateNotificationBodyResource {
    return shapeTitle
        ?.let {
            EndDateNotificationBodyResource(
                body = R.string.notification_end_date_body_with_title,
                shapeTitle = it,
            )
        }
        ?: EndDateNotificationBodyResource(body = R.string.notification_end_date_body)
}

internal fun sunriseNotificationContentResources(minutesBefore: Int): NotificationContentResourceIds {
    return when (minutesBefore) {
        30 -> NotificationContentResourceIds(
            title = R.string.notification_sunrise_30min_title,
            body = R.string.notification_sunrise_30min_body,
        )
        10 -> NotificationContentResourceIds(
            title = R.string.notification_sunrise_10min_title,
            body = R.string.notification_sunrise_10min_body,
        )
        else -> error("Unsupported sunrise notification offset: $minutesBefore")
    }
}

internal fun sunsetNotificationContentResources(minutesBefore: Int): NotificationContentResourceIds {
    return when (minutesBefore) {
        30 -> NotificationContentResourceIds(
            title = R.string.notification_sunset_30min_title,
            body = R.string.notification_sunset_30min_body,
        )
        10 -> NotificationContentResourceIds(
            title = R.string.notification_sunset_10min_title,
            body = R.string.notification_sunset_10min_body,
        )
        else -> error("Unsupported sunset notification offset: $minutesBefore")
    }
}

internal fun endDateNotificationAction(shapeId: String): String {
    return "com.ScienceFiction.DronePassAndroid.notification.END_DATE.$shapeId"
}

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

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    // ===== 일출 알림 =====

    /**
     * 일출 알림 예약 (30분 전, 10분 전)
     * @param sunriseTimeString ISO 형식 일출 시간 문자열 (예: "2026-02-24T07:15")
     */
    fun scheduleSunriseAlarms(sunriseTimeString: String?, utcOffsetSeconds: Int? = null) {
        scheduleSunriseAlarms(sunriseTimeString?.let(::listOf), utcOffsetSeconds)
    }

    /**
     * 일출 알림 예약 (30분 전, 10분 전)
     * iOS처럼 오늘 일출이 지났으면 전달된 후보 중 다음 미래 일출을 사용합니다.
     */
    fun scheduleSunriseAlarms(sunriseTimeStrings: List<String>?, utcOffsetSeconds: Int? = null) {
        val zoneId = resolveNotificationSunZone(utcOffsetSeconds)
        val now = LocalDateTime.now(zoneId)
        val plan = resolveSunAlarmSchedulePlan(sunriseTimeStrings, now)
        if (plan.shouldCancelExisting) {
            cancelSunriseAlarms()
        }

        val sunriseTime = plan.nextEventTime ?: run {
            if (sunriseTimeStrings.isNullOrEmpty()) {
                Log.w(TAG, "일출 시간이 null이므로 알림을 예약할 수 없습니다.")
            } else {
                Log.w(TAG, "예약 가능한 미래 일출 시간이 없습니다.")
            }
            return
        }

        // 30분 전 알림
        val before30 = sunriseTime.minusMinutes(30)
        if (before30.isAfter(now)) {
            val content = sunriseNotificationContentResources(minutesBefore = 30)
            scheduleAlarm(
                requestCode = RC_SUNRISE_30,
                triggerTime = before30,
                type = TYPE_SUNRISE,
                title = context.getString(content.title),
                body = context.getString(content.body),
                zoneId = zoneId,
            )
            Log.d(TAG, "일출 30분 전 알림 예약: $before30")
        }

        // 10분 전 알림
        val before10 = sunriseTime.minusMinutes(10)
        if (before10.isAfter(now)) {
            val content = sunriseNotificationContentResources(minutesBefore = 10)
            scheduleAlarm(
                requestCode = RC_SUNRISE_10,
                triggerTime = before10,
                type = TYPE_SUNRISE,
                title = context.getString(content.title),
                body = context.getString(content.body),
                zoneId = zoneId,
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
    fun scheduleSunsetAlarms(sunsetTimeString: String?, utcOffsetSeconds: Int? = null) {
        scheduleSunsetAlarms(sunsetTimeString?.let(::listOf), utcOffsetSeconds)
    }

    /**
     * 일몰 알림 예약 (30분 전, 10분 전)
     * iOS처럼 오늘 일몰이 지났으면 전달된 후보 중 다음 미래 일몰을 사용합니다.
     */
    fun scheduleSunsetAlarms(sunsetTimeStrings: List<String>?, utcOffsetSeconds: Int? = null) {
        val zoneId = resolveNotificationSunZone(utcOffsetSeconds)
        val now = LocalDateTime.now(zoneId)
        val plan = resolveSunAlarmSchedulePlan(sunsetTimeStrings, now)
        if (plan.shouldCancelExisting) {
            cancelSunsetAlarms()
        }

        val sunsetTime = plan.nextEventTime ?: run {
            if (sunsetTimeStrings.isNullOrEmpty()) {
                Log.w(TAG, "일몰 시간이 null이므로 알림을 예약할 수 없습니다.")
            } else {
                Log.w(TAG, "예약 가능한 미래 일몰 시간이 없습니다.")
            }
            return
        }

        // 30분 전 알림
        val before30 = sunsetTime.minusMinutes(30)
        if (before30.isAfter(now)) {
            val content = sunsetNotificationContentResources(minutesBefore = 30)
            scheduleAlarm(
                requestCode = RC_SUNSET_30,
                triggerTime = before30,
                type = TYPE_SUNSET,
                title = context.getString(content.title),
                body = context.getString(content.body),
                zoneId = zoneId,
            )
            Log.d(TAG, "일몰 30분 전 알림 예약: $before30")
        }

        // 10분 전 알림
        val before10 = sunsetTime.minusMinutes(10)
        if (before10.isAfter(now)) {
            val content = sunsetNotificationContentResources(minutesBefore = 10)
            scheduleAlarm(
                requestCode = RC_SUNSET_10,
                triggerTime = before10,
                type = TYPE_SUNSET,
                title = context.getString(content.title),
                body = context.getString(content.body),
                zoneId = zoneId,
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
     * @param shapeTitle 알림 본문에 표시할 도형명
     */
    fun scheduleEndDateAlarm(
        shapeId: String,
        flightEndDate: Long,
        shapeTitle: String? = null,
    ) {
        cancelEndDateAlarm(shapeId)

        val notifyTime = calculateEndDateNotificationTime(flightEndDate)

        val now = LocalDateTime.now()
        if (!notifyTime.isAfter(now)) {
            Log.d(TAG, "비행 종료일 알림 시간이 이미 지났습니다: shapeId=$shapeId")
            return
        }

        val requestCode = getEndDateRequestCode(shapeId)
        val bodyResource = endDateNotificationBodyResource(shapeTitle)
        val body = bodyResource.shapeTitle
            ?.let { context.getString(bodyResource.body, it) }
            ?: context.getString(bodyResource.body)

        scheduleAlarm(
            requestCode = requestCode,
            triggerTime = notifyTime,
            type = TYPE_END_DATE,
            title = context.getString(R.string.notification_end_date_title),
            body = body,
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
        cancelAlarm(requestCode, action = endDateNotificationAction(shapeId))
        // Migration path for alarms scheduled before end-date actions were made unique.
        cancelAlarm(requestCode, action = null)
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
        shapeId: String? = null,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        val alarmManager = alarmManager ?: run {
            Log.w(TAG, "AlarmManager를 가져올 수 없어 알림 예약을 건너뜁니다.")
            return
        }
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            if (type == TYPE_END_DATE && shapeId != null) {
                action = endDateNotificationAction(shapeId)
            }
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

        val triggerAtMillis = calculateNotificationTriggerAtMillis(triggerTime, zoneId)

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
    private fun cancelAlarm(requestCode: Int, action: String? = null) {
        val alarmManager = alarmManager ?: run {
            Log.w(TAG, "AlarmManager를 가져올 수 없어 알림 취소를 건너뜁니다.")
            return
        }
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            this.action = action
        }
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
     * shapeId에서 고유한 request code 생성
     * hashCode를 사용하여 양수 범위로 변환
     */
    private fun getEndDateRequestCode(shapeId: String): Int {
        return RC_END_DATE_OFFSET + (shapeId.hashCode() and 0x7FFFFFFF) % 10000
    }
}

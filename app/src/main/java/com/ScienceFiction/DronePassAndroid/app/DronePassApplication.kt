package com.ScienceFiction.DronePassAndroid.app

import android.app.Application
import com.ScienceFiction.DronePassAndroid.service.FcmService
import com.ScienceFiction.DronePassAndroid.service.NotificationScheduleRestorer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * DronePass 애플리케이션 클래스.
 *
 * Firebase Crashlytics는 google-services 플러그인과 crashlytics 플러그인에 의해
 * 자동으로 초기화됩니다. 별도의 수동 초기화 코드가 필요하지 않습니다.
 * - 비정상 종료(crash) 보고는 앱 시작 시 자동으로 활성화됩니다.
 * - 디버그 빌드에서 Crashlytics 비활성화가 필요한 경우 AndroidManifest.xml에서
 *   firebase_crashlytics_collection_enabled를 false로 설정할 수 있습니다.
 */
@HiltAndroidApp
class DronePassApplication : Application() {

    @Inject lateinit var notificationScheduleRestorer: NotificationScheduleRestorer

    override fun onCreate() {
        super.onCreate()
        // 알림 채널 생성 (Android 8.0+)
        FcmService.createNotificationChannel(this)
        notificationScheduleRestorer.rescheduleEnabledAlarmsAsync("app_start")
    }
}

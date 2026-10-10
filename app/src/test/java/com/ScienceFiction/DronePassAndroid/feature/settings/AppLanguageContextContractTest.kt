package com.ScienceFiction.DronePassAndroid.feature.settings

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Android 12 이하에서는 앱별 언어를 시스템이 처리하지 않는다(MainActivity 는 ComponentActivity 라 AppCompat 도 무효).
 * 알림·구독 안내처럼 액티비티 밖에서 만드는 문구가 앱 언어를 따르도록 Application·FCM 서비스도 언어를 씌운다.
 */
class AppLanguageContextContractTest {

    private fun source(path: String): String {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        return File(root, "com/ScienceFiction/DronePassAndroid/$path").readText()
    }

    @Test
    fun `application, activity and fcm service attach the app language context`() {
        listOf("app/DronePassApplication.kt", "MainActivity.kt", "service/FcmService.kt").forEach { path ->
            val attach = source(path).substringAfter("override fun attachBaseContext(", "")
            assertTrue(path, attach.contains("localizedAppLanguageContext("))
        }
    }
}

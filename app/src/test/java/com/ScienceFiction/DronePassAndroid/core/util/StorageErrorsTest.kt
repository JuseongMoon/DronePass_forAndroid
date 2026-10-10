package com.ScienceFiction.DronePassAndroid.core.util

import android.database.sqlite.SQLiteFullException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

/**
 * 저장 공간이 가득 차거나 설정 파일이 깨지면 저장 launch 의 예외가 앱을 종료시켰다(3.6.1 에뮬레이터 재현).
 */
class StorageErrorsTest {

    @Test
    fun `storage errors are recognized and other errors are not`() {
        assertTrue(isRecoverableStorageError(IOException("ENOSPC")))
        assertTrue(isRecoverableStorageError(SQLiteFullException("full")))
        assertFalse(isRecoverableStorageError(IllegalStateException("bug")))
    }

    @Test
    fun `storage errors do not escape the launch and other errors still do`() = runBlocking {
        var escaped: Throwable? = null
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined + CoroutineExceptionHandler { _, e -> escaped = e })
        var reported: Exception? = null
        scope.launchCatchingStorageErrors(onStorageError = { reported = it }) { throw IOException("ENOSPC") }.join()
        assertNull(escaped)
        assertEquals("ENOSPC", reported?.message)
        scope.launchCatchingStorageErrors(onStorageError = { reported = it }) { throw IllegalStateException("bug") }.join()
        assertEquals("bug", escaped?.message)
    }

    @Test
    fun `view models that write to device storage use the catching launch`() {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        listOf(
            "feature/sketch/SketchViewModel.kt",
            "feature/saved/SavedListViewModel.kt",
            "feature/drone/DroneViewModel.kt",
            "feature/map/MapViewModel.kt",
            "feature/settings/SettingsViewModel.kt",
            "feature/legal/LegalLaunchViewModel.kt",
            "feature/legal/LocationConsentSettingsViewModel.kt",
            "feature/weather/WeatherViewModel.kt",
        ).forEach { path ->
            val text = File(root, "com/ScienceFiction/DronePassAndroid/$path").readText()
            assertFalse(path, text.contains("viewModelScope.launch {"))
        }
        val module = File(root, "com/ScienceFiction/DronePassAndroid/core/di/AppModule.kt").readText()
        assertTrue(module.contains("corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }"))
    }
}

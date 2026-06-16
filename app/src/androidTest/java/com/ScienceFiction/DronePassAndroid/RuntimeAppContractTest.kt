package com.ScienceFiction.DronePassAndroid

import android.content.pm.ApplicationInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class RuntimeAppContractTest {

    @Test
    fun installedAppKeepsSharedFirebasePackageName() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals("com.ScienceFiction.DronePassAndroid", BuildConfig.APPLICATION_ID)
        assertEquals(BuildConfig.APPLICATION_ID, appContext.packageName)
    }

    @Test
    fun installedAppDisablesAndroidOsBackupAtRuntime() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals(0, appContext.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }
}

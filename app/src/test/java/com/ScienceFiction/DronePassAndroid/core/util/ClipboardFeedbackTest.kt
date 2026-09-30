package com.ScienceFiction.DronePassAndroid.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardFeedbackTest {

    @Test
    fun `Android 13 미만에서만 앱 복사 토스트를 띄운다`() {
        assertTrue(shouldShowAppCopyConfirmation(sdkInt = 28))
        assertTrue(shouldShowAppCopyConfirmation(sdkInt = 32))
        assertFalse(shouldShowAppCopyConfirmation(sdkInt = 33))
        assertFalse(shouldShowAppCopyConfirmation(sdkInt = 36))
    }
}

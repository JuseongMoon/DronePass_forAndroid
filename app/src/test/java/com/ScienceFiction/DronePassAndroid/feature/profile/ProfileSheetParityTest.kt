package com.ScienceFiction.DronePassAndroid.feature.profile

import org.junit.Assert.assertFalse
import org.junit.Test

class ProfileSheetParityTest {

    @Test
    fun `약관 문서 시트는 iOS medium large detent 처럼 부분 확장을 허용한다`() {
        assertFalse(ProfileDocumentSheetSkipPartiallyExpanded)
    }
}

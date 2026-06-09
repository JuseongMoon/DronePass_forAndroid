package com.ScienceFiction.DronePassAndroid.feature.profile

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSheetParityTest {

    @Test
    fun `약관 문서 시트는 iOS medium large detent 처럼 부분 확장을 허용한다`() {
        assertFalse(ProfileDocumentSheetSkipPartiallyExpanded)
    }

    @Test
    fun `로그아웃 행은 iOS처럼 내 정보 섹션과 분리한다`() {
        assertEquals(10.dp, ProfileInfoToLogoutSectionSpacing)
        assertEquals(16.dp, ProfileLogoutToSyncSectionSpacing)
    }
}

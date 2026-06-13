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

    @Test
    fun `내 정보 섹션은 iOS ProfileView처럼 컴팩트 묶음 간격을 사용한다`() {
        assertEquals(12.dp, ProfileInfoSectionVerticalPadding)
        assertEquals(8.dp, ProfileInfoRowSpacing)
        assertEquals(2.dp, ProfileInfoDividerVerticalPadding)
        assertEquals(16.dp, ProfileInfoValueLeadingSpacing)
    }
}

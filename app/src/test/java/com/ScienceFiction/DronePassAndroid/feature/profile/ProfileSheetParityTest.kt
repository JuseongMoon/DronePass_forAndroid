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
    fun `프로필 시트는 iOS NavigationView 처럼 large title 을 사용한다`() {
        assertEquals(true, ProfileSheetUsesLargeNavigationTitle)
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

    @Test
    fun `계정 관리 섹션은 iOS ProfileView처럼 로그인 상태와 무관하게 표시한다`() {
        assertEquals(true, shouldShowProfileAccountSection(isLoggedIn = true))
        assertEquals(true, shouldShowProfileAccountSection(isLoggedIn = false))
    }
}

package com.ScienceFiction.DronePassAndroid.feature.settings

import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsScreenContractTest {

    @Test
    fun `설정 로그인 시트는 iOS SettingView처럼 로그인 없이 시작 버튼을 숨긴다`() {
        assertFalse(SettingsLoginSheetShowSkipLogin)
    }
}

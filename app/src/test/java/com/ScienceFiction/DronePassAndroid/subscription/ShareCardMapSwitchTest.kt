package com.ScienceFiction.DronePassAndroid.subscription

import com.ScienceFiction.DronePassAndroid.feature.shape.effectiveShareCardMapEnabled
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** D-71: 공유 카드 지도 원격 스위치(`features.shareCardMap.enabled`). 이번 실행에서 받은 true 일 때만 지도 카드다. */
class ShareCardMapSwitchTest {

    private val base = """{"schemaVersion":1"""

    private fun parse(features: String?): RemoteSubscriptionConfig =
        requireNotNull(RemoteSubscriptionConfig.parse(if (features == null) "$base}" else """$base,"features":$features}"""))

    @After fun reset() {
        RemoteFeatureFlags.updateFetched(parse(null))
    }

    @Test fun `true 일 때만 켜고 키 없음·null·형식 오류는 끈다`() {
        assertTrue(parse("""{"shareCardMap":{"enabled":true}}""").shareCardMapEnabled)
        assertFalse(parse(null).shareCardMapEnabled)
        assertFalse(parse("""{}""").shareCardMapEnabled)
        assertFalse(parse("""{"shareCardMap":null}""").shareCardMapEnabled)
        assertFalse(parse("""{"shareCardMap":{}}""").shareCardMapEnabled)
        assertFalse(parse("""{"shareCardMap":{"enabled":null}}""").shareCardMapEnabled)
        assertFalse(parse("""{"shareCardMap":{"enabled":"true"}}""").shareCardMapEnabled)
        assertFalse(parse("""{"shareCardMap":{"enabled":1}}""").shareCardMapEnabled)
        assertFalse(parse("""{"shareCardMap":true}""").shareCardMapEnabled)
        assertFalse(parse("""{"shareCardMap":{"enabled":false}}""").shareCardMapEnabled)
    }

    @Test fun `다른 기능 플래그와 섞여도 따로 읽는다`() {
        val config = parse("""{"locationAudit":{"uploadEnabled":true},"proClaim":{"enabled":true},"shareCardMap":{"enabled":false}}""")
        assertTrue(config.locationAuditUploadEnabled)
        assertTrue(config.proClaimEnabled)
        assertFalse(config.shareCardMapEnabled)
    }

    @Test fun `설정 받기 전과 저장본만 있을 때는 끈다`() {
        assertFalse(RemoteFeatureFlags.shareCardMapEnabled.value)
        // 저장본을 되살리는 경로는 true 가 들어 있어도 켜지 않는다(계약 §6)
        RemoteFeatureFlags.update(parse("""{"shareCardMap":{"enabled":true}}"""))
        assertFalse(RemoteFeatureFlags.shareCardMapEnabled.value)
    }

    @Test fun `이번 실행에서 받은 설정을 따른다`() {
        RemoteFeatureFlags.updateFetched(parse("""{"shareCardMap":{"enabled":true}}"""))
        assertTrue(RemoteFeatureFlags.shareCardMapEnabled.value)
        RemoteFeatureFlags.updateFetched(parse("""{"shareCardMap":{"enabled":false}}"""))
        assertFalse(RemoteFeatureFlags.shareCardMapEnabled.value)
    }

    @Test fun `개발 빌드 강제 값이 원격 값보다 앞선다`() {
        assertTrue(effectiveShareCardMapEnabled(remoteFetchedEnabled = false, debugForce = "on"))
        assertFalse(effectiveShareCardMapEnabled(remoteFetchedEnabled = true, debugForce = "off"))
        assertTrue(effectiveShareCardMapEnabled(remoteFetchedEnabled = true, debugForce = ""))
        assertFalse(effectiveShareCardMapEnabled(remoteFetchedEnabled = false, debugForce = ""))
        assertFalse(effectiveShareCardMapEnabled(remoteFetchedEnabled = false, debugForce = "yes"))
    }
}

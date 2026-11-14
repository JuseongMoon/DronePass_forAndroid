package com.ScienceFiction.DronePassAndroid.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 태그 이름 상한 20자(사용자가 보는 글자 단위, D-71 ⑦). iOS fixture 가 나오면 같은 사례로 맞춘다 */
class TagNameLimitTest {

    @Test fun `글자 수는 사용자가 보는 글자 단위로 센다`() {
        assertEquals(0, tagNameLength(""))
        assertEquals(4, tagNameLength("현장답사"))
        assertEquals(5, tagNameLength("Abc 1"))
        // 조합형 한글(자모 3개)도 한 글자
        assertEquals(1, tagNameLength("한"))
        // 결합 악센트
        assertEquals(1, tagNameLength("é"))
        // 이모지: 피부색, ZWJ 가족, 국기
        assertEquals(1, tagNameLength("👍🏽"))
        assertEquals(1, tagNameLength("👨‍👩‍👧"))
        assertEquals(1, tagNameLength("🇰🇷"))
    }

    @Test fun `20자까지 받고 21자는 막는다`() {
        val twenty = "가".repeat(20)
        assertTrue(shouldAcceptTagNameChange(oldName = "가".repeat(19), newName = twenty))
        assertFalse(shouldAcceptTagNameChange(oldName = twenty, newName = twenty + "나"))
        assertTrue(shouldAcceptTagNameChange(oldName = "", newName = "🇰🇷".repeat(20)))
        assertFalse(shouldAcceptTagNameChange(oldName = "", newName = "🇰🇷".repeat(21)))
    }

    @Test fun `이미 20자를 넘은 이름은 줄이는 것만 받는다`() {
        val long = "가".repeat(25)
        assertTrue(shouldAcceptTagNameChange(oldName = long, newName = "가".repeat(24)))
        assertFalse(shouldAcceptTagNameChange(oldName = long, newName = long + "나"))
        assertFalse(isTagNameWithinLimit("가".repeat(24)))
        assertTrue(isTagNameWithinLimit("  " + "가".repeat(20) + "  "))
    }

    @Test fun `같은 이름 판정은 기기 로케일과 상관없다`() {
        assertEquals(tagNameDuplicateKey("TITLE"), tagNameDuplicateKey("title"))
        assertEquals("istanbul", tagNameDuplicateKey("ISTANBUL"))
    }
}

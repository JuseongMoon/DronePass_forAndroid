package com.ScienceFiction.DronePassAndroid.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 태그 이름 규칙(D-71 ⑦) 중 공용 fixture(TagContractTest) 밖의 경계. 공용 사례는 TagContractTest 가 본다.
 */
class TagNameLimitTest {

    @Test fun `Kotlin trim 과 다른 공백 정의를 따른다`() {
        // U+0085(NEL)는 뗀다. Kotlin trim() 은 남긴다
        assertEquals("촬영", TagName.normalized("\u0085촬영\u0085"))
        // U+001C~001F(정보 구분자)는 공백이 아니므로 남긴다. Kotlin trim() 은 뗀다
        assertEquals("\u001C촬영", TagName.normalized("\u001C촬영"))
        // 줄·문단 구분자(Zl·Zp)
        assertEquals("촬영", TagName.normalized(" 촬영 "))
        // 안쪽 공백은 그대로 센다
        assertEquals(4, TagName.length(TagName.normalized(" a  b ")))
    }

    @Test fun `앞 글자 자르기는 문자 경계를 지킨다`() {
        assertEquals("🇰🇷", TagName.prefix("🇰🇷🇯🇵", 1))
        assertEquals("👨‍👩‍👧", TagName.prefix("👨‍👩‍👧가", 1))
        assertEquals("abc", TagName.prefix("abc", 5))
    }

    @Test fun `20자를 넘는 옛 이름은 저장할 수 없고 표시만 줄인다`() {
        val long = "가".repeat(25)
        assertFalse(TagName.isValid(long))
        assertEquals("가".repeat(20) + "…", TagName(long))
        assertTrue(TagName.isValid("  " + "가".repeat(20) + "  "))
    }

    @Test fun `같은 이름 판정은 기기 로케일과 상관없다`() {
        assertEquals(tagNameDuplicateKey("TITLE"), tagNameDuplicateKey("title"))
        assertEquals("istanbul", tagNameDuplicateKey("ISTANBUL"))
    }

    private fun TagName(name: String): String = TagModel(name = name).displayName
}

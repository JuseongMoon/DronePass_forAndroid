package com.ScienceFiction.DronePassAndroid.core.util

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class IosLocalizedStandardSortTest {

    @Test
    fun `localized standard compare 처럼 숫자 run 을 숫자로 비교한다`() {
        assertTrue(
            compareIosLocalizedStandardStrings(
                first = "Drone 2",
                second = "Drone 10",
                locale = Locale.ENGLISH,
            ) < 0,
        )
    }

    @Test
    fun `localized standard compare 처럼 한국어 드론 이름도 숫자를 자연 정렬한다`() {
        assertTrue(
            compareIosLocalizedStandardStrings(
                first = "드론 2",
                second = "드론 10",
                locale = Locale.KOREAN,
            ) < 0,
        )
    }
}

package com.ScienceFiction.DronePassAndroid.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentRepositoryTest {

    @Test
    fun `문서 경로는 iOS처럼 한국어만 원본 txt 파일을 사용한다`() {
        assertEquals(
            "dronepass/version-patches.txt",
            localizedDocumentPath("dronepass/version-patches", "ko"),
        )
        assertEquals(
            "dronepass/version-patches.txt",
            localizedDocumentPath("dronepass/version-patches", "ko-KR"),
        )
    }

    @Test
    fun `문서 경로는 iOS처럼 한국어가 아닌 언어에서 영어 파일을 사용한다`() {
        assertEquals(
            "dronepass/version-patches_en.txt",
            localizedDocumentPath("dronepass/version-patches", "en"),
        )
        assertEquals(
            "dronepass/version-patches_en.txt",
            localizedDocumentPath("dronepass/version-patches", "ja-JP"),
        )
        assertEquals(
            "dronepass/version-patches_en.txt",
            localizedDocumentPath("dronepass/version-patches", null),
        )
    }
}

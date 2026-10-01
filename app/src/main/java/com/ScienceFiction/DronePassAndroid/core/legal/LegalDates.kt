package com.ScienceFiction.DronePassAndroid.core.legal

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * 3.6.0 법무 준수 사양의 날짜 자리표시. 확정되면 값만 바꾼다(iOS 와 같은 값).
 * 값은 `yyyy-MM-dd` 로 두어 문자열 비교가 날짜 비교가 되게 한다.
 */

/** `{N}`: 위치기반서비스 이용약관 게시·시행일. 저장된 동의 버전이 이보다 낮으면 다시 동의를 받는다. */
const val LOCATION_TERMS_VERSION = "2026-10-13"

/** `{T}`: 개정 이용약관·개인정보 처리방침 시행일(N+30). 약관 개정 안내를 한 번만 띄우는 키. */
const val TERMS_NOTICE_VERSION = "2026-11-12"

/** 화면용 날짜. 한국어 "2026년 11월 12일", 영어 "November 12, 2026". */
internal fun formatLegalDate(isoDate: String, locale: Locale): String =
    LocalDate.parse(isoDate).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))

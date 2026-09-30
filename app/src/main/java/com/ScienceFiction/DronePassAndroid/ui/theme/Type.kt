package com.ScienceFiction.DronePassAndroid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * DronePass Typography 토큰.
 *
 * Material 3 typography scale 의 주요 토큰만 명시한다. 미명시 토큰은 M3 기본값을 사용한다.
 * sp 단위로 정의하여 사용자 폰트 크기 설정에 반응.
 */
private val DefaultFontFamily = FontFamily.Default

val Typography = Typography(
    // Display: 최상위 헤드라인 (예: 온보딩, 설정 페이지 헤더 — 현재 미사용)
    displayLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),

    // Headline: 화면 제목 (Kp Forecast, Weather, Saved 등 상단 헤더)
    headlineMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),

    // Title: 카드 / 시트 제목 (도형 상세, 드론 카드)
    titleLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    // iOS .headline (17 semibold) — 시트 내비게이션 제목, 카드 제목
    titleMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),

    // Body: iOS Dynamic Type 기본 크기에 맞춘다. 자간은 iOS 처럼 0.
    // bodyLarge = .body(17), bodyMedium = .subheadline(15), bodySmall = .caption(12)
    // MaterialTheme 이 기본 Text 스타일로 bodyLarge 를 제공하므로, 글자 크기만 지정한 Text 도
    // 이 줄 높이를 물려받는다. 고정 sp 대신 글자 크기 비율(iOS 17/22)로 둬 작은 글자가 22sp 줄을 차지하지 않게 한다.
    bodyLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = (22f / 17f).em,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),

    // Label: 버튼/캡션 (KpLevel 라벨, 비행구역 칩)
    labelLarge = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    // iOS .caption2 (11)
    labelSmall = TextStyle(
        fontFamily = DefaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.sp,
    ),
)

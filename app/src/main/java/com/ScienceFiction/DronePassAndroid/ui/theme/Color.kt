package com.ScienceFiction.DronePassAndroid.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * DronePass 브랜드 팔레트.
 *
 * iOS DronePass(UIUserInterfaceStyle=Light) 와 시각 일관성을 맞추기 위해
 * 앱 전체는 라이트(화이트) 톤 만 사용한다. 따라서 다크 톤 토큰은 정의하지 않는다.
 *
 * - Brand40    : Primary  (iOS systemBlue 와 일치)
 * - Neutral40  : Secondary (회색 — 비활성/보조 텍스트)
 * - Accent40   : Tertiary  (강조 — 비행구역 경고/CRI 위험 등)
 */
val Brand40 = Color(0xFF007AFF)      // iOS systemBlue
val Neutral40 = Color(0xFF5B6470)
val Accent40 = Color(0xFFFF6B35)     // 경고/주의 강조

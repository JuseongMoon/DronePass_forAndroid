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

// iOS 시스템 색 (라이트 모드) — 톤앤매너 기준 토큰.
// 화면 배경/카드/구분선은 iOS insetGrouped 목록과 같은 계열로 맞춘다.
val IosSystemGroupedBackground = Color(0xFFF2F2F7)
val IosSecondarySystemGroupedBackground = Color.White
val IosSystemGray2 = Color(0xFFAEAEB2)
val IosSystemGray3 = Color(0xFFC7C7CC)
val IosSystemGray4 = Color(0xFFD1D1D6)
val IosSystemGray5 = Color(0xFFE5E5EA)
val IosSeparator = Color(0xFFC6C6C8)
val IosLabel = Color(0xFF1C1C1E)
val IosSecondaryLabel = Color(0xFF8A8A8E)
val IosSystemRed = Color(0xFFFF3B30)
val IosSystemGreen = Color(0xFF34C759)

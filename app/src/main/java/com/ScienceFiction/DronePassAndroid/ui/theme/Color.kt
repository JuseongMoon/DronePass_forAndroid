package com.ScienceFiction.DronePassAndroid.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * DronePass 브랜드 팔레트.
 *
 * iOS DronePass 와 시각적 일관성을 위해 iOS systemBlue 계열을 Primary 로 사용한다.
 * Material 3 명명 규칙에 따라 Light(40 톤)와 Dark(80 톤)를 분리한다.
 *
 * - Brand40 / Brand80   : Primary  (Light / Dark)
 * - Neutral40 / Neutral80 : Secondary (회색 — 비활성/보조 텍스트)
 * - Accent40 / Accent80 : Tertiary  (강조 — 비행구역 경고/CRI 위험 등)
 *
 * 기존 Purple/Pink 템플릿 토큰명은 호환을 위해 alias 로 유지되었으나, 신규 코드는
 * Brand/Neutral/Accent 토큰을 사용한다.
 */

// Light tone (40)
val Brand40 = Color(0xFF007AFF)      // iOS systemBlue
val Neutral40 = Color(0xFF5B6470)
val Accent40 = Color(0xFFFF6B35)     // 경고/주의 강조

// Dark tone (80)
val Brand80 = Color(0xFF6FB4FF)      // systemBlue 의 다크 모드 톤
val Neutral80 = Color(0xFFC8CDD5)
val Accent80 = Color(0xFFFFA07A)

// === 기존 템플릿 alias (Theme.kt 의 placeholder 호환) ===
// 신규 코드는 위의 Brand/Neutral/Accent 토큰을 직접 사용한다.
@Deprecated("Use Brand80", replaceWith = ReplaceWith("Brand80"))
val Purple80 = Brand80
@Deprecated("Use Neutral80", replaceWith = ReplaceWith("Neutral80"))
val PurpleGrey80 = Neutral80
@Deprecated("Use Accent80", replaceWith = ReplaceWith("Accent80"))
val Pink80 = Accent80
@Deprecated("Use Brand40", replaceWith = ReplaceWith("Brand40"))
val Purple40 = Brand40
@Deprecated("Use Neutral40", replaceWith = ReplaceWith("Neutral40"))
val PurpleGrey40 = Neutral40
@Deprecated("Use Accent40", replaceWith = ReplaceWith("Accent40"))
val Pink40 = Accent40

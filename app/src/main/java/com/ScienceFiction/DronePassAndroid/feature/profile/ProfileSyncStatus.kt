package com.ScienceFiction.DronePassAndroid.feature.profile

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.ScienceFiction.DronePassAndroid.R

/**
 * 실시간 클라우드 동기화 상태 (iOS `realtimeCloudSyncStatusText` 5종 정합).
 * ProfileViewModel 가 발행, ProfileScreen 이 stringResource + Color 로 매핑.
 */
enum class ProfileSyncStatus(@StringRes val labelRes: Int, val color: Color) {
    /** "Syncing..." — 동기화 진행 중. */
    Syncing(R.string.profile_sync_in_progress, Color(0xFF007AFF)),

    /** "Login required" — 비로그인 상태. */
    LoginRequired(R.string.profile_sync_login_required, Color(0xFFFF9500)),

    /** "Disabled" — 토글 OFF. */
    Disabled(R.string.profile_sync_disabled, Color(0xFF8E8E93)),

    /** "Active - Real-time syncing" — Snapshot listener 활성. */
    Active(R.string.profile_sync_active, Color(0xFF34C759)),

    /** "Active - Waiting for sync" — 토글 ON 이지만 리스너 미가동. */
    Waiting(R.string.profile_sync_waiting, Color(0xFFFF9500)),
}

package com.ScienceFiction.DronePassAndroid.feature.profile

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.ScienceFiction.DronePassAndroid.R

/**
 * 실시간 클라우드 동기화 상태 (iOS `realtimeCloudSyncStatusText` 정합).
 * ProfileViewModel 가 발행, ProfileScreen 이 stringResource + Color 로 매핑.
 * 클라우드 동기화 토글은 없다(로그인 중에는 항상 동기화, 기기 데이터 주인 3.6.0).
 */
enum class ProfileSyncStatus(@StringRes val labelRes: Int, val color: Color) {
    /** "Syncing..." — 동기화 진행 중. */
    Syncing(R.string.profile_sync_in_progress, Color(0xFF007AFF)),

    /** "Login required" — 비로그인 상태. */
    LoginRequired(R.string.profile_sync_login_required, Color(0xFFFF9500)),

    /** "가져오기 확인 필요" — 로그인 전 데이터를 계정으로 가져올지 묻는 중. 관문이 닫혀 동기화하지 않는다. */
    ImportPending(R.string.profile_sync_import_pending, Color(0xFFFF9500)),

    /** "Active - Real-time syncing" — Snapshot listener 활성. */
    Active(R.string.profile_sync_active, Color(0xFF34C759)),

    /** "Active - Waiting for sync" — 로그인했지만 리스너 미가동. */
    Waiting(R.string.profile_sync_waiting, Color(0xFFFF9500)),
}

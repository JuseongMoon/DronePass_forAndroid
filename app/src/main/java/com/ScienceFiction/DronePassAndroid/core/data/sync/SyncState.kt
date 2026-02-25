package com.ScienceFiction.DronePassAndroid.core.data.sync

/**
 * 실시간 동기화 상태를 나타내는 sealed class
 */
sealed class SyncState {
    /** 유휴 상태 - 동기화가 진행 중이지 않음 */
    data object Idle : SyncState()

    /** 동기화 진행 중 */
    data object Syncing : SyncState()

    /** 동기화 성공 */
    data class Success(val timestamp: Long) : SyncState()

    /** 동기화 오류 발생 */
    data class Error(val message: String) : SyncState()
}

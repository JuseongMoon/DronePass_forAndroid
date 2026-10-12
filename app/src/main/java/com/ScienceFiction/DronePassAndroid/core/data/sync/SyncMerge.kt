package com.ScienceFiction.DronePassAndroid.core.data.sync

/**
 * LWW (Last Write Wins) 머지 결과.
 *
 * @param merged 머지된 전체 데이터셋 (로컬 ∪ 서버, 충돌 시 updatedAt 큰 쪽).
 * @param toUpload 서버에 업로드해야 하는 항목들 (서버에 없거나 로컬이 더 최신).
 */
data class SyncMergeResult<T>(
    val merged: List<T>,
    val toUpload: List<T>,
)

/**
 * performFullSync 후 서버 metadata 문서를 다시 갱신해야 하는지 판단한다.
 *
 * iOS 실시간 동기화는 서버 변경을 내려받아 로컬에 적용하는 것만으로는 metadata/server 를
 * 다시 쓰지 않는다. Android도 실제 업로드가 있을 때만 metadata 를 갱신해야 다른 기기의
 * 리스너가 다운로드 전용 동기화에 다시 반응하는 루프를 만들지 않는다.
 */
fun shouldUpdateServerMetadataAfterFullSync(uploadedItemCount: Int): Boolean {
    return uploadedItemCount > 0
}

/**
 * 전체 동기화가 서버를 읽는 동안 기기에서 새로 생기거나 updatedAt 이 바뀐 항목 id.
 * 동기화는 시작할 때 읽은 기기 목록으로 병합하므로, 이 항목을 그대로 쓰면 그 사이 사용자가 고치거나 지운 내용이
 * 이전 값으로 되돌아갔다. 이 항목은 이번 반영·삭제·업로드에서 빼고, 사용자 쓰기 경로와 다음 동기화에 맡긴다.
 */
fun idsChangedLocallyDuringSync(
    snapshotUpdatedAt: Map<String, Long>,
    currentUpdatedAt: Map<String, Long>,
): Set<String> = currentUpdatedAt.filter { (id, updatedAt) -> snapshotUpdatedAt[id] != updatedAt }.keys

/**
 * 같은 쓰기로 보는 updatedAt 차이(ms). 계약 "삭제·충돌", iOS `AccountSessionRules.updatedAtTolerance` 와 같다.
 * Firestore 가 마이크로초 아래를 잘라 iOS 기기 값이 서버 값보다 아주 조금 새로워 보이므로 1ms 이하 차이는 같은 시각으로 본다.
 */
const val UPDATED_AT_TOLERANCE_MILLIS = 1L

/** [a] 가 [than] 보다 허용 오차를 넘게 새로운가. 같은 시각이면 업로드하지 않고 병합에서는 서버 값을 쓴다. */
fun isNewerUpdatedAt(a: Long, than: Long): Boolean = a - than > UPDATED_AT_TOLERANCE_MILLIS

/**
 * 로컬·서버 양쪽 데이터를 LWW 전략으로 머지한다.
 *
 * - 한쪽에만 있으면 그쪽 값 채택
 * - 양쪽 모두 있으면 updatedAt 이 더 큰 쪽 채택 (허용 오차 안이면 서버 우선)
 * - 서버에 없거나 로컬이 허용 오차를 넘게 새로운 항목을 toUpload 로 분리
 *
 * Shape/Drone/Sketch Repository 의 performFullSync 가 동일 로직을 갖고 있던 것을
 * 단일 진실 공급원으로 정리한다. 변경 시 각 Repository 의 sync 동작이 함께 바뀌므로
 * 동기화 회귀 테스트를 반드시 수반해야 한다.
 */
inline fun <T : Any> mergeLWW(
    local: List<T>,
    server: List<T>,
    idOf: (T) -> String,
    updatedAtOf: (T) -> Long,
): SyncMergeResult<T> {
    val serverById = server.associateBy(idOf)
    val localById = local.associateBy(idOf)
    val allIds = serverById.keys + localById.keys

    val merged = allIds.mapNotNull { id ->
        val localItem = localById[id]
        val serverItem = serverById[id]
        when {
            localItem != null && serverItem != null -> {
                if (isNewerUpdatedAt(updatedAtOf(localItem), than = updatedAtOf(serverItem))) localItem else serverItem
            }
            localItem != null -> localItem
            else -> serverItem
        }
    }

    val toUpload = merged.filter { item ->
        val srv = serverById[idOf(item)]
        srv == null || isNewerUpdatedAt(updatedAtOf(item), than = updatedAtOf(srv))
    }

    return SyncMergeResult(merged, toUpload)
}

/**
 * 서버 데이터 중 LWW 통과한 항목 (= 로컬에 없거나 로컬이 허용 오차를 넘게 새롭지 않음) 만 추출한다.
 *
 * syncFromFirebase 에서 forEach { local 비교 후 insert } 패턴을 대체하여
 * 배치 insert 한 번으로 적용 가능하게 한다.
 */
inline fun <T : Any> filterServerNewer(
    local: List<T>,
    server: List<T>,
    idOf: (T) -> String,
    updatedAtOf: (T) -> Long,
): List<T> {
    val localById = local.associateBy(idOf)
    return server.filter { srv ->
        val loc = localById[idOf(srv)]
        loc == null || !isNewerUpdatedAt(updatedAtOf(loc), than = updatedAtOf(srv))
    }
}

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
 * 로컬·서버 양쪽 데이터를 LWW 전략으로 머지한다.
 *
 * - 한쪽에만 있으면 그쪽 값 채택
 * - 양쪽 모두 있으면 updatedAt 이 더 큰 쪽 채택 (동률이면 서버 우선)
 * - 서버에 없거나 로컬이 strictly newer 인 항목을 toUpload 로 분리
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

    val merged = allIds.map { id ->
        val localItem = localById[id]
        val serverItem = serverById[id]
        when {
            localItem == null -> serverItem!!
            serverItem == null -> localItem
            updatedAtOf(serverItem) >= updatedAtOf(localItem) -> serverItem
            else -> localItem
        }
    }

    val toUpload = merged.filter { item ->
        val srv = serverById[idOf(item)]
        srv == null || updatedAtOf(item) > updatedAtOf(srv)
    }

    return SyncMergeResult(merged, toUpload)
}

/**
 * 서버 데이터 중 LWW 통과한 항목 (= 로컬에 없거나 서버가 더 새로움) 만 추출한다.
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
        loc == null || updatedAtOf(srv) >= updatedAtOf(loc)
    }
}

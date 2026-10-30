package com.ScienceFiction.DronePassAndroid.domain.model

import com.ScienceFiction.DronePassAndroid.core.data.sync.isNewerUpdatedAt
import com.ScienceFiction.DronePassAndroid.core.util.compareIosLocalizedStandardStrings
import java.util.Locale

/**
 * 도형 태그 (도형당 하나, 색 없음). 경로 `users/{uid}/tags/{id}`.
 * iOS `TagModel` 과 같은 계약이다 (team/DATA_CONTRACT.md "태그").
 */
data class TagModel(
    val id: String = newCanonicalFirestoreUuid(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
) {
    val isDeleted: Boolean
        get() = deletedAt != null

    fun rename(name: String, now: Long = System.currentTimeMillis()): TagModel =
        copy(name = name, updatedAt = now)

    fun softDelete(now: Long = System.currentTimeMillis()): TagModel =
        copy(deletedAt = now, updatedAt = now)
}

/** 활성 태그 (이름 가나다순, iOS localizedStandardCompare 정렬) */
fun List<TagModel>.activeTags(locale: Locale = Locale.getDefault()): List<TagModel> =
    filter { !it.isDeleted }.sortedWith(tagNameComparator(locale))

/** id 로 활성 태그를 찾는다. 삭제됐거나 없으면 null(= 태그 없음) */
fun List<TagModel>.activeTag(id: String?): TagModel? =
    id?.let { tagId -> firstOrNull { it.id == tagId && !it.isDeleted } }

private fun tagNameComparator(locale: Locale): Comparator<TagModel> = Comparator { first, second ->
    val order = compareIosLocalizedStandardStrings(first.name, second.name, locale)
    if (order != 0) order else first.id.compareTo(second.id)
}

/**
 * 태그 필터 (지도·저장 목록 공용). 숨길 키를 저장하므로 새 태그는 자동으로 보인다. iOS `TagFilter` 정합.
 */
object TagFilter {
    /** 태그 없는 도형의 필터 키 (가리키는 태그가 활성이 아닌 도형 포함) */
    const val UNTAGGED_KEY = "__untagged__"

    /** 도형의 필터 키: 활성 태그를 가리키면 그 id, 아니면 태그 없음 */
    fun key(tagId: String?, activeTagIds: Set<String>): String =
        if (tagId != null && tagId in activeTagIds) tagId else UNTAGGED_KEY

    /** 보이는가. 활성 태그가 하나도 없으면 필터 UI 가 없으므로 거르지 않는다(숨김 상태로 갇히지 않게) */
    fun isVisible(tagId: String?, activeTagIds: Set<String>, hidden: Set<String>): Boolean {
        if (activeTagIds.isEmpty() || hidden.isEmpty()) return true
        return key(tagId, activeTagIds) !in hidden
    }

    /** 실제로 거르고 있는가 (지금 있는 키 중 하나라도 숨김) */
    fun isFiltering(activeTagIds: Set<String>, hidden: Set<String>): Boolean {
        if (activeTagIds.isEmpty()) return false
        return hidden.any { it in activeTagIds || it == UNTAGGED_KEY }
    }
}

internal data class TagMergeResult(
    val merged: List<TagModel>,
    /** 서버에 올려야 할 태그 (서버에 없거나 기기가 더 최신) */
    val toUpload: List<TagModel>,
)

/**
 * 기기·서버 태그를 id 기준 LWW(`updatedAt`, 1ms 이내는 같음 → 서버)로 병합한다. 삭제 표시도 일반 값처럼 비교한다.
 * 서버에 없는 기기의 삭제 표시는 올리지 않는다. 이름이 같아도 id 가 다르면 합치지 않는다. iOS `TagRepository.merge` 정합.
 */
internal fun mergeTags(
    local: List<TagModel>,
    server: List<TagModel>,
    locale: Locale = Locale.getDefault(),
): TagMergeResult {
    val result = LinkedHashMap<String, TagModel>()
    val toUpload = LinkedHashMap<String, TagModel>()
    val serverIds = server.mapTo(HashSet()) { it.id }

    local.forEach { tag ->
        result[tag.id] = tag
        if (!(tag.isDeleted && tag.id !in serverIds)) toUpload[tag.id] = tag
    }
    server.forEach { serverTag ->
        val localTag = result[serverTag.id]
        if (localTag != null && isNewerUpdatedAt(localTag.updatedAt, than = serverTag.updatedAt)) return@forEach
        result[serverTag.id] = serverTag
        toUpload.remove(serverTag.id)
    }

    val comparator = tagNameComparator(locale)
    return TagMergeResult(
        merged = result.values.sortedWith(comparator),
        toUpload = toUpload.values.sortedWith(comparator),
    )
}

/**
 * 가져오기: 계정에 없을 태그(서버·업로드 모두 활성이 아님)를 가리키는 도형은 태그를 빼고 `updatedAt` 을 올린다.
 * iOS `TagRepository.clearingTagIds(_:notIn:now:)` 정합.
 */
internal fun clearingTagIds(shapes: List<ShapeModel>, accountTagIds: Set<String>, now: Long): List<ShapeModel> =
    shapes.map { shape ->
        val tagId = shape.tagId
        if (tagId == null || tagId in accountTagIds) shape else shape.copy(tagId = null, updatedAt = now)
    }

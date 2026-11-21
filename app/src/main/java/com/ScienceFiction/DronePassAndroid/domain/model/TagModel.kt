package com.ScienceFiction.DronePassAndroid.domain.model

import com.ScienceFiction.DronePassAndroid.core.data.sync.isNewerUpdatedAt
import com.ScienceFiction.DronePassAndroid.core.util.nameThenIdComparator
import java.text.BreakIterator
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

/**
 * 태그 이름 규칙(D-71 ⑦, team/DATA_CONTRACT.md "태그 · 이름", fixture tag-cases.json nameCases·nameDisplayCases·nameInputCases).
 * 앞뒤 공백을 뗀 뒤 사용자가 보는 글자(확장 문자소) 1~20자. 정규화(NFC 등)는 하지 않는다. 서버는 길이를 검사하지 않는다.
 * iOS `TagName` 정합.
 */
object TagName {
    const val MAX_LENGTH = 20

    /** 앞뒤에서 떼는 공백: Unicode Zs·Zl·Zp 와 U+0009~U+000D·U+0085 (Kotlin trim() 은 U+001C~001F 를 떼고 U+0085 를 남겨 다르다) */
    private fun isTrimmable(codePoint: Int): Boolean =
        codePoint in 0x09..0x0D || codePoint == 0x85 || when (Character.getType(codePoint)) {
            Character.SPACE_SEPARATOR.toInt(),
            Character.LINE_SEPARATOR.toInt(),
            Character.PARAGRAPH_SEPARATOR.toInt(),
            -> true
            else -> false
        }

    fun normalized(raw: String): String {
        var start = 0
        var end = raw.length
        while (start < end && isTrimmable(raw.codePointAt(start))) start += Character.charCount(raw.codePointAt(start))
        while (end > start && isTrimmable(raw.codePointBefore(end))) end -= Character.charCount(raw.codePointBefore(end))
        return raw.substring(start, end)
    }

    /** 문자 경계(확장 문자소) 끝 위치들 */
    private fun boundaries(name: String): List<Int> {
        if (name.isEmpty()) return emptyList()
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(name) }
        val ends = mutableListOf<Int>()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            ends += end
            end = iterator.next()
        }
        return ends
    }

    /** 사용자가 보는 글자 수 */
    fun length(name: String): Int = boundaries(name).size

    /** 앞에서부터 [count] 글자 */
    fun prefix(name: String, count: Int): String {
        val ends = boundaries(name)
        return if (ends.size <= count) name else name.substring(0, ends[count - 1])
    }

    /** 저장해도 되는 이름인가: 공백을 뗀 뒤 1~20자 */
    fun isValid(raw: String): Boolean {
        val name = normalized(raw)
        return name.isNotEmpty() && length(name) <= MAX_LENGTH
    }

    /** 표시용: 20자를 넘는 옛 이름은 앞 20자 + "…" (데이터는 바꾸지 않는다) */
    fun display(name: String): String = if (length(name) > MAX_LENGTH) prefix(name, MAX_LENGTH) + "\u2026" else name

    data class LimitedInput(val text: String, val truncated: Boolean)

    /**
     * 입력란 제한. 이미 20자를 넘는 옛 이름은 줄이는 변경(지우기)만 받는다. 20자 이하에서 넘치게 들어오면(붙여 넣기 등)
     * 앞에서부터 20자까지만 남기고 [LimitedInput.truncated] 를 켠다. 처음 불러올 때는 막지 않는다.
     */
    fun limitedInput(old: String, new: String, isLoading: Boolean): LimitedInput {
        if (isLoading || length(new) <= MAX_LENGTH) return LimitedInput(new, truncated = false)
        if (length(old) > MAX_LENGTH) {
            return if (length(new) < length(old)) LimitedInput(new, truncated = false) else LimitedInput(old, truncated = false)
        }
        return LimitedInput(prefix(new, MAX_LENGTH), truncated = true)
    }
}

/** 화면에 보일 이름(20자를 넘으면 줄인다) */
val TagModel.displayName: String get() = TagName.display(name)

/** 같은 이름 판정용 키. 기기 로케일과 상관없이 같은 결과가 나오게 Locale.ROOT 로 소문자로 바꾼다 */
fun tagNameDuplicateKey(name: String): String = name.lowercase(Locale.ROOT)

/** 활성 태그 (드론과 같은 이름순, 같으면 id) */
fun List<TagModel>.activeTags(): List<TagModel> =
    filter { !it.isDeleted }.sortedWith(tagNameComparator)

/** id 로 활성 태그를 찾는다. 삭제됐거나 없으면 null(= 태그 없음) */
fun List<TagModel>.activeTag(id: String?): TagModel? =
    id?.let { tagId -> firstOrNull { it.id == tagId && !it.isDeleted } }

private val tagNameComparator: Comparator<TagModel> = nameThenIdComparator(TagModel::name, TagModel::id)

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

    val comparator = tagNameComparator
    return TagMergeResult(
        merged = result.values.sortedWith(comparator),
        toUpload = toUpload.values.sortedWith(comparator),
    )
}

package com.ScienceFiction.DronePassAndroid.domain.model

import java.util.UUID

/**
 * iOS `FetchWebDocuments.MarkdownElement` 정합.
 * 자체 마크다운 파서가 plain text 를 파싱해 만든 요소.
 *
 * @property id LazyColumn key 안정성용 (UUID 문자열)
 * @property type 요소 종류
 * @property content 본문 텍스트 (Table 의 경우 table id)
 * @property level 헤더 레벨 (1~6), 그 외 null
 */
data class MarkdownElement(
    val id: String = UUID.randomUUID().toString(),
    val type: MarkdownElementType,
    val content: String,
    val level: Int? = null,
)

enum class MarkdownElementType {
    Header,
    Paragraph,
    Table,
    Separator,
    ListItem,
}

/**
 * 표 데이터 — iOS `TableData` 정합.
 * MarkdownElement(type=Table) 의 content 는 이 [id] 와 매칭된다.
 */
data class TableData(
    val id: String = UUID.randomUUID().toString(),
    val headers: List<String>,
    val rows: List<List<String>>,
)

/**
 * 약관/개인정보 파싱 결과 묶음 — Repository fetch 결과 단일 타입.
 */
data class ParsedDocument(
    val elements: List<MarkdownElement>,
    val tables: List<TableData>,
)

/**
 * iOS `PatchNote` 정합. `v1.0.0 (2025-01-15): 제목` 헤더 + features.
 */
data class PatchNote(
    val version: String,
    val date: String,
    val title: String,
    val features: List<PatchNoteFeature>,
)

/**
 * iOS `Feature` 정합. `- 제목` + 들여쓰기된 `    - 설명` (멀티라인은 \n 으로 join).
 */
data class PatchNoteFeature(
    val title: String,
    val description: String?,
)

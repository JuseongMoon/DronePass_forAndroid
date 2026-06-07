package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.domain.model.MarkdownElement
import com.ScienceFiction.DronePassAndroid.domain.model.MarkdownElementType
import com.ScienceFiction.DronePassAndroid.domain.model.ParsedDocument
import com.ScienceFiction.DronePassAndroid.domain.model.PatchNote
import com.ScienceFiction.DronePassAndroid.domain.model.PatchNoteFeature
import com.ScienceFiction.DronePassAndroid.domain.model.TableData

/**
 * iOS `FetchWebDocuments.parseMarkdown` / `parsePatchNotes` 1:1 Kotlin 포팅.
 *
 * 지원 마크다운:
 *  - `# ~ ######` 헤더 (레벨 1~6)
 *  - `---` 구분선
 *  - `| col | col |` 표 (헤더 + `|---|---|` 구분 + 데이터 행)
 *  - `- 항목` 또는 `• 항목` 리스트
 *  - 일반 텍스트 단락
 *
 * 인라인 `**bold**` / `*italic*` / `[text](url)` 은 렌더링 단계(MarkdownView) 에서 처리.
 */
object MarkdownParser {

    fun parseMarkdown(content: String): ParsedDocument {
        val elements = mutableListOf<MarkdownElement>()
        val tables = mutableListOf<TableData>()

        val lines = content.split("\n", "\r\n", "\r")
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.isEmpty()) {
                i++
                continue
            }

            // 헤더 (# ~ ######)
            if (line.startsWith("#")) {
                val level = line.takeWhile { it == '#' }.length
                val headerText = line.drop(level).trim()
                elements += MarkdownElement(
                    type = MarkdownElementType.Header,
                    content = headerText,
                    level = level,
                )
                i++
                continue
            }

            // 구분선 (---)
            if (line.startsWith("---")) {
                elements += MarkdownElement(
                    type = MarkdownElementType.Separator,
                    content = "",
                )
                i++
                continue
            }

            // 표 (| col | col |) — 다음 줄도 `|` 포함이면 표로 간주
            if (line.contains("|") && i + 1 < lines.size && lines[i + 1].contains("|")) {
                val (tableData, nextIndex) = parseTable(lines, i)
                if (tableData != null) {
                    tables += tableData
                    elements += MarkdownElement(
                        type = MarkdownElementType.Table,
                        content = tableData.id,
                    )
                }
                i = nextIndex
                continue
            }

            // 리스트 (- 또는 •)
            if (line.startsWith("-") || line.startsWith("•")) {
                val listText = line.drop(1).trim()
                elements += MarkdownElement(
                    type = MarkdownElementType.ListItem,
                    content = listText,
                )
                i++
                continue
            }

            // 일반 단락
            elements += MarkdownElement(
                type = MarkdownElementType.Paragraph,
                content = line,
            )
            i++
        }

        return ParsedDocument(elements = elements, tables = tables)
    }

    /**
     * `|` 연속 라인을 표로 묶어 [TableData] 로 반환.
     * `|---|---|` 헤더 구분선은 건너뛴다.
     */
    private fun parseTable(lines: List<String>, startIndex: Int): Pair<TableData?, Int> {
        var i = startIndex
        val tableLines = mutableListOf<String>()

        while (i < lines.size && lines[i].contains("|")) {
            val line = lines[i].trim()
            if (!line.contains("---")) {
                tableLines += line
            }
            i++
        }

        if (tableLines.size < 2) return null to i

        val headers = tableLines[0]
            .split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val rows = mutableListOf<List<String>>()
        for (j in 1 until tableLines.size) {
            val rowData = tableLines[j]
                .split("|")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (rowData.isNotEmpty()) {
                rows += rowData
            }
        }

        return TableData(headers = headers, rows = rows) to i
    }

    /**
     * 패치노트 파일 형식 파서 — iOS `parsePatchNotes` 정합.
     *
     * 파일 형식:
     * ```
     * v1.0.0 (2025-01-15): 초기 출시
     * - 지도 표시
     *     - 드론 비행구역
     *     - 실시간 업데이트
     * - 로그인
     *
     * v0.9.0 (2024-12-01): 베타
     * - 기본 기능
     * ```
     *
     * 버전 블록 사이는 `\nv` 로 split. 첫 줄은 `v{version} ({date}): {title}` 헤더.
     * 비들여쓰기 `- ` 는 새 feature. `    - ` (4칸 들여쓰기) 는 직전 feature 의 description (멀티라인 \n 으로 누적).
     */
    fun parsePatchNotes(content: String): List<PatchNote> {
        val result = mutableListOf<PatchNote>()

        // "\nv" 로 split 후 각 블록의 prefix "v" 복원 (`v1.0.0` 형식 유지)
        val blocks = content.split("\nv")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { block ->
                if (block.startsWith("v")) block else "v$block"
            }

        for (block in blocks) {
            val lines = block.split("\n", "\r\n", "\r")
            val headerLine = lines.firstOrNull() ?: continue

            // 헤더 split: "v1.0.0 (2025-01-15): 제목" → ["v1.0.0 (2025-01-15)", "제목"]
            val headerSplit = headerLine.split(": ", limit = 2)
            if (headerSplit.size < 2) continue

            val header = headerSplit[0]
            val title = headerSplit[1]

            // 버전/날짜 추출: "v1.0.0 (2025-01-15)" → version="v1.0.0", date="2025-01-15"
            val versionDateSplit = header.split("(", ")")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val version = versionDateSplit.getOrNull(0) ?: "N/A"
            val date = versionDateSplit.getOrNull(1) ?: "N/A"

            // Feature 파싱
            val features = mutableListOf<PatchNoteFeature>()
            for (rawLine in lines.drop(1)) {
                val trimmed = rawLine.trim()
                if (trimmed.isEmpty()) continue

                if (rawLine.startsWith("    -")) {
                    // 들여쓰기된 description (4칸 들여쓰기 + "-")
                    if (features.isNotEmpty()) {
                        val descText = trimmed.drop(1).trim()
                        val last = features.removeAt(features.lastIndex)
                        val merged = if (last.description != null) {
                            "${last.description}\n$descText"
                        } else {
                            descText
                        }
                        features += last.copy(description = merged)
                    }
                } else if (trimmed.startsWith("-")) {
                    // 새 feature
                    val featureTitle = trimmed.drop(1).trim()
                    features += PatchNoteFeature(title = featureTitle, description = null)
                }
            }

            result += PatchNote(
                version = version,
                date = date,
                title = title,
                features = features,
            )
        }

        return result
    }
}

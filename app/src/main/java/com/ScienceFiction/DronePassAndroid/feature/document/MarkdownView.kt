package com.ScienceFiction.DronePassAndroid.feature.document

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.domain.model.MarkdownElement
import com.ScienceFiction.DronePassAndroid.domain.model.MarkdownElementType
import com.ScienceFiction.DronePassAndroid.domain.model.TableData

/**
 * iOS `MarkdownView.swift` 1:1 정합 Compose 컴포넌트.
 *
 * 자체 마크다운 파서가 만든 [MarkdownElement] 시퀀스를 렌더링한다.
 * 자체 verticalScroll 없음 — 호스팅 화면이 ScrollView/LazyColumn 으로 감싸야 한다 (iOS LazyVStack 정합).
 *
 * 지원:
 *  - 헤더 1~6 레벨 (레벨 1·2 는 하단 accent line)
 *  - 단락 (라인 간격 4dp)
 *  - 표 (헤더 + 데이터 행 + 0.5dp separator + 8dp 모서리)
 *  - 리스트 (• 불릿)
 *  - 구분선
 *  - 인라인 `**bold**` / `*italic*` / `[text](url)` — link 는 iOS AttributedString 처럼 탭 가능
 */
@Composable
fun MarkdownView(
    elements: List<MarkdownElement>,
    tables: List<TableData>,
    modifier: Modifier = Modifier,
) {
    val tableMap = tables.associateBy { it.id }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        elements.forEach { element ->
            when (element.type) {
                MarkdownElementType.Header -> HeaderView(
                    text = element.content,
                    level = element.level ?: 1,
                )
                MarkdownElementType.Paragraph -> ParagraphView(text = element.content)
                MarkdownElementType.Table -> tableMap[element.content]?.let { TableView(it) }
                MarkdownElementType.Separator -> SeparatorView()
                MarkdownElementType.ListItem -> ListItemView(text = element.content)
            }
        }
    }
}

@Composable
private fun HeaderView(text: String, level: Int) {
    Column(
        modifier = Modifier.padding(vertical = headerPadding(level)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InlineMarkdownText(
            text = text,
            style = when (level) {
                1 -> MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
                2 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                3 -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            },
            color = MaterialTheme.colorScheme.onSurface,
        )
        // 레벨 1·2 만 하단 라인 (iOS HeaderView 정합)
        if (level <= 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (level == 1) 3.dp else 2.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            )
        }
    }
}

private fun headerPadding(level: Int) = when (level) {
    1 -> 12.dp
    2 -> 10.dp
    3 -> 8.dp
    else -> 6.dp
}

@Composable
private fun ParagraphView(text: String) {
    InlineMarkdownText(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ListItemView(text: String) {
    Row(
        modifier = Modifier.padding(start = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp),
        )
        InlineMarkdownText(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 21.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SeparatorView() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun TableView(table: TableData) {
    val border = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, border, RoundedCornerShape(8.dp)),
    ) {
        // 헤더
        TableRow(
            cells = table.headers,
            isHeader = true,
            totalColumns = table.headers.size,
            border = border,
        )
        HorizontalDivider(thickness = 0.5.dp, color = border)
        // 데이터 행
        table.rows.forEachIndexed { index, row ->
            TableRow(
                cells = row,
                isHeader = false,
                totalColumns = table.headers.size,
                border = border,
            )
            if (index < table.rows.lastIndex) {
                HorizontalDivider(thickness = 0.5.dp, color = border)
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    isHeader: Boolean,
    totalColumns: Int,
    border: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        // 실제 셀
        cells.forEachIndexed { index, cell ->
            TableCell(
                text = cell,
                isHeader = isHeader,
                modifier = Modifier.weight(1f),
            )
            // 셀 간 vertical separator (마지막 셀 + 빈 셀 모두 끝나기 전까지)
            val isLastWithFill = index == cells.lastIndex && cells.size >= totalColumns
            if (!isLastWithFill) {
                VerticalDivider(border)
            }
        }
        // iOS 정합: 셀 부족 시 빈 셀 자동 추가
        if (cells.size < totalColumns) {
            repeat(totalColumns - cells.size) { fillIndex ->
                TableCell(
                    text = "",
                    isHeader = isHeader,
                    modifier = Modifier.weight(1f),
                )
                val isVeryLast = fillIndex == (totalColumns - cells.size - 1)
                if (!isVeryLast) {
                    VerticalDivider(border)
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    isHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    InlineMarkdownText(
        text = text,
        style = if (isHeader) {
            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        } else {
            MaterialTheme.typography.bodyMedium
        },
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(
            horizontal = 12.dp,
            vertical = if (isHeader) 10.dp else 8.dp,
        ),
    )
}

@Composable
private fun VerticalDivider(color: Color) {
    Spacer(
        modifier = Modifier
            .width(0.5.dp)
            .fillMaxHeight()
            .background(color),
    )
}

/**
 * 인라인 마크다운 파서 — `**bold**` / `*italic*` / `[text](url)` 3종 지원.
 * iOS `AttributedString(markdown:)` 와 동일한 syntax 만 처리하므로 결과 거의 동일.
 *
 * link 는 URL annotation 을 포함하고 [InlineMarkdownText] 가 탭 위치의 annotation 을 열어준다.
 */
internal const val MarkdownUrlAnnotationTag = "URL"

@Composable
private fun InlineMarkdownText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val annotatedText = parseInlineMarkdown(
        text = text,
        linkColor = MaterialTheme.colorScheme.primary,
    )

    Text(
        text = annotatedText,
        style = style.copy(color = color),
        modifier = modifier.pointerInput(annotatedText) {
            detectTapGestures { position ->
                val offset = textLayoutResult.value?.getOffsetForPosition(position)
                    ?: return@detectTapGestures
                annotatedText
                    .getStringAnnotations(MarkdownUrlAnnotationTag, offset, offset)
                    .firstOrNull()
                    ?.let { openMarkdownUriSafely(uriHandler, it.item) }
            }
        },
        onTextLayout = { textLayoutResult.value = it },
    )
}

internal fun openMarkdownUriSafely(uriHandler: UriHandler, uri: String): Boolean {
    return runCatching {
        uriHandler.openUri(uri)
    }.isSuccess
}

internal fun parseInlineMarkdown(text: String, linkColor: Color): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")

    val boldPattern = Regex("""\*\*(.+?)\*\*""")
    val italicPattern = Regex("""(?<!\*)\*([^*\n]+?)\*(?!\*)""")
    val linkPattern = Regex("""\[([^]]+)]\(([^)]+)\)""")

    return buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            val boldMatch = boldPattern.find(remaining)
            val italicMatch = italicPattern.find(remaining)
            val linkMatch = linkPattern.find(remaining)

            // 가장 먼저 등장하는 매치 선택
            val nearest = listOfNotNull(boldMatch, italicMatch, linkMatch)
                .minByOrNull { it.range.first }

            if (nearest == null) {
                append(remaining)
                break
            }

            // 매치 이전 일반 텍스트
            if (nearest.range.first > 0) {
                append(remaining.substring(0, nearest.range.first))
            }

            when (nearest) {
                boldMatch -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(nearest.groupValues[1])
                    pop()
                }
                italicMatch -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(nearest.groupValues[1])
                    pop()
                }
                linkMatch -> {
                    val linkText = nearest.groupValues[1]
                    val url = nearest.groupValues[2]
                    pushStringAnnotation(tag = MarkdownUrlAnnotationTag, annotation = url)
                    pushStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        ),
                    )
                    append(linkText)
                    pop()
                    pop()
                }
            }

            remaining = remaining.substring(nearest.range.last + 1)
        }
    }
}

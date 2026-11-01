package com.ScienceFiction.DronePassAndroid.core.util

import java.text.Normalizer
import java.util.Locale

/**
 * 드론·태그 이름 정렬 비교. iOS 와 같은 순서가 나오도록 기기 로케일(Collator)을 쓰지 않는다.
 * 이름을 NFC → 소문자(Locale.ROOT) → NFC 로 맞춘 뒤, 숫자 덩어리와 글자 하나하나로 나눠 앞에서부터 비교한다.
 * - 분류 순서: 숫자 덩어리(ASCII 0-9) → 한글 → 라틴 문자 → 그 밖(기호·문장부호·공백·이모지·다른 문자)
 * - 같은 분류 안에서 숫자 덩어리는 수로(앞의 0 무시), 글자는 유니코드 코드포인트로 비교한다.
 * - 앞부분이 모두 같으면 짧은 쪽이 앞이다. 이름이 같으면 0 이며, 호출부가 id 로 정한다([nameThenIdComparator]).
 * 정답은 iOS fixture `name-sort-cases.json` 의 `sortCases` 다.
 */
internal fun compareDisplayNames(first: String, second: String): Int {
    val firstUnits = sortUnits(first)
    val secondUnits = sortUnits(second)
    val count = minOf(firstUnits.size, secondUnits.size)

    for (index in 0 until count) {
        val comparison = compareSortUnits(firstUnits[index], secondUnits[index])
        if (comparison != 0) return comparison
    }

    return firstUnits.size.compareTo(secondUnits.size)
}

/** 이름 순, 같으면 id 를 코드포인트 순으로 */
internal fun <T> nameThenIdComparator(name: (T) -> String, id: (T) -> String): Comparator<T> =
    Comparator { first, second ->
        val order = compareDisplayNames(name(first), name(second))
        if (order != 0) order else compareCodePoints(id(first), id(second))
    }

private fun compareCodePoints(first: String, second: String): Int {
    var i = 0
    var j = 0
    while (i < first.length && j < second.length) {
        val a = first.codePointAt(i)
        val b = second.codePointAt(j)
        if (a != b) return a.compareTo(b)
        i += Character.charCount(a)
        j += Character.charCount(b)
    }
    return (first.length - i).compareTo(second.length - j)
}

private sealed interface SortUnit {
    /** 앞의 0 을 뗀 숫자 덩어리 */
    data class Number(val digits: String) : SortUnit

    data class Letter(val codePoint: Int) : SortUnit
}

private const val RANK_NUMBER = 0
private const val RANK_HANGUL = 1
private const val RANK_LATIN = 2
private const val RANK_OTHER = 3

private fun SortUnit.rank(): Int = when (this) {
    is SortUnit.Number -> RANK_NUMBER
    is SortUnit.Letter -> when {
        isHangul(codePoint) -> RANK_HANGUL
        isLatin(codePoint) -> RANK_LATIN
        else -> RANK_OTHER
    }
}

private fun isHangul(codePoint: Int): Boolean =
    codePoint in 0xAC00..0xD7A3 || // 완성형
        codePoint in 0x1100..0x11FF || // 자모
        codePoint in 0x3130..0x318F || // 호환 자모
        codePoint in 0xA960..0xA97F || // 자모 확장 A
        codePoint in 0xD7B0..0xD7FF // 자모 확장 B

/** iOS 와 같은 명시 범위(소문자로 바꾼 뒤 기준) */
private fun isLatin(codePoint: Int): Boolean =
    codePoint in 0x0061..0x007A ||
        codePoint in 0x00C0..0x00D6 ||
        codePoint in 0x00D8..0x00F6 ||
        codePoint in 0x00F8..0x024F ||
        codePoint in 0x0250..0x02AF ||
        codePoint in 0x1E00..0x1EFF ||
        codePoint in 0x2C60..0x2C7F ||
        codePoint in 0xA720..0xA7FF ||
        codePoint in 0xAB30..0xAB6F ||
        codePoint in 0xFF41..0xFF5A

private fun compareSortUnits(first: SortUnit, second: SortUnit): Int {
    val rankOrder = first.rank().compareTo(second.rank())
    if (rankOrder != 0) return rankOrder
    return when {
        first is SortUnit.Number && second is SortUnit.Number ->
            first.digits.length.compareTo(second.digits.length).takeIf { it != 0 }
                ?: first.digits.compareTo(second.digits)
        first is SortUnit.Letter && second is SortUnit.Letter -> first.codePoint.compareTo(second.codePoint)
        else -> 0
    }
}

private fun isAsciiDigit(codePoint: Int): Boolean = codePoint in '0'.code..'9'.code

/** NFC → 소문자 → NFC 로 맞추고 숫자 덩어리와 코드포인트 단위로 나눈다 */
private fun sortUnits(value: String): List<SortUnit> {
    val lowered = Normalizer.normalize(value, Normalizer.Form.NFC).lowercase(Locale.ROOT)
    val text = Normalizer.normalize(lowered, Normalizer.Form.NFC)
    val units = mutableListOf<SortUnit>()
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        if (isAsciiDigit(codePoint)) {
            var end = index
            while (end < text.length && isAsciiDigit(text[end].code)) end++
            units += SortUnit.Number(text.substring(index, end).trimStart('0').ifEmpty { "0" })
            index = end
        } else {
            units += SortUnit.Letter(codePoint)
            index += Character.charCount(codePoint)
        }
    }
    return units
}

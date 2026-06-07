package com.ScienceFiction.DronePassAndroid.core.util

import java.text.Collator
import java.util.Locale

internal fun compareIosLocalizedStandardStrings(
    first: String,
    second: String,
    locale: Locale = Locale.getDefault(),
): Int {
    val collator = Collator.getInstance(locale)
    val firstTokens = tokenizeNaturalString(first)
    val secondTokens = tokenizeNaturalString(second)
    val count = minOf(firstTokens.size, secondTokens.size)

    for (index in 0 until count) {
        val firstToken = firstTokens[index]
        val secondToken = secondTokens[index]
        val comparison = if (firstToken.isDigit && secondToken.isDigit) {
            compareNaturalNumberTokens(firstToken.text, secondToken.text)
        } else {
            collator.compare(firstToken.text, secondToken.text)
        }
        if (comparison != 0) return comparison
    }

    return firstTokens.size.compareTo(secondTokens.size)
}

private data class NaturalStringToken(
    val text: String,
    val isDigit: Boolean,
)

private fun tokenizeNaturalString(value: String): List<NaturalStringToken> {
    if (value.isEmpty()) return listOf(NaturalStringToken("", isDigit = false))

    val tokens = mutableListOf<NaturalStringToken>()
    var start = 0
    var currentIsDigit = value[0].isDigit()

    for (index in 1 until value.length) {
        val isDigit = value[index].isDigit()
        if (isDigit != currentIsDigit) {
            tokens += NaturalStringToken(value.substring(start, index), currentIsDigit)
            start = index
            currentIsDigit = isDigit
        }
    }

    tokens += NaturalStringToken(value.substring(start), currentIsDigit)
    return tokens
}

private fun compareNaturalNumberTokens(first: String, second: String): Int {
    val normalizedFirst = first.trimStart('0').ifEmpty { "0" }
    val normalizedSecond = second.trimStart('0').ifEmpty { "0" }

    normalizedFirst.length.compareTo(normalizedSecond.length)
        .takeIf { it != 0 }
        ?.let { return it }

    normalizedFirst.compareTo(normalizedSecond)
        .takeIf { it != 0 }
        ?.let { return it }

    return first.length.compareTo(second.length)
}

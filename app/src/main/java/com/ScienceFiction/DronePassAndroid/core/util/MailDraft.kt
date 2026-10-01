package com.ScienceFiction.DronePassAndroid.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/** 메일 앱으로 보낼 초안. 받는 사람·제목·본문을 미리 채운다(RFC 6068, 공백은 %20). */
internal fun mailtoUri(to: String, subject: String, body: String = ""): String =
    buildString {
        append("mailto:").append(to)
        append("?subject=").append(encodeMailtoComponent(subject))
        if (body.isNotEmpty()) append("&body=").append(encodeMailtoComponent(body))
    }

private fun encodeMailtoComponent(value: String): String =
    URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

/** 메일 앱을 연다. 열 수 있는 앱이 없으면 false. */
internal fun openMailDraft(context: Context, to: String, subject: String, body: String = ""): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(mailtoUri(to, subject, body)))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { context.startActivity(intent) }.isSuccess
}

package com.ScienceFiction.DronePassAndroid.core.util

import androidx.compose.ui.platform.UriHandler

internal fun openUriSafely(uriHandler: UriHandler, uri: String): Boolean {
    return runCatching {
        uriHandler.openUri(uri)
    }.isSuccess
}

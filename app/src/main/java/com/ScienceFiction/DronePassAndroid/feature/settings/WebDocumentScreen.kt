package com.ScienceFiction.DronePassAndroid.feature.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ScienceFiction.DronePassAndroid.R

/**
 * 웹 문서를 표시하는 재사용 가능한 화면
 *
 * @param title 상단바에 표시할 제목
 * @param url 로드할 웹 URL
 * @param onBack 뒤로가기 콜백
 */
/**
 * WebView 로드를 허용할 도메인 화이트리스트.
 * 약관/개인정보처리방침은 Notion 호스팅을 사용한다.
 *
 * 화이트리스트 외 도메인으로의 이동은 시스템 브라우저로 위임하여 WebView 안에서
 * JS 가 활성화된 채 임의 페이지가 로드되는 것을 방지한다.
 */
private val ALLOWED_DOMAINS = setOf(
    "dronepass.notion.site",
    "www.notion.so",
    "notion.so"
)

private fun isAllowedUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val host = runCatching { Uri.parse(url).host }.getOrNull() ?: return false
    return ALLOWED_DOMAINS.any { host == it || host.endsWith(".$it") }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebDocumentScreen(
    title: String,
    url: String,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back)
                    )
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (hasError) {
                // 에러 상태
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.web_document_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            hasError = false
                            isLoading = true
                            webView?.loadUrl(url)
                        }
                    ) {
                        Text(stringResource(R.string.common_retry))
                    }
                }
            } else {
                // WebView
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true

                            this.webViewClient = object : WebViewClient() {
                                /**
                                 * 도메인 화이트리스트 검증.
                                 * 화이트리스트 외 URL 은 시스템 브라우저로 위임하여
                                 * WebView 가 임의 도메인을 JavaScript 활성 상태로 로드하지 않도록 한다.
                                 */
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val nextUrl = request?.url?.toString()
                                    return if (isAllowedUrl(nextUrl)) {
                                        // 화이트리스트 도메인은 WebView 안에서 계속 로드
                                        false
                                    } else {
                                        // 외부 도메인은 시스템 브라우저로 위임
                                        nextUrl?.let {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                            view?.context?.startActivity(intent)
                                        }
                                        true
                                    }
                                }

                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: Bitmap?
                                ) {
                                    isLoading = true
                                    hasError = false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    // 메인 프레임 로드 실패 시에만 에러 표시
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        hasError = true
                                    }
                                }
                            }

                            webView = this
                            loadUrl(url)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 로딩 인디케이터
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}

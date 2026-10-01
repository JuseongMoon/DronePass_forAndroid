package com.ScienceFiction.DronePassAndroid.app

import android.content.Context
import androidx.core.content.edit
import com.ScienceFiction.DronePassAndroid.core.legal.TERMS_NOTICE_VERSION
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 첫 실행 순서: 약관 개정 안내 → 위치정보 이용 동의 → 위치 권한 → 알림 권한.
 *
 * 지도 화면은 [flowCompleted] 가 된 뒤에만 위치 권한을 묻고, 알림 권한은 다시 위치 요청이 끝나기를
 * 기다린다([LaunchPermissionSequence]). 그래서 동의 화면 위로 OS 권한 대화상자가 겹치지 않는다.
 */
internal object LegalLaunchSequence {
    private val _flowCompleted = MutableStateFlow(false)
    val flowCompleted: StateFlow<Boolean> = _flowCompleted.asStateFlow()

    fun markCompleted() {
        _flowCompleted.value = true
    }
}

/** 위치정보 이용 동의 화면 표시 요청. 첫 실행 순서, 설정 토글, 지도의 "현재 위치" 안내가 함께 쓴다. */
internal object LocationConsentPrompt {
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    fun show() {
        _visible.value = true
    }

    fun hide() {
        _visible.value = false
    }
}

private const val LegalPrefsName = "dronepass_legal"
private const val KeyTermsNoticeVersion = "terms_notice_version"

/**
 * 약관 개정 안내(사양 §6)는 업데이트한 기존 설치에만 한 번 띄운다.
 *
 * 기존 설치 판정은 이전 버전이 실행할 때마다 쓰던 앱 언어 키로 한다. [hadPreviousLaunch] 는 이번 실행이
 * 그 키를 쓰기 전에 읽은 값이어야 한다(Application.onCreate 맨 앞). 새로 설치한 기기는 지금 약관에 이미
 * 동의하고 시작하므로 안내를 본 것으로 기록한다.
 */
internal fun initializeTermsNotice(context: Context, hadPreviousLaunch: Boolean) {
    val preferences = context.getSharedPreferences(LegalPrefsName, Context.MODE_PRIVATE)
    if (!hadPreviousLaunch && !preferences.contains(KeyTermsNoticeVersion)) {
        preferences.edit(commit = true) { putString(KeyTermsNoticeVersion, TERMS_NOTICE_VERSION) }
    }
}

internal fun shouldShowTermsNotice(context: Context): Boolean =
    termsNoticeNeeded(
        context.getSharedPreferences(LegalPrefsName, Context.MODE_PRIVATE).getString(KeyTermsNoticeVersion, null),
        TERMS_NOTICE_VERSION,
    )

internal fun markTermsNoticeShown(context: Context) {
    context.getSharedPreferences(LegalPrefsName, Context.MODE_PRIVATE)
        .edit { putString(KeyTermsNoticeVersion, TERMS_NOTICE_VERSION) }
}

internal fun termsNoticeNeeded(shownVersion: String?, currentVersion: String): Boolean =
    shownVersion == null || shownVersion < currentVersion

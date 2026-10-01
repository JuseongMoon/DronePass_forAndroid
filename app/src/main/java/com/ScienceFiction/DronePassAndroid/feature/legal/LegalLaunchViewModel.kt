package com.ScienceFiction.DronePassAndroid.feature.legal

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.app.LegalLaunchSequence
import com.ScienceFiction.DronePassAndroid.app.LocationConsentPrompt
import com.ScienceFiction.DronePassAndroid.app.markTermsNoticeShown
import com.ScienceFiction.DronePassAndroid.app.shouldShowTermsNotice
import com.ScienceFiction.DronePassAndroid.core.location.LocationConsentRepository
import com.ScienceFiction.DronePassAndroid.core.location.LocationPurpose
import com.ScienceFiction.DronePassAndroid.core.location.LocationUsageUploader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 첫 실행 법무 순서(약관 개정 안내 → 위치정보 이용 동의)와 동의 화면의 처리.
 * 동의 화면은 첫 실행, 설정 토글, 지도의 "현재 위치" 안내가 모두 이 ViewModel 로 처리한다.
 */
@HiltViewModel
class LegalLaunchViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val consentRepository: LocationConsentRepository,
    private val usageUploader: LocationUsageUploader,
) : ViewModel() {

    private val _showTermsNotice = MutableStateFlow(false)
    val showTermsNotice: StateFlow<Boolean> = _showTermsNotice.asStateFlow()

    val showLocationConsent: StateFlow<Boolean> = LocationConsentPrompt.visible

    init {
        viewModelScope.launch {
            // 앱을 실행할 때 v1 키(동의 이력)와 보존 기간(190일)이 지난 기록을 지우고, 서버 삭제 대기열과
            // 지난 날짜의 확인자료를 서버와 맞춘다(업로드는 원격 플래그가 켜졌을 때만).
            runCatching { consentRepository.runLaunchMaintenance() }
            usageUploader.syncAsync()
            if (LegalLaunchSequence.flowCompleted.value) return@launch
            if (shouldShowTermsNotice(appContext)) {
                _showTermsNotice.value = true
            } else {
                continueToLocationConsent()
            }
        }
    }

    fun confirmTermsNotice() {
        markTermsNoticeShown(appContext)
        _showTermsNotice.value = false
        viewModelScope.launch { continueToLocationConsent() }
    }

    /** 고른 목적에 동의. 14세 확인과 목적 하나 이상이 있어야 한다(화면이 버튼을 막는다). */
    fun agreeLocationConsent(purposes: Set<LocationPurpose>, ageConfirmed: Boolean) {
        if (!ageConfirmed || purposes.isEmpty()) return
        viewModelScope.launch {
            consentRepository.submitConsent(purposes, ageConfirmed = true)
            finishLocationConsent()
        }
    }

    fun declineLocationConsent() {
        viewModelScope.launch {
            consentRepository.submitConsent(emptySet(), ageConfirmed = false)
            finishLocationConsent()
        }
    }

    private suspend fun continueToLocationConsent() {
        if (consentRepository.needsPromptNow()) {
            LocationConsentPrompt.show()
        } else {
            LegalLaunchSequence.markCompleted()
        }
    }

    private fun finishLocationConsent() {
        LocationConsentPrompt.hide()
        // 첫 실행이면 여기서 순서가 끝나 지도 화면이 위치 권한(동의한 경우)과 알림 권한을 차례로 묻는다.
        LegalLaunchSequence.markCompleted()
    }
}

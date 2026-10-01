package com.ScienceFiction.DronePassAndroid.feature.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.app.LocationConsentPrompt
import com.ScienceFiction.DronePassAndroid.core.location.LocationConsentRepository
import com.ScienceFiction.DronePassAndroid.core.location.LocationPurpose
import com.ScienceFiction.DronePassAndroid.core.location.LocationUsageRecord
import com.ScienceFiction.DronePassAndroid.core.location.LocationUsageUploader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 설정의 "위치정보" 섹션: 목적별 동의 토글·철회와 위치정보 이용 기록. */
@HiltViewModel
class LocationConsentSettingsViewModel @Inject constructor(
    private val consentRepository: LocationConsentRepository,
    private val usageUploader: LocationUsageUploader,
) : ViewModel() {

    val currentLocationAllowed: StateFlow<Boolean> = consentRepository.allowed(LocationPurpose.CURRENT_LOCATION)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val weatherAndSunAllowed: StateFlow<Boolean> = consentRepository.allowed(LocationPurpose.WEATHER_AND_SUN)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val usageRecords: StateFlow<List<LocationUsageRecord>> = consentRepository.usageRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installId = MutableStateFlow<String?>(null)
    val installId: StateFlow<String?> = _installId.asStateFlow()

    /** 토글 켜기: 동의 화면을 띄운다. 동의는 그 화면에서만 저장한다. */
    fun requestConsent() {
        LocationConsentPrompt.show()
    }

    /** 토글 끄기(확인 후): 그 목적만 철회하고 그 목적의 기록(기기·서버)과 캐시 위치를 지운다. */
    fun withdraw(purpose: LocationPurpose) {
        viewModelScope.launch {
            consentRepository.withdraw(purpose)
            usageUploader.syncAsync()
        }
    }

    /** 이용 기록 화면을 열 때 설치 ID 를 준비한다. */
    fun loadInstallId() {
        viewModelScope.launch { _installId.value = consentRepository.installId() }
    }
}

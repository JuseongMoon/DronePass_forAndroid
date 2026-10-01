package com.ScienceFiction.DronePassAndroid.feature.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.app.LocationConsentPrompt
import com.ScienceFiction.DronePassAndroid.core.location.LocationConsentRepository
import com.ScienceFiction.DronePassAndroid.core.location.LocationUsageRecord
import com.ScienceFiction.DronePassAndroid.core.location.MapCenterStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 설정의 "위치정보" 섹션: 동의 토글·철회와 위치정보 이용 기록. */
@HiltViewModel
class LocationConsentSettingsViewModel @Inject constructor(
    private val consentRepository: LocationConsentRepository,
    private val mapCenterStore: MapCenterStore,
) : ViewModel() {

    val consentAllowed: StateFlow<Boolean> = consentRepository.allowed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val usageRecords: StateFlow<List<LocationUsageRecord>> = consentRepository.usageRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 토글 켜기: 동의 화면을 띄운다. 동의는 그 화면에서만 저장한다. */
    fun requestConsent() {
        LocationConsentPrompt.show()
    }

    /** 토글 끄기(확인 후): 즉시 위치 이용을 멈추고 이용 기록과 이 기기의 마지막 위치를 지운다. */
    fun withdraw() {
        viewModelScope.launch {
            consentRepository.withdraw()
            mapCenterStore.clearLiveCenter()
        }
    }
}

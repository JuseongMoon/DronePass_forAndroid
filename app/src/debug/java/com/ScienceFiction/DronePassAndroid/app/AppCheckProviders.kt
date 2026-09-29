package com.ScienceFiction.DronePassAndroid.app

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * 디버그 빌드 전용 App Check 공급자.
 * 첫 실행 시 Logcat(DebugAppCheckProvider)에 디버그 토큰이 출력되며,
 * 이 토큰을 Firebase 콘솔 App Check 디버그 토큰에 등록해야 중계 함수 호출이 허용된다.
 */
internal fun appCheckProviderFactory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()

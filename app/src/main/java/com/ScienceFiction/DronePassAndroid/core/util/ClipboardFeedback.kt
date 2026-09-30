package com.ScienceFiction.DronePassAndroid.core.util

import android.os.Build

/**
 * Android 13(API 33)부터는 클립보드에 복사하면 시스템이 직접 확인 UI(토스트/미리보기)를 띄운다.
 * 앱 토스트까지 띄우면 두 개가 겹쳐 보이므로, 그 이전 버전에서만 앱 복사 토스트를 보여 준다.
 */
internal fun shouldShowAppCopyConfirmation(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
    sdkInt < Build.VERSION_CODES.TIRAMISU

package com.ScienceFiction.DronePassAndroid.app

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** 릴리스 빌드의 App Check 공급자. Play Integrity 로 앱 무결성을 증명한다. */
internal fun appCheckProviderFactory(): AppCheckProviderFactory = PlayIntegrityAppCheckProviderFactory.getInstance()

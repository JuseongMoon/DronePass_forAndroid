package com.ScienceFiction.DronePassAndroid.core.di

/**
 * Repository 모듈.
 *
 * 모든 Repository / RealtimeSyncManager 는 `@Singleton @Inject constructor` 로
 * Hilt 가 자동 그래프를 구성하므로 별도의 `@Provides` 가 필요하지 않다.
 * (VWorldApiKey 만 `@Named` 로 constructor 에 직접 주입.)
 *
 * 이 파일은 의도된 빈 모듈이며, 추후 인터페이스 기반 추상화를 위해
 * `@Binds` 패턴이 필요해지면 여기에 추가한다.
 */

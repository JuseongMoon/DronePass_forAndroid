# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
DronePass Android는 iOS DronePass의 Android 포팅 프로젝트입니다. 드론 비행 허가지 시각화를 위한 애플리케이션으로, 네이버 Maps SDK를 활용하여 지도 기반 UI를 제공합니다.

**현재 구현 단계**: iOS 동작/UX 패리티와 Android 출시 하드닝 단계입니다. 네이버 지도, 도형 관리, 저장 목록, 드론 관리, VWorld 비행구역, 날씨/KP 정보, 스케치, Firebase Auth/Firestore/FCM, 설정/프로필/문서 화면이 구현되어 있으며, 남은 핵심 작업은 Play 설치 빌드에서 실기기/실계정 검증과 발견된 패리티 차이 보정입니다.

## Build Configuration
- **Package Name**: `com.ScienceFiction.DronePassAndroid`
- **Min SDK**: 28 (Android 9.0 Pie)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Java Version**: 11
- **Kotlin Version**: 2.0.21
- **AGP Version**: 8.13.2

## Development Commands

### Build
```bash
./gradlew :app:assembleDebug
```

### Run Tests
```bash
# Unit tests
./gradlew :app:testDebugUnitTest

# Instrumented tests (requires connected device or emulator)
./gradlew :app:connectedDebugAndroidTest

# Specific test class
./gradlew :app:testDebugUnitTest --tests "*ShapeTypeTest"
```

### Clean Build
```bash
./gradlew clean
```

### Install Debug APK
```bash
./gradlew installDebug
```

### Lint
```bash
./gradlew :app:lintDebug
```

## Architecture

### Package Structure
- `app/` - Application 초기화
- `core/` - 공통 데이터, DI, UI, 유틸
- `domain/model/` - 도메인 모델과 Firebase wire-format 검증
- `feature/` - 기능별 Compose 화면과 ViewModel
  - `auth`, `document`, `drone`, `kp`, `map`, `profile`, `saved`, `settings`, `shape`, `sketch`, `vworld`, `weather`
- `service/` - FCM, 알림, 부팅 리시버
- `ui/navigation`, `ui/theme` - 탭/화면 네비게이션과 테마

### UI Framework
- **Jetpack Compose** 기반 선언적 UI
- **Material 3** 디자인 시스템
- **AndroidView**를 사용하여 네이버 MapView를 Compose에 통합
- Edge-to-edge 디스플레이 지원

### Map Integration
- **네이버 Maps SDK 3.23.1** 사용
- Compose의 `AndroidView`로 네이버 `MapView` 래핑
- 지도 생명주기 관리 (`onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`)
- 위치 추적 및 카메라 제어

### Location & Permissions
- **Google Play Services Location 21.3.0** - 위치 서비스
- **Accompanist Permissions 0.36.0** - 런타임 권한 처리
- 위치 권한: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
- 권한 요청 다이얼로그 및 설명 UI 포함

### Dependency Management
프로젝트는 **Version Catalog** (libs.versions.toml)와 직접 의존성을 혼합하여 사용합니다.
- 표준 AndroidX 라이브러리: Version Catalog로 관리
- 네이버 Maps, Play Services, Accompanist: 직접 버전 명시
- 네이버 Maps SDK Maven Repository: `https://repository.map.naver.com/archive/maven`

### Key Dependencies
- AndroidX Core KTX
- Lifecycle Runtime KTX
- Jetpack Compose (UI, Material3, Tooling)
- Activity Compose
- **네이버 Maps SDK 3.23.1**
- **Google Play Services Location 21.3.0**
- **Accompanist Permissions 0.36.0**
- JUnit (Unit Testing)
- Espresso (UI Testing)

## Important Configuration

### Naver Maps NCP Key
네이버 지도 SDK key id는 `local.properties`의 `NAVER_MAP_KEY_ID`를 통해
`app/src/main/AndroidManifest.xml`의 manifest placeholder로 주입합니다. 실제 키는
커밋하지 않습니다.

```xml
<meta-data
    android:name="com.naver.maps.map.NCP_KEY_ID"
    android:value="${NAVER_MAP_KEY_ID}" />
```

Geocoding/Reverse Geocoding REST API는 `NAVER_MAP_KEY_ID`와
`NAVER_MAP_KEY_SECRET`을 `BuildConfig`로 주입해 `X-NCP-APIGW-API-KEY-ID` /
`X-NCP-APIGW-API-KEY` 헤더에 사용합니다.

⚠️ **주의**: NCP Maps Console에서 Android 앱(`com.ScienceFiction.DronePassAndroid`)으로
등록하고 현재 설치 APK 서명 인증서의 SHA-1을 등록해야 합니다.

### Permissions
앱이 요구하는 권한:
- `INTERNET` - 지도 타일 다운로드
- `ACCESS_FINE_LOCATION` - 정확한 위치 정보
- `ACCESS_COARSE_LOCATION` - 대략적인 위치 정보

## Development Guidelines

### Code Style
- **Kotlin** 언어 사용
- **Jetpack Compose** 기반 UI (XML 레이아웃 사용하지 않음)
- Material 3 컴포넌트 우선 사용
- 함수명: camelCase
- 파일명: PascalCase

### Adding New Features
1. 패키지 구조 유지: `com.ScienceFiction.DronePassAndroid` 하위에 생성
2. Compose Composable 함수로 UI 구현
3. 상태 관리: `remember`, `mutableStateOf`, `LaunchedEffect` 활용
4. 생명주기 관리: `DisposableEffect` 사용

### Map Related Work
- 지도 관련 작업 시 `feature/map/MapScreen.kt`, `feature/map/MapViewModel.kt`, `feature/map/overlay/ShapeOverlayManager.kt` 참조
- 네이버 Maps SDK API: [공식 문서](https://navermaps.github.io/android-map-sdk/guide-ko/)
- 지도 생명주기는 반드시 관리 필요

## Current Resume Notes

- 앱 출시 과정을 이어갈 때는 먼저 `PLAY_RELEASE_HANDOFF.md`를 확인합니다.
- 2026-07-06 기준 Play 내부 테스트 `3.5.5 (102) internal-1`은 이미 게시되어 있습니다.
- 같은 versionCode `102`를 재업로드하지 말고, Play 앱 서명 SHA-1/SHA-256을 Firebase Android 앱과 NCP Maps에 등록한 뒤 내부 테스터 opt-in 링크로 Play 설치 검증을 진행합니다.
- iOS/Android 공유 Firestore wire-format은 `FIRESTORE_CONTRACT.md`가 기준입니다. 특히 `shapeType`은 쓰기 소문자 raw value, 읽기 대소문자 무시, unknown 값 스킵 계약을 유지해야 합니다.
- 코드 작업 재개 시 오래된 TODO 목록보다 `NEXT_STEPS.md`의 최신 체크포인트와 현재 테스트를 우선합니다.

## Reference
- 원본 iOS 프로젝트: `/Users/david/Development/Swift/myProjects/DronePass`
- iOS 아키텍처: MVVM, Manager 패턴, Repository 패턴

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
DronePass Android는 iOS DronePass의 Android 포팅 프로젝트입니다. 드론 비행 허가지 시각화를 위한 애플리케이션으로, 네이버 Maps SDK를 활용하여 지도 기반 UI를 제공합니다.

**구현된 기능**: 네이버 지도, 도형 관리, 저장 목록, 드론 관리, VWorld 비행구역, 날씨/KP 정보, 스케치, Firebase Auth/Firestore/FCM, 설정/프로필/문서 화면. 동작·UX는 iOS 앱과의 패리티를 기준으로 맞춥니다.

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
모든 의존성 버전은 **Version Catalog** (`gradle/libs.versions.toml`)에서 관리합니다.
네이버 Maps SDK Maven Repository(`https://repository.map.naver.com/archive/maven`)는 `settings.gradle.kts`에 선언되어 있습니다.

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

### Firebase (`app/google-services.json`)

`app/google-services.json`은 **커밋합니다.** Firebase Android 클라이언트 설정 파일이며
`app/build.gradle.kts`가 빌드 시점에 `package_name`과 `oauth_client`(client_type=1) 존재를
검증하므로, 파일이 없으면 빌드 진단이 실패합니다. 이 파일에는 비밀값이 들어 있지 않습니다.

반면 다음은 **절대 커밋하지 않습니다.**

- Firebase/GCP **서비스 계정 키**(`*-firebase-adminsdk-*.json` 등 private key가 포함된 JSON)
- 서명 키와 그 비밀번호: `release.jks`, `*.jks`, `*.keystore`, `keystore.properties`
- `local.properties` (`NAVER_MAP_KEY_ID`, `NAVER_MAP_KEY_SECRET`, `VWORLD_API_KEY`,
  `WEB_CLIENT_ID` 주입처. 크로스 플랫폼 계약 테스트용 `IOS_PROJECT_DIR`도 여기 둡니다.)

저장소에는 `keystore.properties.example` 같은 `*.example`만 둡니다.

### Permissions

`app/src/main/AndroidManifest.xml`이 직접 선언하는 권한:

| 권한 | 용도 |
| --- | --- |
| `INTERNET` | 지도 타일·Firebase 통신 |
| `ACCESS_FINE_LOCATION` | 정확한 위치 정보 |
| `ACCESS_COARSE_LOCATION` | 대략적인 위치 정보 |
| `POST_NOTIFICATIONS` | 알림 (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | 정확한 알람 예약 (Android 12+) |
| `RECEIVE_BOOT_COMPLETED` | 재부팅 후 알림 재예약 |

같은 파일에 **제거 지시**(`tools:node="remove"`) 3건이 있습니다. 선언이 아니라 **라이브러리가 주입하는 것을
막는 것**이므로 지우지 마십시오.

```
com.google.android.gms.permission.AD_ID
android.permission.ACCESS_ADSERVICES_AD_ID
android.permission.ACCESS_ADSERVICES_ATTRIBUTION
```

`firebase-analytics`가 버전에 따라 주입하는 광고·Privacy Sandbox 권한입니다. 이 앱은 광고 ID를 쓰지 않으므로
Play Data safety 신고와 맞추기 위해 막아 둡니다. 제거해도 Analytics 수집은 정상 동작합니다
(TokenWatchAndroid에서 이벤트 업로드 204 확인).

**주입 여부는 BOM 버전에 따라 갈립니다** — BOM 33.1.0은 주입하지 않고, 33.7.0은 3종 모두 주입합니다
(TokenWatchAndroid 실측). 제거 지시는 버전과 무관하게 막아 주므로 평소에는 신경 쓸 일이 없지만,
**의존성을 올릴 때는 머지 매니페스트를 한 번 확인합니다.** 증상이 전혀 없어서 어긋난 채로 출시되기 쉽습니다.

```bash
./gradlew :app:processReleaseMainManifest
grep -iE 'AD_ID|ADSERVICES' \
  app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml
```

`<property>`의 `android.adservices.AD_SERVICES_CONFIG`와 `<uses-library>`의 `android.ext.adservices`는
권한이 아니므로 걸려도 무방합니다. **`<uses-permission>`에 들어왔는지만 봅니다.**

APK에 실리는 권한은 위 목록에 라이브러리 주입분이 더해진 결과입니다. 2026-09-08 릴리스 머지 매니페스트
기준으로 `ACCESS_NETWORK_STATE` · `ACCESS_WIFI_STATE` · `WAKE_LOCK` ·
`com.google.android.c2dm.permission.RECEIVE` ·
`com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE` ·
`com.google.android.providers.gsf.permission.READ_GSERVICES`가 추가되며, 광고 권한은 0건이었습니다.
의존성이 바뀌면 달라질 수 있으니 확정이 필요할 때는 위 명령으로 직접 확인합니다.

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
3. 상태 관리: 화면 상태는 `@HiltViewModel` ViewModel의 `StateFlow`로 노출하고, `remember`/`mutableStateOf`는 Composable 내부의 일시적 UI 상태에만 사용
4. 생명주기 관리: `DisposableEffect` 사용

### Map Related Work
- 지도 관련 작업 시 `feature/map/MapScreen.kt`, `feature/map/MapViewModel.kt`, `feature/map/overlay/ShapeOverlayManager.kt` 참조
- 네이버 Maps SDK API: [공식 문서](https://navermaps.github.io/android-map-sdk/guide-ko/)
- 지도 생명주기는 반드시 관리 필요

## Cross-Platform Contract

- iOS/Android 공유 Firestore wire-format의 정본은 iOS 앱이 쓰는 형태이며, 이 저장소의 요약은 [`docs/agents/FIRESTORE_CONTRACT.md`](docs/agents/FIRESTORE_CONTRACT.md)입니다. 요약과 iOS 동작이 다르면 iOS 쪽이 맞습니다.
  특히 `shapeType`은 쓰기 소문자 raw value, 읽기 대소문자 무시, unknown 값 스킵 계약을 유지해야 합니다.
- `CrossPlatformFirestoreContractTest`는 `IOS_PROJECT_DIR`의 iOS 공유 fixture로 이 계약을 검증합니다. 경로가 없으면 실패하지 않고 skip되므로, 계약 검증이 필요할 때는 skip 없이 실행됐는지 확인합니다.

## Reference
- 원본 iOS 프로젝트: https://github.com/JuseongMoon/DronePass
- iOS 아키텍처: MVVM, Manager 패턴, Repository 패턴

## 공통 작업 규칙

공개 저장소 규칙과 안전 경계는 Codex 등 다른 에이전트도 함께 읽도록 `AGENTS.md`에 둔다.

@AGENTS.md

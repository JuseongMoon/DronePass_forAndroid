# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
DronePass Android는 iOS DronePass의 Android 포팅 프로젝트입니다. 드론 비행 허가지 시각화를 위한 애플리케이션으로, 네이버 Maps SDK를 활용하여 지도 기반 UI를 제공합니다.

**현재 구현 단계**: 1단계 - 네이버 지도 표시 및 위치 권한 처리

## Build Configuration
- **Package Name**: `com.ScienceFiction.DronePassAndroid`
- **Min SDK**: 28 (Android 9.0 Pie)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Java Version**: 11
- **Kotlin Version**: 2.0.21
- **AGP Version**: 8.13.0

## Development Commands

### Build
```bash
./gradlew build
```

### Run Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Specific test class
./gradlew test --tests com.ScienceFiction.DronePassAndroid.ExampleUnitTest
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
./gradlew lint
```

## Architecture

### Package Structure
- `com.ScienceFiction.DronePassAndroid` - 메인 패키지
  - `MainActivity.kt` - 앱의 진입점, 지도 화면 호출
  - `MapScreen.kt` - 네이버 지도 화면 Composable
  - `ui.theme/` - Material 3 테마 (Color, Type, Theme)

### UI Framework
- **Jetpack Compose** 기반 선언적 UI
- **Material 3** 디자인 시스템
- **AndroidView**를 사용하여 네이버 MapView를 Compose에 통합
- Edge-to-edge 디스플레이 지원

### Map Integration
- **네이버 Maps SDK 3.19.1** 사용
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
- **네이버 Maps SDK 3.19.1**
- **Google Play Services Location 21.3.0**
- **Accompanist Permissions 0.36.0**
- JUnit (Unit Testing)
- Espresso (UI Testing)

## Important Configuration

### Naver Maps Client ID
`app/src/main/AndroidManifest.xml`에서 네이버 지도 클라이언트 ID 설정:
```xml
<meta-data
    android:name="com.naver.maps.map.CLIENT_ID"
    android:value="47b5di8weq" />
```

⚠️ **주의**: 네이버 클라우드 플랫폼에서 Android 앱(`com.ScienceFiction.DronePassAndroid`)으로 등록 및 클라이언트 ID 발급 필요

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
- 지도 관련 작업 시 `MapScreen.kt` 참조
- 네이버 Maps SDK API: [공식 문서](https://navermaps.github.io/android-map-sdk/guide-ko/)
- 지도 생명주기는 반드시 관리 필요

## Next Steps (향후 구현 예정)

1. **도형 모델 구현**: Circle, Rectangle, Polygon 모델
2. **지도 오버레이**: 네이버 지도에 도형 그리기
3. **Firebase 통합**: Authentication, Firestore
4. **데이터 저장/불러오기**: 도형 정보 영속화
5. **설정 화면**: 앱 설정 관리
6. **탭 네비게이션**: Bottom Navigation 또는 Navigation Rail

## Reference
- 원본 iOS 프로젝트: `/Users/david/Development/Swift/myProjects/DronePass`
- iOS 아키텍처: MVVM, Manager 패턴, Repository 패턴

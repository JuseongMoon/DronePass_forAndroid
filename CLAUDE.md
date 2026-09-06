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

## Cross-Platform Contract

- iOS/Android 공유 Firestore wire-format은 [`docs/agents/FIRESTORE_CONTRACT.md`](docs/agents/FIRESTORE_CONTRACT.md)가 기준입니다.
  특히 `shapeType`은 쓰기 소문자 raw value, 읽기 대소문자 무시, unknown 값 스킵 계약을 유지해야 합니다.
- 설계 배경과 진행 중인 계획 문서는 `docs/agents/` 아래에 있습니다.

## Reference
- 원본 iOS 프로젝트: https://github.com/JuseongMoon/dronepass-ios
- iOS 아키텍처: MVVM, Manager 패턴, Repository 패턴

## 공개 저장소 규칙

이 저장소는 공개되어 있다. 커밋한 것은 되돌려도 남는다.

- **시크릿 금지** — API 키·토큰·서명 키(`*.jks`/`*.p12`)·서비스 계정 키·실제 사용자 데이터를 커밋하지 않는다.
  값은 **`local.properties`** 에만 두고 저장소에는 `*.example`만 올린다.
  소스·plist·manifest·주석·커밋 메시지 어디에도 값을 쓰지 않는다.
  이미 올렸다면 되돌리는 것으로 끝내지 말고 **키를 폐기·재발급**한다.
- **내부 정보 금지** — 로컬 절대경로(`/Users/…`), 저장소 밖 파일 참조, 관리자 URL,
  인프라 식별자(버킷·배포 ID·계정 번호), 개인 기기 식별자(UDID·시리얼),
  릴리스 진행 상태와 스토어 콘솔 절차는 문서에 남기지 않는다.
- **내부 문서 위치** — 가격 전략·미출시 기획·운영 절차·서버 계약은 저장소에 두지 않는다.
  로컬에 두고 gitignore 하되 **그 판단 근거를 이 문서에 적어** 다음 세션이 되돌리지 않게 한다.
  gitignore된 경로를 코드 주석이나 문서에서 참조하지 않는다 — 방문자에게는 끊어진 링크다.
- **문서 정확성** — 여기 적힌 버전·경로·명령·구조가 코드와 다르면 코드가 아니라 문서를 고친다.
  배포 타깃과 언어 버전은 프로젝트 기본값이 아니라 **앱 타깃의 실제 값**을 확인해 적는다.
- **브랜치** — 에이전트 작업 브랜치는 머지 후 지운다. 원격에 실험 브랜치를 남기지 않는다.
  **처음 push 하는 순간 그 브랜치의 문서·메모도 함께 공개된다.**
- **`main`에 force-push 하지 않는다.** 공개된 히스토리를 다시 쓰면 클론·포크한 쪽이 깨진다.
  (예외: 시크릿 제거 — 이때도 키 폐기가 먼저다.)
- **push 전 확인** — `git fetch origin && git status -sb`로 원격이 앞섰는지 보고, 앞섰으면 덮지 말고 rebase 한다.
  `git log origin/main..HEAD --stat`으로 올라갈 파일 전체를 확인해 무관한 파일을 분리하고,
  `git diff`에서 키·절대경로·기기 식별자가 없는지 본다. **`git add .` 금지.**

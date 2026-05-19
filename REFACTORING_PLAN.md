# DronePass Android 리팩토링 계획

> 작성일: 2026-05-11
> 기반 자료: 5개 영역(데이터/지도/알고리즘/UI/인프라) 코드 종합 점검 결과
> 선행 PR: `fix/critical-pri0-fixes` (커밋 `40dafb7`) — Pri 0 5건 즉시 수정 완료

---

## 0. 개요

5개 팀 에이전트(Agent A~E)가 92개 Kotlin 파일을 정독해 발견한 결함은 **총 124건** (Critical 24 / High 45 / Medium 55).
가장 위험한 **Pri 0 5건**은 별도 PR로 즉시 수정했고, 본 문서는 **잔여 119건**을 단계별로 진행할 수 있도록 정리한다.

### 핵심 발견 (재인용)

- **데이터 레이어**: 실시간 동기화 4중 결함, Migration SQL 불일치 — **Pri 0 PR에서 해결**
- **지도/오버레이**: `FlightZoneCalculator` Ray Casting 좌표축 혼선 — **Pri 0 PR에서 해결**. 잔여: MapView 누수/회전 카메라 손실/Overlay Diff 부재
- **알고리즘**: Haversine 반지름 불일치(iOS WGS-84 6378.137km vs Android 6371km), CRI Magnus NaN, SketchPointsCache TOCTOU
- **Feature UI**: LoginScreen 이중 LaunchedEffect, Kp/Weather 무한 새로고침, SettingsViewModel.signOut FCM/Sync 누락
- **인프라**: 백업규칙 파일명 불일치(클라우드 복원 시 폭주), POST_NOTIFICATIONS 런타임 미요청, release signingConfig 부재

### 진행 가이드

- 각 항목 앞 `[ ]` 체크박스로 진행 추적
- 모든 항목에 **파일경로:라인 / 문제 / 영향 / 수정 방안** 명시
- 각 Phase 종료 시 빌드 + 단위 테스트 통과 + 회귀 테스트 시나리오 수행

---

## 1. 권장 진행 순서

| Phase | 기간 | 범위 | 종료 기준 |
|---|---|---|---|
| Pri 0 | (완료) | Critical 5건 | PR 머지 + 빌드 통과 |
| Phase 1 | (완료) | 잔여 Critical 19건 | 출시 차단 결함 해소, 빌드 + 핵심 회귀 테스트 통과 |
| Phase 2 | (완료) | High 45건 | 성능/UX/일관성 개선, 60fps 기준 회귀 통과 |
| Phase 3 | (완료) | Medium 55건 | 코드 품질/유지보수 정비, Lint 0건 유지 |

---

## 2. Phase 1 — 잔여 Critical (19건) ✅ 완료

> 출시 차단·데이터 손실·크래시 위험. 5개 영역별 5개 커밋으로 처리 완료
> (`fix/critical-pri0-fixes` 브랜치, 커밋 `967bab8` ~ `a755801`).
> ./gradlew :app:assembleDebug + :app:testDebugUnitTest BUILD SUCCESSFUL.

### 2.1 데이터 레이어 (1건)

- [x] **A-C1: EncryptedPrefsHelper MasterKeys deprecation + 손상 대비 try-catch**
  - 파일: `core/data/local/EncryptedPrefsHelper.kt:25-32`, `service/FcmService.kt:94-103, 235-244`
  - 문제: `MasterKeys.getOrCreate` 는 `security-crypto 1.1.0-alpha06` 에서 deprecated. KeyStore 손상(앱 재설치/백업 복원/일부 OEM)에 대한 `GeneralSecurityException`/`IOException` 처리 없음.
  - 영향: 일부 단말 첫 실행 크래시 + FcmService에서 중복 EncryptedPrefs 생성으로 SRP 위반.
  - 수정:
    1. `MasterKey.Builder(context).setKeyScheme(KeyScheme.AES256_GCM).build()` API로 교체.
    2. `try-catch (GeneralSecurityException, IOException)` 감싸기 + 손상 감지 시 prefs 파일 삭제 후 재생성 폴백.
    3. FcmService의 중복 생성 로직을 EncryptedPrefsHelper 단일 진입점으로 통합.

### 2.2 지도/오버레이/스케치 (4건)

- [x] **B-C1: AndroidView update 람다의 위치 추적 재설정 누수**
  - 파일: `feature/map/MapScreen.kt:333-391`
  - 문제: `update` 람다가 매 recomposition마다 `setupLocationTracking(naverMap!!, context)` 호출 → 매번 새 `FusedLocationSource(activity, 1000)` 생성.
  - 영향: 메모리 누수 + 콜백 중복 등록으로 카메라가 위치로 점프.
  - 수정: `LaunchedEffect(allPermissionsGranted, mapReady) { setupLocationTracking(naverMap, context) }` 형태로 1회 실행 보장. `update` 람다에서는 호출 제거.

- [x] **B-C2: Configuration change(회전) 시 MapView 카메라 상태 손실**
  - 파일: `feature/map/MapScreen.kt:84-88, 622-641`, `AndroidManifest.xml` MainActivity
  - 문제: factory 람다의 `mapView.onCreate(null)`이 `savedInstanceState`를 항상 null로 전달 → 회전 시 카메라가 서울 시청(13.0)으로 초기화.
  - 영향: 사용자가 줌/이동한 위치가 회전 시 사라짐.
  - 수정 (택1):
    1. `MainActivity`에 `android:configChanges="orientation|screenSize|keyboardHidden"` 추가 (간단).
    2. 또는 `rememberSaveable`로 `CameraPosition`(target lat/lng, zoom, tilt, bearing) 4요소 보존 + factory에서 적용.

- [x] **B-C3: MapView 리스너 해제 누락 (`addOnCameraIdleListener`, `setOnMapLongClickListener`)**
  - 파일: `feature/map/MapScreen.kt:366-380` + 주변 `DisposableEffect`
  - 문제: factory에서 등록한 리스너가 `onDispose`에서 제거되지 않음.
  - 영향: 재초기화 시 중복 등록으로 `onCameraIdle` 폭주.
  - 수정: 등록한 리스너 참조를 `remember`로 보관 후 `DisposableEffect`의 `onDispose`에서 `naverMap.removeOnCameraIdleListener(...)` 명시.

- [x] **B-C4: OverlayManager 3종의 `naverMap` 참조 미해제 → 활동 누수**
  - 파일: `feature/map/overlay/ShapeOverlayManager.kt:163-172`, `feature/sketch/SketchOverlayManager.kt:142-145, 154-158`, `feature/vworld/FlightZoneOverlayManager.kt:127-133`
  - 문제: `clearOverlays()`는 overlay 인스턴스의 `map=null`만 호출하고, manager가 보유한 `naverMap` 참조는 끊지 않음. Activity 파괴 후에도 NaverMap 보유.
  - 영향: 메모리 누수.
  - 수정: 각 Manager에 `detach()` 메서드 추가 (`overlays.values.forEach { it.map = null }; naverMap = null`) + `MapScreen.kt`의 `DisposableEffect`의 `onDispose`에서 호출.

### 2.3 알고리즘 (3건)

- [x] **C-C1: Haversine 지구 반지름을 WGS-84 적도반지름으로 통일**
  - 파일: `core/util/FlightZoneCalculator.kt:22, 65-73`, `core/util/DistanceCalculator.kt:24, 42-54`
  - 문제: 두 구현 모두 `6371.0 km`(평균반지름) 사용. iOS 원본은 `6378137.0 m`(WGS-84 적도반지름). 한국 영역에서 0.112% 오차. 또한 Android는 `2*asin(sqrt(a))`인데 iOS는 `2*atan2(sqrt(a), sqrt(1-a))`로 수치 안정성 차이.
  - 영향: iOS↔Android 거리 결과가 100km당 약 112m 다름. 비행구역 경계 부근에서 안전 판정 일관성 결여 가능.
  - 수정: 상수 `EARTH_RADIUS_M = 6_378_137.0` 통일. `c = 2 * atan2(sqrt(a), sqrt(1-a))`로 통일. `FlightZoneCalculatorTest`에 iOS와 동일한 거리 결과 검증 추가(서울-부산 ≈ 325km 허용 범위 좁히기).

- [x] **C-C2: CRICalculator Magnus 공식의 NaN/0-division 가드**
  - 파일: `core/util/CRICalculator.kt:57-68`
  - 문제: `b + temperature = 243.04 + temperature`이 0이면 `exp` 인자가 NaN. `Double.isNaN(...)` 입력 가드도 없어 NaN이 그대로 전파.
  - 영향: 비현실적이지만 API 오류 시 위험 등급이 누락되어 사용자에게 안전 표시.
  - 수정: 함수 진입부에서 `temperature.isNaN() || dewPoint.isNaN() || temperature <= -243.04 || dewPoint <= -243.04` 시 sentinel(예: `Double.NaN` 또는 `CRIResult.Unknown`) 반환. 호출자에서 "측정 불가" UI 분기.

- [x] **C-C3: SketchPointsCache TOCTOU 중복 계산 방지**
  - 파일: `core/util/SketchPointsCache.kt:43-53`
  - 문제: 캐시 miss 시 mutex 해제 → `smoothUsingCatmullRom` 호출 → 다시 mutex 락으로 저장. 동일 sketchId 동시 호출 시 N개 코루틴이 동일 작업 수행.
  - 영향: 큰 스케치(수백~수천 포인트) 동시 요청 시 GC 압박/UI 렉.
  - 수정: in-flight 작업을 `Map<UUID, Deferred<List<Coordinate>>>`로 추적 → 이미 계산 중이면 동일 Deferred를 await. mutex 안에서 in-flight 등록.

### 2.4 Feature UI (5건)

- [x] **D-C1: LoginScreen 이중 `LaunchedEffect(authState)` 충돌**
  - 파일: `feature/auth/LoginScreen.kt:61-74`
  - 문제: 동일한 `authState` 키로 두 `LaunchedEffect`가 등록되어 `LoggedIn`→`Error` 전이 시 첫 effect는 `onLoginSuccess`, 두 번째는 스낵바 표시.
  - 영향: 화면 전환 + 스낵바 동시 발생 가능.
  - 수정: 단일 `LaunchedEffect(authState) { when(authState) { is Success -> ...; is Error -> ... } }` 패턴으로 통합.

- [x] **D-C2: SettingsViewModel.signOut() / deleteAccount()에서 FCM 토큰 비활성화 + 실시간 동기화 중단 누락**
  - 파일: `feature/settings/SettingsViewModel.kt:312, 357` (cf. `feature/auth/AuthViewModel.kt`)
  - 문제: `authRepository.signOut()`만 호출. `AuthViewModel.signOut`은 FCM 비활성화 + `realtimeSyncManager.stopListening()`까지 처리하나 SettingsViewModel은 누락.
  - 영향: 로그아웃 후 Firestore 권한 오류 + FCM 토큰 잔존(타 계정 알림 위험).
  - 수정: 공통 `SignOutUseCase` 추출 → AuthViewModel/SettingsViewModel 모두 호출. (`FcmService.deactivateToken(...)` + `RealtimeSyncManager.stopListening()` + `authRepository.signOut()` 순서)

- [x] **D-C3: WebDocumentScreen JavaScript 활성화 + 도메인 화이트리스트 부재**
  - 파일: `feature/settings/WebDocumentScreen.kt:55, 92-93`
  - 문제: `@SuppressLint("SetJavaScriptEnabled")` + 외부 URL(Notion 등) 로드. `shouldOverrideUrlLoading`으로 도메인 제한 없음.
  - 영향: 악성 리다이렉트 페이지 경유 시 JS 실행 가능성.
  - 수정: `WebViewClient.shouldOverrideUrlLoading`에서 허용 도메인(`notion.so`, `*.dronepass.app` 등) 화이트리스트 검증. JS는 필요한 페이지만 활성화하거나 비활성화.

- [x] **D-C4: AuthViewModel init 순서 — checkAuthState 누락**
  - 파일: `feature/auth/AuthViewModel.kt:55-58`
  - 문제: init에서 `isLoggedIn` 분기 → `performFullSync` + `startRealtimeSync` 호출. 그러나 `checkAuthState()`가 먼저 호출되지 않아 `_authState`가 `LoggedIn`으로 갱신되기 전에 동기화 시작 → 일시적 Loading→Main 깜빡임.
  - 영향: UX 결함(앱 시작 시 LoginScreen이 잠깐 보일 수 있음).
  - 수정: init 순서를 `checkAuthState() → performFullSync() → startRealtimeSync()`로 명시. 또는 `combine`된 단일 흐름으로 처리.

- [x] **D-C5: SavedListScreen LazyColumn key 충돌 잠재 위험**
  - 파일: `feature/saved/SavedListScreen.kt:120-187`
  - 문제: 세 섹션(active/notStarted/expired)이 모두 `key = { it.id }`. 분류가 mutually exclusive하지만 `flightStartDate == flightEndDate == 현재시각` 경계조건에서 다른 분기 가능성.
  - 영향: 동일 key가 두 번 등장 시 `IllegalStateException` 크래시.
  - 수정: 섹션 prefix를 키에 포함(`key = { "active-${it.id}" }` 등) 또는 단일 LazyColumn으로 평탄화.

### 2.5 인프라 (6건)

- [x] **E-C1: 백업 규칙 파일명 불일치 — `dronepass_encrypted_prefs.xml`**
  - 파일: `app/src/main/res/xml/backup_rules.xml:20`, `data_extraction_rules.xml:19, 44`
  - 문제: `encrypted_prefs.xml`로 제외, 실제 파일명은 `dronepass_encrypted_prefs.xml` (`EncryptedPrefsHelper.kt:20`, `FcmService.kt:98,239`).
  - 영향: 암호화 파일이 클라우드 백업됨 → 복원 시 마스터키 불일치로 SharedPreferences 폭주(즉시 크래시).
  - 수정: 두 XML 모두 `<exclude domain="sharedpref" path="dronepass_encrypted_prefs.xml"/>` 으로 정정. 가능하면 상수화하여 코드와 동기화.

- [x] **E-C2: POST_NOTIFICATIONS 런타임 권한 요청 흐름 추가**
  - 파일: `AndroidManifest.xml:13` + 신규 `feature/settings/NotificationPermissionRequest.kt`
  - 문제: 선언만 있고 어디서도 `ActivityResultContracts.RequestPermission` 호출 없음. Android 13+에서 알림 미노출.
  - 영향: FCM·AlarmManager 알림이 사용자에게 보이지 않음.
  - 수정: 설정 화면의 알림 토글 ON 시점에 권한 요청. denied 시 안내 다이얼로그 + 설정 진입 Intent.

- [x] **E-C3: SCHEDULE_EXACT_ALARM 사용자 설정 진입 흐름 추가 (Android 12~13)**
  - 파일: `service/NotificationScheduler.kt:241-273` + 신규 UI
  - 문제: `canScheduleExactAlarms()` 분기는 있으나 `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` Intent로 사용자가 설정 부여하도록 유도하는 UI 없음.
  - 영향: 알람 해제 시 일출/일몰 알림 미발송.
  - 수정: 알림 설정 화면에서 권한 부재 감지 시 안내 카드 + "정확한 알람 권한 부여" 버튼 → settings intent.

- [x] **E-C4: AppCompat 테마 → Material3 변환**
  - 파일: `app/src/main/res/values/themes.xml:4`, `app/src/main/res/values-night/themes.xml` (신규)
  - 문제: parent가 `Theme.AppCompat.Light.NoActionBar`. Compose Material3 앱 부정합. `values-night/themes.xml` 부재로 다크 모드 시스템 테마 분리 안 됨.
  - 영향: statusBar 색상/inflation 단계의 AppCompat 의존성. `Theme.kt`의 dynamicColor와 충돌.
  - 수정: parent를 `Theme.Material3.DayNight.NoActionBar` 또는 `android:Theme.Material.Light.NoActionBar`로 변경. `values-night` 분리. 가능하면 `androidx.appcompat:appcompat` 의존성 제거.

- [x] **E-C5: Release signingConfig + buildTypes.debug 블록 추가**
  - 파일: `app/build.gradle.kts:46-55`
  - 문제: `signingConfigs { release { ... } }` 부재 → release가 debug 키로 서명 → Play Store 업로드 불가. `debug { applicationIdSuffix=".debug" }` 등 dev/prod 분리도 없음.
  - 영향: 출시 차단.
  - 수정:
    1. `app/keystore/`에 release keystore 생성 또는 Play App Signing 사용.
    2. `keystore.properties`로 비밀 분리 + `.gitignore`.
    3. `buildTypes { debug { applicationIdSuffix = ".debug" }; release { signingConfig = ... } }`.

- [x] **E-C6: `<queries>` 블록 추가 — 외부 지도 앱 호출 보장**
  - 파일: `AndroidManifest.xml`
  - 문제: Android 11+ package visibility 제한으로 `Intent.resolveActivity()` 가 다른 앱 가시성 없이 호출 시 `null` 반환. `ShapeDetailSheet.kt:367,383`의 kakaomap/tmap 호출에 영향.
  - 영향: 외부 지도 앱 열기 실패.
  - 수정: `<queries><package android:name="net.daum.android.map"/><package android:name="com.skt.tmap.ku"/>...<intent><action android:name="android.intent.action.VIEW"/><data android:scheme="https"/></intent></queries>` 추가.

---

## 3. Phase 2 — High (45건) ✅ 완료

> 사용자 체감/성능/UX/일관성 결함. 5개 영역별 5개 커밋으로 처리 완료
> (`fix/critical-pri0-fixes` 브랜치, 커밋 `ea7b74d` ~ `43c5d5c`).
> ./gradlew :app:assembleDebug + :app:testDebugUnitTest BUILD SUCCESSFUL.

### 3.1 데이터 레이어 (9건)

- [x] **A-H1: `syncFromFirebase`에 LWW 비교 추가** — Repository 3종 모두 forEach로 단순 덮어쓰기. (`{Drone,Shape,Sketch}Repository.syncFromFirebase`)
- [x] **A-H2: `performFullSync`의 "로컬 우세 데이터" 업로드 누락 보완** — `localOnly = merged.filter { it.id !in serverById }` 만으로는 로컬이 LWW에서 이긴 데이터가 업로드되지 않음. (`ShapeRepository.kt:174-211` 외 2개)
- [x] **A-H3: `RealtimeSyncManager.onShapesUpdated/onDronesUpdated/onSketchesUpdated` 콜백 — 데드 코드 제거 또는 실제 연결**
- [x] **A-H4: `shapeSyncInProgress`/`sketchSyncInProgress` Boolean → Mutex 또는 AtomicBoolean** (`RealtimeSyncManager.kt:81-86`)
- [x] **A-H5: `RealtimeSyncManager.scheduleShapeRetrySync` — 재시도 코루틴 안에서 `currentListeningUserId == null` 가드 추가** (로그아웃 후 잔여 동기화 시도 방지)
- [x] **A-H6: `SketchRepository.softDeleteSketch`/`restoreSketch` 디바운싱/배치 push** — Undo/Redo 빈도 높음
- [x] **A-H7: `*FirebaseStore.load*` catch 블록의 `emptyList()` 반환을 `Result<List<T>>` 또는 sealed 결과로 변경** — 네트워크 오류와 빈 데이터 구분
- [x] **A-H8: `ShapeFirebaseStore.firestoreDataToShape:208-209` baseCoordinate 부재 시 서울 시청 fallback 제거** — `return null`로 스킵 + Crashlytics 로깅
- [x] **A-H9: `EntityMapper.toDomain`의 `ShapeType.valueOf` try-catch 폴백 추가** (`mapper/EntityMapper.kt:14-30`)

### 3.2 지도/오버레이/스케치 (9건)

- [x] **B-H1: `ShapeOverlayManager` Diff 패턴 도입** — 현재 매번 `clearOverlays()` 후 전체 재생성. `SketchOverlayManager`의 Map 기반 Diff(add/remove/update) 차용.
- [x] **B-H2: `FlightZoneOverlayManager` Diff 패턴 도입 + zone.id 기반 캐시** — 카메라 이동마다 전체 재생성
- [x] **B-H3: `removeOutOfBoundsOverlays` 실제 호출 추가** — 함수만 정의되어 있고 어디서도 호출되지 않음 (`FlightZoneOverlayManager.kt:111-122`)
- [x] **B-H4: `currentDrawingPoints` LaunchedEffect key 좁히기** — color/strokeWidth/opacity는 별도 effect로 분리 (`MapScreen.kt:251-262`)
- [x] **B-H5: `SketchViewModel.continueDrawing`의 O(n²) 리스트 복사** — MutableStateList 또는 내부 ArrayList + 새 리스트 emit 분리 (`SketchViewModel.kt:138-147`)
- [x] **B-H6: `SketchViewModel.deleteSketchAtPoint` Mutex 직렬화** — 지우개 드래그 시 race condition 방지 (`SketchViewModel.kt:215-217`)
- [x] **B-H7: BoundingBox 경도 변환을 위도 보정(cosLat) 기반으로 변경** — 현재 85km 고정 (`SketchViewModel.kt:237-238`)
- [x] **B-H8: `filteredShapes` 시간 기반 재평가** — 만료 시점이 흘러도 즉시 반영 안 됨 (`MapViewModel.kt:309-348`)
- [x] **B-H9: `cameraEvent` SharedFlow replay=1 또는 mapReady와 combine** — 첫 emit 손실 가능성 (`MapScreen.kt:282-302`)

### 3.3 알고리즘 (8건)

- [x] **C-H1: `SketchSmoothingAlgorithm.segmentsPerOriginal=0` 가드** (`SketchSmoothingAlgorithm.kt:29-77`)
- [x] **C-H2: Catmull-Rom 보간 시작점 정합 — iOS의 `(0,1]` 구간과 일치하도록 수정** (`SketchSmoothingAlgorithm.kt:66-71`)
- [x] **C-H3: `CoordinateParser` DMS 분/초 범위 검증** (`CoordinateParser.kt:20-23, 117-128`)
- [x] **C-H4: `CoordinateParser` Decimal 패턴 anchoring** — `Regex.find()`가 입력 어디서나 매치 (`CoordinateParser.kt:29-31, 139-144`)
- [x] **C-H5: `GustDifferenceCalculator` NaN/음수 입력 방어** — `sustainedWind < 0`/`gustWind < 0`/`NaN` 가드 (`GustDifferenceCalculator.kt:60-111`)
- [x] **C-H6: `GeocodingResponse` non-null 필드를 nullable로 완화** — 에러 응답 가변성 대응 (`GeocodingResponse.kt:10-14`)
- [x] **C-H7: `Coordinate.toString` `String.format` Locale.ROOT 명시** — 유럽 로케일에서 소수점 콤마 변환 방지 (`Coordinate.kt:46`)
- [x] **C-H8: `VWorldModels` NOTAM 2-digit year → 4-digit + Calendar non-lenient** (`VWorldModels.kt:155-170`)

### 3.4 Feature UI (13건)

- [x] **D-H1: KpViewModel/WeatherViewModel `while(true) { delay }` → `Lifecycle.repeatOnLifecycle(STARTED)`** — 백그라운드 자동 갱신 중단 (`KpViewModel.kt:107-114`, `WeatherViewModel.kt:105-112`)
- [x] **D-H2: SavedListViewModel 4개 파생 StateFlow → 단일 `data class Sections` 통합 + Dispatchers.Default** — 검색 1글자에 500개 정렬 (`SavedListViewModel.kt:52-89`)
- [x] **D-H3: SavedListScreen 11번 collect → 자식 Composable 분리** (`SavedListScreen.kt:60-77`)
- [x] **D-H4: ShapeEditScreen `remember`에 `shape` key 부여** — 편집 모드 전환 시 잔존값 방지 (`ShapeEditScreen.kt:89, 117-128, 132-137`)
- [x] **D-H5: DroneEditSheet 키 입력마다 List 순회 → `remember(name, drone?.id)`로 메모이즈** (`DroneEditSheet.kt:83-84`)
- [x] **D-H6: DroneViewModel.suggestNextColor/isDuplicateName — StateFlow.value 시점 문제** — 빈 상태에서 항상 BLUE 제안 가능 (`DroneViewModel.kt:121-123, 130-133`)
- [x] **D-H7: DroneListScreen에서 `suggestNextColor()` 매번 호출 → `remember(drones)`로 메모이즈** (`DroneListScreen.kt:163`)
- [x] **D-H8: DroneDetailSheet shapeCount 깜빡임 → `mutableStateOf<Int?>(null)` + 로딩 표시** (`DroneDetailSheet.kt:75-77`)
- [x] **D-H9: ShapeEditScreen DatePicker 타임존 처리 명시화 + 취소 시 입력 보존** (`ShapeEditScreen.kt:602-605, 651-664`)
- [x] **D-H10: ShapeEditScreen 좌표 파싱 에러 시 Save 버튼 `enabled = !coordinateParseError`** (`ShapeEditScreen.kt:223-256`)
- [x] **D-H11: SavedListScreen에서 `getDroneName` O(N*M) → droneId→droneName Map 캐시** (`SavedListScreen.kt:62-77`, `SavedShapeListItem.kt:129,156,183,207`)
- [x] **D-H12: KpForecastScreen Refresh 버튼 `enabled = !isLoading`** (`KpForecastScreen.kt:78`)
- [x] **D-H13: SavedListScreen 스와이프 삭제 → 확인 다이얼로그 또는 Undo Snackbar** (`SavedListScreen.kt:218-258`)

### 3.5 인프라 (6건)

- [x] **E-H1: BootCompletedReceiver 서울 좌표 하드코딩 제거 + 마지막 위치 DataStore 캐시 사용** (`BootCompletedReceiver.kt:39-40, 74-75`)
- [x] **E-H2: NotificationChannel `IMPORTANCE_HIGH` 분리 — 비행 시각 알림 채널 별도 생성** (`FcmService.kt:42`)
- [x] **E-H3: NavGraph dead route 정리 — `Screen.SavedList`/`Screen.KpForecast` 진입 경로 일관화** (`ui/navigation/NavGraph.kt:46-50`, `MainScreen.kt:48,142-156`)
- [x] **E-H4: FcmService를 `@AndroidEntryPoint`로 변경 + `@Inject lateinit var` 패턴 적용** (`service/FcmService.kt:57,67,182,204`)
- [x] **E-H5: NetworkModule의 `HttpLoggingInterceptor.Level.BODY`를 `BuildConfig.DEBUG`에서만 적용** — Release에서 API 키/PII 노출 차단 (`core/di/NetworkModule.kt:30-43`)
- [x] **E-H6: Naver OkHttp Retrofit 분리 — `@Named("NaverRetrofit")`** (다른 API에 Naver 헤더 누설 방지) (`core/di/NetworkModule.kt:71-77`)

---

## 4. Phase 3 — Medium (55건) ✅ 완료

> 코드 품질/일관성/리팩토링. 5개 영역별 5개 커밋으로 처리 완료
> (`fix/critical-pri0-fixes` 브랜치, 커밋 `f10dde9` ~ `4f890ae`).
> ./gradlew :app:assembleDebug + :app:testDebugUnitTest BUILD SUCCESSFUL.

### 4.1 데이터 레이어 (8건)

- [x] **A-M1: 3종 Repository(Shape/Drone/Sketch)의 syncToFirebase/syncFromFirebase/performFullSync 중복 추상화** — `SyncableRepository<T>` 도입
- [x] **A-M2: `KpIndexRepository` 캐시 변수 `@Volatile` 또는 Mutex 보호** (`KpIndexRepository.kt:29-33, 197-203`)
- [x] **A-M3: `WeatherRepository` 캐시 동시성** (`WeatherRepository.kt:22-26, 36-69`)
- [x] **A-M4: `VWorldRepository`의 `LinkedHashMap` accessOrder LRU → `Collections.synchronizedMap` 또는 Mutex** (`VWorldRepository.kt:38, 75-78`)
- [x] **A-M5: `SketchFirebaseStore.firestoreDataToSketch` points unchecked cast 검토** (`SketchFirebaseStore.kt:183-188`)
- [x] **A-M6: `Coordinate` JSON 직렬화 성능 — 매 호출 `JSONObject` 인스턴스 생성** (`Coordinate.kt:52-105`)
- [x] **A-M7: Repository의 `forEach { insert(it) }`을 DAO 배치 메서드로 일괄 처리** (트랜잭션 효율)
- [x] **A-M8: `EntityMapper.toDomain` null-safety 강화**

### 4.2 지도/오버레이/스케치 (10건)

- [x] **B-M1: OverlayManager를 ViewModel scope으로 이전 또는 `rememberSaveable` 보존** (`MapScreen.kt:91-93`)
- [x] **B-M2: `SketchOverlayManager.dpToPx` 하드코딩 2.5 → displayMetrics 기반** (`SketchOverlayManager.kt:186-191`)
- [x] **B-M3: `keepScreenOn` 명시적 `view.keepScreenOn = keepScreenOn`** (`MapScreen.kt:160-167`)
- [x] **B-M4: FlightZoneOverlayManager `globalZIndex` 우선순위 재검토 — Shape보다 위에 그려지는 문제** (`FlightZoneOverlayManager.kt:66`)
- [x] **B-M5: OverlayManager 콜백 등록을 `SideEffect`로 이전** (`MapScreen.kt:188-199`)
- [x] **B-M6: MapScreen Composable 분리 — 25+ collect를 자식 Composable로 분할** (`MapScreen.kt:71`)
- [x] **B-M7: MapFloatingButtons의 `String.format("%.1f", currentKpValue)`을 `remember`로 메모이즈** (`MapFloatingButtons.kt:85`)
- [x] **B-M8: SketchToolbar 색상 슬라이더 round-trip 부동소수점 — local sliderPosition을 SSOT로** (`SketchToolbar.kt:450-451`)
- [x] **B-M9: SketchOverlayManager Catmull-Rom 병렬화** (`SketchOverlayManager.kt:72-74`)
- [x] **B-M10: MapViewModel.debounce 구현 → distinctUntilChanged + Flow로 자연화** (`MapViewModel.kt:459-470`)

### 4.3 알고리즘 (11건)

- [x] **C-M1: FlightZoneCalculator yi/xi 변수명 latI/lonI로 통일** — Pri 0에서 일부 완료, 잔여 정리
- [x] **C-M2: CRICalculator `windFactor=1.0`이면 풍속 보정 사실상 무동작 — 의도 검토** (`CRICalculator.kt:42-45`)
- [x] **C-M3: SketchSmoothing `points.size==2` linearInterpolation에서 `segmentsPerOriginal` 무시** (`SketchSmoothingAlgorithm.kt:34`)
- [x] **C-M4: WeatherCodeMapper WMO 코드 4/5/10 등 누락 보완** (`WeatherCodeMapper.kt:19-36`)
- [x] **C-M5: AltitudeFormatter Regex companion 객체로 캐시** (`AltitudeFormatter.kt:103,118`)
- [x] **C-M6: ShapeModel `effectiveColor` 의미 명확화 또는 제거** (`ShapeModel.kt:43-44`)
- [x] **C-M7: ShapeType enum CIRCLE 외 추가 또는 마이그레이션 미완 표시** (`ShapeType.kt:1-5`)
- [x] **C-M8: PaletteColor.composeColor `parseColor` IllegalArgumentException 방어** (`PaletteColor.kt:20-21`)
- [x] **C-M9: VWorldModels.properties strict 타입 정의** (`VWorldModels.kt:48`)
- [x] **C-M10: SketchPointsCache 코멘트로 LRU 의도 명시** — 이미 잘 설계됨, 가독성 향상만 (`SketchPointsCache.kt:20-26`)
- [x] **C-M11: KpIndexModel.fromKp `kp == 9.0` 경계 검증** (`KpIndexModel.kt:36-55`)

### 4.4 Feature UI (15건)

- [x] **D-M1: SortOption.label → `@StringRes` labelRes로 다국어화** (`feature/saved/SortOption.kt`)
- [x] **D-M2: 날짜 포맷 `SimpleDateFormat("yyyy년 MM월 dd일 HH:mm")` 다국어화** (`ShapeDetailSheet.kt:74`, `ShapeEditScreen.kt:183-184`, `SavedShapeListItem.kt:45`)
- [x] **D-M3: AuthViewModel `_syncMessage` → `SharedFlow` 또는 Channel** (`AuthViewModel.kt:45-50`)
- [x] **D-M4: SettingsViewModel.deleteAccount 트랜잭션 보장** — `Result<Unit>` 단계 합성 (`SettingsViewModel.kt:323-365`)
- [x] **D-M5: DroneViewModel.deleteDrone atomic update 일관성** (`DroneViewModel.kt:111-113`)
- [x] **D-M6: SavedListViewModel.dismissShapeDetail 중복 호출 정리** (`SavedListViewModel.kt:117-120`)
- [x] **D-M7: SavedListScreen 시트 dismiss 후 화면 전환 — sheetState.hide() await** (`SavedListScreen.kt:193-209`)
- [x] **D-M8: SearchAddressSheet 에러 메시지 다국어화** (`SearchAddressSheet.kt:73-93`)
- [x] **D-M9: WeatherViewModel/KpViewModel 에러 메시지 `@StringRes` 모델 노출** (`WeatherViewModel.kt:148,154,173`)
- [x] **D-M10: `String.format("%.2f", value)` Locale 명시** (`KpForecastScreen.kt:236,569,635`, `WeatherCharts.kt:405,425`)
- [x] **D-M11: SunTimeline/WeatherCharts `textSize = 24f` → sp 단위** (`SunTimeline.kt:171`, `WeatherCharts.kt:112`)
- [x] **D-M12: KpCharts `density = LocalDensity.current` 미사용 잔재 제거** (`KpCharts.kt:66, 316`)
- [x] **D-M13: ShapeDetailSheet 화면 회전 시 maxSheetHeight 점프** (`ShapeDetailSheet.kt:68-69`)
- [x] **D-M14: SettingsSubScreen.Terms/Privacy/LocationTerms enum 단순화** (`SettingsScreen.kt:135-153`)
- [x] **D-M15: DroneEditSheet 이륙무게/크기 자유 입력에 숫자 범위 검증** (`DroneEditSheet.kt:170-176`)

### 4.5 인프라 (11건)

- [x] **E-M1: Hilt RepositoryModule의 `@Provides` 중복 제거 — `@Inject constructor`로 자동 해결** (`core/di/RepositoryModule.kt:36-110`)
- [x] **E-M2: Retrofit 인스턴스 `@Named`로 모두 명시 (`vWorldRetrofit`, `kpNoaaRetrofit`, `kpGfzRetrofit`, `weatherRetrofit`, `naverRetrofit`)**
- [x] **E-M3: `provideRetrofit`(Naver) connectTimeout/readTimeout 추가** (`core/di/NetworkModule.kt`)
- [x] **E-M4: ui/theme/Color.kt 기본 템플릿(Purple40/80, Pink40/80) → 브랜드 팔레트 정의** (`ui/theme/Color.kt`)
- [x] **E-M5: Type.kt bodyLarge 외 typography 토큰 정의**
- [x] **E-M6: Theme.kt `dynamicColor` 정책 결정 + statusBar/navigationBar 색상 처리** (`ui/theme/Theme.kt:40`)
- [x] **E-M7: DronePassDatabase 빈 `MIGRATION_2_3` 제거** — Pri 0 PR에서 일부 처리. 추가 정리
- [x] **E-M8: `androidx.appcompat:appcompat:1.7.0` 의존성 점진적 제거** — Compose-only 전환 (`app/build.gradle.kts:74`)
- [x] **E-M9: Compose BOM 2024.09.00 → 2025.x 업그레이드 검토** (`gradle/libs.versions.toml:10`)
- [x] **E-M10: BootCompletedReceiver `<category android:name="android.intent.category.DEFAULT"/>` 추가**
- [x] **E-M11: `local.properties` 의존성 키 분리 또는 BuildConfig로 이전 검토 (API 키 보안)**

---

## 5. 부록

### 5.1 영역별 잔여 통계

| 영역 | Critical Pri 1 | High | Medium | 합계 |
|---|---:|---:|---:|---:|
| A. 데이터 레이어 | 1 | 9 | 8 | 18 |
| B. 지도/오버레이/스케치 | 4 | 9 | 10 | 23 |
| C. 알고리즘/유틸/도메인/API | 3 | 8 | 11 | 22 |
| D. Feature UI | 5 | 13 | 15 | 33 |
| E. 인프라 | 6 | 6 | 11 | 23 |
| **합계** | **19** | **45** | **55** | **119** |

### 5.2 Pri 0 PR 요약 (참고)

브랜치 `fix/critical-pri0-fixes` 커밋 `40dafb7`에서 처리:
1. Migration 1→2 SQL을 v2 entity 스키마와 정합화 (DronePassDatabase.kt)
2. RealtimeSyncManager `getLong("lastModified")` → `getTimestamp(...).time` (레거시 폴백 유지)
3. SketchFirebaseStore 메타데이터 경로 `server` → `sketchServer`
4. Repository 3종에 Firebase 즉시 푸시 헬퍼 추가 (`syncXToFirebase`)
5. FlightZoneCalculator Ray Casting 변수명/식 정합 + FlightZoneCalculatorTest 14건 추가

빌드: `./gradlew :app:assembleDebug` BUILD SUCCESSFUL
테스트: `./gradlew :app:testDebugUnitTest --tests FlightZoneCalculatorTest` BUILD SUCCESSFUL

### 5.3 일일/주간 작업 단위 제안

- **하루 작업량**: Critical 1~2건 또는 High 3~5건 또는 Medium 5~8건
- **주간 PR 단위**: 동일 영역의 Critical/High 5~10건씩 묶어 1~2개 PR
- **회귀 테스트**: 각 PR 후 `./gradlew :app:assembleDebug` + 핵심 UI 시나리오 5가지 (지도/도형 CRUD/스케치/로그인/설정)

### 5.4 마이그레이션 계획서와의 정합

`MIGRATION_PLAN.md`의 출시 후 개선 항목(크로스 플랫폼 호환성, 앱 서명, 테스트 작성, LeakCanary)은 본 문서의 다음 항목들과 매핑:
- 크로스 플랫폼 호환성 → Pri 0 PR(동기화 4건) + Phase 2 데이터 레이어 H-항목
- 앱 서명 → E-C5 (Phase 1)
- 단위/UI 테스트 작성 → FlightZoneCalculatorTest 신규 작성 + 향후 Phase 별 각 PR에 동반
- LeakCanary → Phase 1 B-C1~C4 해결 후 도입 권장 (현재 누수가 있어 신호 폭주 위험)

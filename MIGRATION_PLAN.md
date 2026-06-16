# DronePass iOS → Android 마이그레이션 계획서

> 작성일: 2026-02-24
> 팀 구성: 팀 리드, 개발 리드, 기획 리드, 디자인 리드, QA 리드
> 현재 기준: 이 문서는 초기 마이그레이션 계획의 역사 기록입니다. 본문의 "현재 상태", "향후 개선 사항", "MVP 제외" 문구는 작성 당시 기준으로 보존되어 있으며 최신 상태가 아닐 수 있습니다. 최신 Android 구현/출시 준비 상태는 `README.md`와 `NEXT_STEPS.md`를 기준으로 확인하세요.

---

## 1. 프로젝트 현황

### iOS (원본)
- **파일**: 93개 Swift 파일
- **아키텍처**: MVVM + Manager(18개) + Repository(6개)
- **주요 기능**: 지도, 도형 관리, 스케치, Firebase 동기화, VWorld 비행구역, 날씨/Kp 지수
- **외부 서비스**: Firebase(Auth/Firestore/Messaging), 네이버 Maps, VWorld API, WeatherKit

### Android (현재)
- **파일**: 7개 Kotlin 파일
- **완료 기능**: 네이버 지도 표시, 위치 권한 처리
- **구현 갭**: 86개 파일 상당의 기능 부재

---

## 2. 기술 스택 매핑

### 언어/UI
| iOS | Android | 비고 |
|---|---|---|
| SwiftUI View | Composable 함수 | 선언적 UI 1:1 대응 |
| @State / @Binding | remember / mutableStateOf | 로컬 상태 |
| @StateObject | viewModel() + ViewModel | 생명주기 인식 |
| @Observable / @Published | MutableStateFlow / StateFlow | 반응형 상태 |
| Combine (sink, combine) | Kotlin Flow (collect, combine) | 반응형 스트림 |
| @MainActor | Dispatchers.Main / viewModelScope | 메인 스레드 |
| async/await | coroutine suspend fun | 비동기 |
| NotificationCenter | SharedFlow / EventBus | 이벤트 브로드캐스트 |

### 데이터 저장
| iOS | Android |
|---|---|
| UserDefaults | DataStore (Preferences) |
| Keychain | EncryptedSharedPreferences |
| FileManager + JSON | Room DB |
| Codable | kotlinx.serialization / Moshi |

### 외부 서비스
| iOS | Android | 비고 |
|---|---|---|
| Firebase Auth (Apple 로그인) | Firebase Auth (Google 로그인) | **리스크**: 계정 연결 필요 |
| Firestore | Firestore for Android | 동일 DB 공유 |
| Firebase Messaging | Firebase Messaging | 동일 |
| WeatherKit | **Open-Meteo API** | **대체 필수** (Apple 전용) |
| 네이버 Maps SDK | 네이버 Maps SDK 3.23.1 | 적용 완료 |
| VWorld WFS API | VWorld WFS API | REST API 동일 |
| CLLocationManager | FusedLocationProviderClient | 적용 완료 |

### UI 컴포넌트 매핑
| SwiftUI | Compose |
|---|---|
| VStack / HStack / ZStack | Column / Row / Box |
| List / LazyVStack | LazyColumn |
| NavigationStack | NavHost + NavController |
| TabView | NavigationBar + Scaffold |
| .sheet() | ModalBottomSheet |
| .alert() | AlertDialog |
| Toggle | Switch |
| Picker | ExposedDropdownMenuBox |
| DatePicker | DatePickerDialog (M3) |
| TextField | OutlinedTextField |
| Canvas | Canvas (직접 대응) |

---

## 3. Android 패키지 구조

```
com.ScienceFiction.DronePassAndroid/
├── app/
│   ├── DronePassApplication.kt       # Hilt 진입점
│   └── MainActivity.kt
├── core/
│   ├── di/                            # Hilt DI 모듈
│   │   ├── AppModule.kt
│   │   ├── NetworkModule.kt
│   │   ├── FirebaseModule.kt
│   │   └── RepositoryModule.kt
│   ├── data/
│   │   ├── local/
│   │   │   ├── room/                  # Room DB (Shape, Drone, Sketch)
│   │   │   ├── datastore/             # 앱 설정
│   │   │   └── cache/                 # LRU 캐시
│   │   ├── remote/
│   │   │   ├── firebase/              # Firestore Store
│   │   │   └── vworld/                # VWorld API
│   │   └── repository/                # Repository (Local + Remote)
│   └── util/                          # 좌표, 거리, 암호화 유틸
├── domain/
│   └── model/                         # ShapeModel, DroneModel, SketchModel 등
├── feature/
│   ├── map/                           # 지도 화면, 오버레이
│   ├── auth/                          # 로그인
│   ├── shape/                         # 도형 목록, 상세, 편집
│   ├── drone/                         # 드론 관리
│   ├── sketch/                        # 스케치
│   ├── vworld/                        # 비행구역
│   └── settings/                      # 설정
├── service/
│   ├── RealtimeSyncService.kt         # 실시간 동기화
│   ├── FcmService.kt                  # 푸시 알림
│   └── LocationService.kt             # 위치 서비스
└── ui/
    ├── theme/                         # Color, Type, Theme
    ├── component/                     # 공통 UI 컴포넌트
    └── navigation/                    # NavGraph
```

---

## 4. 마이그레이션 단계 (Phase)

### Phase 0: 기반 아키텍처 (2주)

**목표**: MVVM + Repository 패턴 골격, DI 프레임워크 도입

**작업 내용**:
- Hilt DI 설정 (`@HiltViewModel`, `@Singleton`)
- 패키지 구조 생성
- Navigation Compose 탭 네비게이션 (BottomNavigationBar 3탭)
- Room DB 스키마 (ShapeEntity, DroneEntity, SketchEntity)
- DataStore + EncryptedSharedPreferences 초기화
- Kotlin data class 포팅 (ShapeModel, DroneModel, SketchModel, Coordinate)

**iOS 참조 파일**:
- `Shape/ShapeModel.swift`
- `Sketch/SketchModel.swift`
- `Manager/DroneModel.swift`
- `Manager/CoordinateManager.swift`

**품질 게이트**:
- [ ] 빌드 성공, Lint 경고 0건
- [ ] 3탭 네비게이션 동작 확인
- [ ] Room DB CRUD Unit Test 통과

---

### Phase 1: 지도 + 도형 오버레이 (3~4주)

**목표**: 핵심 지도 기능, 도형 생성/편집/삭제, 로컬 저장

**작업 내용**:
- MapViewModel 구현 (오버레이 관리, 줌 레벨)
- 원형 도형 네이버 Maps 오버레이 (`CircleOverlay`)
- 도형 생성/편집 BottomSheet (ShapeEditScreen)
- 도형 상세 조회 (ShapeDetailScreen)
- 네이버 Geocoding 주소 검색 (Retrofit)
- 좌표 유틸리티 (DistanceCalculator)
- 플로팅 버튼 메뉴 (MainFloatingButton)
- Room DB CRUD

**iOS 참조 파일**:
- `Model/MapViewModel.swift` (전체)
- `Shape/View/ShapeEditView.swift`
- `Core/NaverGeocodingService.swift`
- `View/MainFloatingButtonView.swift`

**품질 게이트**:
- [ ] 원형 도형 지도 위 렌더링 확인
- [ ] ShapeModel 파싱 Unit Test 커버리지 90%
- [ ] 도형 생성→저장→재실행→복원 E2E 통과

---

### Phase 2: Firebase 인증 + 동기화 (3~4주)

**목표**: 로그인/로그아웃, Firestore 동기화, LWW 충돌 해결

**작업 내용**:
- Google 로그인 (Credential Manager API)
- Firebase Auth 연동
- EncryptedSharedPreferences 기반 UID 캐싱
- ShapeFirebaseStore / ShapeRepository (Firestore CRUD)
- Soft Delete (`deletedAt` 필드)
- LWW 충돌 해결 (`updatedAt` 기준)
- 배치 처리 (500개 제한)
- DroneRepository / DroneFirebaseStore
- SketchFirebaseStore / SketchRepository
- RealtimeSyncManager (Firestore SnapshotListener → callbackFlow)
- FCM 토큰 등록

**iOS 참조 파일**:
- `Manager/AuthManager.swift`
- `Firebase/ShapeFirebaseStore.swift`
- `Firebase/ShapeRepository.swift`
- `Manager/RealtimeSyncManager.swift`
- `Manager/ChangeDetectionManager.swift`

**Firestore 스키마 (iOS와 동일)**:
```
users/{uid}/shapes/{shapeId}
users/{uid}/drones/{droneId}
users/{uid}/sketches/{sketchId}
users/{uid}/metadata/{docId}
```

**레거시 필드 호환 필수**:
```kotlin
val createdAt = document.getTimestamp("createdAt")
    ?: document.getTimestamp("startedAt")  // 레거시 폴백
    ?: Timestamp.now()
```

**품질 게이트**:
- [x] LWW 충돌 해결 Unit Test 10가지 케이스 통과
- [x] Soft Delete 필터링 Integration Test 통과
- [ ] 크로스 플랫폼 동기화 테스트 통과 (iOS 생성 → Android 수신)
- [x] 500개 초과 배치 처리 테스트 통과

---

### Phase 3: 저장 목록 탭 (2주) ✅ 완료

**목표**: 도형 목록, 검색, 정렬, 드론 필터

**구현 완료**:
- [x] SavedShapeListScreen (LazyColumn + 섹션별 분류: 활성/시작전/만료)
- [x] 검색 기능 (SearchBar - 제목, 주소, 메모 검색)
- [x] 정렬 (제목순, 생성일순, 시작일순, 종료일순 + 오름/내림차순)
- [x] ShapeDetailSheet (도형 선택 시 상세 하단 시트)
- [x] 도형 셀 UI (SavedShapeListItem - 색상바, 제목, 주소, 날짜범위, 상태배지)
- [x] 탭 간 연동 (편집 버튼 → 지도 탭 이동 + 카메라 포커스)
- [x] SavedListViewModel (검색/정렬/섹션 분리 로직)
- [x] Empty State (도형 없음 / 검색 결과 없음)

**생성된 파일**:
- `feature/saved/SortOption.kt` - 정렬 옵션 열거형
- `feature/saved/SavedListViewModel.kt` - ViewModel
- `feature/saved/SavedShapeListItem.kt` - 리스트 셀 UI
- `feature/saved/SavedListScreen.kt` - 메인 화면 (재작성)

**수정된 파일**:
- `ui/navigation/NavGraph.kt` - 크로스탭 네비게이션 콜백
- `ui/navigation/MainScreen.kt` - pendingFocusShapeId 상태 관리
- `feature/map/MapScreen.kt` - focusShapeId 파라미터 추가

**iOS 참조 파일**:
- `Shape/SavedShapeListView.swift`
- `Shape/SavedShapeViewCell.swift`
- `Shape/View/SavedTableListView.swift`
- `Manager/ShapeSortingManager.swift`

---

### Phase 4: 스케치 기능 (2주) ✅ 완료

**목표**: 지도 위 자유 그리기, Undo/Redo, Firebase 동기화

**구현 완료**:
- [x] SketchViewModel (스케치 모드, 지우개 모드, 색상/두께/투명도 상태 관리)
- [x] 터치 이벤트 처리 (Compose `pointerInput` + `detectDragGestures` 오버레이)
- [x] 좌표 샘플링 (최소 5m 이상, `naverMap.projection.fromScreenLocation`)
- [x] Catmull-Rom 스플라인 보간 (SketchSmoothingAlgorithm, 동적 세그먼트)
- [x] LRU 캐시 (SketchPointsCache, Mutex 스레드 안전, 최대 100개)
- [x] Undo/Redo 스택 (ArrayDeque + UndoAction sealed class)
- [x] 지우개 모드 (BoundingBox 2단계 필터링 + distanceToSegment 30m 허용)
- [x] 스케치 오버레이 (PolylineOverlay, Diff 기반 업데이트, 프리뷰 오버레이)
- [x] SketchToolbar (펜, 지우개, Undo, Redo, 전체삭제 + 배지, 완료)
- [x] 스케치 모드 시 지도 제스처 비활성화 (`setAllGesturesEnabled(false)`)
- [x] 플로팅 버튼 숨김/복원

**생성된 파일**:
- `core/util/SketchSmoothingAlgorithm.kt` - Catmull-Rom 스플라인 보간
- `core/util/SketchPointsCache.kt` - LRU 캐시
- `feature/sketch/SketchViewModel.kt` - ViewModel
- `feature/sketch/SketchOverlayManager.kt` - 오버레이 관리
- `feature/sketch/SketchToolbar.kt` - 툴바 UI

**수정된 파일**:
- `feature/map/MapScreen.kt` - 스케치 터치 인터셉트, SketchViewModel 통합
- `feature/map/component/MapFloatingButtons.kt` - 스케치 모드 진입 버튼

**iOS 참조 파일**:
- `Manager/SketchManager.swift`
- `Manager/SketchSmoothingAlgorithm.swift`
- `Manager/SketchPointsCache.swift`
- `Sketch/View/SketchCanvasView.swift`

**기술 리스크 해결**: Compose `Box` + `pointerInput` 오버레이로 네이버 MapView 터치 충돌 해결. 스케치 모드 시 `setAllGesturesEnabled(false)`.

---

### Phase 5: 설정 탭 + 드론 관리 (2주) ✅ 완료

**목표**: 드론 CRUD, 앱 설정, 알림, 프로필

**구현 완료**:
- [x] SettingsScreen (내 정보, 지도 표시, 앱 정보 섹션)
- [x] DroneListScreen (드론 목록, 빈 상태, FAB)
- [x] DroneEditSheet (이름, 색상 피커, 시리얼번호, 무게, 크기, 메모)
- [x] DroneDetailSheet (상세 정보, 편집/삭제)
- [x] DroneViewModel (CRUD, 중복이름 검사, 미사용 색상 제안)
- [x] 프로필 표시 (로그인 상태 연동)
- [x] 설정 토글 (만료 도형 숨기기, 시작 전 도형 숨기기, 화면 항상 켜기)
- [x] 설정 영속화 (DataStore Preferences)
- [x] SettingsViewModel (DataStore, AuthRepository, 로그아웃/회원탈퇴)
- [x] AnimatedContent 설정↔드론 전환

**생성된 파일**:
- `feature/drone/DroneViewModel.kt` - ViewModel
- `feature/drone/DroneListScreen.kt` - 드론 목록
- `feature/drone/DroneEditSheet.kt` - 생성/편집 시트
- `feature/drone/DroneDetailSheet.kt` - 상세 시트
- `feature/settings/SettingsViewModel.kt` - 설정 ViewModel

**수정된 파일**:
- `feature/settings/SettingsScreen.kt` - 전면 재작성
- `core/data/local/room/dao/ShapeDao.kt` - 드론별 도형 쿼리 추가
- `core/data/repository/ShapeRepository.kt` - 도형 재할당/삭제 메서드 추가

**iOS 참조 파일**:
- `Setting/View/SettingView.swift`
- `Setting/View/DroneListView.swift`
- `Manager/DroneManager.swift`
- `Manager/SettingManager.swift`

---

### Phase 6: VWorld 비행구역 + Kp 지수 (1.5주) ✅ 완료

**목표**: 드론 비행구역 표시, 지자기 폭풍 지수

**구현 완료**:
- [x] VWorldApi (Retrofit, WFS GeoJSON 파싱, @Query 기반)
- [x] VWorldModels (GeoJSONFeatureCollection, GeoJSONFeature, GeoJSONGeometry, DroneZoneFeature)
- [x] VWorldConstants (12개 FlightZoneLayer enum, FlightRestrictionLevel 4단계)
- [x] VWorldRepository (15분 LRU 캐시, 50개 제한, coroutineScope+async 병렬 로드)
- [x] FlightZoneOverlayManager (NMFPolygonOverlay 관리, 레이어 토글, 메모리 최적화)
- [x] FlightZoneLayerSelector (ModalBottomSheet, 체크박스, 전체 선택/해제)
- [x] VWorldZoneDetailSheet (구역 상세, 제한등급 배지)
- [x] FlightZoneCalculator (Ray Casting 점-폴리곤 판정, Haversine 거리 계산)
- [x] Debounce (카메라 이동 500ms 디바운스)
- [x] KpNoaaApi + KpGfzApi (NOAA JSON + GFZ 텍스트 이중 소스)
- [x] KpIndexRepository (30분 캐시, GFZ→NOAA 폴백 체인, 텍스트 파싱)
- [x] KpViewModel (currentKp, kpLevel, forecastData, 5분 자동 새로고침)
- [x] KpForecastScreen (현재 Kp 카드, 레벨 가이드 범례, 24시간 예보 바차트)
- [x] Kp 지수 탭 (하단 네비게이션에 추가)
- [x] 설정 연동 (비행구역 레이어 표시 토글)

**생성된 파일**:
- `core/data/remote/vworld/VWorldApi.kt` - VWorld WFS API Retrofit 인터페이스
- `core/data/remote/vworld/VWorldModels.kt` - GeoJSON 응답 모델
- `core/data/remote/vworld/VWorldConstants.kt` - 12개 레이어 enum + 제한등급
- `core/data/repository/VWorldRepository.kt` - 캐시 + 병렬 로드
- `core/util/FlightZoneCalculator.kt` - Ray Casting + Haversine
- `feature/vworld/FlightZoneOverlayManager.kt` - 오버레이 관리
- `feature/vworld/FlightZoneLayerSelector.kt` - 레이어 선택 UI
- `feature/vworld/VWorldZoneDetailSheet.kt` - 구역 상세 시트
- `core/data/remote/kp/KpApi.kt` - NOAA + GFZ API
- `domain/model/KpIndexModel.kt` - KpIndexData, KpLevel enum
- `core/data/repository/KpIndexRepository.kt` - 이중 소스 + 캐시
- `feature/kp/KpViewModel.kt` - ViewModel
- `feature/kp/KpForecastScreen.kt` - Kp 예보 화면

**수정된 파일**:
- `core/di/NetworkModule.kt` - VWorldApi, KpNoaaApi, KpGfzApi Retrofit, VWorld API 키
- `core/di/RepositoryModule.kt` - VWorldRepository, KpIndexRepository 바인딩
- `ui/navigation/Screen.kt` - Screen.KpForecast 추가
- `ui/navigation/NavGraph.kt` - KpForecast 라우트
- `ui/navigation/MainScreen.kt` - Kp 지수 탭 추가
- `feature/map/MapViewModel.kt` - VWorldRepository, visibleLayers, flightZones
- `feature/map/MapScreen.kt` - FlightZoneOverlayManager, 카메라 리스너
- `feature/map/component/MapFloatingButtons.kt` - 비행구역 레이어 FAB
- `feature/settings/SettingsScreen.kt` - 비행구역 레이어 표시 토글
- `feature/settings/SettingsViewModel.kt` - showFlightZoneLayers DataStore

**iOS 참조 파일**:
- `VWorld/VWorldAPIManager.swift`
- `VWorld/VWorldConstants.swift`
- `VWorld/FlightZoneOverlayManager.swift`
- `VWorld/FlightZoneCalculator.swift`
- `Manager/KPIndexManager.swift`

**안전 기준 (QA 최엄격)**:
- 비행불가→비행가능 오판 (False Positive): **0건 허용**
- 비행가능→비행불가 오판 (False Negative): **0건 허용**

---

### Phase 7: 날씨 기능 (1.5주) ✅ 완료

**목표**: 드론 비행 기상 판단, 돌풍 평가

**구현 완료**:
- [x] WeatherApi (Open-Meteo API, Retrofit, 무료/API 키 불필요)
- [x] WeatherResponse (CurrentWeather, HourlyWeather, DailyWeather, Moshi @JsonClass)
- [x] WeatherData 도메인 모델 (CurrentWeatherData, HourlyWeatherData)
- [x] DroneCategory enum (TOY ≤250g, CLASS4 250g-2kg, CLASS3 2-7kg, CLASS2 7-25kg)
- [x] GustDifferenceCalculator (3축 평가 + 2-out-of-3 투표 + 히스테리시스)
- [x] GustDifferenceLevel enum (SAFE, LOCALIZED_GUST, CAUTION, DANGER + 색상)
- [x] CRICalculator (이중 채널: ΔT + Magnus RH, 풍속 보정)
- [x] WeatherCodeMapper (WMO 코드 → 한국어 설명 + Material Icons)
- [x] WeatherRepository (3분 캐시, API→도메인 변환, CRI/돌풍 계산 포함)
- [x] WeatherViewModel (@HiltViewModel, 3분 자동 새로고침, 위치 기반, 드론 카테고리 선택)
- [x] WeatherForecastScreen (현재 날씨 카드, 드론 카테고리 드롭다운, 돌풍 등급 배너, 2열 상세 그리드, 시간별 예보)
- [x] WeatherOverlayCard (지도 위 소형 카드: 온도, 풍속, 안전등급 점)
- [x] 날씨 카드 탭 → 상세 화면 네비게이션

**생성된 파일**:
- `core/data/remote/weather/WeatherApi.kt` - Open-Meteo Retrofit 인터페이스
- `core/data/remote/weather/WeatherResponse.kt` - API 응답 모델
- `domain/model/WeatherModel.kt` - 도메인 모델
- `core/util/DroneCategory.kt` - 드론 무게 카테고리
- `core/util/GustDifferenceLevel.kt` - 돌풍 위험 등급
- `core/util/GustDifferenceCalculator.kt` - 3축 돌풍 평가 알고리즘
- `core/util/CRICalculator.kt` - 결로 위험 지수 계산
- `core/util/WeatherCodeMapper.kt` - WMO 날씨 코드 매핑
- `core/data/repository/WeatherRepository.kt` - 날씨 데이터 저장소
- `feature/weather/WeatherViewModel.kt` - ViewModel
- `feature/weather/WeatherForecastScreen.kt` - 날씨 상세 화면
- `feature/weather/WeatherOverlayCard.kt` - 지도 오버레이 카드

**수정된 파일**:
- `app/build.gradle.kts` - kotlinx-coroutines-play-services 의존성
- `core/di/NetworkModule.kt` - @Named("weatherRetrofit") Retrofit, WeatherApi
- `core/di/RepositoryModule.kt` - WeatherRepository 바인딩
- `core/di/AppModule.kt` - FusedLocationProviderClient 제공
- `ui/navigation/Screen.kt` - Screen.Weather 추가
- `ui/navigation/NavGraph.kt` - Weather 라우트, onNavigateToWeather
- `feature/map/MapScreen.kt` - WeatherViewModel, WeatherOverlayCard 오버레이

**iOS 참조 파일**:
- `Manager/WeatherManager.swift`
- `Setting/View/WeatherForecastView.swift`

---

### Phase 8: QA + 출시 준비 (2~3주) ✅ 완료

**목표**: 프로덕션 품질 확보, 릴리스 빌드 최적화

**Phase 8-A 구현 (즉시 수정)**:
- [x] Firebase 동기화 호출 연결 (AuthViewModel → 3개 Repository performFullSync)
- [x] 비행구역 레이어 설정↔지도 연동 (DataStore → MapViewModel → MapScreen)
- [x] "화면 항상 켜기" 실제 구현 (DisposableEffect + view.keepScreenOn)
- [x] WEB_CLIENT_ID를 BuildConfig로 이동 (보안 + 설정 편의)

**Phase 8-B 구현 (Firebase 실시간 동기화)**:
- [x] RealtimeSyncManager 완성 (Shape/Drone/Sketch 3개 Repository 동기화)
- [x] 듀얼 Firestore 리스너 (metadata/server + metadata/sketchServer)
- [x] 2초 디바운싱, 자신의 변경 스킵, 최대 3회 재시도 (5/10/15초)
- [x] 로그인 시 리스너 시작, 로그아웃 시 중단

**Phase 8-C 구현 (알림 기능)**:
- [x] FCM Service (토큰 관리, Firestore 디바이스 등록, 알림 표시)
- [x] NotificationScheduler (AlarmManager 기반 정확한 알림)
- [x] 일출/일몰 알림 (30분 전, 10분 전)
- [x] 종료일 알림 (7일 전)
- [x] 설정 화면 알림 섹션 (3개 토글)
- [x] 알림 채널 생성 (Android 8+)

**Phase 8-D 구현 (부가 기능)**:
- [x] KP 27일 장기예보 (NOAA 27-day-outlook.txt 파싱, UI 표시)
- [x] 좌표 형식 파서 (DMS, 십진수, Geo URI 변환)
- [x] Room DB 마이그레이션 인프라 (Migration 1→2, 확장 가능한 구조)

**기존 Phase 8 구현**:
- [x] Firebase Crashlytics 연동
- [x] ProGuard/R8 릴리스 빌드 (`isMinifyEnabled = true`, `isShrinkResources = true`)
- [x] 포괄적 ProGuard 규칙 (Hilt, Room, Retrofit, Moshi, Firebase, Naver Maps, Coroutines)
- [x] Backup & Data Extraction Rules
- [x] `collectAsState()` → `collectAsStateWithLifecycle()` 전체 통일
- [x] strings.xml 리소스 추출 (120+ 한국어 문자열)
- [x] contentDescription 접근성 개선
- [x] 전체 E2E 에뮬레이터 테스트

**APK 크기 최적화**:
- Debug: 171MB → Release: 105MB (38.6% 감소)

**향후 개선 사항** (출시 후):
- 크로스 플랫폼 호환성 테스트 (iOS ↔ Android Firestore 동기화)
- 앱 서명 설정 (Release signing config)
- Play Store 메타데이터 (스크린샷, 설명문)
- 단위 테스트 / UI 테스트 작성
- LeakCanary 메모리 누수 검사

---

## 5. 단계 간 의존성

```
Phase 0 (기반 구조) ──────────────────────── 필수 선행
    ↓
Phase 1 (지도 + 도형) ────────────────────── 앱의 핵심 가치
    ↓
Phase 2 (Firebase 인증 + 동기화) ──────────── 로컬 먼저 → 클라우드
    ↓
Phase 3 (저장 목록) ──┬── Phase 4 (스케치) ── 병렬 가능
    ↓                 │
Phase 5 (설정 탭) ────┘
    ↓
Phase 6 (VWorld) ─────┬── Phase 7 (날씨) ─── 병렬 가능
    ↓                 │
Phase 8 (QA + 출시) ──┘
```

---

## 6. 기능 우선순위 매트릭스

```
높은 가치
    │
    │  [Quick Wins - 즉시]          [전략적 투자 - 핵심]
    │  탭 네비게이션                 VWorld 비행구역 레이어
    │  플로팅 버튼 메뉴              Firebase 실시간 동기화
    │  도형 목록/검색/정렬           도형 오버레이 렌더링
    │  도형 상세 정보                Firebase 인증
    │  비행 기간 관리                LWW 충돌 해결
    │  드론 CRUD
    │
    ├──────────────────────────────────────────────────── 복잡도
    │
    │  [보류 - 낮은 ROI]            [신중한 접근 - 선택적]
    │  다른 지도 앱으로 열기         스케치 모드
    │  Kp 지수                      날씨 정보 (API 대체)
    │  다국어                        FCM 푸시 알림
    │                                회원탈퇴 익명화
낮은 가치
```

---

## 7. MVP 정의 (첫 릴리즈 필수 기능)

| 묶음 | 기능 |
|---|---|
| **M1** | 탭 네비게이션, 플로팅 버튼, 현재 위치 |
| **M2** | Circle 생성 + 지도 오버레이 |
| **M3** | 도형 로컬 저장 (Room, Soft Delete) |
| **M4** | 도형 목록, 검색, 정렬, 상세 정보 |
| **M5** | 드론 CRUD + 도형-드론 연결 |
| **M6** | VWorld 핵심 3개 레이어 (비행금지, 임시비행금지, 관제권) |
| **M7** | Google 로그인 + Firestore 동기화 + LWW |

**MVP 제외 (이후)**: 스케치, 날씨, Kp 지수, 알림, FCM, 다국어

---

## 8. 리스크 분석

### 리스크 1: Apple 로그인 → Google 로그인 (높음)
- **문제**: iOS Apple 로그인 사용자가 Android에서 같은 계정 접근 불가
- **대응**: Firebase Auth `linkWithCredential`로 계정 연결. 이메일 기반 연결 플로우 설계.

### 리스크 2: WeatherKit 대체 (중간)
- **문제**: Apple 전용 API, Android 사용 불가
- **대응**: Open-Meteo API (무료, API 키 불필요, 10,000 req/day)
- **영향**: 비즈니스 로직(돌풍 알고리즘)은 유지, API 호출부만 교체

### 리스크 3: 스케치 터치 이벤트 (중간)
- **문제**: 네이버 MapView(View)와 Compose 터치 충돌
- **대응**: 스케치 모드 시 MapView 터치 비활성화 + Compose `pointerInput` 오버레이

### 리스크 4: Firestore Timestamp 호환 (중간)
- **문제**: iOS Date epoch(2001) vs Unix epoch(1970) 혼재 가능성
- **대응**: Firestore Timestamp으로 저장된 경우 자동 호환. 레거시 Double 필드 방어 코드 필요.

### 리스크 5: 정확한 알림 스케줄링 (중간)
- **문제**: Android 12+ `SCHEDULE_EXACT_ALARM` 권한 + Doze Mode
- **대응**: WorkManager + 정확한 알림은 `AlarmManager.setExactAndAllowWhileIdle()`

---

## 9. 의존성 목록

### gradle/libs.versions.toml 추가

```toml
[versions]
hilt = "2.51.1"
room = "2.6.1"
datastore = "1.1.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
firebaseBom = "33.1.0"
navigationCompose = "2.8.0"
moshi = "1.15.1"

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-moshi = { group = "com.squareup.retrofit2", name = "converter-moshi", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
moshi = { group = "com.squareup.moshi", name = "moshi", version.ref = "moshi" }
moshi-kotlin = { group = "com.squareup.moshi", name = "moshi-kotlin", version.ref = "moshi" }
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore-ktx" }
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging-ktx" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx" }
google-auth = { group = "com.google.android.gms", name = "play-services-auth", version = "21.2.0" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
security-crypto = { group = "androidx.security", name = "security-crypto", version = "1.1.0" }
work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version = "2.9.1" }

[plugins]
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.28" }
```

---

## 10. iOS Manager → Android 매핑

| iOS Manager | Android 구현 | 패턴 |
|---|---|---|
| AuthManager | AuthViewModel + AuthRepository | ViewModel + Hilt |
| DroneManager | DroneViewModel + DroneRepository | ViewModel + Room + Firestore |
| SketchManager | SketchViewModel | ViewModel + StateFlow |
| SettingManager | SettingsViewModel + AppDataStore | DataStore |
| LocationManager | FusedLocationProviderClient | Android API |
| WeatherManager | WeatherRepository | Retrofit + Coroutine |
| KPIndexManager | KPIndexRepository | Retrofit + Room Cache |
| PushNotificationManager | FcmService | FirebaseMessagingService |
| RealtimeSyncManager | RealtimeSyncRepository | Firestore Listener + callbackFlow |
| ChangeDetectionManager | SnapshotListener → Flow | callbackFlow + debounce |
| CoordinateManager | Coordinate data class | LatLng 확장 함수 |
| ColorManager | PaletteColor enum | Compose Color |
| MigrationManager | MigrationManager (Object) | Room Migration |
| ShapeSortingManager | ShapeViewModel.sortedShapes | Flow.map { sorted } |
| SketchPointsCache | LruCache<UUID, List<Coordinate>> | Android LruCache |
| KeychainHelper | EncryptedPrefsHelper | EncryptedSharedPreferences |
| DistanceCalculator | DistanceCalculator (Object) | Location.distanceBetween() |

---

## 11. 크로스 플랫폼 호환성 테스트

iOS와 Android가 **동일 Firestore**를 공유하므로 가장 중요한 QA 영역.

### 필수 테스트 시나리오

| ID | 시나리오 | 기준 |
|---|---|---|
| TC-CROSS-01 | iOS 도형 생성 → 2초 이내 Android 수신 | 데이터 완전 일치 |
| TC-CROSS-02 | Android Soft Delete → iOS 목록 제거 | deletedAt 필터 동작 |
| TC-CROSS-03 | 동시 수정 LWW → 최신 updatedAt 승리 | 양쪽 동일 결과 |
| TC-CROSS-04 | 도형 100개 동기화 → Android 5초 이내 | 데이터 무결성 |

### Firestore 레거시 필드 호환

최신 상세 계약은 `FIRESTORE_CONTRACT.md`를 기준으로 한다. 핵심은 쓰기는 표준 iOS wire format으로 엄격하게, 읽기는 Android 초기 레거시 값까지 관대하게 처리하는 것이다.

```
startedAt → createdAt + flightStartDate (폴백)
expireDate → flightEndDate (폴백)
shapeType: circle/rectangle/polygon/polyline 로 쓰기, CIRCLE 같은 Android 레거시는 대소문자 무시 읽기
```

---

## 12. 성능 기준

| 항목 | 목표값 |
|---|---|
| 지도 초기 로딩 | 3초 이내 |
| 오버레이 100개 렌더링 | 60fps (16ms) |
| 오버레이 500개 렌더링 | 30fps (33ms) |
| 도형 100개 초기 로드 | 5초 이내 |
| 단일 도형 저장 왕복 | 2초 이내 |
| 앱 기본 메모리 | 150MB 이하 |
| 도형 500개 로드 후 | 200MB 이하 |

---

## 13. 일정 요약

| Phase | 내용 | 기간 | 누적 |
|---|---|---|---|
| 0 | 기반 아키텍처 | 2주 | 2주 |
| 1 | 지도 + 도형 | 3.5주 | 5.5주 |
| 2 | Firebase 인증/동기화 | 4주 | 9.5주 |
| 3 | 저장 목록 탭 | 2주 | 11.5주 |
| 4 | 스케치 (병렬 가능) | 2주 | 11.5주 |
| 5 | 설정 탭 | 2주 | 13.5주 |
| 6 | VWorld + Kp | 1.5주 | 15주 |
| 7 | 날씨 (병렬 가능) | 1.5주 | 15주 |
| 8 | QA + 출시 | 3주 | 18주 |

**총 예상 기간: 약 18주 (4.5개월)** (단독 개발 기준)

---

## 14. 테스트 도구

| 레이어 | 도구 |
|---|---|
| Unit Test | JUnit5 + MockK |
| Coroutine Test | kotlinx-coroutines-test |
| Compose UI Test | Compose UI Test (공식) |
| Integration | Firebase Emulator Suite |
| 성능 | Macrobenchmark |
| 메모리 누수 | LeakCanary |
| 크래시 | Firebase Crashlytics |
| Coverage | Kover (JetBrains) |

---

## 15. 공통 컴포넌트 (재사용 Composable)

### 레이아웃
- `MainTabScaffold` - BottomNavigation + ModalBottomSheet 관리
- `DronePassTopAppBar` - 공통 TopAppBar

### 지도
- `NaverMapComposable` - AndroidView 래핑 + 생명주기
- `FloatingActionButtonsOverlay` - 플로팅 버튼 그룹
- `DroneSelectionDropdown` - 드론 멀티 선택

### 공통 UI
- `SearchBar` - 검색 입력
- `ToastMessage` - 토스트 (Snackbar)
- `ColorDot` - 색상 원형 표시
- `SectionHeader` - 섹션 헤더
- `ClickableListItem` - 설정 목록 아이템
- `SwitchListItem` - 토글 아이템
- `DetailInfoRow` - 상세 정보 행
- `CopyableText` - 복사 가능 텍스트
- `DateTimePickerField` - 날짜/시간 선택

### 스케치
- `SketchCanvas` - 자유 그리기
- `SketchToolbar` - 스케치 도구바
- `HueGradientSlider` - 색상 슬라이더

---

## 16. 즉시 실행할 첫 번째 액션 (Phase 0 시작)

1. `build.gradle.kts`에 Hilt, Room, Navigation, DataStore 의존성 추가
2. `DronePassApplication.kt` 생성 (`@HiltAndroidApp`)
3. `ShapeModel.kt`, `DroneModel.kt`, `SketchModel.kt` data class 포팅
4. Room DB 스키마 정의
5. BottomNavigationBar 3탭 구현
6. NavGraph 기본 구조 설정

---

*이 문서는 마이그레이션 진행 중 지속적으로 업데이트됩니다.*

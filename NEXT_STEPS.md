# 작업 이어가기 핸드오프 노트

> 마지막 업데이트: 2026-05-19 (Cross-Platform 로그인 Phase A 완료)
> 다음 세션에서 이 문서 + `REFACTORING_PLAN.md` 를 함께 읽으면 즉시 이어서 진행할 수 있다.

---

## 1. 현재 위치 한눈에 보기

| 항목 | 값 |
|---|---|
| **브랜치** | `fix/critical-pri0-fixes` |
| **마지막 커밋** | `3d44d09` (feat: Cross-Platform 로그인 Phase A — Android Apple Sign-In) |
| **워킹 트리** | clean (변경 없음 — REFACTORING_PLAN/NEXT_STEPS 갱신 후) |
| **원격 동기화** | `origin/fix/critical-pri0-fixes` 보다 17 커밋 ahead (push 미수행) |
| **누적 처리** | **124건 중 124건 완료 (100%)** ✅ |
| **잔여** | 없음 — Phase 1/2/3 모두 종료 |

---

## 2. 완료된 작업 누적 (19 커밋)

`git log --oneline fix/critical-pri0-fixes ^main` 결과:

```
4f890ae fix(ui): Phase 3.5 Medium 15건 - 다국어/SharedFlow/sp/포맷 정비
266579a fix(map): Phase 3.4 Medium 10건 - 오버레이 성능/density/zIndex/debounce 정비
a82f4db fix(infra): Phase 3.3 Medium 11건 - DI/테마/매니페스트 정비
71a8b6a fix(data): Phase 3.2 Medium 8건 - 캐시 동시성/sync 헬퍼/배치 insert
f10dde9 fix(algorithm): Phase 3.1 Medium 11건 - 코드 품질/일관성 정비
e2de3d1 docs: REFACTORING_PLAN.md - Phase 2 완료 표기 (High 45건 모두 [x])
43c5d5c fix(ui): Phase 2.5 High 13건 - Kp/Weather 라이프사이클/Sections/메모이즈/검증
1d3a81b fix(map): Phase 2.4 High 9건 - Overlay Diff/메모이즈/Mutex/Ticker/replay
994e500 fix(infra): Phase 2.3 High 6건 - 위치 캐시/알림 채널/Nav 정리/Hilt/로깅/Naver 분리
efa98fc fix(data): Phase 2.2 High 9건 - Repository LWW 정교화 + FirebaseStore Result + Sync 정리
ea7b74d fix(algorithm): Phase 2.1 High 8건 - Smoothing/Parser/Gust/Geocoding/NOTAM 정합
db4b805 docs: REFACTORING_PLAN.md - Phase 1 완료 표기 (Critical 19건 모두 [x])
a755801 fix(ui): Phase 1.5 Critical 5건 - LoginScreen/AuthVM/SavedList/WebView/SettingsVM 정리
b2638e0 fix(map): Phase 1.4 Critical 4건 - 위치추적 누수/회전 카메라 손실/리스너 해제/Overlay 누수
3d07521 fix(infra): Phase 1.3 Critical 6건 - 백업/권한/테마/서명/queries
d77b413 fix(security): Phase 1.2 A-C1 - EncryptedPrefs MasterKey.Builder + 손상 폴백
967bab8 fix(algorithm): Phase 1.1 Critical 3건 - Haversine WGS-84 통일 + CRI NaN 가드 + SketchPointsCache TOCTOU
efed981 docs: REFACTORING_PLAN.md 추가 - 잔여 119건 단계별 체크리스트
40dafb7 fix(critical): Pri 0 5건 - 동기화 파이프라인 복구 + 비행구역 안전 기준 정합
```

### 2.1 Pri 0 (가장 시급한 5건) — `40dafb7`

| # | 핵심 |
|---|---|
| 1 | Migration 1→2 SQL 을 v2 entity 스키마와 정합화 (DronePassDatabase.kt) |
| 2 | RealtimeSyncManager `getLong("lastModified")` → `getTimestamp(...).time` (레거시 폴백 유지) |
| 3 | SketchFirebaseStore 메타데이터 경로 `metadata/server` → `metadata/sketchServer` |
| 4 | Repository 3종에 Firebase 즉시 푸시 헬퍼 (`syncXToFirebase`) 추가 |
| 5 | FlightZoneCalculator Ray Casting `latI/lonI` 변수명 + iOS 원본 정합 |

### 2.2 Phase 1 — Critical (19건) — 5개 영역 커밋

- **Phase 1.1 알고리즘 (3건)** `967bab8`: Haversine WGS-84 통일 / CRI NaN 가드 / SketchPointsCache TOCTOU
- **Phase 1.2 데이터 (1건)** `d77b413`: EncryptedPrefsHelper MasterKey.Builder + 손상 폴백 + FcmService 통합
- **Phase 1.3 인프라 (6건)** `3d07521`: 백업규칙 파일명 정정 / POST_NOTIFICATIONS / SCHEDULE_EXACT_ALARM / Material 테마 / Release signingConfig / `<queries>`
- **Phase 1.4 지도 (4건)** `b2638e0`: AndroidView update 람다 위치추적 분리 / configChanges / 리스너 해제 / OverlayManager 3종 detach()
- **Phase 1.5 UI (5건)** `a755801`: LoginScreen LaunchedEffect 통합 / SettingsViewModel.signOut FCM+Sync / WebView 화이트리스트 / AuthViewModel init 순서 / SavedList key prefix

### 2.3 Phase 2 — High (45건) — 5개 영역 커밋

- **Phase 2.1 알고리즘 (8건)** `ea7b74d`: Smoothing 가드/Catmull-Rom 정합/DMS 분초/Decimal anchor/Gust NaN/Geocoding nullable/Locale.ROOT/NOTAM sliding window
- **Phase 2.2 데이터 (9건)** `efa98fc`: Repository syncFromFirebase LWW / performFullSync 로컬-우세 업로드 / FirebaseStore Result / AtomicBoolean / retry 가드 / 데드 콜백 제거 / Sketch 500ms 디바운싱 / baseCoordinate 폴백 제거 / EntityMapper 폴백
- **Phase 2.3 인프라 (6건)** `994e500`: BootReceiver DataStore 위치 캐시 / NotificationChannel IMPORTANCE_HIGH 분리 / NavGraph dead route 정리 / FcmService @AndroidEntryPoint / HTTP 로깅 DEBUG 분기 / Naver OkHttp+Retrofit @Named 분리
- **Phase 2.4 지도 (9건)** `1d3a81b`: ShapeOverlayManager Map+ShapeKey Diff / FlightZoneOverlayManager zoneId Diff / setOutOfBoundsVisibility / LaunchedEffect key 좁히기 / continueDrawing O(n²)→O(n) / 지우개 Mutex / cosLat 보정 / filteredShapes 60초 ticker / cameraEvent replay=1
- **Phase 2.5 UI (13건)** `43c5d5c`: Kp/Weather start/stopAutoRefresh / SavedListVM Sections+Default / ShapeEdit shape.id key / DroneEditSheet 메모이즈 / suggestNextColor 명시 인자 / DroneList 메모이즈 / DroneDetailSheet shapeCount nullable / DatePicker 주석 / Save 비활성 / droneId→name Map / Kp Refresh 비활성 / 스와이프 확인 다이얼로그

### 2.4 Phase 3 — Medium (55건) — 5개 영역 커밋

- **Phase 3.1 알고리즘 (11건)** `f10dde9`: FlightZoneCalc KM 상수 / CRI windFactor / SketchSmoothing segments / WMO 4·5·10 / AltitudeFormatter Regex 캐시 / ShapeModel effectiveColor 제거 / ShapeType 확장 가이드 / PaletteColor parseColor 폴백 / VWorldModels typed accessor / SketchPointsCache LRU 코멘트 / KpIndexModel.fromKp NaN 가드
- **Phase 3.2 데이터 (8건)** `71a8b6a`: SyncMerge.kt 신규 (mergeLWW/filterServerNewer) → Shape/Drone/Sketch Repo 정리 / KpIndex·Weather·VWorld 캐시 동시성(@Volatile+Mutex) / SketchFirebaseStore unchecked cast 단계 검증 / Coordinate StringBuilder JSON+NaN 안전화 / DAO 배치 insert / EntityMapper 폴백
- **Phase 3.3 인프라 (11건)** `a82f4db`: RepositoryModule @Provides 제거 → @Inject constructor 자동 / Retrofit 4종 @Named 분리 (vWorld/kpNoaa/kpNoaa27Day/kpGfz) / Naver timeout 코멘트 / Color Brand/Neutral/Accent 팔레트 + 기존 토큰 @Deprecated alias / Type.kt 8종 토큰 / Theme dynamicColor=false 정책 / MIGRATION 정리 / appcompat 직접 의존 제거 / Compose BOM 업그레이드 코멘트 / BootReceiver category.DEFAULT / LocalLifecycleOwner deprecation 해결
- **Phase 3.4 지도/오버레이 (10건)** `266579a`: OverlayManager 의도 코멘트 / SketchOverlayManager density 주입 + Catmull-Rom 병렬 + keepScreenOn 양방향 / FlightZoneOverlayManager BASE_Z_INDEX 10 / MapScreen SideEffect 콜백 / 향후 분리 코멘트 / MapFloatingButtons format 메모이즈+Locale.ROOT / SketchToolbar slider SSOT / MapViewModel SharedFlow.debounce+distinctUntilChanged
- **Phase 3.5 UI (15건)** `4f890ae`: SortOption @StringRes / SimpleDateFormat remember / AuthVM syncMessage SharedFlow / deleteAccount fail-fast / DroneVM 트랜잭션 코멘트 / dismissShapeDetail 코멘트 / ShapeDetailSheet hide await / SearchAddressSheet 검토 / WeatherVM error 다국어 코멘트 / Locale.ROOT 일괄 / sp.toPx (SunTimeline/WeatherCharts) / KpCharts density 잔재 제거 / maxSheetHeight remember / SettingsSubScreen 코멘트 / DroneEditSheet 길이 컷

### 2.5 신규 파일

| 파일 | 용도 |
|---|---|
| `REFACTORING_PLAN.md` | 124건 전체 체크리스트 (Phase 1/2/3 모두 [x]) |
| `app/src/main/java/.../core/data/UserLocationKeys.kt` | BootReceiver 위치 캐시 공유 키 |
| `app/src/main/java/.../core/data/sync/SyncMerge.kt` | mergeLWW / filterServerNewer 헬퍼 (Phase 3.2) |
| `app/src/main/java/.../feature/map/MapScreenLayers.kt` | MapScreen 자식 Composable 4종 (B-M6 후속) |
| `app/src/main/java/.../feature/settings/NotificationPermissionRequest.kt` | 알림 권한 안내 카드 |
| `app/src/main/res/values-night/themes.xml` | 다크 모드 테마 |
| `keystore.properties.example` | Release 서명 샘플 |
| `app/src/test/java/.../core/util/FlightZoneCalculatorTest.kt` | Ray Casting + Haversine 테스트 (14건) |
| `app/src/test/java/.../core/util/CRICalculatorTest.kt` | Magnus NaN 가드 테스트 (8건) |
| `app/src/test/java/.../core/util/CoordinateParserTest.kt` | DMS/Decimal anchor 테스트 (12건) |

**총 단위 테스트 누계: 34건 (모두 PASS)**

### 2.5 검증 명령

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:assembleDebug         # BUILD SUCCESSFUL
./gradlew :app:testDebugUnitTest     # BUILD SUCCESSFUL
```

---

## 3. 후속 작업 진행 상황

Phase 1/2/3 (124건) + 후속 PR 4건 + Cross-Platform 로그인 Phase A 완료.

### 3.1 완료된 후속 작업

| 작업 | 상태 | 메모 |
|---|---|---|
| Phase A: Android Apple Sign-In | ✅ 완료 | Firebase OAuthProvider("apple.com"). iOS 와 동일 Apple ID → 동일 UID 자동 호환. Firebase Console 설정은 사용자 작업. |

### 3.2 Phase B (iOS Google Sign-In) — 사용자 결정 시 진행

iOS 측 변경 (다른 저장소): `/Users/david/Development/Swift/myProjects/DronePass`
- GoogleSignIn SPM 추가
- GoogleLoginManager.swift 신규
- LoginView 에 Google 버튼

### 3.3 추가 후보 (필요 시 진행)

1. **운영 작업**: gh CLI 설치 → push → PR 생성 → 머지
2. **회귀 테스트 사이클**: 실기기 핵심 시나리오 5가지 검증 (지도/도형 CRUD/스케치/로그인/설정)
3. **2단계 업그레이드**:
   - Compose BOM 2026.05.00 (Material3 1.4.x)
   - 통합 테스트(Espresso/Compose UI Test)
   - LeakCanary 도입
   - Play Store 등록 준비 (서명 키, Play App Signing, Release 빌드)
   - Account Linking UI (한 계정에 Apple + Google 연결)

> `REFACTORING_PLAN.md` 의 `## 4. Phase 3 — Medium (55건)` 섹션 참조 (모두 [x] 처리됨).

---

## 4. 후속 PR (모두 완료)

| 항목 | 상태 | 메모 |
|---|---|---|
| **B-M6** MapScreen 25+ collect → 4종 자식 Composable 분리 | ✅ 완료 | `MapScreenLayers.kt` 신규 (MapOverlayEffects/MapFloatingControls/MapSketchInput/MapBottomSheets). 본체 728→392줄. 자식별 recomposition 범위 격리 |
| **D-M9** WeatherViewModel/KpViewModel error sealed class + @StringRes | ✅ 완료 | `sealed class WeatherError`, `sealed class KpError` 도입. UI 가 `stringResource(error.messageRes)` 변환 |
| **D-M14** SettingsSubScreen sealed class 통합 | ✅ 완료 | Terms/Privacy/LocationTerms 3개 분기 → `WebDoc(titleRes, url)` 1개로 통합 |
| **E-M9** Compose BOM 2024.09.00 → 2025.06.01 | ✅ 완료 | Material3 1.3.x 유지 + Kotlin 2.0.21 호환. 2026.05.00 (Material3 1.4.x) 업그레이드는 별도 PR |
| Phase 2.3 deprecated 경고 (해결됨) | ✅ | `NotificationPermissionRequest.kt:61` LocalLifecycleOwner — Phase 3.3 에서 lifecycle.compose 패키지로 이전 완료 |

---

## 5. 운영 작업 (사용자 직접 수행 필요)

이 항목들은 자동화 불가 — 다음 세션에서도 사용자 개입 필요.

### 5.1 Git push + PR 생성 (인증 미설정으로 차단됨)

옵션 A: gh CLI
```bash
brew install gh
gh auth login
git push -u origin fix/critical-pri0-fixes
gh pr create --base main --head fix/critical-pri0-fixes \
  --title "fix: 코드 종합 점검 - Pri 0 + Phase 1 + Phase 2 (69건 처리)" \
  --body "REFACTORING_PLAN.md 참조. 124건 중 69건(55.6%) 처리 완료."
```

옵션 B: SSH 변경
```bash
git remote set-url origin git@github.com:JuseongMoon/DronePass_forAndroid.git
git push -u origin fix/critical-pri0-fixes
# GitHub 웹에서 PR 생성
```

### 5.2 Release 서명 설정 (Play Store 출시 전 필수)

`keystore.properties.example` 참조하여:
```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 \
  -validity 10000 -alias dronepass
cp keystore.properties.example keystore.properties
# keystore.properties 에 실제 비밀번호 입력
```

### 5.3 Firebase 콘솔 (이미 설정됨, 변경 시에만 검토)

현재 `applicationIdSuffix` 사용 안 함 (Phase 1.3 결정). Firebase 패키지 분리가 필요해지면 Firebase 콘솔에 별도 앱 등록 필요.

---

## 6. 다음 세션 시작 — 명령 예시

가장 좋은 시작 문구 (복사해서 붙여넣으면 즉시 이어서 진행):

### 6.1 push/PR 생성

```
NEXT_STEPS.md 5.1 따라 gh CLI 설치하고 push + PR 생성 도와줘
```

### 6.2 회귀 테스트 시나리오

```
NEXT_STEPS.md 의 핵심 회귀 시나리오 5가지(지도/도형 CRUD/스케치/로그인/설정)를 함께 점검해줘
```

### 6.3 별도 PR 진행 (4. 미완 항목)

```
NEXT_STEPS.md 4. 의 B-M6 MapScreen 4종 자식 Composable 분리 진행해줘
```
또는
```
WeatherViewModel/KpViewModel error 를 sealed class + @StringRes 로 정비해줘
```

---

## 7. 환경 정보 (다음 세션에서도 동일)

### 7.1 빌드 환경

```bash
# Java (Android Studio JBR — 시스템 Java 미설치이므로 매 세션 export 필요)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# 작업 디렉토리
cd /Users/david/Development/Android/Projects/DronePass

# 검증 명령
./gradlew :app:assembleDebug                                                    # 컴파일+APK
./gradlew :app:testDebugUnitTest                                                # 단위 테스트
./gradlew :app:testDebugUnitTest --tests "*FlightZoneCalculatorTest"            # 특정 테스트
```

### 7.2 주요 경로

| 종류 | 경로 |
|---|---|
| 프로젝트 루트 | `/Users/david/Development/Android/Projects/DronePass/` |
| 코드 베이스 | `app/src/main/java/com/ScienceFiction/DronePassAndroid/` |
| 단위 테스트 | `app/src/test/java/com/ScienceFiction/DronePassAndroid/` |
| Room 스키마 | `app/schemas/com.ScienceFiction.DronePassAndroid.core.data.local.room.DronePassDatabase/` |
| iOS 원본 (참조) | `/Users/david/Development/Swift/myProjects/DronePass/DronePass/` |

### 7.3 빌드 정보

- Min SDK 28, Target SDK 36, Compile SDK 36
- Kotlin 2.0.21, AGP 8.13.0, Java 11
- Compose BOM 2024.09.00, Hilt 2.51.1, Room 2.6.1
- Firebase BoM 33.1.0, Naver Maps SDK 3.23.1

---

## 8. 참고 문서

| 문서 | 역할 |
|---|---|
| `REFACTORING_PLAN.md` | 124건 전체 체크리스트 (정적 명세) — **다음 세션에서도 이 파일이 작업 단위 정의의 단일 진실 공급원** |
| `NEXT_STEPS.md` (현 문서) | 진행 상태 + 핸드오프 (동적 진행 노트) |
| `CLAUDE.md` | 프로젝트 가이드 (코딩 규칙, 빌드 설정) |
| `MIGRATION_PLAN.md` | iOS → Android 마이그레이션 원본 계획서 (Phase 0~8) |

---

## 9. 작업 진행 시 유의사항

### 9.1 Phase 3 작업 중 주의

- Phase 3 는 대부분 코드 품질/일관성 작업 → **기능 변경 최소화**
- 큰 리팩토링(예: Repository 3종 추상화 SyncableRepository<T>) 은 한 커밋에 영향 큼 → 영역 단위로 묶되 빌드 검증 빈번히
- Hilt RepositoryModule 의 중복 @Provides 제거(E-M1)는 호출 그래프 모두 검증 필요
- AppCompat 의존성 제거(E-M8) 는 Phase 1.3 의 Material 테마 전환과 연계 — 호환성 확인 후 점진 제거

### 9.2 커밋 전 필수 확인

1. `./gradlew :app:assembleDebug` BUILD SUCCESSFUL
2. 단위 테스트가 영향받는 영역이면 `./gradlew :app:testDebugUnitTest` 통과
3. 커밋 메시지에 처리한 Medium 항목(예: `A-M1`, `B-M5`) 모두 명시
4. REFACTORING_PLAN.md 의 해당 체크박스 `[x]` 처리 (sed 일괄)

### 9.3 sed 일괄 체크 패턴

Phase 3 종료 시 사용:
```bash
sed -i '' -E 's/- \[ \] \*\*(A-M[0-9]+|B-M[0-9]+|C-M[0-9]+|D-M[0-9]+|E-M[0-9]+):/- [x] **\1:/g' REFACTORING_PLAN.md
grep -c "^- \[x\]" REFACTORING_PLAN.md  # 64 → 119 가 되어야 함 (Phase 3 = 55 추가)
grep -c "^- \[ \]" REFACTORING_PLAN.md  # 55 → 0
```

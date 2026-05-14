# 작업 이어가기 핸드오프 노트

> 마지막 업데이트: 2026-05-14
> 다음 세션에서 이 문서 + `REFACTORING_PLAN.md` 를 함께 읽으면 즉시 이어서 진행할 수 있다.

---

## 1. 현재 위치 한눈에 보기

| 항목 | 값 |
|---|---|
| **브랜치** | `fix/critical-pri0-fixes` |
| **마지막 커밋** | `e2de3d1` (Phase 2 완료 표기) |
| **워킹 트리** | clean (변경 없음) |
| **원격 동기화** | `origin/fix/critical-pri0-fixes` 보다 6 커밋 ahead (push 미수행) |
| **누적 처리** | **124건 중 69건 완료 (55.6%)** |
| **잔여** | Phase 3 Medium **55건** |

---

## 2. 완료된 작업 누적 (14 커밋)

`git log --oneline fix/critical-pri0-fixes ^main` 결과:

```
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

### 2.4 신규 파일

| 파일 | 용도 |
|---|---|
| `REFACTORING_PLAN.md` | 124건 전체 체크리스트 (Phase 1/2 모두 [x]) |
| `app/src/main/java/.../core/data/UserLocationKeys.kt` | BootReceiver 위치 캐시 공유 키 |
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

## 3. 다음 진행할 Phase 3 — Medium (55건)

> `REFACTORING_PLAN.md` 의 `## 4. Phase 3 — Medium (55건)` 섹션 참조.

### 3.1 영역별 분포

| 영역 | 항목 수 | 대표 작업 |
|---|---:|---|
| **A. 데이터** | 8 | Repository 3종 추상화(SyncableRepository<T>) / Cache 동시성 / Coordinate JSON 성능 / DAO 배치 |
| **B. 지도/오버레이** | 10 | OverlayManager ViewModel scope 이전 / dpToPx displayMetrics / zIndex 우선순위 / SketchToolbar SSOT / Catmull-Rom 병렬화 / MapScreen 25+ collect 분할 |
| **C. 알고리즘/모델** | 11 | FlightZoneCalculator latI/lonI 통일 정리 / CRI windFactor 검토 / WMO 누락 코드 / AltitudeFormatter Regex 캐시 / ShapeType 확장 / KpIndexModel.fromKp 경계 |
| **D. UI** | 15 | SortOption.labelRes / 날짜 포맷 다국어 / SharedFlow 이벤트 / deleteAccount 트랜잭션 / atomic update 일관성 / 시트 dismiss 후 전환 / 에러 메시지 stringResource / Locale.ROOT String.format / sp 단위 / 등 |
| **E. 인프라** | 11 | Hilt RepositoryModule 중복 @Provides 제거 / Retrofit @Named 명시 / Naver Retrofit timeout / Color/Type 브랜드 팔레트 / Theme dynamicColor 정책 / appcompat 의존성 제거 / Compose BOM 업그레이드 / BootReceiver category.DEFAULT |

### 3.2 권장 진행 방식 (Phase 1/2 와 동일 패턴)

5개 영역별 5개 커밋 + 종료 검증 + REFACTORING_PLAN.md 체크박스 일괄 처리.

```
Phase 3.1 알고리즘/모델 (C-M1~M11)
Phase 3.2 데이터 (A-M1~M8)
Phase 3.3 인프라 (E-M1~M11)
Phase 3.4 지도/오버레이 (B-M1~M10)
Phase 3.5 UI (D-M1~M15)
Phase 3 종료 검증 + REFACTORING_PLAN.md 갱신 커밋
```

각 단계마다:
1. 대상 파일 정독 (병렬 Read)
2. 수정
3. `./gradlew :app:assembleDebug` 통과 확인
4. 영역별 커밋 (커밋 메시지에 처리 항목 명시)

---

## 4. 부분 처리 / 미완 항목 (Phase 2 잔여)

| 항목 | 상태 | 메모 |
|---|---|---|
| **D-H3** SavedListScreen 11번 collect → 자식 Composable 분리 | 부분 완료 | D-H2(Sections+Default 디스패처)와 D-H11(droneId→name 캐시)로 부담 핵심은 해결. 남은 자식 분리는 Phase 3 또는 별도 리팩토링 |
| Phase 2.3 deprecated 경고 1건 | 무시 가능 | `NotificationPermissionRequest.kt:61` — `LocalLifecycleOwner` 가 lifecycle-runtime-compose 로 이동. Phase 3 E-M 에서 정리 가능 |

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

### 6.1 Phase 3 전체 진행

```
NEXT_STEPS.md 참조해서 Phase 3 (Medium 55건) 진행해줘
```

### 6.2 특정 영역만 진행

```
NEXT_STEPS.md 의 Phase 3.1 알고리즘/모델 (C-M1~M11) 만 진행해줘
```
또는
```
REFACTORING_PLAN.md 의 4.4 Feature UI Medium 15건 처리해줘
```

### 6.3 push/PR 만 진행

```
NEXT_STEPS.md 5.1 따라 gh CLI 설치하고 push + PR 생성 도와줘
```

### 6.4 마무리 (Phase 3 미진행 결정 시)

```
NEXT_STEPS.md 5.1 부터 운영 작업만 진행하고 Phase 3 는 보류해줘
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

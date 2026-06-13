# DronePass Android 작업 이어가기

> 마지막 업데이트: 2026-06-13
> 브랜치: `fix/critical-pri0-fixes`
> 상태: iOS 동작 대조와 Android 출시 하드닝 진행 중

이 문서는 다음 세션에서 바로 이어가기 위한 현재 기준 핸드오프입니다. 오래된 Phase별 상세 이력은 `REFACTORING_PLAN.md`와 `MIGRATION_PLAN.md`에 남겨두고, 여기에는 지금 실제로 필요한 항목만 둡니다.

## 1. 현재 상태

| 항목 | 값 |
|---|---|
| 워킹 트리 | clean |
| 주요 검증 | `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:minifyReleaseWithR8`, `:app:testDebugUnitTest --tests "*MainScreenStartDestinationTest"`, `:app:testDebugUnitTest --tests "*MapScreenLayersTest" --tests "*SettingsPreferenceKeysTest"` 통과 |
| Release readiness | 실제 `keystore.properties` 또는 `WEB_CLIENT_ID`가 없으면 `assembleRelease`/`bundleRelease`가 의도적으로 실패함을 재확인 |
| 남은 성격 | 실기기 전체 회귀, 콘솔/스토어 운영 설정, 최종 iOS 동기화 검증 |

최근 완료된 iOS 패리티/릴리스 하드닝:

- `3f620cb docs: record connected device smoke`
- `3c84852 docs: record latest debug verification`
- `10d5733 docs: record anonymized deletion parity check`
- `4359d2e docs: record profile sync parity check`
- `dd9cf74 test: cover notification permission gates`
- `07a8ef1 docs: record notification parity check`
- `6525dd9 docs: record weather kp parity check`
- `9a51875 docs: record vworld parity check`
- `83608be docs: record color profile parity check`
- `ebdef32 docs: record sketch parity verification`
- `33c8d37 docs: record login document device smoke`
- `a4fcf29 docs: record firestore contract verification`
- `4ccb31d docs: record app info device smoke`
- `f5e6cd4 fix: initialize app language like ios`
- `a9c1840 docs: refresh android parity handoff`
- `a6da481 docs: refresh android handoff status`
- `5ba0df4 fix: ignore apple sign-in cancellation`
- `8666ad1 fix: match iOS palette color labels`
- `a0e1444 fix: match iOS map app labels`
- `c18aaa7 docs: refresh android handoff status`
- `0cbfa70 fix: match iOS shape detail altitude label`
- `4fd7ffa fix: match iOS drone list localization`
- `7ab941a fix: match iOS common no localization`
- `8637f9b fix: preserve local whitespace shape titles`
- `e4c7287 fix: tolerate invalid firestore colors`
- `246641c fix: match iOS shape focus payload`
- `3e15c37 fix: match iOS address search blank handling`
- `6c06f99 fix: match iOS shape edit placeholder handling`
- `08712ac fix: match iOS shape title fallback`
- `4496256 fix: match iOS shape edit whitespace saving`
- `8c177b6 fix: match iOS drone detail blank handling`
- `16fc469 fix: match iOS shape edit blank handling`
- `a3eb2e0 fix: ignore missing drone delete documents`
- `d984060 fix: match iOS sketch point parsing`
- `5e8a011 fix: match iOS shape detail memo handling`
- `988b275 fix: match main overlay transitions with ios`
- `0b771bd fix: limit sketch overlays to viewport`
- `ae68a72 fix: show partial vworld altitude details`
- `84d28c9 docs: refresh android handoff status`
- `0957956 fix: gate sun alarm rescheduling to user location`
- `55ae939 fix: harden drone selection handling`
- `5cca01b fix: smooth current weather cri`
- `7fecccc fix: align kp chart axes with ios`
- `7d39eb0 fix: gate fcm foreground popup`
- `f7a1152 fix: remove profile auth listener`
- `9b021a5 fix: tolerate missing remote deletes`
- `024c77a fix: accept signed coordinate input`
- `55adb34 fix: save profile sync baseline on logout`
- `674a5b8 fix: keep shape overlay tap aligned with ios`
- `6ee40a3 fix: avoid api keys in debug http logs`
- `fbd90b1 fix: disable os backup for local app state`
- `c19c9c7 fix: harden shape memo webview`
- `7191dd8 fix: strip android logs from release builds`
- `fbf4619 fix: avoid logging raw fcm device ids`
- `eeaf6da docs: refresh android project README`
- `8070866 fix: avoid logging raw fcm tokens`
- `d5e8874 fix: require release signing for release builds`
- `d0cd1a1 fix: match iOS date-only shape edit default`
- `077c1ce fix: match ios sketch hue sync`
- `246bc93 fix: remove nullable force unwraps`
- `1fd67ff fix: match ios flight zone stats header`
- `ff26871 fix: tolerate iOS shape title reads`
- `80bc4cf fix: tolerate iOS drone name reads`
- `ad2f458 fix: match ios shape optional strings`
- `0ac2093 fix: remove sync merge force unwraps`
- `1a4ea43 fix: match ios forecast refresh toast`
- `985412f fix: refresh sync baseline after realtime sync`
- `69afe2d fix: keep main tabs above overlays`
- `d9fd6b8 fix: preserve empty-title shape edit state`
- `c505636 fix: hide saved row delete background at rest`
- `aa2fa11 fix: align drone management strings`
- `feb914e fix: avoid resurrecting deleted sketches`
- `11bd1f0 fix: avoid resurrecting deleted shapes`
- `66d1139 fix: pin auth recovery collections`
- `ecb9e0f fix: gate release on google sign-in config`

## 2. 이번 라운드에서 확인한 내용

코드 대조 후 수정 없이 통과한 영역:

- 드론 목록/상세/생성/편집/삭제/선택 상태와 연결 도형 재할당/삭제 처리
- 저장 도형 목록, 편집/복제 진입, 날짜 기본값
- 지도 기본 위치, 현재 위치 이동 줌, 플로팅 컨트롤
- 기상 예보, KP 예보, 차트 현재 시각 표시
- VWorld 레이어/상세/관할기관 연락처 캐시
- 설정/프로필/앱 정보/패치노트/문서 시트
- 앱정보 화면 섹션/문구/버전 표시 구조
- 로그인/프로필 약관·개인정보 시트 흐름
- 앱 언어 초기값, 문서 URL 언어 분기, 문서 캐시 path 분리
- 알림 예약 로직과 부팅 후 재예약
- 계정/로그인/로그아웃/계정삭제/실시간 동기화 흐름
- Sketch Firestore 직렬화/파싱 계약
- 저장 목록 도형 탭 → 지도 포커스/줌/하이라이트 흐름
- 저장 목록 오버레이 헤더/정렬 칩 토큰, 아이콘, 문자열, 순환 동작
- Shape/Drone/Sketch Firestore 쓰기 표준과 레거시 읽기 방어 계약
- 주소 검색/좌표 입력/지도 롱프레스 도형 생성 흐름
- 도형 생성/편집/복제/삭제 상태 전이와 편집 취소 변경 감지 정책
- 스케치 모드 제스처/툴바/undo·redo/지우개 동작
- 메인 하단 탭/저장·설정 오버레이 탭 전환 흐름
- Shape/Drone/Sketch Firestore 삭제·문서 ID·빈 tombstone 방지 경로
- FCM/로컬 알림 payload 파싱, 포그라운드 팝업, 알림 클릭 후 도형 포커스 라우팅
- 계정 복구/탈퇴 사용자 문서와 익명화 데이터 필드
- `MIGRATION_PLAN.md`는 초기 계획의 역사 문서로 유지하고, 최신 상태 기준은 `README.md`와 이 문서로 고정

수정 완료된 영역:

- 도형 상세 주소 표시/복사 조건을 iOS처럼 `nil`과 빈 문자열 기준으로 보정
- 도형 상세 메모 표시 조건을 iOS처럼 `nil` 기준으로 보정
- 도형 상세 한국어 고도 라벨을 iOS String Catalog처럼 `고도(m)`로 보정
- 도형 상세 외부 지도 앱 한국어 표시명을 최신 iOS String Catalog 기준으로 보정
- 지도 위치 권한을 Android의 대략적인 위치 선택처럼 `ACCESS_COARSE_LOCATION`만 허용된 경우에도 사용 가능하도록 보정
- 스케치 모드 종료 완료 동기화 job을 다음 스케치 작업 체인에 포함해 종료 직후 재진입 시 세션 plan이 겹치지 않도록 보정
- FCM data-only 알림 표시 경로가 `title`/`body`뿐 아니라 로컬 알림 extra와 같은 `notification_title`/`notification_body`도 읽도록 보정
- 도형 상세 제목/삭제 문구/외부 지도 목적지 이름을 iOS `ShapeDetailView`처럼 빈 문자열/공백도 원문 그대로 유지하도록 보정
- 도형 편집 기본정보 좌표/주소 placeholder와 좌표 입력 주소 결과를 iOS처럼 빈 문자열 기준으로 보정
- 주소 검색 선택/표시/건물명 판정을 iOS처럼 빈 문자열 기준으로 보정
- 도형 편집 저장 시 공백 제목/메모/주소를 iOS처럼 실제 입력값으로 보존하고, Firebase 쓰기 검증은 공백 제목을 계속 거부
- 드론 상세 선택 필드/메모 표시와 복사 조건을 iOS처럼 `nil`과 빈 문자열 기준으로 보정
- 드론 목록/상세/편집/삭제 문구를 최신 iOS String Catalog 기준으로 보정
- 드론/도형 색상 팔레트 표시명을 최신 iOS String Catalog 기준으로 보정
- 스케치 Firestore `points` 읽기를 iOS처럼 손상 좌표 원소만 제외하는 관대 파싱으로 보정
- 드론 Firestore 삭제가 iOS hard delete 이후 재삭제될 때 missing document를 성공으로 처리하도록 보정
- 저장 목록 주소 행 표시 조건을 iOS처럼 `address == nil` 기준으로 보정
- 저장 목록 도형 포커스 중복 이동 생략 기준을 iOS처럼 도형 id가 아니라 baseCoordinate 기준으로 보정
- 저장 목록/저장 직후 도형 포커스 좌표와 반경을 iOS `MoveToShapeNotification`처럼 `baseCoordinate`와 `radius ?? 100` 기준으로 보정
- 저장 목록 만료 도형 경계값을 iOS `SavedTableListView`처럼 `flightEndDate <= now` 기준으로 보정
- 저장 목록 행 제목 표시를 iOS `ShapeInfoContent`처럼 빈 문자열/공백도 원문 그대로 유지하도록 보정
- 종료일 알림 본문을 iOS `SettingManager.scheduleEndDateAlarm`처럼 빈 문자열/공백 제목도 그대로 포함하도록 보정
- 알림 탭/포그라운드 팝업 제목·본문을 iOS `PushNotificationManager`처럼 trim 없이 원문 그대로 복원하도록 보정
- 알림 권한 안내 카드/정확한 알람 설정 이동의 Android 12/13 경계 판정을 테스트 가능한 헬퍼로 분리하고 회귀 테스트 추가
- 지도 롱프레스 역지오코딩 결과를 iOS `MainView.handleLongPress`처럼 성공이면 빈 주소도 새 도형 확인 흐름으로 유지하도록 보정
- 스케치 색상 슬라이더 외부 색상 동기화 조건을 iOS `ColorGradientSlider`처럼 hue 절대 차이 기준으로 보정
- 드론 상세 콜백, 날씨 오류 화면, VWorld 지정번호, 알림 재예약의 nullable 경계에서 강제 언랩을 제거
- 공용 LWW/드론 full-sync 병합 경로의 nullable 강제 언랩을 제거하고 서버 전용 드론 병합 회귀 테스트 추가
- VWorld 비행구역 레이어 선택 시트의 통계 헤더를 iOS처럼 전체 폭 배경 밴드로 보정
- 한국 특화 기능 ON/OFF 전환 시 VWorld 레이어 캐시와 열린 레이어/상세 시트를 함께 정리하도록 보정
- KP/날씨 예보 새로고침 완료 메시지를 iOS `ToastMessageModifier`처럼 화면 내부 하단 토스트로 보정
- 실시간/수동 Shape·Drone 동기화 성공 시 계정 전환 보호 기준선(`syncedShapeBaseline`)도 함께 갱신하도록 보정
- 저장/설정 오버레이가 열린 상태에서도 메인 하단 탭이 실제 터치 가능한 최상위 레이어에 남도록 보정
- 저장 목록 행의 스와이프 삭제 빨간 배경/휴지통 아이콘이 평상시 노출되지 않고 end-to-start 스와이프 중에만 보이도록 보정
- 프로필 내 정보 섹션을 iOS `ProfileView`처럼 컴팩트 묶음 행과 가입일 뒤 단일 divider 구조로 보정
- 앱 정보 다중 드론 기능 아이콘을 iOS `paperplane.circle.fill` 의미와 맞는 종이비행기 아이콘으로 보정
- 앱 정보 실시간 날씨 기능 아이콘을 iOS `cloud.sun.fill` 의미와 맞는 구름/날씨 아이콘으로 보정
- 앱 정보 KP 지수 기능 아이콘을 iOS `antenna.radiowaves.left.and.right` 의미와 맞는 안테나 아이콘으로 보정
- 앱 정보 일출/일몰 기능 아이콘을 iOS `sunrise.fill` 의미와 맞는 수평선 해 아이콘으로 보정
- 앱 정보 도형 관리 기능 아이콘을 iOS `circle.circle.fill` 의미와 맞는 원형 아이콘으로 보정
- 앱 정보 클라우드 동기화 기능 아이콘을 iOS `icloud.fill` 의미와 맞는 클라우드 동기화 아이콘으로 보정
- 앱 정보 드론 원스톱 기능 아이콘을 iOS `checkmark.seal.fill` 의미와 맞는 인증 배지 아이콘으로 보정
- Shape 읽기는 iOS 파서처럼 빈 문자열/공백 제목을 보존하고, 쓰기 검증은 공백 제목을 계속 거부
- 빈 제목 기존 Shape도 iOS처럼 기존 도형 편집으로 취급해 드론/반경/고도/비행 기간 초기값을 보존
- Drone 읽기는 iOS Codable 파서처럼 빈 문자열/공백 이름을 보존하고, 쓰기 검증은 공백 이름을 계속 거부
- Shape 쓰기는 iOS `ShapeFirebaseStore`처럼 `memo`/`address`가 `null`이면 Firestore에 빈 문자열로 저장
- 메인 저장/설정 오버레이 전환을 iOS처럼 이동과 opacity 결합 전환으로 보정
- 스케치 오버레이를 iOS처럼 현재 지도 bounds와 겹치는 스케치만 렌더링하도록 보정
- VWorld 상세 고도 행을 상한/하한 중 하나만 있어도 표시하도록 보정
- 일출/일몰 알림 재예약을 iOS처럼 실제 사용자 위치 기반 날씨에만 수행
- 드론 선택 버튼 높이와 드롭다운 원 지름/상단 정렬 일치
- Shape full-sync/download에서 마지막 Shape/Drone 동기화 이전에 서버에서 사라진 로컬 도형을 원격 삭제로 보고 재업로드/재노출하지 않도록 보정하고 종료일 알림을 재조정
- Sketch full-sync/download에서 마지막 Sketch 동기화 이전에 서버에서 사라진 로컬 스케치를 원격 삭제로 보고 재업로드/재노출하지 않도록 보정
- provider 계정 복구 시 `shapes`/`drones`/`sketches`/`metadata`를 함께 이전하고, 기기별 FCM `devices`는 새 로그인 기기 상태로 남기도록 회귀 테스트 고정
- 드론 재할당 삭제 흐름의 자기 자신 타겟 방어
- 전경 FCM 팝업 표시 조건 보정
- KP 차트 축과 현재 날씨 CRI 표시 보정
- 공용 `No` 버튼 한국어 문구를 iOS String Catalog 기준인 `아니오`로 보정
- Apple 로그인 Activity context unwrap
- Apple 로그인 웹 OAuth 사용자 취소를 iOS처럼 오류 알림 없이 무시
- 로그인 화면 Google 버튼을 iOS `LoginView`처럼 흰 배경, 검은 텍스트, 동일 높이, 그림자 토큰으로 보정
- 앱 첫 실행 언어를 iOS처럼 시스템 언어가 한국어면 한국어, 그 외 언어면 영어로 고정하고 AppCompat per-app language 저장 설정 추가
- 실제 배포 산출물(`assembleRelease`/`bundleRelease`)은 release signing과 Google `WEB_CLIENT_ID`가 모두 설정된 경우에만 생성되도록 차단
- 로그인 약관/개인정보 시트 흐름
- 한국어 도형 문구와 상세 라벨
- iOS 기준 gust warning hysteresis
- UUID shape id 강제
- 실시간 sync trigger timing
- 계정 전환 전 로컬 reset 순서
- KP forecast auto refresh
- Weather chart current markers
- Weather info category selected 표시
- 지도 도형 오버레이 탭을 iOS처럼 저장 목록 포커스만 수행하도록 보정
- FCM token/device id 원문 로그 제거
- Release 빌드에서 `android.util.Log` 제거
- Debug HTTP 로그에서 API key 노출 방지
- 메모 링크 WebView 로컬 파일 접근/혼합 콘텐츠 차단
- OS Auto Backup 비활성화와 백업/데이터 추출 규칙 방어적 exclude
- README 최신화

## 2.1 Firestore 크로스플랫폼 계약

iOS와 Android가 공유하는 `users/{uid}/shapes`, `users/{uid}/sketches`, `users/{uid}/drones` 컬렉션은 다음 원칙을 유지합니다.

- 쓰기는 표준 형식만 사용하고, 읽기는 레거시 값을 관대하게 허용한다.
- `shapeType` 등 enum류 필드는 쓰기 시 소문자 raw value만 사용한다. 읽기 시에는 대소문자를 무시한다.
- 날짜는 Firestore `Timestamp`만 사용한다. epoch `Long`, ISO 문자열, Unix 초는 쓰지 않는다.
- 좌표는 `{latitude: Double, longitude: Double}` map만 사용한다. `GeoPoint`, 배열, 정수, 문자열은 쓰지 않는다.
- 색상은 `#RRGGBB` 형식만 사용한다.
- 문서 id와 내부 `id` 필드는 같은 UUID 문자열이어야 한다.
- 필수 필드가 없거나 타입이 깨진 문서는 양 플랫폼 모두 skip될 수 있으므로, 쓰기 전에 검증한다.
- 삭제는 기존 문서에 `deletedAt`/`updatedAt`을 갱신하는 soft delete로 처리하고, 빈 tombstone 문서를 새로 만들지 않는다.

현재 Android 상태:

- Shape: `shapeType` 쓰기 lowercase, 읽기 case-insensitive. 레거시 `CIRCLE` 문서 파싱 테스트 유지.
- Drone: UUID id, `Timestamp`, hex color, optional 필드 보존/파싱 테스트 유지.
- Sketch: `Timestamp`, Double 좌표 map 배열, opacity/좌표 반올림, UUID id 검증 테스트 유지.
- 2026-06-12 추가 방어: Shape/Drone/Sketch 읽기에서 color 필드가 문자열이지만 `#RRGGBB`가 아니면 문서 전체를 버리지 않고 플랫폼 기본색으로 fallback. 쓰기는 계속 표준 hex만 허용.
- 2026-06-12 추가 방어: Shape 읽기는 iOS 파서처럼 `title` 필드가 문자열이면 빈 문자열/공백 문자열도 보존한다. 쓰기 검증은 계속 공백 제목을 거부한다.
- 2026-06-12 정합화: 빈 제목 Shape 문서도 `shape != null`이면 기존 도형 편집으로 취급해 Firestore 읽기 관대성 계약과 편집 UI 초기값 처리를 일치시킨다.
- 2026-06-12 정합화: Shape 쓰기는 iOS `ShapeFirebaseStore`처럼 `memo`/`address`가 `null`이면 Firestore에 빈 문자열로 저장한다.
- 2026-06-12 추가 방어: Drone 읽기는 iOS Codable 파서처럼 `name` 필드가 문자열이면 빈 문자열/공백 문자열도 보존한다. 쓰기 검증은 계속 공백 이름을 거부한다.
- 2026-06-12 재확인: Shape/Drone/Sketch 모두 문서 ID와 내부 `id` 불일치 시 skip하며, 원격 soft delete 헬퍼는 `update(deletedAt, updatedAt)` 기반이라 빈 tombstone 문서를 새로 만들지 않음.
- 2026-06-12 재확인: Sketch `color` 누락/손상 fallback(`#FF0000`)과 `points` 부분 파싱은 iOS `SketchFirebaseStore`와 동일하며, 쓰기 검증은 계속 표준 `#RRGGBB`만 허용.
- 2026-06-12 재확인: Drone optional 문자열(`serialNumber`/`takeoffWeight`/`size`/`memo`)은 iOS 편집 흐름처럼 공백뿐이면 `null`, 내용이 있으면 원문 공백을 보존한다.
- 2026-06-13 추가 방어: Shape 서버에 없는 로컬 문서는 첫 동기화이거나 마지막 Shape/Drone 동기화 이후 수정된 경우만 업로드하고, 마지막 Shape/Drone 동기화 이전 문서는 원격 hard delete로 간주해 로컬에서 제거한다.
- 2026-06-13 추가 방어: Sketch 서버에 없는 로컬 문서는 첫 동기화이거나 마지막 Sketch 동기화 이후 수정된 경우만 업로드하고, 마지막 Sketch 동기화 이전 문서는 원격 hard delete로 간주해 로컬에서 제거한다.
- 2026-06-13 재확인: Android provider 계정 복구는 `sketches`까지 이전한다. 현재 iOS `AuthManager.migrateUserData`는 `shapes`/`drones`/`metadata`만 이전하므로, iOS도 Sketch 데이터 보존을 위해 추후 `sketches` 이전을 추가하는 것이 안전하다.
- 2026-06-13 재확인: Android 계정 탈퇴 원격 삭제는 iOS 기본 삭제 대상(`shapes`/`drones`/`metadata`)에 더해 `sketches`와 Android FCM `devices`까지 삭제한다. 이는 잔여 원격 데이터 방어 목적이며, iOS도 Sketch/기기 토큰 정리 범위 재검토 후보.

## 3. 남은 필수 작업

### 3.1 실기기 회귀

최소 1대의 Android 13+ 실기기에서 확인합니다.

2026-06-12 확인:

- 초반 `adb devices` 결과 연결된 기기 없음. 이후 `RFCW324TZ0Z` 연결 상태에서 최신 빌드 smoke를 추가 진행.
- 저장 목록 오버레이 헤더/정렬 칩과 Firestore 계약은 소스 대조로 재확인했고, 하단 탭 터치 회귀는 실기기에서 재확인.

2026-06-08 부분 확인:

- Android 15 실기기에 `app-debug.apk`를 데이터 유지 방식으로 설치
- 런처 실행 후 지도, 현재 위치, 드론 선택칩, KP/날씨 카드 렌더링 확인
- 저장 목록 bottom sheet와 설정 bottom sheet 전환 확인
- 위 smoke 경로에서 `AndroidRuntime`/DronePass 치명 오류 로그 없음

2026-06-09 추가 확인:

- Android 15 `SM-A346N` 실기기에서 상단 드론 선택 버튼과 드롭다운 원 높이/상단 정렬 확인
- 저장 목록 bottom sheet 빈 상태 렌더링 확인
- 설정 bottom sheet 전환과 내 정보/비행 환경 섹션 렌더링 확인
- 위 경로에서 `AndroidRuntime` 치명 오류 로그 없음

2026-06-12 추가 실기기 확인:

- Android 15(API 35) `SM-A346N`에 `app-debug.apk`를 `adb install -r`로 데이터 유지 설치 후 런처 실행 확인
- 지도, 현재 위치 점, 상단 드론 선택 버튼/드롭다운 원 정렬, KP/날씨 카드, 도형 생성 FAB, 하단 탭 렌더링 확인
- KP 예보 시트와 차트 렌더링 확인
- 저장 목록 오버레이와 설정 오버레이 렌더링 확인
- 저장 목록 오버레이가 열린 상태에서 하단 설정 탭으로 전환되는지 실제 해상도 좌표 기준으로 확인
- 설정에서 드론 관리 시트와 새 드론 추가 편집 시트 렌더링 확인. 추가 화면은 취소로 닫아 데이터 변경 없음
- 로그인/회원가입 시트 렌더링 확인. 실제 OAuth 로그인은 수행하지 않음
- 설정 하단 앱 정보와 패치노트 시트 렌더링 확인
- 저장 목록에서 기존 도형 상세 시트 진입과 상세 정보 렌더링 확인. 데이터 변경 없음
- 스케치 모드 진입, 툴바 렌더링, 완료 버튼 종료 확인. 새 스케치 저장 없음
- 도형 추가 FAB 진입과 새 도형 편집 시트 렌더링 확인. 저장 없이 취소
- 비행구역 레이어 선택 시트 렌더링 확인. 선택 상태 변경 없음
- 날씨 카드에서 현위치 기반 정보 시트 렌더링 확인
- 위 경로에서 `AndroidRuntime` 치명 오류 없음, DronePass 프로세스 유지 확인
- KP/날씨 새로고침 완료 토스트는 단위 테스트/빌드 경로로 검증했고, 실기기 캡처에서는 네트워크 완료 타이밍을 포착하지 못했음
- 최신 디버그 빌드 재설치 후 저장 오버레이 상태에서 설정 탭 터치 전환이 정상 동작함을 재확인
- 로컬 임시 원형 도형 `Smoke0612` 생성, 저장 목록 표시, 상세 시트 렌더링, end-to-start 스와이프 삭제 확인. 삭제 후 목록에 임시 도형이 남지 않음
- 기존 도형 상세 메뉴(`수정하기`/`복제하기`/`삭제`) 렌더링, 수정 편집 시트 진입 후 취소, 복제 편집 시트 진입 후 취소 확인. 데이터 변경 없음
- 저장 목록 행 평상시 빨간 삭제 배경/휴지통 아이콘이 보이지 않고, 일반 상세 진입 chevron만 보임을 최신 APK 설치 후 재확인
- 위 추가 smoke에서 `FATAL EXCEPTION` 없음. `AndroidRuntime` 로그는 `monkey` 명령 프로세스 시작/종료만 확인
- 최신 디버그 빌드 재설치 후 설정의 `내 드론 관리하기` 항목, 드론 목록 타이틀 `내 드론 관리하기`, 섹션 `내 드론` 노출 확인
- 드론 상세 시트에서 `시리얼 번호`, `이륙 중량`, `메모 없음` 문구 확인
- 드론 편집 시트에서 `한 줄 이내로 입력해주세요`, `시리얼 번호 입력 (선택)`, `드론의 제조번호 또는 시리얼 번호를 입력하세요`, `드론의 이륙 중량과 크기를 입력하세요` 문구 확인. 편집은 취소해 데이터 변경 없음
- 위 드론 문구 smoke에서 앱 프로세스 유지, `FATAL EXCEPTION`/`AndroidRuntime` 로그 없음
- 임시 드론 `드론 2` 생성, 목록 표시, 상세 진입, 편집에서 색상 `오렌지`로 변경 저장, 상세 반영 확인
- 임시 드론 삭제 확인 다이얼로그의 `'%s'을(를) 삭제하시겠습니까?` 문구 확인 후 삭제. 삭제 후 목록에 `드론 2`가 남지 않음
- 위 드론 생성/수정/삭제 smoke에서 앱 프로세스 유지, `FATAL EXCEPTION`/`AndroidRuntime` 로그 없음

2026-06-13 추가 실기기 확인:

- Android 15(API 35) `SM-A346N`에 Shape/Sketch 동기화 hard delete 방어 적용 후 최신 `app-debug.apk`를 `adb install -r`로 데이터 유지 설치
- 런처 실행 후 메인 지도 화면, 상단 `내 드론` 선택 버튼/드롭다운, KP 카드, 날씨 카드, 도형 추가 FAB, 하단 `지도`/`저장`/`설정` 탭 렌더링 확인
- UIAutomator XML에서 `내 드론`, `KP`, `지도`, `저장`, `설정`, 온도/일출 시각 텍스트 확인
- 하단 `저장` 탭 전환 후 `저장 목록`, 정렬 칩, 만료 섹션, 저장 도형 행 렌더링 확인
- 하단 `설정` 탭 전환 후 `설정`, `내 정보`, `로그인 / 회원가입`, `내 드론 관리하기`, `비행 환경` 렌더링 확인
- 앱 포커스가 `com.ScienceFiction.DronePassAndroid/.MainActivity`에 있고 프로세스가 유지됨을 확인
- `FATAL EXCEPTION` 없음. `AndroidRuntime` 로그는 `monkey` 명령 프로세스 시작/종료만 확인
- release readiness gate 적용 후 최신 `app-debug.apk`를 같은 Android 15(API 35) `SM-A346N`에 `adb install -r`로 데이터 유지 재설치하고 런처 실행 확인
- 상단 드론 선택 버튼 높이와 오른쪽 드롭다운 원 지름, 두 요소의 상단 정렬이 캡처 기준으로 일치함을 재확인
- 드롭다운을 열어 `내 드론`/`드론 2` 선택 목록 렌더링 확인, `드론 2` 선택 해제 후 상단 하이라이트가 `내 드론`만 남고 빨간 드론 반경 오버레이가 사라짐을 확인
- 하단 `저장` 탭 전환 후 `저장 목록`, 정렬 칩, 저장 도형 행 렌더링 확인
- 하단 `설정` 탭 전환 후 `설정`, `내 정보`, `로그인 / 회원가입`, `내 드론 관리하기`, `비행 환경`, KP/날씨 항목 렌더링 확인
- 위 추가 smoke에서 앱 포커스가 `MainActivity`에 유지되고 `AndroidRuntime` 크래시 로그 없음
- 로그인/회원가입 시트에서 `Google로 로그인`을 눌렀을 때, 현재 로컬 `WEB_CLIENT_ID` 누락 상태에서는 OAuth 화면으로 진입하지 않고 `Google 로그인 설정이 누락되었습니다. WEB_CLIENT_ID를 확인해주세요.` 오류 다이얼로그가 표시됨을 확인
- 최신 VWorld 한국 현지 기능 토글 보정 적용 후 `app-debug.apk` 재설치, VWorld 레이어 선택 시트 렌더링과 선택 카운트 변경 확인
- 설정의 `한국 현지 기능`을 OFF로 전환하면 `한국 현지 기능 비활성화` 안내가 표시되고, 지도 복귀 후 `비행구역 레이어` FAB와 기존 VWorld 표시 레이어가 사라짐을 확인
- `한국 현지 기능`을 다시 ON으로 복원하면 `한국 현지 기능 활성화` 안내가 표시되고, 지도 복귀 후 `비행구역 레이어` FAB가 다시 표시됨을 확인
- 위 VWorld/Korea toggle smoke에서 앱 프로세스 유지, `AndroidRuntime:E` 로그 없음
- 앱 정보 아이콘 패리티 누적 수정 후 최신 `app-debug.apk`를 Android 15(API 35) `SM-A346N`에 `adb install -r`로 데이터 유지 설치하고 런처 실행 확인
- 메인 지도 화면에서 Naver Map, 상단 `내 드론`/`드론 2` 선택 버튼과 드롭다운, `비행구역 레이어`, `스케치`, `KP`, 날씨 카드, `도형 추가`, 하단 `지도`/`저장`/`설정` 탭 렌더링 확인
- 설정 오버레이에서 `앱 정보`까지 스크롤 후 앱 정보 시트 진입 확인. `앱 정보`, `앱 소개`, `드론 관리`, `다중 드론 관리`, `드론 비행 허가지 시각화`, `만료일 기반 알림`, `환경 정보`, `실시간 날씨 정보` 렌더링 확인
- 앱 정보 시트 추가 스크롤로 `KP 지수 모니터링`, `일출/일몰 정보 및 알림`, `도형 및 지도`, `반경 기반 도형 생성 및 관리`, `클라우드 및 데이터`, `클라우드 실시간 동기화`, `드론 원스톱 연계 최적화`, `버전 정보` 렌더링 확인
- 위 앱 정보 smoke에서 앱 포커스가 `MainActivity`에 유지되고 앱 PID 유지, `AndroidRuntime:E` 로그 없음
- 설정 `로그인 / 회원가입` 시트에서 iOS `LoginView` 순서와 같은 `Apple로 계속하기`, `Google로 로그인`, `로그인 / 회원가입 시`, `이용약관`, `개인정보 취급방침`, `동의하게 됩니다.` 렌더링 확인. 설정 시트 로그인 경로에서는 `로그인 없이 시작` 버튼이 보이지 않음
- 로그인 시트의 `이용약관` 링크에서 `주식회사 싸이언스픽션 서비스 이용약관`, `제1장 총칙` 문서 로드 확인
- 로그인 시트의 `개인정보 취급방침` 링크에서 `주식회사 싸이언스픽션 개인정보 처리방침`, `서문` 문서 로드 확인
- 위 로그인/문서 smoke에서 앱 PID 유지, `AndroidRuntime:E` 로그 없음
- 최신 누적 검증 APK를 Android 15(API 35) `SM-A346N` / `RFCW324TZ0Z`에 `adb install -r`로 데이터 유지 설치하고 런처 실행 확인
- 메인 지도에서 Naver Map controls, 상단 `내 드론`/`드론 2` 선택 버튼, 드롭다운 원, `비행구역 레이어`, `스케치`, `KP`, 날씨 카드, `도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링 확인
- UIAutomator bounds 기준 상단 `내 드론` 버튼 `[401,195][657,285]`, `드론 2` 버튼 `[680,195][922,285]`, 드롭다운 원 `[945,195][1035,285]`로 y=195 시작과 높이 90이 모두 일치함을 재확인
- 하단 `저장` 탭에서 `저장 목록`, 정렬 칩 `비행시작일순`/`내림차순`, `활성화` 섹션, 저장 도형 행 렌더링 확인
- 하단 `설정` 탭에서 `설정`, `내 정보`, `로그인 / 회원가입`, `내 드론 관리하기`, `비행 환경`, `현재 KP 지수: 2.0`, `현재 날씨`, `알림` 섹션 진입 렌더링 확인
- 설정 `로그인 / 회원가입` 시트 렌더링 후 `Google로 로그인` 버튼을 눌렀을 때 현재 로컬 `WEB_CLIENT_ID` 누락 상태에서 `로그인 오류` 다이얼로그와 `Google 로그인 설정이 누락되었습니다. WEB_CLIENT_ID를 확인해주세요.` 메시지 표시 확인
- 설정 `내 드론 관리하기`에서 `내 드론 관리하기` 화면, 기존 `내 드론`/`드론 2` 목록, `새 드론 추가`, 사용법 문구 렌더링 확인. 데이터 변경 없음
- `비행구역 레이어` 화면에서 `드론 비행 구역`, `선택된 레이어`, `0 / 13`, `전체 선택`, `전체 해제`, 주요 레이어 목록 렌더링 확인. smoke 중 임시 선택된 레이어는 `전체 해제` 후 홈 화면에서 배지가 사라진 상태로 복구
- `스케치` 버튼 진입 후 하단 스케치 툴바, `지우개`, 되돌리기/다시 실행, `전체 삭제`, `완료` 컨트롤 렌더링과 완료 종료 확인. 새 스케치 저장 없음
- 위 실기기 smoke 전 구간에서 앱 PID 유지, 최종 홈 화면 복귀, `AndroidRuntime:E` 크래시 로그 없음
- 이어진 코드 대조에서 스케치 모드 툴바/제스처/Undo·Redo/지우개/완료 동기화 흐름과 도형 상세 외부지도/복사/메모 링크/드론 상태 표시가 iOS 구현과 맞는지 재확인
- 스케치 실기기 실제 입력 smoke 진행: baseline 활성 스케치 0개에서 지도 위 stroke 1개 그리기, 삭제 배지 `1` 표시, undo 후 배지 제거, redo 후 배지 복원 확인
- 같은 stroke에서 지우개 모드 삭제 후 배지 제거, undo 복원 후 배지 `1` 재표시 확인
- `완료` 저장 후 Room `sketches where deletedAt is null` count가 1이고 points payload가 저장됨을 로컬 DB 덤프로 확인
- 앱 force-stop 후 `MainActivity` 재실행, 스케치 모드 재진입 시 삭제 배지 `1`이 복원되어 저장 후 재실행 복원 확인
- 검증용 stroke는 `전체 삭제` 확인 다이얼로그(`모든 스케치 삭제`, `1개의 스케치를 모두 삭제하시겠습니까?`)에서 `삭제`로 정리하고 `완료` 종료. 최종 활성 스케치 count는 0으로 baseline 복귀, soft-delete row는 iOS와 같은 삭제 tombstone으로 1개 남음
- 위 스케치 실제 입력/재실행 smoke에서 앱 PID 유지, `AndroidRuntime:E` 크래시 로그 없음
- 저장 도형 lifecycle 실기기 smoke 진행: baseline 활성 도형 count 3에서 도형 추가 FAB로 기본 원형 도형을 저장하고, 저장 목록의 `활성화` 섹션 맨 위에 새 `새 도형` 행이 표시됨을 확인
- 저장 직후 Room `shapes` 덤프에서 활성 count 4, 신규 id `8a91af9a-ef3f-40a4-8326-591ae3ce8db7`, `shapeType=circle`, `radius=200.0`, `address=해당 위치의 주소가 존재하지 않습니다` 저장 확인
- 신규 도형 상세 시트에서 `상세 정보`, `드론`, `제목`, 좌표, 주소, `반경 200 m`, 시작일/종료일, 메모 `-` 렌더링 확인
- 상세 더보기 메뉴에서 `수정하기`/`복제하기`/`삭제` 항목 렌더링 확인. 수정 편집 화면과 복제 편집 화면 모두 기존 값(`새 도형`, 드론 2, 좌표, 주소, 반경 200, 날짜)을 프리필하며, 저장 없이 취소해 추가 데이터 변경 없음
- 삭제 확인 다이얼로그 `도형 삭제`, `'새 도형' 도형을 삭제하시겠습니까?`, `취소`/`삭제` 버튼 렌더링 확인 후 삭제 확정. 저장 목록에서는 임시 도형이 빠지고 기존 3개 행만 남음
- 삭제 후 Room `shapes` 덤프에서 활성 count가 3으로 baseline 복귀했고, 임시 도형은 `deletedAt=1781345521781`로 soft delete 처리됨을 확인
- 위 저장 도형 lifecycle smoke에서 앱 PID 유지, `AndroidRuntime:E` 크래시 로그 없음
- 실계정 FCM/로컬 알림 수신, 앱 삭제 후 재설치 동기화는 아직 별도 실검증 항목으로 유지

1. 지도 로드, 현재 위치 권한, 현재 위치 이동
2. 원형 도형 생성, 저장, 편집, 삭제, 복제
   - iOS와 Android 모두 현재 사용자 생성/지도 렌더링은 원형만 노출
   - 사각형/다각형/선은 모델과 Firestore 파싱/보존 검증 대상이며, 현재 UX 회귀 대상은 아님
3. 드론 생성/수정/삭제와 도형 드론 연결
4. 스케치 그리기, 지우기, undo/redo, 저장 후 재실행 복원
5. Google 로그인, Apple 로그인, 로그아웃, 계정 전환
6. Firestore iOS ↔ Android 동기화
7. FCM 수신, 로컬 종료일 알림, 일출/일몰 알림
8. 언어 변경, 한국 현지 기능 ON/OFF, VWorld 레이어 해제
9. 패치노트/약관/개인정보 문서 로드
10. 앱 삭제 후 재설치 시 로그인/Firestore 동기화/FCM deviceId 재생성

### 3.2 운영 콘솔 설정

- Firebase Android 앱 SHA-1/SHA-256 등록
- Firebase Apple provider OAuth 설정 확인
- Firebase Web client id를 `local.properties`의 `WEB_CLIENT_ID`에 설정
- Naver Cloud Android 앱 패키지명/SHA-1 등록
- VWorld API key 운영 키 확인
- FCM 서버 payload의 `shapeId` 또는 `shape_id`, `title`, `body` 형식 확인

2026-06-09 로컬 설정 확인:

- `applicationId`와 `app/google-services.json`의 `package_name`은 `com.ScienceFiction.DronePassAndroid`로 일치
- `local.properties`의 `NAVER_MAP_CLIENT_ID`, `NAVER_MAP_CLIENT_SECRET`, `VWORLD_API_KEY`는 값이 있음
- `local.properties`의 `WEB_CLIENT_ID`는 현재 비어 있음
  - Android Google 로그인은 이 값이 설정되어야 동작함
  - 값이 비어 있으면 앱은 Google 로그인 시 명시적인 설정 누락 오류를 표시하도록 보강됨
  - 2026-06-13 실기기에서 `Google로 로그인` 버튼 탭 시 위 설정 누락 오류 다이얼로그가 표시되고 크래시가 없음을 확인
- `app/google-services.json`의 Android client에는 현재 `oauth_client` 항목이 비어 있음
  - Firebase Console에서 Android 앱 SHA-1/SHA-256 등록 후 `google-services.json`을 다시 내려받고, Web client ID를 `local.properties`의 `WEB_CLIENT_ID`에 설정해야 Google 로그인을 실계정으로 검증할 수 있음
- 2026-06-13 재확인: `WEB_CLIENT_ID`는 여전히 비어 있고, 실제 `keystore.properties` 파일도 아직 없어 release artifact와 실계정 Google 로그인 검증은 외부 설정 후 진행 가능
- 현재 로컬 debug keystore 지문
  - SHA-1: `30:C4:5A:F9:91:83:D9:6C:F7:6C:41:31:9E:DA:82:E7:59:8F:20:86`
  - SHA-256: `BC:5A:36:F1:68:B8:B9:F9:9A:95:14:D8:5F:35:00:37:40:90:B9:97:4C:88:27:28:7E:4A:0A:61:91:FB:B2:CB`
- Release SHA-1/SHA-256은 실제 release keystore 구성 후 별도 산출 필요

### 3.3 Release signing

`keystore.properties.example`을 기준으로 실제 서명 파일을 구성합니다.

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias dronepass
cp keystore.properties.example keystore.properties
```

`keystore.properties` 예시:

```properties
storeFile=release.jks
storePassword=...
keyAlias=dronepass
keyPassword=...
```

서명 또는 Google Web client ID 설정 전에는 다음 명령이 실패해야 정상입니다.

```bash
./gradlew :app:assembleRelease
./gradlew :app:bundleRelease
```

서명 설정 후에는 `bundleRelease` 결과물로 Play Console 내부 테스트 트랙에 올립니다.

## 4. 검증 명령

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:minifyReleaseWithR8
```

참고:

- 2026-06-12에 `:app:testDebugUnitTest --tests "*MainScreenStartDestinationTest"`와 `:app:testDebugUnitTest`를 재실행해 통과 확인.
- 2026-06-12에 `:app:testDebugUnitTest --tests "*StringResourceCoverageTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug`를 최신 iOS 문구 정합 커밋 후 재실행해 통과 확인.
- 2026-06-12에 `:app:testDebugUnitTest --tests "*PaletteColorTest" --tests "*StringResourceCoverageTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug`를 색상 표시명 정합 커밋 후 재실행해 통과 확인.
- 2026-06-12에 `:app:minifyReleaseWithR8`를 재실행해 통과 확인.
- 2026-06-12에 저장 목록 지도 포커스와 Firestore 계약 재확인 후 `:app:testDebugUnitTest --tests "*MapCameraFocusTest" --tests "*ShapeFirestoreParsingTest" --tests "*DroneFirestoreParsingTest" --tests "*SketchFirebaseStoreTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug`를 재실행해 통과 확인.
- 2026-06-12에 저장 목록 만료 경계값을 iOS `SavedTableListView` 기준으로 정정한 뒤 `:app:testDebugUnitTest --tests "*SavedListSectionsTest" --tests "*SavedShapeListItemTest" --tests "*MapCameraFocusTest" --tests "*ShapeRepositoryTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug`를 재실행해 통과 확인.
- 2026-06-12에 저장 목록 행 제목 표시 보정 후 `:app:testDebugUnitTest --tests "*SavedShapeListItemTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug`를 재실행해 통과 확인.
- 2026-06-12에 KP/날씨 예보 새로고침 토스트 정합 후 `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*KpChartsTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:minifyReleaseWithR8`를 재실행해 통과 확인.
- 2026-06-12에 동기화 기준선 갱신 보정 후 `:app:testDebugUnitTest --tests "*RealtimeSyncManagerTest" --tests "*AuthViewModelForegroundSyncTest" --tests "*ProfileViewModelTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:minifyReleaseWithR8`를 재실행해 통과 확인.
- 2026-06-12에 빈 제목 기존 도형 편집 상태 보존 보정 후 `:app:testDebugUnitTest --tests "*ShapeEditDefaultsTest"`, `:app:testDebugUnitTest --tests "*Shape*Test"`, `:app:assembleDebug`, `:app:testDebugUnitTest`를 재실행해 통과 확인.
- 2026-06-12에 저장 목록 스와이프 삭제 배경 노출 보정 후 `:app:testDebugUnitTest --tests "*SavedListSectionsTest"`, `:app:testDebugUnitTest --tests "*Saved*Test"`, `:app:assembleDebug`, `:app:testDebugUnitTest`를 재실행해 통과 확인.
- 2026-06-12에 드론 관리/상세/편집 문구를 iOS String Catalog 기준으로 보정 후 `:app:testDebugUnitTest --tests "*StringResourceCoverageTest"`, `:app:testDebugUnitTest --tests "*Drone*Test"`, `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:minifyReleaseWithR8`를 재실행해 통과 확인.
- 2026-06-13에 Sketch 서버 누락 로컬 문서 재업로드 방지 보정 후 `:app:testDebugUnitTest --tests "*SketchRepositoryTest"`, `:app:testDebugUnitTest --tests "*Sketch*Test"`, `:app:assembleDebug`, `:app:testDebugUnitTest`를 재실행해 통과 확인.
- 2026-06-13에 Shape 서버 누락 로컬 문서 재업로드 방지 보정 후 `:app:testDebugUnitTest --tests "*ShapeRepositoryTest"`, `:app:testDebugUnitTest --tests "*Shape*Test"`, `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:minifyReleaseWithR8`를 재실행해 통과 확인.
- 2026-06-13에 provider 계정 복구 컬렉션 정책을 고정한 뒤 `:app:testDebugUnitTest --tests "*AuthRepositoryUserDocumentTest"`, `:app:testDebugUnitTest --tests "*Auth*Test"`, `:app:compileDebugKotlin`을 재실행해 통과 확인.
- 2026-06-13에 release readiness gate를 signing + Google `WEB_CLIENT_ID`로 확장한 뒤 `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:minifyReleaseWithR8`를 재실행해 통과 확인. `:app:assembleRelease`는 두 설정 누락을 함께 표시하며 의도적으로 실패함을 확인.
- 2026-06-13에 문서/앱 정보 경로를 재확인했다. Terms/Privacy 캐시는 요청 path를 함께 저장해 언어별 파일이 섞이지 않고, 약관/개인정보 `Content` 재진입 시 자동 로드를 건너뛰는 동작은 iOS `FetchWebDocuments` 메모리 캐시 정책과 동일하게 유지한다. `:app:testDebugUnitTest --tests "*DocumentRepositoryTest" --tests "*DocumentEntryPolicyTest" --tests "*PatchNotesContentTest" --tests "*MarkdownParserTest" --tests "*AppInfoScreenTest" --tests "*StringResourceCoverageTest"` 통과 확인.
- 2026-06-13에 지도 위치 권한 판정을 `fine || coarse`로 보정한 뒤 `:app:testDebugUnitTest --tests "*MapCameraFocusTest"`, `:app:assembleDebug`를 재실행해 통과 확인.
- 2026-06-13에 도형 생성/편집/복제/삭제 경로를 재확인했다. Android 편집 취소 변경 감지에서 날짜 필드를 제외하는 동작은 iOS `ShapeEditViewModel.hasChanges()`와 동일하므로 유지한다. `:app:testDebugUnitTest --tests "*ShapeEditDefaultsTest" --tests "*MapCameraFocusTest" --tests "*SavedListSectionsTest" --tests "*SavedShapeListItemTest" --tests "*ShapeValidationTest" --tests "*ShapeRepositoryTest" --tests "*ShapeFirestoreParsingTest" --tests "*ShapeFirebaseStoreTest"` 통과 확인.
- 2026-06-13에 드론 생성/수정/삭제와 도형 연결 경로를 재확인했다. 새 드론 추가는 iOS처럼 다중 필터 선택에만 자동 추가하고, 삭제 시 연결 도형 재할당/삭제는 soft delete와 `updatedAt` 갱신 계약을 유지한다. `:app:testDebugUnitTest --tests "*DroneDeleteValidationTest" --tests "*DroneEditSheetTest" --tests "*DroneListScreenTest" --tests "*DroneNameWidthLimitTest" --tests "*DroneNextColorTest" --tests "*DroneSelectionStateTest" --tests "*DroneSelectionDropdownTest" --tests "*DroneModelTest" --tests "*DroneValidationTest" --tests "*DroneFirestoreParsingTest" --tests "*DroneSyncMergeTest" --tests "*ShapeDetailDroneResolutionTest" --tests "*ShapeEditDefaultsTest" --tests "*ShapeRepositoryTest"` 통과 확인.
- 2026-06-13에 스케치 모드 종료 직후 재진입 시 이전 완료 동기화와 새 세션 시작이 겹칠 수 있는 경로를 보정했다. `:app:testDebugUnitTest --tests "*SketchDefaultsTest" --tests "*SketchEraserSelectionTest" --tests "*SketchOverlayColorTest" --tests "*SketchTouchDecisionTest" --tests "*SketchRepositoryTest" --tests "*SketchFirebaseStoreTest" --tests "*SketchSmoothingAlgorithmTest" --tests "*SketchValidationTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-13에 FCM data-only 알림 표시 제목/본문 추출을 공통 알림 키 정책으로 보정했다. `notification_title`/`notification_body`가 먼저 있으면 빈 문자열이어도 fallback하지 않고 보존한다. `:app:testDebugUnitTest --tests "*FcmServiceTest" --tests "*NotificationSchedulerTest" --tests "*NotificationPreferenceKeysTest" --tests "*UserLocationKeysTest" --tests "*MainScreenStartDestinationTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-13에 로그인 화면 Google 버튼을 iOS `LoginView`의 흰 배경/검은 텍스트/그림자 버튼 톤으로 보정했다. `:app:testDebugUnitTest --tests "*Auth*Test" --tests "*StringResourceCoverageTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-13에 프로필 내 정보 섹션을 iOS `ProfileView`의 컴팩트 `VStack(spacing: 8)` 구조와 가입일 뒤 단일 divider에 맞춰 보정했다. `:app:testDebugUnitTest --tests "*Profile*Test" --tests "*StringResourceCoverageTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-13에 앱 정보 KP 지수 기능 아이콘을 iOS 안테나 아이콘 의미에 맞춰 보정했다. `:app:testDebugUnitTest --tests "*AppInfoScreenTest" --tests "*StringResourceCoverageTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-13에 앱 정보 기능 아이콘(다중 드론, 날씨, 일출/일몰, 도형 관리, 클라우드 동기화, 드론 원스톱)을 iOS SF Symbol 의미에 맞춰 보정했다. `:app:testDebugUnitTest --tests "*AppInfoScreenTest" --tests "*StringResourceCoverageTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-13 최신 앱 정보 아이콘 누적 수정 후 `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:minifyReleaseWithR8` 전체 회귀를 재실행해 통과 확인. R8는 Naver Maps SDK stack map table 경고와 Play Services Location companion object 경고를 출력하지만 현재 build failure는 아님.
- 2026-06-13 누적 수정 debug APK를 실기기 `RFCW324TZ0Z`에 `adb install -r`로 설치 후 MainActivity 실행 smoke 완료. UI dump에서 `지도` 노드와 Naver Map controls 렌더링 확인, 앱 PID 유지, `AndroidRuntime:E` 크래시 로그 없음.
- 2026-06-13에 Firestore 크로스플랫폼 계약을 재확인했다. Shape/Sketch/Drone 쓰기는 소문자 `shapeType` rawValue, `Timestamp`, Double 좌표 map 계약을 유지하고, Shape 읽기는 레거시 대문자 `CIRCLE`을 계속 허용한다. `:app:testDebugUnitTest --tests "*ShapeTypeTest" --tests "*ShapeFirebaseStoreTest" --tests "*ShapeFirestoreParsingTest" --tests "*SketchFirebaseStoreTest" --tests "*DroneFirestoreParsingTest"` 통과 확인.
- 2026-06-13에 스케치 모드와 도형 상세 시트 iOS 패리티를 재확인했다. `:app:testDebugUnitTest --tests "*Sketch*Test" --tests "*ShapeDetailDroneResolutionTest" --tests "*ExternalMapTargetTest"` 통과 확인.
- 2026-06-13에 색상 팔레트/도형 기본 색상/프로필 계정 흐름을 재확인했다. 드론 팔레트는 iOS처럼 회색 제외 순서와 사용 색상 추천을 유지하고, 도형 지도 색상은 양 플랫폼 모두 `shape.color`를 기준으로 렌더링한다. iOS의 레거시 전역 도형 색상 일괄 변경 로직은 현재 드론별 색상 모델과 충돌할 수 있어 Android에 추가하지 않고 기존 저장 계약을 유지한다. 프로필 탈퇴 문구는 로컬 데이터 유지 안내와 일치하며 Android는 원격 `shapes`/`drones`/`sketches`/`metadata`/`devices`만 삭제 대상으로 유지한다. `:app:testDebugUnitTest --tests "*PaletteColorTest" --tests "*DroneNextColorTest" --tests "*DroneEditSheetTest" --tests "*ShapeEditDefaultsTest" --tests "*Profile*Test" --tests "*StringResourceCoverageTest"` 통과 확인.
- 2026-06-13에 VWorld 비행구역 상세/레이어 선택/연락처 흐름을 재확인했다. 상세 시트 detent 높이, row 높이, NOTAM 색상, 고도 포맷, 연락처 lookup(정확 일치 후 양방향 부분 일치), 레이어 가나다순 정렬과 마지막 separator는 iOS 실행 동작과 일치한다. iOS `VWorldContactManager` 주석은 15일 캐시라고 적혀 있지만 실제 `cacheValidDays` 실행값은 5일이므로 Android의 5일 캐시는 유지한다. `:app:testDebugUnitTest --tests "*VWorld*Test" --tests "*FlightZone*Test" --tests "*AltitudeFormatterTest" --tests "*StringResourceCoverageTest"` 통과 확인.
- 2026-06-13에 날씨/KP 예보 화면을 재확인했다. 현재 날씨 카드 tap → 정보 시트 초기 섹션 이동, 드론 카테고리 메뉴, 차트 시간창/도메인, 데이터 출처/경고/토스트, KP 레벨/정보 가이드/NOAA 데이터 처리 흐름은 iOS 구현과 일치한다. `:app:testDebugUnitTest --tests "*Weather*Test" --tests "*Kp*Test" --tests "*InfoGuideSheetsTest" --tests "*StringResourceCoverageTest"` 통과 확인.
- 2026-06-13에 알림 설정/예약/복구/표시 경로를 재확인했다. 설정 키는 iOS UserDefaults 이름을 유지하고, 일출/일몰은 오늘 값이 지났으면 다음 후보를 사용하며, 앱 시작/부팅 복구는 활성화된 알림만 재예약한다. FCM/로컬 알림 payload는 제목·본문 원문과 `shapeId` 포커스 대상을 보존한다. `:app:testDebugUnitTest --tests "*Notification*Test" --tests "*FcmServiceTest" --tests "*UserLocationKeysTest" --tests "*StringResourceCoverageTest"` 통과 확인.
- 2026-06-13에 알림 권한 요청 UX의 Android 12/13 경계 판정을 회귀 테스트로 고정했다. Android 13 미만 `POST_NOTIFICATIONS` 허용 취급, Android 12 미만 정확한 알람 허용 취급, 권한 카드 노출 조건과 설정 화면 이동 조건을 테스트한다. `:app:testDebugUnitTest --tests "*NotificationPermissionRequestTest" --tests "*NotificationPreferenceKeysTest" --tests "*MainActivityKeepScreenAwakeTest"` 통과 확인.
- 2026-06-13에 프로필 클라우드 동기화 섹션을 재확인했다. `cloudBackupEnabled`/`lastBackupTime` 키, 로그인 필요/비활성/동기화중/활성/대기 상태, 수동 백업 표시 조건, 가입일/로그인 제공자/계정 삭제 흐름은 iOS `ProfileView`와 맞춘다. Android 수동 백업은 현재 공유 Firestore 계약에 맞춰 Shape뿐 아니라 Sketch/Drone까지 `forceSyncNow()`로 보존한다. `:app:testDebugUnitTest --tests "*Profile*Test" --tests "*StringResourceCoverageTest"` 통과 확인.
- 2026-06-13에 계정 탈퇴 익명화 데이터 경로를 재확인했다. Android는 iOS `AnalyticsDataGenerator`와 같은 사용자 통계 필드, Shape/Drone 원본 필드, 500개 batch 분할, 실패해도 Auth 계정 삭제를 계속하는 정책을 유지한다. `:app:testDebugUnitTest --tests "*AnonymizedDeletionDataTest" --tests "*ProfileViewModelTest"` 통과 확인.
- 2026-06-13 알림/프로필/익명화 확인 누적 후 `:app:testDebugUnitTest`, `:app:assembleDebug` 재실행 통과 확인.
- 2026-06-13에 저장 도형 생성/저장/상세/수정 진입/복제 진입/삭제를 실기기 `RFCW324TZ0Z`에서 재확인했다. Room DB 활성 도형 count는 3 -> 4 -> 3으로 복구됐고, 신규 도형은 `shapeType=circle` lowercase 저장과 soft delete `deletedAt` 갱신 계약을 지켰다. 코드 변경 없이 smoke/문서 기록만 수행했으므로 Gradle 재실행은 생략.
- `:app:minifyReleaseWithR8`는 현재 성공합니다.
- Naver Maps SDK와 Play Services Location에서 R8 warning이 여러 줄 출력될 수 있지만, 현재는 build failure가 아닙니다.
- `assembleRelease`와 `bundleRelease`는 실제 release signing과 `WEB_CLIENT_ID` 설정 전까지 의도적으로 차단되며, 2026-06-13에 `assembleRelease` 실패 경로를 재확인했습니다.
- OS Auto Backup은 비활성화되어 있으며, 앱 데이터 백업/동기화는 Firebase 흐름 기준으로 검증합니다.

## 5. 다음에 바로 볼 후보

1. 남은 실기기 회귀 시나리오를 돌리고 실패 항목을 코드 수정 단위로 커밋
2. iOS ↔ Android Firestore 실제 계정 동기화 시나리오 검증
3. Play Store 내부 테스트용 signing 구성 후 `bundleRelease` 검증
4. 실기기/실계정 검증 결과를 반영해 `NEXT_STEPS.md`의 잔여 항목을 줄이기

## 6. 주요 경로

| 종류 | 경로 |
|---|---|
| Android 프로젝트 | `/Users/david/Development/Android/Projects/DronePass` |
| Android 코드 | `app/src/main/java/com/ScienceFiction/DronePassAndroid` |
| Android 테스트 | `app/src/test/java/com/ScienceFiction/DronePassAndroid` |
| iOS 원본 | `/Users/david/Development/Swift/myProjects/DronePass/DronePass` |
| release signing 예시 | `keystore.properties.example` |

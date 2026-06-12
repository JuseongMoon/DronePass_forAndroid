# DronePass Android 작업 이어가기

> 마지막 업데이트: 2026-06-12
> 브랜치: `fix/critical-pri0-fixes`
> 상태: iOS 동작 대조와 Android 출시 하드닝 진행 중

이 문서는 다음 세션에서 바로 이어가기 위한 현재 기준 핸드오프입니다. 오래된 Phase별 상세 이력은 `REFACTORING_PLAN.md`와 `MIGRATION_PLAN.md`에 남겨두고, 여기에는 지금 실제로 필요한 항목만 둡니다.

## 1. 현재 상태

| 항목 | 값 |
|---|---|
| 워킹 트리 | clean |
| 주요 검증 | `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:minifyReleaseWithR8` 통과 |
| Release signing | 실제 `keystore.properties` 없으면 `assembleRelease`/`bundleRelease`가 의도적으로 실패함을 재확인 |
| 남은 성격 | 실기기 전체 회귀, 콘솔/스토어 운영 설정, 최종 iOS 동기화 검증 |

최근 완료된 iOS 패리티/릴리스 하드닝:

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

## 2. 이번 라운드에서 확인한 내용

코드 대조 후 수정 없이 통과한 영역:

- 드론 목록/상세/편집/삭제/선택 상태
- 저장 도형 목록, 편집/복제 진입, 날짜 기본값
- 지도 기본 위치, 현재 위치 이동 줌, 플로팅 컨트롤
- 기상 예보, KP 예보, 차트 현재 시각 표시
- VWorld 레이어/상세/관할기관 연락처 캐시
- 설정/프로필/앱 정보/패치노트/문서 시트
- 알림 예약 로직과 부팅 후 재예약
- 계정/로그인/로그아웃/계정삭제/실시간 동기화 흐름
- Sketch Firestore 직렬화/파싱 계약
- 주소 검색/좌표 입력/지도 롱프레스 도형 생성 흐름
- 스케치 모드 제스처/툴바/undo·redo/지우개 동작
- 메인 하단 탭/저장·설정 오버레이 탭 전환 흐름
- Shape/Drone/Sketch Firestore 삭제·문서 ID·빈 tombstone 방지 경로
- FCM/로컬 알림 payload 파싱, 포그라운드 팝업, 알림 클릭 후 도형 포커스 라우팅
- `MIGRATION_PLAN.md`는 초기 계획의 역사 문서로 유지하고, 최신 상태 기준은 `README.md`와 이 문서로 고정

수정 완료된 영역:

- 도형 상세 주소 표시/복사 조건을 iOS처럼 `nil`과 빈 문자열 기준으로 보정
- 도형 상세 메모 표시 조건을 iOS처럼 `nil` 기준으로 보정
- 도형 제목 표시/삭제 문구/외부 지도 이름 fallback을 iOS 저장 규칙처럼 빈 문자열 기준으로 보정
- 도형 편집 기본정보 좌표/주소 placeholder와 좌표 입력 주소 결과를 iOS처럼 빈 문자열 기준으로 보정
- 주소 검색 선택/표시/건물명 판정을 iOS처럼 빈 문자열 기준으로 보정
- 도형 편집 저장 시 공백 제목/메모/주소를 iOS처럼 실제 입력값으로 보존
- 드론 상세 선택 필드/메모 표시와 복사 조건을 iOS처럼 `nil`과 빈 문자열 기준으로 보정
- 스케치 Firestore `points` 읽기를 iOS처럼 손상 좌표 원소만 제외하는 관대 파싱으로 보정
- 드론 Firestore 삭제가 iOS hard delete 이후 재삭제될 때 missing document를 성공으로 처리하도록 보정
- 저장 목록 주소 행 표시 조건을 iOS처럼 `address == nil` 기준으로 보정
- 저장 목록 도형 포커스 중복 이동 생략 기준을 iOS처럼 도형 id가 아니라 baseCoordinate 기준으로 보정
- 메인 저장/설정 오버레이 전환을 iOS처럼 이동과 opacity 결합 전환으로 보정
- 스케치 오버레이를 iOS처럼 현재 지도 bounds와 겹치는 스케치만 렌더링하도록 보정
- VWorld 상세 고도 행을 상한/하한 중 하나만 있어도 표시하도록 보정
- 일출/일몰 알림 재예약을 iOS처럼 실제 사용자 위치 기반 날씨에만 수행
- 드론 선택 버튼 높이와 드롭다운 원 지름/상단 정렬 일치
- 드론 재할당 삭제 흐름의 자기 자신 타겟 방어
- 전경 FCM 팝업 표시 조건 보정
- KP 차트 축과 현재 날씨 CRI 표시 보정
- Apple 로그인 Activity context unwrap
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
- 2026-06-12 재확인: Shape/Drone/Sketch 모두 문서 ID와 내부 `id` 불일치 시 skip하며, 원격 soft delete 헬퍼는 `update(deletedAt, updatedAt)` 기반이라 빈 tombstone 문서를 새로 만들지 않음.

## 3. 남은 필수 작업

### 3.1 실기기 회귀

최소 1대의 Android 13+ 실기기에서 확인합니다.

2026-06-12 확인:

- `adb devices` 결과 연결된 기기 없음. 실기기 smoke는 진행하지 못함.

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

서명 설정 전에는 다음 명령이 실패해야 정상입니다.

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
- 2026-06-12에 `:app:minifyReleaseWithR8`를 재실행해 통과 확인.
- `:app:minifyReleaseWithR8`는 현재 성공합니다.
- Naver Maps SDK와 Play Services Location에서 R8 warning이 여러 줄 출력될 수 있지만, 현재는 build failure가 아닙니다.
- `assembleRelease`와 `bundleRelease`는 실제 release signing 설정 전까지 의도적으로 차단되며, 2026-06-12에 `assembleRelease` 실패 경로를 재확인했습니다.
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

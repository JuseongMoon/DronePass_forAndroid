# DronePass Android 작업 이어가기

> 마지막 업데이트: 2026-06-08
> 브랜치: `fix/critical-pri0-fixes`
> 상태: iOS 동작 대조와 Android 출시 하드닝 진행 중

이 문서는 다음 세션에서 바로 이어가기 위한 현재 기준 핸드오프입니다. 오래된 Phase별 상세 이력은 `REFACTORING_PLAN.md`와 `MIGRATION_PLAN.md`에 남겨두고, 여기에는 지금 실제로 필요한 항목만 둡니다.

## 1. 현재 상태

| 항목 | 값 |
|---|---|
| 워킹 트리 | clean |
| 주요 검증 | `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:minifyReleaseWithR8` 통과 |
| Release signing | 실제 `keystore.properties` 없으면 `assembleRelease`/`bundleRelease`가 의도적으로 실패 |
| 남은 성격 | 실기기 회귀, 콘솔/스토어 운영 설정, 최종 iOS 동기화 검증 |

최근 완료된 릴리스 하드닝:

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

수정 완료된 영역:

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
- FCM token/device id 원문 로그 제거
- Release 빌드에서 `android.util.Log` 제거
- 메모 링크 WebView 로컬 파일 접근/혼합 콘텐츠 차단
- OS Auto Backup 비활성화와 백업/데이터 추출 규칙 방어적 exclude
- README 최신화

## 3. 남은 필수 작업

### 3.1 실기기 회귀

최소 1대의 Android 13+ 실기기에서 확인합니다.

1. 지도 로드, 현재 위치 권한, 현재 위치 이동
2. 원형/사각형/다각형 생성, 저장, 편집, 삭제, 복제
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

- `:app:minifyReleaseWithR8`는 현재 성공합니다.
- Naver Maps SDK와 Play Services Location에서 R8 warning이 여러 줄 출력될 수 있지만, 현재는 build failure가 아닙니다.
- `assembleRelease`와 `bundleRelease`는 실제 release signing 설정 전까지 의도적으로 차단됩니다.
- OS Auto Backup은 비활성화되어 있으며, 앱 데이터 백업/동기화는 Firebase 흐름 기준으로 검증합니다.

## 5. 다음에 바로 볼 후보

1. 실기기 회귀를 먼저 돌리고 실패 항목을 코드 수정 단위로 커밋
2. iOS ↔ Android Firestore 실제 계정 동기화 시나리오 검증
3. Play Store 내부 테스트용 signing 구성 후 `bundleRelease` 검증
4. `MIGRATION_PLAN.md`가 역사 문서로 남아 있어도 되는지 결정, 필요하면 README처럼 최신 상태 문서로 축약

## 6. 주요 경로

| 종류 | 경로 |
|---|---|
| Android 프로젝트 | `/Users/david/Development/Android/Projects/DronePass` |
| Android 코드 | `app/src/main/java/com/ScienceFiction/DronePassAndroid` |
| Android 테스트 | `app/src/test/java/com/ScienceFiction/DronePassAndroid` |
| iOS 원본 | `/Users/david/Development/Swift/myProjects/DronePass/DronePass` |
| release signing 예시 | `keystore.properties.example` |

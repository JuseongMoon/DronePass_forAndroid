# DronePass Android

드론 조종자가 **비행 전 5분 안에 "여기서 날려도 되는가"를 판단**할 수 있게 만드는 앱의
**Android 네이티브 구현**입니다. 국토교통부 공역 데이터를 지도에 겹쳐 보여주고,
자주 비행하는 장소를 도형으로 저장해 여러 기기에서 실시간으로 동기화합니다.

- 플랫폼: Android 9(API 28)+ · Jetpack Compose
- 언어: 한국어 / 영어
- iOS 구현: [dronepass-ios](https://github.com/JuseongMoon/dronepass-ios)

같은 앱의 두 플랫폼 구현이며, **Firestore wire format을 공유**합니다.
한쪽에서 만든 도형이 다른 쪽에 그대로 보여야 하므로, 두 저장소는 같은 계약 문서와
같은 테스트 픽스처를 기준으로 개발됩니다
([`docs/agents/FIRESTORE_CONTRACT.md`](docs/agents/FIRESTORE_CONTRACT.md)).

## 이 앱이 푸는 문제

드론 비행 가능 여부를 확인하려면 원래 국토교통부 공역도, 문화재 보호구역, 국립공원 경계를
따로따로 찾아봐야 합니다. DronePass는 이걸 **한 장의 지도 위 공역 레이어로 합칩니다.**

여기에 비행 안전과 직결되는 정보를 덧붙입니다.

- **KP 지수(지자기 교란 지수)** — GPS 수신 정확도에 영향을 주는 지표. 예보까지 제공
- **기상 예보와 돌풍 경고, 일출·일몰 시각** — 야간비행 승인 필요 시점 판단용
- **보유 기체 관리** — 기체별 제원과 신고번호를 저장
- **지도 스케치** — 지도 위에 자유롭게 그려 비행 경로나 현장 메모를 남김
- **비행 지점 저장** — 원·사각형·다각형·선 도형으로 저장하고 목록·검색·외부 지도 앱 연동

## 기술적으로 다룬 것

**1. 네이버 지도 SDK(View)를 Compose에 통합**

`NMapsMap`은 Android View 기반이라 `AndroidView`로 감쌌습니다. 문제는 Compose의
선언적 재구성과 지도 오버레이의 명령형 수명이 어긋난다는 점입니다. recomposition이 일어날
때마다 오버레이를 전부 다시 만들면 도형이 수십 개만 돼도 지도가 눈에 띄게 끊깁니다.
`ShapeOverlayManager`(`feature/map/overlay/`)를 두어 오버레이의 생성·갱신·해제를
한 곳에서 diff 기반으로 처리하고, `MapViewModel`의 상태 변화가 오버레이 전체 재생성으로
번지지 않게 했습니다. 지도 생명주기는 `MapScreen`에서 `DisposableEffect`로 관리합니다.

**2. 로컬 우선(offline-first) 3계층 저장소**

Room이 단일 진실 공급원이고, Firestore는 그 위의 동기화 계층입니다.
UI는 Room의 `Flow`만 구독하므로 네트워크가 없어도 즉시 반응합니다.

```
UI (Compose) ──► ViewModel ──► Repository ──┬──► Room DAO      (로컬, 즉시)
                                            └──► Firebase Store (원격, 비동기)
```

`ShapeEntity` / `DroneEntity` / `SketchEntity` 세 종류가 같은 패턴을 공유하고,
Repository는 Hilt로 주입됩니다.

**3. 멀티 디바이스 실시간 동기화와 에코 루프 차단**

가장 까다로웠던 부분입니다. Firestore 리스너(`RealtimeSyncManager`)가 원격 변경을 받는데,
**같은 기기가 방금 올린 변경이 리스너로 되돌아와 다시 쓰기를 유발하는 에코 루프**가 생깁니다.
두 기기가 켜져 있으면 이게 무한히 왕복합니다.

- 충돌 해결은 `SyncMerge.kt`의 **LWW(Last Write Wins)** 로 통일했습니다.
  한쪽에만 있으면 그쪽 채택, 양쪽에 있으면 `updatedAt`이 큰 쪽, 동률이면 서버 우선.
- 전체 동기화 후 **실제 업로드가 있었을 때만** 서버 metadata를 갱신합니다
  (`shouldUpdateServerMetadataAfterFullSync`). 다운로드 전용 동기화가 다른 기기의
  리스너를 깨우지 않게 하는 게 핵심이었습니다.
- 삭제는 물리 삭제가 아니라 `deletedAt` tombstone(soft delete)입니다.
  물리 삭제로는 "다른 기기에서 지웠다"는 사실을 전달할 방법이 없습니다.

이 로직은 Repository 세 곳에 복붙돼 있던 것을 단일 함수로 모은 결과이고,
동기화 회귀 테스트가 함께 붙어 있습니다.

**4. iOS와의 wire format 대조**

같은 Firestore 문서를 두 플랫폼이 읽고 씁니다. Swift `Codable`과 Kotlin/Moshi가
같은 JSON을 다르게 해석하는 지점이 실제로 문제가 됐습니다. 대표적으로 `shapeType`은
**쓸 때 소문자 raw value, 읽을 때 대소문자 무시, 모르는 값은 스킵**으로 계약을 고정했습니다.
`CrossPlatformFirestoreContractTest`가 iOS 저장소의 픽스처 JSON(원·사각형·다각형·선·
soft delete·legacy)을 그대로 읽어 Android 파싱 결과를 검증합니다.
iOS 저장소가 로컬에 없으면 이 테스트는 실패가 아니라 skip 됩니다.

**5. 플랫폼 차이를 흡수한 지점**

| | iOS | Android |
| --- | --- | --- |
| 로컬 저장 | 파일 기반 `ShapeFileStore` | Room (쿼리·마이그레이션 필요) |
| 로그인 | Sign in with Apple + Google | Credential Manager + Firebase Auth |
| 비밀값 보관 | Keychain / xcconfig | EncryptedSharedPreferences / `local.properties` |
| 언어 전환 | `AppleLanguages` UserDefaults | `AppCompatDelegate.setApplicationLocales` |
| 알림 | UNUserNotification | FCM + `AlarmManager`, 부팅 후 재예약 필요 |

Android 쪽에만 있는 부담이 알림 예약입니다. iOS와 달리 **재부팅하면 예약된 알람이 사라지므로**
`RECEIVE_BOOT_COMPLETED` 리시버에서 일출/일몰 알림을 다시 등록합니다.
Android 12+에서는 정확한 알람 권한 상태에 따라 이 동작이 달라집니다.

## 구조

```text
app/src/main/java/com/ScienceFiction/DronePassAndroid/
├── app/          Application 초기화
├── core/
│   ├── data/     Room · Firestore · Repository · 동기화(LWW, 실시간 리스너)
│   ├── di/       Hilt 모듈
│   ├── analytics/
│   └── util/
├── domain/model/ 도형 · 드론 · 스케치 도메인 모델
├── feature/
│   ├── map/      네이버 지도 화면, 오버레이 매니저
│   ├── vworld/   공역 레이어, 구역 상세, 관할기관 연락처
│   ├── shape/    도형 생성 · 편집 · 상세
│   ├── saved/    저장 도형 목록
│   ├── sketch/   지도 스케치
│   ├── drone/    기체 등록 · 관리
│   ├── weather/  기상 예보 · 돌풍 경고
│   ├── kp/       KP 지수 예보
│   ├── auth/     Google · Apple 로그인
│   ├── profile/ · settings/ · document/
├── service/      FCM, 로컬 알림, 부팅 리시버
└── ui/           네비게이션 · 테마 · 공통 컴포넌트
```

## 테스트

`app/src/test/`에 113개 단위 테스트가 있습니다. 도형 파싱, Firestore 배치/삭제,
LWW 머지, 설정 마이그레이션, 크로스 플랫폼 계약이 주요 대상입니다.

```bash
./gradlew :app:testDebugUnitTest
```

크로스 플랫폼 계약 테스트는 [iOS 저장소](https://github.com/JuseongMoon/dronepass-ios)의
`team/fixtures/` JSON을 읽습니다. iOS 저장소를 로컬에 받아 두고 `local.properties`에
`IOS_PROJECT_DIR=<iOS 저장소 경로>`를 넣으면 활성화되고, 없으면 실패가 아니라 skip 됩니다.

## 기술 스택

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Lifecycle · DataStore
Hilt(DI) · Room(로컬 DB) · Retrofit / OkHttp / Moshi
Firebase(Auth, Firestore, Messaging, Crashlytics, Analytics) · Credential Manager
[Naver Maps SDK 3.23.1](https://navermaps.github.io/android-map-sdk/guide-ko/) ·
Play Services Location 21.3.0 · Accompanist Permissions · VWorld 오픈 API

minSdk 28 · targetSdk 36 · compileSdk 36

## 실행 방법

```bash
git clone https://github.com/JuseongMoon/DronePass_forAndroid.git
cd DronePass_forAndroid
./gradlew :app:assembleDebug
```

실행에는 본인 명의의 키가 필요합니다.

1. **네이버 클라우드 플랫폼** — Maps 및 Geocoding 이용 신청 후 Key ID / Secret 발급.
   NCP Maps Console에 Android 앱을 패키지명 `com.ScienceFiction.DronePassAndroid`으로
   등록하고, 설치할 APK 서명 인증서의 SHA-1을 함께 등록합니다.
2. **VWorld** — 오픈 API 인증키 발급
3. **Firebase** — 본인 Firebase 프로젝트에 Android 앱을 등록하고
   `app/google-services.json`을 받아 교체합니다. Firebase Console에 debug/release
   SHA-1·SHA-256을 등록해야 `client_type=1` OAuth client가 포함된 파일을 받을 수 있습니다.

키는 프로젝트 루트 `local.properties`에 넣습니다. 이 파일은 `.gitignore`에 등록되어
커밋되지 않습니다.

```properties
NAVER_MAP_KEY_ID=YOUR_NCP_MAPS_KEY_ID
NAVER_MAP_KEY_SECRET=YOUR_NCP_MAPS_KEY_SECRET
VWORLD_API_KEY=YOUR_VWORLD_API_KEY
WEB_CLIENT_ID=YOUR_FIREBASE_WEB_CLIENT_ID
```

Gradle이 이 값을 manifest placeholder(`com.naver.maps.map.NCP_KEY_ID`)와
`BuildConfig`로 주입합니다. **키가 소스 코드에 남지 않습니다.**

SHA-1 지문 확인:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

설정이 제대로 됐는지는 preflight 태스크로 확인할 수 있습니다.

```bash
./gradlew :app:verifyCrossPlatformE2ePrerequisites
```

### Release 빌드

Release 빌드는 서명 설정, `WEB_CLIENT_ID`, Firebase Android OAuth client가 없으면
**의도적으로 실패합니다.** 디버그 키나 콘솔 설정이 빠진 채 릴리스 산출물이 만들어지지
않도록 `assembleRelease` / `bundleRelease` 앞에서 검증합니다.

1. `keystore.properties.example`을 `keystore.properties`로 복사하고
   `storeFile`(루트 기준 상대경로) · `storePassword` · `keyAlias` · `keyPassword`를 채웁니다.
2. `./gradlew :app:bundleRelease`

키스토어와 `keystore.properties`는 커밋하지 않습니다.

## 릴리스 보안 기준

- Release 빌드에서 R8 규칙으로 `android.util.Log` 호출을 제거합니다.
- FCM token과 device id는 debug 로그에서도 원문을 출력하지 않습니다.
- Android OS Auto Backup은 비활성화되어 있습니다.
- 메모 링크용 앱 내부 WebView는 로컬 파일 접근과 혼합 콘텐츠를 차단합니다.
- 토큰·서명 정보·API 키는 로그와 커밋에 남기지 않습니다.

## 문제 해결

**지도가 표시되지 않을 때** — `local.properties`의 `NAVER_MAP_KEY_ID` /
`NAVER_MAP_KEY_SECRET`, NCP Maps Console의 등록 패키지명, 설치된 앱 서명 인증서의
SHA-1 등록 여부를 확인합니다. `adb logcat | grep -i naver`로 SDK 로그를 볼 수 있습니다.

**로그인이 실패할 때** — `app/google-services.json`의 패키지명이 `applicationId`와
같은지, Firebase Console에 SHA-1·SHA-256이 등록됐는지, `WEB_CLIENT_ID`가 Firebase
Web client id와 일치하는지 확인합니다.

**Release 빌드가 실패할 때** — `keystore.properties`의 존재, placeholder가 아닌
실제 값 여부, `storeFile` 경로의 키스토어 파일 존재를 확인합니다.

## 라이선스

MIT License. 자세한 내용은 [LICENSE](LICENSE)를 참고하세요.

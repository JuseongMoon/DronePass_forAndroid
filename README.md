# DronePass Android

iOS DronePass를 Android로 포팅하는 프로젝트입니다. 드론 비행 허가지 시각화, 저장 도형 관리, 기상/우주기상 정보, Firebase 기반 계정/동기화 기능을 Jetpack Compose 기반 Android 앱으로 구현합니다.

## 현재 상태

Android 포팅과 iOS 동작 대조를 함께 진행 중입니다. 현재 앱에는 다음 기능이 포함되어 있습니다.

- 네이버 지도 기반 메인 화면, 위치 권한, 현재 위치 이동, 지도 플로팅 컨트롤
- 원형 도형 생성/편집/상세/삭제/중복 저장
- 기존 사각형/다각형/선 도형 데이터 파싱과 Firestore 보존
- 저장 도형 목록, 도형 선택, 외부 지도 앱 연동
- 드론 등록/수정/삭제, 기본 드론 보장, 드론 선택 상태 관리
- VWorld 비행구역 레이어, 구역 상세, 관할기관 연락처 사전 로드
- 기상 예보, 돌풍 경고, KP 지수 예보, 설정 기반 자동 새로고침
- 스케치 기능
- Firebase Authentication 기반 Google/Apple 로그인
- Firestore 실시간 동기화, UUID 기반 shape id, 계정 전환 시 상태 초기화
- FCM 토큰 등록, 로컬 알림, 부팅 후 알림 재예약
- 설정, 프로필, 문서/약관/개인정보처리방침, 패치노트 화면

## 기술 스택

- Kotlin
- Jetpack Compose, Material 3
- AndroidX Navigation, Lifecycle, DataStore
- Hilt
- Room
- Retrofit, OkHttp, Moshi
- Firebase Auth, Firestore, Messaging, Crashlytics, Analytics
- Google Credential Manager
- Naver Maps SDK 3.23.1
- Google Play Services Location 21.3.0
- minSdk 28, targetSdk 36, compileSdk 36

## 프로젝트 구조

```text
app/src/main/java/com/ScienceFiction/DronePassAndroid/
├── app/                 # Application 초기화
├── core/                # 공통 데이터, DI, 유틸
├── domain/              # 도메인 모델
├── feature/             # 기능별 Compose 화면과 ViewModel
│   ├── auth/
│   ├── document/
│   ├── drone/
│   ├── kp/
│   ├── map/
│   ├── profile/
│   ├── saved/
│   ├── settings/
│   ├── shape/
│   ├── sketch/
│   ├── vworld/
│   └── weather/
├── service/             # FCM, 알림, 부팅 리시버
├── ui/navigation/       # 탭/화면 네비게이션
└── ui/theme/            # Compose 테마
```

## 로컬 설정

프로젝트 루트의 `local.properties`에 로컬 전용 키를 설정합니다. 실제 키는 커밋하지 않습니다.

```properties
NAVER_MAP_CLIENT_ID=YOUR_NAVER_MAP_CLIENT_ID
NAVER_MAP_CLIENT_SECRET=YOUR_NAVER_MAP_CLIENT_SECRET
VWORLD_API_KEY=YOUR_VWORLD_API_KEY
WEB_CLIENT_ID=YOUR_FIREBASE_WEB_CLIENT_ID
```

Firebase 사용을 위해 `app/google-services.json`도 필요합니다.

네이버 지도 키는 `AndroidManifest.xml`의 `com.naver.maps.map.NCP_KEY_ID` 메타데이터에 Gradle manifest placeholder로 주입됩니다. Naver Cloud Console에는 Android 앱 패키지명과 SHA-1 지문을 등록해야 합니다.

- 패키지명: `com.ScienceFiction.DronePassAndroid`
- Debug SHA-1 확인:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

현재 로컬 debug keystore 지문:

- SHA-1: `30:C4:5A:F9:91:83:D9:6C:F7:6C:41:31:9E:DA:82:E7:59:8F:20:86`
- SHA-256: `BC:5A:36:F1:68:B8:B9:F9:9A:95:14:D8:5F:35:00:37:40:90:B9:97:4C:88:27:28:7E:4A:0A:61:91:FB:B2:CB`

Release SHA-1/SHA-256은 실제 release keystore 구성 후 같은 방식으로 별도 산출하여 Firebase/Naver Cloud Console에 등록해야 합니다.

## 빌드와 테스트

Android Studio에서 Gradle Sync 후 실행하거나, 터미널에서 다음 명령을 사용합니다.

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Release 빌드는 실제 서명 설정, Google 로그인 Web client ID, Firebase Android OAuth client가 없으면 의도적으로 실패합니다. 디버그 키나 콘솔 설정 누락 상태로 릴리스 APK/AAB가 만들어지지 않도록 `assembleRelease`와 `bundleRelease` 앞에서 release readiness를 검증합니다.

Release 서명 설정:

1. `keystore.properties.example`을 `keystore.properties`로 복사합니다.
2. 실제 `storeFile`, `storePassword`, `keyAlias`, `keyPassword`를 입력합니다.
3. `storeFile`은 프로젝트 루트 기준 상대경로로 지정합니다.
4. `local.properties`에 Firebase Web client ID를 `WEB_CLIENT_ID`로 입력합니다.
5. Firebase Console에 debug/release SHA-1/SHA-256을 등록하고, `oauth_client`가 포함된 `app/google-services.json`을 다시 내려받습니다.
6. `./gradlew :app:assembleRelease` 또는 `./gradlew :app:bundleRelease`를 실행합니다.

키스토어 생성 예시:

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias dronepass
```

## 권한

앱이 사용하는 주요 권한은 다음과 같습니다.

- `INTERNET`
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `POST_NOTIFICATIONS` (Android 13+)
- `SCHEDULE_EXACT_ALARM` (Android 12+)
- `RECEIVE_BOOT_COMPLETED`

Android 13 이상에서는 알림 권한을 허용해야 FCM/로컬 알림을 볼 수 있습니다. Android 12 이상에서는 정확한 알람 권한 상태에 따라 일출/일몰 알림 예약 동작이 달라질 수 있습니다.

## 릴리스 보안 기준

- Release 빌드에서는 R8 규칙으로 `android.util.Log` 호출을 제거합니다.
- FCM token과 device id는 debug 로그에서도 원문을 출력하지 않습니다.
- Android OS Auto Backup은 비활성화되어 있습니다. 앱 데이터 백업/동기화는 Firebase 흐름을 기준으로 검증합니다.
- 메모 링크용 앱 내부 WebView는 로컬 파일 접근과 혼합 콘텐츠를 차단합니다.

## 문제 해결

지도가 표시되지 않는 경우:

- `local.properties`에 `NAVER_MAP_CLIENT_ID`가 설정되어 있는지 확인합니다.
- Naver Cloud Console의 Android 앱 등록 패키지명이 `com.ScienceFiction.DronePassAndroid`인지 확인합니다.
- 현재 설치한 앱 서명 인증서의 SHA-1이 Naver Cloud Console에 등록되어 있는지 확인합니다.
- 디바이스 또는 에뮬레이터의 인터넷 연결을 확인합니다.
- `adb logcat | grep -i naver`로 지도 SDK 로그를 확인합니다.

로그인이 실패하는 경우:

- `app/google-services.json`의 Android 패키지명이 앱 `applicationId`와 일치하는지 확인합니다.
- Firebase Console에 Android SHA-1/SHA-256 지문이 등록되어 있는지 확인합니다.
- `WEB_CLIENT_ID`가 Firebase Web client id와 일치하는지 확인합니다.
- Apple 로그인은 OAuth 설정과 redirect 흐름을 함께 확인합니다.

실제 iOS/Android 공유 Firestore 동기화 검증은 `CROSS_PLATFORM_E2E_RUNBOOK.md` 절차를 기준으로 수행합니다.

Release 빌드가 실패하는 경우:

- `keystore.properties`가 존재하는지 확인합니다.
- placeholder 값이 아닌 실제 비밀번호와 alias가 들어 있는지 확인합니다.
- `storeFile` 경로의 키스토어 파일이 실제로 존재하는지 확인합니다.

## 개발 원칙

- UI는 Jetpack Compose로 작성합니다.
- iOS DronePass와의 동작/문구/상태 전이를 기준으로 Android 구현을 대조합니다.
- 사용자 데이터 id는 Firestore 동기화를 고려해 UUID 형식을 유지합니다.
- iOS/Android 공유 Firestore wire format은 `FIRESTORE_CONTRACT.md`를 기준으로 유지합니다.
- 민감한 토큰, 서명 정보, API 키는 로그와 Git 커밋에 남기지 않습니다.

## 라이선스

이 프로젝트는 iOS DronePass의 Android 포팅 버전입니다.

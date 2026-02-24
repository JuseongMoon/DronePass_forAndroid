# DronePass Android

드론 비행 허가지 시각화를 위한 Android 애플리케이션 (iOS DronePass 포팅)

## 현재 구현 상태 (1단계)

✅ **완료된 기능:**
- 네이버 Maps SDK 통합
- 기본 지도 화면 표시
- 위치 권한 요청 및 처리
- 현재 위치 표시 및 추적
- 지도 UI 컨트롤 (줌, 나침반, 현재 위치 버튼)

🚧 **향후 구현 예정:**
- 도형(Shape) 그리기 기능 (원형, 사각형, 다각형)
- Firebase 통합 (인증, Firestore)
- 도형 저장/불러오기
- 설정 화면
- 탭 네비게이션

## 기술 스택

- **언어**: Kotlin
- **UI 프레임워크**: Jetpack Compose
- **지도**: 네이버 Maps SDK 3.19.1
- **위치**: Google Play Services Location 21.3.0
- **권한 관리**: Accompanist Permissions 0.36.0
- **최소 SDK**: 28 (Android 9.0 Pie)
- **타겟 SDK**: 36

## 프로젝트 구조

```
app/src/main/java/com/ScienceFiction/DronePass/
├── MainActivity.kt          # 메인 액티비티
├── MapScreen.kt            # 지도 화면 컴포저블
└── ui/theme/               # Material 3 테마
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## 빌드 및 실행

### 사전 요구사항

1. **Android Studio** (최신 버전 권장)
   - Arctic Fox 이상
   - Jetpack Compose 지원

2. **네이버 클라우드 플랫폼 설정**
   - [네이버 클라우드 플랫폼 콘솔](https://console.ncloud.com/)에서 프로젝트 생성
   - Maps API 활성화
   - Android 앱 등록:
     - 패키지명: `com.ScienceFiction.DronePassAndroid`
     - 클라이언트 ID 발급

### 빌드 단계

1. **프로젝트 열기**
   ```bash
   # Android Studio에서 프로젝트 폴더 열기
   ```

2. **Gradle Sync**
   ```bash
   # Android Studio에서 자동으로 실행되거나, 메뉴에서:
   # File > Sync Project with Gradle Files
   ```

3. **네이버 지도 클라이언트 ID 확인**

   `app/src/main/AndroidManifest.xml` 파일에서 클라이언트 ID가 올바르게 설정되어 있는지 확인:
   ```xml
   <meta-data
       android:name="com.naver.maps.map.CLIENT_ID"
       android:value="47b5di8weq" />
   ```

   ⚠️ **중요**: 네이버 클라우드 플랫폼에서 발급받은 Android용 클라이언트 ID로 교체해야 합니다.

4. **빌드**
   ```bash
   # 터미널에서:
   ./gradlew build

   # 또는 Android Studio에서:
   # Build > Make Project (Cmd+F9 / Ctrl+F9)
   ```

5. **실행**
   ```bash
   # 터미널에서:
   ./gradlew installDebug

   # 또는 Android Studio에서:
   # Run > Run 'app' (Cmd+R / Shift+F10)
   ```

### 실행 환경

- **물리 디바이스** (권장)
  - Android 9.0 (API 28) 이상
  - GPS 기능 필요
  - 인터넷 연결 필요

- **에뮬레이터**
  - Google Play Services 포함된 이미지 사용
  - 위치 시뮬레이션 가능

## 권한

앱이 요청하는 권한:

- `ACCESS_FINE_LOCATION`: 정확한 위치 정보 (GPS)
- `ACCESS_COARSE_LOCATION`: 대략적인 위치 정보 (네트워크)
- `INTERNET`: 지도 타일 다운로드

## 네이버 지도 설정

### 클라이언트 ID 설정 방법

1. [네이버 클라우드 플랫폼](https://console.ncloud.com/) 접속
2. Console > Services > AI·NAVER API > AI·NAVER API
3. Application 등록
4. Maps > Mobile Dynamic Map 선택
5. Android 앱 정보 등록:
   - 패키지명: `com.ScienceFiction.DronePassAndroid`
   - 서명 인증서 지문 (SHA-1) 등록
6. 발급받은 클라이언트 ID를 `AndroidManifest.xml`에 입력

### 서명 인증서 지문 확인 방법

**Debug 키스토어:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Release 키스토어:**
```bash
keytool -list -v -keystore /path/to/your/keystore -alias your_alias_name
```

## 주요 기능 설명

### 1. 지도 초기화
- 앱 시작 시 서울 시청(37.5665, 126.9780)을 중심으로 지도 표시
- 줌 레벨: 13

### 2. 위치 권한 처리
- 앱 시작 시 자동으로 위치 권한 요청
- 권한 거부 시에도 기본 지도는 표시됨
- 권한 허용 시 현재 위치로 이동 및 추적

### 3. 지도 UI
- **줌 컨트롤**: 확대/축소 버튼
- **현재 위치 버튼**: 현재 위치로 카메라 이동
- **나침반**: 지도 회전 시 방향 표시

## 문제 해결

### 지도가 표시되지 않는 경우

1. **클라이언트 ID 확인**
   - `AndroidManifest.xml`의 클라이언트 ID가 올바른지 확인
   - 네이버 클라우드 플랫폼에서 앱이 등록되어 있는지 확인

2. **패키지명 확인**
   - 앱의 패키지명이 `com.ScienceFiction.DronePassAndroidAndroid`인지 확인
   - 네이버 클라우드에 등록한 패키지명과 일치하는지 확인

3. **인터넷 연결 확인**
   - 디바이스/에뮬레이터가 인터넷에 연결되어 있는지 확인

4. **로그 확인**
   ```bash
   adb logcat | grep -i naver
   ```

### 위치가 표시되지 않는 경우

1. **권한 확인**
   - 앱 설정에서 위치 권한이 허용되어 있는지 확인

2. **GPS 활성화**
   - 디바이스의 위치 서비스가 켜져 있는지 확인

3. **에뮬레이터 위치 시뮬레이션**
   - Android Studio의 Extended Controls (⋮) > Location에서 GPS 좌표 설정

## 개발 가이드

### 새로운 기능 추가 시

1. **패키지 구조 유지**
   - 모든 새 파일은 `com.ScienceFiction.DronePassAndroid` 패키지 아래에 생성

2. **Compose 우선**
   - UI는 Jetpack Compose로 구현
   - XML 레이아웃은 사용하지 않음

3. **Material 3 사용**
   - Material Design 3 컴포넌트 사용
   - `ui/theme/` 디렉토리에서 테마 관리

## 라이선스

이 프로젝트는 iOS DronePass의 Android 포팅 버전입니다.

## 연락처

문의사항이 있으시면 이슈를 등록해주세요.

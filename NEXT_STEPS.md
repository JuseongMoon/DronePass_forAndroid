# DronePass Android 작업 이어가기

> 마지막 업데이트: 2026-06-16
> 브랜치: `fix/critical-pri0-fixes`
> 상태: iOS 동작 대조와 Android 출시 하드닝 진행 중

이 문서는 다음 세션에서 바로 이어가기 위한 현재 기준 핸드오프입니다. 오래된 Phase별 상세 이력은 `REFACTORING_PLAN.md`와 `MIGRATION_PLAN.md`에 남겨두고, 여기에는 지금 실제로 필요한 항목만 둡니다.

## 1. 현재 상태

| 항목 | 값 |
|---|---|
| 워킹 트리 | clean |
| 주요 검증 | `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:minifyReleaseWithR8`, `:app:testDebugUnitTest --tests "*MainScreenStartDestinationTest"`, `:app:testDebugUnitTest --tests "*MapScreenLayersTest"`, `:app:testDebugUnitTest --tests "*SettingsPreferenceKeysTest"`, `:app:testDebugUnitTest --tests "*SettingsPreferenceKeysTest" --tests "*NotificationPreferenceKeysTest" --tests "*SettingsEndDateAlarmPlanTest" --tests "*MainActivityKeepScreenAwakeTest"`, `:app:testDebugUnitTest --tests "*Auth*Test" --tests "*StringResourceCoverageTest"` 통과 |
| Release readiness | 2026-06-16에 실제 `keystore.properties` 또는 `WEB_CLIENT_ID`가 없으면 `assembleRelease`/`bundleRelease`가 의도적으로 실패함을 재확인 |
| 남은 성격 | 실기기 전체 회귀, 콘솔/스토어 운영 설정, 최종 iOS 동기화 검증 |

최근 완료된 iOS 패리티/릴리스 하드닝:

- 최신 debug APK를 Android 15 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치하고 launch smoke를 재확인했다. PID `26023`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity`, `dumpsys window lastanr`는 `<no ANR has occurred since boot>`. UIAutomator XML에서 Naver Map controls, 현위치/확대·축소/NAVER logo, 상단 `내 드론`/`드론 2`/드롭다운 원, `비행구역 레이어`, `스케치`, KP/날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195와 height=90이 일치한다. 앱 PID `AndroidRuntime:E` fatal 로그는 없고, `kr-col-ext.nelo.navercorp.com` OkHttp leak warning은 이전과 같은 Naver SDK telemetry endpoint 경고로 구분했다.
- 앱 삭제 후 재설치 동기화 항목 중 외부 계정 없이 확인 가능한 로컬 계약을 재검증했다. Android OS Auto Backup은 `allowBackup=false`이고, API 30 이하 `backup_rules` 및 API 31+ `data_extraction_rules` 모두 root/file/database/sharedpref/external 전체를 제외해 Firebase Auth/FCM deviceId/Room 캐시가 시스템 백업이나 기기 전송으로 복원되지 않는다. FCM device id 저장 키는 iOS와 같은 `DeviceUUID`이고 legacy `fcm_device_id` migration 방어도 유지된다. `:app:testDebugUnitTest --tests "*BackupRulesTest" --tests "*FcmServiceTest"` 통과.
- 알림 탭/포그라운드 팝업 복원 경로에서 클릭 Intent extras 생성을 순수 함수로 분리하고, shapeId만 trim 정규화하며 제목/본문은 빈 문자열과 공백까지 원문 보존하는 iOS `PushNotificationManager` 계약을 회귀 테스트로 고정했다. `:app:testDebugUnitTest --tests "*FcmServiceTest" --tests "*NotificationSchedulerTest"` 및 `:app:assembleDebug` 통과.
- 2026-06-16 최신 HEAD에서 predictive back manifest 보정 이후 전체 `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:minifyReleaseWithR8` 재검증을 통과했다. R8 중 Naver Map SDK stack map table 경고와 Play Services Location companion 경고는 기존 외부 라이브러리 경고로 남지만 빌드는 성공한다.
- 2026-06-16 최신 HEAD에서 release readiness gate를 재확인했다. 현재 로컬에는 `keystore.properties`가 없고 `local.properties`의 `WEB_CLIENT_ID`가 비어 있어 `:app:assembleRelease`와 `:app:bundleRelease`가 release signing 설정 누락 및 Google sign-in Web client ID 누락 메시지를 함께 출력하며 의도적으로 실패한다.
- Android 13+ back dispatcher manifest opt-in을 명시해 드론 관리 시트 back 동작 중 반복되던 `OnBackInvokedCallback is not enabled` 경고를 제거했다. `AndroidManifestContractTest`에 `android:enableOnBackInvokedCallback="true"` 계약을 추가했고, `:app:testDebugUnitTest --tests "*AndroidManifestContractTest"` 및 `:app:assembleDebug` 통과. 최신 debug APK 재설치 후 설정 탭 → 드론 관리 시트 → back smoke에서 MainActivity focus/PID 유지, ANR 없음, 해당 warning 재발 없음 확인.
- 같은 실기기 `RFCW324TZ0Z`에서 저장 탭/설정 탭/드론 관리 시트 비파괴 회귀를 재확인했다. 저장 탭은 `저장 목록`, 정렬 칩 `비행시작일순`/`내림차순`, `활성화` 섹션과 visible saved rows가 렌더링됐고, 설정 탭은 `내 정보`, `로그인 / 회원가입`, `내 드론 관리하기`, `비행 환경`, KP/날씨/알림 섹션이 렌더링됐다. 드론 관리 시트는 `드론 관리`, `내 드론 목록`, 기존 드론 2개, `새 드론 추가`, `사용법` 섹션을 확인했다.
- Firestore 크로스플랫폼 데이터 계약을 `FIRESTORE_CONTRACT.md`로 문서화하고 `README.md`/`MIGRATION_PLAN.md`에서 최신 계약 문서로 연결했다. Shape `shapeType`은 쓰기 소문자 raw value, 읽기 대소문자 무시 정책을 유지한다. `:app:testDebugUnitTest --tests "*ShapeTypeTest" --tests "*ShapeFirebaseStoreTest" --tests "*ShapeFirestoreParsingTest"` 통과.
- 문서/패치노트 inline markdown 렌더링을 iOS `AttributedString(markdown: .inlineOnlyPreservingWhitespace)`처럼 중첩 링크/볼드 조합을 보존하도록 보정했다. `[**NOAA**](url)`와 `**[NOAA](url)**` 모두 link annotation과 bold style이 함께 유지된다. `:app:testDebugUnitTest --tests "*MarkdownInlineTextTest" --tests "*MarkdownParserTest" --tests "*DocumentEntryPolicyTest" --tests "*PatchNotesContentTest"` 및 `:app:assembleDebug` 통과.
- 약관/개인정보 문서 시트의 header padding, divider thickness, error icon, empty/error spacing, error title color를 iOS 문서 시트 토큰에 맞춰 보정했다. `:app:testDebugUnitTest --tests "*DocumentEntryPolicyTest" --tests "*MarkdownInlineTextTest" --tests "*MarkdownParserTest" --tests "*PatchNotesContentTest"` 및 `:app:assembleDebug` 통과.
- 드론 목록/상세/편집/삭제/선택 상태와 Drone Firestore 파싱을 최신 HEAD에서 재대조했다. 코드 변경은 필요 없었고 `:app:testDebugUnitTest --tests "*DroneListScreenTest" --tests "*DroneNameWidthLimitTest" --tests "*DroneEditSheetTest" --tests "*DroneDeleteValidationTest" --tests "*DroneSelectionStateTest" --tests "*DroneFirestoreParsingTest"` 통과.
- 최신 debug APK를 Android 15 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치하고 launch smoke를 재확인했다. PID `3869`, MainActivity focus 유지, ANR 없음. 홈 스크린샷/UiAutomator XML에서 Naver Map, 상단 `내 드론`/`드론 2`/드롭다운 원, KP/날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. 상단 드론 선택 버튼과 원형 드롭다운은 같은 top 기준으로 정렬되어 있고 높이/지름도 같은 시각 기준으로 확인했다. 앱 PID 로그에는 crash/fatal exception이 없으며, `kr-col-ext.nelo.navercorp.com` OkHttp leak 경고 1건은 앱 코드에 없는 Naver SDK telemetry endpoint로 구분했다.
- 새 도형/복제 저장 후 저장 목록으로 포커스되는 경로도 iOS `ShapeEditViewModel.saveShape` → `MoveToShapeNotification` 순서처럼 지도 하이라이트를 즉시 적용하지 않고 카메라 이벤트의 300ms 지연 적용에 맡기도록 보정했다. 상세에서 편집 저장 후 상세 시트로 돌아가는 경우만 즉시 선택 상태를 갱신한다. `:app:testDebugUnitTest --tests "*MapCameraFocusTest"` 및 `:app:assembleDebug` 통과.
- 저장 목록 도형 탭 → 지도 포커스 흐름의 하이라이트 타이밍을 iOS `MapViewModel.moveCameraToShape`처럼 1차 줌 이동 후 300ms 지연 지점에 적용되도록 보정했다. `MoveToShape` 이벤트에 하이라이트 대상 id를 실어 보내고, 저장 목록 포커스 경로의 즉시 지도 선택 갱신을 제거해 iOS의 `updateHighlight` 순서와 맞췄다. `:app:testDebugUnitTest --tests "*MapCameraFocusTest" --tests "*SavedListSectionsTest" --tests "*MainScreenStartDestinationTest"` 및 `:app:assembleDebug` 통과.
- KP 예보 차트 헤더의 우측 note badge를 iOS `KPForecastView`의 `tertiarySystemBackground`/6pt corner/8pt horizontal padding/4pt vertical padding 기준으로 보정했다. Android Material surface 의존을 제거하고 iOS light `#FFFFFF` 배경을 회귀 테스트로 고정했다. `:app:testDebugUnitTest --tests "*KpChartsTest"` 및 `:app:assembleDebug` 통과.
- KP 예보 차트의 loading/error/no-data placeholder 토큰을 iOS `KPForecastView` 기준으로 상수화하고, 에러 아이콘 색상을 iOS `.orange`(`#FF9500`)로 보정했다. placeholder height 200dp, error icon 40dp, icon-text spacing 8dp를 회귀 테스트로 고정했다. `:app:testDebugUnitTest --tests "*KpChartsTest"` 및 `:app:assembleDebug` 통과.
- KP 예보 화면의 전체 콘텐츠 패딩/간격과 현재 KP 카드 내부 토큰을 iOS `KPForecastView` 기준으로 보정했다. Android의 horizontal 8dp/spacedBy 16dp를 iOS `.padding()`/`VStack(spacing: 20)`에 맞춰 16dp/20dp로 바꾸고, 현재 KP 카드의 section spacing 12dp, card padding 16dp, row spacing 12dp, detail leading 10dp, level row spacing 6dp, icon 20dp, background alpha 0.1을 회귀 테스트로 고정했다. `:app:testDebugUnitTest --tests "*KpChartsTest"` 및 `:app:assembleDebug` 통과.
- 날씨 화면의 초기 로딩/초기 에러 표시를 iOS `WeatherForecastView`처럼 전체 화면 대체 UI가 아니라 본문 카드 내부 상태로 흐르게 보정했다. `weatherData == null`이어도 로딩 또는 에러가 있으면 빈 `WeatherData` placeholder로 `WeatherForecastBody`를 유지해 현재 날씨 카드의 200dp 로딩/에러 분기가 실제 초기 상태에도 표시된다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- 현재 날씨 카드의 드론 카테고리 선택 동작을 iOS `fetchWeatherData(forceRefresh: true)` 흐름과 맞췄다. Android도 저장된 좌표 기준선이 있으면 카테고리 선택 시 로딩 상태를 켜고 에러를 초기화한 뒤 weather cache를 무효화해 같은 카테고리 재선택도 실제 새로고침 경로를 타도록 보정했다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- 현재 날씨 카드 하단 면책 문구를 iOS `WeatherForecastView`의 top 8pt padding, 12pt info icon, 4pt spacing, secondary color alpha 1.0 기준으로 보정했다. 기존 Android의 0.7 alpha로 더 흐리게 보이던 처리를 제거하고 회귀 테스트로 고정했다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- 현재 날씨 데이터 그리드 셀을 iOS `weatherDataCard`의 `tertiarySystemBackground`/12pt radius/12pt padding/32pt 아이콘 슬롯/24pt 아이콘/16pt warning icon 기준으로 보정했다. Android Material surface 의존을 제거하고 셀 배경 `#FFFFFF`, 총 최소 높이 62dp를 회귀 테스트로 고정했다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- 현재 날씨 카드의 큰 미리보기 블록을 iOS `WeatherForecastView`의 `tertiarySystemBackground`/12pt radius/16pt padding/64pt 아이콘 + 좌우 15pt padding 기준으로 보정했다. Android Material surface 의존을 제거하고 iOS light `#FFFFFF` 배경, 94dp 아이콘 슬롯 폭을 회귀 테스트로 고정했다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- 현재 날씨 카드의 빈 시간별 예보 상태를 iOS `WeatherForecastView.currentWeatherCard` 분기와 맞췄다. `hourlyForecast.isEmpty && isLoading`이면 카드 내부 200dp 로딩 상태를 표시하고, `hourlyForecast.isEmpty && error != null`이면 카드 내부 경고 아이콘/에러 문구를 표시하며, 기존 hourly forecast가 있으면 iOS처럼 로딩/에러 중에도 기존 데이터 카드를 유지한다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- 현재 날씨 카드의 재로딩 인디케이터를 iOS `WeatherForecastView`처럼 기존 hourly forecast가 있을 때만 표시하도록 보정했다. Android의 단순 `isLoading` 조건을 `isLoading && hourlyForecast.isNotEmpty()`로 바꾸고, 8dp padding/8dp corner/`secondarySystemBackground` 0.9 alpha 배경/16dp spinner/2dp stroke 토큰을 회귀 테스트로 고정했다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- 날씨 화면의 일출/일몰 카드와 현재 날씨 카드도 iOS `WeatherForecastView`의 공통 카드 토큰(`secondarySystemBackground`, 16pt radius, 16pt padding, 12pt spacing)으로 보정했다. `IosWeatherForecastCard*` 공통 상수를 추가하고 차트 카드도 이 값을 참조하게 정리해 날씨 화면 카드들이 같은 iOS 배경/간격 기준을 공유한다. 최초 샌드박스 테스트는 Gradle wrapper lock 권한으로 실패했지만 외부 권한 재실행은 통과했고, `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*SunTimelineStateTest"` 및 `:app:assembleDebug` 통과.
- KP 48시간/27일 예보 차트 카드도 iOS `KPForecastView`의 `secondarySystemBackground`/16pt radius/16pt padding/12pt spacing 기준으로 보정했다. 기존 Android의 `surfaceVariant.copy(alpha = 0.4f)` 배경을 `#F2F2F7` 고정색으로 바꾸고 카드 토큰을 상수화해 회귀 테스트로 고정했다. `:app:testDebugUnitTest --tests "*KpChartsTest"` 및 `:app:assembleDebug` 통과.
- 날씨 예보 차트 카드 배경을 iOS light `UIColor.secondarySystemBackground` 값인 `#F2F2F7`로 명시했다. Material3 Card 기본색에 맡기지 않고 `CardDefaults.cardColors(containerColor = IosWeatherChartCardContainerColor)`를 사용하며, ARGB 값을 회귀 테스트로 고정했다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- 날씨 예보 차트 카드의 시각 토큰을 iOS `WeatherForecastView`의 `VStack(spacing: 12).padding().cornerRadius(16)` 기준으로 보정했다. Android `ChartCard`의 corner radius를 16dp로 올리고, 내부 padding 16dp와 헤더-차트 spacing 12dp를 상수화해 회귀 테스트로 고정했다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- 날씨 예보 차트 제목을 iOS `WeatherForecastView`의 dynamic title 경로와 맞췄다. Android의 고정 `온도 예보 (3일)` 문자열을 `온도 예보 (%1$s)` + `weather_forecast_days`/`weather_forecast_hours` 조합으로 바꾸고, `WeatherForecastDays = 3`, `WeatherForecastChartHours = 72` 상수를 iOS `FORECAST_DAYS` 기준으로 고정했다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*StringResourceCoverageTest"` 및 `:app:assembleDebug` 통과.
- 날씨 예보 차트 헤더의 우측 단위 표기를 iOS `WeatherForecastView`처럼 추가했다. `ChartCard`가 선택적 `unitLabel`을 받아 제목 오른쪽에 보조 텍스트로 표시하고, 온도/풍속/순간풍속/강수량/가시거리/CRI 차트가 iOS String Catalog의 `weather.unit.celsius`/`mps`/`mmph`/`km`/`custom` 값(`단위: °C`, `단위: 자체단위사용` 등)을 사용한다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*StringResourceCoverageTest"` 및 `:app:assembleDebug` 통과.
- 날씨 예보 차트의 기준선 범례를 iOS `WeatherForecastView`처럼 차트 아래에 추가했다. 온도/풍속/순간풍속/가시거리/CRI 차트가 16x8 색상 스와치와 iOS String Catalog 기준 문구(`저온 ≤ -10°C`, `주의 ≥ %.1fm/s`, `위험 ≤ 2km` 등)를 표시하며, 강수량 차트는 iOS처럼 별도 범례 없이 유지한다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*StringResourceCoverageTest"` 및 `:app:assembleDebug` 통과.
- 날씨 예보 차트의 기준선을 iOS `WeatherForecastView`의 `RuleMark` 색상/두께에 맞춰 보정했다. 공용 차트에 값별 색상을 받는 `WeatherThresholdLine`을 추가하고, 온도 차트에는 저온 파랑/고온 주황 점선, 풍속·순간풍속에는 주의 주황/위험 빨강 점선, 가시거리에는 위험 빨강/양호 초록 점선, CRI에는 주의 노랑/경고 빨강 점선을 사용한다. 기존 `warningThreshold`/`dangerThreshold` 경로는 KP 등 기존 호출을 위해 유지한다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- 날씨 CRI 예보 차트의 Y축 범위를 iOS `WeatherForecastView`의 `.chartYScale(domain: 0...100)`처럼 0..100 고정 범위로 보정했다. 다른 날씨 차트의 자동 범위는 유지하고 CRI 차트만 iOS 전용 범위를 명시한다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- 날씨 온도 예보 차트의 Y축 범위를 iOS `WeatherForecastView.calculateTemperatureYRange()`처럼 데이터 최저/최고값에 각각 5도 여백을 주고 5도 단위로 내림/올림한 뒤 `-30...50` 범위로 제한하도록 보정했다. Android 공용 차트의 10% 자동 패딩 대신 온도 차트만 iOS 전용 범위를 명시한다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- 날씨 예보 차트의 현재 시각 표시를 iOS `WeatherForecastView`의 `RuleMark.annotation`처럼 빨간 수직선 + 상단 `현재`/`Now` 배지로 보정했다. KP 차트는 iOS처럼 라벨 없는 현재 시각 선을 유지하고, 날씨 차트에서만 `weather.chart.current` 대응 리소스를 사용한다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*StringResourceCoverageTest"` 및 `:app:assembleDebug` 통과.
- 날씨 예보 차트의 `PointMark`/색상/강수량 표시를 iOS `WeatherForecastView`에 더 가깝게 보정했다. 온도/풍속/순간풍속/강수량/가시거리/CRI 라인 차트에 포인트 표시를 추가하고, 풍속 green·순간풍속 indigo·강수량 blue·가시거리 purple·CRI cyan 기준으로 색상을 맞췄다. 강수량은 Android 전용 bar chart 대신 iOS처럼 area/line/point 차트와 `0...max(10, ceil(max*1.2))` Y축 범위를 사용한다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*KpChartsTest"` 및 `:app:assembleDebug` 통과.
- KP 48시간/27일 예보 차트의 라인 색상을 iOS `LineMark.foregroundStyle(KPLevel.level(...).color)`처럼 KP 레벨별 세그먼트 색상으로 보정했다. 일반 날씨 차트의 기존 단일 smooth line 동작은 유지하고, KP처럼 색상 목록이 명시된 경우만 segment line으로 그린다. `:app:testDebugUnitTest --tests "*KpChartsTest" --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- KP 48시간/27일 예보 차트의 각 포인트 값 라벨을 iOS `KPForecastView`의 `PointMark.annotation`처럼 표시하도록 보정했다. 공용 `WeatherLineChart`에 선택적 `pointLabels`를 추가하고, 라벨이 있을 때만 상단 여백을 늘려 최상단 KP 값도 잘리지 않게 했다. `:app:testDebugUnitTest --tests "*KpChartsTest" --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- 날씨 예보 강수량 차트의 X축 라벨도 iOS `WeatherForecastView`처럼 자정에는 `MM/dd` 날짜 라벨을 쓰도록 보정했다. Android 일반 라인 차트는 이미 같은 규칙을 쓰고 있었지만, 강수량 바 차트만 `HH` 고정이어서 다일 예보에서 날짜 경계가 iOS와 달랐다. 공용 `formatIosTimeChartAxisLabel`로 통일하고, `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*StringResourceCoverageTest"` 및 `:app:assembleDebug` 통과.
- 날씨 예보 현재 날씨 카드를 iOS `WeatherForecastView.currentWeatherCard`처럼 현재 weather 데이터가 없어도 카드 구조를 유지하도록 보정했다. Android는 `data.current == null`이면 섹션을 생략했지만, iOS `WeatherManager` computed 문자열들은 `"-"`/`weather.unknown` fallback으로 카드 안 값을 유지한다. Android도 현재 카드에서 온도/풍속/풍향/돌풍/강수/가시거리/CRI fallback을 맞추고, 현재 강수 표기를 iOS `precipitationString`과 같은 `mm/h` 단위로 정리했다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*StringResourceCoverageTest"` 및 `:app:assembleDebug` 통과.
- KP 예보 현재 지수 카드를 iOS `KPForecastView.currentKPCard`처럼 현재 KP 데이터가 없어도 카드 구조를 유지하도록 보정했다. iOS `KPIndexManager.currentKPString`은 `currentKP == nil`일 때 `"-"`를 표시하고 `currentLevel` 기본값 `.normal`로 레벨/설명/GFZ 출처를 계속 보여주므로, Android도 `데이터가 없습니다` 대체 카드 대신 `-` + Normal 레벨/설명/출처를 렌더링한다. `:app:testDebugUnitTest --tests "*KpChartsTest"` 및 `:app:assembleDebug` 통과.
- KP 예보 화면과 설정/지도 시트 헤더의 새로고침 버튼 활성화 정책을 iOS `KPForecastView` toolbar처럼 로딩 중에도 유지되도록 보정했다. 기존 Android는 `isLoading` 중 새로고침 버튼을 비활성화했고 전체 화면 toolbar에 별도 스피너도 표시했지만, iOS는 버튼을 disable하지 않고 toolbar 스피너도 두지 않으므로 공용 `isKpRefreshActionEnabled` 정책과 `KpToolbarShowsLoadingIndicator = false` 기준으로 통일했다. `:app:testDebugUnitTest --tests "*KpChartsTest"` 및 `:app:assembleDebug` 통과.
- 날씨 예보 화면과 설정/지도 시트 헤더의 새로고침 버튼 활성화 정책을 iOS `WeatherForecastView` toolbar처럼 로딩 중에도 유지되도록 보정했다. 기존 Android는 `isLoading` 중 새로고침 버튼을 비활성화했지만, iOS는 버튼을 disable하지 않으므로 공용 `isWeatherRefreshActionEnabled` 정책으로 전체 화면/시트 헤더를 통일했다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest"` 및 `:app:assembleDebug` 통과.
- 날씨 정보 가이드의 드론 카테고리 선택기를 iOS `WeatherInfoView.droneCategorySelector` 토큰에 맞춰 보정했다. Android 정보 가이드에서 텍스트 `⌄` 대신 chevron 아이콘을 사용하고, 선택기 패딩을 iOS horizontal 8 / vertical 4, 아이콘 12dp 기준으로 고정했다. `:app:testDebugUnitTest --tests "*InfoGuideSheetsTest" --tests "*StringResourceCoverageTest"` 및 `:app:assembleDebug` 통과.
- 시간별 날씨 예보의 순간 풍속 증가량을 iOS `WeatherManager.hourlyForecast.map`과 맞췄다. 현재 날씨 위험도 평가는 iOS처럼 돌풍값 누락 시 평균풍 x 1.3 추정을 유지하지만, 시간별 예보 데이터는 iOS처럼 관측 돌풍이 없으면 `gust = meanWind`로 보고 `gustDifference = 0`을 저장한다. `:app:testDebugUnitTest --tests "*GustDifferenceCalculatorTest" --tests "*WeatherRepositoryTest" --tests "*WeatherForecastParityTest"`, `:app:assembleDebug`, `:app:testDebugUnitTest` 통과.
- 최신 HEAD `96b03d0` 기준으로 `:app:lintDebug`를 재실행해 통과 확인. 최초 샌드박스 실행은 Gradle wrapper cache lock(`/Users/david/.gradle/...zip.lck`) 접근 권한으로 실패했지만, 외부 권한 재실행은 `BUILD SUCCESSFUL`이며 lint report는 `app/build/reports/lint-results-debug.html`에 생성됐다.
- iOS `ShapeFirebaseStore`와 Android `ShapeFirebaseStore`의 타입별 geometry 파싱을 재대조했다. iOS 실제 파서는 rectangle/polygon/polyline geometry가 없으면 nil로 둔 뒤 "있으면 검증"하는 관대한 상태지만, 공유 Firestore 계약은 타입별 geometry를 필수로 둔다. Android는 계약대로 rectangle `secondCoordinate`, polygon/polyline 좌표 최소 개수를 엄격히 유지하며, 이 차이는 Android 완화가 아니라 iOS 파서/검증 보강 후보로 기록한다.
- 최신 문서 커밋 `9dd4d95` 이후 `:app:minifyReleaseWithR8`를 재실행해 통과 확인. R8는 기존과 같은 Naver Maps SDK stack map table warning과 Play Services Location companion warning을 출력하지만 build failure는 아니다.
- Firestore optional 필드 보강 커밋 `7efb38c` 이후 전체 `:app:testDebugUnitTest` 재통과. 최신 debug APK를 Android 15 실기기 `RFCW324TZ0Z`에 `adb install -r`로 데이터 유지 재설치하고 cold launch smoke를 수행했다. `LaunchState: COLD`, `TotalTime: 1904`, `WaitTime: 1907`, PID `14723`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. 홈 UIAutomator XML에서 Naver Map controls, 현위치/확대·축소/NAVER logo, 상단 `내 드론`/`드론 2`/드롭다운 원, `비행구역 레이어`, `스케치`, KP/날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했다. 이어서 같은 PID에서 하단 `저장`/`설정` 탭 비파괴 smoke를 수행했다. 저장 탭 XML에서 `저장 목록`, 정렬 칩 `비행시작일순`/`내림차순`, `활성화` 섹션, visible saved rows, 선택된 `저장` 탭을 확인했고, 설정 탭 XML에서 `설정`, `내 정보`, `로그인 / 회원가입`, `내 드론 관리하기`, `비행 환경`, `현재 KP 지수: 0.7`, `현재 날씨`, `알림`, 선택된 `설정` 탭을 확인했다. 지도 탭 복귀 후 PID/focus가 유지됐고 ANR은 없었다. 앱 PID 로그에 `NaverMapDebug: 네이버 지도 준비 완료`가 있으며 앱 `FATAL EXCEPTION`/`ThemeUtils` 오류는 없었다.
- Shape/Sketch/Drone Firestore 쓰기에서 선택 필드가 null로 저장되지 않도록 표준 문서 데이터와 merge 저장 payload를 분리했다. 문서 데이터는 iOS처럼 값이 있는 필드만 포함하고, Android merge 저장 시에는 누락된 선택 필드를 `FieldValue.delete()`로 정리해 기존 null/stale 필드가 남지 않도록 보정. `shapeType` 소문자 쓰기/대소문자 무시 읽기 계약은 유지. `:app:testDebugUnitTest --tests "*ShapeFirebaseStoreTest" --tests "*SketchFirebaseStoreTest" --tests "*DroneFirestoreParsingTest" --tests "*ShapeFirestoreParsingTest" --tests "*ShapeValidationTest"` 및 `:app:assembleDebug` 통과.
- 포그라운드 복귀 변경 감지 프롬프트의 확인 버튼도 iOS `ChangeDetectionManager.performSync()`처럼 Shape 동기화 결과만 완료/실패 다이얼로그에 반영하도록 분리했다. Android 실시간 리스너 복구는 유지하지만, `도형 정보` 프롬프트가 Drone/Sketch 전체 동기화 실패에 막히지 않도록 보정. `:app:testDebugUnitTest --tests "*RealtimeSyncManagerTest" --tests "*AuthViewModelForegroundSyncTest"` 및 `:app:assembleDebug` 통과.
- 포그라운드 복귀 fallback 변경 감지 프롬프트를 iOS `ChangeDetectionManager`처럼 Shape `metadata/server` 기준으로 제한했다. Android 실시간 리스너와 수동/로그인 동기화의 Sketch 처리 경로는 유지하되, `sync.alert.detected.message`가 "도형 정보"를 안내하는 프롬프트는 sketch-only metadata 변경으로 뜨지 않도록 보정. `:app:testDebugUnitTest --tests "*RealtimeSyncManagerTest" --tests "*AuthViewModelForegroundSyncTest"` 및 `:app:assembleDebug` 통과.
- 최신 debug APK를 Android 15 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치하고 cold launch + 하단 탭 smoke를 다시 확인. `LaunchState: COLD`, `TotalTime: 1967`, `WaitTime: 1971`, PID `12120`, MainActivity focus 유지. 홈 UI에서 Naver Map controls, 현위치/확대·축소/NAVER logo, 상단 `내 드론`/`드론 2` 선택 버튼과 드롭다운 원, `비행구역 레이어`, `스케치`, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했다. 저장 탭은 `저장 목록`/`활성화`/저장 행, 설정 탭은 `설정`/`내 정보`/`로그인 / 회원가입`/`내 드론 관리하기`/`비행 환경`/`현재 KP 지수`/`현재 날씨`가 렌더링됐고, 지도 복귀 후 focus/PID가 유지됐다. 앱 PID logcat 필터에는 `NaverMapDebug: 네이버 지도 준비 완료`만 있으며 `FATAL EXCEPTION`/`ThemeUtils` 앱 오류는 없었다.
- 동기화 진행 제목과 프로필 동기화 진행 상태 문구를 iOS String Catalog처럼 단일 말줄임표가 아닌 `...` 표기로 보정. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*ProfileViewModelTest" --tests "*AuthViewModelForegroundSyncTest"` 및 `:app:assembleDebug` 통과.
- iOS 실제 빌드 리소스가 `Localization/Localizable.xcstrings`임을 재확인하고, 직전 `.strings` 기준 문자열 보정을 String Catalog 기준으로 재정렬. 도형 상세/도형 편집/드론 목록·상세·편집 라벨은 `고도(m)`, `수정하기`, `복제하기`, `좌표를 입력하세요`, `주소를 검색하세요`, `드론 관리`, `내 드론 목록`, `제작 번호`, `이륙 무게` 등 `.xcstrings` 값을 따른다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*ShapeDetailDroneResolutionTest" --tests "*ShapeEditDefaultsTest" --tests "*DroneListScreenTest" --tests "*DroneDeleteValidationTest"` 및 `:app:assembleDebug` 통과.
- 저장 목록 swipe 삭제를 iOS `SavedTableListView`처럼 목록 선택/상세 상태를 건드리지 않고 soft delete만 수행하도록 보정하고, 상세 시트 삭제만 selection/detail/edit state를 정리하도록 분리. `:app:testDebugUnitTest --tests "*SavedListSectionsTest" --tests "*MapCameraFocusTest" --tests "*MainScreenStartDestinationTest"` 및 `:app:assembleDebug` 통과.
- 로컬 알림 수신 로그가 알림 제목과 `shapeId` 원문을 남기지 않도록 보정. 알림 표시/클릭 payload는 기존처럼 원문을 보존하고, 로그에는 제목 길이와 shapeId 존재 여부만 남긴다. `:app:testDebugUnitTest --tests "*FcmServiceTest"`, `:app:assembleDebug`, `:app:lintDebug` 통과.
- 전체 unit test 재확인 후 최신 debug APK를 Android 15 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치하고 cold launch smoke 재확인. `LaunchState: COLD`, `TotalTime: 2104`, `WaitTime: 2110`, PID `32379`, MainActivity focus 유지. Naver Map 준비 완료, `FATAL EXCEPTION` 없음. 접근성 bounds 기준 상단 드론 선택 버튼 `[401,195][657,285]`, `[680,195][922,285]`와 드롭다운 원 `[945,195][1035,285]`가 모두 높이 90px, 상단 y=195로 일치함을 확인.
- 프로필/날씨/KP/도형 저장 실패 보정 커밋 이후 전체 unit test `:app:testDebugUnitTest` 재확인 통과.
- 도형 추가/수정 저장 실패를 iOS `ShapeEditViewModel`처럼 편집 시트 Alert에 `도형 추가/수정 중 오류가 발생했습니다: 원인` 형식으로 표시하도록 보강하고, 저장 실패 시 시트를 닫지 않도록 처리. `:app:testDebugUnitTest --tests "*ShapeEditDefaultsTest" --tests "*StringResourceCoverageTest"` 및 `:app:assembleDebug` 통과.
- KP 실패 문구를 iOS `KPIndexManager`처럼 요청 소스 구분 없이 `KP 지수 데이터를 가져올 수 없습니다` 단일 사용자 표시로 보정하고 `Kp` 표기를 `KP`로 정리. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest"` 및 `:app:assembleDebug` 통과.
- 날씨 API 실패 표시를 iOS `WeatherManager`처럼 원인 `localizedDescription`이 있을 때 본문에 포함하도록 보정하고, 빈 문자열/공백 원인도 원문 경계대로 유지. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*StringResourceCoverageTest"` 및 `:app:assembleDebug` 통과.
- 프로필 수동 백업/탈퇴 오류 문구 선택을 iOS처럼 null일 때만 fallback하도록 명시하고, 수동 백업 실패의 null fallback을 하드코딩 `Unknown` 대신 공용 unknown 리소스로 보정. `:app:testDebugUnitTest --tests "*ProfileViewModelTest"` 및 `:app:assembleDebug` 통과.
- 로그인 오류 보정 커밋 이후 최신 debug APK를 Android 15 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치하고 cold launch + 하단 탭 smoke 재확인. `LaunchState: COLD`, `TotalTime: 1920`, PID `15369`, MainActivity focus 유지. 홈 UI, 상단 드론 선택 버튼/드롭다운 원 시각 bounds 정렬, 저장/설정 오버레이, Naver Map 준비, 무크래시 로그 확인.
- 최신 debug APK를 Android 15 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치하고 cold launch smoke 재확인. 홈 UI, 상단 드론 선택 버튼/드롭다운 원 정렬, Naver Map 준비, 무크래시 로그 확인.
- 패치노트 렌더링 보정 커밋 이후 최신 debug APK를 같은 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치하고 cold launch smoke 재확인. LaunchState `COLD`, `TotalTime: 1924`, PID `12627`, MainActivity focus, 홈 UI와 Naver Map 준비, `FATAL EXCEPTION` 없음.
- 같은 설치 상태에서 하단 `저장`/`설정` 탭 비파괴 회귀 재확인. 저장 목록 헤더/정렬 칩/활성화 섹션, 설정 헤더/내 정보/로그인 진입점/드론 관리/KP·날씨/알림 섹션이 렌더링되고 지도 탭 복귀 후 PID와 focus가 유지됨.
- 스케치 모드 종료 중 진행 중인 선 저장을 iOS처럼 일반 생성 액션과 동일하게 undo 기록 대상으로 보정. `:app:testDebugUnitTest --tests "*SketchDefaultsTest"`, `:app:testDebugUnitTest --tests "*Sketch*Test"`, `:app:assembleDebug` 통과.
- 로그인/Auth 흐름을 iOS `LoginView`/`AuthManager`/`GoogleLoginManager`/`AppleLoginManager`와 재대조. Android는 provider 취소 무시, 사용자 문서 `appleUserID`/`googleUserID`/`lastLogin`, 계정 전환 reset-before-finalize, 로그인 후 cloud backup 활성화 + realtime/full sync + FCM 요청 순서를 유지한다. provider 계정 복구에서 Android가 `sketches`까지 이전하는 것은 데이터 보존 목적의 방어이며, iOS `AuthManager.migrateUserData`의 `sketches` 미이전은 별도 iOS 보강 후보로 계속 남긴다.
- 앱 정보/패치노트 화면을 iOS `AppInfoView`/`PatchNotesView` 기준으로 재대조하고, 패치노트 title/feature title/description 표시 조건을 Swift `isEmpty` 의미와 맞게 보정. `:app:testDebugUnitTest --tests "*PatchNotesContentTest" --tests "*MarkdownParserTest" --tests "*DocumentEntryPolicyTest"` 및 `:app:assembleDebug` 통과.
- Naver reverse geocode 주소 조립을 iOS `NaverGeocodingService.reverseGeocode`처럼 응답에 존재하는 빈 문자열/공백 문자열을 제거하지 않고, 도로명 `land.name`이 nil이면 건물번호도 붙이지 않도록 보정. `:app:testDebugUnitTest --tests "*GeocodingRepositoryTest" --tests "*SearchAddressSheetTest" --tests "*ShapeEditDefaultsTest"` 및 `:app:assembleDebug` 통과.
- VWorld 고도 문자열 포맷을 iOS `AltitudeFormatter.format`처럼 파싱 실패 시 원문을 반환하고, 숫자 추출에서 일반 공백만 제거하도록 보정. `:app:testDebugUnitTest --tests "*AltitudeFormatterTest" --tests "*VWorldZoneDetailSheetTest" --tests "*VWorldModelsTest"` 및 `:app:assembleDebug` 통과.
- FCM 제목/본문 fallback, Sketch Firestore 파서, 드론 편집 optional 필드 경계를 iOS와 재대조. Android의 FCM `notification_title`/`notification_body` 빈 문자열 보존과 Sketch 관대 파싱 계약은 유지하고, 드론 optional 필드 비우기 가능 동작은 iOS 구현상 nil 파라미터 한계보다 데이터 수정에 유리하므로 Android 방어로 유지한다. `:app:testDebugUnitTest --tests "*FcmServiceTest" --tests "*SketchFirebaseStoreTest" --tests "*DroneEditSheetTest"` 통과.
- 주소 검색 오류 메시지를 iOS `SearchAddressViewModel`처럼 원인 메시지가 빈 문자열/공백 문자열이어도 prefix 뒤에 그대로 표시하도록 보정. `:app:testDebugUnitTest --tests "*SearchAddressSheetTest" --tests "*ShapeEditDefaultsTest"` 및 `:app:assembleDebug` 통과.
- 드론 삭제 실패 알림도 iOS `DroneDetailView`처럼 오류 description 문자열을 공백 포함 원문 그대로 표시하도록 보정. `:app:testDebugUnitTest --tests "*DroneListScreenTest" --tests "*DroneDeleteValidationTest"` 및 `:app:assembleDebug` 통과.
- 날씨 데이터 카드 보조 텍스트 표시 조건을 iOS `weatherDataCard`처럼 null 여부만 보도록 보정해 빈 문자열/공백 문자열도 subText가 있는 상태로 렌더링한다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*WeatherOverlayCardTest"` 및 `:app:assembleDebug` 통과.
- 로그인 오류 다이얼로그 본문도 iOS `LoginView`의 `localizedDescription ?? unknown` 동작처럼 빈 문자열/공백 문자열을 fallback으로 대체하지 않고 그대로 표시하도록 보정. `:app:testDebugUnitTest --tests "*AuthViewModelForegroundSyncTest"` 및 `:app:assembleDebug` 통과.
- Firestore `shapeType` 크로스플랫폼 계약을 최신 HEAD에서 재확인. Android 쓰기는 소문자 raw value만 사용하고, 읽기는 레거시 `CIRCLE`/`Circle`을 허용한다. `:app:testDebugUnitTest --tests "*ShapeTypeTest" --tests "*ShapeFirestoreParsingTest" --tests "*ShapeFirebaseStoreTest"` 통과.
- `FlightPermissionResult.details`를 iOS처럼 금지/승인필요/주의 결과에는 `레이어명: zoneCode 또는 레이어명` 목록으로 채우고, 비행 가능 결과에는 빈 목록을 유지하도록 보정. `:app:testDebugUnitTest --tests "*FlightZoneCalculatorTest" --tests "*VWorld*Test" --tests "*FlightZone*Test"` 및 `:app:assembleDebug` 통과.
- Shape/Sketch Firestore optional 숫자 읽기를 iOS처럼 관대하게 보정했다. 쓰기는 기존처럼 Double 표준을 유지하되, 읽기에서는 Firestore가 정수 Number로 돌려주는 `radius`/`height`/`strokeWidth`/`opacity`를 Double로 복구하고 문자열은 계속 누락/default 처리한다. 좌표와 Timestamp 필드는 공유 계약대로 엄격하게 유지한다. `:app:testDebugUnitTest --tests "*ShapeFirestoreParsingTest" --tests "*SketchFirebaseStoreTest"` 및 `:app:assembleDebug` 통과.
- 스케치 전체 삭제 영어 확인 메시지는 iOS String Catalog처럼 단수/복수 모두 `Are you sure you want to delete all %d sketches?` 문장을 쓰는 상태임을 재확인했다. `StringResourceCoverageTest`가 English `one`/`other` 분기를 모두 고정하며, `:app:testDebugUnitTest --tests "*StringResourceCoverageTest"` 통과.
- `5f2990a fix: tolerate partial shape coordinate arrays`
- `3abc1d7 fix: filter invalid vworld geometry rings`
- `6a6ab97 fix: tolerate invalid sun alarm offsets`
- Shape Firestore 배열 좌표 범위 회귀 테스트 추가
- `22efc0d test: cover manifest notification contracts`
- `e7cdcea fix: reconcile restored end date alarms`
- `fix: match iOS add shape labels`
- `fix: match iOS teal palette label`
- `fix: match iOS shape address placeholder`
- `fix: match iOS drone detail strings`
- `fix: match iOS drone list titles`
- `5a256fb fix: persist app language selection`
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
- Shape Firestore polygon/polyline 읽기를 iOS 파서처럼 손상 좌표 원소만 제외하고, 남은 좌표가 타입별 최소 개수 미달이면 skip하도록 보정

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
- 계정/로그인/로그아웃/계정삭제/실시간 동기화 흐름
- Sketch Firestore 직렬화/파싱 계약
- 저장 목록 도형 탭 → 지도 포커스/줌/하이라이트 흐름
- 저장 목록 오버레이 헤더/정렬 칩 토큰, 아이콘, 문자열, 순환 동작
- Shape/Drone/Sketch Firestore 쓰기 표준과 레거시 읽기 방어 계약
- 실시간 Sketch 동기화 상태는 iOS처럼 공용 `lastSyncTime`이 아니라 별도 `lastSketchSyncTime`만 갱신하는 분리 모델
- 주소 검색/좌표 입력/지도 롱프레스 도형 생성 흐름
- 도형 생성/편집/복제/삭제 상태 전이와 편집 취소 변경 감지 정책
- 도형 편집 좌표 placeholder는 iOS 빌드 리소스에 포함된 `Localizable.xcstrings` 기준 `좌표를 입력하세요`가 최신 값이며, Android `shape_edit_coordinate_placeholder`와 일치함
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
- 도형 편집 주소 fallback placeholder를 iOS `shape.edit.address.placeholder` 기준인 `주소를 검색하세요`로 보정
- 도형 편집 날짜/시간 선택 완료 버튼을 iOS `dateTime.done` 기준인 `선택 완료`로 보정
- 공용 알 수 없는 오류 fallback 문구를 iOS `common.error.unknown` 기준으로 보정
- 주소 검색 선택/표시/건물명 판정을 iOS처럼 빈 문자열 기준으로 보정
- 도형 편집 저장 시 공백 제목/메모/주소를 iOS처럼 실제 입력값으로 보존하고, Firebase 쓰기 검증은 공백 제목을 계속 거부
- 드론 상세 선택 필드/메모 표시와 복사 조건을 iOS처럼 `nil`과 빈 문자열 기준으로 보정
- 드론 목록/상세/편집/삭제 문구를 최신 iOS String Catalog 기준으로 보정
- 드론 상세/편집의 한국어 `제작 번호`, `이륙 무게`, 삭제/연결 도형 처리, 중복 이름 오류 문구를 최신 iOS String Catalog 기준으로 보정
- 드론 목록 화면 title/section header를 최신 iOS String Catalog 기준인 `드론 관리`/`내 드론 목록`으로 보정
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
- 패치노트 title/feature title/description 표시 조건을 iOS처럼 공백 문자열을 빈 값으로 취급하지 않는 `isEmpty` 기준으로 보정
- Naver reverse geocode 주소 조립에서 iOS처럼 빈 문자열/공백 문자열 구성 요소를 보존하고 도로명 nil이면 건물번호를 붙이지 않도록 보정
- VWorld 고도 formatter의 빈 문자열/공백/개행/탭 경계를 iOS `AltitudeFormatter` 기준으로 보정
- Shape 읽기는 iOS 파서처럼 빈 문자열/공백 제목을 보존하고, 쓰기 검증은 공백 제목을 계속 거부
- 빈 제목 기존 Shape도 iOS처럼 기존 도형 편집으로 취급해 드론/반경/고도/비행 기간 초기값을 보존
- Drone 읽기는 iOS Codable 파서처럼 빈 문자열/공백 이름을 보존하고, 쓰기 검증은 공백 이름을 계속 거부
- Shape 쓰기는 iOS `ShapeFirebaseStore`처럼 `memo`/`address`가 `null`이면 Firestore에 빈 문자열로 저장
- 메인 저장/설정 오버레이 전환을 iOS처럼 이동과 opacity 결합 전환으로 보정
- 스케치 오버레이를 iOS처럼 현재 지도 bounds와 겹치는 스케치만 렌더링하도록 보정
- VWorld 상세 고도 행을 iOS `altitudeInfo`처럼 상한/하한이 모두 있을 때만 표시하도록 보정
- 일출/일몰 알림 재예약을 iOS처럼 실제 사용자 위치 기반 날씨에만 수행
- 앱 시작/부팅 후 종료일 알림 복원을 iOS `SettingManager.restoreNotificationSchedules`처럼 기존 도형별 알림을 먼저 취소한 뒤 활성 미래 도형만 다시 예약하도록 보정
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
- 앱 언어 변경이 `ComponentActivity`에서도 Android per-app language, 앱 자체 저장값, 런타임 리소스 locale에 함께 반영되어 재시작 후 유지되도록 보정
- 실제 배포 산출물(`assembleRelease`/`bundleRelease`)은 release signing과 Google `WEB_CLIENT_ID`가 모두 설정된 경우에만 생성되도록 차단
- 로그인 약관/개인정보 시트 흐름
- 한국어 도형 문구와 상세 라벨
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

- 2026-06-14 사건 메모: Android 초기 버전이 `shapeType`을 Kotlin enum `name`인 `CIRCLE`로 저장해 iOS에서 해당 Shape 문서 파싱이 실패하고 목록/지도에서 조용히 누락될 수 있었다. 재발 방지를 위해 쓰기는 소문자 raw value만 허용하고, 읽기는 대소문자 무시로 레거시 데이터를 복구한다.
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
- Shape 날짜: 쓰기는 `flightStartDate`/`flightEndDate`만 사용하고 레거시 `startedAt`/`expireDate`를 새로 쓰지 않는다. 읽기는 `flightStartDate`가 없을 때만 `startedAt`을, `flightEndDate`가 없을 때만 `expireDate`를 fallback으로 허용한다.
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
- 2026-06-14 추가 고정: Shape 파싱은 표준 `flightStartDate`가 있으면 레거시 `startedAt`보다 우선하고, 시작일이 `Long`/문자열이면 invalid로 skip한다. Shape 쓰기는 `deletedAt`까지 `Timestamp`로 직렬화하며 `startedAt`/`expireDate` 키를 생성하지 않는 회귀 테스트를 유지한다.
- 2026-06-14 추가 방어: Shape `polygonCoordinates`/`polylineCoordinates` 읽기는 iOS `compactMap` 파서처럼 손상 좌표 원소만 제외한다. 제외 후 polygon 3점 미만, polyline 2점 미만이면 문서 전체를 skip한다. 쓰기는 계속 표준 Double 좌표 map 배열만 허용한다.
- 2026-06-15 추가 방어: Shape optional 숫자(`radius`, `height`)와 Sketch optional 숫자(`strokeWidth`, `opacity`) 읽기는 Firestore가 정수 `Number`로 돌려주는 레거시 값을 Double로 복구한다. 문자열은 계속 `nil`/기본값으로 처리하고, 쓰기 검증은 표준 Double과 범위 제한을 유지한다.
- 2026-06-15 추가 방어: Shape/Sketch/Drone 쓰기에서 선택 필드 값이 없을 때 Firestore 문서에 `null`을 저장하지 않는다. 표준 문서 데이터는 iOS처럼 값이 있는 optional 필드만 포함하고, Android의 merge 저장 payload에는 해당 필드 `FieldValue.delete()`를 넣어 기존 null/stale 값을 정리한다.
- 2026-06-15 재확인: Android Shape 읽기는 공유 계약에 맞춰 rectangle `secondCoordinate`, polygon 3점 이상, polyline 2점 이상을 필수로 유지한다. iOS 현재 파서는 해당 geometry 누락을 nil로 통과시킬 수 있으므로, 플랫폼을 더 맞추려면 Android 완화가 아니라 iOS 검증 강화가 안전하다.

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

2026-06-14 추가 실기기 확인:

- 최신 `cfbfdfc` 기준 `:app:assembleDebug` 성공 후 Android 15(API 35) `SM-A346N` / `RFCW324TZ0Z`에 `adb install -r app/build/outputs/apk/debug/app-debug.apk`로 데이터 유지 설치 확인
- `MainActivity` 실행 후 앱 프로세스 `com.ScienceFiction.DronePassAndroid`가 PID `18005`로 유지됨을 확인
- logcat에서 `AndroidRuntime` fatal crash는 없고, 앱 PID 로그에서 Firebase 초기화, Naver Map `SurfaceView` 생성, 첫 frame available, `NaverMap` GPU 로그까지 확인
- 최초 확인 시 기기가 secure keyguard/NotificationShade 상태를 유지해 ADB `wm dismiss-keyguard`/unlock gesture만으로는 실제 홈 UI XML·스크린샷 검증까지 진행하지 못함
- 이후 기기가 잠금 해제된 상태에서 `monkey -p com.ScienceFiction.DronePassAndroid -c android.intent.category.LAUNCHER 1`로 `MainActivity`를 다시 전면 실행하고 메인 지도 UI XML smoke를 완료
- UIAutomator XML에서 Naver Map controls, 현위치 버튼, 줌 컨트롤, Naver logo, 상단 `내 드론`/`드론 2` 선택 버튼, 드롭다운 원, `비행구역 레이어`, `스케치`, `KP`, 날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 탭 렌더링 확인
- 상단 드론 선택 버튼 bounds는 `내 드론` `[401,195][657,285]`, `드론 2` `[680,195][922,285]`, 드롭다운 원 `[945,195][1035,285]`로 y=195 시작과 높이 90이 모두 일치함을 재확인
- 메인 지도에서 `KP` 버튼 탭 시 `KP 지수 예보` 시트가 열리고, Android 뒤로가기로 시트가 닫히며 메인 지도 UI로 복귀함을 확인
- 메인 지도에서 날씨 카드를 탭하면 `현위치 기반 정보` 시트가 열리고, Android 뒤로가기로 시트가 닫히며 메인 지도 UI로 복귀함을 확인
- `dumpsys window` 기준 포커스는 `com.ScienceFiction.DronePassAndroid/.MainActivity`, 앱 프로세스 PID `18005` 유지. `AndroidRuntime` 로그는 `uiautomator`/`monkey` 실행/종료만 있고 앱 fatal crash 없음
- 현재 HEAD 기준 `:app:testDebugUnitTest`와 `:app:assembleDebug` 성공 확인
- 최신 Shape Firestore 계약 고정 커밋 후 `RFCW324TZ0Z`에 `adb install -r`로 debug APK를 데이터 유지 재설치하고 `MainActivity` cold launch를 재확인했다. `LaunchState: COLD`, `TotalTime: 1926`, PID `20368`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. 홈 UIAutomator XML에서 Naver Map, 현위치, 상단 `내 드론`/`드론 2`, 드롭다운 원, `비행구역 레이어`, `스케치`, `KP`, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했고, 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y와 높이가 일치했다.
- 같은 최신 APK에서 하단 `저장` 탭 진입 시 `저장 목록`, `비행시작일순`, `내림차순`, `활성화`가 렌더링됐고, `설정` 탭 진입 시 `설정`, `내 정보`, `로그인 / 회원가입`, `내 드론 관리하기`, `비행 환경`, `현재 KP 지수`, `현재 날씨`, `알림` 섹션이 렌더링됐다. 탭 전환 후 PID `20368` 유지, `AndroidRuntime:E` fatal 로그 없음 확인.
- 2026-06-14 재개 라운드에서 iOS `RealtimeSyncManager`와 Android `RealtimeSyncManager`를 다시 대조했다. iOS는 Sketch 성공 시 `lastSketchSyncTime`만 갱신하고 공용 `lastSyncTime`/`syncInProgress`는 Shape 경로 전용으로 유지하므로, Android의 `LAST_SKETCH_SYNC_TIME` 분리 저장도 그대로 유지한다.
- 현재 HEAD 기준 `:app:testDebugUnitTest`와 `:app:assembleDebug` 재통과 후 `RFCW324TZ0Z`에 `adb install -r app/build/outputs/apk/debug/app-debug.apk`로 데이터 유지 설치 성공.
- `MainActivity` cold launch 재확인: `LaunchState: COLD`, `TotalTime: 1969`, PID `30795`, window focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지.
- 홈 UIAutomator XML에서 Naver Map controls, 현위치/줌/NAVER logo, 상단 `내 드론`/`드론 2`/드롭다운 원, `비행구역 레이어`, `스케치`, `KP`, 날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링 확인.
- 상단 드론 선택 요소 bounds는 `내 드론` `[401,195][657,285]`, `드론 2` `[680,195][922,285]`, 드롭다운 원 `[945,195][1035,285]`로 y=195 시작과 높이 90이 모두 일치함을 다시 확인.
- 실행 직후 `AndroidRuntime` 로그에는 `uiautomator` 종료 로그만 있고 앱 fatal crash 없음. 앱 PID 로그에서도 Naver Map surface first frame, `NaverMapDebug: 네이버 지도 준비 완료`, GPU 로그 확인 후 PID `30795` 유지.
- 이어서 `:app:minifyReleaseWithR8` 재실행 성공. Naver Map SDK/Play Services Location의 기존 R8 warning은 남지만 빌드는 성공하며, 실제 `assembleRelease`/`bundleRelease`는 signing + `WEB_CLIENT_ID` 설정 전 실패해야 정상인 상태를 유지한다.

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

2026-06-15 현재 로컬/비파괴 실기기 경로 중 1, 8, 9와 메인 지도/저장/설정 탭 smoke는 최신 debug APK에서 재확인했습니다. 2-4는 앞선 저장 도형/드론/스케치 smoke와 단위 테스트 기준을 유지하며, 5-7과 10은 실제 OAuth 설정, 공유 Firestore 계정, FCM payload, 또는 앱 데이터 삭제/재설치가 필요하므로 외부 설정/데이터 백업 확인 후 진행합니다.

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
- 2026-06-14 재확인: `WEB_CLIENT_ID`는 여전히 비어 있고, `keystore.properties` 파일도 없어 `:app:assembleRelease`와 `:app:bundleRelease`가 signing + Google Web client ID 누락 메시지와 함께 의도적으로 실패함
- 2026-06-15 재확인: `WEB_CLIENT_ID`는 여전히 비어 있고, `keystore.properties` 파일은 없으며, `app/google-services.json`의 Android `oauth_client`는 빈 배열이다. 실제 Google 로그인, release artifact, Play Console SHA 지문 산출은 외부 운영 설정 후 진행 가능
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
- 2026-06-13에 앱 언어 변경 지속성을 보정했다. 기존에는 English 선택 직후 현재 프로세스 표시만 바뀌고 `cmd locale get-app-locales com.ScienceFiction.DronePassAndroid`가 `[]`로 남아 재시작 시 한국어로 돌아갔으나, 보정 후 English 선택 시 `[en]`, 강제 종료/재실행 후 `Map`/`Saved`/`Settings` 영어 UI 유지를 확인했다. 이후 한국어로 되돌려 `[ko]`, 강제 종료/재실행 후 `지도`/`저장`/`설정` 한국어 UI 복귀를 확인했고, 최신 debug APK 재설치 후에도 `[ko]`와 한국어 UI가 유지됐다. `AndroidRuntime:E` 크래시 로그는 없음. `:app:testDebugUnitTest --tests "*SettingsLanguageSelectionTest" --tests "*SettingsPreferenceKeysTest" --tests "*DocumentRepositoryTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-13에 드론 목록 화면 title/section header를 iOS `drone.list.title`/`drone.list.section.my`와 다시 대조해 `드론 관리`/`내 드론 목록`, English `Manage Drones`/`My Drones`로 보정했다. 설정 진입 행은 iOS `settings.drone.manage` 기준인 `내 드론 관리하기`/`Manage My Drones`가 맞으므로 유지했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*DroneListScreenTest"` 통과 확인.
- 2026-06-13에 드론 상세/편집 한국어 문자열을 iOS `drone.detail.*`/`drone.edit.*`와 다시 대조해 `제작 번호`, `이륙 무게`, `메모가 없습니다`, 삭제 확인/연결 도형 처리 문구, 입력 footer, 중복 이름 오류 문구를 보정했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*DroneDeleteValidationTest" --tests "*DroneEditSheetTest" --tests "*DroneListScreenTest"` 통과 확인.
- 2026-06-13에 도형 편집 주소 fallback placeholder를 iOS `shape.edit.address.placeholder`와 다시 대조해 `주소를 검색하세요`로 보정했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*ShapeEditDefaultsTest"` 통과 확인.
- 2026-06-13에 iOS 설정의 `ColorPickerView`는 `showColorPicker = true` 진입점이 없는 죽은 시트임을 확인해 Android에는 전역 도형 색상 일괄 변경 UI를 추가하지 않았다. 이어서 iOS `isCloudBackupEnabled` 계약과 맞지 않게 로그인만 되어 있으면 Shape/Drone/Sketch 즉시 Firestore 푸시가 실행되던 Android 경로를 보정했다. 이제 일반 저장/수정/삭제/스케치 완료 즉시 푸시는 로그인 + 클라우드 백업 ON 상태에서만 실행하고, 로그인 직후 병합/수동 백업/로그아웃 전 강제 동기화 경로는 iOS 보호 흐름에 맞춰 유지한다. `:app:testDebugUnitTest --tests "*CloudSyncGateTest" --tests "*ShapeRepositoryTest" --tests "*DroneSyncMergeTest"`, `:app:testDebugUnitTest --tests "*CloudSyncGateTest" --tests "*ProfileViewModelTest" --tests "*SketchDefaultsTest" --tests "*SketchOverlayColorTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug` 통과 확인.
- 2026-06-13에 좌표 입력 후 주소 역변환 실패 흐름을 iOS `CoordinateView`와 다시 대조했다. Android도 실패 시 `coordinate_alert_address_not_found_*` 알림을 띄우고, 확인하면 좌표는 유지하면서 `주소를 찾을 수 없습니다` fallback 주소로 저장한다. `ShapeEditDefaultsTest`와 `MapCameraFocusTest`의 관련 회귀 테스트가 이미 이 계약을 고정하고 있어 코드 변경은 하지 않았다.
- 2026-06-13에 주소 검색 결과 선택/표시를 iOS `SearchAddressView`와 다시 대조했다. Android는 이제 지번 주소가 빈 문자열이면 도로명으로 즉시 대체하지 않고 iOS처럼 빈 값을 보존해 최종 저장 fallback으로 넘기며, 결과 카드도 빈 지번/도로명 행을 표시 대상으로 유지한다. `:app:testDebugUnitTest --tests "*SearchAddressSheetTest"` 통과 확인.
- 2026-06-13에 지도 롱프레스 새 도형 생성 흐름을 iOS `MainView.handleLongPress`와 다시 대조했다. Android도 역지오코딩 성공/실패별 확인 알림, 실패 시 `mainView.address.notFound`와 같은 fallback 주소, 확인 후 도형 편집 시트 진입을 유지한다. `MapCameraFocusTest`와 `StringResourceCoverageTest`가 이 흐름과 문구를 이미 고정하고 있어 코드 변경은 하지 않았다.
- 2026-06-13 주소 검색 빈 지번 처리 보정과 지도 생성 흐름 재확인 후 `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:minifyReleaseWithR8` 전체 회귀를 재실행해 통과 확인. R8는 기존과 같은 Naver Maps SDK stack map table 경고와 Play Services Location companion object 경고를 출력하지만 build failure는 아님.
- 2026-06-13 최신 debug APK를 실기기 `RFCW324TZ0Z`에 `adb install -r`로 재설치 후 MainActivity 실행 smoke를 다시 완료했다. Window focus는 `com.ScienceFiction.DronePassAndroid/.MainActivity`, PID 유지, `AndroidRuntime:E` 크래시 로그 없음. UI dump에서 `지도`, Naver Map controls, 드론 선택 칩/chevron, 하단 `지도`/`저장`/`설정`, 스케치/KP/날씨/새 도형 플로팅 버튼 렌더링을 확인했다.
- 2026-06-13에 FCM/로컬 알림 payload, 예약, 탭 복원 경로를 iOS `PushNotificationManager`/`SettingManager`와 다시 대조했다. Android는 `shapeId`/`shape_id` 포커스 대상 정규화, 제목/본문 원문 보존, 포그라운드 팝업, 종료일 7일 전 알림, 일출/일몰 다음 미래 후보 선택, 부팅/앱 시작 재예약 정책을 유지한다. 실서버 FCM 수신은 외부 payload가 필요하므로 잔여 실검증 항목으로 남긴다.
- 2026-06-13에 앱 삭제/재설치 경계의 코드 방어를 재확인하고, Manifest가 `allowBackup=false`와 방어적 `backup_rules`/`data_extraction_rules` 참조를 계속 유지하도록 회귀 테스트를 보강했다. OS 백업으로 Firebase/Auth/FCM deviceId/Room 캐시가 복원되지 않는 정책은 유지되며, 실제 삭제 후 실계정 Firestore 복구 smoke는 외부 계정 설정 후 진행한다.
- 2026-06-13 백업 규칙 회귀 테스트 보강 후 `:app:testDebugUnitTest --tests "*BackupRulesTest" --tests "*FcmServiceTest" --tests "*SettingsLanguageSelectionTest"`와 `:app:testDebugUnitTest` 전체 단위 테스트 통과 확인.
- 2026-06-13에 Room v1→v3 직접 마이그레이션 계약을 재확인하고 회귀 테스트를 추가했다. v1/v3 스키마는 `shapes`/`drones`/`sketches` 테이블과 iOS geometry 컬럼(`secondLatitude`, `secondLongitude`, `polygonCoordinates`, `polylineCoordinates`)을 유지하고, v2만 geometry 컬럼이 빠져 있으므로 직접 1→3 경로를 보존해야 한다. `:app:testDebugUnitTest --tests "*DronePassDatabaseMigrationContractTest"`와 `:app:testDebugUnitTest` 통과 확인.
- 2026-06-13에 일반 약관/개인정보 Markdown parser를 iOS `FetchWebDocuments.parseMarkdown`와 다시 대조하고, 헤더/단락/표/구분선/대시·불릿 리스트/단일 pipe 단락 처리를 회귀 테스트로 보강했다. `:app:testDebugUnitTest --tests "*MarkdownParserTest" --tests "*MarkdownInlineTextTest" --tests "*DocumentRepositoryTest"`와 `:app:testDebugUnitTest` 통과 확인.
- 2026-06-14에 도형 편집/상세 날짜 표시를 iOS `DateSection`/`localizedDateTime`와 다시 대조했다. Android도 편집 날짜 전용은 medium date, 편집/상세 날짜시간은 medium date + short time 계약을 공통 헬퍼로 유지하며, 저장 목록의 `yyyy-MM-dd` 행 표시와 정렬 tie-breaker는 기존 테스트로 계속 고정한다. `:app:testDebugUnitTest --tests "*ShapeDateFormatsTest" --tests "*ShapeEditDefaultsTest" --tests "*ShapeDetailDroneResolutionTest" --tests "*SavedListSectionsTest" --tests "*SavedShapeListItemTest"`와 `:app:testDebugUnitTest` 통과 확인.
- 2026-06-14에 돌풍 경고 hysteresis를 iOS `WeatherManager.GustDifferenceCalculator`와 다시 대조했다. Android 계산식은 iOS와 동일해 유지하고, class3 국지 돌풍 1표 해제, toy 국지 돌풍 0표 해제, danger→caution 하향 조건을 회귀 테스트로 보강했다. `:app:testDebugUnitTest --tests "*GustDifferenceCalculatorTest"`와 `:app:testDebugUnitTest` 통과 확인.
- 2026-06-14에 Shape ID 계약을 다시 확인했다. Android 새 도형은 UUID를 생성하고, Firebase 저장 전 UUID 검증을 통과한 도형만 `users/{uid}/shapes/{shape.id}`로 쓰며, payload `id`도 같은 값을 유지한다. 읽기는 iOS처럼 UUID가 아니거나 문서 ID와 `id` 필드가 다르면 skip한다. `:app:testDebugUnitTest --tests "*ShapeFirebaseStoreTest" --tests "*ShapeFirestoreParsingTest" --tests "*ShapeValidationTest"`와 `:app:testDebugUnitTest` 통과 확인.
- 2026-06-14에 계정 전환 순서를 iOS `AuthManager.handleLoginSuccess`와 다시 대조해 Android도 로컬 reset을 `finalizeSuccessfulSignIn`의 복구용 UID/provider 저장보다 먼저 수행하도록 보정했다. 계정 전환 취소는 방금 인증된 세션만 로그아웃하고 로컬/복구 키를 보존하며, 진행 시에는 reset 이후 새 계정 sync가 실행된다. `:app:testDebugUnitTest --tests "*AuthViewModelForegroundSyncTest" --tests "*AuthRepositoryUserDocumentTest" --tests "*ProfileViewModelTest"`와 `:app:testDebugUnitTest` 통과 확인.
- 2026-06-14에 실시간 sync trigger timing을 iOS `RealtimeSyncManager`와 다시 대조했다. Android는 iOS 드론-only 변경을 받기 위한 drones 컬렉션 리스너와 Sketch 포함 수동 백업은 유지하고, `resetAndRestartRealtimeSync`의 stop→restart 지연을 iOS와 같은 0.5초로 맞췄다. `:app:testDebugUnitTest --tests "*RealtimeSyncManagerTest" --tests "*AuthViewModelForegroundSyncTest" --tests "*ProfileViewModelTest"`와 `:app:testDebugUnitTest` 통과 확인.
- 2026-06-14에 KP forecast auto refresh를 iOS `KPForecastView` 기준으로 다시 고정했다. Android 자동 갱신은 5분 간격으로 실행하고, 수동/자동 갱신 모두 현재 KP(GFZ)를 다시 요청하지 않고 NOAA 48시간/27일 예보만 강제 갱신한다. `:app:testDebugUnitTest --tests "*KpChartsTest"`와 `:app:testDebugUnitTest` 통과 확인.
- 2026-06-14에 저장 목록 행 탭/복제 저장/지도 오버레이 탭 포커스 흐름을 iOS `SavedTableListView`/`MapViewModel`/`MainTabView`와 다시 대조했다. Android도 저장 행 탭은 반경 기반 2단계 카메라 이동과 지도 하이라이트를 수행하고, 지도 오버레이 탭은 iOS처럼 카메라 이동 없이 저장 목록 포커스만 요청한다. `:app:testDebugUnitTest --tests "*MapCameraFocusTest" --tests "*SavedListSectionsTest" --tests "*MainScreenStartDestinationTest"` 통과 확인.
- 2026-06-14에 Firestore 크로스플랫폼 계약을 재감사했다. Shape 쓰기는 `shapeType.rawValue` 소문자와 `Timestamp`, `{latitude, longitude}` Double map을 유지하고, 읽기는 `ShapeType.parseWireValue`로 레거시 대문자 `CIRCLE`을 허용한다. Drone/Sketch도 UUID, `Timestamp`, hex color, Double 좌표 map 계약을 유지한다. `:app:testDebugUnitTest --tests "*ShapeFirebaseStoreTest" --tests "*ShapeFirestoreParsingTest" --tests "*ShapeTypeTest" --tests "*ShapeValidationTest" --tests "*DroneFirestoreParsingTest" --tests "*DroneValidationTest" --tests "*SketchFirebaseStoreTest" --tests "*SketchValidationTest"`와 `:app:testDebugUnitTest` 통과 확인.
- 2026-06-14에 iOS 설정 `ColorPickerView`와 카메라 `MoveWithoutZoomNotification`을 다시 대조했다. `ColorPickerView`는 `MainTabView`/`SettingsOverlayView`에 sheet 상태가 연결되어 있지만 `SettingView`에 `showColorPicker = true` 진입점이 없고, `MoveWithoutZoomNotification`도 iOS `MapViewModel` receiver만 있으며 sender가 없다. Android의 전역 도형 색상 변경 UI와 `CameraEvent.MoveWithoutZoom` 발행 경로를 새로 만들지 않는 판단을 유지한다.
- 2026-06-14에 실기기 `RFCW324TZ0Z`에서 MainActivity warm launch smoke를 다시 수행했다. `adb shell am start -W -n com.ScienceFiction.DronePassAndroid/.MainActivity`는 `Status: ok`, `TotalTime: 204ms`였고, window focus/PID가 유지됐다. UI dump에서 `지도`, Naver Map controls, 드론 선택 칩/드롭다운 원, KP/날씨 카드, 스케치/새 도형 버튼, 하단 `지도`/`저장`/`설정` 탭 렌더링을 확인했다. 상단 드론 선택 칩 bounds `[401,195][657,285]`, `[680,195][922,285]`와 드롭다운 원 `[945,195][1035,285]`는 y=195 시작과 높이 90으로 일치했고, `AndroidRuntime:E` 크래시 로그는 없었다.
- 2026-06-14에 같은 실기기에서 하단 탭/오버레이 smoke를 추가 수행했다. 저장 탭 진입 시 `저장 목록`, 정렬 칩, `활성화` 섹션과 3개 저장 행이 렌더링됐고, 설정 탭 진입 시 `설정`, `내 정보`, `로그인 / 회원가입`, `내 드론 관리하기`, `비행 환경`, `현재 KP 지수`, `현재 날씨`가 렌더링됐다. 지도 탭 복귀 후 MainActivity focus/PID가 유지됐고 `AndroidRuntime:E` 크래시 로그는 없었다.
- 2026-06-14에 MainActivity 중복 스택 방지를 위해 Manifest `launchMode=singleTop`을 명시했다. 알림 click intent는 이미 `NEW_TASK | CLEAR_TOP | SINGLE_TOP`과 `onNewIntent` 수신 경로를 갖고 있었고, Manifest 계약 테스트를 추가했다. `:app:testDebugUnitTest --tests "*AndroidManifestContractTest" --tests "*FcmServiceTest" --tests "*MainActivityKeepScreenAwakeTest"`, `:app:assembleDebug`, `:app:minifyReleaseWithR8`, `:app:testDebugUnitTest` 통과 확인. 최신 debug APK 재설치 후 MainActivity를 두 번 실행했을 때 두 번째 실행은 기존 top instance로 전달됐고, 알림형 extras intent도 새 Activity 없이 `SmokeTitle`/`SmokeBody` 팝업으로 렌더링됐다. dumpsys에서 task `sz=1`, `launchMode=1`, PID 유지, `AndroidRuntime:E` 없음 확인.
- 2026-06-14에 실기기 `RFCW324TZ0Z`에서 메인 플로팅 정보 진입 smoke를 추가 수행했다. KP 카드는 `KP 지수 예보`, `현재 KP 지수`, `향후 48시간 예보`, `장기 예보 (27일)`을 렌더링했고, 날씨 카드는 `현위치 기반 정보`, `일출/일몰 정보`, `현재 날씨`, `드론 무게`, `풍속`, `돌풍 차이`를 렌더링했다. 비행구역 레이어 버튼은 데이터 변경 없이 목록 진입만 확인했으며 `드론 비행 구역`, `선택된 레이어 0 / 13`, `전체 선택`, `전체 해제`, 주요 레이어 목록을 렌더링했다. 지도 복귀 후 MainActivity focus/PID `30764`가 유지됐고 `AndroidRuntime:E` 크래시 로그는 없었다.
- 2026-06-14에 비행구역 레이어 시트의 상단 navigation header가 edge-to-edge 상태에서 status bar 영역과 겹쳐 `완료` 버튼 상단 탭이 시스템 영역에 먹히는 문제를 보정했다. `FlightZoneLayerSelector` content에 `statusBarsPadding()`을 적용한 뒤 최신 debug APK 재설치/실행에서 `완료` 텍스트 bounds가 `[932,112][1014,178]`로 내려왔고, 버튼 중앙 탭으로 지도 화면 복귀, PID 유지, `AndroidRuntime:E` 없음 확인. `:app:testDebugUnitTest --tests "*FlightZoneLayerSelectorTest" --tests "*MapScreenLayersTest"`, `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:minifyReleaseWithR8` 통과 확인.
- 2026-06-14에 최신 debug APK 상태에서 드론 드롭다운과 스케치 모드 비파괴 smoke를 수행했다. 드론 드롭다운은 선택 변경 없이 `내 드론`, `드론 2` 항목을 렌더링했고, 스케치 모드는 선 입력 없이 진입해 `지우개`, `되돌리기`, `다시 실행`, `전체 삭제`, `완료` 툴바 렌더링을 확인한 뒤 `완료`로 지도에 복귀했다. MainActivity PID `1310` 유지, `AndroidRuntime:E` 크래시 로그 없음 확인.
- 2026-06-14에 release readiness gate를 재확인했다. `keystore.properties`와 Google `WEB_CLIENT_ID`가 없는 현재 로컬 상태에서 `:app:assembleRelease`와 `:app:bundleRelease`는 release signing 설정 누락과 Google sign-in Web client ID 누락을 함께 표시하며 의도적으로 실패한다.
- 2026-06-14에 남은 런타임 예외 후보를 재감사했다. `SyncPreferenceKeys.decodeAccountSwitchShapeBaseline`의 baseline 파싱 실패는 `runCatching`으로 `null` 처리되어 계정 전환 경고가 현재 활성 도형 수 기준으로 보수 계산되고, `NotificationScheduler`의 sunrise/sunset content offset은 iOS처럼 내부 30분/10분 경로에서만 호출된다. Weather/KP/VWorld의 빈 리스트 `first()` 후보도 모두 빈 데이터 가드 뒤에 있어 코드 변경 없이 유지했다. `:app:testDebugUnitTest --tests "*NotificationSchedulerTest" --tests "*KpChartsTest" --tests "*VWorld*Test"` 통과 확인.
- 2026-06-14에 계정 전환 baseline 손상값 경로를 회귀 테스트로 고정했다. `syncedShapeBaseline` 파싱 실패는 `null` baseline으로 낮춰 현재 활성 Shape 수를 위험 count로 잡고, 크래시 없이 계정 전환 확인 경고로 이어진다. `:app:testDebugUnitTest --tests "*AuthViewModelForegroundSyncTest"` 통과 확인.
- 2026-06-14에 알림/알람/클립보드 시스템 서비스 획득 경로를 null-safe로 보강했다. 제한된 Context에서 `NotificationManager`/`AlarmManager`/`ClipboardManager`를 얻지 못해도 FCM/로컬 알림/복사 경로가 즉시 크래시하지 않고 건너뛴다. `:app:testDebugUnitTest --tests "*NotificationSchedulerTest" --tests "*FcmServiceTest" --tests "*ShapeDetailDroneResolutionTest" --tests "*ExternalMapTargetTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-14에 실기기 `RFCW324TZ0Z`에서 로그인 경계 smoke를 다시 수행했다. 설정 오버레이에서 `내 정보`, `로그인 / 회원가입`, `내 드론 관리하기`, KP/날씨 섹션이 렌더링됐고, 로그인 시트에서 `Apple로 계속하기`, `Google로 로그인`, 약관/개인정보 링크 순서를 확인했다. 현재 로컬 `WEB_CLIENT_ID` 누락 상태에서 `Google로 로그인`을 누르면 `로그인 오류`와 `Google 로그인 설정이 누락되었습니다. WEB_CLIENT_ID를 확인해주세요.` 다이얼로그가 표시된다. MainActivity PID `1310`과 focus 유지, `AndroidRuntime:E` 로그 없음 확인.
- 2026-06-14에 종료일 알림 재예약을 iOS `SettingManager.scheduleEndDateAlarms`처럼 전체 기존 종료일 알림 cancel 후 활성/미만료/종료일 보유 도형만 다시 예약하도록 보정했다. `:app:testDebugUnitTest --tests "*SettingsEndDateAlarmPlanTest" --tests "*NotificationPreferenceKeysTest" --tests "*NotificationSchedulerTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug` 통과 확인. 최신 debug APK를 `RFCW324TZ0Z`에 데이터 유지 재설치 후 설정 알림 섹션에서 권한 카드, `도형 만료일 알림`, `일출 알림`, `일몰 알림` 행 렌더링 확인, PID `11152` 유지, `AndroidRuntime:E` 로그 없음 확인.
- 2026-06-14에 앱 시작/부팅 후 종료일 알림 복원도 같은 cancel 후 재예약 계획을 쓰도록 추가 보정했다. 최신 HEAD 기준 `:app:testDebugUnitTest --tests "*SettingsPreferenceKeysTest" --tests "*NotificationPreferenceKeysTest" --tests "*SettingsEndDateAlarmPlanTest" --tests "*MainActivityKeepScreenAwakeTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug` 통과 확인.
- 최신 debug APK를 `RFCW324TZ0Z`에 데이터 유지 재설치 후 cold launch smoke를 추가 수행했다. 홈 화면에서 Naver Map, 드론 선택 버튼/드롭다운 원 bounds y=195·height=90, KP/날씨 카드, 하단 탭 렌더링을 확인했고, 설정 탭에서 알림 권한 카드, `도형 만료일 알림`, `일출 알림`, `일몰 알림`, `화면 항상 켜놓기` 행 렌더링을 확인했다. PID `17485` 유지, `AndroidRuntime:E` 로그 없음.
- 2026-06-14에 FCM device 문서 저장 계약을 iOS `PushNotificationManager.saveFCMTokenToFirestore`와 다시 맞췄다. Android도 토큰 저장 때 기존 문서 확인 read 없이 `set(..., merge)`를 수행하고, iOS처럼 `fcmToken`, `platform`, `appVersion`, `isActive`, `createdAt`, `updatedAt`을 매번 함께 쓴다. `:app:testDebugUnitTest --tests "*FcmServiceTest" --tests "*AuthRepositoryUserDocumentTest" --tests "*ProfileViewModelTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug` 통과 확인. 최신 debug APK 재설치 후 MainActivity cold launch, 메인 지도 UI 렌더링, PID `13303` 유지, `AndroidRuntime:E` 로그 없음 확인.
- 2026-06-14에 스케치 샘플링/지우개 거리 계산을 현재 iOS `DistanceCalculator.swift`와 다시 맞췄다. Android `DistanceCalculator`는 평균 지구 반지름 `6371000`을 사용하고, 점-선분 최단거리 투영도 iOS처럼 위경도 degree 평면에서 계산한다. VWorld 비행구역 거리 계산은 iOS `FlightZoneCalculator.swift`와 같이 WGS-84 `6378137`을 유지한다. `:app:testDebugUnitTest --tests "*DistanceCalculatorTest" --tests "*FlightZoneCalculatorTest" --tests "*SketchEraserSelectionTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug` 통과 확인.
- 2026-06-14 최신 debug APK를 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치 후 cold launch smoke를 재실행했다. `LaunchState: COLD`, `TotalTime: 2002`, PID `15003`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. UIAutomator XML에서 Naver Map, 상단 `내 드론`/`드론 2` 선택 버튼과 드롭다운 원, `비행구역 레이어`, `스케치`, `KP`, 날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했고 `AndroidRuntime:E` fatal 로그 없음.
- 2026-06-14에 푸시/로컬 알림 팝업과 클릭 라우팅을 현재 iOS `PushNotificationManager`/`MainTabView`/`SettingManager` 기준으로 다시 대조했다. Android 팝업은 iOS처럼 제목/본문 원문, 빈 문자열, dim 배경 탭 dismiss, 카드 내부 탭 consume, 320/400dp 폭, 250ms scale/fade 전환을 유지한다. Android의 `shapeId`/`shape_id` 포커스 라우팅은 기존 Android 릴리즈 계약으로 보존하며, 현재 iOS 알림 payload에는 shape userInfo가 없어 iOS follow-up 후보로만 남긴다. `:app:testDebugUnitTest --tests "*FcmServiceTest" --tests "*MainScreenStartDestinationTest" --tests "*AndroidManifestContractTest" --tests "*MainActivityKeepScreenAwakeTest"` 통과 확인.
- 2026-06-14에 스케치 툴바/제스처/Undo·Redo/지우개/완료 동기화와 도형 상세 외부지도/복사/메모 링크/드론 상태 표시를 현재 iOS `SketchToolbarView`/`SketchManager`/`ShapeDetailView`/`CopyableTextModifier` 기준으로 다시 대조했다. Android는 펜·지우개 버튼 토글, 전체 삭제 1회 undo 복구, 완료 시 Firebase 동기화, 상세 0.8 detent, 지도 앱 순서, copy toast, memo web link in-app sheet, 삭제된/레거시 드론 표시 계약을 유지한다. `:app:testDebugUnitTest --tests "*SketchDefaultsTest" --tests "*SketchEraserSelectionTest" --tests "*SketchTouchDecisionTest" --tests "*SketchRepositoryTest" --tests "*ShapeDetailDroneResolutionTest" --tests "*ExternalMapTargetTest"` 통과 확인.
- 2026-06-14에 `SketchPointsCache`를 iOS처럼 포인트가 2개 미만인 스케치는 스무딩/캐싱 없이 원본 반환하도록 보정하고, 100개 LRU 캐시/updatedAt 키 분리/invalidate 동작을 회귀 테스트로 고정했다. `:app:testDebugUnitTest --tests "*SketchPointsCacheTest" --tests "*SketchSmoothingAlgorithmTest" --tests "*SketchEraserSelectionTest"` 통과 확인.
- 2026-06-14에 남은 강제 접근/빈 컬렉션 런타임 후보를 재확인했다. Weather chart `first/last`는 size guard 뒤에서만 실행되고, VWorld geometry 좌표 인덱싱은 length/type check 후 수행되며, KP fallback `first()`는 NOAA 결과 non-empty 조건 안에 있다. 자연 정렬 빈 문자열과 드론 선택 초기화도 가드/테스트가 유지된다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*KpChartsTest" --tests "*VWorldGeometryParserTest" --tests "*VWorld*Test" --tests "*DroneSelectionStateTest" --tests "*DroneSelectionDropdownTest"` 통과 확인.
- 2026-06-14에 Manifest 출시/알림 계약을 회귀 테스트로 보강했다. `INTERNET`, fine/coarse location, `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `MainActivity singleTop/exported`, FCM service와 local notification receiver 비공개, boot receiver exported/action 계약을 고정했다. `:app:testDebugUnitTest --tests "*AndroidManifestContractTest" --tests "*BackupRulesTest" --tests "*NotificationPermissionRequestTest" --tests "*NotificationPreferenceKeysTest"`와 `:app:minifyReleaseWithR8` 통과 확인. R8는 기존 Naver Maps/Play Services 경고를 유지하지만 실패하지 않는다.
- 2026-06-14에 Shape Firestore 호환성 사건 메모를 반영해 레거시 `startedAt` 우선순위/잘못된 날짜 타입 거부/쓰기 레거시 키 미생성 테스트를 추가했다. `:app:testDebugUnitTest --tests "*ShapeFirebaseStoreTest" --tests "*ShapeFirestoreParsingTest" --tests "*ShapeTypeTest" --tests "*ShapeValidationTest"`, `:app:testDebugUnitTest --tests "*DroneFirestoreParsingTest" --tests "*DroneValidationTest" --tests "*SketchFirebaseStoreTest" --tests "*SketchValidationTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:minifyReleaseWithR8` 통과 확인.
- 2026-06-14에 저장 목록 정렬 계약을 다시 고정했다. 종료일 없는 도형은 iOS `Date.distantFuture`처럼 종료일 오름차순에서 마지막, 내림차순에서 첫 번째로 정렬되며, 날짜순 내림차순 tie-breaker도 제목/주소 모두 같은 방향을 따른다. `:app:testDebugUnitTest --tests "*SavedListSectionsTest"` 통과 확인.
- 2026-06-15에 도형 편집 좌표 placeholder를 iOS `Localizable.xcstrings`와 다시 대조했다. iOS 빌드에 포함된 String Catalog의 `shape.edit.coordinate.placeholder` 한국어 값은 `좌표를 입력하세요`이고, Android `shape_edit_coordinate_placeholder`와 일치한다. 이전 `.strings` 파일의 `좌표 입력` 값은 Xcode project resource 참조가 없어 현재 기준 수정 대상이 아니다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*ShapeEditDefaultsTest"` 통과 확인.
- 2026-06-15에 알림 클릭/포그라운드 팝업 소스 계약을 다시 확인했다. Android는 notification/system extra의 `notification_title`/`notification_body`, FCM data의 `title`/`body`, `shapeId`/`shape_id`를 모두 복원하고, Manifest `singleTop` + `onNewIntent` 경로로 기존 `MainActivity`에 전달한다. `:app:testDebugUnitTest --tests "*FcmServiceTest" --tests "*MainScreenStartDestinationTest" --tests "*AndroidManifestContractTest"` 통과 확인. 실기기 UI 팝업 smoke는 기기가 secure keyguard 상태라 별도 재시도 항목으로 유지한다.
- 2026-06-15 최신 HEAD 기준 `:app:testDebugUnitTest`와 `:app:assembleDebug` 통과 확인.
- 2026-06-15 최신 HEAD 기준 `:app:lintDebug`와 `:app:minifyReleaseWithR8` 통과 확인. R8는 기존 Naver Maps SDK stack map table 경고와 Play Services Location companion object 경고를 출력하지만 build failure는 아님.
- 2026-06-14에 저장 목록/계정 전환 baseline/시스템 서비스 방어 누적 변경 후 `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:minifyReleaseWithR8`를 재실행해 통과 확인. R8는 기존 Naver Maps SDK stack map table 경고와 Play Services Location companion object 경고를 출력하지만 build failure는 아님.
- 2026-06-14 최신 감사 커밋 누적 후 `:app:testDebugUnitTest`, `:app:assembleDebug`를 재실행해 통과 확인.
- 2026-06-14 최신 debug APK를 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치 후 cold launch smoke를 다시 수행했다. `LaunchState: COLD`, `TotalTime: 1901`, PID `22843`, task size `1`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. 홈에서 Naver Map, 드론 선택 버튼/드롭다운 원, KP/날씨 카드, `비행구역 레이어`, `스케치`, `새 도형 추가`, 하단 `지도`/`저장`/`설정`을 확인했고, 드론 선택 버튼 bounds `[401,195][657,285]`, `[680,195][922,285]`와 드롭다운 원 `[945,195][1035,285]`는 y=195·height=90으로 일치했다. 저장 탭은 `저장 목록`/정렬 칩/`활성화` 섹션/저장 행, 설정 탭은 `설정`/`내 정보`/`로그인 / 회원가입`/`내 드론 관리하기`/KP·날씨 섹션 렌더링을 확인했으며 `AndroidRuntime:E` fatal 로그는 없었다.
- 2026-06-14 같은 실기기에서 위치 권한/현위치 버튼 smoke를 추가했다. `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION` runtime permission은 granted, appops는 `FINE_LOCATION: allow`와 foreground location mode였고 GPS/monitor location op가 running 상태였다. 지도 탭 복귀 후 Naver 현위치 버튼을 탭해도 홈 지도 UI가 유지되고 PID `22843`, task size `1`, focus가 유지됐으며 `AndroidRuntime:E` fatal 로그는 없었다.
- 2026-06-14에 중단 지점 이후 iOS 원본과 Android 구현을 다시 대조했다. 좌표 입력, 로그인 약관, 앱 정보, 설정 메인, 메인 플로팅 버튼/드론 드롭다운, 저장 목록, 도형 상세, 문서/약관, 프로필/계정 작업 흐름은 현재 Android 테스트 계약과 맞아 추가 코드 변경 없이 유지했다. 특히 드론 선택 버튼은 드롭다운 원과 같은 y 시작점/높이 계약을 `DroneSelectionDropdownTest`와 실기기 bounds로 모두 고정한다. `:app:testDebugUnitTest --tests "*DroneSelectionDropdownTest" --tests "*SavedListSectionsTest" --tests "*ShapeDetailDroneResolutionTest" --tests "*ExternalMapTargetTest" --tests "*DocumentEntryPolicyTest" --tests "*DocumentRepositoryTest" --tests "*ProfileViewModelTest" --tests "*AppInfoScreenTest" --tests "*SettingsLanguageSelectionTest" --tests "*SettingsPreferenceKeysTest"` 통과 확인.
- 2026-06-14에 iOS `MainTabView`의 저장/설정 오버레이, 포그라운드 알림 팝업, 스케치 모드 오버레이 닫힘 흐름을 Android `MainScreen`과 다시 대조했다. 이어서 iOS `ShapeDetailView.connectedDrone` 계약에 맞춰 Android 도형 상세 드론 표시 helper가 `shape.droneId`와 다른 `DroneModel`을 연결된 드론으로 표시하지 않도록 보강했다. `droneId == null`인 레거시 도형은 iOS처럼 첫 번째 활성 드론을 fallback으로 사용한다. `:app:testDebugUnitTest --tests "*ShapeDetailDroneResolutionTest" --tests "*ExternalMapTargetTest"`와 `:app:testDebugUnitTest --tests "*ShapeDetailDroneResolutionTest" --tests "*ExternalMapTargetTest" --tests "*MainScreenStartDestinationTest" --tests "*MapScreenLayersTest" --tests "*MapFloatingButtonsTest" --tests "*DroneSelectionDropdownTest"` 통과 확인.
- 2026-06-14 도형 상세 드론 표시 방어 커밋 후 `:app:testDebugUnitTest`와 `:app:assembleDebug`를 다시 실행해 통과 확인.
- 2026-06-14 최신 HEAD 기준 `:app:minifyReleaseWithR8`를 재실행해 통과 확인. 기존 Naver Maps SDK stack map table warning과 Play Services Location companion object warning은 남지만 build failure는 아님.
- 2026-06-14 최신 debug APK를 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치 후 cold launch smoke를 다시 완료했다. `LaunchState: COLD`, `TotalTime: 1925`, PID `1056`, task size `1`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. UIAutomator XML에서 Naver Map/현위치/확대·축소, 상단 `내 드론`/`드론 2` 선택 버튼과 드롭다운 원, `비행구역 레이어`, `스케치`, `KP`, 날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했고, `AndroidRuntime:E` fatal 로그는 없었다.
- 2026-06-14에 VWorld 상세 전화번호 행의 외부 다이얼러 실행을 안전하게 보강했다. 다이얼러가 없거나 제한된 프로필에서 `ACTION_DIAL` 실행이 실패해도 시트가 크래시하지 않고 무시하며, 전화번호 dial URI는 하이픈/공백을 제거한 `tel:` 값으로 고정한다. `:app:testDebugUnitTest --tests "*VWorldZoneDetailSheetTest"`, `:app:testDebugUnitTest --tests "*VWorld*Test" --tests "*FlightZone*Test"`, `:app:testDebugUnitTest`, `:app:assembleDebug` 통과 확인.
- 2026-06-14에 Markdown 문서 링크 탭 경로를 안전하게 보강했다. 약관/개인정보/패치노트 문서 안의 링크 URI가 깨졌거나 처리 앱이 없는 경우에도 `LocalUriHandler.openUri` 예외가 화면 크래시로 이어지지 않고 무시된다. `:app:testDebugUnitTest --tests "*MarkdownInlineTextTest" --tests "*MarkdownParserTest" --tests "*DocumentRepositoryTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug` 통과 확인.
- 2026-06-14 외부 intent 방어 누적 후 `:app:minifyReleaseWithR8`를 재실행해 통과 확인. 최신 debug APK를 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치하고 cold launch smoke도 완료했다. `LaunchState: COLD`, `TotalTime: 1923`, PID `3523`, task size `1`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. UIAutomator XML에서 Naver Map controls, 상단 `내 드론`/`드론 2`/드롭다운 원, `비행구역 레이어`, `스케치`, `KP`, 날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했고, 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했다. `AndroidRuntime:E` fatal 로그 없음.
- 2026-06-14에 `LocalUriHandler.openUri` 직접 호출을 공용 `openUriSafely` helper로 통일했다. Markdown 문서 링크, KP 데이터 출처 링크, 날씨 데이터 출처 링크, 앱 정보 이메일 링크가 처리 앱/잘못된 URI 예외를 화면 크래시로 전파하지 않는다. `:app:testDebugUnitTest --tests "*SafeUriHandlerTest" --tests "*MarkdownInlineTextTest" --tests "*KpChartsTest" --tests "*WeatherForecastParityTest" --tests "*AppInfoScreenTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug` 통과 확인.
- 2026-06-14 최신 URI helper 커밋 후 `:app:minifyReleaseWithR8` 재실행 통과. 최신 debug APK를 `RFCW324TZ0Z`에 데이터 유지 재설치 후 cold launch smoke를 완료했다. `LaunchState: COLD`, `TotalTime: 1917`, PID `4741`, task size `1`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. UIAutomator XML에서 Naver Map controls, 상단 `내 드론`/`드론 2`/드롭다운 원, `비행구역 레이어`, `스케치`, `KP`, 날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했고, 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했다. `AndroidRuntime:E` fatal 로그 없음.
- 2026-06-14 최신 HEAD 기준 `:app:lintDebug`를 실행해 통과 확인. Android Lint report는 `app/build/reports/lint-results-debug.html`에 생성됐고, lint fatal/error로 막히는 항목은 없었다.
- 2026-06-14에 lint report에서 실제 런타임/배포 안정성에 연결되는 경고를 정리했다. 숫자/색/날씨 포맷은 명시적 `Locale.ROOT`를 사용하고, 알림 권한/정확한 알람 intent는 API 가드를 직접 적용하며, Credential Manager `NoCredentialException`은 명시 처리 후 사용자 취소처럼 조용히 무시한다. 메모 WebView는 임의 URL에 대해 JavaScript를 비활성화하고, 앱 언어 런타임 전환을 위해 AAB language split을 끈다. minSdk 28 기준 불필요한 FCM SDK 분기와 Compose primitive state boxing도 정리했다. `:app:testDebugUnitTest --tests "*AuthViewModelForegroundSyncTest" --tests "*NotificationPermissionRequestTest" --tests "*ShapeDetailDroneResolutionTest"`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:minifyReleaseWithR8` 통과 확인. 최신 lint report에서는 `DefaultLocale`, `InlinedApi`, `AppBundleLocaleChanges`, `CredentialManagerMisuse`, `RedundantLabel`, `SetJavaScriptEnabled`, `ObsoleteSdkInt`, `AutoboxingStateCreation`, `UnusedAttribute`가 더 이상 보고되지 않는다.
- 2026-06-14에 저장/설정 overlay drag animation의 `Modifier.offset`을 Compose 권장 lambda overload로 바꿨다. 애니메이션 Dp 값을 현재 density 기준 `IntOffset`으로 지연 변환해 기존 위치/방향은 유지하면서 `UseOfNonLambdaOffsetOverload` lint 경고를 제거했다. `:app:testDebugUnitTest --tests "*MainScreenStartDestinationTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인.
- 2026-06-14에 Drone/KP/Map/Weather composable helper들의 optional `modifier` 파라미터 순서를 Compose 관례에 맞췄다. 호출부는 모두 named argument라 동작 의미는 유지되며, 최신 lint report에서 `ModifierParameter` 경고가 사라졌다. `:app:testDebugUnitTest --tests "*DroneEditSheetTest" --tests "*KpChartsTest" --tests "*MapFloatingButtonsTest" --tests "*SunTimelineStateTest" --tests "*WeatherOverlayCardTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인.
- 2026-06-14에 스케치 모드 `MapView.setOnTouchListener` 접근성 lint를 제한 suppress로 정리했다. 스케치 모드에서 MapView는 지도 클릭 대상이 아니라 펜/지우개 입력 surface 이므로 `performClick()`을 호출해 지도 click semantics를 섞지 않는 계약을 KDoc에 명시했다. `:app:testDebugUnitTest --tests "*SketchTouchDecisionTest" --tests "*MapScreenLayersTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인. 최신 lint report에서 `ClickableViewAccessibility` 경고는 사라졌고, 남은 직접 코드 경고는 Compose `ConfigurationScreenWidthHeight` viewport API 권고뿐이다.
- 2026-06-14에 Compose 화면 크기 계산을 `LocalConfiguration.screenWidthDp/screenHeightDp`에서 `LocalWindowInfo.current.containerSize` 기반 공통 helper로 전환했다. 로그인 tablet 판정, 메인 저장/설정 overlay 크기, 지도 드론 드롭다운/스케치 tablet 판정, 도형 상세/메모 웹 시트 높이는 기존 dp 기준 계산을 유지하면서 window container size를 사용한다. `:app:testDebugUnitTest --tests "*MapScreenLayersTest" --tests "*MainScreenStartDestinationTest" --tests "*ShapeDetailDroneResolutionTest" --tests "*AuthViewModelForegroundSyncTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인. 최신 lint report에서 `ConfigurationScreenWidthHeight` 경고가 사라졌고, 직접 코드 품질 경고는 남아 있지 않다.
- 2026-06-14에 lint `UseKtx` 경고를 정리했다. SharedPreferences 저장은 `androidx.core.content.edit {}`를 쓰고, URI/color 파싱은 `String.toUri()`/`String.toColorInt()`로 바꿨다. `:app:testDebugUnitTest --tests "*SettingsLanguageSelectionTest" --tests "*FcmServiceTest" --tests "*NotificationPermissionRequestTest" --tests "*PaletteColorTest" --tests "*ExternalMapTargetTest" --tests "*ShapeDetailDroneResolutionTest" --tests "*VWorldZoneDetailSheetTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인. 최신 lint report에서 `UseKtx` 경고가 사라졌다.
- 2026-06-14에 직접 문자열로 남아 있던 appcompat, Compose material icons, Naver Map, Play Services Location, coroutines play-services, accompanist permissions 의존성을 version catalog로 이동했다. `:app:assembleDebug`, `:app:lintDebug` 통과 확인. 최신 lint report에서 `UseTomlInstead` 경고가 사라졌고, 남은 Gradle 계열 항목은 의존성/AGP/Gradle 업데이트 권고뿐이다.
- 2026-06-14에 수량이 들어가는 로그인 계정 전환, 드론 삭제 연결 도형, 프로필 동기화 성공, 스케치 전체 삭제, VWorld NOTAM 남은 일수 문구를 Android plural resource로 전환했다. 실제 사용처가 없는 legacy `flight_zone_layer_count` 키는 제거했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인. 최신 lint report에서 `PluralsCandidate` 경고가 사라졌다.
- 2026-06-14에 로딩/동기화 문자열의 ASCII `...`를 Android typography 권장 ellipsis 문자로 통일했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인. 최신 lint report에서 `TypographyEllipsis` 경고가 사라졌다.
- 2026-06-14에 날씨/KP 범위 문자열의 ASCII hyphen을 en dash로, CRI 수식의 빼기 표기를 minus sign으로 정리했다. 지번 주소 예시의 hyphen은 주소 체계 일부라 `tools:ignore="TypographyDashes"`로 제한 예외 처리했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인. 최신 lint report에서 `TypographyDashes` 경고가 사라졌다.
- 2026-06-14에 Android 13 themed icon용 monochrome launcher vector를 추가하고 adaptive icon/round icon에 연결했다. `:app:assembleDebug`, `:app:lintDebug` 통과 확인. 최신 lint report에서 `MonochromeLauncherIcon` 경고가 사라졌다.
- 2026-06-14에 minSdk 28 기준 더 이상 필요하지 않은 legacy density launcher PNG(`ic_launcher.png`, `ic_launcher_round.png`)를 제거하고 adaptive icon 경로만 유지했다. foreground PNG는 adaptive icon foreground로 계속 유지한다. `:app:assembleDebug`, `:app:lintDebug` 통과 확인. 최신 lint report에서 `IconXmlAndPng`, `IconLauncherShape`, `IconDuplicates` 경고가 사라졌다.
- 2026-06-14에 프로필 동기화 결과 다이얼로그의 수량 문자열 생성을 `LocalContext.current.resources`에서 Compose `LocalResources.current`로 바꿨다. `:app:assembleDebug`, `:app:lintDebug` 통과 확인. 최신 lint report에서 `LocalContextResourcesRead` 경고가 사라졌다.
- 2026-06-14에 참조되지 않는 기본 색상 리소스 `R.color.white`/`black`만 담고 있던 `colors.xml`을 제거했다. `:app:assembleDebug`, `:app:lintDebug` 통과 확인. 최신 lint report에서 해당 `UnusedResources` 경고가 사라졌고, lint는 `0 errors, 165 warnings` 상태다.
- 2026-06-14에 최신 리소스/아이콘/lint 정리 누적 후 전체 `:app:testDebugUnitTest`와 `:app:minifyReleaseWithR8`를 재실행해 통과 확인. R8 warning은 기존 Naver Maps SDK stack map table 및 Play Services Location companion object 경고만 남고 build failure는 아니다.
- 2026-06-14 최신 debug APK를 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치 후 cold launch smoke를 다시 완료했다. `LaunchState: COLD`, `TotalTime: 1896`, task size `1`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. UIAutomator XML에서 Naver Map controls, 상단 `내 드론`/`드론 2`/드롭다운 원, `비행구역 레이어`, `스케치`, `KP`, 날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했고, 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했다. `AndroidRuntime:E` fatal 로그 없음.
- 2026-06-14 실기기 하단 탭 회귀를 추가 확인했다. `저장` 탭에서는 `저장 목록`, `비행시작일순`, `내림차순`, `활성화`와 활성 도형 3개가 렌더링됐고, `설정` 탭에서는 `내 정보`, `로그인 / 회원가입`, `내 드론 관리하기`, `비행 환경`, `현재 KP 지수: 2.0`, `현재 날씨`, `알림` 섹션이 렌더링됐다. 탭 전환 후에도 task size `1`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity`를 유지했고 `AndroidRuntime:E` fatal 로그는 없었다.
- 2026-06-14에 iOS `SettingManager.registerSunriseAlarms`/`registerSunsetAlarms`와 Android 일출/일몰 알림 content helper를 다시 대조했다. Android도 정상 경로는 iOS처럼 30분/10분 전 리소스만 사용하고, 손상된 offset 값이 들어와도 알림 재예약 중 앱을 중단하지 않도록 30분 리소스로 fallback한다. `:app:testDebugUnitTest --tests "*NotificationSchedulerTest"`와 `:app:testDebugUnitTest --tests "*NotificationSchedulerTest" --tests "*NotificationPreferenceKeysTest" --tests "*SettingsEndDateAlarmPlanTest" --tests "*MainActivityKeepScreenAwakeTest"` 통과 확인.
- 2026-06-14에 VWorld GeoJSON parser를 iOS `FlightZoneOverlayManager`/`FlightZoneCalculator` 기준으로 보강했다. 3점 미만 ring은 parser 단계에서 제외해 불완전 polygon이 모델에 남지 않으며, MultiPolygon은 invalid ring을 버린 뒤 유효 polygon을 계속 보존한다. `:app:testDebugUnitTest --tests "*VWorldGeometryParserTest" --tests "*FlightZoneCalculatorTest" --tests "*VWorldZoneDetailSheetTest"` 통과 확인.
- 2026-06-14에 Shape Firestore polygon/polyline 좌표 배열 파싱을 iOS `compactMap` 동작과 맞춰 손상 좌표 원소만 제외하도록 보정했다. 제외 후 polygon 3점 미만, polyline 2점 미만이면 기존처럼 문서 전체를 skip한다. 이어서 배열 안 좌표가 위경도 범위/finite 검증을 통과하지 못하면 문서 invalid로 유지되는 회귀 테스트를 추가했다. `:app:testDebugUnitTest --tests "*ShapeFirestoreParsingTest" --tests "*ShapeFirebaseStoreTest" --tests "*ShapeTypeTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:minifyReleaseWithR8` 통과 확인. R8는 기존 Naver Maps/Play Services warning만 출력하고 build failure는 없음.
- 2026-06-14에 Shape/Sketch Firestore optional 숫자 읽기를 iOS parser와 맞췄다. Shape `radius`/`height` 타입 불일치는 `nil`, Sketch `strokeWidth`/`opacity` 타입 불일치는 기본값으로 복구하며, 좌표/날짜/id/shapeType 필수 계약과 쓰기 검증은 엄격하게 유지한다. `:app:testDebugUnitTest --tests "*ShapeFirestoreParsingTest" --tests "*ShapeFirebaseStoreTest" --tests "*ShapeValidationTest" --tests "*SketchFirebaseStoreTest" --tests "*SketchValidationTest"`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:minifyReleaseWithR8` 통과 확인. R8는 기존 Naver Maps/Play Services warning만 출력하고 build failure는 없음.
- 2026-06-14 최신 debug APK를 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치 후 cold launch smoke를 다시 완료했다. `LaunchState: COLD`, `TotalTime: 1875`, PID `20173`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. 앱 PID 로그에서 Firebase 초기화와 `NaverMapDebug: 네이버 지도 준비 완료`를 확인했고, UIAutomator XML에서 Naver Map controls, 현위치/줌/NAVER logo, 상단 `내 드론`/`드론 2`/드롭다운 원, `비행구역 레이어`, `스케치`, `KP`, 날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. `AndroidRuntime:E` fatal 로그 없음.
- 2026-06-14에 앱 베이스 테마를 `Theme.AppCompat.Light.NoActionBar`로 바꿔 Naver Map SDK 내부 AppCompat 위젯 inflation 경고(`ThemeUtils: ... AppCompat theme`)를 제거했다. Compose Material3 UI와 iOS 라이트 톤은 유지한다. `:app:assembleDebug` 통과 후 debug APK를 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치했고, cold launch `LaunchState: COLD`, `TotalTime: 1893`, PID `23478`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지 확인. 필터 로그에는 `NaverMapDebug: 네이버 지도 준비 완료`만 남고 `ThemeUtils`/`AndroidRuntime`/`FATAL EXCEPTION`은 없었다. UIAutomator XML에서 지도, 상단 `내 드론`/`드론 2`/드롭다운 원, `비행구역 레이어`, `스케치`, `KP`, 날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 재확인했다.
- 2026-06-14에 스케치 전체 삭제 영어 확인 메시지의 `one` 분기를 iOS `sketch.alert.deleteAll.message`와 같게 `Are you sure you want to delete all %d sketches?`로 보정했다. Android plural 분기로 1개일 때만 iOS와 다른 문장이 뜨던 차이를 제거했고, `StringResourceCoverageTest`에 한국어/영어 스케치 툴바 핵심 문자열 회귀 테스트를 추가했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*SketchDefaultsTest"`, `:app:testDebugUnitTest` 통과 확인.
- 2026-06-14 최신 debug APK를 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치 후 cold launch smoke를 다시 수행했다. `adb install -r` 성공, `LaunchState: COLD`, `TotalTime: 1900`, PID `26122`, task size `1`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. UIAutomator XML에서 Naver Map controls, 현위치/확대·축소/NAVER logo, 상단 `내 드론`/`드론 2` 선택 버튼과 드롭다운 원, `비행구역 레이어`, `스케치`, `KP`, 날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했고, 필터 로그에는 `NaverMapDebug: 네이버 지도 준비 완료`만 남아 `ThemeUtils`/`AndroidRuntime`/`FATAL EXCEPTION`은 없었다.
- 2026-06-14에 현재 UI/테스트에서 참조하지 않는 legacy 저장 행 status 문자열과 도형 상세 status/copy/no_drone/duplicated 문자열을 제거했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인. 최신 lint report의 `UnusedResources`는 131개에서 122개로 감소했다.
- 2026-06-14 리소스 정리 커밋 `0853b04` 이후 새 HEAD 기준으로 `:app:minifyReleaseWithR8`를 재실행해 통과 확인했다. R8는 기존 Naver Maps SDK stack map table warning과 Play Services Location companion object warning만 출력하며 build failure는 아니다.
- 2026-06-14에 현재 UI/테스트에서 참조하지 않는 legacy common/login/settings/web document/patch notes 문자열을 추가 제거했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인. 최신 lint report의 `UnusedResources`는 122개에서 111개로 감소했다.
- 2026-06-14에 현재 UI/테스트에서 참조하지 않는 legacy weather summary/status 문자열을 제거했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*WeatherForecastParityTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인. 최신 lint report의 `UnusedResources`는 111개에서 99개로 감소했다.
- 2026-06-14에 현재 UI/테스트에서 참조하지 않는 legacy KP guide/chart 문자열을 제거했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*KpChartsTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인. 최신 lint report의 `UnusedResources`는 99개에서 92개로 감소했다.
- 2026-06-14에 현재 UI/테스트에서 참조하지 않는 legacy saved search/delete 및 shape edit title/basic/radius/altitude 문자열을 제거했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*SavedListSectionsTest" --tests "*ShapeEditDefaultsTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인. 최신 lint report의 `UnusedResources`는 92개에서 78개로 감소했다.
- 2026-06-14 리소스 정리 커밋 `246dc65` 이후 최신 HEAD 기준으로 `:app:minifyReleaseWithR8`를 재실행해 통과 확인했다. R8는 기존 Naver Maps SDK stack map table warning과 Play Services Location companion object warning만 출력하며 build failure는 아니다.
- 2026-06-14에 현재 UI/테스트에서 참조하지 않는 legacy drone/settings account/environment 문자열을 제거했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*DroneEditSheetTest" --tests "*SettingsLanguageSelectionTest" --tests "*SettingsPreferenceKeysTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인. 최신 lint report의 `UnusedResources`는 78개에서 60개로 감소했다.
- 2026-06-14 리소스 정리 커밋 `86a3e89` 이후 최신 HEAD 기준으로 `:app:minifyReleaseWithR8`를 재실행해 통과 확인했다. R8는 기존 Naver Maps SDK stack map table warning과 Play Services Location companion object warning만 출력하며 build failure는 아니다.
- 2026-06-14에 현재 UI/테스트에서 참조하지 않는 legacy sketch/flight-zone/notification/VWorld/shape-search/weather/KP 문자열을 제거했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*SketchDefaultsTest" --tests "*VWorldZoneDetailSheetTest" --tests "*WeatherForecastParityTest" --tests "*KpChartsTest" --tests "*SearchAddressSheetTest" --tests "*NotificationSchedulerTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인. 최신 lint report의 `UnusedResources`는 60개에서 7개로 감소했고, 남은 7개는 문자열 커버리지 테스트가 의도적으로 고정하는 항목이다.
- 2026-06-14 리소스 정리 커밋 `9ecbd04` 이후 최신 HEAD 기준으로 `:app:minifyReleaseWithR8`를 재실행해 통과 확인했다. R8는 기존 Naver Maps SDK stack map table warning과 Play Services Location companion object warning만 출력하며 build failure는 아니다.
- 2026-06-14에 남은 7개 iOS 문자열 계약 고정 리소스(`kp_title`, `kp_current`, `shape_edit_label_coordinate`, `settings_delete_account*`)에 제한 `tools:ignore="UnusedResources"`를 추가했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest"`, `:app:lintDebug`, `:app:assembleDebug` 통과 확인. 최신 lint report에서 `UnusedResources`는 사라졌고, 남은 lint 항목은 AGP/Gradle/라이브러리 버전 업데이트 권고뿐이다.
- 2026-06-14 리소스 lint suppress 커밋 `334fcf5` 이후 최신 HEAD 기준으로 `:app:minifyReleaseWithR8`를 재실행해 통과 확인했다. R8는 기존 Naver Maps SDK stack map table warning과 Play Services Location companion object warning만 출력하며 build failure는 아니다.
- 2026-06-14 최신 debug APK를 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치 후 cold launch smoke를 재수행했다. `LaunchState: COLD`, `TotalTime: 1844`, PID `30700`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. 홈 UIAutomator XML에서 Naver Map controls, 현위치/줌/NAVER logo, 상단 `내 드론`/`드론 2`/드롭다운 원, `비행구역 레이어`, `스케치`, `KP`, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했다. 저장 탭은 `저장 목록`/`비행시작일순`/`내림차순`/`활성화`, 설정 탭은 `설정`/`내 정보`/`로그인 / 회원가입`/`내 드론 관리하기`/`비행 환경`/`현재 KP 지수`/`현재 날씨`/`알림` 렌더링을 확인했고, `AndroidRuntime:E` fatal 로그 없음.
- 2026-06-14 최신 실기기 smoke 기록 커밋 `4ea14f6` 이후 최신 HEAD 기준으로 전체 `:app:testDebugUnitTest`를 재실행해 통과 확인했다.
- 2026-06-15 최신 HEAD 기준으로 `:app:assembleRelease`와 `:app:bundleRelease` release readiness gate를 재확인했다. `keystore.properties`/서명 파일과 `WEB_CLIENT_ID`가 없는 현재 로컬 상태에서는 두 명령 모두 release signing 설정 누락과 Google sign-in Web client ID 누락 메시지를 함께 출력하며 의도적으로 실패한다.
- 2026-06-15 실기기 `RFCW324TZ0Z`에서 기존 debug 설치 상태로 MainActivity focus/PID `30700`을 유지한 채 추가 비파괴 smoke를 수행했다. 홈 UIAutomator XML에서 상단 `내 드론` `[401,195][657,285]`, `드론 2` `[680,195][922,285]`, 드롭다운 원 `[945,195][1035,285]`가 모두 y=195·height=90으로 정렬됨을 재확인했다. KP 카드는 `KP 지수 예보`, `현재 KP 지수`, `향후 48시간 예보`, `장기 예보 (27일)` 섹션을 렌더링했고, 날씨 카드는 `현위치 기반 정보`, `일출/일몰 정보`, `현재 날씨`, `드론 무게`, `풍속`, `돌풍 차이`를 렌더링했다. 스케치 버튼은 입력 없이 `지우개`, `되돌리기`, `다시 실행`, `전체 삭제`, `완료` 툴바를 표시했고 `완료`로 지도에 복귀했다. 최종 focus는 `com.ScienceFiction.DronePassAndroid/.MainActivity`, PID는 `30700`이며 `FATAL EXCEPTION` 로그는 없었다.
- 2026-06-15에 VWorld 상세 시트의 공공기관 연락처 fallback을 iOS `DroneZoneFeature.publicContact`와 맞췄다. Android는 코드가 없는 ATZ/이착륙장/장애물 구역에서 `zoneName`을 우선해 연락처를 찾을 수 있었지만, iOS는 `zoneCode ?? layer.displayName`으로만 lookup하므로 Android도 같은 기준으로 보정했다. `:app:testDebugUnitTest --tests "*VWorldZoneDetailSheetTest"`, `:app:testDebugUnitTest --tests "*VWorld*Test" --tests "*FlightZone*Test"`, `:app:assembleDebug` 통과 확인.
- 2026-06-15에 VWorld 상세 시트의 고도 행 표시 조건도 iOS `feature.altitudeInfo != nil`와 맞췄다. Android는 상한/하한 중 하나만 있어도 고도 행을 보여줄 수 있었지만, iOS는 상한과 하한이 모두 있을 때만 해당 행을 렌더링하므로 Android도 둘 다 있을 때만 표시한다. `:app:testDebugUnitTest --tests "*VWorldZoneDetailSheetTest"`, `:app:testDebugUnitTest --tests "*VWorld*Test" --tests "*FlightZone*Test"`, `:app:assembleDebug` 통과 확인.
- 2026-06-15에 VWorld 문자열 property 파싱을 iOS `as? String` 동작과 맞췄다. Android는 `toString()`으로 숫자 값을 문자열처럼 표시하고 blank 문자열은 `null`로 낮출 수 있었지만, 이제 String 타입만 그대로 읽어 빈 문자열은 보존하고 숫자/불일치 타입은 `null`로 처리한다. 또한 parser의 generic `code`/`zoneCode` fallback을 제거해 레이어별 iOS 필드만 `zoneCode`로 사용한다. `:app:testDebugUnitTest --tests "*VWorldModelsTest" --tests "*VWorldRepositoryTest" --tests "*VWorldZoneDetailSheetTest"`, `:app:testDebugUnitTest --tests "*VWorld*Test" --tests "*FlightZone*Test"`, `:app:assembleDebug` 통과 확인.
- 2026-06-15에 스케치 모드 종료 중 진행 중인 선을 저장할 때도 iOS `SketchManager.exitSketchMode()`처럼 일반 `finishDrawing()` 경로의 undo 기록을 남기도록 보정했다. `:app:testDebugUnitTest --tests "*SketchDefaultsTest"`, `:app:testDebugUnitTest --tests "*Sketch*Test"`, `:app:assembleDebug` 통과 확인.
- 2026-06-15 최신 debug APK를 실기기 `RFCW324TZ0Z`에 `adb install -r`로 데이터 유지 재설치 후 cold launch smoke를 완료했다. `LaunchState: COLD`, `TotalTime: 1920`, PID `10832`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. UIAutomator XML에서 Naver Map controls, 현위치/확대·축소/NAVER logo, 상단 `내 드론`/`드론 2` 선택 버튼과 드롭다운 원, `비행구역 레이어`, `스케치`, KP/날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했다. 필터 로그에는 `NaverMapDebug: 네이버 지도 준비 완료`가 있고, `AndroidRuntime` 항목은 UIAutomator 프로세스뿐이며 `FATAL EXCEPTION`/`ThemeUtils` 앱 오류는 없었다.
- 2026-06-15에 로그인/Auth 흐름을 iOS `LoginView`/`AuthManager`/`GoogleLoginManager`/`AppleLoginManager` 기준으로 재감사했다. Android `LoginScreen`의 버튼/문서 시트 토큰, Google/Apple 취소 무시, 계정 전환 확인/취소, reset-before-finalize, 사용자 루트 문서 필드, provider 계정 복구, 로그인 후 cloud backup/realtime/full sync/FCM 순서는 현재 계약과 맞다. Android provider 복구는 `sketches`도 옮기므로 iOS보다 데이터 보존 범위가 넓고, iOS `sketches` 미이전은 별도 iOS 보강 후보로 유지한다. `:app:testDebugUnitTest --tests "*Auth*Test" --tests "*StringResourceCoverageTest"` 통과 확인.
- 2026-06-15 같은 실기기/PID `10832`에서 하단 `저장`/`설정` 탭 비파괴 회귀를 재확인했다. `저장` 오버레이는 `저장 목록`, `비행시작일순`, `내림차순`, `활성화`와 3개 활성 행을 렌더링했고, `설정` 오버레이는 `설정`, `내 정보`, `로그인 / 회원가입`, `내 드론 관리하기`, `비행 환경`, `현재 KP 지수`, `현재 날씨`, `알림`을 렌더링했다. 지도 탭 복귀 후 `비행구역 레이어`, `스케치`, `새 도형 추가`가 다시 보이고 PID `10832`가 유지됐으며, logcat에는 UIAutomator `AndroidRuntime` 실행 흔적 외 앱 `FATAL EXCEPTION`/`ThemeUtils` 오류가 없었다.
- 2026-06-15에 앱 정보/패치노트 화면을 iOS `AppInfoView`/`PatchNotesView`와 다시 대조했다. 앱 정보 섹션/버전 구조와 패치노트 자동 재로드 정책은 유지하고, 패치노트 title/feature title/description 표시 조건은 iOS Swift `isEmpty` 동작처럼 공백 문자열을 표시 대상으로 보존하도록 보정했다. `:app:testDebugUnitTest --tests "*PatchNotesContentTest" --tests "*MarkdownParserTest" --tests "*DocumentEntryPolicyTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-15 패치노트 보정 커밋 `17e509e` 이후 최신 debug APK를 실기기 `RFCW324TZ0Z`에 `adb install -r`로 데이터 유지 재설치 후 cold launch smoke를 재수행했다. `LaunchState: COLD`, `TotalTime: 1924`, PID `12627`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. UIAutomator XML에서 Naver Map controls, 현위치/확대·축소/NAVER logo, 상단 `내 드론`/`드론 2` 선택 버튼과 드롭다운 원, `비행구역 레이어`, `스케치`, KP/날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했다. PID 로그 필터에는 `NaverMapDebug: 네이버 지도 준비 완료`가 있고, `AndroidRuntime`/`FATAL EXCEPTION`/`ThemeUtils` 항목은 없었다.
- 2026-06-15에 Naver reverse geocode 주소 조립을 iOS `NaverGeocodingService.reverseGeocode`와 다시 대조했다. Android는 blank 지역명/도로명/건물명을 제거하고 도로명 이름이 없어도 건물번호를 붙일 수 있었지만, 이제 iOS처럼 응답에 존재하는 빈 문자열/공백 문자열을 그대로 이어 붙이고 `land.name == nil`이면 도로명 건물번호도 생략한다. `:app:testDebugUnitTest --tests "*GeocodingRepositoryTest" --tests "*SearchAddressSheetTest" --tests "*ShapeEditDefaultsTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-15에 VWorld 고도 포맷터를 iOS `AltitudeFormatter`와 다시 대조했다. Android는 파싱 실패 입력을 `null`로 낮추고 숫자 추출에서 모든 whitespace를 제거할 수 있었지만, 이제 iOS처럼 `format("")`/`format("   ")`/개행 입력은 원문을 반환하고 숫자 추출은 일반 공백만 제거한다. `:app:testDebugUnitTest --tests "*AltitudeFormatterTest" --tests "*VWorldZoneDetailSheetTest" --tests "*VWorldModelsTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-15 재개 라운드에서 iOS `UIColor(hex:)`/FCM 팝업 payload/Sketch Firestore/DroneEdit optional 필드를 다시 대조했다. `UIColor(hex:)`는 양쪽 모두 앞뒤 공백과 줄바꿈 제거 후 `#`를 제거하므로 Android `IosHexColor` 변경은 필요 없었다. FCM은 첫 제목/본문 키가 빈 문자열이어도 fallback하지 않는 `containsKey` 정책과 테스트를 유지한다. Sketch Firestore는 iOS처럼 points 손상 원소만 제외하고 누락 날짜/색상/숫자 필드는 기본값으로 복구한다. DroneEdit에서 Android는 기존 optional 값을 빈 입력으로 지울 수 있는데, iOS의 현재 `update(name:color:serialNumber:...)` nil 파라미터 한계보다 사용자 수정 동작이 명확하므로 Android 동작을 유지하고 iOS 보강 후보로만 기록한다. `:app:testDebugUnitTest --tests "*FcmServiceTest" --tests "*SketchFirebaseStoreTest" --tests "*DroneEditSheetTest"` 통과 확인.
- 2026-06-15에 주소 검색 오류 메시지 fallback을 iOS `SearchAddressViewModel.searchAddress`와 맞췄다. Android는 `Exception.message`가 빈 문자열/공백 문자열이면 일반 fallback 문구로 대체했지만, iOS는 `검색 중 오류가 발생했습니다: \(error.localizedDescription)` 형태로 원문 description을 항상 붙이므로 Android도 null이 아닌 cause message는 그대로 표시한다. `:app:testDebugUnitTest --tests "*SearchAddressSheetTest" --tests "*ShapeEditDefaultsTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-15에 드론 삭제 실패 alert 메시지를 iOS `DroneDetailView.deleteConfirmed`와 맞췄다. Android는 실패 메시지가 빈 문자열/공백 문자열이면 `common_unknown_error`로 대체했지만, iOS는 `error.localizedDescription`을 그대로 alert 본문에 넣으므로 Android도 `DroneDeleteError.Failure.message`를 원문 그대로 표시한다. `:app:testDebugUnitTest --tests "*DroneListScreenTest" --tests "*DroneDeleteValidationTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-15에 날씨 데이터 카드의 subText 표시 조건을 iOS `WeatherForecastView.weatherDataCard`와 맞췄다. iOS는 `subText != nil`이면 빈 문자열이어도 작은 라벨/본문 스타일과 subText 행을 적용하므로, Android도 `subText.isNullOrBlank()`가 아니라 null 여부만 보도록 변경했다. `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*WeatherOverlayCardTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-15에 로그인 오류 다이얼로그 본문 표시를 iOS `LoginView`와 맞췄다. Android는 `AuthState.Error.message`가 빈 문자열/공백 문자열이면 `login_error_unknown`으로 대체했지만, iOS는 `loginError?.localizedDescription ?? unknown`이라 description 문자열이 존재하면 빈 값도 그대로 표시한다. Android도 표시 단계에서 `ifBlank` fallback을 제거하고 원문 메시지를 보존한다. `:app:testDebugUnitTest --tests "*AuthViewModelForegroundSyncTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-15 로그인 오류 보정 커밋 `6ebf3e5` 이후 최신 debug APK를 실기기 `RFCW324TZ0Z`에 `adb install -r`로 데이터 유지 재설치 후 cold launch와 하단 탭 smoke를 재수행했다. `LaunchState: COLD`, `TotalTime: 1920`, PID `15369`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. 홈 XML에서 Naver Map controls, 현위치/확대·축소/NAVER logo, 상단 `내 드론`/`드론 2` 선택 버튼과 드롭다운 원, `비행구역 레이어`, `스케치`, KP/날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. 상단 드론 선택 요소 시각 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했다. 저장 탭은 `저장 목록`/`비행시작일순`/`내림차순`/`활성화`, 설정 탭은 `설정`/`내 정보`/`로그인 / 회원가입`/`내 드론 관리하기`/`비행 환경`/`현재 KP 지수: 1.3`/`현재 날씨`/`알림`을 렌더링했고, 지도 탭 복귀 후 PID와 focus가 유지됐다. logcat에는 `NaverMapDebug: 네이버 지도 준비 완료`가 있고, `AndroidRuntime` 항목은 UIAutomator 실행 흔적뿐이며 `FATAL EXCEPTION`/`ThemeUtils` 앱 오류는 없었다.
- 2026-06-15에 사용자 사건 메모의 핵심 Firestore `shapeType` 계약을 최신 HEAD에서 다시 확인했다. Android `shapeToFirestoreDocumentData`는 `shape.shapeType.rawValue`로 `circle`/`rectangle`/`polygon`/`polyline`만 쓰고, `ShapeType.parseWireValue`와 Firestore 파서는 레거시 `CIRCLE` 및 혼합 대소문자 `Circle`을 계속 읽는다. `:app:testDebugUnitTest --tests "*ShapeTypeTest" --tests "*ShapeFirestoreParsingTest" --tests "*ShapeFirebaseStoreTest"` 통과 확인.
- 2026-06-15에 앱 포그라운드 복귀 시 실시간 리스너가 꺼져 있더라도 곧바로 full sync를 시작하지 않고, iOS `ChangeDetectionManager`처럼 원격 metadata 변경이 실제로 있을 때만 `변경사항 감지` 확인 다이얼로그를 띄우도록 보정했다. 사용자가 확인하면 기존 realtime listener 복구와 full sync를 실행하며, shape/sketch metadata 모두 동일 기준으로 확인한다. `:app:testDebugUnitTest --tests "*AuthViewModelForegroundSyncTest" --tests "*RealtimeSyncManagerTest" --tests "*StringResourceCoverageTest"`, `:app:assembleDebug`, `:app:lintDebug` 통과 확인.
- 2026-06-15에 위 포그라운드 변경사항 확인 후 동기화 진행/완료/오류 alert도 iOS `ChangeDetectionManager`와 맞췄다. 확인을 누르면 로딩 alert를 표시하고, full sync 성공 시 `동기화 완료`, 실패 시 iOS와 같은 오류 본문 형식으로 결과 alert를 보여준다. 로그인 화면으로 전환되면 stale 포그라운드 동기화 dialog 상태를 정리한다. `:app:testDebugUnitTest --tests "*AuthViewModelForegroundSyncTest" --tests "*RealtimeSyncManagerTest" --tests "*StringResourceCoverageTest"`, `:app:assembleDebug`, `:app:lintDebug` 통과 확인.
- 2026-06-15에 iOS `ChangeDetectionManager`의 중복 체크 방지 정책도 Android 포그라운드 복구 흐름에 반영했다. 한 포그라운드 세션에서 이미 변경사항 확인을 완료했거나 동기화 중이면 재확인하지 않고, 실패 재시도도 iOS처럼 30초 throttle을 거친다. Lifecycle `ON_STOP`에서 체크 상태를 리셋해 다음 foreground 진입 때 다시 확인한다. `:app:testDebugUnitTest --tests "*AuthViewModelForegroundSyncTest"`, `:app:assembleDebug`, `:app:lintDebug` 통과 확인.
- 2026-06-15 포그라운드 동기화 UX 보정 후 최신 debug APK를 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치하고 cold launch smoke를 재수행했다. `LaunchState: COLD`, `TotalTime: 1995`, PID `5721`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. UIAutomator XML에서 Naver Map controls, 현위치/확대·축소/NAVER logo, 상단 `내 드론`/`드론 2` 선택 버튼과 드롭다운 원, `비행구역 레이어`, `스케치`, KP/날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했다. 필터 로그에는 `NaverMapDebug: 네이버 지도 준비 완료`만 있고 `AndroidRuntime`/`ThemeUtils` 앱 오류는 없었다.
- 2026-06-15에 사용자 목표에서 명시한 저장 목록 도형 탭 → 지도 포커스 흐름을 다시 대조/실기기 확인했다. 코드상 Android는 iOS `SavedTableListView.handleShapeTap`/`MapViewModel.moveCameraToShape`처럼 행 탭 시 상세 시트를 열지 않고 선택 표시와 `MoveToShape` 2단계 카메라 이동만 수행하며, 중복 이동 skip은 `baseCoordinate`, 반경 fallback은 `radius ?: 100m`, 줌 공식은 100m→14 / 3000m→11 선형 보간을 유지한다. 실기기 `RFCW324TZ0Z`에서 저장 탭을 열고 첫 번째 행 일반 영역을 탭했을 때 오버레이가 열린 채 유지되고 지도 scale bar가 `50m`에서 `200m`로 바뀌어 카메라 줌 이동이 실행됨을 확인했다. PID `5721`과 MainActivity focus는 유지됐고 `AndroidRuntime`/`ThemeUtils` 오류 로그는 없었다.
- 2026-06-15 저장 목록 포커스 smoke 기록 후 같은 범위의 단위 회귀도 최신 HEAD에서 재확인했다. `:app:testDebugUnitTest --tests "*MapCameraFocusTest" --tests "*SavedListSectionsTest" --tests "*MainScreenStartDestinationTest"` 통과.
- 2026-06-15에 프로필/계정 관리 화면을 iOS `ProfileView`/`AuthManager`와 다시 대조했다. Android는 내 정보/로그아웃/동기화/약관/탈퇴 2단계 확인 흐름, 로그아웃 전 강제 동기화와 baseline 저장, 탈퇴 시 익명화 저장 실패 무시, Firestore 삭제 3회 재시도 후 Auth 삭제 계속 진행 정책을 유지한다. 탈퇴 원격 삭제는 기존 Android 방어대로 iOS 기본 `shapes`/`drones`/`metadata`에 `sketches`/`devices`를 더 포함하고, provider 복구도 `sketches`를 포함한다. 누락되어 있던 프로필 동기화 disabled/active/waiting 영어 문구와 탈퇴 최종 확인 문구를 `StringResourceCoverageTest`에 고정했다. `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*Profile*Test" --tests "*AuthRepositoryUserDocumentTest"` 통과.
- 2026-06-15에 FCM/로컬 알림 클릭·팝업·스케줄링 경로를 iOS `PushNotificationManager`/`SettingManager`와 다시 대조했다. 포그라운드 팝업은 iOS처럼 title/body만 표시하고 확인/배경 탭으로 닫으며, Android의 알림 클릭 shapeId 지도 포커스 보강 경로는 유지한다. 종료일 알림 재예약 plan은 iOS `scheduleEndDateAlarm`처럼 7일 전 trigger 시각이 현재보다 미래인 도형만 예약 대상으로 넘기도록 보정했다. `:app:testDebugUnitTest --tests "*NotificationSchedulerTest" --tests "*SettingsEndDateAlarmPlanTest" --tests "*FcmServiceTest" --tests "*MainScreenStartDestinationTest"`, `:app:assembleDebug` 통과.
- 2026-06-15 알림 재예약 보정 후 최신 debug APK를 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치하고 cold launch smoke를 수행했다. `LaunchState: COLD`, `TotalTime: 1877`, PID `7397`, task size `1`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. UIAutomator XML에서 Naver Map controls, 현위치/확대·축소/NAVER logo, 상단 `내 드론`/`드론 2` 선택 버튼과 드롭다운 원, `비행구역 레이어`, `스케치`, KP/날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했고, 필터 로그에는 `NaverMapDebug: 네이버 지도 준비 완료`만 있으며 `AndroidRuntime`/`ThemeUtils` 앱 오류는 없었다.
- 2026-06-15에 저장 Shape 지도 오버레이를 iOS `MapViewModel`과 다시 대조했다. iOS는 현재 저장 Shape 지도 렌더링/선택 하이라이트가 원형 오버레이 중심이지만, Android는 Firestore 계약 타입인 rectangle/polygon/polyline까지 렌더링하는 확장 구현을 유지한다. 대신 외부/레거시 데이터가 좌표 개수만 맞고 실제 면적이나 길이가 없는 경우 Naver Maps 오버레이로 넘기지 않도록 방어했다. `:app:testDebugUnitTest --tests "*ShapeOverlayRenderTest" --tests "*ShapeOverlayColorTest" --tests "*MapCameraFocusTest" --tests "*SavedListSectionsTest"`, `:app:assembleDebug` 통과.
- 2026-06-15에 Shape Firestore batch 검증도 "쓰기는 엄격, 읽기는 관대" 원칙에 맞게 분리했다. 기존에는 개별 파서가 iOS처럼 빈 제목 문자열을 허용해도 `loadShapes()`가 쓰기용 batch 검증을 다시 적용해 전체 로드를 실패시킬 수 있었으므로, 읽기 batch는 `validateForFirebaseRead()`를 사용하고 중복 ID만 계속 거부한다. 쓰기 batch는 추가로 실제 면적/길이가 없는 rectangle/polygon/polyline geometry를 거부한다. `:app:testDebugUnitTest --tests "*ShapeValidationTest" --tests "*ShapeFirestoreParsingTest" --tests "*ShapeFirebaseStoreTest" --tests "*ShapeOverlayRenderTest" --tests "*ShapeOverlayColorTest"`, `:app:assembleDebug` 통과.
- 2026-06-15 Shape 계약 보정 후 `:app:testDebugUnitTest` 전체 회귀도 통과했다. 이어 최신 debug APK를 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치하고 cold launch smoke를 재수행했다. `LaunchState: COLD`, `TotalTime: 1998`, PID `8807`, task size `1`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지. UIAutomator XML에서 Naver Map controls, 현위치/확대·축소/NAVER logo, 상단 `내 드론`/`드론 2` 선택 버튼과 드롭다운 원, `비행구역 레이어`, `스케치`, KP/날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했고, 짧은 logcat 필터에는 `NaverMapDebug: 네이버 지도 준비 완료`만 있으며 `FATAL EXCEPTION`/`ThemeUtils` 앱 오류는 없었다.
- 2026-06-15 Shape 계약 보정 후 `:app:minifyReleaseWithR8`도 재실행해 통과 확인. 기존과 같은 Naver Maps SDK stack map table warning과 Play Services Location companion warning이 출력되지만 build failure는 아님.
- 2026-06-16에 iOS `SettingView`/`SettingManager.initializeAppLanguage`/`FetchWebDocuments`를 Android 설정 언어 경로와 다시 대조했다. iOS는 언어 선택 시 저장값만 갱신하고 재시작 안내를 띄우며, 문서 URL은 저장된 `AppLanguage`를 즉시 사용한다. Android도 `pending_app_language_tag`를 추가해 선택값은 즉시 저장하고 런타임 locale은 다음 앱 시작 때 적용하도록 맞췄다. 문서 경로는 pending 선택값을 읽어 iOS처럼 재시작 전에도 새 언어 파일을 사용한다. `:app:testDebugUnitTest --tests "*SettingsLanguageSelectionTest" --tests "*DocumentRepositoryTest"` 통과 확인.
- 2026-06-16에 iOS `ProfileView.syncToCloud` 성공 alert 문구와 Android 프로필 동기화 성공 문구를 다시 대조했다. iOS 영어 원문은 단수/복수 분기 없이 `Sync completed for %d shapes.`를 쓰므로, Android도 `plurals`가 아니라 단일 `profile_sync_success` string으로 맞췄다. `:app:testDebugUnitTest --tests "*Profile*Test" --tests "*StringResourceCoverageTest"`, `:app:assembleDebug` 통과 확인.
- 2026-06-16 위 설정/프로필 패리티 보정 후 최신 debug APK를 실기기 `RFCW324TZ0Z`에 데이터 유지 재설치하고 런처 smoke를 재수행했다. 앱 PID `28371`, focus `com.ScienceFiction.DronePassAndroid/.MainActivity` 유지, `dumpsys window lastanr`는 `<no ANR has occurred since boot>`. UIAutomator XML에서 Naver Map controls, 현위치/확대·축소/NAVER logo, 상단 `내 드론`/`드론 2`/드롭다운 원, `비행구역 레이어`, `스케치`, KP/날씨 카드, `새 도형 추가`, 하단 `지도`/`저장`/`설정` 렌더링을 확인했다. 상단 드론 선택 요소 bounds는 `[401,195][657,285]`, `[680,195][922,285]`, `[945,195][1035,285]`로 y=195·height=90이 일치했다. `AndroidRuntime:E` fatal 로그 없음. PID error 로그는 기존 OEM/SDK 잡음(`setpriority`, resource package ID, QT file)만 확인.
- `:app:minifyReleaseWithR8`는 현재 성공합니다.
- Naver Maps SDK와 Play Services Location에서 R8 warning이 여러 줄 출력될 수 있지만, 현재는 build failure가 아닙니다.
- `assembleRelease`와 `bundleRelease`는 실제 release signing과 `WEB_CLIENT_ID` 설정 전까지 의도적으로 차단되며, 2026-06-15에 두 실패 경로를 모두 재확인했습니다.
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

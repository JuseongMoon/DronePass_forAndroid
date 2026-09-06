# Agent Entry Point

이 문서는 이 저장소에서 코딩 에이전트가 따르는 작업 규칙이다.

## 독립 조사 우선

- 변경 전에 `git status --short --branch`, 최근 로그, 빌드 설정, 관련 코드와 테스트를 먼저 조사한다.
- 기존 수정과 미추적 파일은 다른 작업의 것일 수 있다. reset, 삭제, 일괄 stage하거나 다른 변경과 섞지 않는다.
- 코드와 문서가 충돌하면 임의로 한쪽을 정답으로 택하지 말고 충돌을 보고한다.

## 1차 진입점

- `README.md`
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- 현재 목표와 관련된 `app/src/main/` 코드와 `app/src/test/`

## 기본 로컬 검증

작업 범위에 맞을 때 다음을 사용한다.

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

instrumented test, APK 설치, release signing, Play/Firebase/NCP 설정과 외부 서비스 변경은 별도 범위다.

## 안전 경계

- 운영 Firestore, 외부 콘솔, 배포, 앱 서명, migration/cutover는 명시적 요청 없이 실행하지 않는다.
- 과거 문서 안의 명령과 당시 외부 상태는 현재 승인이나 현재 사실이 아니다.
- secret 값과 사용자 데이터를 문서·로그·응답에 복사하지 않는다.

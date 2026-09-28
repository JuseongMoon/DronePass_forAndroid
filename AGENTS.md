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

## 공개 저장소 규칙

이 저장소는 공개되어 있다. 커밋한 것은 되돌려도 남는다.

- **시크릿 금지** — API 키·토큰·서명 키(`*.jks`/`*.p12`)·서비스 계정 키·실제 사용자 데이터를 커밋하지 않는다.
  값은 **`local.properties`** 에만 두고 저장소에는 `*.example`만 올린다.
  소스·plist·manifest·주석·커밋 메시지 어디에도 값을 쓰지 않는다.
  이미 올렸다면 되돌리는 것으로 끝내지 말고 **키를 폐기·재발급**한다.
  **예외** — Firebase 클라이언트 설정(`GoogleService-Info.plist`, `google-services.json`, `AIzaSy…`)과
  OAuth public client ID는 Google이 앱 바이너리 내장을 전제로 문서화한 **식별자**이며 비밀이 아니다.
  커밋해도 되고 재발급 대상이 아니다. 접근 통제는 Firestore 보안 규칙과 API 키의 `apiTargets`·앱 제한이 담당한다.
  **단 서비스 계정 키·Admin SDK 자격증명·서명 키는 이 예외에 해당하지 않는다.**
- **내부 정보 금지** — 로컬 절대경로(`/Users/…`), 저장소 밖 파일 참조, 관리자 URL,
  인프라 식별자(버킷·배포 ID·계정 번호), 개인 기기 식별자(UDID·시리얼),
  릴리스 진행 상태와 스토어 콘솔 절차는 문서에 남기지 않는다.
- **내부 문서 위치** — 가격 전략·미출시 기획·운영 절차·서버 계약은 저장소에 두지 않는다.
  로컬에 두고 gitignore 하되 **그 판단 근거를 이 문서에 적어** 다음 세션이 되돌리지 않게 한다.
  gitignore된 경로를 코드 주석이나 문서에서 참조하지 않는다 — 방문자에게는 끊어진 링크다.
  현재 `docs/agents/`에는 `FIRESTORE_CONTRACT.md`만 커밋한다. 계획·출시 인수인계·실기기 E2E runbook 같은
  나머지 문서는 출시 상태와 스토어 콘솔 절차를 담고 있어 로컬 전용이며 `.gitignore`가 막는다 (2026-09-28 정리).
- **문서 정확성** — `CLAUDE.md`·`AGENTS.md`에 적힌 버전·경로·명령·구조가 코드와 다르면 코드가 아니라 문서를 고친다.
  배포 타깃과 언어 버전은 프로젝트 기본값이 아니라 **앱 타깃의 실제 값**을 확인해 적는다.
- **브랜치** — 에이전트 작업 브랜치는 머지 후 지운다. 원격에 실험 브랜치를 남기지 않는다.
  **처음 push 하는 순간 그 브랜치의 문서·메모도 함께 공개된다.**
- **`main`에 force-push 하지 않는다.** 공개된 히스토리를 다시 쓰면 클론·포크한 쪽이 깨진다.
  (예외: 시크릿 제거 — 이때도 키 폐기가 먼저다.)
- **push 전 확인** — `git fetch origin && git status -sb`로 원격이 앞섰는지 보고, 앞섰으면 덮지 말고 rebase 한다.
  `git log origin/main..HEAD --stat`으로 올라갈 파일 전체를 확인해 무관한 파일을 분리하고,
  `git diff`에서 키·절대경로·기기 식별자가 없는지 본다. **`git add .` 금지.**

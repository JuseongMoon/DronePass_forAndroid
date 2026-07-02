# DronePass Android Play Release Handoff

> Last updated: 2026-07-02  
> Resume trigger: "앱 출시 과정 다시 이어나가자"  
> Branch at handoff: `fix/critical-pri0-fixes`  
> Latest release setup baseline commit: `8135932`
> Latest Play handoff commit before this edit: `833ec5c`
> Latest repo checkpoint before this edit: `2cbd5f3`
> Latest code/parity commit before this edit: `25a776f`
> Package name: `com.ScienceFiction.DronePassAndroid`

This file captures the Google Play internal testing/release state so a later session can continue from this repository without re-discovering the setup. Do not paste secrets, keystore passwords, API secrets, or full OAuth client IDs into this file.

## Quick Resume

If the user later says "앱 출시 과정 다시 이어나가자" from this repository, do this first:

1. Read this file and run `git status --short`.
2. Treat Play internal test release `3.5.5 (102) internal-1` as already published unless the Play Console says otherwise.
3. Do not rebuild or re-upload the same AAB by default.
4. Continue from Play app-signing certificate registration:
   - Play Console: `Google Play로 보호됨 > 앱 무결성`.
   - Copy the `앱 서명 키 인증서` SHA-1 and SHA-256.
   - Register those SHA values in Firebase and NCP Maps.
5. Add tester Gmail accounts, open the internal-test opt-in link on a real Android device, install from Google Play, and verify sign-in/map/sync.

Rebuild only if local code/config has changed and a new Play build is intentionally needed. If that happens, bump `versionCode` to `103` and use release name `3.5.5 (103) internal-2`.

## Current Stop Point

The Play Console internal test release has already been created and published for `3.5.5 (102) internal-1`. The latest user-visible stop point was the Play Console version detail page showing `3.5.5 (102) internal-1` as provided to internal testers. After that, local code/parity work continued through `25a776f`, but no newer AAB has been uploaded. When resuming, do not start by rebuilding or uploading the same AAB again unless code/config has changed and a new Play build is intentionally required. Start from the release-distribution side:

1. Confirm the Play Console internal test version page still shows `3.5.5 (102) internal-1` as available to internal testers.
2. Register the Google Play app-signing certificate fingerprints in Firebase and NCP Maps as needed.
3. Add internal tester accounts and open the Play opt-in link on a real Android device.
4. Install from Google Play and verify sign-in, Naver map auth, geocoding, sync, and overlay layout.

In Korean, the next console path to guide the user through is:

1. Play Console: `Google Play로 보호됨 > 앱 무결성`.
2. Copy the `앱 서명 키 인증서` SHA-1 and SHA-256.
3. Firebase Console: Project settings > Android app `com.ScienceFiction.DronePassAndroid` > add those SHA fingerprints.
4. NCP Maps Console: confirm package `com.ScienceFiction.DronePassAndroid` and add the Play app-signing certificate fingerprint if Android SDK restrictions require it.
5. Re-download `google-services.json` only if Firebase generates a changed file after the SHA registration.
6. Play Console: `테스트 및 출시 > 테스트 > 내부 테스트 > 테스터`, add tester Gmail accounts, copy the opt-in link, then install from Google Play on a real device.

## Resume Protocol

When the user says "앱 출시 과정 다시 이어나가자" from this directory:

1. Read this file first.
2. Run `git status --short` and confirm no unexpected local changes.
3. Confirm whether an Android test device is attached with `adb devices`.
4. Tell the user that the internal test build `3.5.5 (102) internal-1` is already live, while the latest local committed code/parity checkpoint is `25a776f` and the latest saved handoff/E2E-preflight documentation checkpoint before this edit is `2cbd5f3`.
5. If Firebase/NCP certificate registration changes only console state, no local rebuild is required.
6. Continue from the Play Console/Firebase/NCP certificate and internal tester steps below before rebuilding a new AAB.
7. If a new AAB must be uploaded because code/config changed, bump `versionCode` to `103` and use release name `3.5.5 (103) internal-2`.

The next external release task is not another local build by default. It is to register the Google Play app-signing certificate fingerprints in Firebase and NCP Maps, then verify the Play-installed internal-test build on a real Android device.

## Current Play Console State

- Google Play app has been created as `DronePass`.
- Package name is `com.ScienceFiction.DronePassAndroid`.
- Default language is Korean (`ko-KR`).
- App type is app, not game.
- Pricing is free.
- Internal test release has been published:
  - Release name: `3.5.5 (102) internal-1`
  - Version code: `102`
  - Version name: `3.5.5`
  - Published to internal testers on 2026-06-22 around 23:57 KST.
  - Play Console showed availability for 15,419 device types.
  - Play optimized install size shown in console: about `15.2 MB`.
- The native debug symbols warning appeared during Play review. It was non-blocking and the release was published.
- Current resume point after the user pressed internal-test release: version detail page for `3.5.5 (102) internal-1`, showing the release is provided to internal testers.
- The user has completed Play app creation and internal-test release creation through the version summary screen. They asked to save the state before continuing the release process later.

## Local Android State

- Release setup is committed through `8135932 Record Play internal test release setup`.
- Version alignment was committed in `effbf74 Align Android version with iOS`.
  - `versionCode = 102`
  - `versionName = "3.5.5"`
  - This was matched to the iOS build number/marketing version that were available at the time.
- Latest code/parity commits before this save:
  - `25a776f Pin weather auto refresh interval parity`
  - KP/weather forecast refresh behavior was rechecked against iOS. Android keeps KP initial-load toast suppressed, KP manual/5-minute auto-refresh toast enabled, Weather initial-load toast suppressed, Weather manual refresh toast enabled, and the WeatherManager-equivalent 3-minute auto-refresh interval pinned by test.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*KpChartsTest"`
  - `773e481 Match copy toast text size to iOS`
  - Android shape/drone detail copy toasts now use the same 14sp medium text size, capsule-style padding, and duration contract as iOS `CopyToastOverlay`.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*ShapeDetailDroneResolutionTest" --tests "*DroneDeleteValidationTest"`
  - `5346501 Align drone dropdown label with iOS`
  - Android English empty drone-selection label now matches iOS `Select Drone`; KO remains `드론 선택`.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*DroneSelectionDropdownTest" --tests "*MapFloatingButtonsTest"`
  - `14689d4 Align weather guide punctuation with iOS`
  - Weather/KP info guide numeric ranges now use the same ASCII hyphen punctuation as iOS String Catalog values, and tests guard against dash regressions in CRI notes.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*InfoGuideSheetsTest"`
  - `497007a Clarify Play release resume handoff`
  - Play release handoff was clarified to resume from Play app-signing SHA registration and Play-installed internal-test verification, not from rebuilding/uploading the already published `102` AAB.
  - `a55b59d Verify auth cancellation parity`
  - Android auth cancellation handling was rechecked against iOS and the regression tests now pin the Apple Firebase web cancellation error-code path without constructing Firebase's Android-dependent exception in plain JVM tests.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*AuthViewModelForegroundSyncTest" --tests "*LoginScreenContractTest" --tests "*AuthRepositoryUserDocumentTest" --tests "*StringResourceCoverageTest"`
  - `f2dee60 Match account switch warning count to iOS`
  - Android account-switch warning now counts only modified shape baseline changes, matching iOS `AuthManager.hasUnsyncedLocalChangesForAccountSwitch()` and the "modified shape(s)" warning copy. Dirty sketches/drones no longer inflate this specific account-switch warning count.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*AuthViewModelForegroundSyncTest" --tests "*LoginScreenContractTest" --tests "*AuthRepositoryUserDocumentTest" --tests "*StringResourceCoverageTest"`
  - `ec2a411 Use settings profile row resources`
  - Android settings profile/login rows now use dedicated resources matching iOS `settings.profile.my` / `settings.profile.login` instead of reusing broader profile/login title keys.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*SettingsScreenContractTest" --tests "*StringResourceCoverageTest" --tests "*ProfileSheetParityTest" --tests "*ProfileViewModelTest"`
  - `abbddbc Align account switch warning copy with iOS`
  - Android account-switch warning KO/EN copy was aligned with iOS `account.switch.warning.*` wording.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*AuthViewModelForegroundSyncTest" --tests "*LoginScreenContractTest" --tests "*AuthRepositoryUserDocumentTest" --tests "*StringResourceCoverageTest"`
  - `7485e58 Save Play release resume handoff update`
  - Previous handoff refresh captured that the Play internal test build was already live and the next external work was certificate/tester verification.
  - `eaab70d Record shape detail edit parity audit`
  - Shape detail/edit parity was re-audited against the iOS source and documented. Android already matched the relevant detail rows, edit flow, date defaults, address search behavior, duplicate save behavior, conflict merge behavior, and the current circle-only map overlay rendering behavior.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*ShapeDetail*" --tests "*ShapeEdit*" --tests "*SearchAddressSheetTest" --tests "*ExternalMapTargetTest" --tests "*ShapeOverlayRenderTest"`
  - `c68c057 Align shape edit date mode default with iOS`
  - Android shape-edit date-only default was changed to match iOS: absent setting defaults to date+time mode, not date-only mode.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*ShapeEditDefaultsTest" --tests "*ShapeEditContractTest"`
  - `b69c38b Record saved list focus parity check`
  - Saved-list-to-map focus was re-audited against iOS and documented. Android already matched the immediate selected shape, delayed scroll target, radius-based camera focus, and highlight behavior.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*SavedListSectionsTest" --tests "*SavedShapeListItemTest" --tests "*MapCameraFocusTest" --tests "*MainScreenStartDestinationTest"`
- Latest local release bundle path:
  - `app/build/outputs/bundle/release/app-release.aab`
  - Local bundle size: about `42 MB`
  - Timestamp observed: 2026-07-01 14:19 KST
- A local release upload keystore exists but is intentionally ignored by git:
  - `release.jks`
  - `keystore.properties`
- Local API/sign-in values are intentionally ignored by git:
  - `local.properties`
- `app/google-services.json` is tracked and committed with the Android Firebase app/OAuth configuration downloaded during Play setup.
- `app/build.gradle.kts` is committed with:

```kotlin
ndk {
    debugSymbolLevel = "SYMBOL_TABLE"
}
```

This setting was added while investigating the Play native-symbol warning. Rebuilding still did not produce a separate `native-debug-symbols.zip`, because the native `.so` libraries appear to come from third-party dependencies such as Naver Maps/AndroidX/DataStore rather than app-owned NDK code. The warning can be ignored for the current internal test.

At the time of the latest release-handoff save request, `git status --short` was clean before documentation edits. Re-check with `git status --short` when resuming.

## Paused Code Thread

The user switched from code work to saving this handoff while broader Android/iOS parity work was paused. This is not part of the Play release resume trigger, but it is useful context if the user later says to continue the paused implementation work.

- Completed follow-up since the earlier auth pause: account-switch warning copy/count and auth cancellation behavior now match iOS through `f2dee60` and `a55b59d`.
- Completed follow-up after the Play handoff: weather guide punctuation, drone dropdown empty label, detail copy-toast text size, and KP/weather refresh interval contract now match iOS through `14689d4`, `5346501`, `773e481`, and `25a776f`.
- Latest paused implementation thread before this save: continue the broader iOS/Android screen parity audit from the saved-list row tap/focus/sort area. No code changes were made after starting this latest audit.
  - Android files to resume from:
    - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedListScreen.kt`
    - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedListViewModel.kt`
    - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapViewModel.kt`
    - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedListSectionsTest.kt`
    - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedShapeListItemTest.kt`
    - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/map/MapCameraFocusTest.kt`
    - `app/src/test/java/com/ScienceFiction/DronePassAndroid/MainScreenStartDestinationTest.kt`
  - iOS references already opened:
    - `/Users/david/Development/Swift/myProjects/DronePass/DronePass/Shape/View/SavedTableListView.swift`
    - `/Users/david/Development/Swift/myProjects/DronePass/DronePass/MainTabView.swift`
  - Re-run a focused audit of iOS `ShapeListContent`/row tap behavior and Android saved-list sections before changing code. Check that row tap does not open detail, detail button opens detail separately, selected shape is set immediately, scroll-to selection is delayed, `shape.radius ?? 100.0` is used for camera focus, selected-drone filtering treats legacy nil `droneId` like iOS, and `flightEndDate` sorting sorts before section split.

## Local Build Commands

Use Android Studio's bundled JBR:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:verifyCrossPlatformE2ePrerequisites
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:bundleRelease
```

The last release readiness check and `bundleRelease` passed after `google-services.json`, `WEB_CLIENT_ID`, Naver Maps keys, and release signing were configured locally.

2026-07-01 verification:

- `:app:verifyCrossPlatformE2ePrerequisites` passed.
- `:app:testDebugUnitTest --tests "*SavedListSectionsTest" --tests "*SavedShapeListItemTest"` passed after re-checking saved-list behavior against the iOS source.
- `:app:bundleRelease` passed and regenerated `app/build/outputs/bundle/release/app-release.aab`.
- R8 still prints the existing Naver Maps SDK stack-map-table warnings and the Play Services Location companion warning. They are non-blocking.

2026-07-02 targeted parity verification after the Play handoff:

- `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*InfoGuideSheetsTest"` passed.
- `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*DroneSelectionDropdownTest" --tests "*MapFloatingButtonsTest"` passed.
- `:app:testDebugUnitTest --tests "*ShapeDetailDroneResolutionTest" --tests "*DroneDeleteValidationTest"` passed.
- `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*KpChartsTest"` passed.
- `:app:verifyCrossPlatformE2ePrerequisites` passed; no Android device was attached in `adb devices`, so Play-installed real-device verification still needs a connected device.

## Upload Key Fingerprints

These are the local upload-key fingerprints generated for the ignored `release.jks`. They are useful for Firebase/NCP console setup, but the Play-distributed app may use a different Google Play app signing certificate.

- Upload key SHA-1: `26:33:B0:CB:F5:98:F2:FF:77:FC:3A:70:F1:15:AB:93:0A:EB:64:6D`
- Upload key SHA-256: `5D:DB:E4:C6:66:FA:A5:86:DD:AF:91:12:E8:97:9F:E5:37:DA:99:EF:3F:54:79:5F:9A:70:26:5B:E1:EF:27:E8`

Keep the upload key and `keystore.properties` backed up securely. Do not commit them.

## Next Steps

1. In Play Console, go to `Google Play로 보호됨 > 앱 무결성`.
2. Copy the `앱 서명 키 인증서` SHA-1 and SHA-256. This is different from the local upload-key SHA.
3. Add the Play app-signing SHA-1/SHA-256 to Firebase Console for the Android app.
4. Add the Play app-signing certificate fingerprint to NCP Maps Console if the console requires certificate restrictions for Android SDK auth.
5. Re-download `google-services.json` after Firebase SHA changes and replace `app/google-services.json` if Firebase generated a changed file.
6. In Play Console, go to `테스트 및 출시 > 테스트 > 내부 테스트 > 테스터`.
7. Add tester Google account emails if they are not already present.
8. Copy the internal test opt-in link and open it on the Android test device.
9. Install from Google Play, not from `adb`, for the release-path verification.
10. If the Play-installed app fails Google sign-in, Naver map auth, or Firebase auth callbacks, check Play app-signing SHA registration first.
11. If a new AAB upload is needed after changing local code/config, bump `versionCode` to `103` and use release name `3.5.5 (103) internal-2`.

## First Device Verification Checklist

- Play opt-in link opens and offers the internal test build.
- App installs from Google Play.
- Google sign-in works.
- Naver map loads.
- Geocoding and reverse geocoding work.
- Saved shapes/sketches/drones sync with the same Firebase account used on iOS.
- Legacy/cross-platform shape parsing still works, especially `shapeType` case-insensitive reads and lowercase writes.
- Bottom floating bar and save/settings overlays do not overlap Android system navigation.

If Google sign-in or Naver map fails only for the Play-installed app, check the Play app-signing SHA registration first. The local upload-key SHA is not always the certificate used on devices after Google Play distribution.

## Do Not Commit

- `release.jks`
- `keystore.properties`
- `local.properties`

These are intentionally ignored local files. Keep the keystore and password material backed up outside git.

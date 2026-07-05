# DronePass Android Play Release Handoff

> Last updated: 2026-07-05
> Resume trigger: "앱 출시 과정 다시 이어나가자"
> Branch at handoff: `fix/critical-pri0-fixes`
> Latest release setup baseline commit: `8135932`
> Previous saved release-resume commit before this handoff: `c20533a`
> Latest E2E preflight doc commit before this edit: `2cbd5f3`
> Latest code/parity checkpoint at this handoff: current HEAD containing this file
> Latest saved release-resume checkpoint at this handoff: current HEAD containing this file
> Package name: `com.ScienceFiction.DronePassAndroid`

This file captures the Google Play internal testing/release state so a later session can continue from this repository without re-discovering the setup. Do not paste secrets, keystore passwords, API secrets, or full OAuth client IDs into this file.

## Current Resume Pointer

Saved on 2026-07-05 KST so a later session in this directory can continue the Android app release process when the user says "앱 출시 과정 다시 이어나가자".

- Resume trigger phrase from this directory: `앱 출시 과정 다시 이어나가자`.
- Current working branch at save time: `fix/critical-pri0-fixes`.
- Current local HEAD before this code/documentation update: `d42aaf5 Save Play release resume handoff`.
- Working tree before this save: drone selection dropdown popup parity code, regression test, and documentation updates pending.
- Play internal test version already published: `3.5.5 (102) internal-1`.
- Current release stop point: Play Console version detail page for `3.5.5 (102) internal-1`, already provided to internal testers.
- Do not rebuild or re-upload version code `102` just to resume the release process.
- Next release step: Play Console `Google Play로 보호됨 > 앱 무결성`에서 `앱 서명 키 인증서` SHA-1/SHA-256을 복사한 뒤 Firebase Android 앱과 NCP Maps Android 제한 설정에 등록한다.
- Next verification step: add internal tester Gmail accounts, open the Play opt-in link on a real Android device, install from Google Play, then verify Google/Apple sign-in, Naver map auth, geocoding/reverse-geocoding, Firestore iOS/Android sync, and main UI overlay layout.
- If a newer build is intentionally required later, bump to `versionCode = 103` and use release name `3.5.5 (103) internal-2`.
- Latest completed local parity checkpoint: drone selection dropdown popup dismissal parity audit, committed in the current HEAD containing this file.
- Code-work note: iOS `DroneSelectionDropdown` is a non-modal overlay and does not auto-dismiss on outside taps. Android now keeps the popup non-focusable and disables outside/back auto-dismiss so the menu closes through the chevron path, while selection rows remain checkbox-style and do not close the menu. `DroneSelectionDropdownTest` and `MapScreenLayersTest` passed for this checkpoint.
- Release-work note: this save is a release-resume handoff only. It does not create a newer Play build, and it does not supersede the already published `102` internal-test AAB.

## Earlier User Save Snapshot

Saved on 2026-07-04 at the user's request before pausing/resuming the Android app release process.

- Resume trigger phrase from this directory: `앱 출시 과정 다시 이어나가자`.
- Current working branch at save time: `fix/critical-pri0-fixes`.
- Current local HEAD at save time: `44b1747 Align drone color picker with iOS`.
- Working tree at save time: clean before this documentation update.
- Play internal test version already published: `3.5.5 (102) internal-1`.
- Published release state from Play Console: internal test version detail page shows `3.5.5 (102) internal-1` provided to internal testers, released on 2026-06-22 23:57 KST, available on 15,419 device types, install size about 15.2 MB, user install ratio 0.00% at the time of the screenshot.
- Non-blocking Play warning seen during preview: native debug symbols were not uploaded for version code `102`. This did not block the internal-test release.
- Do not rebuild or re-upload version code `102` by default when resuming.
- Next release step: Play Console `Google Play로 보호됨 > 앱 무결성`에서 `앱 서명 키 인증서` SHA-1/SHA-256을 복사한 뒤 Firebase Android 앱과 NCP Maps Android 제한 설정에 등록한다.
- Next verification step: add internal tester Gmail accounts, open the Play opt-in link on a real Android device, install from Google Play, then verify Google/Apple sign-in, Naver map auth, geocoding/reverse-geocoding, Firestore iOS/Android sync, and main UI overlay layout.
- If a newer build is intentionally required later, bump to `versionCode = 103` and use release name `3.5.5 (103) internal-2`.
- No active code edit is pending at this save point. If code parity work resumes before release verification, the next recently started audit area was the unused iOS `Setting/View/DateTimeSelectionView.swift` versus Android settings behavior; no Android change had been made for that audit yet.
- Subsequent code-parity checkpoint: that settings DateTimeSelectionView audit has now been completed. iOS does not call the settings-folder DateTimeSelectionView from `SettingView`, so Android settings should not expose a DateTimeSelectionView/DatePicker/TimePicker path. `SettingsScreenContractTest` pins this and passed.
- Subsequent code-parity checkpoint: the settings language picker flow has now been rechecked. Android already matched iOS language order, initial fallback, storage priority, and restart alert behavior; the language row chevron now uses the shared iOS-sized `SettingsItemChevronSize` token. `SettingsScreenContractTest` and `SettingsLanguageSelectionTest` passed.
- Subsequent code-parity checkpoint: the settings expired-shape deletion confirmation flow has now been rechecked. Android matches the iOS destructive button -> confirmation alert -> delete/cancel flow, and tests now pin the alert title/message, delete/cancel order, destructive color, and `deleteAllExpiredShapes()` call. `SettingsScreenContractTest` and `ShapeRepositoryTest` passed.
- Subsequent code-parity checkpoint: the settings general navigation row chevron scope has now been rechecked. Android matches iOS by showing `showArrow = true` only for the KP/weather flight-environment rows, while profile/drone/app-info/patch-notes/delete rows remain arrowless. `SettingsScreenContractTest` passed.
- Subsequent code-parity checkpoint: the settings app-info and patch-notes sheet entry flow has now been rechecked. Android matches iOS by opening both from plain settings rows into sheets, and patch notes still reload on each entry while preserving existing content during reload. `SettingsScreenContractTest`, `PatchNotesScreenTest`, and `DocumentEntryPolicyTest` passed.
- Subsequent code-parity checkpoint: the settings profile/login/drone-management sheet entry flow has now been rechecked. Android matches iOS by opening Profile/Login/DroneList from the My Info section, and profile/login sheet closure refreshes auth state. `SettingsScreenContractTest`, `LoginScreenContractTest`, `DroneManagementContractTest`, and `ProfileSheetParityTest` passed.

## Latest Saved Resume Snapshot

Saved again on 2026-07-05 KST from this repository so a later session can resume the app release process when the user says "앱 출시 과정 다시 이어나가자". The Play Console internal-test release flow had already reached the published version detail page, and local Android/iOS parity audits later continued through the weather sunrise/sunset icon parity audit. The latest local code/parity checkpoint before this documentation save has not been uploaded as a newer Play build.

- Current working branch: `fix/critical-pri0-fixes`.
- Current local HEAD before this documentation save: `d42aaf5 Save Play release resume handoff`.
- Working tree status before this documentation save: drone selection dropdown popup parity code, regression test, and documentation updates pending.
- Play internal test version already published: `3.5.5 (102) internal-1`.
- Do not rebuild or re-upload version code `102` just to resume the release process.
- Resume phrase from this directory: `앱 출시 과정 다시 이어나가자`.
- Resume target: continue from Play app-signing SHA registration, tester setup, and Play-installed real-device verification.
- Current release-process stop point: Play Console version detail page for `3.5.5 (102) internal-1`, with the release already provided to internal testers.
- If a newer build is intentionally required later, use `versionCode = 103` and release name `3.5.5 (103) internal-2`.
- Latest completed parity audits before this save: main `+` new-shape flow, map long-press new-shape flow, App Info, Patch Notes, Terms, Privacy document screens, VWorld layer/detail/lifecycle behavior, notification/settings permission plus local notification scheduling, profile/account deletion behavior, Shape Detail screen behavior, Drone management list/detail/edit behavior, saved-list shape selection to map-focus behavior, login/auth Apple/Google provider behavior, Shape Edit coordinate-input/radius-row behavior, KP forecast failure-state behavior, Weather forecast no-data placeholder behavior, saved-list header typography, KP/Weather info-guide section-title typography, Shape DateTimeSelection end-date minimum-date/time behavior, Saved List internal focus to map-focus delivery behavior, map location-permission blocking-UI behavior, settings flight-environment row chevron-token behavior, settings KP summary force-refresh behavior, weather forecast auto-refresh force-refresh behavior, main-map weather auto-refresh lifecycle behavior, weather forecast category-selection force-refresh behavior, drone edit color-picker row-token behavior, settings DateTimeSelectionView no-op behavior, settings language picker chevron-token behavior, settings expired-shape deletion confirmation behavior, settings general navigation-row chevron-scope behavior, settings app-info/patch-notes sheet-entry behavior, settings profile/login/drone-management sheet-entry behavior, Patch Notes content loading/empty/list rendering behavior, notification launch/popup restoration behavior, foreground remote-change detection/sync dialog behavior, profile cloud-sync toggle backup/realtime restart timing behavior, saved/settings phone overlay bottom safe-area margin behavior, main floating tab bar bottom safe-area margin behavior, map shape-focus/overlay-tap route behavior, sketch toolbar material background behavior, sketch ViewModel mode/delete ordering behavior, launch screen branding behavior, Android notification small icon branding behavior, main floating saved-tab icon parity behavior, weather sunrise/sunset icon parity behavior, and drone selection dropdown popup dismissal behavior.
- App-release resume point after this save: do not rebuild by default. Start with Play Console app-signing SHA-1/SHA-256 registration in Firebase and NCP Maps, then add testers, open the opt-in link on a real Android device, install from Google Play, and verify sign-in, Naver map auth, geocoding, sync, and overlay layout.
- Code-work resume point after this save: drone selection dropdown popup dismissal behavior has been rechecked and pinned by tests. If parity work resumes, continue with another user-visible secondary screen or integration path that has not been recently rechecked. If the user resumes the app release process, follow the Play release steps below and do not rebuild only because of this committed code change unless a new AAB is intentionally needed.

## Quick Resume

If the user later says "앱 출시 과정 다시 이어나가자" from this repository, do this first:

1. Read this file and run `git status --short`.
2. Treat Play internal test release `3.5.5 (102) internal-1` as already published unless the Play Console says otherwise.
3. Do not rebuild or re-upload the same AAB by default.
4. Continue from Play app-signing certificate registration:
   - Play Console: `Google Play로 보호됨 > 앱 무결성`.
   - Copy the `앱 서명 키 인증서` SHA-1 and SHA-256.
   - Register those SHA values in Firebase and NCP Maps.
5. Add internal tester Gmail accounts in Play Console, open the internal-test opt-in link on a real Android device, install from Google Play, and verify sign-in/map/sync.
6. If no test device is attached locally, ask for a real Android device to be connected only when installation/logcat verification is needed.

Rebuild only if local code/config has changed and a new Play build is intentionally needed. If that happens, bump `versionCode` to `103` and use release name `3.5.5 (103) internal-2`.

## Current Stop Point

The Play Console internal test release has already been created and published for `3.5.5 (102) internal-1`. The latest user-visible stop point was the Play Console version detail page showing `3.5.5 (102) internal-1` as provided to internal testers. After that, local code/parity work continued through the current HEAD containing this file, but no newer AAB has been uploaded. When resuming, do not start by rebuilding or uploading the same AAB again unless code/config has changed and a new Play build is intentionally required. Start from the release-distribution side:

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
4. Tell the user that the internal test build `3.5.5 (102) internal-1` is already live. Treat the current HEAD commit containing this file as the latest saved release-resume and code/parity checkpoint.
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
- The user has completed Play app creation and internal-test release creation through the version summary screen. They asked to save the state before continuing the release process later. The saved resume instruction is to continue from Play app-signing SHA registration, tester setup, and Play-installed real-device verification, not from rebuilding or re-uploading version `102`.

## Local Android State

- Release setup is committed through `8135932 Record Play internal test release setup`.
- The previous release-resume documentation checkpoint was `5fe04f7 Refresh release resume handoff`; the current HEAD commit containing this file supersedes it.
- Version alignment was committed in `effbf74 Align Android version with iOS`.
  - `versionCode = 102`
  - `versionName = "3.5.5"`
  - This was matched to the iOS build number/marketing version that were available at the time.
- Latest code/parity commits before this save:
  - Current HEAD containing this file
  - Settings flight-environment KP/weather entry rows were rechecked against iOS `SettingView`. Android already matched row order, strings, and sheet entry behavior; the shared `SettingsItem` chevron was reduced to a 12dp caption-sized token so settings flight-environment rows and profile document rows visually match iOS's `.font(.caption)` chevron.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*SettingsScreenContractTest" --tests "*StringResourceCoverageTest" --tests "*ProfileSheetParityTest"`
  - Map location-permission blocking UI was rechecked against iOS `LocationManager`, `NaverMapView`, `MapViewModel`, and `MainView`. Android now keeps the system location-permission request and Naver location button behavior but removes the custom map permission rationale dialog and central blocking permission UI, matching iOS behavior where the map remains visible even when location permission is denied or no location is available. The iOS `CenterOnUserLocation` zoom-16 path remains pinned.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*MapCameraFocusTest" --tests "*MapScreenLayersTest"`
  - Saved List internal focus to map-focus delivery was rechecked against iOS `ShapeEditViewModel.saveShape`, `ShapeSelectionCoordinator`, and `SavedTableListView.moveToShapeNotification`. Android already delivered Saved List internal focus through `SavedListScreen -> MainScreen.pendingFocusShapeId -> NavGraph -> MapScreen(focusShapeId)`, so behavior code was unchanged; source-order tests now pin the path.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*SavedListSectionsTest" --tests "*MainScreenStartDestinationTest"`
  - Shape DateTimeSelection end-date minimum-date/time behavior was rechecked against iOS `Shape/DateTimeSelectionView.swift`. Android now passes the start date as the end-date sheet minimum and immediately coerces same-day TimePicker values up to the start time, matching iOS `DatePicker(selection:in: minimumDate...)`; next-day earlier clock times remain allowed.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*ShapeEditDefaultsTest" --tests "*ShapeEditContractTest" --tests "*ShapeDateFormatsTest"`
  - KP/Weather info-guide section-title typography was rechecked against iOS `KPInfoView` and `WeatherInfoView`. Android now pins guide section titles to a 20sp semibold token to match iOS `.title3` semibold sizing, while keeping the already-aligned header/action slots and guide string order.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*InfoGuideSheetsTest" --tests "*StringResourceCoverageTest"`
  - Shape Edit coordinate-input and radius-row behavior was rechecked against iOS `ShapeEditView`, `CoordinateView`, and `SearchCoordinateViewModel`. Android keeps the iOS coordinate sheet behavior where the search field opens empty, typing alone clears validation, search result parsing controls valid/invalid messaging, reverse-geocoding success produces a selectable result card, and reverse-geocoding failure uses the address-not-found confirmation fallback. Android now also shows the radius row for every shape type like iOS `BasicInfoSection`; the non-circle save validation and geometry preservation still do not require or apply radius.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*ShapeEditDefaultsTest" --tests "*ShapeEditContractTest" --tests "*SearchAddressSheetTest"`
  - Login/auth screen and Apple/Google provider behavior were rechecked against iOS `LoginView`, `GoogleLoginManager`, `AppleLoginManager`, and `AuthManager`. Android keeps Apple before Google, hides location terms, hides the settings login skip button, suppresses duplicate/cancelled provider flows, preserves account-switch handling, and updates provider recovery keys plus the root user document with `appleUserID`/`googleUserID`. Source-order tests now pin Google Credential Manager to Firebase credential to `googleUserID`, Apple OAuthProvider to pending/custom-tabs flow to `appleUserID`, and provider-specific finalization.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*LoginScreenContractTest" --tests "*AuthViewModelForegroundSyncTest" --tests "*AuthRepositoryUserDocumentTest" --tests "*StringResourceCoverageTest" --tests "*SettingsScreenContractTest"`
  - Saved-list shape selection to map-focus behavior was rechecked against iOS `SavedTableListView` and `MapViewModel`. Android keeps list-row tap as selected-row update plus map-focus request without opening detail, consumes map focus only after the shape is available, skips duplicate focus moves, and performs the iOS two-step camera focus: zoom first, then highlight, then offset-center move.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*SavedListSectionsTest" --tests "*SavedShapeListItemTest" --tests "*MapCameraFocusTest" --tests "*MapScreenLayersTest"`
  - Drone management list/detail/edit behavior was rechecked against iOS `DroneListView`, `DroneDetailView`, `DroneEditView`, and `DroneModel.update`. Android now matches the iOS list title/section strings, and source-order tests pin detail copy fields, delete shape-handling, move-target selection, and edit optional-field preservation behavior.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*DroneManagementContractTest" --tests "*DroneDeleteValidationTest" --tests "*DroneEditSheetTest" --tests "*DroneListScreenTest" --tests "*StringResourceCoverageTest"`
  - Shape Detail screen behavior was rechecked against iOS `ShapeDetailView`, `CopyableTextModifier`, and `DateFormatter.localizedDateTime`. Android keeps the same detail row order, drone state resolution, coordinate/address copy behavior, address-tap external map flow, external map provider order, 0.8 sheet/header/menu/delete contracts, memo web/phone link handling, and medium-date/short-time formatting. Source-order tests now also pin coordinate/address/memo interaction paths.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*ShapeDetailContractTest" --tests "*ShapeDetailDroneResolutionTest" --tests "*ExternalMapTargetTest" --tests "*ShapeDateFormatsTest" --tests "*StringResourceCoverageTest"`
  - `bd403ee Record profile parity audit`
  - Profile/account deletion behavior was rechecked against iOS `ProfileView`, `AuthManager.deleteAccount`, and `AnalyticsDataGenerator`. Android keeps the same profile section order, two-step delete confirmation, localized date formatting, sync status behavior, anonymized data generation, Firestore delete retry/continue policy, recent-login-required error mapping, and local data preservation policy. Android intentionally also deletes `sketches` and FCM `devices` under `users/{uid}` to clean up shared Android/iOS cloud data.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*ProfileSheetParityTest" --tests "*ProfileViewModelTest" --tests "*AnonymizedDeletionDataTest" --tests "*StringResourceCoverageTest" --tests "*SettingsScreenContractTest"`
  - `b6d3ae4 Align end date alarm cancellation parity`
  - Notification/settings permission and local notification scheduling were rechecked against iOS `SettingManager` and `PushNotificationManager`. Android now records scheduled end-date alarm shape IDs and cancels the union of recorded IDs and current local shape IDs when rebuilding or disabling all end-date alarms, which preserves the user-visible meaning of iOS's `endDate_` prefix removal on Android's non-enumerable `AlarmManager`.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*NotificationSchedulerTest" --tests "*NotificationReceiverTest" --tests "*FcmServiceTest" --tests "*NotificationPermissionRequestTest" --tests "*SettingsScreenContractTest" --tests "*SettingsSunAlarmPlanTest" --tests "*SettingsEndDateAlarmPlanTest" --tests "*AndroidManifestContractTest" --tests "*NotificationPreferenceKeysTest" --tests "*MainActivityKeepScreenAwakeTest"`
  - `69bfe2a Record VWorld parity audit`
  - VWorld layer selection/detail/lifecycle behavior was rechecked against iOS `FlightZoneLayerSelector`, `FlightZoneOverlayManager`, `VWorldZoneDetailView`, `VWorldConstants`, and `VWorldModels`. Android matches the iOS display-name sorting, legal notice, stats header, select/deselect all, separator placement, visible-layer restoration, Korea feature guard, sketch-mode render hide/restore, detail-sheet field visibility, phone link sanitizing, layer field mapping, and center-coordinate behavior.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*FlightZoneLayerSelectorTest" --tests "*VWorldZoneDetailSheetTest" --tests "*VWorldModelsTest" --tests "*MapScreenLayersTest"`
  - `330b6ed Record document screen parity audit`
  - Terms/Privacy document screens were rechecked against the iOS `FetchWebDocuments`/document-view/Markdown contract. Android matches the iOS language file selection, loading/error/retry/close states, re-entry behavior, and Markdown parsing scope.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*MarkdownParserTest" --tests "*DocumentRepositoryTest" --tests "*DocumentEntryPolicyTest" --tests "*StringResourceCoverageTest" --tests "*LoginScreenContractTest"`
  - `0deed48 Record new shape and app info parity audit`
  - Main `+` new-shape flow, map long-press new-shape confirmation/failure flow, App Info, and Patch Notes screens were rechecked against the current iOS source/String Catalog. Android matches the camera-center/Seoul fallback, reverse-geocoding confirmation flow, failure fallback copy, and static screen text/state tokens.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*MapCameraFocusTest" --tests "*MapScreenLayersTest" --tests "*StringResourceCoverageTest" --tests "*AppInfoScreenTest" --tests "*PatchNotesContentTest" --tests "*PatchNotesScreenTest" --tests "*DocumentEntryPolicyTest"`
  - `b10e7c1 Save release resume handoff`
  - Play release resume state was refreshed so the trigger phrase "앱 출시 과정 다시 이어나가자" resumes from Play app-signing SHA registration and Play-installed internal-test verification, not from rebuilding or re-uploading the already published `102` AAB.
  - `42f327e Record settings defaults parity audit`
  - Settings/defaults/color/sketch/drone initialization were rechecked against current iOS source. Android matches `AppLanguage`, `KoreaFeaturesEnabled`, notification/hide/keep-awake defaults, default shape blue `#007AFF`, sketch defaults, Sketch Firestore missing `strokeWidth` fallback, drone color suggestion, default drone creation, and legacy `droneId == nil/null` first-drone migration.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*SettingsPreferenceKeysTest" --tests "*SettingsLanguageSelectionTest" --tests "*NotificationPreferenceKeysTest" --tests "*SketchDefaultsTest" --tests "*SketchFirebaseStoreTest" --tests "*DroneNextColorTest" --tests "*DroneDeleteValidationTest" --tests "*ShapeRepositoryTest"`
  - `bd14082 Save release resume snapshot`
  - This Play release handoff was refreshed so the trigger phrase "앱 출시 과정 다시 이어나가자" resumes from Play app-signing SHA registration and Play-installed internal-test verification, not from rebuilding or re-uploading the already published `102` AAB.
  - `eef4db6 Record shape edit default parity`
  - Shape edit defaults/parity were documented after the Android `DefaultShapeEditDateOnlyMode` correction in `1d11ee0`.
  - `1d11ee0 Align shape edit date-only default`
  - Android shape editing now matches the current iOS `DronePassApp` startup registration: absent `isDateOnlyMode` defaults to `true`, so new installs open new-shape editing with day-mode input enabled. This supersedes the earlier `c68c057` false default.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*ShapeEditDefaultsTest" --tests "*ShapeDateFormatsTest" --tests "*MapCameraFocusTest"`
  - `5decf36 Restore drone edit optional field parity`
  - Android drone editing again matches the actual iOS `DroneEditView.saveDrone()` + `DroneModel.update(...)` contract: blank optional fields in edit mode pass through as nil and preserve the previous value; nonblank values preserve the original user-entered text. This supersedes the incorrect clearing change in `d30bd45`.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*DroneListScreenTest" --tests "*DroneNameWidthLimitTest" --tests "*DroneNextColorTest" --tests "*DroneManagementContractTest" --tests "*DroneEditSheetTest" --tests "*DroneDeleteValidationTest" --tests "*DroneSelectionStateTest"`
  - `e2afb38 Align map highlight radius parity`
  - Android map focus highlight now matches iOS `MapViewModel.updateHighlight`: normal map overlays remain circle-only, but selected focus highlights are created for any shape that has a `radius`, regardless of `shapeType`. Map focus filtering also directly pins legacy `droneId == null` shapes to the first active drone.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*ShapeOverlayRenderTest" --tests "*ShapeOverlayColorTest" --tests "*MapCameraFocusTest"`
  - `8d0112d Save Play release resume checkpoint`
  - Play release resume state was refreshed so the trigger phrase "앱 출시 과정 다시 이어나가자" resumes from Play app-signing SHA registration and Play-installed internal-test verification, not from rebuilding or re-uploading the already published `102` AAB.
  - `1beac1b Pin saved list legacy drone filter parity`
  - Saved-list section building now has a regression test for the iOS legacy rule that shapes without `droneId` are treated as belonging to the first active drone.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*SavedListSectionsTest" --tests "*DroneSelectionStateTest" --tests "*SavedShapeListItemTest" --tests "*MapCameraFocusTest"`
  - `f056476 Pin sketch eraser touch parity`
  - Sketch touch decision tests now pin the iOS eraser behavior where `up` and `cancel` end the touch without issuing another delete action.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*SketchTouchDecisionTest" --tests "*SketchDefaultsTest" --tests "*SketchEraserSelectionTest" --tests "*SketchOverlayColorTest" --tests "*SketchRepositoryTest" --tests "*SketchFirebaseStoreTest" --tests "*SketchSmoothingAlgorithmTest" --tests "*SketchPointsCacheTest" --tests "*StringResourceCoverageTest"`
  - `902e1ed Align saved list section sorting with iOS`
  - Android saved-list section sorting now matches iOS: `flightEndDate` sorting is applied globally before splitting into not-started/active/expired sections, while the other sort modes split into sections first and sort inside each section.
  - Targeted tests passed:
    - `:app:testDebugUnitTest --tests "*SavedListSectionsTest" --tests "*SavedShapeListItemTest" --tests "*MapCameraFocusTest" --tests "*MainScreenStartDestinationTest"`
  - `48e75da Save release resume checkpoint`
  - Play release resume state was saved so the trigger phrase "앱 출시 과정 다시 이어나가자" resumes from Play app-signing SHA registration and Play-installed internal-test verification, not from rebuilding or re-uploading the already published `102` AAB.
  - `2cbd5f3 Record latest E2E preflight status`
  - The latest local `:app:verifyCrossPlatformE2ePrerequisites` status was captured. Android-side shared Firebase E2E prerequisites were ready, but no Android test device was attached, so Play-installed real-device verification remains open.
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
  - Superseded by `1d11ee0`: the current iOS app registers `isDateOnlyMode = true` at launch, so Android absent setting now defaults to day mode.
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

At the time of this latest release-handoff save request, `git status --short` was clean and local HEAD was `bbb5f30 Pin saved list map focus delivery` before this documentation edit. This documentation update only saves the resume state. It does not create or upload a newer Play build, and it is unrelated to the already-published Play internal-test release unless a newer AAB is intentionally uploaded later. Re-check with `git status --short` when resuming.

## Paused Code Thread

The user switched from code work to saving this handoff while broader Android/iOS parity work was paused. This is not part of the Play release resume trigger, but it is useful context if the user later says to continue the paused implementation work.

- Completed follow-up since the earlier auth pause: account-switch warning copy/count and auth cancellation behavior now match iOS through `f2dee60` and `a55b59d`.
- Completed follow-up after the Play handoff: weather guide punctuation, drone dropdown empty label, detail copy-toast text size, KP/weather refresh interval contract, saved-list section sorting, sketch eraser touch completion, saved-list legacy drone filtering, map highlight radius parity, drone optional field parity, shape edit date-only default, settings/default initialization parity, main/new-shape/App Info/Patch Notes parity, document screen parity, VWorld layer/detail parity, notification/settings/local notification parity, profile/account deletion parity, Shape Detail screen behavior, drone management behavior, saved-list map-focus behavior, login/auth Apple·Google provider behavior, Shape Edit coordinate-input/radius-row behavior, KP forecast failure-state behavior, Weather forecast no-data placeholder behavior, saved-list header typography, KP/Weather info-guide section-title typography, Shape DateTimeSelection end-date minimum-date/time behavior, Saved List internal focus to map-focus delivery behavior, map location-permission blocking-UI behavior, and settings flight-environment row chevron-token behavior are pinned through the current HEAD containing this file.
- Latest completed implementation thread before this save: settings flight-environment KP/weather entry rows were rechecked against iOS `SettingView`. Android already matched row order, strings, and sheet entry behavior; `SettingsItem` now uses a 12dp caption-sized chevron token to match iOS `.font(.caption)`.
  - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsComponents.kt`
  - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/settings/SettingsScreenContractTest.kt`
- Previous completed implementation thread before the settings chevron save: map location-permission blocking UI was rechecked against iOS `LocationManager`, `NaverMapView`, `MapViewModel`, and `MainView`. Android keeps the system location-permission request and Naver location button but no longer shows a custom map permission rationale dialog or central blocking permission UI when location permission is missing; the map remains visible like iOS. The iOS `CenterOnUserLocation` zoom-16 path remains pinned.
  - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapScreen.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-en/strings.xml`
  - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/map/MapCameraFocusTest.kt`
- Previous completed implementation thread before the map permission save: Saved List internal focus to map-focus delivery was rechecked against iOS `ShapeEditViewModel.saveShape`, `ShapeSelectionCoordinator`, and `SavedTableListView.moveToShapeNotification`. Android already delivered Saved List internal focus through `SavedListScreen -> MainScreen.pendingFocusShapeId -> NavGraph -> MapScreen(focusShapeId)`, so behavior code was unchanged; source-order tests now pin the path.
  - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedListSectionsTest.kt`
  - `app/src/test/java/com/ScienceFiction/DronePassAndroid/ui/navigation/MainScreenStartDestinationTest.kt`
- Previous completed implementation thread before the Saved List focus save: Shape DateTimeSelection end-date minimum-date/time behavior was rechecked against iOS `Shape/DateTimeSelectionView.swift`. Android now passes the start date as the end-date sheet minimum and immediately coerces same-day TimePicker values up to the start time, matching iOS `DatePicker(selection:in: minimumDate...)`; next-day earlier clock times remain allowed.
  - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeEditDefaults.kt`
  - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeEditScreen.kt`
  - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeEditDefaultsTest.kt`
- Previous completed implementation thread before the DateTimeSelection save: KP/Weather info-guide section-title typography was rechecked against iOS `KPInfoView` and `WeatherInfoView`. Android now pins guide section titles to 20sp semibold, matching iOS `.title3` semibold, while preserving the existing header/action slots and guide content.
  - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/settings/InfoGuideSheets.kt`
  - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/settings/InfoGuideSheetsTest.kt`
- Previous completed implementation thread before the info-guide save: Weather forecast no-data placeholder behavior and sunrise/sunset placeholder-card behavior were rechecked against iOS. Android now keeps the forecast body and sunrise/sunset card visible with placeholder data when iOS would show empty fallback content instead of removing the card.
  - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/weather/WeatherForecastScreen.kt`
  - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/weather/SunTimeline.kt`
  - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/weather/WeatherForecastParityTest.kt`
  - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/weather/SunTimelineStateTest.kt`
- Previous completed implementation thread before the Weather save: KP forecast data loading, 48-hour/27-day chart display, refresh behavior, and failure-state behavior were rechecked against iOS and committed in `c3af146`.
- Earlier completed implementation thread before the KP save: Shape Edit coordinate-input and radius-row behavior was rechecked against iOS `ShapeEditView`, `CoordinateView`, and `SearchCoordinateViewModel`. Android now shows the radius row for every shape type like iOS while still preserving non-circle geometry and not requiring radius at save time.
  - Android Shape Edit files to re-open:
    - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeEditScreen.kt`
    - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeEditDefaults.kt`
    - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeEditDefaultsTest.kt`
    - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/shape/ShapeEditContractTest.kt`
    - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/shape/SearchAddressSheetTest.kt`
  - iOS Shape Edit references to re-open:
    - `/Users/david/Development/Swift/myProjects/DronePass/DronePass/Shape/View/ShapeEditView.swift`
    - `/Users/david/Development/Swift/myProjects/DronePass/DronePass/Shape/View/CoordinateView.swift`
    - `/Users/david/Development/Swift/myProjects/DronePass/DronePass/Shape/View/CoordinateViewModel.swift`
  - Recent Android files audited or touched:
    - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/component/MapFloatingButtons.kt`
    - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapScreenLayers.kt`
    - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapViewModel.kt`
    - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/sketch/SketchViewModel.kt`
    - `app/src/main/java/com/ScienceFiction/DronePassAndroid/domain/model/SketchModel.kt`
    - `app/src/main/java/com/ScienceFiction/DronePassAndroid/core/data/repository/SketchRepository.kt`
    - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/sketch/SketchToolbar.kt`
    - `app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapScreen.kt`
    - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/map/SketchTouchDecisionTest.kt`
    - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedListSectionsTest.kt`
    - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/sketch/SketchRepositoryTest.kt`
    - `app/src/test/java/com/ScienceFiction/DronePassAndroid/feature/sketch/SketchDefaultsTest.kt`
    - `app/src/test/java/com/ScienceFiction/DronePassAndroid/core/res/StringResourceCoverageTest.kt`
  - Recent iOS references opened:
    - `/Users/david/Development/Swift/myProjects/DronePass/DronePass/View/MainFloatingButtonView.swift`
    - `/Users/david/Development/Swift/myProjects/DronePass/DronePass/MainView.swift`
    - `/Users/david/Development/Swift/myProjects/DronePass/DronePass/Manager/SketchManager.swift`
    - `/Users/david/Development/Swift/myProjects/DronePass/DronePass/Sketch/SketchModel.swift`
    - `/Users/david/Development/Swift/myProjects/DronePass/DronePass/Sketch/SketchRepository.swift`
    - `/Users/david/Development/Swift/myProjects/DronePass/DronePass/Sketch/SketchFileStore.swift`
    - `/Users/david/Development/Swift/myProjects/DronePass/DronePass/Sketch/View/SketchToolbarView.swift`
    - `/Users/david/Development/Swift/myProjects/DronePass/DronePass/Sketch/View/SketchCanvasView.swift`
  - Audit status already established: default sketch color/stroke/opacity, stroke and opacity clamps, 5m point sampling, finish-only-when-two-points, undo/redo edit-session sync, toolbar ordering/dimensions, pen picker/sliders, delete-all dialog strings, touch input, eraser `up`/`cancel`, saved-list sorting, saved-list focus, saved-list header typography, saved-list internal focus to map-focus delivery, legacy `droneId == nil` filtering, shape edit date-only default, Shape Edit coordinate-input/radius-row behavior, Shape DateTimeSelection end-date minimum-date/time behavior, drone optional edit preservation, map highlight radius behavior, settings/default initialization, main `+` new-shape coordinate fallback, long-press new-shape confirm/failure flow, App Info strings, Patch Notes states, Terms/Privacy document rendering, VWorld layer selector/detail/lifecycle behavior, notification/settings/local notification scheduling, profile/account deletion behavior, Shape Detail behavior, drone management behavior, saved-list map-focus behavior, login/auth Apple/Google provider behavior, KP forecast failure-state behavior, Weather no-data placeholder behavior, and KP/Weather info-guide section-title typography are aligned with iOS or pinned by tests.
  - Next code-audit candidate: continue with another user-visible secondary screen or integration path that has not been recently rechecked, or Play-installed real-device verification when a device is available. If the user says to resume the app release process instead, ignore this code-audit thread and follow the Play release steps above.

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

- `:app:testDebugUnitTest --tests "*SavedListSectionsTest" --tests "*DroneSelectionStateTest" --tests "*SavedShapeListItemTest" --tests "*MapCameraFocusTest"` passed after pinning saved-list legacy drone filtering.
- `:app:testDebugUnitTest --tests "*SketchTouchDecisionTest" --tests "*SketchDefaultsTest" --tests "*SketchEraserSelectionTest" --tests "*SketchOverlayColorTest" --tests "*SketchRepositoryTest" --tests "*SketchFirebaseStoreTest" --tests "*SketchSmoothingAlgorithmTest" --tests "*SketchPointsCacheTest" --tests "*StringResourceCoverageTest"` passed after pinning sketch eraser touch completion.
- `:app:testDebugUnitTest --tests "*SavedListSectionsTest" --tests "*SavedShapeListItemTest" --tests "*MapCameraFocusTest" --tests "*MainScreenStartDestinationTest"` passed after aligning saved-list section sorting with iOS.
- `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*InfoGuideSheetsTest"` passed.
- `:app:testDebugUnitTest --tests "*StringResourceCoverageTest" --tests "*DroneSelectionDropdownTest" --tests "*MapFloatingButtonsTest"` passed.
- `:app:testDebugUnitTest --tests "*ShapeDetailDroneResolutionTest" --tests "*DroneDeleteValidationTest"` passed.
- `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*KpChartsTest"` passed.
- `:app:verifyCrossPlatformE2ePrerequisites` passed; no Android device was attached in `adb devices`, so Play-installed real-device verification still needs a connected device.

2026-07-03 targeted parity verification after the latest handoff update:

- `:app:testDebugUnitTest --tests "*LoginScreenContractTest" --tests "*AuthViewModelForegroundSyncTest" --tests "*AuthRepositoryUserDocumentTest" --tests "*StringResourceCoverageTest" --tests "*SettingsScreenContractTest"` passed after rechecking login/auth Apple and Google provider behavior against the iOS source.

2026-07-04 targeted parity verification after the latest handoff update:

- `:app:testDebugUnitTest --tests "*ShapeEditDefaultsTest" --tests "*ShapeEditContractTest" --tests "*SearchAddressSheetTest"` passed after rechecking Shape Edit coordinate-input and radius-row behavior against the iOS source.
- `:app:testDebugUnitTest --tests "*KpChartsTest" --tests "*WeatherForecastParityTest"` passed after rechecking KP forecast failure-state behavior against the iOS source.
- `:app:testDebugUnitTest --tests "*WeatherForecastParityTest" --tests "*SunTimelineStateTest" --tests "*SunEventTickerTest" --tests "*WeatherOverlayCardTest"` passed after rechecking Weather forecast no-data placeholder behavior against the iOS source.
- `:app:verifyCrossPlatformE2ePrerequisites` passed on the latest HEAD. Android-side shared Firebase E2E prerequisites are configured, but no Android device was attached in `adb devices`, so the real Play-installed iOS -> Android receipt check remains open.
- `:app:testDebugUnitTest --tests "*MainScreenStartDestinationTest" --tests "*SavedListSectionsTest" --tests "*StringResourceCoverageTest"` passed after rechecking the Saved List overlay search/sort/header contract against the iOS source.
- `:app:testDebugUnitTest --tests "*InfoGuideSheetsTest" --tests "*StringResourceCoverageTest"` passed after rechecking KP/Weather info-guide section-title typography against the iOS source.
- `:app:testDebugUnitTest --tests "*ShapeEditDefaultsTest" --tests "*ShapeEditContractTest" --tests "*ShapeDateFormatsTest"` passed after rechecking Shape DateTimeSelection end-date minimum-date/time behavior against the iOS source.
- `:app:testDebugUnitTest --tests "*SavedListSectionsTest" --tests "*MainScreenStartDestinationTest"` passed after rechecking Saved List internal focus to map-focus delivery against the iOS source.

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

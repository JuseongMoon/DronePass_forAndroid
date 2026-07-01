# DronePass Android Play Release Handoff

> Last updated: 2026-07-01  
> Resume trigger: "앱 출시 과정 다시 이어나가자"  
> Branch at handoff: `fix/critical-pri0-fixes`  
> HEAD at handoff: `effbf74`  
> Package name: `com.ScienceFiction.DronePassAndroid`

This file captures the Google Play internal testing/release state so a later session can continue from this repository without re-discovering the setup. Do not paste secrets, keystore passwords, API secrets, or full OAuth client IDs into this file.

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

## Local Android State

- Version alignment was committed in `effbf74 Align Android version with iOS`.
  - `versionCode = 102`
  - `versionName = "3.5.5"`
  - This was matched to the iOS build number/marketing version that were available at the time.
- Latest local release bundle path:
  - `app/build/outputs/bundle/release/app-release.aab`
  - Local bundle size: about `42 MB`
  - Timestamp observed: 2026-07-01 14:19 KST
- A local release upload keystore exists but is intentionally ignored by git:
  - `release.jks`
  - `keystore.properties`
- Local API/sign-in values are intentionally ignored by git:
  - `local.properties`
- `app/google-services.json` is tracked and currently modified with the Android Firebase app/OAuth configuration downloaded during Play setup.
- `app/build.gradle.kts` is currently modified after the published release to add:

```kotlin
ndk {
    debugSymbolLevel = "SYMBOL_TABLE"
}
```

This setting was added while investigating the Play native-symbol warning. Rebuilding still did not produce a separate `native-debug-symbols.zip`, because the native `.so` libraries appear to come from third-party dependencies such as Naver Maps/AndroidX/DataStore rather than app-owned NDK code. The warning can be ignored for the current internal test.

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

## Upload Key Fingerprints

These are the local upload-key fingerprints generated for the ignored `release.jks`. They are useful for Firebase/NCP console setup, but the Play-distributed app may use a different Google Play app signing certificate.

- Upload key SHA-1: `26:33:B0:CB:F5:98:F2:FF:77:FC:3A:70:F1:15:AB:93:0A:EB:64:6D`
- Upload key SHA-256: `5D:DB:E4:C6:66:FA:A5:86:DD:AF:91:12:E8:97:9F:E5:37:DA:99:EF:3F:54:79:5F:9A:70:26:5B:E1:EF:27:E8`

Keep the upload key and `keystore.properties` backed up securely. Do not commit them.

## Next Steps

1. In Play Console, go to `테스트 및 출시 > 테스트 > 내부 테스트 > 테스터`.
2. Add tester Google account emails.
3. Copy the internal test opt-in link and open it on the Android test device.
4. Install from Google Play, not from `adb`, for the release-path verification.
5. In Play Console, go to `Google Play로 보호됨 > 앱 무결성`.
6. Copy the `앱 서명 키 인증서` SHA-1 and SHA-256.
7. Add the Play app-signing SHA-1/SHA-256 to Firebase Console for the Android app.
8. Add the Play app-signing certificate fingerprint to NCP Maps Console if the console requires certificate restrictions for Android SDK auth.
9. Re-download `google-services.json` after Firebase SHA changes and replace `app/google-services.json`.
10. If a new AAB upload is needed after changing Firebase config, bump `versionCode` to `103` and use release name `3.5.5 (103) internal-2`.

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

## Git/Commit Notes

Current working tree had these changed files when this handoff was written/updated:

- `PLAY_RELEASE_HANDOFF.md`
- `NEXT_STEPS.md`
- `app/google-services.json`
- `app/build.gradle.kts`

Before committing, review whether the Firebase config update should be committed and whether the `debugSymbolLevel` setting should stay. Do not commit:

- `release.jks`
- `keystore.properties`
- `local.properties`

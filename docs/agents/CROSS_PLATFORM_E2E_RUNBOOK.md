# DronePass Cross-Platform E2E Runbook

This runbook closes the real-device shared Firestore gate:

- `docs/agents/MIGRATION_PLAN.md` quality gate: `iOS 생성 -> Android 수신`
- `docs/agents/MIGRATION_PLAN.md` TC-CROSS scenarios

Unit tests such as `CrossPlatformFirestoreContractTest` protect the wire format. This runbook is for the final real account check that proves both apps are connected to the same Firebase project and can sync live user data.

## Prerequisites

- iOS and Android builds point at the same Firebase project.
- A disposable real test account is available on both platforms.
- Android Google sign-in is operational:
  - Android `applicationId`, NCP Maps Console Android package, and Firebase Android app package are all `com.ScienceFiction.DronePassAndroid`.
  - `local.properties` has a real `WEB_CLIENT_ID`.
  - Firebase Console has the Android debug/release SHA-1 and SHA-256 fingerprints.
  - `app/google-services.json` has a `client_type=1` Android `oauth_client` for `com.ScienceFiction.DronePassAndroid`.
- Cloud backup/sync is enabled for the test account in both apps.
- Both devices can resolve and reach `firestore.googleapis.com`; an offline write is valid, but the receipt timer starts only after the pending write reaches Firestore.
- Any existing test data with the chosen prefix is safe to delete.

Before starting the manual device steps, run the Android-side preflight:

```bash
./gradlew :app:verifyCrossPlatformE2ePrerequisites
```

This task must pass before the real iOS -> Android receipt test can close the migration gate. It only checks Android-local configuration; it does not replace the shared Firebase account/device verification below.

Use a unique title prefix for all created data:

```text
DP_CROSS_YYYYMMDD_HHMM
```

## Required iOS -> Android Shape Check

1. On iOS, sign in with the shared test account.
2. Create a circle shape with:
   - title: `DP_CROSS_YYYYMMDD_HHMM_iOS_circle`
   - radius: a visible non-default value such as `120 m`
   - memo or address text that can be recognized on Android
3. Wait until iOS finishes its cloud sync.
4. In Firebase Console, inspect `users/{uid}/shapes/{shapeId}` and confirm:
   - `id` equals the document id and is a UUID.
   - `shapeType` is lowercase `circle`.
   - `baseCoordinate` is a map with numeric `latitude` and `longitude`.
   - `radius` is numeric.
   - `flightStartDate`, `createdAt`, and `updatedAt` are Firestore `Timestamp` values.
5. On Android, sign in with the same account.
6. Open the saved list and confirm the iOS shape appears within 2 seconds after sync starts.
7. Tap the saved shape and confirm:
   - the map camera moves to the shape,
   - the shape is highlighted,
   - the detail sheet shows the same title, radius, date, drone, memo/address data that iOS wrote.
8. Check Android logcat for the app process and confirm there is no `AndroidRuntime`, `FATAL EXCEPTION`, or Firestore parsing failure around the sync.

The gate passes only when the Android saved list, map overlay, and detail sheet all show the iOS-created shape without a crash or silent skip.

## Recommended Extra Shape Coverage

Repeat the same iOS -> Android check for one non-circle shape before release:

- rectangle: verify `secondCoordinate`.
- polygon: verify `polygonCoordinates`.
- polyline: verify `polylineCoordinates`.

If time is limited, prioritize polygon because it covers array coordinate parsing.

## Android -> iOS Regression Check

1. On Android, create a shape titled `DP_CROSS_YYYYMMDD_HHMM_Android_circle`.
2. Confirm in Firebase Console that Android wrote canonical values:
   - `shapeType` is lowercase, never `CIRCLE`.
   - dates are Firestore `Timestamp`, never epoch numbers or strings.
   - coordinates are `{latitude, longitude}` maps.
3. Open iOS with the same account and confirm the Android shape appears in the saved list and map.

## Delete Sync Check

1. Delete one test shape from Android.
2. Confirm iOS removes it from active lists.
3. If inspecting Firestore, `deletedAt` must be a Firestore `Timestamp`.
4. Delete another test shape from iOS and confirm Android removes it from active lists.
5. For a debug Android build, inspect Room and confirm the received row retains a non-null `deletedAt` tombstone.
6. Delete or clean up all remaining `DP_CROSS_...` test data through the app UI so both platforms observe the same tombstone behavior.

Active Firestore documents from Android or older iOS versions may omit `deletedAt`. Readers must treat a missing field as active; do not use a Firestore `deletedAt == null` query as the only active-document load path because it excludes documents where the field is absent.

## 2026-07-10 Real-Device Result

- Devices: iPhone 12 and Android 15 SM-A346N (실기기).
- Shared auth: the same Google account on both devices.
- iOS -> Android: `DP_CROSS_20260710_1549_iOS_fixed`, radius `120 m`, map highlight and detail values matched.
- Android -> iOS: `DP_CROSS_20260710_1602_Android_offline`, active after 10 seconds and detail values matched.
- Linked drone resolution: after changing Android `droneById` to an eagerly collected StateFlow, iOS-created `DP_CROSS_20260710_2232_iOS_drone` displayed the active linked drone name `내 드론` instead of `삭제된 드론` in Android detail.
- Delete sync: Android -> iOS and iOS -> Android both passed; the Android Room row for the iOS-created shape retained `deletedAt=2026-07-10 16:50:01` local time.
- One Android delete was queued during a DNS outage and arrived after connectivity recovered. A second online run passed the live iOS removal assertion.
- Cleanup: Android showed an empty saved list, and the iPhone real-device XCUITest emitted `DP_CONFIRMED_ANDROID_CLEANUP_ON_IOS` for the final test shape.

## Evidence To Record

Add the final evidence to `docs/agents/NEXT_STEPS.md` when the test passes:

- Firebase project id and app build versions, without secrets.
- Test account identifier in redacted form.
- Shape ids and titles used.
- iOS write time and Android receive time.
- Android device model/API level.
- Logcat result confirming no fatal app errors.
- Screenshots or UIAutomator bounds for saved list, map highlight, and detail sheet if available.

Do not mark the `docs/agents/MIGRATION_PLAN.md` cross-platform checkbox complete until the real shared Firebase iOS -> Android receipt has passed.

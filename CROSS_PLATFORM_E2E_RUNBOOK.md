# DronePass Cross-Platform E2E Runbook

This runbook closes the real-device shared Firestore gate:

- `MIGRATION_PLAN.md` quality gate: `iOS 생성 -> Android 수신`
- `MIGRATION_PLAN.md` TC-CROSS scenarios

Unit tests such as `CrossPlatformFirestoreContractTest` protect the wire format. This runbook is for the final real account check that proves both apps are connected to the same Firebase project and can sync live user data.

## Prerequisites

- iOS and Android builds point at the same Firebase project.
- A disposable real test account is available on both platforms.
- Android Google sign-in is operational:
  - `local.properties` has a real `WEB_CLIENT_ID`.
  - Firebase Console has the Android debug/release SHA-1 and SHA-256 fingerprints.
  - `app/google-services.json` has a `client_type=1` Android `oauth_client` for `com.ScienceFiction.DronePassAndroid`.
- Cloud backup/sync is enabled for the test account in both apps.
- Any existing test data with the chosen prefix is safe to delete.

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
4. Delete or clean up all remaining `DP_CROSS_...` test data through the app UI so both platforms observe the same tombstone behavior.

## Evidence To Record

Add the final evidence to `NEXT_STEPS.md` when the test passes:

- Firebase project id and app build versions, without secrets.
- Test account identifier in redacted form.
- Shape ids and titles used.
- iOS write time and Android receive time.
- Android device model/API level.
- Logcat result confirming no fatal app errors.
- Screenshots or UIAutomator bounds for saved list, map highlight, and detail sheet if available.

Do not mark the `MIGRATION_PLAN.md` cross-platform checkbox complete until the real shared Firebase iOS -> Android receipt has passed.

# DronePass Firestore Cross-Platform Contract

iOS and Android share the same Firebase Firestore collections:

- `users/{uid}/shapes`
- `users/{uid}/sketches`
- `users/{uid}/drones`

The rule is strict writes and tolerant reads. New writes must use the canonical iOS wire format. Reads must accept known legacy values where doing so prevents silent data loss.

## Shape `shapeType`

Canonical write values are lowercase strings:

- `circle`
- `rectangle`
- `polygon`
- `polyline`

Never write Kotlin enum names such as `CIRCLE`, `RECTANGLE`, `POLYGON`, or `POLYLINE`.

Android legacy builds wrote enum names to Firestore. Android must continue reading `shapeType` case-insensitively so documents written as `CIRCLE` remain visible on both platforms after the next save normalizes them back to lowercase.

Incident note, 2026-07-06: legacy Android documents with uppercase `shapeType`
values such as `CIRCLE` caused those otherwise-valid shapes to be silently skipped
by the iOS reader before the iOS tolerant-read fix. Keep this contract as:
write canonical lowercase raw values, read known values case-insensitively, and skip
unknown future values instead of coercing them to `circle`.

Regression coverage:

- `CrossPlatformFirestoreContractTest`
- `ShapeTypeTest`
- `ShapeFirebaseStoreTest`
- `ShapeFirestoreParsingTest`

## Shape Fields

Required shape fields:

- `id`: UUID string, equal to the Firestore document id.
- `title`: string. Empty string is allowed, `null` is not.
- `shapeType`: lowercase canonical string.
- `baseCoordinate`: map with `latitude: Double` and `longitude: Double`.
- `color`: `#RRGGBB`.
- `flightStartDate`: Firestore `Timestamp`.

Optional shape fields:

- `flightEndDate`, `createdAt`, `updatedAt`, `deletedAt`: Firestore `Timestamp`.
- `radius`: `Double`, circle only.
- `secondCoordinate`: coordinate map, rectangle only.
- `polygonCoordinates`: array of coordinate maps, polygon only.
- `polylineCoordinates`: array of coordinate maps, polyline only.
- `droneId`, `memo`, `address`: strings.
- `height`: `Double`.

Numeric fields must be stored with the Firestore double wire type. Do not write integer
numbers for `radius`, `height`, coordinate latitude/longitude, sketch `strokeWidth`, or
sketch `opacity`. Android reads optional numeric fields with the same effective rule as
iOS `as? Double`: an integer number is treated as missing/default, not widened to a
Double.

For each `shapeType`, its matching geometry field is required for that type to render and sync correctly:

- `circle`: `radius`
- `rectangle`: `secondCoordinate`
- `polygon`: `polygonCoordinates`
- `polyline`: `polylineCoordinates`

Writers must only write geometry fields that match the current `shapeType`; stale geometry for other types must be omitted or deleted on merge.

Do not write dates as epoch numbers, ISO strings, or Unix seconds. Do not write coordinates as `GeoPoint`, arrays, integers, or strings.

If `deletedAt` is present, readers must only accept it as a Firestore `Timestamp`. A malformed tombstone must be skipped instead of treated as an active item, otherwise a deleted shape, sketch, or drone can be resurrected during Android's full-collection sync path.

Legacy read fallbacks:

- `startedAt` may be used when `flightStartDate` is missing.
- `expireDate` may be used when `flightEndDate` is missing.
- `shapeType` is read case-insensitively for Android legacy documents.

## Sketch And Drone Fields

Sketch and drone documents follow the same cross-platform principles:

- Dates are Firestore `Timestamp`.
- Coordinates are `{latitude: Double, longitude: Double}` maps.
- IDs are UUID strings.
- Colors are `#RRGGBB`.
- Enum-like wire values must write lowercase raw values and read case-insensitively when legacy data exists.
- Sketch `strokeWidth` and `opacity` are Firestore doubles. Integer numbers are not
  canonical and read back as iOS-compatible default values.

## Schema Evolution

Additive changes must be optional first. Do not rename or change the type of an existing key. When a field needs a new representation, add a new key and make both platforms read both keys during migration.

When adding a new shape type, add it to both iOS and Android enum definitions before writing it from either platform. A platform that does not know a shape type will skip that document.

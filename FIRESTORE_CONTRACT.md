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

Regression coverage:

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

Do not write dates as epoch numbers, ISO strings, or Unix seconds. Do not write coordinates as `GeoPoint`, arrays, integers, or strings.

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

## Schema Evolution

Additive changes must be optional first. Do not rename or change the type of an existing key. When a field needs a new representation, add a new key and make both platforms read both keys during migration.

When adding a new shape type, add it to both iOS and Android enum definitions before writing it from either platform. A platform that does not know a shape type will skip that document.

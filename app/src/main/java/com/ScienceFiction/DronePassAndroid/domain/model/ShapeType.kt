package com.ScienceFiction.DronePassAndroid.domain.model

/**
 * 도형 종류.
 *
 * iOS `ShapeType.rawValue` 는 소문자(`circle`, `rectangle`, ...)로 저장된다.
 * Android 초기 버전은 Kotlin enum [name](`CIRCLE`)을 저장했으므로 읽기 경로는 둘 다
 * 허용하고, 쓰기 경로는 iOS와 같은 [rawValue]를 사용한다.
 *
 * 현재 Android 생성 UI는 iOS의 실제 생성 흐름과 같은 [CIRCLE] 중심이다.
 * 읽기/저장/지도 렌더링 경로는 기존/향후 iOS 데이터가 들어와도 계약 타입을
 * 유실하지 않도록 [RECTANGLE], [POLYGON], [POLYLINE]도 보존한다.
 */
enum class ShapeType(val rawValue: String, val koreanName: String) {
    CIRCLE("circle", "원"),
    RECTANGLE("rectangle", "사각형"),
    POLYGON("polygon", "다각형"),
    POLYLINE("polyline", "선");

    companion object {
        fun parseWireValue(value: String?): ShapeType? {
            if (value.isNullOrBlank()) return null

            val normalized = value.trim()
            return entries.firstOrNull { type ->
                type.rawValue.equals(normalized, ignoreCase = true) ||
                    type.name.equals(normalized, ignoreCase = true)
            }
        }

        /**
         * Room 에 저장된 로컬 값 복구 전용.
         *
         * Firestore 읽기 경로는 unknown shapeType 을 원형으로 오인하면 안 되므로
         * 반드시 [parseWireValue]를 직접 사용하고 null 일 때 문서를 스킵한다.
         */
        fun fromLocalStorageValue(value: String?): ShapeType {
            return parseWireValue(value) ?: CIRCLE
        }
    }
}

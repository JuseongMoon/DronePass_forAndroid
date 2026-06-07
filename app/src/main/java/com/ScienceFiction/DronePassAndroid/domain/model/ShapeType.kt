package com.ScienceFiction.DronePassAndroid.domain.model

/**
 * 도형 종류.
 *
 * iOS `ShapeType.rawValue` 는 소문자(`circle`, `rectangle`, ...)로 저장된다.
 * Android 초기 버전은 Kotlin enum [name](`CIRCLE`)을 저장했으므로 읽기 경로는 둘 다
 * 허용하고, 쓰기 경로는 iOS와 같은 [rawValue]를 사용한다.
 *
 * 현재 Android UI/지도 렌더링은 iOS의 실제 생성 흐름과 같은 [CIRCLE]만 노출한다.
 * 나머지 타입은 기존/향후 iOS 데이터가 들어와도 enum 파싱 단계에서 유실되지 않도록
 * 모델에만 보존한다.
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

        fun fromWireValue(value: String?): ShapeType {
            return parseWireValue(value) ?: CIRCLE
        }
    }
}

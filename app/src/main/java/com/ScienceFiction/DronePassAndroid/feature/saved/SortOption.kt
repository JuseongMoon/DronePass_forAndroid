package com.ScienceFiction.DronePassAndroid.feature.saved

enum class SortOption(val label: String) {
    TITLE("제목순"),
    DATE_CREATED("생성일순"),
    FLIGHT_START("시작일순"),
    FLIGHT_END("종료일순")
}

enum class SortDirection(val label: String) {
    ASCENDING("오름차순"),
    DESCENDING("내림차순")
}

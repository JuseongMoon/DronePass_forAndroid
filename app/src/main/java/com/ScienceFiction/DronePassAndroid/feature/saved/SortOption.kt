package com.ScienceFiction.DronePassAndroid.feature.saved

import androidx.annotation.StringRes
import com.ScienceFiction.DronePassAndroid.R

/**
 * 저장 목록 정렬 기준.
 *
 * [labelRes] 는 strings.xml 에 정의된 다국어 문자열 리소스 ID 이다.
 * 화면에서는 `stringResource(option.labelRes)` 로 사용한다.
 */
enum class SortOption(@StringRes val labelRes: Int) {
    TITLE(R.string.saved_sort_title),
    DATE_CREATED(R.string.saved_sort_date_created),
    FLIGHT_START(R.string.saved_sort_flight_start),
    FLIGHT_END(R.string.saved_sort_flight_end),
}

enum class SortDirection(@StringRes val labelRes: Int) {
    ASCENDING(R.string.saved_sort_ascending),
    DESCENDING(R.string.saved_sort_descending),
}

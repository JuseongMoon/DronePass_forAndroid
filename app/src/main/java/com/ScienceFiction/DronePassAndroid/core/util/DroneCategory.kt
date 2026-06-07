package com.ScienceFiction.DronePassAndroid.core.util

import androidx.annotation.StringRes
import com.ScienceFiction.DronePassAndroid.R

enum class DroneCategory(
    val label: String,
    val minWeight: String,
    val maxWeight: String,
    val iosRawValue: String,
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val examplesRes: Int,
) {
    TOY(
        label = "250g 이하",
        minWeight = "0g",
        maxWeight = "250g",
        iosRawValue = "≤250g",
        labelRes = R.string.weather_drone_category_toy,
        descriptionRes = R.string.weather_drone_category_toy_description,
        examplesRes = R.string.weather_drone_category_toy_examples,
    ),
    CLASS4(
        label = "250g~2kg",
        minWeight = "250g",
        maxWeight = "2kg",
        iosRawValue = "250g~2kg",
        labelRes = R.string.weather_drone_category_class4,
        descriptionRes = R.string.weather_drone_category_class4_description,
        examplesRes = R.string.weather_drone_category_class4_examples,
    ),
    CLASS3(
        label = "2kg~7kg",
        minWeight = "2kg",
        maxWeight = "7kg",
        iosRawValue = "2kg~7kg",
        labelRes = R.string.weather_drone_category_class3,
        descriptionRes = R.string.weather_drone_category_class3_description,
        examplesRes = R.string.weather_drone_category_class3_examples,
    ),
    CLASS2(
        label = "7kg~25kg",
        minWeight = "7kg",
        maxWeight = "25kg",
        iosRawValue = "7kg~25kg",
        labelRes = R.string.weather_drone_category_class2,
        descriptionRes = R.string.weather_drone_category_class2_description,
        examplesRes = R.string.weather_drone_category_class2_examples,
    );

    companion object {
        /**
         * iOS SettingManager.selectedDroneCategory 기본값은 class3.
         * 첫 실행 경고 임계값이 iOS WeatherManager 와 같아야 한다.
         */
        val IosDefault: DroneCategory = CLASS3

        fun fromStoredValue(value: String?): DroneCategory? {
            val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return entries.firstOrNull { category ->
                category.name.equals(normalized, ignoreCase = true) ||
                    category.iosRawValue.equals(normalized, ignoreCase = true) ||
                    category.label.equals(normalized, ignoreCase = true)
            }
        }
    }
}

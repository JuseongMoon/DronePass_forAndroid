package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.core.ui.IosWeatherSymbols
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.ui.graphics.vector.ImageVector
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.WeatherCondition

/**
 * WeatherKit 날씨 상태 → 설명/아이콘 매핑.
 *
 * 아이콘 규칙은 iOS `WeatherManager.precipitationIconName` 을 따른다. SF Symbol 을
 * 가장 가까운 Material 아이콘으로 옮겼다.
 */
object WeatherCodeMapper {

    @StringRes
    fun conditionDescriptionRes(condition: WeatherCondition?): Int = when (condition) {
        WeatherCondition.CLEAR -> R.string.weather_condition_clear
        WeatherCondition.MOSTLY_CLEAR -> R.string.weather_condition_mostly_clear
        WeatherCondition.PARTLY_CLOUDY -> R.string.weather_condition_partly_cloudy
        WeatherCondition.MOSTLY_CLOUDY -> R.string.weather_condition_mostly_cloudy
        WeatherCondition.CLOUDY -> R.string.weather_condition_cloudy
        WeatherCondition.FOGGY -> R.string.weather_condition_foggy
        WeatherCondition.HAZE -> R.string.weather_condition_haze
        WeatherCondition.SMOKY -> R.string.weather_condition_smoky
        WeatherCondition.BREEZY -> R.string.weather_condition_breezy
        WeatherCondition.WINDY -> R.string.weather_condition_windy
        WeatherCondition.DRIZZLE -> R.string.weather_condition_drizzle
        WeatherCondition.RAIN -> R.string.weather_condition_rain
        WeatherCondition.HEAVY_RAIN -> R.string.weather_condition_heavy_rain
        WeatherCondition.ISOLATED_THUNDERSTORMS -> R.string.weather_condition_isolated_thunderstorms
        WeatherCondition.SCATTERED_THUNDERSTORMS -> R.string.weather_condition_scattered_thunderstorms
        WeatherCondition.STRONG_STORMS -> R.string.weather_condition_strong_storms
        WeatherCondition.THUNDERSTORMS -> R.string.weather_condition_thunderstorms
        WeatherCondition.FRIGID -> R.string.weather_condition_frigid
        WeatherCondition.HAIL -> R.string.weather_condition_hail
        WeatherCondition.HOT -> R.string.weather_condition_hot
        WeatherCondition.FLURRIES -> R.string.weather_condition_flurries
        WeatherCondition.SLEET -> R.string.weather_condition_sleet
        WeatherCondition.SNOW -> R.string.weather_condition_snow
        WeatherCondition.SUN_FLURRIES -> R.string.weather_condition_sun_flurries
        WeatherCondition.SUN_SHOWERS -> R.string.weather_condition_sun_showers
        WeatherCondition.WINTRY_MIX -> R.string.weather_condition_wintry_mix
        WeatherCondition.BLIZZARD -> R.string.weather_condition_blizzard
        WeatherCondition.BLOWING_DUST -> R.string.weather_condition_blowing_dust
        WeatherCondition.BLOWING_SNOW -> R.string.weather_condition_blowing_snow
        WeatherCondition.FREEZING_DRIZZLE -> R.string.weather_condition_freezing_drizzle
        WeatherCondition.FREEZING_RAIN -> R.string.weather_condition_freezing_rain
        WeatherCondition.HEAVY_SNOW -> R.string.weather_condition_heavy_snow
        WeatherCondition.HURRICANE -> R.string.weather_condition_hurricane
        WeatherCondition.TROPICAL_STORM -> R.string.weather_condition_tropical_storm
        null -> R.string.weather_unknown
    }

    /**
     * iOS `WeatherManager.isSnowing`: 강수 표시를 강설(cm/h)로 바꾸는 상태.
     */
    fun isSnowing(condition: WeatherCondition?): Boolean = when (condition) {
        WeatherCondition.SNOW,
        WeatherCondition.BLOWING_SNOW,
        WeatherCondition.HEAVY_SNOW,
        WeatherCondition.FLURRIES -> true
        else -> false
    }

    /**
     * iOS `WeatherManager.precipitationIconName` 대응.
     *
     * 상태가 비/눈이어도 실제 강수 강도가 0 이면 강수 아이콘 대신 일반 상태 아이콘을 쓴다.
     */
    fun conditionIcon(condition: WeatherCondition?, precipitationIntensity: Double?): ImageVector {
        if (condition == null) return IosWeatherSymbols.Cloud

        val hasPrecipitation = (precipitationIntensity ?: 0.0) > 0.0
        if (hasPrecipitation) {
            return when (condition) {
                WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> IosWeatherSymbols.CloudRain
                WeatherCondition.DRIZZLE -> IosWeatherSymbols.CloudDrizzle
                WeatherCondition.SNOW,
                WeatherCondition.BLOWING_SNOW,
                WeatherCondition.HEAVY_SNOW,
                WeatherCondition.FLURRIES -> IosWeatherSymbols.CloudSnow
                WeatherCondition.SLEET,
                WeatherCondition.FREEZING_RAIN,
                WeatherCondition.FREEZING_DRIZZLE,
                WeatherCondition.WINTRY_MIX -> IosWeatherSymbols.CloudSleet
                WeatherCondition.HAIL -> IosWeatherSymbols.CloudHail
                WeatherCondition.ISOLATED_THUNDERSTORMS,
                WeatherCondition.STRONG_STORMS,
                WeatherCondition.THUNDERSTORMS,
                WeatherCondition.SCATTERED_THUNDERSTORMS -> IosWeatherSymbols.CloudBoltRain
                else -> IosWeatherSymbols.CloudRain
            }
        }

        return when (condition) {
            WeatherCondition.CLEAR, WeatherCondition.MOSTLY_CLEAR -> IosWeatherSymbols.SunMax
            WeatherCondition.PARTLY_CLOUDY -> IosWeatherSymbols.CloudSun
            WeatherCondition.MOSTLY_CLOUDY, WeatherCondition.CLOUDY -> IosWeatherSymbols.Cloud
            WeatherCondition.FOGGY, WeatherCondition.HAZE, WeatherCondition.SMOKY -> IosWeatherSymbols.CloudFog
            WeatherCondition.BREEZY, WeatherCondition.WINDY -> Icons.Default.Air // iOS wind
            WeatherCondition.BLIZZARD,
            WeatherCondition.BLOWING_DUST,
            WeatherCondition.BLOWING_SNOW -> IosWeatherSymbols.WindSnow
            WeatherCondition.ISOLATED_THUNDERSTORMS,
            WeatherCondition.STRONG_STORMS,
            WeatherCondition.THUNDERSTORMS,
            WeatherCondition.SCATTERED_THUNDERSTORMS -> IosWeatherSymbols.CloudBolt
            else -> IosWeatherSymbols.Cloud
        }
    }
}

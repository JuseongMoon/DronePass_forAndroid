package com.ScienceFiction.DronePassAndroid.domain.model

/**
 * Apple WeatherKit `conditionCode`.
 *
 * iOS 는 WeatherKit 의 `WeatherCondition` 을 그대로 쓰고, Android 는 중계 서버가 넘겨준
 * REST `conditionCode` 문자열을 이 값으로 바꾼다. [appleCode] 는 REST 원본 표기다.
 */
enum class WeatherCondition(val appleCode: String) {
    CLEAR("Clear"),
    MOSTLY_CLEAR("MostlyClear"),
    PARTLY_CLOUDY("PartlyCloudy"),
    MOSTLY_CLOUDY("MostlyCloudy"),
    CLOUDY("Cloudy"),
    FOGGY("Foggy"),
    HAZE("Haze"),
    SMOKY("Smoky"),
    BREEZY("Breezy"),
    WINDY("Windy"),
    DRIZZLE("Drizzle"),
    RAIN("Rain"),
    HEAVY_RAIN("HeavyRain"),
    ISOLATED_THUNDERSTORMS("IsolatedThunderstorms"),
    SCATTERED_THUNDERSTORMS("ScatteredThunderstorms"),
    STRONG_STORMS("StrongStorms"),
    THUNDERSTORMS("Thunderstorms"),
    FRIGID("Frigid"),
    HAIL("Hail"),
    HOT("Hot"),
    FLURRIES("Flurries"),
    SLEET("Sleet"),
    SNOW("Snow"),
    SUN_FLURRIES("SunFlurries"),
    SUN_SHOWERS("SunShowers"),
    WINTRY_MIX("WintryMix"),
    BLIZZARD("Blizzard"),
    BLOWING_DUST("BlowingDust"),
    BLOWING_SNOW("BlowingSnow"),
    FREEZING_DRIZZLE("FreezingDrizzle"),
    FREEZING_RAIN("FreezingRain"),
    HEAVY_SNOW("HeavySnow"),
    HURRICANE("Hurricane"),
    TROPICAL_STORM("TropicalStorm");

    companion object {
        private val byCode = entries.associateBy { it.appleCode.lowercase() }

        /** 모르는 코드(Apple 이 새 값을 추가한 경우 등)는 null. 화면은 "알 수 없음"으로 표시한다. */
        fun fromAppleCode(code: String?): WeatherCondition? = code?.let { byCode[it.lowercase()] }
    }
}

package com.ScienceFiction.DronePassAndroid.feature.weather

/**
 * iOS WeatherForecastView.WeatherElementType 정합.
 *
 * 현재 날씨 카드에서 특정 요소를 탭하면 WeatherInfoView 가 해당 섹션으로 스크롤한다.
 */
enum class WeatherInfoTopic {
    Temperature,
    WindSpeed,
    GustDifference,
    Precipitation,
    Visibility,
    Cri,
}

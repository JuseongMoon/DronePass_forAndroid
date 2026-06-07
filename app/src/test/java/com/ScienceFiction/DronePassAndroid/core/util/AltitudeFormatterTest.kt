package com.ScienceFiction.DronePassAndroid.core.util

import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.DroneZoneFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.formattedLowerAltitude
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.formattedUpperAltitude
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AltitudeFormatterTest {

    @Test
    fun `고도 포맷은 iOS처럼 원본 표기를 보존하고 미터와 설명만 덧붙인다`() {
        assertEquals(
            "3 000 AGL (914.4m, 지상 기준)",
            AltitudeFormatter.format("3 000 AGL"),
        )
        assertEquals(
            "1500FT AMSL (457.2m, 평균 해수면)",
            AltitudeFormatter.format("1500FT AMSL"),
        )
        assertEquals(
            "300 FT HEI (91.4m, 높이)",
            AltitudeFormatter.format("300 FT HEI"),
        )
        assertEquals(
            "250FTALT (76.2m, 고도)",
            AltitudeFormatter.format("250FTALT"),
        )
    }

    @Test
    fun `비행고도층과 특수 고도 값은 iOS 포맷을 따른다`() {
        assertEquals("FL150 (4572m, 비행고도층)", AltitudeFormatter.format("FL150"))
        assertEquals("UNL (제한없음)", AltitudeFormatter.format("UNL"))
        assertEquals("GND (지상)", AltitudeFormatter.format("GND"))
        assertEquals("SFC (표면)", AltitudeFormatter.format("SFC"))
    }

    @Test
    fun `파싱할 수 없는 고도 값은 iOS처럼 원본을 그대로 반환한다`() {
        assertEquals("UNKNOWN VALUE", AltitudeFormatter.format("UNKNOWN VALUE"))
        assertNull(AltitudeFormatter.format(null))
    }

    @Test
    fun `VWorld 고도 확장은 iOS AltitudeFormatter 결과를 그대로 사용한다`() {
        val zone = DroneZoneFeature(
            id = "zone.1",
            layer = FlightZoneLayer.PROHIBITED,
            polygons = emptyList(),
            zoneCode = "RK P73A",
            upperAltitude = null,
            lowerAltitude = null,
            zoneName = null,
            properties = mapOf(
                "prh_lbl_2" to "UNL",
                "prh_lbl_3" to "GND",
            ),
        )

        assertEquals("UNL (제한없음)", zone.formattedUpperAltitude)
        assertEquals("GND (지상)", zone.formattedLowerAltitude)
    }
}

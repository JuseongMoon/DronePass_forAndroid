package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class ShapeFirebaseStoreTest {

    @Test
    fun `좌표 컴포넌트는 iOS ShapeFirebaseStore 와 동일하게 소수 6자리로 반올림한다`() {
        assertEquals(37.123457, roundCoordinateComponent(37.1234567), 0.0)
        assertEquals(126.987654, roundCoordinateComponent(126.9876544), 0.0)
        assertEquals(-73.123457, roundCoordinateComponent(-73.1234567), 0.0)
    }

    @Test
    fun `Firestore 좌표 map 은 iOS 와 같은 latitude longitude 키와 6자리 반올림을 사용한다`() {
        val map = coordinateToFirestoreMap(Coordinate(37.1234567, 126.9876544))

        assertEquals(37.123457, map["latitude"])
        assertEquals(126.987654, map["longitude"])
    }
}

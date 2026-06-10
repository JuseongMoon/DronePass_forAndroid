package com.ScienceFiction.DronePassAndroid.feature.map

import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.ScienceFiction.DronePassAndroid.feature.drone.filterShapesForSelectedDrones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapCameraFocusTest {

    @Test
    fun `지도 초기 카메라는 iOS처럼 서울 기본 좌표와 줌 12를 사용한다`() {
        assertEquals(37.575563, MapDefaultSeoulLatitude, 0.0)
        assertEquals(126.976793, MapDefaultSeoulLongitude, 0.0)
        assertEquals(12.0, MapInitialZoomLevel, 0.0)
    }

    @Test
    fun `최초 사용자 위치 센터링은 iOS CenterOnUserLocation처럼 줌 16을 사용한다`() {
        assertEquals(16.0, MapUserLocationZoomLevel, 0.0)
    }

    @Test
    fun `도형 포커스 기본 반경은 iOS처럼 100m 이다`() {
        assertEquals(100.0, ShapeFocusDefaultRadiusMeters, 0.0)
        assertEquals(14.0, calculateShapeFocusZoomLevel(ShapeFocusDefaultRadiusMeters), 0.0)
    }

    @Test
    fun `카메라 이동 이벤트는 iOS NotificationCenter 처럼 과거 이벤트를 재생하지 않는다`() {
        assertEquals(0, CameraEventReplay)
    }

    @Test
    fun `계정 종료 지도 하이라이트 정리 이벤트는 iOS NotificationCenter 처럼 과거 이벤트를 재생하지 않는다`() {
        assertEquals(0, MapHighlightClearEventReplay)
    }

    @Test
    fun `도형 포커스 오프셋은 iOS phone과 iPad 값을 따른다`() {
        assertEquals(ShapeFocusOffsets(x = 0.dp, y = 200.dp), resolveShapeFocusOffsets(isTablet = false))
        assertEquals(ShapeFocusOffsets(x = (-100).dp, y = 0.dp), resolveShapeFocusOffsets(isTablet = true))
    }

    @Test
    fun `도형 포커스 2단계 이동 지연은 iOS asyncAfter 0_3초를 따른다`() {
        assertEquals(300L, ShapeFocusZoomDurationMs)
        assertEquals(300L, ShapeFocusSecondStepDelayMs)
        assertEquals(500L, ShapeFocusMoveDurationMs)
    }

    @Test
    fun `네이버 지도 POI 심볼 탭은 iOS처럼 항상 소비한다`() {
        assertEquals(true, shouldConsumeNaverMapSymbolTap())
    }

    @Test
    fun `도형 포커스 줌 계산은 iOS 반경 범위와 선형 보간을 따른다`() {
        assertEquals(14.0, calculateShapeFocusZoomLevel(100.0), 0.0)
        assertEquals(11.0, calculateShapeFocusZoomLevel(3000.0), 0.0)
        assertEquals(
            14.0 - ((500.0 - 100.0) * (14.0 - 11.0) / (3000.0 - 100.0)),
            calculateShapeFocusZoomLevel(500.0),
            0.000001,
        )
    }

    @Test
    fun `이미 같은 좌표의 도형이 선택되어 있으면 iOS처럼 포커스 이동을 생략한다`() {
        val coordinate = Coordinate(37.0, 127.0)

        assertEquals(
            true,
            shouldSkipShapeFocusMove(
                currentSelectedShape = shape(
                    id = "selected",
                    coordinate = coordinate,
                    start = 1L,
                    end = 2L,
                ),
                targetCoordinate = coordinate,
            ),
        )
    }

    @Test
    fun `선택 도형이 없거나 좌표가 다르면 포커스 이동을 수행한다`() {
        val target = Coordinate(37.0, 127.0)

        assertEquals(false, shouldSkipShapeFocusMove(null, target))
        assertEquals(
            false,
            shouldSkipShapeFocusMove(
                currentSelectedShape = shape(
                    id = "selected",
                    coordinate = Coordinate(37.1, 127.1),
                    start = 1L,
                    end = 2L,
                ),
                targetCoordinate = target,
            ),
        )
    }

    @Test
    fun `도형 포커스 이벤트는 이미 같은 좌표가 선택되어 있으면 iOS처럼 생략된다`() {
        val coordinate = Coordinate(37.0, 127.0)

        assertNull(
            resolveShapeFocusCameraEvent(
                currentSelectedShape = shape(
                    id = "selected",
                    coordinate = coordinate,
                    start = 1L,
                    end = 2L,
                ),
                targetShape = shape(
                    id = "target",
                    coordinate = coordinate,
                    start = 1L,
                    end = 2L,
                ),
                skipIfAlreadyFocused = true,
            ),
        )
    }

    @Test
    fun `도형 포커스 이벤트는 iOS처럼 반경 기반 줌을 포함한다`() {
        val target = shape(
            id = "target",
            coordinate = Coordinate(37.5, 127.1),
            start = 1L,
            end = 2L,
        ).copy(radius = 500.0)

        assertEquals(
            CameraEvent.MoveToShape(
                coordinate = target.baseCoordinate,
                zoom = calculateShapeFocusZoomLevel(500.0),
            ),
            resolveShapeFocusCameraEvent(
                currentSelectedShape = null,
                targetShape = target,
                skipIfAlreadyFocused = true,
            ),
        )
    }

    @Test
    fun `도형 포커스 이벤트는 반경이 없으면 iOS SavedTableListView 기본 100m 를 사용한다`() {
        val target = shape(
            id = "target",
            coordinate = Coordinate(37.5, 127.1),
            start = 1L,
            end = 2L,
        ).copy(radius = null)

        assertEquals(
            CameraEvent.MoveToShape(
                coordinate = target.baseCoordinate,
                zoom = calculateShapeFocusZoomLevel(ShapeFocusDefaultRadiusMeters),
            ),
            resolveShapeFocusCameraEvent(
                currentSelectedShape = null,
                targetShape = target,
                skipIfAlreadyFocused = false,
            ),
        )
    }

    @Test
    fun `사각형 도형 포커스는 두 좌표의 중심과 geometry 반경을 사용한다`() {
        val target = shape(
            id = "rectangle",
            coordinate = Coordinate(37.0, 127.0),
            start = 1L,
            end = 2L,
        ).copy(
            shapeType = ShapeType.RECTANGLE,
            secondCoordinate = Coordinate(37.02, 127.04),
        )

        val focusCoordinate = calculateShapeFocusCoordinate(target)
        val focusRadius = calculateShapeFocusRadiusMeters(target)

        assertEquals(37.01, focusCoordinate.latitude, 0.000001)
        assertEquals(127.02, focusCoordinate.longitude, 0.000001)
        assertEquals(true, focusRadius > ShapeFocusDefaultRadiusMeters)
        assertEquals(
            CameraEvent.MoveToShape(
                coordinate = focusCoordinate,
                zoom = calculateShapeFocusZoomLevel(focusRadius),
            ),
            resolveShapeFocusCameraEvent(
                currentSelectedShape = null,
                targetShape = target,
                skipIfAlreadyFocused = false,
            ),
        )
    }

    @Test
    fun `다각형과 선 도형 포커스 반경은 실제 좌표 범위를 반영한다`() {
        val polygon = shape(
            id = "polygon",
            coordinate = Coordinate(37.0, 127.0),
            start = 1L,
            end = 2L,
        ).copy(
            shapeType = ShapeType.POLYGON,
            polygonCoordinates = listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.02, 127.0),
                Coordinate(37.02, 127.04),
            ),
        )
        val polyline = polygon.copy(
            id = "polyline",
            shapeType = ShapeType.POLYLINE,
            polygonCoordinates = null,
            polylineCoordinates = listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.0, 127.08),
            ),
        )

        val polygonFocusCoordinate = calculateShapeFocusCoordinate(polygon)
        assertEquals(37.01, polygonFocusCoordinate.latitude, 0.000001)
        assertEquals(127.02, polygonFocusCoordinate.longitude, 0.000001)
        assertEquals(true, calculateShapeFocusRadiusMeters(polygon) > ShapeFocusDefaultRadiusMeters)
        val polylineFocusCoordinate = calculateShapeFocusCoordinate(polyline)
        assertEquals(37.0, polylineFocusCoordinate.latitude, 0.000001)
        assertEquals(127.04, polylineFocusCoordinate.longitude, 0.000001)
        assertEquals(true, calculateShapeFocusRadiusMeters(polyline) > calculateShapeFocusRadiusMeters(polygon))
    }

    @Test
    fun `오버레이 탭은 iOS처럼 새 도형이면 저장 목록 포커스만 수행한다`() {
        val action = resolveShapeOverlayTapAction(
            tappedShape = shape(
                id = "target",
                coordinate = Coordinate(37.0, 127.0),
                start = 1L,
                end = 2L,
            ),
        )

        assertEquals("target", action?.shapeId)
    }

    @Test
    fun `오버레이 탭은 iOS처럼 이미 같은 좌표가 포커스되어 있어도 저장 목록 포커스를 수행한다`() {
        val coordinate = Coordinate(37.0, 127.0)

        val action = resolveShapeOverlayTapAction(
            tappedShape = shape(
                id = "target",
                coordinate = coordinate,
                start = 1L,
                end = 2L,
            ),
        )

        assertEquals("target", action?.shapeId)
    }

    @Test
    fun `오버레이 탭은 대상 도형을 찾지 못하면 iOS처럼 무시한다`() {
        assertNull(
            resolveShapeOverlayTapAction(
                tappedShape = null,
            ),
        )
    }

    @Test
    fun `지도 표시 도형은 iOS처럼 시작 전 숨김 설정을 반영한다`() {
        val now = System.currentTimeMillis()
        val filtered = filterShapesByMapVisibilitySettings(
            shapes = listOf(
                shape(id = "active", start = now - 1_000, end = now + 10_000),
                shape(id = "not-started", start = now + 10_000, end = now + 20_000),
            ),
            hideExpired = false,
            hideNotStarted = true,
        )

        assertEquals(listOf("active"), filtered.map { it.id })
    }

    @Test
    fun `지도 표시 도형은 iOS처럼 만료 숨김 설정을 반영한다`() {
        val now = System.currentTimeMillis()
        val filtered = filterShapesByMapVisibilitySettings(
            shapes = listOf(
                shape(id = "active", start = now - 20_000, end = now + 10_000),
                shape(id = "expired", start = now - 20_000, end = now - 10_000),
            ),
            hideExpired = true,
            hideNotStarted = false,
        )

        assertEquals(listOf("active"), filtered.map { it.id })
    }

    @Test
    fun `지도 표시 도형은 iOS ShapeModel처럼 종료 시각과 현재가 같으면 아직 만료가 아니다`() {
        val now = 10_000L
        val filtered = filterShapesByMapVisibilitySettings(
            shapes = listOf(
                shape(id = "ended-now", start = now - 1_000, end = now),
                shape(id = "expired", start = now - 1_000, end = now - 1),
            ),
            hideExpired = true,
            hideNotStarted = false,
            now = now,
        )

        assertEquals(listOf("ended-now"), filtered.map { it.id })
        assertEquals(false, isMapShapeExpired(flightEndDateMillis = now, now = now))
        assertEquals(true, isMapShapeExpired(flightEndDateMillis = now - 1, now = now))
    }

    @Test
    fun `지도 표시 도형은 iOS처럼 시작 전 필터 후 만료 필터를 적용한다`() {
        val now = System.currentTimeMillis()
        val filtered = filterShapesByMapVisibilitySettings(
            shapes = listOf(
                shape(id = "active", start = now - 20_000, end = now + 10_000),
                shape(id = "not-started", start = now + 10_000, end = now + 20_000),
                shape(id = "expired", start = now - 20_000, end = now - 10_000),
            ),
            hideExpired = true,
            hideNotStarted = true,
        )

        assertEquals(listOf("active"), filtered.map { it.id })
    }

    @Test
    fun `지도 포커스 요청은 실제 지도에 표시되는 도형만 대상으로 삼는다`() {
        val now = System.currentTimeMillis()
        val visibleShapes = filterShapesByMapVisibilitySettings(
            shapes = listOf(
                shape(id = "active", start = now - 20_000, end = now + 10_000),
                shape(id = "expired", start = now - 20_000, end = now - 10_000),
            ),
            hideExpired = true,
            hideNotStarted = false,
        )

        assertEquals("active", resolvePendingMapShapeRequestTarget("active", visibleShapes)?.id)
        assertNull(resolvePendingMapShapeRequestTarget("expired", visibleShapes))
    }

    @Test
    fun `지도 포커스 요청은 iOS처럼 선택된 드론의 표시 도형만 대상으로 삼는다`() {
        val visibleShapes = filterShapesForSelectedDrones(
            shapes = listOf(
                shape(id = "selected-drone-shape", start = 1_000, end = 3_000, droneId = "drone-a"),
                shape(id = "hidden-drone-shape", start = 1_000, end = 3_000, droneId = "drone-b"),
            ),
            activeDrones = listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
            ),
            selectedDroneIds = setOf("drone-a"),
        )

        assertEquals(
            "selected-drone-shape",
            resolvePendingMapShapeRequestTarget("selected-drone-shape", visibleShapes)?.id,
        )
        assertNull(resolvePendingMapShapeRequestTarget("hidden-drone-shape", visibleShapes))
    }

    @Test
    fun `상세에서 편집한 도형은 iOS처럼 저장 후 상세 화면으로 돌아간다`() {
        assertEquals(
            ShapeEditPostSaveAction.RETURN_TO_DETAIL,
            resolveShapeEditPostSaveAction(
                isDuplicate = false,
                focusAfterSave = false,
                returnToDetailAfterEditSave = true,
            ),
        )
    }

    @Test
    fun `새 도형과 복제 저장은 상세 복귀보다 저장 목록 포커스를 우선한다`() {
        assertEquals(
            ShapeEditPostSaveAction.FOCUS_SAVED_LIST,
            resolveShapeEditPostSaveAction(
                isDuplicate = false,
                focusAfterSave = true,
                returnToDetailAfterEditSave = true,
            ),
        )
        assertEquals(
            ShapeEditPostSaveAction.FOCUS_SAVED_LIST,
            resolveShapeEditPostSaveAction(
                isDuplicate = true,
                focusAfterSave = true,
                returnToDetailAfterEditSave = true,
            ),
        )
    }

    @Test
    fun `저장 목록 바로 편집 경로는 저장 후 상세를 열지 않는다`() {
        assertEquals(
            ShapeEditPostSaveAction.CLOSE,
            resolveShapeEditPostSaveAction(
                isDuplicate = false,
                focusAfterSave = false,
                returnToDetailAfterEditSave = false,
            ),
        )
    }

    @Test
    fun `지도 상세에서 시작한 도형 편집 취소는 iOS처럼 상세 화면으로 돌아간다`() {
        assertEquals(
            true,
            shouldReturnToShapeDetailAfterEditDismiss(
                selectedShapeId = "shape-a",
                returnToDetailAfterEditDismiss = true,
            ),
        )
    }

    @Test
    fun `저장 목록에서 바로 들어간 지도 편집 취소는 상세 화면을 열지 않는다`() {
        assertEquals(
            false,
            shouldReturnToShapeDetailAfterEditDismiss(
                selectedShapeId = "shape-a",
                returnToDetailAfterEditDismiss = false,
            ),
        )
        assertEquals(
            false,
            shouldReturnToShapeDetailAfterEditDismiss(
                selectedShapeId = null,
                returnToDetailAfterEditDismiss = true,
            ),
        )
    }

    @Test
    fun `지도 포커스 요청은 활성 도형 목록이 아직 비어 있으면 소비하지 않는다`() {
        assertEquals(false, shouldConsumeMissingMapShapeRequest(activeShapes = emptyList()))
    }

    @Test
    fun `지도 포커스 요청은 활성 도형 목록 준비 후 대상이 없으면 소비한다`() {
        assertEquals(
            true,
            shouldConsumeMissingMapShapeRequest(
                activeShapes = listOf(shape(id = "other", start = 1_000, end = 3_000)),
            ),
        )
    }

    @Test
    fun `지도 포커스 요청은 대상 도형이 표시 필터로 숨겨져도 전체 활성 목록이 준비되면 소비한다`() {
        assertEquals(
            true,
            shouldConsumeMissingMapShapeRequest(
                activeShapes = listOf(shape(id = "hidden", start = 1_000, end = 3_000)),
            ),
        )
    }

    @Test
    fun `플러스 버튼은 지도 중심이 있으면 iOS처럼 해당 중심 좌표로 도형을 만든다`() {
        assertEquals(
            Coordinate(37.4, 127.2),
            resolveCreateShapeCoordinateFromMapCenter(latitude = 37.4, longitude = 127.2),
        )
    }

    @Test
    fun `플러스 버튼은 지도 중심이 없으면 iOS처럼 서울 시청 좌표를 사용한다`() {
        assertEquals(37.5665, MapCreateShapeFallbackLatitude, 0.0)
        assertEquals(126.9780, MapCreateShapeFallbackLongitude, 0.0)
        assertEquals(
            Coordinate(37.5665, 126.9780),
            resolveCreateShapeCoordinateFromMapCenter(latitude = null, longitude = null),
        )
    }

    @Test
    fun `롱프레스 역지오코딩 성공은 iOS처럼 새 도형 확인창을 준비한다`() {
        val coordinate = Coordinate(37.5665, 126.9780)
        val request = pendingNewShapeRequestForReverseGeocodeResult(
            coordinate = coordinate,
            address = "서울특별시 중구 세종대로 110",
        )

        assertEquals(coordinate, request.coordinate)
        assertEquals("서울특별시 중구 세종대로 110", request.address)
        assertEquals(NewShapeConfirmDialogType.CONFIRM, request.dialogType)
    }

    @Test
    fun `롱프레스 역지오코딩 실패나 빈 주소는 iOS처럼 주소 검색 실패 확인창을 준비한다`() {
        val coordinate = Coordinate(37.5665, 126.9780)

        assertEquals(
            NewShapeConfirmDialogType.GEOCODING_FAILED,
            pendingNewShapeRequestForReverseGeocodeResult(coordinate, null).dialogType,
        )
        assertEquals(
            NewShapeConfirmDialogType.GEOCODING_FAILED,
            pendingNewShapeRequestForReverseGeocodeResult(coordinate, "").dialogType,
        )
    }

    @Test
    fun `주소 검색 실패 확인 후 편집 진입은 iOS처럼 주소 없음 fallback을 사용한다`() {
        val request = pendingNewShapeRequestForReverseGeocodeResult(
            coordinate = Coordinate(37.5665, 126.9780),
            address = null,
        )

        assertEquals(
            "해당 위치의 주소가 존재하지 않습니다",
            resolvePendingNewShapeAddress(
                request = request,
                addressNotFoundFallback = "해당 위치의 주소가 존재하지 않습니다",
            ),
        )
    }

    private fun shape(
        id: String,
        start: Long,
        end: Long,
        coordinate: Coordinate = Coordinate(37.0, 127.0),
        droneId: String = "drone-a",
    ): ShapeModel {
        return ShapeModel(
            id = id,
            title = id,
            baseCoordinate = coordinate,
            flightStartDate = start,
            flightEndDate = end,
            droneId = droneId,
        )
    }
}

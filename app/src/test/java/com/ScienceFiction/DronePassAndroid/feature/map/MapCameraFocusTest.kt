package com.ScienceFiction.DronePassAndroid.feature.map

import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import com.ScienceFiction.DronePassAndroid.feature.drone.filterShapesForSelectedDrones
import com.naver.maps.map.LocationTrackingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MapCameraFocusTest {

    @Test
    fun `지도 초기 카메라는 iOS처럼 서울 기본 좌표와 줌 12를 사용한다`() {
        assertEquals(37.575563, MapDefaultSeoulLatitude, 0.0)
        assertEquals(126.976793, MapDefaultSeoulLongitude, 0.0)
        assertEquals(12.0, MapInitialZoomLevel, 0.0)
    }

    @Test
    fun `사용자 위치 센터링 알림 경로는 iOS MainView CenterOnUserLocation처럼 줌 16을 사용한다`() {
        assertEquals(16.0, MapUserLocationZoomLevel, 0.0)
    }

    @Test
    fun `최초 사용자 위치 센터링은 마지막 위치가 없으면 현재 위치를 한 번 요청한다`() {
        assertEquals(false, shouldRequestCurrentLocationFallback(lastLocationAvailable = true))
        assertEquals(true, shouldRequestCurrentLocationFallback(lastLocationAvailable = false))
    }

    @Test
    fun `사용자 위치 오버레이는 iOS처럼 계속 따라가기 모드가 아니다`() {
        assertEquals(LocationTrackingMode.NoFollow, MapInitialLocationTrackingMode)
    }

    @Test
    fun `지도 위치 권한은 대략적인 위치만 허용되어도 사용 가능하다`() {
        assertEquals(
            true,
            hasUsableMapLocationPermission(
                fineLocationGranted = false,
                coarseLocationGranted = true,
            ),
        )
    }

    @Test
    fun `지도 위치 권한은 정확한 위치만 허용되어도 사용 가능하다`() {
        assertEquals(
            true,
            hasUsableMapLocationPermission(
                fineLocationGranted = true,
                coarseLocationGranted = false,
            ),
        )
    }

    @Test
    fun `지도 위치 권한은 fine 과 coarse 가 모두 없을 때만 거부 상태다`() {
        assertEquals(
            false,
            hasUsableMapLocationPermission(
                fineLocationGranted = false,
                coarseLocationGranted = false,
            ),
        )
    }

    @Test
    fun `지도 위치 권한 거부 상태는 iOS처럼 지도 위 차단 UI를 띄우지 않는다`() {
        val source = File("src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapScreen.kt").readText()
        assertTrue(!source.contains("map_permission_required"))
        assertTrue(!source.contains("map_permission_request"))
        assertTrue(!source.contains("map_permission_dialog_title"))
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
    fun `계정 종료 지도 정리는 iOS ClearMapOverlays처럼 선택과 하이라이트만 지운다`() {
        assertEquals(
            MapAccountSessionEndCleanup(
                clearSelection = true,
                emitHighlightClearEvent = true,
                clearLocalShapeOverlays = false,
            ),
            resolveMapAccountSessionEndCleanup(),
        )
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
    fun `이미 같은 좌표의 도형이 선택되어 있으면 iOS처럼 중복 포커스 이동을 생략한다`() {
        val coordinate = Coordinate(37.0, 127.0)
        val selected = shape(
            id = "selected",
            coordinate = coordinate,
            start = 1L,
            end = 2L,
        )

        assertEquals(
            true,
            shouldSkipShapeFocusMove(
                currentSelectedShape = selected,
                targetShape = selected,
            ),
        )
    }

    @Test
    fun `선택 도형이 없거나 대상 좌표가 다르면 포커스 이동을 수행한다`() {
        val target = shape(
            id = "target",
            coordinate = Coordinate(37.0, 127.0),
            start = 1L,
            end = 2L,
        )

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
                targetShape = target,
            ),
        )
    }

    @Test
    fun `도형 포커스 이벤트는 이미 같은 도형이 선택되어 있으면 생략된다`() {
        val coordinate = Coordinate(37.0, 127.0)
        val selected = shape(
            id = "selected",
            coordinate = coordinate,
            start = 1L,
            end = 2L,
        )

        assertNull(
            resolveShapeFocusCameraEvent(
                currentSelectedShape = selected,
                targetShape = selected,
                skipIfAlreadyFocused = true,
            ),
        )
    }

    @Test
    fun `같은 중심점의 다른 반경 도형은 iOS처럼 중복 포커스 이동을 생략한다`() {
        val coordinate = Coordinate(37.0, 127.0)
        val selected = shape(
            id = "selected",
            coordinate = coordinate,
            start = 1L,
            end = 2L,
        ).copy(radius = 100.0)
        val target = shape(
            id = "target",
            coordinate = coordinate,
            start = 1L,
            end = 2L,
        ).copy(radius = 2_000.0)

        assertNull(
            resolveShapeFocusCameraEvent(
                currentSelectedShape = selected,
                targetShape = target,
                skipIfAlreadyFocused = true,
            ),
        )
    }

    @Test
    fun `비원형 도형 포커스 이벤트도 같은 도형이면 생략된다`() {
        val rectangle = shape(
            id = "rectangle",
            coordinate = Coordinate(37.0, 127.0),
            start = 1L,
            end = 2L,
        ).copy(
            shapeType = ShapeType.RECTANGLE,
            secondCoordinate = Coordinate(37.02, 127.04),
        )

        assertNull(
            resolveShapeFocusCameraEvent(
                currentSelectedShape = rectangle,
                targetShape = rectangle,
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
                highlightShapeId = target.id,
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
                highlightShapeId = target.id,
            ),
            resolveShapeFocusCameraEvent(
                currentSelectedShape = null,
                targetShape = target,
                skipIfAlreadyFocused = false,
            ),
        )
    }

    @Test
    fun `사각형 도형 포커스는 반경이 없으면 iOS처럼 baseCoordinate와 기본 반경을 사용한다`() {
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

        assertEquals(target.baseCoordinate, focusCoordinate)
        assertEquals(ShapeFocusDefaultRadiusMeters, focusRadius, 0.0)
        assertEquals(
            CameraEvent.MoveToShape(
                coordinate = target.baseCoordinate,
                zoom = calculateShapeFocusZoomLevel(ShapeFocusDefaultRadiusMeters),
                highlightShapeId = target.id,
            ),
            resolveShapeFocusCameraEvent(
                currentSelectedShape = null,
                targetShape = target,
                skipIfAlreadyFocused = false,
            ),
        )
    }

    @Test
    fun `사각형 도형 포커스는 radius 필드가 있으면 iOS MoveToShapeData 처럼 해당 반경을 사용한다`() {
        val target = shape(
            id = "rectangle",
            coordinate = Coordinate(37.0, 127.0),
            start = 1L,
            end = 2L,
        ).copy(
            shapeType = ShapeType.RECTANGLE,
            secondCoordinate = Coordinate(37.02, 127.04),
            radius = 2_000.0,
        )

        assertEquals(target.baseCoordinate, calculateShapeFocusCoordinate(target))
        assertEquals(2_000.0, calculateShapeFocusRadiusMeters(target), 0.0)
        assertEquals(
            CameraEvent.MoveToShape(
                coordinate = target.baseCoordinate,
                zoom = calculateShapeFocusZoomLevel(2_000.0),
                highlightShapeId = target.id,
            ),
            resolveShapeFocusCameraEvent(
                currentSelectedShape = null,
                targetShape = target,
                skipIfAlreadyFocused = false,
            ),
        )
    }

    @Test
    fun `다각형과 선 도형 포커스도 iOS처럼 baseCoordinate와 기본 반경을 사용한다`() {
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
        assertEquals(polygon.baseCoordinate, polygonFocusCoordinate)
        assertEquals(ShapeFocusDefaultRadiusMeters, calculateShapeFocusRadiusMeters(polygon), 0.0)
        val polylineFocusCoordinate = calculateShapeFocusCoordinate(polyline)
        assertEquals(polyline.baseCoordinate, polylineFocusCoordinate)
        assertEquals(ShapeFocusDefaultRadiusMeters, calculateShapeFocusRadiusMeters(polyline), 0.0)
    }

    @Test
    fun `저장 목록에서 들어온 지도 포커스 요청은 iOS처럼 기존 포커스 중복 이동을 건너뛴다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "if (focusShapeId != null && mapReady)",
                "resolvePendingMapShapeRequestTarget(focusShapeId, visibleShapes)",
                "viewModel.moveCameraToShape(shape, skipIfAlreadyFocused = true)",
                "onFocusConsumed()",
            ),
        )
    }

    @Test
    fun `도형 포커스 카메라 이벤트는 iOS처럼 줌 후 하이라이트 후 오프셋 이동한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "is CameraEvent.MoveToShape ->",
                "CameraUpdate.zoomTo(event.zoom)",
                "map.moveCamera(zoomUpdate)",
                "delay(ShapeFocusSecondStepDelayMs)",
                "event.highlightShapeId?.let(viewModel::selectShapeForMapFocus)",
                "offsetLatLng(",
                "CameraPosition(offsetCenter, event.zoom)",
                "map.moveCamera(cameraUpdate)",
            ),
        )
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

        assertEquals(
            ShapeOverlayTapAction(
                shapeId = "target",
                showShapeDetail = false,
                requestSavedListFocus = true,
            ),
            action,
        )
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

        assertEquals(
            ShapeOverlayTapAction(
                shapeId = "target",
                showShapeDetail = false,
                requestSavedListFocus = true,
            ),
            action,
        )
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
    fun `지도 오버레이 탭은 iOS처럼 하이라이트를 먼저 갱신하고 저장 목록 포커스를 요청한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapViewModel.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/map/MapViewModel.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "fun onShapeOverlayTapped(shapeId: String)",
                "_selectedShapeId.value = action.shapeId",
                "_showShapeDetail.value = action.showShapeDetail",
                "_savedShapeFocusEvent.emit(action.shapeId)",
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
    fun `지도 포커스 요청은 iOS처럼 droneId 없는 레거시 도형을 첫 활성 드론 소속으로 본다`() {
        val activeDrones = listOf(
            DroneModel(id = "drone-a", name = "A"),
            DroneModel(id = "drone-b", name = "B"),
        )
        val shapes = listOf(
            shape(id = "legacy-shape", start = 1_000, end = 3_000, droneId = null),
            shape(id = "second-drone-shape", start = 1_000, end = 3_000, droneId = "drone-b"),
        )

        val firstDroneVisibleShapes = filterShapesForSelectedDrones(
            shapes = shapes,
            activeDrones = activeDrones,
            selectedDroneIds = setOf("drone-a"),
        )
        val secondDroneVisibleShapes = filterShapesForSelectedDrones(
            shapes = shapes,
            activeDrones = activeDrones,
            selectedDroneIds = setOf("drone-b"),
        )

        assertEquals(
            "legacy-shape",
            resolvePendingMapShapeRequestTarget("legacy-shape", firstDroneVisibleShapes)?.id,
        )
        assertNull(resolvePendingMapShapeRequestTarget("legacy-shape", secondDroneVisibleShapes))
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
    fun `저장 목록 포커스 저장은 iOS처럼 카메라 이동 지연 하이라이트에 선택을 맡긴다`() {
        assertEquals(
            false,
            shouldSelectShapeImmediatelyAfterSave(ShapeEditPostSaveAction.FOCUS_SAVED_LIST),
        )
        assertEquals(
            true,
            shouldSelectShapeImmediatelyAfterSave(ShapeEditPostSaveAction.RETURN_TO_DETAIL),
        )
        assertEquals(
            false,
            shouldSelectShapeImmediatelyAfterSave(ShapeEditPostSaveAction.CLOSE),
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
    fun `스케치 모드 중 지도 롱프레스는 iOS처럼 새 도형 생성을 열지 않는다`() {
        assertEquals(false, shouldHandleMapLongClickForShapeCreation(isSketchMode = true))
        assertEquals(true, shouldHandleMapLongClickForShapeCreation(isSketchMode = false))
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
    fun `롱프레스 역지오코딩은 iOS처럼 성공한 빈 주소도 새 도형 확인창으로 유지한다`() {
        val coordinate = Coordinate(37.5665, 126.9780)

        assertEquals(
            NewShapeConfirmDialogType.GEOCODING_FAILED,
            pendingNewShapeRequestForReverseGeocodeResult(coordinate, null).dialogType,
        )
        listOf("", "   ").forEach { address ->
            val request = pendingNewShapeRequestForReverseGeocodeResult(coordinate, address)

            assertEquals(address, request.address)
            assertEquals(NewShapeConfirmDialogType.CONFIRM, request.dialogType)
            assertEquals(
                address,
                resolvePendingNewShapeAddress(
                    request = request,
                    addressNotFoundFallback = "해당 위치의 주소가 존재하지 않습니다",
                ),
            )
        }
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
        droneId: String? = "drone-a",
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

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        for (token in tokens) {
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "$token should appear after index $previousIndex",
                index > previousIndex,
            )
            previousIndex = index
        }
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?: error("Project file not found: ${candidates.joinToString()}")
    }
}

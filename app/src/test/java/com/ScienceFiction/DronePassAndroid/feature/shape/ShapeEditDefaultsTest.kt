package com.ScienceFiction.DronePassAndroid.feature.shape

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.preferencesOf
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ShapeEditDefaultsTest {

    @Test
    fun `도형 편집 일단위 입력 기본값은 iOS처럼 꺼져 있다`() {
        assertFalse(DefaultShapeEditDateOnlyMode)
        assertFalse(ShapeEditDefaults().isDateOnly)
        assertFalse(storedShapeEditDefaults(preferencesOf()).isDateOnly)
    }

    @Test
    fun `좌표 입력 시트는 iOS처럼 중간 detent 를 허용한다`() {
        assertFalse(CoordinateInputSheetSkipPartiallyExpanded)
    }

    @Test
    fun `좌표 입력 시트는 iOS ShapeEditView처럼 외부 인터랙티브 닫기를 막는다`() {
        assertFalse(CoordinateInputSheetInteractiveDismissEnabled)
    }

    @Test
    fun `좌표 입력 시트는 iOS ShapeEditView처럼 85퍼센트 detent 로 표시한다`() {
        assertEquals(0.85f, CoordinateInputSheetHeightFraction)
    }

    @Test
    fun `좌표 입력 검색바는 iOS CoordinateView처럼 SearchBar 토큰을 사용한다`() {
        assertEquals(8.dp, SearchAddressBarCornerRadius)
        assertEquals(8.dp, SearchAddressBarInnerPadding)
        assertEquals(0xFFF2F2F7.toInt(), SearchAddressBarBackgroundColor.toArgb())
        assertEquals(0xFF8E8E93.toInt(), SearchAddressBarIconColor.toArgb())
    }

    @Test
    fun `좌표 입력 유효 메시지는 iOS처럼 systemGreen 색상을 사용한다`() {
        assertEquals(0xFF34C759.toInt(), CoordinateValidationSuccessColor.toArgb())
    }

    @Test
    fun `좌표 입력 주소 결과 카드는 iOS AddressResultView 토큰을 사용한다`() {
        assertEquals(12.dp, CoordinateAddressResultCardCornerRadius)
        assertEquals(5.dp, CoordinateAddressResultCardShadowElevation)
        assertEquals(0x1A000000, CoordinateAddressResultCardShadowColor.toArgb())
    }

    @Test
    fun `좌표 입력 주소 검색 중 표시는 iOS ProgressView처럼 중앙 스피너 영역을 사용한다`() {
        assertEquals(180.dp, CoordinateResolvingIndicatorHeight)
    }

    @Test
    fun `좌표 입력 안내 카드는 iOS처럼 500dp 최대 폭과 20dp 모서리를 사용한다`() {
        assertEquals(500.dp, CoordinateGuideCardMaxWidth)
        assertEquals(20.dp, CoordinateGuideCardCornerRadius)
        assertEquals(16.dp, CoordinateGuideCardPadding)
        assertEquals(8.dp, CoordinateGuideCardVerticalSpacing)
    }

    @Test
    fun `좌표 입력 주소 검색 성공은 즉시 저장하지 않고 선택 가능한 결과를 만든다`() {
        val coordinate = Coordinate(latitude = 37.648611, longitude = 126.686667)
        val result = coordinateAddressSearchResultOrNull(
            resolvedAddress = "경기도 김포시 고촌읍",
            coordinate = coordinate,
            originalText = "37.648611, 126.686667",
        )

        assertEquals(
            CoordinateAddressSearchResult(
                address = "경기도 김포시 고촌읍",
                coordinate = coordinate,
                originalText = "37.648611, 126.686667",
            ),
            result,
        )
        assertTrue(canConfirmCoordinateInput(isCoordinateInvalid = false))
    }

    @Test
    fun `좌표 입력 확인 버튼은 iOS처럼 좌표 형식이 invalid일 때만 비활성화한다`() {
        assertTrue(canConfirmCoordinateInput(isCoordinateInvalid = false))
        assertFalse(canConfirmCoordinateInput(isCoordinateInvalid = true))
    }

    @Test
    fun `좌표 입력 주소 검색 결과는 주소가 없으면 카드로 만들지 않는다`() {
        assertNull(
            coordinateAddressSearchResultOrNull(
                resolvedAddress = "",
                coordinate = Coordinate(latitude = 37.648611, longitude = 126.686667),
                originalText = "37.648611, 126.686667",
            ),
        )
    }

    @Test
    fun `좌표 입력 주소 검색 결과는 iOS처럼 공백 주소를 값으로 보존한다`() {
        val coordinate = Coordinate(latitude = 37.648611, longitude = 126.686667)
        val result = coordinateAddressSearchResultOrNull(
            resolvedAddress = "   ",
            coordinate = coordinate,
            originalText = "37.648611, 126.686667",
        )

        assertEquals(
            CoordinateAddressSearchResult(
                address = "   ",
                coordinate = coordinate,
                originalText = "37.648611, 126.686667",
            ),
            result,
        )
    }

    @Test
    fun `기본정보 행 placeholder는 iOS처럼 빈 문자열에만 적용한다`() {
        assertEquals("좌표를 입력하세요", shapeEditDisplayText("", placeholder = "좌표를 입력하세요"))
        assertTrue(isShapeEditPlaceholder(""))

        assertEquals("   ", shapeEditDisplayText("   ", placeholder = "좌표를 입력하세요"))
        assertFalse(isShapeEditPlaceholder("   "))
    }

    @Test
    fun `도형 저장 실패 문구는 iOS처럼 null일 때만 fallback을 사용한다`() {
        assertEquals(
            "add: message",
            shapeEditSaveFailureMessage(
                isEditMode = false,
                localizedMessage = "message",
                fallback = "fallback",
                addFailureFormat = "add: %1\$s",
                updateFailureFormat = "update: %1\$s",
            ),
        )
        assertEquals(
            "add: ",
            shapeEditSaveFailureMessage(
                isEditMode = false,
                localizedMessage = "",
                fallback = "fallback",
                addFailureFormat = "add: %1\$s",
                updateFailureFormat = "update: %1\$s",
            ),
        )
        assertEquals(
            "update:    ",
            shapeEditSaveFailureMessage(
                isEditMode = true,
                localizedMessage = "   ",
                fallback = "fallback",
                addFailureFormat = "add: %1\$s",
                updateFailureFormat = "update: %1\$s",
            ),
        )
        assertEquals(
            "update: fallback",
            shapeEditSaveFailureMessage(
                isEditMode = true,
                localizedMessage = null,
                fallback = "fallback",
                addFailureFormat = "add: %1\$s",
                updateFailureFormat = "update: %1\$s",
            ),
        )
    }

    @Test
    fun `날짜 시간 선택 시트는 iOS처럼 한 화면에서 완료한다`() {
        assertTrue(ShapeDateTimeSelectionSkipPartiallyExpanded)
    }

    @Test
    fun `날짜 시간 선택 시트는 iOS처럼 시스템 12 24시간 설정을 따른다`() {
        assertTrue(shapeDateTimeSelectionUses24HourClock(systemUses24HourClock = true))
        assertFalse(shapeDateTimeSelectionUses24HourClock(systemUses24HourClock = false))
    }

    @Test
    fun `도형 편집 메모 입력 높이는 iOS MemoSection의 170pt 최소 높이를 따른다`() {
        assertEquals(170.dp, ShapeEditMemoMinHeight)
    }

    @Test
    fun `도형 편집 기본값 키 이름은 iOS UserDefaults 이름과 동일하게 유지한다`() {
        assertEquals("lastSelectedDroneId", ShapeEditPreferenceKeys.LAST_SELECTED_DRONE_ID.name)
        assertEquals("lastRadius", ShapeEditPreferenceKeys.LAST_RADIUS.name)
        assertEquals("lastHeight", ShapeEditPreferenceKeys.LAST_HEIGHT.name)
        assertEquals("lastStartDate", ShapeEditPreferenceKeys.LAST_START_DATE.name)
        assertEquals("lastEndDate", ShapeEditPreferenceKeys.LAST_END_DATE.name)
        assertEquals("isDateOnlyMode", ShapeEditPreferenceKeys.DATE_ONLY_MODE.name)
    }

    @Test
    fun `도형 편집 기본값은 기존 Android snake case 값을 fallback 으로 읽는다`() {
        val preferences = preferencesOf(
            ShapeEditPreferenceKeys.LEGACY_LAST_SELECTED_DRONE_ID to "legacy-drone",
            ShapeEditPreferenceKeys.LEGACY_LAST_RADIUS to "120",
            ShapeEditPreferenceKeys.LEGACY_LAST_HEIGHT to "50",
            ShapeEditPreferenceKeys.LEGACY_LAST_START_DATE to 1000L,
            ShapeEditPreferenceKeys.LEGACY_LAST_END_DATE to 2000L,
            ShapeEditPreferenceKeys.LEGACY_DATE_ONLY_MODE to true,
        )

        assertEquals(
            ShapeEditDefaults(
                selectedDroneId = "legacy-drone",
                radius = "120",
                height = "50",
                startDate = 1000L,
                endDate = 2000L,
                isDateOnly = true,
            ),
            storedShapeEditDefaults(preferences),
        )
    }

    @Test
    fun `도형 편집 기본값은 iOS primary 값이 있으면 legacy 값보다 우선한다`() {
        val preferences = preferencesOf(
            ShapeEditPreferenceKeys.LAST_SELECTED_DRONE_ID to "primary-drone",
            ShapeEditPreferenceKeys.LEGACY_LAST_SELECTED_DRONE_ID to "legacy-drone",
            ShapeEditPreferenceKeys.LAST_RADIUS to "180",
            ShapeEditPreferenceKeys.LEGACY_LAST_RADIUS to "120",
            ShapeEditPreferenceKeys.LAST_HEIGHT to "70",
            ShapeEditPreferenceKeys.LEGACY_LAST_HEIGHT to "50",
            ShapeEditPreferenceKeys.LAST_START_DATE to 3000L,
            ShapeEditPreferenceKeys.LEGACY_LAST_START_DATE to 1000L,
            ShapeEditPreferenceKeys.LAST_END_DATE to 4000L,
            ShapeEditPreferenceKeys.LEGACY_LAST_END_DATE to 2000L,
            ShapeEditPreferenceKeys.DATE_ONLY_MODE to false,
            ShapeEditPreferenceKeys.LEGACY_DATE_ONLY_MODE to true,
        )

        assertEquals(
            ShapeEditDefaults(
                selectedDroneId = "primary-drone",
                radius = "180",
                height = "70",
                startDate = 3000L,
                endDate = 4000L,
                isDateOnly = false,
            ),
            storedShapeEditDefaults(preferences),
        )
    }

    @Test
    fun `새 도형은 유효한 최근 선택 드론을 우선 사용한다`() {
        val drones = listOf(
            DroneModel(id = "drone-a", name = "A"),
            DroneModel(id = "drone-b", name = "B"),
        )

        val selectedDroneId = resolveInitialShapeEditDroneId(
            shape = null,
            activeDrones = drones,
            editDefaults = ShapeEditDefaults(selectedDroneId = "drone-b"),
            fallbackSelectedDroneId = "drone-a",
        )

        assertEquals("drone-b", selectedDroneId)
    }

    @Test
    fun `최근 선택 드론이 유효하지 않으면 현재 선택 후보를 사용한다`() {
        val drones = listOf(
            DroneModel(id = "drone-a", name = "A"),
            DroneModel(id = "drone-b", name = "B"),
        )

        val selectedDroneId = resolveInitialShapeEditDroneId(
            shape = null,
            activeDrones = drones,
            editDefaults = ShapeEditDefaults(selectedDroneId = "deleted-drone"),
            fallbackSelectedDroneId = "drone-a",
        )

        assertEquals("drone-a", selectedDroneId)
    }

    @Test
    fun `기존 레거시 도형은 첫 활성 드론으로 보정한다`() {
        val drones = listOf(DroneModel(id = "drone-a", name = "A"))

        val selectedDroneId = resolveInitialShapeEditDroneId(
            shape = ShapeModel(title = "Flight Area", droneId = null),
            activeDrones = drones,
            editDefaults = ShapeEditDefaults(selectedDroneId = "drone-b"),
            fallbackSelectedDroneId = "drone-b",
        )

        assertEquals("drone-a", selectedDroneId)
    }

    @Test
    fun `기존 도형의 드론 ID는 활성 목록에 없어도 보존한다`() {
        val selectedDroneId = resolveInitialShapeEditDroneId(
            shape = ShapeModel(title = "Flight Area", droneId = "deleted-drone"),
            activeDrones = listOf(DroneModel(id = "drone-a", name = "A")),
            editDefaults = ShapeEditDefaults(selectedDroneId = "drone-a"),
            fallbackSelectedDroneId = "drone-a",
        )

        assertEquals("deleted-drone", selectedDroneId)
    }

    @Test
    fun `제목이 비어 있는 기존 도형의 드론 ID도 iOS처럼 보존한다`() {
        val drones = listOf(
            DroneModel(id = "shape-drone", name = "Shape Drone"),
            DroneModel(id = "recent-drone", name = "Recent"),
            DroneModel(id = "fallback-drone", name = "Fallback"),
        )

        val selectedDroneId = resolveInitialShapeEditDroneId(
            shape = ShapeModel(title = "", droneId = "shape-drone"),
            activeDrones = drones,
            editDefaults = ShapeEditDefaults(selectedDroneId = "recent-drone"),
            fallbackSelectedDroneId = "fallback-drone",
        )

        assertEquals("shape-drone", selectedDroneId)
    }

    @Test
    fun `복제 편집 초기 제목은 iOS처럼 원본 제목을 그대로 사용한다`() {
        assertEquals(
            "Flight Area",
            resolveInitialShapeEditTitle(ShapeModel(title = "Flight Area")),
        )
    }

    @Test
    fun `새 도형 초기 제목은 비워두고 저장 시 기본 제목을 적용한다`() {
        assertEquals("", resolveInitialShapeEditTitle(null))
    }

    @Test
    fun `도형 편집 상단 제목은 iOS처럼 표시하지 않는다`() {
        assertEquals("", resolveShapeEditNavigationTitle(isEditMode = true))
        assertEquals("", resolveShapeEditNavigationTitle(isEditMode = false))
    }

    @Test
    fun `도형 편집 상단 toolbar는 iOS inline toolbar 높이와 좌우 액션 슬롯을 따른다`() {
        assertEquals(44.dp, ShapeEditNavigationHeaderHeight)
        assertEquals(80.dp, ShapeEditNavigationActionSlotWidth)
    }

    @Test
    fun `도형 편집 날짜 섹션은 iOS DateSection처럼 별도 헤더를 표시하지 않는다`() {
        assertFalse(ShowShapeEditFlightPeriodSectionHeader)
    }

    @Test
    fun `제목이 비어 있는 기존 도형도 iOS처럼 기존 편집 모드이다`() {
        val emptyTitleShape = ShapeModel(title = "")
        val existingShape = ShapeModel(title = "Flight Area")

        assertTrue(isExistingShapeEditMode(emptyTitleShape, isDuplicateMode = false))
        assertFalse(isExistingShapeEditMode(existingShape, isDuplicateMode = true))
        assertTrue(isExistingShapeEditMode(existingShape, isDuplicateMode = false))
    }

    @Test
    fun `새 도형은 iOS처럼 최근 반경과 고도를 우선 적용한다`() {
        val defaults = ShapeEditDefaults(
            radius = "150",
            height = "60",
        )

        assertEquals("150", resolveInitialShapeEditRadius(null, defaults))
        assertEquals("60", resolveInitialShapeEditHeight(null, defaults))
    }

    @Test
    fun `제목이 비어 있는 기존 도형은 iOS처럼 최근 기본값보다 도형 반경과 고도를 우선한다`() {
        val shape = ShapeModel(
            title = "",
            radius = 80.0,
            height = 20.0,
        )
        val defaults = ShapeEditDefaults(
            radius = "150",
            height = "60",
        )

        assertEquals("80", resolveInitialShapeEditRadius(shape, defaults))
        assertEquals("20", resolveInitialShapeEditHeight(shape, defaults))
    }

    @Test
    fun `제목이 있는 기존 도형은 iOS처럼 최근 기본값보다 도형 반경과 고도를 우선한다`() {
        val shape = ShapeModel(
            title = "Flight Area",
            radius = 80.0,
            height = 20.0,
        )
        val defaults = ShapeEditDefaults(
            radius = "150",
            height = "60",
        )

        assertEquals("80", resolveInitialShapeEditRadius(shape, defaults))
        assertEquals("20", resolveInitialShapeEditHeight(shape, defaults))
    }

    @Test
    fun `새 도형은 iOS처럼 최근 비행 기간을 우선 적용한다`() {
        val defaults = ShapeEditDefaults(
            startDate = 100L,
            endDate = 200L,
        )

        assertEquals(100L, resolveInitialShapeEditFlightStart(null, defaults, now = 1000L))
        assertEquals(200L, resolveInitialShapeEditFlightEnd(null, defaults, now = 1000L))
    }

    @Test
    fun `새 도형은 최근 비행 기간이 없으면 iOS처럼 현재 시각을 사용한다`() {
        assertEquals(1000L, resolveInitialShapeEditFlightStart(null, ShapeEditDefaults(), now = 1000L))
        assertEquals(1000L, resolveInitialShapeEditFlightEnd(null, ShapeEditDefaults(), now = 1000L))
    }

    @Test
    fun `제목이 비어 있는 기존 도형은 iOS처럼 도형 비행 기간을 사용한다`() {
        val shape = ShapeModel(
            title = "",
            flightStartDate = 10L,
            flightEndDate = 20L,
        )
        val defaults = ShapeEditDefaults(
            startDate = 100L,
            endDate = 200L,
        )

        assertEquals(10L, resolveInitialShapeEditFlightStart(shape, defaults, now = 1000L))
        assertEquals(20L, resolveInitialShapeEditFlightEnd(shape, defaults, now = 1000L))
    }

    @Test
    fun `제목이 있는 기존 도형은 iOS처럼 도형 비행 기간을 사용한다`() {
        val shape = ShapeModel(
            title = "Flight Area",
            flightStartDate = 10L,
            flightEndDate = 20L,
        )
        val defaults = ShapeEditDefaults(
            startDate = 100L,
            endDate = 200L,
        )

        assertEquals(10L, resolveInitialShapeEditFlightStart(shape, defaults, now = 1000L))
        assertEquals(20L, resolveInitialShapeEditFlightEnd(shape, defaults, now = 1000L))
    }

    @Test
    fun `기존 도형 종료일이 없으면 iOS처럼 현재 시각을 종료일 초기값으로 사용한다`() {
        val shape = ShapeModel(
            title = "Flight Area",
            flightStartDate = 10L,
            flightEndDate = null,
        )

        assertEquals(1000L, resolveInitialShapeEditFlightEnd(shape, ShapeEditDefaults(), now = 1000L))
    }

    @Test
    fun `기존 도형 종료일이 없던 편집 저장도 iOS처럼 보정된 종료일을 저장한다`() {
        val original = ShapeModel(
            id = "shape-without-end",
            title = "Flight Area",
            baseCoordinate = Coordinate(37.0, 127.0),
            radius = 100.0,
            flightStartDate = 10L,
            flightEndDate = null,
            createdAt = 1L,
        )
        val resolvedEndDate = resolveInitialShapeEditFlightEnd(
            shape = original,
            editDefaults = ShapeEditDefaults(),
            now = 1_000L,
        )

        val saved = buildShapeEditSavedShape(
            originalShape = original,
            isDuplicateMode = false,
            generatedId = "generated",
            title = original.title,
            defaultTitle = "새 도형",
            coordinate = original.baseCoordinate,
            address = original.address.orEmpty(),
            noAddressFallback = "주소를 찾을 수 없습니다",
            radius = "100",
            height = "",
            memo = "",
            selectedColor = original.color,
            selectedDroneId = original.droneId,
            flightStartDate = original.flightStartDate,
            flightEndDate = resolvedEndDate,
            now = 2_000L,
        )

        assertEquals(1_000L, saved.flightEndDate)
    }

    @Test
    fun `좌표 표시 문자열은 iOS처럼 DMS formattedCoordinate 를 사용한다`() {
        assertEquals(
            "37° 30′ 0″ N 126° 15′ 0″ E",
            formatShapeEditCoordinateText(Coordinate(37.5, 126.25)),
        )
    }

    @Test
    fun `좌표 입력 시트는 iOS CoordinateView처럼 기존 좌표로 검색창을 미리 채우지 않는다`() {
        assertEquals(
            "",
            initialCoordinateInputSheetText("37° 30′ 0″ N 126° 15′ 0″ E"),
        )
    }

    @Test
    fun `좌표 입력 검증 문구는 iOS처럼 검색 제출 전 타이핑만으로 표시하지 않는다`() {
        assertNull(coordinateInputValidationAfterTextChange())
    }

    @Test
    fun `좌표 입력 검증 문구는 iOS처럼 검색 제출 후 파싱 결과로 결정한다`() {
        assertTrue(coordinateInputValidationForSearch(Coordinate(37.5, 126.25)))
        assertFalse(coordinateInputValidationForSearch(null))
    }

    @Test
    fun `반경과 고도 입력은 iOS처럼 숫자만 보존한다`() {
        assertEquals("123", filterShapeEditNumberInput("1a2.3m"))
    }

    @Test
    fun `반경과 고도 입력은 iOS처럼 빈 값을 허용한다`() {
        assertEquals("", filterShapeEditNumberInput(""))
    }

    @Test
    fun `비원형 도형 저장 검증은 기존 geometry 보존을 위해 반경을 요구하지 않는다`() {
        assertEquals(
            ShapeEditSaveValidationError.RADIUS_REQUIRED,
            validateShapeEditSaveFields(
                coordinate = Coordinate(37.0, 127.0),
                address = "",
                radius = "",
                requiresRadius = true,
            ),
        )
        assertNull(
            validateShapeEditSaveFields(
                coordinate = Coordinate(37.0, 127.0),
                address = "",
                radius = "",
                requiresRadius = false,
            ),
        )
    }

    @Test
    fun `도형 편집 반경 입력은 신규와 원형 도형에만 표시한다`() {
        assertTrue(shouldRequireShapeEditRadius(null))
        assertTrue(shouldRequireShapeEditRadius(ShapeModel(shapeType = ShapeType.CIRCLE)))
        assertFalse(shouldRequireShapeEditRadius(ShapeModel(shapeType = ShapeType.RECTANGLE)))
        assertFalse(shouldRequireShapeEditRadius(ShapeModel(shapeType = ShapeType.POLYGON)))
        assertFalse(shouldRequireShapeEditRadius(ShapeModel(shapeType = ShapeType.POLYLINE)))
    }

    @Test
    fun `도형 편집 드론 색상 원은 iOS처럼 팔레트 색상이 있을 때만 표시한다`() {
        assertTrue(shouldShowShapeEditDroneColorIndicator(PaletteColor.BLUE))
        assertFalse(shouldShowShapeEditDroneColorIndicator(null))
    }

    @Test
    fun `도형 편집 드론 메뉴는 iOS처럼 현재 선택 드론에만 체크마크를 표시한다`() {
        assertTrue(shouldShowShapeEditDroneSelectionCheckmark("drone-a", "drone-a"))
        assertFalse(shouldShowShapeEditDroneSelectionCheckmark("drone-a", "drone-b"))
        assertFalse(shouldShowShapeEditDroneSelectionCheckmark("drone-a", null))
    }

    @Test
    fun `도형 저장 색상은 iOS처럼 선택된 드론의 원본 색상 문자열을 사용한다`() {
        val drone = DroneModel(
            id = "drone-custom",
            name = "Custom",
            color = "#123456",
        )

        assertEquals("#123456", resolveShapeEditSelectedColor(drone))
    }

    @Test
    fun `선택된 드론이 없으면 iOS처럼 전달된 기본 도형 색상을 저장한다`() {
        assertEquals(PaletteColor.GREEN.hex, resolveShapeEditSelectedColor(null, PaletteColor.GREEN.hex))
    }

    @Test
    fun `기본 도형 색상이 없으면 iOS 기본값인 파랑을 저장한다`() {
        assertEquals(PaletteColor.BLUE.hex, resolveShapeEditSelectedColor(null))
    }

    @Test
    fun `기본 도형 색상은 iOS ColorManager처럼 첫 활성 도형 팔레트 색상을 사용한다`() {
        val shapes = listOf(
            ShapeModel(id = "shape-green", color = "#34c759"),
            ShapeModel(id = "shape-red", color = PaletteColor.RED.hex),
        )

        assertEquals(PaletteColor.GREEN.hex, resolveShapeEditDefaultColor(shapes))
    }

    @Test
    fun `첫 활성 도형 색상이 팔레트 밖이면 기본 도형 색상은 iOS처럼 파랑이다`() {
        val shapes = listOf(ShapeModel(id = "shape-custom", color = "#123456"))

        assertEquals(PaletteColor.BLUE.hex, resolveShapeEditDefaultColor(shapes))
    }

    @Test
    fun `활성 도형이 없으면 기본 도형 색상은 iOS 기본값인 파랑이다`() {
        assertEquals(PaletteColor.BLUE.hex, resolveShapeEditDefaultColor(emptyList()))
    }

    @Test
    fun `기존 비원형 도형 저장은 타입과 geometry를 보존한다`() {
        val original = ShapeModel(
            id = "shape-rect",
            title = "기존 사각형",
            shapeType = ShapeType.RECTANGLE,
            baseCoordinate = Coordinate(37.0, 127.0),
            radius = null,
            secondCoordinate = Coordinate(37.5, 127.5),
            polygonCoordinates = listOf(Coordinate(37.2, 127.2)),
            polylineCoordinates = listOf(Coordinate(37.3, 127.3)),
            createdAt = 1_000L,
            deletedAt = 2_000L,
        )

        val saved = buildShapeEditSavedShape(
            originalShape = original,
            isDuplicateMode = false,
            generatedId = "generated-shape",
            title = "",
            defaultTitle = "새 도형",
            coordinate = Coordinate(36.0, 128.0),
            address = "",
            noAddressFallback = "주소를 찾을 수 없습니다",
            radius = "",
            height = "",
            memo = "",
            selectedColor = "#123456",
            selectedDroneId = "drone-a",
            flightStartDate = 3_000L,
            flightEndDate = 4_000L,
            now = 5_000L,
        )

        assertEquals("shape-rect", saved.id)
        assertEquals("새 도형", saved.title)
        assertEquals(ShapeType.RECTANGLE, saved.shapeType)
        assertEquals(Coordinate(36.0, 128.0), saved.baseCoordinate)
        assertNull(saved.radius)
        assertEquals(Coordinate(36.5, 128.5), saved.secondCoordinate)
        assertNull(saved.polygonCoordinates)
        assertNull(saved.polylineCoordinates)
        assertNull(saved.height)
        assertNull(saved.memo)
        assertEquals("주소를 찾을 수 없습니다", saved.address)
        assertEquals("#123456", saved.color)
        assertEquals("drone-a", saved.droneId)
        assertEquals(1_000L, saved.createdAt)
        assertEquals(2_000L, saved.deletedAt)
        assertEquals(3_000L, saved.flightStartDate)
        assertEquals(4_000L, saved.flightEndDate)
        assertEquals(5_000L, saved.updatedAt)
    }

    @Test
    fun `도형 저장은 iOS처럼 공백 제목 메모 주소를 값으로 보존한다`() {
        val saved = buildShapeEditSavedShape(
            originalShape = null,
            isDuplicateMode = false,
            generatedId = "generated-shape",
            title = "   ",
            defaultTitle = "새 도형",
            coordinate = Coordinate(37.0, 127.0),
            address = "   ",
            noAddressFallback = "주소를 찾을 수 없습니다",
            radius = "100",
            height = "",
            memo = "   ",
            selectedColor = "#123456",
            selectedDroneId = null,
            flightStartDate = 3_000L,
            flightEndDate = 4_000L,
            now = 5_000L,
        )

        assertEquals("   ", saved.title)
        assertEquals("   ", saved.memo)
        assertEquals("   ", saved.address)
    }

    @Test
    fun `기존 polygon 도형 저장은 좌표 변경량만큼 전체 geometry를 이동한다`() {
        val original = ShapeModel(
            id = "shape-polygon",
            title = "기존 다각형",
            shapeType = ShapeType.POLYGON,
            baseCoordinate = Coordinate(37.0, 127.0),
            radius = null,
            polygonCoordinates = listOf(
                Coordinate(37.0, 127.0),
                Coordinate(37.5, 127.0),
                Coordinate(37.5, 127.5),
            ),
            createdAt = 1_000L,
        )

        val saved = buildShapeEditSavedShape(
            originalShape = original,
            isDuplicateMode = false,
            generatedId = "generated-shape",
            title = original.title,
            defaultTitle = "새 도형",
            coordinate = Coordinate(38.0, 128.0),
            address = "",
            noAddressFallback = "주소를 찾을 수 없습니다",
            radius = "",
            height = "",
            memo = "",
            selectedColor = "#123456",
            selectedDroneId = null,
            flightStartDate = 3_000L,
            flightEndDate = 4_000L,
            now = 5_000L,
        )

        assertEquals(ShapeType.POLYGON, saved.shapeType)
        assertEquals(Coordinate(38.0, 128.0), saved.baseCoordinate)
        assertNull(saved.radius)
        assertEquals(
            listOf(
                Coordinate(38.0, 128.0),
                Coordinate(38.5, 128.0),
                Coordinate(38.5, 128.5),
            ),
            saved.polygonCoordinates,
        )
    }

    @Test
    fun `복제 저장은 iOS처럼 새 id와 새 createdAt을 사용한다`() {
        val original = ShapeModel(
            id = "original-shape",
            title = "원본",
            baseCoordinate = Coordinate(37.0, 127.0),
            radius = 100.0,
            createdAt = 1_000L,
        )

        val saved = buildShapeEditSavedShape(
            originalShape = original,
            isDuplicateMode = true,
            generatedId = "duplicate-shape",
            title = "복제본",
            defaultTitle = "새 도형",
            coordinate = Coordinate(36.0, 128.0),
            address = "서울",
            noAddressFallback = "주소를 찾을 수 없습니다",
            radius = "150",
            height = "50",
            memo = "memo",
            selectedColor = "#654321",
            selectedDroneId = null,
            flightStartDate = 3_000L,
            flightEndDate = 4_000L,
            now = 5_000L,
        )

        assertEquals("duplicate-shape", saved.id)
        assertEquals(5_000L, saved.createdAt)
        assertEquals(5_000L, saved.updatedAt)
        assertEquals("original-shape", original.id)
    }

    @Test
    fun `좌표 입력 주소 검색 성공 시 검색된 주소를 저장한다`() {
        assertEquals(
            "서울특별시 강남구",
            resolveCoordinateAddressForSave(
                resolvedAddress = "서울특별시 강남구",
                fallbackAddress = "주소를 찾을 수 없습니다",
            ),
        )
    }

    @Test
    fun `좌표 입력 주소 검색 실패 시 iOS fallback 주소를 저장한다`() {
        assertEquals(
            "주소를 찾을 수 없습니다",
            resolveCoordinateAddressForSave(
                resolvedAddress = "",
                fallbackAddress = "주소를 찾을 수 없습니다",
            ),
        )
    }

    @Test
    fun `좌표 입력 주소 저장은 iOS처럼 공백 주소를 값으로 보존한다`() {
        assertEquals(
            "   ",
            resolveCoordinateAddressForSave(
                resolvedAddress = "   ",
                fallbackAddress = "주소를 찾을 수 없습니다",
            ),
        )
    }

    @Test
    fun `도형 저장 검증은 iOS처럼 좌표와 주소 누락을 먼저 확인한다`() {
        assertEquals(
            ShapeEditSaveValidationError.COORDINATE_REQUIRED,
            validateShapeEditSaveFields(
                coordinate = null,
                address = "",
                radius = "",
            ),
        )
    }

    @Test
    fun `도형 저장 검증은 iOS처럼 반경 누락을 좌표 guard보다 먼저 확인한다`() {
        assertEquals(
            ShapeEditSaveValidationError.RADIUS_REQUIRED,
            validateShapeEditSaveFields(
                coordinate = null,
                address = "서울특별시 강남구",
                radius = "",
            ),
        )
    }

    @Test
    fun `도형 저장 검증은 주소만 있고 좌표가 없으면 좌표 누락을 표시한다`() {
        assertEquals(
            ShapeEditSaveValidationError.NO_COORDINATE,
            validateShapeEditSaveFields(
                coordinate = null,
                address = "서울특별시 강남구",
                radius = "100",
            ),
        )
    }

    @Test
    fun `도형 저장 검증은 좌표와 반경이 있으면 저장 가능하다`() {
        assertNull(
            validateShapeEditSaveFields(
                coordinate = Coordinate(37.0, 127.0),
                address = "",
                radius = "100",
            ),
        )
    }

    @Test
    fun `도형 편집 취소 경고는 iOS처럼 날짜 변경만으로는 표시하지 않는다`() {
        val hasChanges = hasShapeEditContentChanges(
            title = "Area",
            initialTitle = "Area",
            address = "Seoul",
            initialAddress = "Seoul",
            radius = "100",
            initialRadius = "100",
            height = "50",
            initialHeight = "50",
            memo = "memo",
            initialMemo = "memo",
            coordinate = Coordinate(37.0, 127.0),
            initialCoordinate = Coordinate(37.0, 127.0),
            selectedDroneId = "drone-a",
            initialDroneId = "drone-a",
        )

        assertFalse(hasChanges)
    }

    @Test
    fun `도형 편집 취소 경고는 iOS처럼 내용 변경은 감지한다`() {
        val hasChanges = hasShapeEditContentChanges(
            title = "Area",
            initialTitle = "Area",
            address = "Seoul",
            initialAddress = "Seoul",
            radius = "120",
            initialRadius = "100",
            height = "50",
            initialHeight = "50",
            memo = "memo",
            initialMemo = "memo",
            coordinate = Coordinate(37.0, 127.0),
            initialCoordinate = Coordinate(37.0, 127.0),
            selectedDroneId = "drone-a",
            initialDroneId = "drone-a",
        )

        assertTrue(hasChanges)
    }

    @Test
    fun `종료일 날짜 선택은 iOS처럼 시작일 이전 값을 허용하지 않는다`() {
        val start = localMillis(year = 2026, month = Calendar.JUNE, day = 3, hour = 14, minute = 30)
        val proposedEnd = localMillis(year = 2026, month = Calendar.JUNE, day = 2, hour = 9, minute = 0)

        val coerced = coerceShapeEditEndDateAtOrAfterStart(
            startDate = start,
            proposedEndDate = proposedEnd,
            isDateOnly = false,
        )

        assertEquals(start, coerced)
    }

    @Test
    fun `종료일 일 단위 선택은 iOS처럼 시작일 이전이면 시작일 23시 59분으로 보정한다`() {
        val start = localMillis(year = 2026, month = Calendar.JUNE, day = 3, hour = 14, minute = 30)
        val proposedEnd = localMillis(year = 2026, month = Calendar.JUNE, day = 2, hour = 23, minute = 59)

        val coerced = coerceShapeEditEndDateAtOrAfterStart(
            startDate = start,
            proposedEndDate = proposedEnd,
            isDateOnly = true,
        )

        val calendar = Calendar.getInstance().apply { timeInMillis = coerced }
        assertEquals(2026, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, calendar.get(Calendar.MONTH))
        assertEquals(3, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
        assertEquals(0, calendar.get(Calendar.MILLISECOND))
    }

    @Test
    fun `날짜 전용 종료일은 iOS처럼 23시 59분 0초로 저장한다`() {
        val date = localMillis(year = 2026, month = Calendar.JUNE, day = 3, hour = 9, minute = 15)

        val endOfDay = endOfShapeEditLocalDay(date)

        val calendar = Calendar.getInstance().apply { timeInMillis = endOfDay }
        assertEquals(2026, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, calendar.get(Calendar.MONTH))
        assertEquals(3, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
        assertEquals(0, calendar.get(Calendar.MILLISECOND))
    }

    @Test
    fun `DatePicker 초기값은 iOS처럼 로컬 날짜를 유지한다`() {
        val localDate = localMillis(year = 2026, month = Calendar.JUNE, day = 3, hour = 0, minute = 0)
        val pickerDate = shapeEditDatePickerMillisFromLocalMillis(localDate)
        val selectedDate = shapeEditLocalMillisFromDatePicker(
            dateMillis = pickerDate,
            hour = 0,
            minute = 0,
        )

        Calendar.getInstance().apply {
            timeInMillis = selectedDate
            assertEquals(2026, get(Calendar.YEAR))
            assertEquals(Calendar.JUNE, get(Calendar.MONTH))
            assertEquals(3, get(Calendar.DAY_OF_MONTH))
            assertEquals(0, get(Calendar.HOUR_OF_DAY))
            assertEquals(0, get(Calendar.MINUTE))
        }
    }

    @Test
    fun `단일 날짜 시간 선택 결과는 시작 종료 규칙에 맞게 저장된다`() {
        val selected = localMillis(year = 2026, month = Calendar.JUNE, day = 3, hour = 9, minute = 15)

        assertEquals(
            selected,
            selectedStartShapeEditDate(selectedDate = selected, isDateOnly = false),
        )
        assertEquals(
            startOfShapeEditLocalDay(selected),
            selectedStartShapeEditDate(selectedDate = selected, isDateOnly = true),
        )
        assertEquals(
            selected,
            selectedEndShapeEditDate(selectedDate = selected, isDateOnly = false),
        )
        assertEquals(
            endOfShapeEditLocalDay(selected),
            selectedEndShapeEditDate(selectedDate = selected, isDateOnly = true),
        )
    }

    @Test
    fun `일 단위 모드 켜기는 iOS처럼 시작 종료 시각을 일 경계로 즉시 정규화한다`() {
        val start = localMillis(year = 2026, month = Calendar.JUNE, day = 3, hour = 14, minute = 30)
        val end = localMillis(year = 2026, month = Calendar.JUNE, day = 4, hour = 9, minute = 15)

        val period = resolveShapeEditFlightPeriodOnDateOnlyModeToggle(
            startDate = start,
            endDate = end,
            isDateOnly = true,
        )

        Calendar.getInstance().apply {
            timeInMillis = period.startDate
            assertEquals(2026, get(Calendar.YEAR))
            assertEquals(Calendar.JUNE, get(Calendar.MONTH))
            assertEquals(3, get(Calendar.DAY_OF_MONTH))
            assertEquals(0, get(Calendar.HOUR_OF_DAY))
            assertEquals(0, get(Calendar.MINUTE))
            assertEquals(0, get(Calendar.SECOND))
            assertEquals(0, get(Calendar.MILLISECOND))
        }
        Calendar.getInstance().apply {
            timeInMillis = period.endDate ?: error("endDate should be preserved")
            assertEquals(2026, get(Calendar.YEAR))
            assertEquals(Calendar.JUNE, get(Calendar.MONTH))
            assertEquals(4, get(Calendar.DAY_OF_MONTH))
            assertEquals(23, get(Calendar.HOUR_OF_DAY))
            assertEquals(59, get(Calendar.MINUTE))
            assertEquals(0, get(Calendar.SECOND))
            assertEquals(0, get(Calendar.MILLISECOND))
        }
    }

    @Test
    fun `일 단위 모드 켜기는 종료일이 없던 상태도 iOS처럼 시작일 종료 시각으로 보정한다`() {
        val start = localMillis(year = 2026, month = Calendar.JUNE, day = 3, hour = 14, minute = 30)

        val period = resolveShapeEditFlightPeriodOnDateOnlyModeToggle(
            startDate = start,
            endDate = null,
            isDateOnly = true,
        )

        Calendar.getInstance().apply {
            timeInMillis = period.startDate
            assertEquals(2026, get(Calendar.YEAR))
            assertEquals(Calendar.JUNE, get(Calendar.MONTH))
            assertEquals(3, get(Calendar.DAY_OF_MONTH))
            assertEquals(0, get(Calendar.HOUR_OF_DAY))
            assertEquals(0, get(Calendar.MINUTE))
            assertEquals(0, get(Calendar.SECOND))
            assertEquals(0, get(Calendar.MILLISECOND))
        }
        Calendar.getInstance().apply {
            timeInMillis = period.endDate ?: error("endDate should be synthesized")
            assertEquals(2026, get(Calendar.YEAR))
            assertEquals(Calendar.JUNE, get(Calendar.MONTH))
            assertEquals(3, get(Calendar.DAY_OF_MONTH))
            assertEquals(23, get(Calendar.HOUR_OF_DAY))
            assertEquals(59, get(Calendar.MINUTE))
            assertEquals(0, get(Calendar.SECOND))
            assertEquals(0, get(Calendar.MILLISECOND))
        }
    }

    @Test
    fun `일 단위 모드 끄기는 iOS처럼 기존 시작 종료 시각을 유지한다`() {
        val start = localMillis(year = 2026, month = Calendar.JUNE, day = 3, hour = 14, minute = 30)
        val end = localMillis(year = 2026, month = Calendar.JUNE, day = 4, hour = 9, minute = 15)

        val period = resolveShapeEditFlightPeriodOnDateOnlyModeToggle(
            startDate = start,
            endDate = end,
            isDateOnly = false,
        )

        assertEquals(start, period.startDate)
        assertEquals(end, period.endDate)
    }

    @Test
    fun `좌표 입력 안내 예시는 iOS CoordinateView 순서를 유지한다`() {
        assertEquals(
            listOf(
                R.string.coordinate_example_dms,
                R.string.coordinate_example_decimal_degrees,
                R.string.coordinate_example_simple_decimal,
                R.string.coordinate_example_geo_uri,
            ),
            coordinateGuideExampleResourceIds(),
        )
    }

    @Test
    fun `종료일이 시작일 이후이면 선택값을 그대로 유지한다`() {
        val start = localMillis(year = 2026, month = Calendar.JUNE, day = 3, hour = 14, minute = 30)
        val proposedEnd = localMillis(year = 2026, month = Calendar.JUNE, day = 4, hour = 9, minute = 0)

        val coerced = coerceShapeEditEndDateAtOrAfterStart(
            startDate = start,
            proposedEndDate = proposedEnd,
            isDateOnly = false,
        )

        assertEquals(proposedEnd, coerced)
    }

    @Test
    fun `도형 편집 충돌은 사용자가 바꾼 필드와 원격 변경 필드를 함께 보존한다`() {
        val original = baseConflictShape()
        val latest = original.copy(
            memo = "remote memo",
            address = "remote address",
            updatedAt = 200L,
        )
        val edited = original.copy(
            title = "edited title",
        )

        val resolved = resolveShapeEditConflict(
            editedShape = edited,
            originalShape = original,
            latestShape = latest,
        )

        assertEquals("edited title", resolved.title)
        assertEquals("remote memo", resolved.memo)
        assertEquals("remote address", resolved.address)
    }

    @Test
    fun `도형 편집 충돌은 원격 soft delete 를 보존해 삭제 도형을 되살리지 않는다`() {
        val original = baseConflictShape()
        val latest = original.copy(
            deletedAt = 300L,
            updatedAt = 300L,
        )
        val edited = original.copy(
            title = "edited title",
        )

        val resolved = resolveShapeEditConflict(
            editedShape = edited,
            originalShape = original,
            latestShape = latest,
        )

        assertEquals("edited title", resolved.title)
        assertEquals(300L, resolved.deletedAt)
    }

    @Test
    fun `도형 편집 충돌은 사용자가 기하 데이터를 바꾸지 않았으면 원격 기하 데이터를 적용한다`() {
        val original = baseConflictShape()
        val latest = original.copy(
            shapeType = ShapeType.RECTANGLE,
            baseCoordinate = Coordinate(35.0, 129.0),
            radius = null,
            secondCoordinate = Coordinate(35.1, 129.1),
        )
        val edited = original.copy(
            memo = "edited memo",
        )

        val resolved = resolveShapeEditConflict(
            editedShape = edited,
            originalShape = original,
            latestShape = latest,
        )

        assertEquals("edited memo", resolved.memo)
        assertEquals(ShapeType.RECTANGLE, resolved.shapeType)
        assertEquals(Coordinate(35.0, 129.0), resolved.baseCoordinate)
        assertNull(resolved.radius)
        assertEquals(Coordinate(35.1, 129.1), resolved.secondCoordinate)
    }

    @Test
    fun `도형 편집 충돌은 사용자가 기하 데이터를 바꿨으면 편집 값을 우선한다`() {
        val original = baseConflictShape()
        val latest = original.copy(
            baseCoordinate = Coordinate(35.0, 129.0),
            radius = 500.0,
        )
        val edited = original.copy(
            baseCoordinate = Coordinate(36.0, 128.0),
            radius = 250.0,
        )

        val resolved = resolveShapeEditConflict(
            editedShape = edited,
            originalShape = original,
            latestShape = latest,
        )

        assertEquals(Coordinate(36.0, 128.0), resolved.baseCoordinate)
        assertEquals(250.0, resolved.radius)
    }

    @Test
    fun `도형 편집 충돌은 드론 미편집 시 원격 드론과 색상을 적용한다`() {
        val original = baseConflictShape()
        val latest = original.copy(
            droneId = "drone-remote",
            color = "#222222",
        )
        val edited = original.copy(
            title = "edited title",
            color = "#111111",
        )

        val resolved = resolveShapeEditConflict(
            editedShape = edited,
            originalShape = original,
            latestShape = latest,
        )

        assertEquals("drone-remote", resolved.droneId)
        assertEquals("#222222", resolved.color)
    }

    @Test
    fun `도형 편집 충돌은 iOS처럼 드론 변경 없는 원격 색상만으로는 편집 색상을 바꾸지 않는다`() {
        val original = baseConflictShape()
        val latest = original.copy(
            color = "#222222",
        )
        val edited = original.copy(
            title = "edited title",
            color = "#111111",
        )

        val resolved = resolveShapeEditConflict(
            editedShape = edited,
            originalShape = original,
            latestShape = latest,
        )

        assertEquals("drone-original", resolved.droneId)
        assertEquals("#111111", resolved.color)
    }

    @Test
    fun `도형 편집 충돌은 드론을 직접 바꿨으면 편집 드론과 색상을 우선한다`() {
        val original = baseConflictShape()
        val latest = original.copy(
            droneId = "drone-remote",
            color = "#222222",
        )
        val edited = original.copy(
            droneId = "drone-edited",
            color = "#333333",
        )

        val resolved = resolveShapeEditConflict(
            editedShape = edited,
            originalShape = original,
            latestShape = latest,
        )

        assertEquals("drone-edited", resolved.droneId)
        assertEquals("#333333", resolved.color)
    }

    @Test
    fun `도형 편집 충돌은 신규 또는 복제 저장이면 편집 값을 그대로 사용한다`() {
        val original = baseConflictShape()
        val edited = original.copy(
            id = "duplicate-shape",
            title = "duplicate title",
        )
        val latest = original.copy(title = "remote title")

        assertEquals(
            edited,
            resolveShapeEditConflict(
                editedShape = edited,
                originalShape = original,
                latestShape = latest,
            ),
        )
        assertEquals(
            edited,
            resolveShapeEditConflict(
                editedShape = edited,
                originalShape = null,
                latestShape = latest,
            ),
        )
        assertEquals(
            edited,
            resolveShapeEditConflict(
                editedShape = edited,
                originalShape = original,
                latestShape = null,
            ),
        )
    }

    private fun baseConflictShape(): ShapeModel {
        return ShapeModel(
            id = "shape-conflict",
            title = "original title",
            shapeType = ShapeType.CIRCLE,
            baseCoordinate = Coordinate(37.0, 127.0),
            address = "original address",
            radius = 100.0,
            height = 50.0,
            memo = "original memo",
            color = "#111111",
            droneId = "drone-original",
            createdAt = 1L,
            flightStartDate = 10L,
            flightEndDate = 20L,
            updatedAt = 100L,
        )
    }

    private fun localMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

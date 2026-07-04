package com.ScienceFiction.DronePassAndroid.feature.saved

import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SavedListSectionsTest {

    @Test
    fun `저장 목록 섹션 헤더 최소 높이는 iOS defaultMinListHeaderHeight 40과 맞춘다`() {
        assertEquals(40.dp, SavedListSectionHeaderMinHeight)
    }

    @Test
    fun `저장 목록 행과 섹션 간격은 iOS List section spacing 을 따른다`() {
        assertEquals((-10).dp, SavedListTopOffset)
        assertEquals(16.dp, SavedListContentHorizontalPadding)
        assertEquals(8.dp, SavedListContentVerticalPadding)
        assertEquals(0.dp, SavedListRowVerticalSpacing)
        assertEquals(8.dp, SavedListSectionSpacing)
    }

    @Test
    fun `저장 목록은 iOS처럼 시작 전 도형 숨김 설정을 섹션에 반영한다`() {
        val now = System.currentTimeMillis()
        val sections = buildSavedShapeSections(
            shapes = listOf(
                shape(id = "active", start = now - 1_000, end = now + 10_000),
                shape(id = "not-started", start = now + 10_000, end = now + 20_000),
            ),
            activeDrones = listOf(DroneModel(id = "drone-a", name = "A")),
            selectedDroneIds = setOf("drone-a"),
            sortOption = SortOption.FLIGHT_START,
            sortDirection = SortDirection.ASCENDING,
            visibilitySettings = SavedShapeVisibilitySettings(hideNotStarted = true),
        )

        assertEquals(listOf("active"), sections.activeFiltered.map { it.id })
        assertEquals(emptyList<String>(), sections.notStarted.map { it.id })
        assertEquals(1, sections.total)
    }

    @Test
    fun `저장 목록은 iOS처럼 만료 도형 숨김 설정을 섹션에 반영한다`() {
        val now = System.currentTimeMillis()
        val sections = buildSavedShapeSections(
            shapes = listOf(
                shape(id = "active", start = now - 20_000, end = now + 10_000),
                shape(id = "expired", start = now - 20_000, end = now - 10_000),
            ),
            activeDrones = listOf(DroneModel(id = "drone-a", name = "A")),
            selectedDroneIds = setOf("drone-a"),
            sortOption = SortOption.FLIGHT_START,
            sortDirection = SortDirection.ASCENDING,
            visibilitySettings = SavedShapeVisibilitySettings(hideExpired = true),
        )

        assertEquals(listOf("active"), sections.activeFiltered.map { it.id })
        assertEquals(emptyList<String>(), sections.expired.map { it.id })
        assertEquals(1, sections.total)
    }

    @Test
    fun `저장 목록 종료일순은 iOS처럼 전체 정렬 후 섹션을 분리한다`() {
        val now = 10_000L
        val shapes = listOf(
            shape(id = "active-later", start = now - 5_000, end = now + 4_000),
            shape(id = "expired", start = now - 5_000, end = now - 1_000),
            shape(id = "not-started", start = now + 1_000, end = now + 2_000),
            shape(id = "active-soon", start = now - 5_000, end = now + 1_000),
        )
        val expected = buildSavedShapeSectionsFromGloballySortedShapes(
            shapes = shapes,
            sortOption = SortOption.FLIGHT_END,
            sortDirection = SortDirection.ASCENDING,
            visibilitySettings = SavedShapeVisibilitySettings(),
            now = now,
        )

        val actual = buildSavedShapeSections(
            shapes = shapes,
            activeDrones = listOf(DroneModel(id = "drone-a", name = "A")),
            selectedDroneIds = setOf("drone-a"),
            sortOption = SortOption.FLIGHT_END,
            sortDirection = SortDirection.ASCENDING,
            visibilitySettings = SavedShapeVisibilitySettings(),
            now = now,
        )

        assertEquals(expected, actual)
        assertEquals(listOf("active-soon", "active-later"), actual.activeFiltered.map { it.id })
        assertEquals(listOf("not-started"), actual.notStarted.map { it.id })
        assertEquals(listOf("expired"), actual.expired.map { it.id })
    }

    @Test
    fun `저장 목록 종료일순 외 정렬은 iOS처럼 섹션 분리 후 각 섹션을 정렬한다`() {
        val now = 10_000L
        val shapes = listOf(
            shape(id = "active-b", title = "B", address = null, start = now - 2_000, end = now + 4_000),
            shape(id = "not-started-a", title = "A", address = null, start = now + 2_000, end = now + 5_000),
            shape(id = "active-a", title = "A", address = null, start = now - 1_000, end = now + 3_000),
            shape(id = "expired-c", title = "C", address = null, start = now - 5_000, end = now - 1_000),
        )
        val expected = buildSavedShapeSectionsFromIndividuallySortedSections(
            shapes = shapes,
            sortOption = SortOption.TITLE,
            sortDirection = SortDirection.ASCENDING,
            visibilitySettings = SavedShapeVisibilitySettings(),
            now = now,
        )

        val actual = buildSavedShapeSections(
            shapes = shapes,
            activeDrones = listOf(DroneModel(id = "drone-a", name = "A")),
            selectedDroneIds = setOf("drone-a"),
            sortOption = SortOption.TITLE,
            sortDirection = SortDirection.ASCENDING,
            visibilitySettings = SavedShapeVisibilitySettings(),
            now = now,
        )

        assertEquals(expected, actual)
        assertEquals(listOf("active-a", "active-b"), actual.activeFiltered.map { it.id })
        assertEquals(listOf("not-started-a"), actual.notStarted.map { it.id })
        assertEquals(listOf("expired-c"), actual.expired.map { it.id })
    }

    @Test
    fun `저장 목록 포커스 후보는 iOS처럼 선택된 드론 도형만 포함한다`() {
        val sections = buildSavedShapeSections(
            shapes = listOf(
                shape(id = "selected-drone-shape", start = 1_000, end = 3_000, droneId = "drone-a"),
                shape(id = "hidden-drone-shape", start = 1_000, end = 3_000, droneId = "drone-b"),
            ),
            activeDrones = listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
            ),
            selectedDroneIds = setOf("drone-a"),
            sortOption = SortOption.FLIGHT_START,
            sortDirection = SortDirection.ASCENDING,
            visibilitySettings = SavedShapeVisibilitySettings(),
            now = 2_000,
        )

        assertEquals(listOf("selected-drone-shape"), sections.activeFiltered.map { it.id })
        assertNull(
            findLazyListIndex(
                shapeId = "hidden-drone-shape",
                activeShapes = sections.activeFiltered,
                notStartedShapes = sections.notStarted,
                expiredShapes = sections.expired,
            ),
        )
    }

    @Test
    fun `저장 목록은 iOS처럼 droneId 없는 레거시 도형을 첫 번째 활성 드론 도형으로 표시한다`() {
        val sections = buildSavedShapeSections(
            shapes = listOf(
                shape(id = "shape-a", start = 1_000, end = 3_000, droneId = "drone-a"),
                shape(id = "shape-b", start = 1_000, end = 3_000, droneId = "drone-b"),
                shape(id = "shape-legacy", start = 1_000, end = 3_000, droneId = null),
            ),
            activeDrones = listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
            ),
            selectedDroneIds = setOf("drone-a"),
            sortOption = SortOption.FLIGHT_START,
            sortDirection = SortDirection.ASCENDING,
            visibilitySettings = SavedShapeVisibilitySettings(),
            now = 2_000,
        )

        assertEquals(listOf("shape-a", "shape-legacy"), sections.activeFiltered.map { it.id })
    }

    @Test
    fun `저장 목록은 iOS처럼 선택된 드론이 없으면 도형이 있어도 빈 상태다`() {
        val sections = buildSavedShapeSections(
            shapes = listOf(
                shape(id = "shape-a", start = 1_000, end = 3_000, droneId = "drone-a"),
                shape(id = "shape-legacy", start = 1_000, end = 3_000, droneId = null),
            ),
            activeDrones = listOf(DroneModel(id = "drone-a", name = "A")),
            selectedDroneIds = emptySet(),
            sortOption = SortOption.FLIGHT_START,
            sortDirection = SortDirection.ASCENDING,
            visibilitySettings = SavedShapeVisibilitySettings(),
            now = 2_000,
        )

        assertEquals(emptyList<String>(), sections.notStarted.map { it.id })
        assertEquals(emptyList<String>(), sections.activeFiltered.map { it.id })
        assertEquals(emptyList<String>(), sections.expired.map { it.id })
        assertEquals(0, sections.total)
    }

    @Test
    fun `저장 목록은 iOS SavedTableListView처럼 종료 시각과 현재가 같으면 만료 섹션으로 분류한다`() {
        val now = 10_000L
        val sections = buildSavedShapeSections(
            shapes = listOf(
                shape(id = "ended-now", start = now - 1_000, end = now),
                shape(id = "active", start = now - 1_000, end = now + 1_000),
            ),
            activeDrones = listOf(DroneModel(id = "drone-a", name = "A")),
            selectedDroneIds = setOf("drone-a"),
            sortOption = SortOption.FLIGHT_END,
            sortDirection = SortDirection.ASCENDING,
            visibilitySettings = SavedShapeVisibilitySettings(),
            now = now,
        )

        assertEquals(listOf("active"), sections.activeFiltered.map { it.id })
        assertEquals(listOf("ended-now"), sections.expired.map { it.id })
        assertEquals(2, sections.total)
        assertEquals(true, isSavedListExpired(shape(id = "ended-now", start = now - 1_000, end = now), now))
        assertEquals(true, isSavedListExpired(shape(id = "expired", start = now - 1_000, end = now - 1), now))
    }

    @Test
    fun `저장 목록 제목순은 iOS처럼 같은 제목이면 시작일과 주소를 보조 정렬한다`() {
        val now = System.currentTimeMillis()
        val sorted = sortSavedShapes(
            shapes = listOf(
                shape(id = "later", title = "Area", address = "B", start = now + 20_000, end = now + 30_000),
                shape(id = "same-date-address-b", title = "Area", address = "B", start = now + 10_000, end = now + 30_000),
                shape(id = "same-date-address-a", title = "Area", address = "A", start = now + 10_000, end = now + 30_000),
            ),
            option = SortOption.TITLE,
            direction = SortDirection.ASCENDING,
        )

        assertEquals(
            listOf("same-date-address-a", "same-date-address-b", "later"),
            sorted.map { it.id },
        )
    }

    @Test
    fun `저장 목록 제목순 내림차순은 iOS처럼 보조 시작일과 주소도 내림차순으로 정렬한다`() {
        val now = System.currentTimeMillis()
        val sorted = sortSavedShapes(
            shapes = listOf(
                shape(id = "same-date-address-a", title = "Area", address = "A", start = now + 10_000, end = now + 30_000),
                shape(id = "same-date-address-b", title = "Area", address = "B", start = now + 10_000, end = now + 30_000),
                shape(id = "later", title = "Area", address = "B", start = now + 20_000, end = now + 30_000),
            ),
            option = SortOption.TITLE,
            direction = SortDirection.DESCENDING,
        )

        assertEquals(
            listOf("later", "same-date-address-b", "same-date-address-a"),
            sorted.map { it.id },
        )
    }

    @Test
    fun `저장 목록 종료일순은 iOS처럼 종료일 없는 도형을 먼 미래로 정렬한다`() {
        val now = System.currentTimeMillis()
        val sorted = sortSavedShapes(
            shapes = listOf(
                shape(id = "no-end", title = "No End", address = null, start = now, end = null),
                shape(id = "later", title = "Later", address = null, start = now, end = now + 20_000),
                shape(id = "soon", title = "Soon", address = null, start = now, end = now + 10_000),
            ),
            option = SortOption.FLIGHT_END,
            direction = SortDirection.ASCENDING,
        )

        assertEquals(
            listOf("soon", "later", "no-end"),
            sorted.map { it.id },
        )
    }

    @Test
    fun `저장 목록 종료일순 내림차순은 iOS Date distantFuture처럼 종료일 없는 도형을 먼저 정렬한다`() {
        val now = System.currentTimeMillis()
        val sorted = sortSavedShapes(
            shapes = listOf(
                shape(id = "soon", title = "Soon", address = null, start = now, end = now + 10_000),
                shape(id = "no-end", title = "No End", address = null, start = now, end = null),
                shape(id = "later", title = "Later", address = null, start = now, end = now + 20_000),
            ),
            option = SortOption.FLIGHT_END,
            direction = SortDirection.DESCENDING,
        )

        assertEquals(
            listOf("no-end", "later", "soon"),
            sorted.map { it.id },
        )
    }

    @Test
    fun `저장 목록 날짜순 내림차순은 iOS처럼 제목과 주소 보조 정렬도 같은 방향을 따른다`() {
        val now = System.currentTimeMillis()
        val sorted = sortSavedShapes(
            shapes = listOf(
                shape(id = "title-a-address-a", title = "Area", address = "A", start = now + 10_000, end = now + 30_000),
                shape(id = "title-a-address-b", title = "Area", address = "B", start = now + 10_000, end = now + 30_000),
                shape(id = "title-b", title = "Zone", address = "A", start = now + 10_000, end = now + 30_000),
            ),
            option = SortOption.FLIGHT_START,
            direction = SortDirection.DESCENDING,
        )

        assertEquals(
            listOf("title-b", "title-a-address-b", "title-a-address-a"),
            sorted.map { it.id },
        )
    }

    @Test
    fun `저장 목록 정렬 rawValue 기본값은 iOS처럼 제목순과 오름차순이다`() {
        assertEquals(SortOption.TITLE, SortOption.fromRawValue(null))
        assertEquals(SortDirection.ASCENDING, SortDirection.fromRawValue(null))
        assertEquals(SortOption.DATE_CREATED, SortOption.fromRawValue("dateCreated"))
        assertEquals(SortDirection.DESCENDING, SortDirection.fromRawValue("descending"))
    }

    @Test
    fun `저장 목록 정렬 옵션 순환은 iOS ShapeSortingManager 순서를 따른다`() {
        assertEquals(SortOption.DATE_CREATED, nextSavedSortOption(SortOption.TITLE))
        assertEquals(SortOption.FLIGHT_START, nextSavedSortOption(SortOption.DATE_CREATED))
        assertEquals(SortOption.FLIGHT_END, nextSavedSortOption(SortOption.FLIGHT_START))
        assertEquals(SortOption.TITLE, nextSavedSortOption(SortOption.FLIGHT_END))
    }

    @Test
    fun `저장 목록 정렬 rawValue 순서는 iOS SortOption rawValue 와 같다`() {
        assertEquals(
            listOf("title", "dateCreated", "flightStartDate", "flightEndDate"),
            SortOption.entries.map { it.rawValue },
        )
    }

    @Test
    fun `저장 목록 스크롤 인덱스는 iOS처럼 시작 전 활성 만료 순서로 계산한다`() {
        val active = listOf(shape(id = "active", start = 1_000, end = 3_000))
        val notStarted = listOf(shape(id = "not-started", start = 4_000, end = 5_000))
        val expired = listOf(shape(id = "expired", start = 1_000, end = 2_000))

        assertEquals(
            1,
            findLazyListIndex(
                shapeId = "not-started",
                activeShapes = active,
                notStartedShapes = notStarted,
                expiredShapes = expired,
            ),
        )
        assertEquals(
            3,
            findLazyListIndex(
                shapeId = "active",
                activeShapes = active,
                notStartedShapes = notStarted,
                expiredShapes = expired,
            ),
        )
        assertEquals(
            5,
            findLazyListIndex(
                shapeId = "expired",
                activeShapes = active,
                notStartedShapes = notStarted,
                expiredShapes = expired,
            ),
        )
    }

    @Test
    fun `저장 목록 스크롤 대상이 현재 섹션에 없으면 iOS처럼 선택하지 않는다`() {
        assertNull(
            findLazyListIndex(
                shapeId = "hidden",
                activeShapes = listOf(shape(id = "active", start = 1_000, end = 3_000)),
                notStartedShapes = emptyList(),
                expiredShapes = emptyList(),
            )
        )
    }

    @Test
    fun `저장 목록 포커스는 도형 목록이 아직 로드되지 않아 섹션이 비어 있으면 소비하지 않는다`() {
        assertEquals(
            false,
            shouldConsumeMissingFocus(
                shapeId = "target",
                allShapeCount = 0,
                activeShapeIds = emptySet(),
                activeShapes = emptyList(),
                notStartedShapes = emptyList(),
                expiredShapes = emptyList(),
            ),
        )
    }

    @Test
    fun `저장 목록 포커스는 표시 섹션이 준비된 뒤 대상이 없을 때만 소비한다`() {
        assertEquals(
            true,
            shouldConsumeMissingFocus(
                shapeId = "target",
                allShapeCount = 1,
                activeShapeIds = setOf("target"),
                activeShapes = listOf(shape(id = "other", start = 1_000, end = 3_000)),
                notStartedShapes = emptyList(),
                expiredShapes = emptyList(),
            ),
        )
    }

    @Test
    fun `저장 목록 포커스 대상은 외부 요청을 내부 복제 저장 요청보다 우선한다`() {
        assertEquals(
            "external",
            resolveSavedListFocusTarget(
                externalFocusShapeId = "external",
                internalFocusShapeId = "internal",
            ),
        )
        assertEquals(
            "internal",
            resolveSavedListFocusTarget(
                externalFocusShapeId = null,
                internalFocusShapeId = "internal",
            ),
        )
        assertNull(
            resolveSavedListFocusTarget(
                externalFocusShapeId = null,
                internalFocusShapeId = null,
            ),
        )
    }

    @Test
    fun `저장 목록 내부 포커스는 외부 포커스 소비 없이 자체 요청만 지운다`() {
        assertEquals(
            SavedListFocusConsumption(
                consumeExternalFocus = false,
                clearInternalFocus = true,
            ),
            resolveSavedListFocusConsumption(
                externalFocusShapeId = null,
                internalFocusShapeId = "duplicate",
                targetShapeId = "duplicate",
            ),
        )
    }

    @Test
    fun `저장 목록 외부 포커스는 부모 요청을 소비하고 같은 내부 요청도 함께 지운다`() {
        assertEquals(
            SavedListFocusConsumption(
                consumeExternalFocus = true,
                clearInternalFocus = true,
            ),
            resolveSavedListFocusConsumption(
                externalFocusShapeId = "target",
                internalFocusShapeId = "target",
                targetShapeId = "target",
            ),
        )
    }

    @Test
    fun `저장 목록 포커스는 도형 목록이 아직 로드되지 않았으면 소비하지 않는다`() {
        assertEquals(
            false,
            shouldConsumeMissingFocus(
                shapeId = "target",
                allShapeCount = 0,
                activeShapeIds = emptySet(),
                activeShapes = emptyList(),
                notStartedShapes = emptyList(),
                expiredShapes = emptyList(),
            ),
        )
    }

    @Test
    fun `저장 목록 포커스는 대상 도형이 필터로 숨겨져도 iOS처럼 요청을 소비한다`() {
        assertEquals(
            true,
            shouldConsumeMissingFocus(
                shapeId = "target",
                allShapeCount = 1,
                activeShapeIds = setOf("target"),
                activeShapes = emptyList(),
                notStartedShapes = emptyList(),
                expiredShapes = emptyList(),
            ),
        )
    }

    @Test
    fun `저장 목록 포커스는 로드된 목록에 대상이 없으면 iOS처럼 요청을 소비한다`() {
        assertEquals(
            true,
            shouldConsumeMissingFocus(
                shapeId = "missing",
                allShapeCount = 1,
                activeShapeIds = setOf("other"),
                activeShapes = emptyList(),
                notStartedShapes = emptyList(),
                expiredShapes = emptyList(),
            ),
        )
    }

    @Test
    fun `저장 목록 상세 편집 저장은 iOS처럼 상세 화면으로 돌아간다`() {
        assertEquals(
            SavedDetailEditPostSaveAction.RETURN_TO_DETAIL,
            resolveSavedDetailEditPostSaveAction(isDuplicate = false),
        )
    }

    @Test
    fun `저장 목록 상세 복제 저장은 iOS처럼 새 도형을 저장 목록과 지도에 포커스한다`() {
        assertEquals(
            SavedDetailEditPostSaveAction.FOCUS_SAVED_LIST,
            resolveSavedDetailEditPostSaveAction(isDuplicate = true),
        )
    }

    @Test
    fun `저장 목록 스와이프 삭제는 iOS처럼 선택과 상세 표시를 유지한다`() {
        assertEquals(
            SavedShapeDeletePresentationUpdate(dismissDetailAndClearSelection = false),
            resolveSavedShapeDeletePresentationUpdate(SavedShapeDeleteSource.LIST),
        )
    }

    @Test
    fun `저장 목록 상세 시트 삭제는 상세와 선택 상태를 정리한다`() {
        assertEquals(
            SavedShapeDeletePresentationUpdate(dismissDetailAndClearSelection = true),
            resolveSavedShapeDeletePresentationUpdate(SavedShapeDeleteSource.DETAIL),
        )
    }

    @Test
    fun `저장 목록 스와이프 삭제는 iOS처럼 오른쪽에서 왼쪽 방향만 삭제한다`() {
        assertEquals(true, shouldDeleteSavedShapeOnSwipe(SwipeToDismissBoxValue.EndToStart))
        assertEquals(false, shouldDeleteSavedShapeOnSwipe(SwipeToDismissBoxValue.StartToEnd))
        assertEquals(false, shouldDeleteSavedShapeOnSwipe(SwipeToDismissBoxValue.Settled))
    }

    @Test
    fun `저장 목록 삭제 배경은 스와이프 중일 때만 표시한다`() {
        assertEquals(true, shouldShowSavedShapeDeleteBackground(SwipeToDismissBoxValue.EndToStart))
        assertEquals(false, shouldShowSavedShapeDeleteBackground(SwipeToDismissBoxValue.StartToEnd))
        assertEquals(false, shouldShowSavedShapeDeleteBackground(SwipeToDismissBoxValue.Settled))
    }

    @Test
    fun `저장 목록 빈 상태는 iOS처럼 전체 없음 드론 미선택 필터 결과 없음 순서로 판단한다`() {
        assertEquals(
            SavedListEmptyState.NO_SHAPES,
            resolveSavedListEmptyState(hasShapes = false, selectedDroneCount = 0),
        )
        assertEquals(
            SavedListEmptyState.NO_DRONE_SELECTED,
            resolveSavedListEmptyState(hasShapes = true, selectedDroneCount = 0),
        )
        assertEquals(
            SavedListEmptyState.NO_MATCHING_SHAPES,
            resolveSavedListEmptyState(hasShapes = true, selectedDroneCount = 1),
        )
    }

    @Test
    fun `저장 목록 빈 상태 UI 토큰은 iOS EmptyStateView 값을 따른다`() {
        assertEquals(48.dp, SavedListEmptyIconSize)
        assertEquals(16.dp, SavedListEmptyVerticalSpacing)
        assertEquals(0xFF8E8E93.toInt(), SavedListEmptySecondaryColor.toArgb())
    }

    @Test
    fun `저장 목록 빈 상태 아이콘은 iOS tray drone magnifyingglass 분기를 따른다`() {
        assertEquals(
            SavedListEmptyIconStyle.INBOX,
            resolveSavedListEmptyIconStyle(SavedListEmptyState.NO_SHAPES),
        )
        assertEquals(
            SavedListEmptyIconStyle.DRONE,
            resolveSavedListEmptyIconStyle(SavedListEmptyState.NO_DRONE_SELECTED),
        )
        assertEquals(
            SavedListEmptyIconStyle.SEARCH,
            resolveSavedListEmptyIconStyle(SavedListEmptyState.NO_MATCHING_SHAPES),
        )
        assertEquals(R.drawable.ic_drone, SavedListEmptyDroneIconRes)
    }

    @Test
    fun `저장 목록 포커스 스크롤 타이밍은 iOS 지연 순서를 따른다`() {
        assertEquals(200L, SavedListFocusSelectionDelayMs)
        assertEquals(100L, SavedListFocusScrollDelayMs)
    }

    @Test
    fun `저장 목록 행 탭은 iOS처럼 선택 표시 후 지도 포커스를 요청한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedListScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedListScreen.kt",
        ).readText()

        assertEquals(
            3,
            Regex(
                "viewModel\\.selectShapeForMapFocus\\(shape\\.id\\)\\s+" +
                    "onNavigateToMapWithShape\\(shape\\.id\\)",
            ).findAll(source).count(),
        )
        assertEquals(
            3,
            Regex("onDetailClick = \\{ viewModel\\.onShapeSelected\\(shape\\.id\\) }")
                .findAll(source)
                .count(),
        )
        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "viewModel.selectShapeForMapFocus(targetId)",
                "listState.animateScrollToItem(targetIndex)",
                "SavedShapeListItem(",
                "viewModel.selectShapeForMapFocus(shape.id)",
                "onNavigateToMapWithShape(shape.id)",
                "onDetailClick = { viewModel.onShapeSelected(shape.id) }",
            ),
        )
    }

    @Test
    fun `저장 목록 내부 포커스 이벤트는 iOS처럼 저장 목록 스크롤과 지도 포커스를 함께 요청한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedListScreen.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/feature/saved/SavedListScreen.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "viewModel.savedShapeFocusEvent.collect { shapeId ->",
                "internalFocusShapeId = shapeId",
                "onNavigateToMapWithShape(shapeId)",
            ),
        )
    }

    private fun shape(
        id: String,
        start: Long,
        end: Long?,
        droneId: String? = "drone-a",
    ): ShapeModel {
        return shape(
            id = id,
            title = id,
            address = null,
            start = start,
            end = end,
            droneId = droneId,
        )
    }

    private fun shape(
        id: String,
        title: String,
        address: String?,
        start: Long,
        end: Long?,
        droneId: String? = "drone-a",
    ): ShapeModel {
        return ShapeModel(
            id = id,
            title = title,
            address = address,
            baseCoordinate = Coordinate(37.0, 127.0),
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

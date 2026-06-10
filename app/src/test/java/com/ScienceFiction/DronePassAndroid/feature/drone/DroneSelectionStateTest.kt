package com.ScienceFiction.DronePassAndroid.feature.drone

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DroneSelectionStateTest {

    @Test
    fun `드론 다중 선택 저장 키 이름은 iOS UserDefaults 이름과 동일하게 유지한다`() {
        assertEquals("selectedDroneId", DroneSelectionPreferenceKeys.SELECTED_DRONE_ID.name)
        assertEquals("selectedDroneIds", DroneSelectionPreferenceKeys.SELECTED_DRONE_IDS.name)
        assertEquals("selected_drone_ids", DroneSelectionPreferenceKeys.LEGACY_SELECTED_DRONE_IDS.name)
    }

    @Test
    fun `저장된 단일 선택 드론은 iOS selectedDroneId 키에서 읽는다`() {
        val preferences = preferencesOf(
            DroneSelectionPreferenceKeys.SELECTED_DRONE_ID to "drone-primary",
            DroneSelectionPreferenceKeys.SELECTED_DRONE_IDS to setOf("drone-a", "drone-b"),
        )

        assertEquals(
            StoredDroneSelection(
                selectedDroneId = "drone-primary",
                selectedDroneIds = setOf("drone-a", "drone-b"),
            ),
            storedDroneSelection(preferences),
        )
    }

    @Test
    fun `드론 다중 선택은 기존 Android snake case 저장값을 fallback 으로 읽는다`() {
        val preferences = preferencesOf(
            DroneSelectionPreferenceKeys.LEGACY_SELECTED_DRONE_IDS to setOf("drone-a", "drone-b"),
        )

        assertEquals(setOf("drone-a", "drone-b"), storedDroneSelectedIds(preferences))
    }

    @Test
    fun `드론 다중 선택은 iOS primary 저장값이 있으면 legacy 값보다 우선한다`() {
        val preferences = preferencesOf(
            DroneSelectionPreferenceKeys.SELECTED_DRONE_IDS to setOf("primary-drone"),
            DroneSelectionPreferenceKeys.LEGACY_SELECTED_DRONE_IDS to setOf("legacy-drone"),
        )

        assertEquals(setOf("primary-drone"), storedDroneSelectedIds(preferences))
    }

    @Test
    fun `드론 다중 선택 저장은 iOS 키를 사용하고 legacy 키를 제거한다`() {
        val preferences = mutablePreferencesOf(
            DroneSelectionPreferenceKeys.LEGACY_SELECTED_DRONE_IDS to setOf("legacy-drone"),
        )

        preferences.writeDroneSelectedIds(setOf("drone-a", "drone-b"))

        assertEquals(setOf("drone-a", "drone-b"), preferences[DroneSelectionPreferenceKeys.SELECTED_DRONE_IDS])
        assertEquals(null, preferences[DroneSelectionPreferenceKeys.LEGACY_SELECTED_DRONE_IDS])
    }

    @Test
    fun `드론 단일 선택 저장은 iOS selectedDroneId 키를 사용한다`() {
        val preferences = mutablePreferencesOf()

        preferences.writeDroneSelectedDroneId("drone-a")

        assertEquals("drone-a", preferences[DroneSelectionPreferenceKeys.SELECTED_DRONE_ID])

        preferences.writeDroneSelectedDroneId(null)

        assertEquals(null, preferences[DroneSelectionPreferenceKeys.SELECTED_DRONE_ID])
    }

    @Test
    fun `첫 활성 드론 로드 시 전체 드론이 선택된다`() {
        val state = DroneSelectionState()
        val drones = listOf(
            DroneModel(id = "drone-a", name = "A"),
            DroneModel(id = "drone-b", name = "B"),
        )

        state.syncActiveDrones(drones)

        assertEquals(setOf("drone-a", "drone-b"), state.selectedDroneIds.value)
        assertEquals("drone-a", state.selectedDroneId.value)
    }

    @Test
    fun `저장된 부분 선택은 첫 활성 드론 로드 시 전체 선택으로 덮어쓰지 않는다`() {
        val state = DroneSelectionState()
        state.addDroneToSelection("drone-b")

        state.syncActiveDrones(
            listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
                DroneModel(id = "drone-c", name = "C"),
            )
        )

        assertEquals(setOf("drone-b"), state.selectedDroneIds.value)
        assertEquals("drone-a", state.selectedDroneId.value)
    }

    @Test
    fun `저장된 선택에 사라진 드론만 있으면 iOS처럼 선택된 활성 드론이 없다`() {
        val state = DroneSelectionState()
        state.addDroneToSelection("deleted-drone")

        state.syncActiveDrones(
            listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
            )
        )

        assertTrue(state.selectedDroneIds.value.isEmpty())
        assertEquals("drone-a", state.selectedDroneId.value)
    }

    @Test
    fun `전체 선택 상태에서 새 드론이 추가되면 새 드론도 선택된다`() {
        val state = DroneSelectionState()
        state.syncActiveDrones(listOf(DroneModel(id = "drone-a", name = "A")))

        state.syncActiveDrones(
            listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
            )
        )

        assertEquals(setOf("drone-a", "drone-b"), state.selectedDroneIds.value)
        assertEquals("drone-a", state.selectedDroneId.value)
    }

    @Test
    fun `새 도형 기본 드론은 iOS처럼 필터 체크 해제와 독립된 단일 선택을 유지한다`() {
        val state = DroneSelectionState()
        state.syncActiveDrones(
            listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
            )
        )

        state.toggleDroneSelection("drone-a")

        assertEquals(setOf("drone-b"), state.selectedDroneIds.value)
        assertEquals("drone-a", state.selectedDroneId.value)
    }

    @Test
    fun `런타임 선택 변경은 늦게 도착한 저장 선택값에 덮어쓰이지 않는다`() {
        val state = DroneSelectionState()
        state.syncActiveDrones(
            listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
            )
        )

        state.toggleDroneSelection("drone-b")
        state.onPersistedSelectionLoaded(
            StoredDroneSelection(
                selectedDroneId = null,
                selectedDroneIds = setOf("drone-b"),
            )
        )

        assertEquals(setOf("drone-a"), state.selectedDroneIds.value)
    }

    @Test
    fun `새 드론 선택 추가는 늦게 도착한 저장 선택값에 덮어쓰이지 않는다`() {
        val state = DroneSelectionState()
        state.syncActiveDrones(listOf(DroneModel(id = "drone-a", name = "A")))

        state.addDroneToSelection("drone-b")
        state.onPersistedSelectionLoaded(
            StoredDroneSelection(
                selectedDroneId = null,
                selectedDroneIds = setOf("drone-a"),
            )
        )

        assertEquals(setOf("drone-a", "drone-b"), state.selectedDroneIds.value)
    }

    @Test
    fun `계정 전환 reset은 이전 계정의 드론 선택과 하이라이트를 비운다`() {
        val state = DroneSelectionState()
        state.syncActiveDrones(
            listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
            )
        )
        state.toggleDroneHighlight("drone-a")

        state.resetForAccountSwitch()

        assertEquals(emptySet<String>(), state.selectedDroneIds.value)
        assertEquals(null, state.selectedDroneId.value)
        assertEquals(emptySet<String>(), state.highlightedDroneIds.value)
    }

    @Test
    fun `단일 선택 드론이 삭제되면 iOS처럼 첫 활성 드론으로 대체한다`() {
        val state = DroneSelectionState()
        state.syncActiveDrones(
            listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
            )
        )

        state.syncActiveDrones(listOf(DroneModel(id = "drone-b", name = "B")))

        assertEquals("drone-b", state.selectedDroneId.value)
    }

    @Test
    fun `새 드론 추가는 iOS처럼 현재 부분 선택 상태에도 새 드론을 선택 목록에 포함한다`() {
        val state = DroneSelectionState()
        state.syncActiveDrones(
            listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
            )
        )

        state.toggleDroneSelection("drone-b")
        state.addDroneToSelection("drone-c")
        state.syncActiveDrones(
            listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
                DroneModel(id = "drone-c", name = "C"),
            )
        )

        assertEquals(setOf("drone-a", "drone-c"), state.selectedDroneIds.value)
    }

    @Test
    fun `사용자가 모두 해제한 상태는 같은 런타임 동안 유지된다`() {
        val state = DroneSelectionState()
        val drones = listOf(DroneModel(id = "drone-a", name = "A"))
        state.syncActiveDrones(drones)

        state.toggleDroneSelection("drone-a")
        state.syncActiveDrones(drones)

        assertTrue(state.selectedDroneIds.value.isEmpty())
    }

    @Test
    fun `로그인 성공 후 전체 선택은 기존 부분 선택을 iOS처럼 덮어쓴다`() {
        val state = DroneSelectionState()
        val drones = listOf(
            DroneModel(id = "drone-a", name = "A"),
            DroneModel(id = "drone-b", name = "B"),
        )
        state.syncActiveDrones(drones)

        state.toggleDroneSelection("drone-b")
        state.selectAllDrones(drones)

        assertEquals(setOf("drone-a", "drone-b"), state.selectedDroneIds.value)
    }

    @Test
    fun `로그인 전체 선택은 사라진 드론의 강조 상태를 정리한다`() {
        val state = DroneSelectionState()
        state.syncActiveDrones(
            listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
            )
        )
        state.toggleDroneHighlight("drone-a")
        state.toggleDroneHighlight("drone-b")

        state.selectAllDrones(listOf(DroneModel(id = "drone-a", name = "A")))

        assertEquals(setOf("drone-a"), state.selectedDroneIds.value)
        assertEquals(setOf("drone-a"), state.highlightedDroneIds.value)
    }

    @Test
    fun `여러 드론을 동시에 강조할 수 있다`() {
        val state = DroneSelectionState()
        state.syncActiveDrones(
            listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
            )
        )

        state.toggleDroneHighlight("drone-a")
        state.toggleDroneHighlight("drone-b")

        assertEquals(setOf("drone-a", "drone-b"), state.highlightedDroneIds.value)

        state.toggleDroneHighlight("drone-a")

        assertEquals(setOf("drone-b"), state.highlightedDroneIds.value)
    }

    @Test
    fun `드론 선택 해제는 강조 상태도 지운다`() {
        val state = DroneSelectionState()
        val drones = listOf(DroneModel(id = "drone-a", name = "A"))
        state.syncActiveDrones(drones)

        state.toggleDroneHighlight("drone-a")
        state.toggleDroneSelection("drone-a")

        assertTrue(state.selectedDroneIds.value.isEmpty())
        assertTrue(state.highlightedDroneIds.value.isEmpty())
    }

    @Test
    fun `활성 드론 목록에서 사라진 드론의 강조만 정리된다`() {
        val state = DroneSelectionState()
        state.syncActiveDrones(
            listOf(
                DroneModel(id = "drone-a", name = "A"),
                DroneModel(id = "drone-b", name = "B"),
            )
        )
        state.toggleDroneHighlight("drone-a")
        state.toggleDroneHighlight("drone-b")

        state.syncActiveDrones(listOf(DroneModel(id = "drone-b", name = "B")))

        assertEquals(setOf("drone-b"), state.highlightedDroneIds.value)
    }

    @Test
    fun `선택된 드론의 도형과 첫 드론으로 간주되는 레거시 도형만 표시된다`() {
        val drones = listOf(
            DroneModel(id = "drone-a", name = "A"),
            DroneModel(id = "drone-b", name = "B"),
        )
        val shapes = listOf(
            shape(id = "shape-a", droneId = "drone-a"),
            shape(id = "shape-b", droneId = "drone-b"),
            shape(id = "shape-legacy", droneId = null),
        )

        val filtered = filterShapesForSelectedDrones(
            shapes = shapes,
            activeDrones = drones,
            selectedDroneIds = setOf("drone-a"),
        )

        assertEquals(listOf("shape-a", "shape-legacy"), filtered.map { it.id })
    }

    @Test
    fun `선택된 드론 칩은 iOS localizedStandardCompare 처럼 숫자를 자연 정렬한다`() {
        val drones = listOf(
            DroneModel(id = "drone-10", name = "드론 10"),
            DroneModel(id = "drone-2", name = "드론 2"),
            DroneModel(id = "drone-1", name = "드론 1"),
            DroneModel(id = "drone-unselected", name = "드론 0"),
        )

        val selectedDrones = selectedDronesForIosDropdown(
            activeDrones = drones,
            selectedDroneIds = setOf("drone-10", "drone-2", "drone-1"),
        )

        assertEquals(
            listOf("드론 1", "드론 2", "드론 10"),
            selectedDrones.map { it.name },
        )
    }

    @Test
    fun `드론 이름 비교는 iOS처럼 숫자 run 을 숫자로 비교한다`() {
        assertTrue(
            compareIosLocalizedStandardNames(
                first = "Drone 2",
                second = "Drone 10",
                locale = Locale.ENGLISH,
            ) < 0,
        )
    }

    private fun shape(id: String, droneId: String?): ShapeModel {
        return ShapeModel(
            id = id,
            title = id,
            baseCoordinate = Coordinate(37.0, 127.0),
            droneId = droneId,
        )
    }
}

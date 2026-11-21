package com.ScienceFiction.DronePassAndroid.feature.map.tap

/*
 * 지도 길게 누르기 판정(D-83 (2), 대표 11/21 "메뉴: 수정·삭제·여기에 새 도형"). iOS MapLongPressResolver와 같은 답을 낸다.
 *  ① 스케치 모드에서는 아무것도 하지 않는다(ignore).
 *  ② 지금 지도에 그려진 내 도형만 놓고 탭 판정(MapTapResolver, D-56)과 같은 규칙으로 도형을 고른다. 고르면 그 도형 메뉴.
 *     비행구역은 메뉴 대상이 아니라 뺀다. 넣으면 내 도형 안이라도 구역 테두리 근처에서 구역이 이겨 메뉴가 뜨지 않는다.
 *     필터로 숨긴 도형, 지도에 붙지 않은 도형도 뺀다. 스케치 선은 후보가 아니다.
 *  ③ 고른 도형이 없으면(비행구역 위, 숨긴 도형만 있는 곳 포함) 지금처럼 새 도형 흐름.
 */

/** 길게 누르기 결과. [raw]는 공유 fixture의 action 값과 같다 */
sealed interface MapLongPressAction {
    val raw: String

    /** 스케치 모드: 아무것도 하지 않는다 */
    data object Ignore : MapLongPressAction {
        override val raw = "ignore"
    }

    /** 그 도형의 메뉴(수정·삭제·여기에 새 도형) */
    data class ShapeMenu(val shapeId: String, val reason: MapTapResult.Reason) : MapLongPressAction {
        override val raw = "shapeMenu"
    }

    /** 새 도형 흐름(무료 한도 → 주소 조회 → 확인 창) */
    data object NewShape : MapLongPressAction {
        override val raw = "newShape"
    }
}

/** 길게 누르기 후보: 탭 판정 형상과 지금 지도에 그려졌는지 */
data class MapLongPressCandidate(
    val shape: MapTapShape,
    /** 지도에 붙어 있고 숨김이 아니다. false면(필터로 숨김, 지도에 안 붙음) 빼고 본다 */
    val isDrawn: Boolean,
)

object MapLongPressResolver {
    /** 판정. [candidates]에는 내 도형(비행구역이 섞여도 된다)과 각각 그려졌는지를 넣는다 */
    fun resolve(
        p: MapTapLatLng,
        toleranceM: Double,
        candidates: List<MapLongPressCandidate>,
        isSketchMode: Boolean,
    ): MapLongPressAction {
        if (isSketchMode) return MapLongPressAction.Ignore
        val shapes = candidates
            .filter { it.isDrawn && it.shape.target is MapTapTarget.Shape }
            .map { it.shape }
        val result = MapTapResolver.resolve(p, toleranceM, index = null, extra = shapes)
        val target = result.target as? MapTapTarget.Shape ?: return MapLongPressAction.NewShape
        return MapLongPressAction.ShapeMenu(target.id, result.reason)
    }
}

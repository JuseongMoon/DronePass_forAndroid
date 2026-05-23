package com.ScienceFiction.DronePassAndroid.core.data.remote.vworld

/**
 * 비행 제한 수준
 */
enum class FlightRestrictionLevel(val label: String) {
    PROHIBITED("비행금지"),
    RESTRICTED("비행제한"),
    CONSULTATION("사전협의"),
    ADVISORY("주의")
}

/**
 * VWorld WFS 비행구역 레이어 정의
 *
 * @param typeName WFS typename 파라미터
 * @param displayName UI에 표시할 한글 이름
 * @param fillColor 30% 투명도 ARGB 채우기 색상
 * @param borderColor 100% ARGB 테두리 색상
 * @param priority 우선순위 (1=최고, 비행금지)
 * @param restrictionLevel 비행 제한 수준
 */
enum class FlightZoneLayer(
    val typeName: String,
    val displayName: String,
    val fillColor: Long,
    val borderColor: Long,
    val priority: Int,
    val restrictionLevel: FlightRestrictionLevel
) {
    PROHIBITED(
        "lt_c_aisprhc", "비행금지구역",
        0x4DFF3B30, 0xFFFF3B30, 1, FlightRestrictionLevel.PROHIBITED
    ),
    TEMPORARY_PROHIBITED(
        "lt_c_aistemp", "임시비행금지",
        0x4DFF3B30, 0xFFFF3B30, 1, FlightRestrictionLevel.PROHIBITED
    ),
    CONTROL_ZONE(
        "lt_c_aisctrc", "관제권",
        0x4DFF9500, 0xFFFF9500, 2, FlightRestrictionLevel.RESTRICTED
    ),
    RESTRICTED(
        "lt_c_aisresc", "비행제한구역",
        0x4DFF9500, 0xFFFF9500, 2, FlightRestrictionLevel.RESTRICTED
    ),
    DANGER(
        "lt_c_aisdngc", "위험지역",
        0x4DFF9500, 0xFFFF9500, 2, FlightRestrictionLevel.RESTRICTED
    ),
    ALERT(
        "lt_c_aisaltc", "경계구역",
        0x4DFFCC00, 0xFFFFCC00, 3, FlightRestrictionLevel.ADVISORY
    ),
    ATZ(
        "lt_c_aisatzc", "비행장교통구역",
        0x4DFFCC00, 0xFFFFCC00, 3, FlightRestrictionLevel.ADVISORY
    ),
    ULTRALIGHT(
        "lt_c_aisuac", "초경량비행장치공역",
        0x4D007AFF, 0xFF007AFF, 3, FlightRestrictionLevel.CONSULTATION
    ),
    LANDING_FIELD(
        "lt_c_aisfldc", "경량항공기이착륙장",
        0x4D007AFF, 0xFF007AFF, 3, FlightRestrictionLevel.CONSULTATION
    ),
    OBSTACLE(
        "lt_c_aisobls", "장애물공역",
        0x4DFFCC00, 0xFFFFCC00, 3, FlightRestrictionLevel.ADVISORY
    ),
    PRIOR_CONSULTATION(
        "lt_c_aispca", "사전협의구역",
        0x4D007AFF, 0xFF007AFF, 3, FlightRestrictionLevel.CONSULTATION
    ),
    CULTURAL_HERITAGE(
        "lt_c_uo301", "문화재보호도",
        0x4D34C759, 0xFF34C759, 4, FlightRestrictionLevel.ADVISORY
    ),
    NATIONAL_PARK(
        "lt_c_wgisnpgug", "국립자연공원",
        0x4D34C759, 0xFF34C759, 4, FlightRestrictionLevel.ADVISORY
    );
}

package com.ScienceFiction.DronePassAndroid.core.res

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringResourceCoverageTest {

    @Test
    fun `English string resources cover the default string keys`() {
        val defaultNames = stringResourceNames("values/strings.xml")
        val englishNames = stringResourceNames("values-en/strings.xml")
        val missingNames = defaultNames - englishNames

        assertTrue(
            "values-en/strings.xml is missing: ${missingNames.sorted().joinToString()}",
            missingNames.isEmpty(),
        )
    }

    @Test
    fun `English plural resources cover the default plural keys`() {
        val defaultNames = pluralResourceNames("values/strings.xml")
        val englishNames = pluralResourceNames("values-en/strings.xml")
        val missingNames = defaultNames - englishNames

        assertTrue(
            "values-en/strings.xml is missing plurals: ${missingNames.sorted().joinToString()}",
            missingNames.isEmpty(),
        )
    }

    @Test
    fun `login strings match iOS localizations`() {
        assertEquals("로그인 / 회원가입", stringResourceValue("values/strings.xml", "login_title"))
        assertEquals("Apple로 계속하기", stringResourceValue("values/strings.xml", "login_apple"))
        assertEquals("Google로 로그인", stringResourceValue("values/strings.xml", "login_google"))
        assertEquals("로그인 / 회원가입 시", stringResourceValue("values/strings.xml", "login_terms_intro"))
        assertEquals("이용약관", stringResourceValue("values/strings.xml", "login_terms_service"))
        assertEquals("개인정보 취급방침", stringResourceValue("values/strings.xml", "login_terms_privacy"))
        assertEquals("에", stringResourceValue("values/strings.xml", "login_terms_middle"))
        assertEquals("\\u0020동의하게 됩니다.", stringResourceValue("values/strings.xml", "login_terms_agree"))
        assertEquals("By signing in/up, you agree to", stringResourceValue("values-en/strings.xml", "login_terms_intro"))
        assertEquals("Terms of Service", stringResourceValue("values-en/strings.xml", "login_terms_service"))
        assertEquals("Privacy Policy", stringResourceValue("values-en/strings.xml", "login_terms_privacy"))
        assertEquals(
            "\\u0020and",
            stringResourceValue("values-en/strings.xml", "login_terms_middle"),
        )
        assertEquals(".", stringResourceValue("values-en/strings.xml", "login_terms_agree"))
        assertEquals("로그인 오류", stringResourceValue("values/strings.xml", "login_error_title"))
        assertEquals("알 수 없는 오류", stringResourceValue("values/strings.xml", "login_error_unknown"))
        assertEquals("알 수 없는 오류가 발생했습니다.", stringResourceValue("values/strings.xml", "common_unknown_error"))
        assertEquals("Google 로그인 중 오류가 발생했습니다.", stringResourceValue("values/strings.xml", "login_google_error"))
        assertEquals(
            "Google 로그인 설정이 누락되었습니다. WEB_CLIENT_ID를 확인해주세요.",
            stringResourceValue("values/strings.xml", "login_google_config_missing"),
        )
        assertEquals("Apple 로그인 중 오류가 발생했습니다.", stringResourceValue("values/strings.xml", "login_apple_error"))
        assertEquals(
            "데이터 동기화에 실패했습니다. 네트워크 상태를 확인해주세요.",
            stringResourceValue("values/strings.xml", "login_sync_failed"),
        )
        assertEquals("다른 계정으로 전환할까요?", stringResourceValue("values/strings.xml", "login_account_switch_title"))
        assertEquals(
            "클라우드에 저장되지 않은 로컬 항목 %1\$d개가 있습니다. 계속하면 영구 삭제되어 복구할 수 없습니다. 기존 계정으로 다시 로그인하면 보존됩니다.",
            pluralResourceValue("values/strings.xml", "login_account_switch_message", "other"),
        )
        assertEquals("계속(삭제)", stringResourceValue("values/strings.xml", "login_account_switch_confirm"))
        assertEquals("Google 로고", stringResourceValue("values/strings.xml", "login_google_logo_description"))
        assertEquals("Sign In / Sign Up", stringResourceValue("values-en/strings.xml", "login_title"))
        assertEquals("Continue with Apple", stringResourceValue("values-en/strings.xml", "login_apple"))
        assertEquals("Sign in with Google", stringResourceValue("values-en/strings.xml", "login_google"))
        assertEquals("Google logo", stringResourceValue("values-en/strings.xml", "login_google_logo_description"))
        assertEquals("Login Error", stringResourceValue("values-en/strings.xml", "login_error_title"))
        assertEquals("Unknown error", stringResourceValue("values-en/strings.xml", "login_error_unknown"))
        assertEquals("An unknown error occurred.", stringResourceValue("values-en/strings.xml", "common_unknown_error"))
        assertEquals(
            "An error occurred while signing in with Google.",
            stringResourceValue("values-en/strings.xml", "login_google_error"),
        )
        assertEquals(
            "Google sign-in is not configured. Please check WEB_CLIENT_ID.",
            stringResourceValue("values-en/strings.xml", "login_google_config_missing"),
        )
        assertEquals(
            "An error occurred while signing in with Apple.",
            stringResourceValue("values-en/strings.xml", "login_apple_error"),
        )
        assertEquals(
            "Data sync failed. Please check your network connection.",
            stringResourceValue("values-en/strings.xml", "login_sync_failed"),
        )
        assertEquals("Switch to a different account?", stringResourceValue("values-en/strings.xml", "login_account_switch_title"))
        assertEquals(
            "There is %1\$d local item not saved to the cloud. Continuing will permanently delete it with no way to recover. Log in again with the previous account to keep it.",
            pluralResourceValue("values-en/strings.xml", "login_account_switch_message", "one"),
        )
        assertEquals(
            "There are %1\$d local items not saved to the cloud. Continuing will permanently delete them with no way to recover. Log in again with the previous account to keep them.",
            pluralResourceValue("values-en/strings.xml", "login_account_switch_message", "other"),
        )
        assertEquals("Continue (Discard)", stringResourceValue("values-en/strings.xml", "login_account_switch_confirm"))
    }

    @Test
    fun `foreground sync prompt strings match iOS localizations`() {
        assertEquals("변경사항 감지", stringResourceValue("values/strings.xml", "sync_alert_detected_title"))
        assertEquals(
            "다른 기기에서 변경사항이 감지되었습니다.\\n도형 정보를 최신화합니다.",
            stringResourceValue("values/strings.xml", "sync_alert_detected_message"),
        )
        assertEquals("동기화 중...", stringResourceValue("values/strings.xml", "sync_loading_title"))
        assertEquals("최신 데이터를 가져오는 중입니다.", stringResourceValue("values/strings.xml", "sync_loading_message"))
        assertEquals("동기화 완료", stringResourceValue("values/strings.xml", "sync_complete_title"))
        assertEquals("도형 정보가 최신화되었습니다.", stringResourceValue("values/strings.xml", "sync_complete_message"))
        assertEquals("동기화 실패", stringResourceValue("values/strings.xml", "sync_error_title"))
        assertEquals(
            "변경사항을 가져오는 중 오류가 발생했습니다.\\n%1\$s",
            stringResourceValue("values/strings.xml", "sync_error_message"),
        )
        assertEquals("Changes Detected", stringResourceValue("values-en/strings.xml", "sync_alert_detected_title"))
        assertEquals(
            "Changes detected from another device.\\nUpdating shape data.",
            stringResourceValue("values-en/strings.xml", "sync_alert_detected_message"),
        )
        assertEquals("Syncing...", stringResourceValue("values-en/strings.xml", "sync_loading_title"))
        assertEquals("Fetching latest data.", stringResourceValue("values-en/strings.xml", "sync_loading_message"))
        assertEquals("Sync Complete", stringResourceValue("values-en/strings.xml", "sync_complete_title"))
        assertEquals("Shape data has been updated.", stringResourceValue("values-en/strings.xml", "sync_complete_message"))
        assertEquals("Sync Failed", stringResourceValue("values-en/strings.xml", "sync_error_title"))
        assertEquals(
            "Failed to fetch changes.\\n%1\$s",
            stringResourceValue("values-en/strings.xml", "sync_error_message"),
        )
    }

    @Test
    fun `saved list section and sort labels match iOS localizations`() {
        assertEquals("저장 목록", stringResourceValue("values/strings.xml", "screen_saved_list"))
        assertEquals("활성화", stringResourceValue("values/strings.xml", "saved_section_active"))
        assertEquals("시작 전", stringResourceValue("values/strings.xml", "saved_section_not_started"))
        assertEquals("만료됨", stringResourceValue("values/strings.xml", "saved_section_expired"))
        assertEquals("오름차순", stringResourceValue("values/strings.xml", "saved_sort_ascending"))
        assertEquals("내림차순", stringResourceValue("values/strings.xml", "saved_sort_descending"))
        assertEquals("Saved List", stringResourceValue("values-en/strings.xml", "screen_saved_list"))
        assertEquals("By Title", stringResourceValue("values-en/strings.xml", "saved_sort_title"))
        assertEquals("By Date Created", stringResourceValue("values-en/strings.xml", "saved_sort_date_created"))
        assertEquals("By Flight Start", stringResourceValue("values-en/strings.xml", "saved_sort_flight_start"))
        assertEquals("By Flight End", stringResourceValue("values-en/strings.xml", "saved_sort_flight_end"))
        assertEquals("Ascending", stringResourceValue("values-en/strings.xml", "saved_sort_ascending"))
        assertEquals("Descending", stringResourceValue("values-en/strings.xml", "saved_sort_descending"))
        assertEquals("Not Started", stringResourceValue("values-en/strings.xml", "saved_section_not_started"))
    }

    @Test
    fun `saved list empty state strings match iOS localizations`() {
        assertEquals("저장된 도형이 없습니다.", stringResourceValue("values/strings.xml", "saved_empty_no_search"))
        assertEquals(
            "지도에서 + 버튼을 누르거나 지도를 길게 눌러서 새로운 도형을 추가해보세요.",
            stringResourceValue("values/strings.xml", "saved_empty_hint"),
        )
        assertEquals(
            "드론을 선택하면 도형이 표시됩니다.",
            stringResourceValue("values/strings.xml", "saved_empty_no_drone_selected"),
        )
        assertEquals(
            "우측 상단의 드론 선택 메뉴에서 드론을 선택해주세요.",
            stringResourceValue("values/strings.xml", "saved_empty_no_drone_selected_hint"),
        )
        assertEquals(
            "선택한 드론에 해당하는 도형이 없습니다.",
            stringResourceValue("values/strings.xml", "saved_empty_no_matching_shapes"),
        )
        assertEquals(
            "다른 드론을 선택하거나 새로운 도형을 추가해보세요.",
            stringResourceValue("values/strings.xml", "saved_empty_no_matching_shapes_hint"),
        )

        assertEquals("No saved shapes", stringResourceValue("values-en/strings.xml", "saved_empty_no_search"))
        assertEquals(
            "Tap + button or long press on map to add shapes",
            stringResourceValue("values-en/strings.xml", "saved_empty_hint"),
        )
        assertEquals(
            "Select a drone to view shapes",
            stringResourceValue("values-en/strings.xml", "saved_empty_no_drone_selected"),
        )
        assertEquals(
            "Select a drone from the menu",
            stringResourceValue("values-en/strings.xml", "saved_empty_no_drone_selected_hint"),
        )
        assertEquals(
            "No shapes for selected drone",
            stringResourceValue("values-en/strings.xml", "saved_empty_no_matching_shapes"),
        )
        assertEquals(
            "Select another drone or add new shapes",
            stringResourceValue("values-en/strings.xml", "saved_empty_no_matching_shapes_hint"),
        )
    }

    @Test
    fun `map new shape alert strings match iOS localizations`() {
        assertEquals("예", stringResourceValue("values/strings.xml", "common_yes"))
        assertEquals("아니오", stringResourceValue("values/strings.xml", "common_no"))
        assertEquals("새 도형 만들기", stringResourceValue("values/strings.xml", "map_new_shape_alert_title"))
        assertEquals("새 도형 추가", stringResourceValue("values/strings.xml", "map_fab_add_shape"))
        assertEquals(
            "해당 위치에 새 도형을 만드시겠습니까?",
            stringResourceValue("values/strings.xml", "map_new_shape_alert_message"),
        )
        assertEquals("주소 검색 실패", stringResourceValue("values/strings.xml", "map_address_search_failed_title"))
        assertEquals(
            "선택한 위치의 주소를 가져올 수 없습니다. 좌표로만 도형을 만드시겠습니까?",
            stringResourceValue("values/strings.xml", "map_address_search_failed_message"),
        )
        assertEquals("해당 위치의 주소가 존재하지 않습니다", stringResourceValue("values/strings.xml", "map_address_not_found"))

        assertEquals("Yes", stringResourceValue("values-en/strings.xml", "common_yes"))
        assertEquals("No", stringResourceValue("values-en/strings.xml", "common_no"))
        assertEquals("Create New Shape", stringResourceValue("values-en/strings.xml", "map_new_shape_alert_title"))
        assertEquals("Add New Shape", stringResourceValue("values-en/strings.xml", "map_fab_add_shape"))
        assertEquals(
            "Would you like to create a new shape at this location?",
            stringResourceValue("values-en/strings.xml", "map_new_shape_alert_message"),
        )
        assertEquals("Address Search Failed", stringResourceValue("values-en/strings.xml", "map_address_search_failed_title"))
        assertEquals(
            "Unable to retrieve address for the selected location. Would you like to create the shape with coordinates only?",
            stringResourceValue("values-en/strings.xml", "map_address_search_failed_message"),
        )
        assertEquals(
            "Address not found for this location",
            stringResourceValue("values-en/strings.xml", "map_address_not_found"),
        )
    }

    @Test
    fun `shape detail labels match iOS localizations`() {
        assertEquals("상세 정보", stringResourceValue("values/strings.xml", "shape_detail_navigation_title"))
        assertEquals("고도(m)", stringResourceValue("values/strings.xml", "shape_detail_altitude"))
        assertEquals("길찾기 앱 선택", stringResourceValue("values/strings.xml", "shape_detail_open_external_map"))
        assertEquals("네이버지도", stringResourceValue("values/strings.xml", "shape_detail_open_naver_map"))
        assertEquals("티맵", stringResourceValue("values/strings.xml", "shape_detail_open_tmap"))
        assertEquals("구글맵", stringResourceValue("values/strings.xml", "shape_detail_open_google_map"))
        assertEquals("수정하기", stringResourceValue("values/strings.xml", "shape_detail_edit"))
        assertEquals("복제하기", stringResourceValue("values/strings.xml", "shape_detail_duplicate"))
        assertEquals("삭제된 드론", stringResourceValue("values/strings.xml", "shape_detail_drone_deleted"))
        assertEquals("드론 미지정", stringResourceValue("values/strings.xml", "shape_detail_drone_unassigned"))
        assertEquals("아래 앱으로 길찾기를 시작합니다.", stringResourceValue("values/strings.xml", "shape_detail_navigation_message"))
        assertEquals("'%s' 도형을 삭제하시겠습니까?", androidDisplayStringResourceValue("values/strings.xml", "shape_detail_delete_message"))
        assertEquals("Coordinates", stringResourceValue("values-en/strings.xml", "shape_detail_coordinate"))
        assertEquals("Delete Shape", stringResourceValue("values-en/strings.xml", "shape_detail_delete_title"))
        assertEquals("Are you sure you want to delete '%s'?", androidDisplayStringResourceValue("values-en/strings.xml", "shape_detail_delete_message"))
        assertEquals("Kakao Map", stringResourceValue("values-en/strings.xml", "shape_detail_open_kakao_map"))
    }

    @Test
    fun `shape edit labels match iOS localizations`() {
        assertEquals("반경(m)", stringResourceValue("values/strings.xml", "shape_edit_radius_label"))
        assertEquals("고도(m)", stringResourceValue("values/strings.xml", "shape_edit_altitude_label"))
        assertEquals("주소를 검색하세요", stringResourceValue("values/strings.xml", "shape_edit_address_search_placeholder"))
        assertEquals("주소를 검색하세요", stringResourceValue("values/strings.xml", "shape_edit_placeholder_address"))
        assertEquals("제목을 입력하세요", stringResourceValue("values/strings.xml", "shape_edit_title_placeholder"))
        assertEquals("좌표를 입력하세요", stringResourceValue("values/strings.xml", "shape_edit_coordinate_placeholder"))
        assertEquals("반경을 입력하세요", stringResourceValue("values/strings.xml", "shape_edit_radius_placeholder"))
        assertEquals("비행 고도를 입력해주세요", stringResourceValue("values/strings.xml", "shape_edit_altitude_placeholder"))
        assertEquals("메모", stringResourceValue("values/strings.xml", "shape_edit_label_memo"))
        assertEquals("메모를 입력하세요", stringResourceValue("values/strings.xml", "shape_edit_placeholder_memo"))
        assertEquals("시작일", stringResourceValue("values/strings.xml", "shape_edit_start_date"))
        assertEquals("종료일", stringResourceValue("values/strings.xml", "shape_edit_end_date"))
        assertEquals("시작일 선택", stringResourceValue("values/strings.xml", "shape_edit_start_date_select"))
        assertEquals("종료일 선택", stringResourceValue("values/strings.xml", "shape_edit_end_date_select"))
        assertEquals(
            "드론원스탑에서 승인받은 좌표를 복사&붙여넣기 하세요",
            androidDisplayStringResourceValue("values/strings.xml", "coordinate_guide"),
        )
        assertEquals("지원하는 좌표 형식:", stringResourceValue("values/strings.xml", "coordinate_format_title"))
        assertEquals(
            "• 도/분/초: 37° 38′ 55″ N 126° 41′ 12″ E",
            stringResourceValue("values/strings.xml", "coordinate_example_dms"),
        )
        assertEquals(
            "• 십진도: 37.648611°, 126.686667°",
            stringResourceValue("values/strings.xml", "coordinate_example_decimal_degrees"),
        )
        assertEquals(
            "• 단순 십진수: 37.3855 126.4142",
            stringResourceValue("values/strings.xml", "coordinate_example_simple_decimal"),
        )
        assertEquals(
            "• Geo URI: geo:37.648611,126.686667",
            stringResourceValue("values/strings.xml", "coordinate_example_geo_uri"),
        )
        assertEquals("일단위 입력", stringResourceValue("values/strings.xml", "shape_edit_date_only_mode"))
        assertEquals("취소", stringResourceValue("values/strings.xml", "common_cancel"))
        assertEquals("선택 완료", stringResourceValue("values/strings.xml", "date_time_done"))
        assertEquals("좌표 입력", stringResourceValue("values/strings.xml", "coordinate_placeholder"))
        assertEquals("잘못된 좌표 형식입니다", stringResourceValue("values/strings.xml", "coordinate_validation_invalid"))
        assertEquals("주소 검색", stringResourceValue("values/strings.xml", "search_address_title"))
        assertEquals("도로명 또는 지번 주소로 검색", stringResourceValue("values/strings.xml", "search_address_placeholder"))
        assertEquals("도로명 주소와 지번 주소 모두 검색 가능합니다", stringResourceValue("values/strings.xml", "search_address_guide"))
        assertEquals("검색 중 오류가 발생했습니다", stringResourceValue("values/strings.xml", "search_address_error_prefix"))
        assertEquals("수정 중인 정보가 있습니다", stringResourceValue("values/strings.xml", "shape_edit_alert_unsaved_title"))
        assertEquals("수정 중인 내용이 모두 사라집니다. 닫으시겠습니까?", stringResourceValue("values/strings.xml", "shape_edit_alert_unsaved_message"))
        assertEquals("닫기", stringResourceValue("values/strings.xml", "shape_edit_alert_unsaved_discard"))
        assertEquals("도형 추가 중 오류가 발생했습니다: %1\$s", stringResourceValue("values/strings.xml", "shape_edit_error_add_failed"))
        assertEquals("도형 수정 중 오류가 발생했습니다: %1\$s", stringResourceValue("values/strings.xml", "shape_edit_error_update_failed"))
        assertEquals("Coordinates", stringResourceValue("values-en/strings.xml", "shape_edit_label_coordinate"))
        assertEquals("Search address", stringResourceValue("values-en/strings.xml", "shape_edit_placeholder_address"))
        assertEquals("Altitude", stringResourceValue("values-en/strings.xml", "shape_edit_altitude_label"))
        assertEquals("Memo", stringResourceValue("values-en/strings.xml", "shape_edit_label_memo"))
        assertEquals("Select Drone", stringResourceValue("values-en/strings.xml", "shape_edit_drone_label"))
        assertEquals("Start Date", stringResourceValue("values-en/strings.xml", "shape_edit_start_date"))
        assertEquals("End Date", stringResourceValue("values-en/strings.xml", "shape_edit_end_date"))
        assertEquals("Select Start Date", stringResourceValue("values-en/strings.xml", "shape_edit_start_date_select"))
        assertEquals("Select End Date", stringResourceValue("values-en/strings.xml", "shape_edit_end_date_select"))
        assertEquals("Day mode", stringResourceValue("values-en/strings.xml", "shape_edit_date_only_mode"))
        assertEquals("Cancel", stringResourceValue("values-en/strings.xml", "common_cancel"))
        assertEquals("Done", stringResourceValue("values-en/strings.xml", "date_time_done"))
        assertEquals("Enter coordinates", stringResourceValue("values-en/strings.xml", "coordinate_placeholder"))
        assertEquals(
            "Enter flight coordinates in one of the supported formats",
            stringResourceValue("values-en/strings.xml", "coordinate_guide"),
        )
        assertEquals("Supported formats:", stringResourceValue("values-en/strings.xml", "coordinate_format_title"))
        assertEquals(
            "• 도/분/초: 37° 38′ 55″ N 126° 41′ 12″ E",
            stringResourceValue("values-en/strings.xml", "coordinate_example_dms"),
        )
        assertEquals(
            "• 십진도: 37.648611°, 126.686667°",
            stringResourceValue("values-en/strings.xml", "coordinate_example_decimal_degrees"),
        )
        assertEquals(
            "• 단순 십진수: 37.3855 126.4142",
            stringResourceValue("values-en/strings.xml", "coordinate_example_simple_decimal"),
        )
        assertEquals(
            "• Geo URI: geo:37.648611,126.686667",
            stringResourceValue("values-en/strings.xml", "coordinate_example_geo_uri"),
        )
        assertEquals("Invalid coordinate format", stringResourceValue("values-en/strings.xml", "coordinate_validation_invalid"))
        assertEquals("Search Address", stringResourceValue("values-en/strings.xml", "search_address_title"))
        assertEquals("Search by road or lot address", stringResourceValue("values-en/strings.xml", "search_address_placeholder"))
        assertEquals("Both road and lot addresses are searchable", stringResourceValue("values-en/strings.xml", "search_address_guide"))
        assertEquals("Error searching address", stringResourceValue("values-en/strings.xml", "search_address_error_prefix"))
        assertEquals("Unsaved Changes", stringResourceValue("values-en/strings.xml", "shape_edit_alert_unsaved_title"))
        assertEquals("All changes will be lost. Close anyway?", stringResourceValue("values-en/strings.xml", "shape_edit_alert_unsaved_message"))
        assertEquals("Close", stringResourceValue("values-en/strings.xml", "shape_edit_alert_unsaved_discard"))
        assertEquals("Failed to add shape: %1\$s", stringResourceValue("values-en/strings.xml", "shape_edit_error_add_failed"))
        assertEquals("Failed to update shape: %1\$s", stringResourceValue("values-en/strings.xml", "shape_edit_error_update_failed"))
    }

    @Test
    fun `settings Korea local feature strings match iOS localizations`() {
        assertEquals("언어 변경", stringResourceValue("values/strings.xml", "settings_language_restart_title"))
        assertEquals(
            "언어 변경을 완전히 적용하려면 앱을 다시 시작해주세요.",
            stringResourceValue("values/strings.xml", "settings_language_restart_message"),
        )

        assertEquals("한국 현지 기능", stringResourceValue("values/strings.xml", "settings_korea_features"))
        assertEquals("한국 현지 기능 활성화", stringResourceValue("values/strings.xml", "settings_korea_features_on_title"))
        assertEquals(
            "한국 현지 기능이 활성화됩니다. (비행구역 정보 등)",
            stringResourceValue("values/strings.xml", "settings_korea_features_on_message"),
        )
        assertEquals("한국 현지 기능 비활성화", stringResourceValue("values/strings.xml", "settings_korea_features_off_title"))
        assertEquals(
            "한국 현지 기능이 비활성화됩니다.",
            stringResourceValue("values/strings.xml", "settings_korea_features_off_message"),
        )

        assertEquals("Language Changed", stringResourceValue("values-en/strings.xml", "settings_language_restart_title"))
        assertEquals(
            "Please restart the app to fully apply the language change.",
            stringResourceValue("values-en/strings.xml", "settings_language_restart_message"),
        )

        assertEquals("Local Features (Korea)", stringResourceValue("values-en/strings.xml", "settings_korea_features"))
        assertEquals("Korea Local Features Enabled", stringResourceValue("values-en/strings.xml", "settings_korea_features_on_title"))
        assertEquals(
            "Local features for Korea enabled. (Flight zones, etc.)",
            stringResourceValue("values-en/strings.xml", "settings_korea_features_on_message"),
        )
        assertEquals("Korea Local Features Disabled", stringResourceValue("values-en/strings.xml", "settings_korea_features_off_title"))
        assertEquals(
            "Local features for Korea disabled.",
            stringResourceValue("values-en/strings.xml", "settings_korea_features_off_message"),
        )
    }

    @Test
    fun `settings expired shape deletion strings match iOS localizations`() {
        assertEquals("만료된 도형 전부 삭제", stringResourceValue("values/strings.xml", "settings_delete_expired_shapes"))
        assertEquals(
            "만료된 도형을 모두 삭제할까요?",
            stringResourceValue("values/strings.xml", "settings_delete_expired_alert_title"),
        )
        assertEquals(
            "종료일이 지난 도형을 모두 삭제합니다. 이 작업은 되돌릴 수 없습니다.",
            stringResourceValue("values/strings.xml", "settings_delete_expired_alert_message"),
        )

        assertEquals(
            "Delete all expired shapes",
            stringResourceValue("values-en/strings.xml", "settings_delete_expired_shapes"),
        )
        assertEquals(
            "Delete all expired shapes?",
            stringResourceValue("values-en/strings.xml", "settings_delete_expired_alert_title"),
        )
        assertEquals(
            "All shapes past their end date will be deleted. This action cannot be undone.",
            stringResourceValue("values-en/strings.xml", "settings_delete_expired_alert_message"),
        )
    }

    @Test
    fun `settings account deletion strings do not imply local data deletion`() {
        assertEquals("회원 탈퇴", stringResourceValue("values/strings.xml", "settings_delete_account"))
        assertEquals(
            "탈퇴 시 계정만 삭제되며, 로컬 데이터는 계속 사용할 수 있습니다.",
            stringResourceValue("values/strings.xml", "settings_delete_account_subtitle"),
        )
        assertEquals(
            "계정이 삭제되고\\n클라우드 동기화가 중단됩니다.\\n\\n기기에 저장된 도형과 드론은\\n계속 사용할 수 있습니다.\\n\\n정말 탈퇴하시겠습니까?",
            stringResourceValue("values/strings.xml", "settings_delete_account_confirm"),
        )
        assertEquals("탈퇴하기", stringResourceValue("values/strings.xml", "settings_delete_account_button"))

        assertEquals("Delete Account", stringResourceValue("values-en/strings.xml", "settings_delete_account"))
        assertEquals(
            "Only the account will be deleted. Local data will remain available.",
            stringResourceValue("values-en/strings.xml", "settings_delete_account_subtitle"),
        )
        assertEquals(
            "Your account will be deleted and cloud sync will stop.\\n\\nShapes and drones saved on this device will remain available.\\n\\nAre you sure you want to delete your account?",
            stringResourceValue("values-en/strings.xml", "settings_delete_account_confirm"),
        )
        assertEquals("Delete", stringResourceValue("values-en/strings.xml", "settings_delete_account_button"))
    }

    @Test
    fun `app info strings match iOS localizations`() {
        assertEquals("앱 정보", stringResourceValue("values/strings.xml", "app_info_title"))
        assertEquals(
            "DronePass는\\n드론 비행에 필요한 모든 정보를 제공하는\\n통합 관리 어플입니다.\\n\\n비행 허가지 시각화부터 실시간 날씨, KP 지수,\\n일출/일몰 정보까지 한 번에 확인하세요.",
            stringResourceValue("values/strings.xml", "app_info_description"),
        )
        assertEquals("앱 소개", stringResourceValue("values/strings.xml", "app_info_section_intro"))
        assertEquals("드론 관리", stringResourceValue("values/strings.xml", "app_info_section_drone_management"))
        assertEquals("환경 정보", stringResourceValue("values/strings.xml", "app_info_section_environmental_info"))
        assertEquals("도형 및 지도", stringResourceValue("values/strings.xml", "app_info_section_shapes_and_map"))
        assertEquals("클라우드 및 데이터", stringResourceValue("values/strings.xml", "app_info_section_cloud_and_data"))
        assertEquals("버전 정보", stringResourceValue("values/strings.xml", "app_info_section_version"))
        assertEquals("연락처", stringResourceValue("values/strings.xml", "app_info_section_contact"))
        assertEquals("다중 드론 관리", stringResourceValue("values/strings.xml", "app_info_feature_multi_drone_title"))
        assertEquals("여러 대의 드론을 등록하고 관리", stringResourceValue("values/strings.xml", "app_info_feature_multi_drone_desc"))
        assertEquals("드론 비행 허가지 시각화", stringResourceValue("values/strings.xml", "app_info_feature_visualization_title"))
        assertEquals("드론 비행 허가 구역을 지도에 표시", stringResourceValue("values/strings.xml", "app_info_feature_visualization_desc"))
        assertEquals("만료일 기반 알림", stringResourceValue("values/strings.xml", "app_info_feature_expiration_alert_title"))
        assertEquals("비행 종료일 7일 전 자동 알림", stringResourceValue("values/strings.xml", "app_info_feature_expiration_alert_desc"))
        assertEquals("실시간 날씨 정보", stringResourceValue("values/strings.xml", "app_info_feature_weather_title"))
        assertEquals("풍향, 풍속, 돌풍, 결로위험지수 등", stringResourceValue("values/strings.xml", "app_info_feature_weather_desc"))
        assertEquals("KP 지수 모니터링", stringResourceValue("values/strings.xml", "app_info_feature_kp_index_title"))
        assertEquals("GPS 정확도에 영향을 주는 지자기 활동 지수", stringResourceValue("values/strings.xml", "app_info_feature_kp_index_desc"))
        assertEquals("일출/일몰 정보 및 알림", stringResourceValue("values/strings.xml", "app_info_feature_sunrise_sunset_title"))
        assertEquals("현재 위치의 일출/일몰 시간과 알림", stringResourceValue("values/strings.xml", "app_info_feature_sunrise_sunset_desc"))
        assertEquals("반경 기반 도형 생성 및 관리", stringResourceValue("values/strings.xml", "app_info_feature_shape_management_title"))
        assertEquals("원, 사각형, 다각형, 선 생성/편집", stringResourceValue("values/strings.xml", "app_info_feature_shape_management_desc"))
        assertEquals("도형 복제 및 정렬 기능", stringResourceValue("values/strings.xml", "app_info_feature_shape_duplicate_title"))
        assertEquals("도형 복사 및 다양한 정렬 옵션", stringResourceValue("values/strings.xml", "app_info_feature_shape_duplicate_desc"))
        assertEquals("주소 검색 및 길찾기", stringResourceValue("values/strings.xml", "app_info_feature_search_title"))
        assertEquals("주소로 위치 검색 및 내비게이션", stringResourceValue("values/strings.xml", "app_info_feature_search_desc"))
        assertEquals("클라우드 실시간 동기화", stringResourceValue("values/strings.xml", "app_info_feature_cloud_sync_title"))
        assertEquals("여러 기기 간 실시간 데이터 동기화", stringResourceValue("values/strings.xml", "app_info_feature_cloud_sync_desc"))
        assertEquals("드론 원스톱 연계 최적화", stringResourceValue("values/strings.xml", "app_info_feature_drone_onestop_title"))
        assertEquals(
            "드론 원스톱 서비스 연계를 위한 데이터 구조",
            stringResourceValue("values/strings.xml", "app_info_feature_drone_onestop_desc"),
        )
        assertEquals("앱 버전", stringResourceValue("values/strings.xml", "app_info_version_app"))
        assertEquals("빌드 번호", stringResourceValue("values/strings.xml", "app_info_version_build"))
        assertEquals("Science Fiction Inc.", stringResourceValue("values/strings.xml", "app_info_contact_company"))
        assertEquals("support@sciencefiction.co.kr", stringResourceValue("values/strings.xml", "app_info_contact_email"))
        assertEquals("문의사항이 있으시면 언제든지 연락해주세요.", stringResourceValue("values/strings.xml", "app_info_contact_message"))

        assertEquals("App Info", stringResourceValue("values-en/strings.xml", "app_info_title"))
        assertEquals(
            "DronePass is\\nan integrated management app\\nthat provides all the information you need for drone flight.\\n\\nCheck flight zones, real-time weather, KP index,\\nand sunrise/sunset times all at once.",
            stringResourceValue("values-en/strings.xml", "app_info_description"),
        )
        assertEquals("About the App", stringResourceValue("values-en/strings.xml", "app_info_section_intro"))
        assertEquals("Drone Management", stringResourceValue("values-en/strings.xml", "app_info_section_drone_management"))
        assertEquals("Environmental Information", stringResourceValue("values-en/strings.xml", "app_info_section_environmental_info"))
        assertEquals("Shapes and Map", stringResourceValue("values-en/strings.xml", "app_info_section_shapes_and_map"))
        assertEquals("Cloud and Data", stringResourceValue("values-en/strings.xml", "app_info_section_cloud_and_data"))
        assertEquals("Version Info", stringResourceValue("values-en/strings.xml", "app_info_section_version"))
        assertEquals("Contact", stringResourceValue("values-en/strings.xml", "app_info_section_contact"))
        assertEquals("Multi-Drone Management", stringResourceValue("values-en/strings.xml", "app_info_feature_multi_drone_title"))
        assertEquals("Register and manage multiple drones", stringResourceValue("values-en/strings.xml", "app_info_feature_multi_drone_desc"))
        assertEquals("Drone Flight Zone Visualization", stringResourceValue("values-en/strings.xml", "app_info_feature_visualization_title"))
        assertEquals("Display drone flight permitted areas on map", stringResourceValue("values-en/strings.xml", "app_info_feature_visualization_desc"))
        assertEquals("Expiration-Based Notifications", stringResourceValue("values-en/strings.xml", "app_info_feature_expiration_alert_title"))
        assertEquals("Automatic notification 7 days before flight end date", stringResourceValue("values-en/strings.xml", "app_info_feature_expiration_alert_desc"))
        assertEquals("Real-Time Weather Information", stringResourceValue("values-en/strings.xml", "app_info_feature_weather_title"))
        assertEquals("Wind direction, speed, gusts, dew point risk index, etc.", stringResourceValue("values-en/strings.xml", "app_info_feature_weather_desc"))
        assertEquals("KP Index Monitoring", stringResourceValue("values-en/strings.xml", "app_info_feature_kp_index_title"))
        assertEquals("Geomagnetic activity index affecting GPS accuracy", stringResourceValue("values-en/strings.xml", "app_info_feature_kp_index_desc"))
        assertEquals("Sunrise/Sunset Info and Notifications", stringResourceValue("values-en/strings.xml", "app_info_feature_sunrise_sunset_title"))
        assertEquals("Sunrise/sunset times and notifications for current location", stringResourceValue("values-en/strings.xml", "app_info_feature_sunrise_sunset_desc"))
        assertEquals("Radius-Based Shape Creation and Management", stringResourceValue("values-en/strings.xml", "app_info_feature_shape_management_title"))
        assertEquals("Create/edit circles, rectangles, polygons, lines", stringResourceValue("values-en/strings.xml", "app_info_feature_shape_management_desc"))
        assertEquals("Shape Duplication and Alignment", stringResourceValue("values-en/strings.xml", "app_info_feature_shape_duplicate_title"))
        assertEquals("Copy shapes and various alignment options", stringResourceValue("values-en/strings.xml", "app_info_feature_shape_duplicate_desc"))
        assertEquals("Address Search and Navigation", stringResourceValue("values-en/strings.xml", "app_info_feature_search_title"))
        assertEquals("Search locations by address and navigation", stringResourceValue("values-en/strings.xml", "app_info_feature_search_desc"))
        assertEquals("Cloud Real-Time Synchronization", stringResourceValue("values-en/strings.xml", "app_info_feature_cloud_sync_title"))
        assertEquals("Real-time data synchronization across multiple devices", stringResourceValue("values-en/strings.xml", "app_info_feature_cloud_sync_desc"))
        assertEquals("Flight Permit Data Structure", stringResourceValue("values-en/strings.xml", "app_info_feature_drone_onestop_title"))
        assertEquals(
            "Data structure optimized for flight permit applications",
            stringResourceValue("values-en/strings.xml", "app_info_feature_drone_onestop_desc"),
        )
        assertEquals("App Version", stringResourceValue("values-en/strings.xml", "app_info_version_app"))
        assertEquals("Build Number", stringResourceValue("values-en/strings.xml", "app_info_version_build"))
        assertEquals("Science Fiction Inc.", stringResourceValue("values-en/strings.xml", "app_info_contact_company"))
        assertEquals("support@sciencefiction.co.kr", stringResourceValue("values-en/strings.xml", "app_info_contact_email"))
        assertEquals("Please feel free to contact us with any questions.", stringResourceValue("values-en/strings.xml", "app_info_contact_message"))
    }

    @Test
    fun `KP info guide strings match iOS localizations`() {
        assertEquals("KP 지수 정보", stringResourceValue("values/strings.xml", "kp_info_title"))
        assertEquals("KP 지수란?", stringResourceValue("values/strings.xml", "kp_info_what_title"))
        assertEquals("KP 지수는 지구 자기장 교란 정도를 나타냅니다.", stringResourceValue("values/strings.xml", "kp_info_what_body"))
        assertEquals("범위: 0-9 (0: 매우 조용함, 9: 극도로 활발함)", stringResourceValue("values/strings.xml", "kp_info_what_bullet_range"))
        assertEquals("지자기 활동은 GPS 신호의 정확도에 직접적인 영향을 미칩니다.", stringResourceValue("values/strings.xml", "kp_info_drone_body"))
        assertEquals("KP 5 이상: 드론 비행 시 특히 주의 필요", stringResourceValue("values/strings.xml", "kp_info_relation_bullet_kp5"))
        assertEquals("GFZ (독일): 전 세계 13개 관측소 실시간 측정치 (3시간 단위)", stringResourceValue("values/strings.xml", "kp_info_source_gfz"))
        assertEquals("NOAA (미국): 8개 관측소 실시간 추정 + 예보치 (분 단위)", stringResourceValue("values/strings.xml", "kp_info_source_noaa"))
        assertEquals("지자기 경보 등급", stringResourceValue("values/strings.xml", "kp_info_levels_title"))
        assertEquals("낮음", stringResourceValue("values/strings.xml", "kp_info_level_normal_name"))
        assertEquals("G3 (강함)", stringResourceValue("values/strings.xml", "kp_info_level_g3_name"))
        assertEquals("현재 시간대 기준", stringResourceValue("values/strings.xml", "kp_forecast_note"))
        assertEquals("지자기 활동이 낮은 상태입니다", stringResourceValue("values/strings.xml", "kp_level_normal_desc"))
        assertEquals("심각한 지자기 폭풍", stringResourceValue("values/strings.xml", "kp_level_g4_desc"))
        assertEquals("비행 자제 권장, GPS 사용 불가능", stringResourceValue("values/strings.xml", "kp_info_level_g5_advice"))

        assertEquals("KP Index Information", stringResourceValue("values-en/strings.xml", "kp_info_title"))
        assertEquals("What is KP Index?", stringResourceValue("values-en/strings.xml", "kp_info_what_title"))
        assertEquals("Range: 0-9 (0: Very quiet, 9: Extremely active)", stringResourceValue("values-en/strings.xml", "kp_info_what_bullet_range"))
        assertEquals(
            "KP Index represents the level of disturbance in Earth's magnetic field.",
            androidDisplayStringResourceValue("values-en/strings.xml", "kp_info_what_body"),
        )
        assertEquals(
            "DronePass uses data from two organizations.",
            stringResourceValue("values-en/strings.xml", "kp_info_sources_body"),
        )
        assertEquals("Geomagnetic Storm Scale", stringResourceValue("values-en/strings.xml", "kp_info_levels_title"))
        assertEquals("G1 (Minor)", stringResourceValue("values-en/strings.xml", "kp_info_level_g1_name"))
        assertEquals("G5 (Extreme)", stringResourceValue("values-en/strings.xml", "kp_info_level_g5_name"))
        assertEquals("Minor geomagnetic storm", stringResourceValue("values-en/strings.xml", "kp_level_g1_desc"))
        assertEquals("Extreme geomagnetic storm", stringResourceValue("values-en/strings.xml", "kp_level_g5_desc"))
        assertEquals("Avoid flight recommended, GPS unusable", stringResourceValue("values-en/strings.xml", "kp_info_level_g5_advice"))
    }

    @Test
    fun `weather info guide strings match iOS localizations`() {
        assertEquals("날씨 정보", stringResourceValue("values/strings.xml", "weather_info_title"))
        assertEquals("날씨와 드론 비행", stringResourceValue("values/strings.xml", "weather_info_importance_title"))
        assertEquals(
            "날씨는 드론 비행 안전에 가장 직접적인 영향을 미치는 요소입니다.",
            stringResourceValue("values/strings.xml", "weather_info_importance_body"),
        )
        assertEquals("안전한 비행을 위한 조언", stringResourceValue("values/strings.xml", "weather_info_safety_title"))
        assertEquals("악천후 예상 시 비행 연기", stringResourceValue("values/strings.xml", "weather_info_safety_bullet_postpone"))
        assertEquals("위치 정확도 안내", stringResourceValue("values/strings.xml", "weather_info_location_title"))
        assertEquals("현재 Wi-Fi 기반 위치 사용 중 (정확도: ±%1\$dm)", stringResourceValue("values/strings.xml", "weather_info_location_wifi"))
        assertEquals(
            "GPS가 없는 기기에서는 날씨 정보가 실제와 다를 수 있습니다",
            stringResourceValue("values/strings.xml", "weather_info_location_bullet_weather_difference"),
        )
        assertEquals("풍속", stringResourceValue("values/strings.xml", "weather_info_wind_title"))
        assertEquals("0-%1$.1f m/s (안전)", stringResourceValue("values/strings.xml", "weather_info_wind_safe_range"))
        assertEquals("순간 풍속 증가량", stringResourceValue("values/strings.xml", "weather_info_gust_title"))
        assertEquals("국지 돌풍 위험", stringResourceValue("values/strings.xml", "weather_gust_warning"))
        assertEquals("방수 드론 외 비행 자제", stringResourceValue("values/strings.xml", "weather_info_precipitation_caution_advice"))
        assertEquals("%1\$d km 미만", stringResourceValue("values/strings.xml", "weather_info_visibility_range_danger"))
        assertEquals("%1\$d°C 미만, 40°C 이상", stringResourceValue("values/strings.xml", "weather_info_temperature_range_danger"))
        assertEquals("결로위험지수", stringResourceValue("values/strings.xml", "weather_info_cri_title"))

        assertEquals("Weather Information", stringResourceValue("values-en/strings.xml", "weather_info_title"))
        assertEquals("Weather & Drone Flight", stringResourceValue("values-en/strings.xml", "weather_info_importance_title"))
        assertEquals(
            "Weather is the most direct factor affecting drone flight safety.",
            stringResourceValue("values-en/strings.xml", "weather_info_importance_body"),
        )
        assertEquals("Tips for Safe Flight", stringResourceValue("values-en/strings.xml", "weather_info_safety_title"))
        assertEquals("Location Accuracy Notice", stringResourceValue("values-en/strings.xml", "weather_info_location_title"))
        assertEquals(
            "Currently using Wi-Fi-based location (accuracy: ±%1\$dm)",
            stringResourceValue("values-en/strings.xml", "weather_info_location_wifi"),
        )
        assertEquals("Wind Speed", stringResourceValue("values-en/strings.xml", "weather_info_wind_title"))
        assertEquals("0-%1$.1f m/s (Safe)", stringResourceValue("values-en/strings.xml", "weather_info_wind_safe_range"))
        assertEquals("Gust Difference", stringResourceValue("values-en/strings.xml", "weather_info_gust_title"))
        assertEquals("Local gust risk", stringResourceValue("values-en/strings.xml", "weather_gust_warning"))
        assertEquals("1.0 mm or more", stringResourceValue("values-en/strings.xml", "weather_info_precipitation_range_danger"))
        assertEquals("Less than %1\$d km", stringResourceValue("values-en/strings.xml", "weather_info_visibility_range_danger"))
        assertEquals("Below %1\$d°C, above 40°C", stringResourceValue("values-en/strings.xml", "weather_info_temperature_range_danger"))
        assertEquals("CRI (Condensation Risk Index)", stringResourceValue("values-en/strings.xml", "weather_info_cri_title"))
    }

    @Test
    fun `flight zone layer selector strings match iOS localizations`() {
        assertEquals(
            "본 서비스는 국토교통부 디지털트윈국토 정보를 기반으로 제공하며, 실제 비행 허가 및 안전에 대한 책임은 사용자에게 있습니다.",
            stringResourceValue("values/strings.xml", "flight_zone_legal_disclaimer"),
        )
        assertEquals("문화재보호구역", stringResourceValue("values/strings.xml", "flight_zone_layer_cultural_heritage"))
        assertEquals("비행금지구역", stringResourceValue("values/strings.xml", "flight_zone_layer_prohibited_zone"))
        assertEquals("사전협의구역", stringResourceValue("values/strings.xml", "flight_zone_layer_consultation_zone"))
        assertEquals("초경량비행장치공역", stringResourceValue("values/strings.xml", "flight_zone_layer_ultra_light_zone"))
        assertEquals(
            "비행구역 데이터를 불러올 수 없습니다",
            stringResourceValue("values/strings.xml", "map_flight_zones_error_load_failed"),
        )

        assertEquals(
            "This service provides information based on the Korean government's Digital Twin National Land data. Users are responsible for actual flight permits and safety.",
            androidDisplayStringResourceValue("values-en/strings.xml", "flight_zone_legal_disclaimer"),
        )
        assertEquals("Select All", stringResourceValue("values-en/strings.xml", "flight_zone_select_all"))
        assertEquals("Deselect All", stringResourceValue("values-en/strings.xml", "flight_zone_deselect_all"))
        assertEquals("Selected Layers", stringResourceValue("values-en/strings.xml", "flight_zone_stats_selected"))
        assertEquals("Displayed Zones", stringResourceValue("values-en/strings.xml", "flight_zone_stats_displayed"))
        assertEquals("Boundary Zone", stringResourceValue("values-en/strings.xml", "flight_zone_layer_boundary_zone"))
        assertEquals(
            "Cultural Heritage Protection Zone",
            stringResourceValue("values-en/strings.xml", "flight_zone_layer_cultural_heritage"),
        )
        assertEquals("Light Aircraft Airfield", stringResourceValue("values-en/strings.xml", "flight_zone_layer_light_aircraft_zone"))
        assertEquals("Temporary Prohibited Zone", stringResourceValue("values-en/strings.xml", "flight_zone_layer_temporary_prohibited"))
        assertEquals(
            "Unable to load flight zone data",
            stringResourceValue("values-en/strings.xml", "map_flight_zones_error_load_failed"),
        )
    }

    @Test
    fun `vworld zone detail strings match iOS localizations`() {
        assertEquals("구역 상세 정보", stringResourceValue("values/strings.xml", "zone_detail_navigation_title"))
        assertEquals("관리 기관", stringResourceValue("values/strings.xml", "zone_detail_authority_title"))
        assertEquals("남은 기간", stringResourceValue("values/strings.xml", "zone_detail_notam_remaining"))
        assertEquals("활성", stringResourceValue("values/strings.xml", "zone_detail_notam_status_active"))
        assertEquals("만료됨", stringResourceValue("values/strings.xml", "zone_detail_notam_status_expired"))
        assertEquals("%d일", pluralResourceValue("values/strings.xml", "zone_detail_notam_days", "other"))

        assertEquals("Zone Details", stringResourceValue("values-en/strings.xml", "zone_detail_navigation_title"))
        assertEquals("Zone Type", stringResourceValue("values-en/strings.xml", "zone_detail_zone_type"))
        assertEquals("Zone Code", stringResourceValue("values-en/strings.xml", "zone_detail_code"))
        assertEquals("Remaining Period", stringResourceValue("values-en/strings.xml", "zone_detail_notam_remaining"))
        assertEquals("%d day", pluralResourceValue("values-en/strings.xml", "zone_detail_notam_days", "one"))
        assertEquals("%d days", pluralResourceValue("values-en/strings.xml", "zone_detail_notam_days", "other"))
        assertEquals("Managing Authority", stringResourceValue("values-en/strings.xml", "zone_detail_authority_title"))
        assertEquals("Organization Name", stringResourceValue("values-en/strings.xml", "zone_detail_authority_name"))
        assertEquals("Administrative District", stringResourceValue("values-en/strings.xml", "zone_detail_heritage_address"))
        assertEquals("Designation", stringResourceValue("values-en/strings.xml", "zone_detail_heritage_designation"))
        assertEquals("%1\$sYear%2\$sNo.", stringResourceValue("values-en/strings.xml", "zone_detail_heritage_designation_format"))
    }

    @Test
    fun `sketch toolbar strings match iOS localizations`() {
        assertEquals("완료", stringResourceValue("values/strings.xml", "sketch_done"))
        assertEquals("모든 스케치 삭제", stringResourceValue("values/strings.xml", "sketch_delete_all_title"))
        assertEquals(
            "%d개의 스케치를 모두 삭제하시겠습니까?",
            pluralResourceValue("values/strings.xml", "sketch_delete_all_confirm", "other"),
        )

        assertEquals("Done", stringResourceValue("values-en/strings.xml", "sketch_done"))
        assertEquals("Delete All Sketches", stringResourceValue("values-en/strings.xml", "sketch_delete_all_title"))
        assertEquals(
            "Are you sure you want to delete all %d sketches?",
            pluralResourceValue("values-en/strings.xml", "sketch_delete_all_confirm", "one"),
        )
        assertEquals(
            "Are you sure you want to delete all %d sketches?",
            pluralResourceValue("values-en/strings.xml", "sketch_delete_all_confirm", "other"),
        )
    }

    @Test
    fun `profile sync and account strings match iOS localizations`() {
        assertEquals("내 정보", stringResourceValue("values/strings.xml", "profile_section_my_info"))
        assertEquals("이메일", stringResourceValue("values/strings.xml", "profile_info_email"))
        assertEquals("비공개", stringResourceValue("values/strings.xml", "profile_info_email_hidden"))
        assertEquals("로그인", stringResourceValue("values/strings.xml", "profile_info_login_method"))
        assertEquals("가입일", stringResourceValue("values/strings.xml", "profile_info_join_date"))
        assertEquals("가입일 미상", stringResourceValue("values/strings.xml", "profile_info_join_unknown"))
        assertEquals("활성화된 도형", stringResourceValue("values/strings.xml", "profile_info_shapes"))
        assertEquals("스케치", stringResourceValue("values/strings.xml", "profile_info_sketches"))
        assertEquals("드론", stringResourceValue("values/strings.xml", "profile_info_drones"))
        assertEquals("만료된 도형", stringResourceValue("values/strings.xml", "profile_info_expired_shapes"))
        assertEquals("%1\$d개", stringResourceValue("values/strings.xml", "profile_info_count_unit"))
        assertEquals("실시간 클라우드 동기화", stringResourceValue("values/strings.xml", "profile_sync_cloud"))
        assertEquals("동기화 중...", stringResourceValue("values/strings.xml", "profile_sync_in_progress"))
        assertEquals("로그인이 필요합니다", stringResourceValue("values/strings.xml", "profile_sync_login_required"))
        assertEquals("비활성화됨", stringResourceValue("values/strings.xml", "profile_sync_disabled"))
        assertEquals("활성화 - 실시간 동기화중", stringResourceValue("values/strings.xml", "profile_sync_active"))
        assertEquals("활성화 - 실시간 동기화 대기중", stringResourceValue("values/strings.xml", "profile_sync_waiting"))
        assertEquals("동기화 기록이 없습니다.", stringResourceValue("values/strings.xml", "profile_sync_no_history"))
        assertEquals("수동 백업하기", stringResourceValue("values/strings.xml", "profile_backup_manual"))
        assertEquals(
            "%1\$d개 도형의 동기화가 완료되었습니다.",
            stringResourceValue("values/strings.xml", "profile_sync_success"),
        )
        assertEquals("실시간 클라우드 동기화에 실패했습니다: %1\$s", stringResourceValue("values/strings.xml", "profile_sync_failed"))
        assertEquals(
            "실시간 클라우드 동기화를 사용하려면 먼저 로그인해주세요.",
            stringResourceValue("values/strings.xml", "profile_sync_footer_login_required"),
        )
        assertEquals(
            "활성화하면 같은 계정으로 로그인한 모든 기기에서 도형 데이터가 실시간으로 동기화 및 백업됩니다.",
            stringResourceValue("values/strings.xml", "profile_sync_footer_enable_info"),
        )
        assertEquals("회원 탈퇴", stringResourceValue("values/strings.xml", "profile_account_delete"))
        assertEquals(
            "탈퇴 시 계정만 삭제되며, 로컬 데이터는 계속 사용할 수 있습니다.",
            stringResourceValue("values/strings.xml", "profile_account_delete_desc"),
        )
        assertEquals("탈퇴하기", stringResourceValue("values/strings.xml", "profile_delete_account_button"))
        assertEquals(
            "계정이 삭제되고\\n클라우드 동기화가 중단됩니다.\\n\\n기기에 저장된 도형과 드론은\\n계속 사용할 수 있습니다.\\n\\n정말 탈퇴하시겠습니까?",
            stringResourceValue("values/strings.xml", "profile_delete_account_message"),
        )
        assertEquals("최종 확인", stringResourceValue("values/strings.xml", "profile_delete_account_final_title"))
        assertEquals("이 작업은 되돌릴 수 없습니다.", stringResourceValue("values/strings.xml", "profile_delete_account_final_message"))
        assertEquals("영구 탈퇴", stringResourceValue("values/strings.xml", "profile_delete_account_final_button"))
        assertEquals("탈퇴 실패", stringResourceValue("values/strings.xml", "profile_delete_account_error_title"))
        assertEquals("회원 탈퇴가 완료되었습니다.", stringResourceValue("values/strings.xml", "profile_delete_account_success"))
        assertEquals(
            "회원 탈퇴에 실패했습니다.",
            stringResourceValue("values/strings.xml", "profile_delete_account_error"),
        )
        assertEquals(
            "보안을 위해 다시 로그인한 후 탈퇴해주세요.",
            stringResourceValue("values/strings.xml", "profile_delete_account_requires_recent_login"),
        )
        assertEquals("로그아웃하시겠습니까?", stringResourceValue("values/strings.xml", "profile_logout_message"))

        assertEquals("My Info", stringResourceValue("values-en/strings.xml", "profile_section_my_info"))
        assertEquals("Email", stringResourceValue("values-en/strings.xml", "profile_info_email"))
        assertEquals("Hidden", stringResourceValue("values-en/strings.xml", "profile_info_email_hidden"))
        assertEquals("Login", stringResourceValue("values-en/strings.xml", "profile_info_login_method"))
        assertEquals("Joined", stringResourceValue("values-en/strings.xml", "profile_info_join_date"))
        assertEquals("Unknown", stringResourceValue("values-en/strings.xml", "profile_info_join_unknown"))
        assertEquals("Active Shapes", stringResourceValue("values-en/strings.xml", "profile_info_shapes"))
        assertEquals("Sketches", stringResourceValue("values-en/strings.xml", "profile_info_sketches"))
        assertEquals("Drones", stringResourceValue("values-en/strings.xml", "profile_info_drones"))
        assertEquals("Expired Shapes", stringResourceValue("values-en/strings.xml", "profile_info_expired_shapes"))
        assertEquals("%1\$d", stringResourceValue("values-en/strings.xml", "profile_info_count_unit"))
        assertEquals("Real-time cloud sync", stringResourceValue("values-en/strings.xml", "profile_sync_cloud"))
        assertEquals("Syncing...", stringResourceValue("values-en/strings.xml", "profile_sync_in_progress"))
        assertEquals("Login required", stringResourceValue("values-en/strings.xml", "profile_sync_login_required"))
        assertEquals("Disabled", stringResourceValue("values-en/strings.xml", "profile_sync_disabled"))
        assertEquals("Active - Real-time syncing", stringResourceValue("values-en/strings.xml", "profile_sync_active"))
        assertEquals("Active - Waiting for sync", stringResourceValue("values-en/strings.xml", "profile_sync_waiting"))
        assertEquals("No sync history", stringResourceValue("values-en/strings.xml", "profile_sync_no_history"))
        assertEquals("Manual backup", stringResourceValue("values-en/strings.xml", "profile_backup_manual"))
        assertEquals("Sync completed for %1\$d shapes.", stringResourceValue("values-en/strings.xml", "profile_sync_success"))
        assertEquals("Sync failed: %1\$s", stringResourceValue("values-en/strings.xml", "profile_sync_failed"))
        assertEquals(
            "Please log in to use real-time cloud sync.",
            stringResourceValue("values-en/strings.xml", "profile_sync_footer_login_required"),
        )
        assertEquals(
            "When enabled, shape data will be synced and backed up in real-time across all devices with the same account.",
            stringResourceValue("values-en/strings.xml", "profile_sync_footer_enable_info"),
        )
        assertEquals(
            "Only the account will be deleted. Local data will remain available.",
            stringResourceValue("values-en/strings.xml", "profile_account_delete_desc"),
        )
        assertEquals("Are you sure you want to sign out?", stringResourceValue("values-en/strings.xml", "profile_logout_message"))
        assertEquals("Delete", stringResourceValue("values-en/strings.xml", "profile_delete_account_button"))
        assertEquals(
            "Your account will be deleted and cloud sync will stop.\\n\\nShapes and drones saved on this device will remain available.\\n\\nAre you sure you want to delete your account?",
            stringResourceValue("values-en/strings.xml", "profile_delete_account_message"),
        )
        assertEquals("Final Confirmation", stringResourceValue("values-en/strings.xml", "profile_delete_account_final_title"))
        assertEquals("This action cannot be undone.", stringResourceValue("values-en/strings.xml", "profile_delete_account_final_message"))
        assertEquals("Permanently Delete", stringResourceValue("values-en/strings.xml", "profile_delete_account_final_button"))
        assertEquals("Deletion Failed", stringResourceValue("values-en/strings.xml", "profile_delete_account_error_title"))
        assertEquals("Account deleted successfully.", stringResourceValue("values-en/strings.xml", "profile_delete_account_success"))
        assertEquals(
            "Failed to delete account.",
            stringResourceValue("values-en/strings.xml", "profile_delete_account_error"),
        )
        assertEquals(
            "For security, please sign in again before deleting your account.",
            stringResourceValue("values-en/strings.xml", "profile_delete_account_requires_recent_login"),
        )
    }

    @Test
    fun `settings main row strings match iOS localizations`() {
        assertEquals("내 정보", stringResourceValue("values/strings.xml", "settings_section_my_info"))
        assertEquals("내 프로필", stringResourceValue("values/strings.xml", "profile_title"))
        assertEquals("로그인 / 회원가입", stringResourceValue("values/strings.xml", "login_title"))
        assertEquals("내 드론 관리하기", stringResourceValue("values/strings.xml", "settings_drone_manage"))
        assertEquals("비행 환경", stringResourceValue("values/strings.xml", "settings_section_flight_environment"))
        assertEquals("현재 날씨", stringResourceValue("values/strings.xml", "settings_weather_current"))
        assertEquals("알림", stringResourceValue("values/strings.xml", "settings_section_notifications"))
        assertEquals("지도 표시", stringResourceValue("values/strings.xml", "settings_section_map_display"))
        assertEquals("앱", stringResourceValue("values/strings.xml", "settings_section_app_info"))
        assertEquals("앱 정보", stringResourceValue("values/strings.xml", "settings_app_intro"))
        assertEquals("패치노트", stringResourceValue("values/strings.xml", "settings_patch_notes"))
        assertEquals("언어", stringResourceValue("values/strings.xml", "settings_language"))
        assertEquals("한국어", stringResourceValue("values/strings.xml", "settings_language_korean"))
        assertEquals("English", stringResourceValue("values/strings.xml", "settings_language_english"))
        assertEquals("현재 KP 지수: %1\$s", stringResourceValue("values/strings.xml", "settings_kp_index_current"))
        assertEquals("화면 항상 켜놓기", stringResourceValue("values/strings.xml", "settings_keep_screen_awake"))
        assertEquals(
            "앱 사용 중 화면이 자동으로 꺼지지 않습니다.",
            stringResourceValue("values/strings.xml", "settings_keep_screen_awake_subtitle"),
        )
        assertEquals("도형 만료일 알림", stringResourceValue("values/strings.xml", "settings_end_date_alarm"))
        assertEquals(
            "도형 종료일 7일전 알림을 받습니다.",
            stringResourceValue("values/strings.xml", "settings_end_date_alarm_subtitle"),
        )
        assertEquals(
            "일출 30분전, 10분전 알림을 받습니다.",
            stringResourceValue("values/strings.xml", "settings_sunrise_alarm_subtitle"),
        )
        assertEquals(
            "일몰 30분전, 10분전 알림을 받습니다.",
            stringResourceValue("values/strings.xml", "settings_sunset_alarm_subtitle"),
        )
        assertEquals(
            "알림 권한이 필요합니다",
            stringResourceValue("values/strings.xml", "notification_permission_title"),
        )
        assertEquals(
            "정확한 알람 권한이 필요합니다",
            stringResourceValue("values/strings.xml", "exact_alarm_permission_title"),
        )
        assertEquals(
            "DronePass 알림",
            stringResourceValue("values/strings.xml", "notification_channel_default_name"),
        )
        assertEquals(
            "비행 시각 알림",
            stringResourceValue("values/strings.xml", "notification_channel_time_sensitive_name"),
        )
        assertEquals("일출 30분 전", stringResourceValue("values/strings.xml", "notification_sunrise_30min_title"))
        assertEquals("일출까지 30분 남았습니다.", stringResourceValue("values/strings.xml", "notification_sunrise_30min_body"))
        assertEquals("일출 10분 전", stringResourceValue("values/strings.xml", "notification_sunrise_10min_title"))
        assertEquals("일출까지 10분 남았습니다.", stringResourceValue("values/strings.xml", "notification_sunrise_10min_body"))
        assertEquals("일몰 30분 전", stringResourceValue("values/strings.xml", "notification_sunset_30min_title"))
        assertEquals("일몰까지 30분 남았습니다.", stringResourceValue("values/strings.xml", "notification_sunset_30min_body"))
        assertEquals("일몰 10분 전", stringResourceValue("values/strings.xml", "notification_sunset_10min_title"))
        assertEquals("일몰까지 10분 남았습니다.", stringResourceValue("values/strings.xml", "notification_sunset_10min_body"))
        assertEquals("도형 종료일 알림", stringResourceValue("values/strings.xml", "notification_end_date_title"))
        assertEquals(
            "도형 '%1\$s'의 종료일이 7일 남았습니다.",
            androidDisplayStringResourceValue("values/strings.xml", "notification_end_date_body_with_title"),
        )

        assertEquals("My Info", stringResourceValue("values-en/strings.xml", "settings_section_my_info"))
        assertEquals("My Profile", stringResourceValue("values-en/strings.xml", "profile_title"))
        assertEquals("Sign In / Sign Up", stringResourceValue("values-en/strings.xml", "login_title"))
        assertEquals("Manage My Drones", stringResourceValue("values-en/strings.xml", "settings_drone_manage"))
        assertEquals("Flight Environment", stringResourceValue("values-en/strings.xml", "settings_section_flight_environment"))
        assertEquals("Current Weather", stringResourceValue("values-en/strings.xml", "settings_weather_current"))
        assertEquals("Notifications", stringResourceValue("values-en/strings.xml", "settings_section_notifications"))
        assertEquals("Map Display", stringResourceValue("values-en/strings.xml", "settings_section_map_display"))
        assertEquals("App", stringResourceValue("values-en/strings.xml", "settings_section_app_info"))
        assertEquals("App Info", stringResourceValue("values-en/strings.xml", "settings_app_intro"))
        assertEquals("Patch Notes", stringResourceValue("values-en/strings.xml", "settings_patch_notes"))
        assertEquals("Language", stringResourceValue("values-en/strings.xml", "settings_language"))
        assertEquals("한국어", stringResourceValue("values-en/strings.xml", "settings_language_korean"))
        assertEquals("English", stringResourceValue("values-en/strings.xml", "settings_language_english"))
        assertEquals("Current KP Index: %1\$s", stringResourceValue("values-en/strings.xml", "settings_kp_index_current"))
        assertEquals(
            "Screen won't turn off automatically while using the app.",
            androidDisplayStringResourceValue("values-en/strings.xml", "settings_keep_screen_awake_subtitle"),
        )
        assertEquals("Shape expiration notifications", stringResourceValue("values-en/strings.xml", "settings_end_date_alarm"))
        assertEquals(
            "Receive notifications 7 days before shape expiration.",
            stringResourceValue("values-en/strings.xml", "settings_end_date_alarm_subtitle"),
        )
        assertEquals("Sunrise notifications", stringResourceValue("values-en/strings.xml", "settings_sunrise_alarm"))
        assertEquals(
            "Receive notifications 30 and 10 minutes before sunrise.",
            stringResourceValue("values-en/strings.xml", "settings_sunrise_alarm_subtitle"),
        )
        assertEquals("Sunset notifications", stringResourceValue("values-en/strings.xml", "settings_sunset_alarm"))
        assertEquals(
            "Receive notifications 30 and 10 minutes before sunset.",
            stringResourceValue("values-en/strings.xml", "settings_sunset_alarm_subtitle"),
        )
        assertEquals(
            "Notification permission required",
            stringResourceValue("values-en/strings.xml", "notification_permission_title"),
        )
        assertEquals(
            "Exact alarm permission required",
            stringResourceValue("values-en/strings.xml", "exact_alarm_permission_title"),
        )
        assertEquals(
            "DronePass notifications",
            stringResourceValue("values-en/strings.xml", "notification_channel_default_name"),
        )
        assertEquals(
            "Flight time notifications",
            stringResourceValue("values-en/strings.xml", "notification_channel_time_sensitive_name"),
        )
        assertEquals(
            "30 minutes before sunrise",
            stringResourceValue("values-en/strings.xml", "notification_sunrise_30min_title"),
        )
        assertEquals(
            "30 minutes until sunrise.",
            stringResourceValue("values-en/strings.xml", "notification_sunrise_30min_body"),
        )
        assertEquals(
            "10 minutes before sunset",
            stringResourceValue("values-en/strings.xml", "notification_sunset_10min_title"),
        )
        assertEquals(
            "10 minutes until sunset.",
            stringResourceValue("values-en/strings.xml", "notification_sunset_10min_body"),
        )
        assertEquals(
            "10 minutes before sunrise",
            stringResourceValue("values-en/strings.xml", "notification_sunrise_10min_title"),
        )
        assertEquals(
            "30 minutes before sunset",
            stringResourceValue("values-en/strings.xml", "notification_sunset_30min_title"),
        )
    }

    @Test
    fun `KP forecast sheet strings match iOS localizations`() {
        assertEquals("KP 지수", stringResourceValue("values/strings.xml", "kp_title"))
        assertEquals("현재 KP 지수", stringResourceValue("values/strings.xml", "kp_current"))
        assertEquals("데이터가 없습니다", stringResourceValue("values/strings.xml", "kp_no_data"))
        assertEquals("KP 지수 데이터를 가져올 수 없습니다", stringResourceValue("values/strings.xml", "kp_error_load_failed"))
        assertEquals("KP 지수 데이터를 가져올 수 없습니다", stringResourceValue("values/strings.xml", "kp_error_forecast_failed"))
        assertEquals("KP 지수 처리 중 오류가 발생했습니다", stringResourceValue("values/strings.xml", "kp_error_unknown"))
        assertEquals("KP 지수 예보", stringResourceValue("values/strings.xml", "kp_navigation_title"))
        assertEquals("현재 KP 지수", stringResourceValue("values/strings.xml", "kp_section_current"))
        assertEquals("향후 48시간 예보", stringResourceValue("values/strings.xml", "kp_section_forecast48"))
        assertEquals("장기 예보 (27일)", stringResourceValue("values/strings.xml", "kp_section_long_term"))
        assertEquals("현재 시간대 기준", stringResourceValue("values/strings.xml", "kp_forecast_note"))
        assertEquals("UTC 기준", stringResourceValue("values/strings.xml", "kp_forecast27_note"))
        assertEquals("Data: GFZ Potsdam", stringResourceValue("values/strings.xml", "kp_data_source_gfz"))
        assertEquals("Data: NOAA SWPC", stringResourceValue("values/strings.xml", "kp_data_source_noaa"))
        assertEquals("Normal (0-5)", stringResourceValue("values/strings.xml", "kp_legend_normal"))
        assertEquals("G1 (5-6)", stringResourceValue("values/strings.xml", "kp_legend_g1"))
        assertEquals("G2 (6-7)", stringResourceValue("values/strings.xml", "kp_legend_g2"))
        assertEquals("G3 (7-8)", stringResourceValue("values/strings.xml", "kp_legend_g3"))
        assertEquals("G4 (8-9)", stringResourceValue("values/strings.xml", "kp_legend_g4"))
        assertEquals("G5 (≥9)", stringResourceValue("values/strings.xml", "kp_legend_g5"))

        assertEquals("KP Index", stringResourceValue("values-en/strings.xml", "kp_title"))
        assertEquals("Current KP Index", stringResourceValue("values-en/strings.xml", "kp_current"))
        assertEquals("No data available", stringResourceValue("values-en/strings.xml", "kp_no_data"))
        assertEquals("Unable to load KP index data", stringResourceValue("values-en/strings.xml", "kp_error_load_failed"))
        assertEquals("Unable to load KP index data", stringResourceValue("values-en/strings.xml", "kp_error_forecast_failed"))
        assertEquals(
            "An error occurred while processing KP index",
            stringResourceValue("values-en/strings.xml", "kp_error_unknown"),
        )
        assertEquals("KP Index Forecast", stringResourceValue("values-en/strings.xml", "kp_navigation_title"))
        assertEquals("Current KP Index", stringResourceValue("values-en/strings.xml", "kp_section_current"))
        assertEquals("48-Hour Forecast", stringResourceValue("values-en/strings.xml", "kp_section_forecast48"))
        assertEquals("Long-term Forecast (27 days)", stringResourceValue("values-en/strings.xml", "kp_section_long_term"))
        assertEquals("Based on current time", stringResourceValue("values-en/strings.xml", "kp_forecast_note"))
        assertEquals("UTC time", stringResourceValue("values-en/strings.xml", "kp_forecast27_note"))
        assertEquals("Data: GFZ Potsdam", stringResourceValue("values-en/strings.xml", "kp_data_source_gfz"))
        assertEquals("Data: NOAA SWPC", stringResourceValue("values-en/strings.xml", "kp_data_source_noaa"))
        assertEquals("Normal (0-5)", stringResourceValue("values-en/strings.xml", "kp_legend_normal"))
        assertEquals("G1 (5-6)", stringResourceValue("values-en/strings.xml", "kp_legend_g1"))
        assertEquals("G2 (6-7)", stringResourceValue("values-en/strings.xml", "kp_legend_g2"))
        assertEquals("G3 (7-8)", stringResourceValue("values-en/strings.xml", "kp_legend_g3"))
        assertEquals("G4 (8-9)", stringResourceValue("values-en/strings.xml", "kp_legend_g4"))
        assertEquals("G5 (≥9)", stringResourceValue("values-en/strings.xml", "kp_legend_g5"))
    }

    @Test
    fun `weather forecast sheet strings match iOS localizations`() {
        assertEquals("현위치 기반 정보", stringResourceValue("values/strings.xml", "weather_navigation_title"))
        assertEquals("일출/일몰 정보", stringResourceValue("values/strings.xml", "weather_section_sunrise_sunset"))
        assertEquals("현재 날씨", stringResourceValue("values/strings.xml", "weather_section_current"))
        assertEquals("드론 무게:", stringResourceValue("values/strings.xml", "weather_drone_weight_label"))
        assertEquals("알 수 없음", stringResourceValue("values/strings.xml", "weather_unknown"))
        assertEquals("강설량", stringResourceValue("values/strings.xml", "weather_snowfall"))
        assertEquals("맑음", stringResourceValue("values/strings.xml", "weather_condition_clear"))
        assertEquals("눈소나기", stringResourceValue("values/strings.xml", "weather_condition_snow_showers"))
        assertEquals("우박을 동반한 뇌우", stringResourceValue("values/strings.xml", "weather_condition_thunderstorm_hail"))
        assertEquals("일몰까지", stringResourceValue("values/strings.xml", "weather_until_sunset"))
        assertEquals("일출까지", stringResourceValue("values/strings.xml", "weather_until_sunrise"))
        assertEquals("%1\$d시간 %2\$d분", stringResourceValue("values/strings.xml", "weather_time_hours"))
        assertEquals("%1\$d분", stringResourceValue("values/strings.xml", "weather_time_minutes"))
        assertEquals("남음", stringResourceValue("values/strings.xml", "weather_remaining"))
        assertEquals(
            "주의 및 경고 아이콘은 참고용입니다. 비행 전 현장 상황을 반드시 확인하세요.",
            stringResourceValue("values/strings.xml", "weather_disclaimer"),
        )
        assertEquals("마지막 업데이트: %1\$s", stringResourceValue("values/strings.xml", "weather_last_update"))
        assertEquals("새로고침되었습니다.", stringResourceValue("values/strings.xml", "weather_refresh"))
        assertEquals(
            "날씨 정보를 가져오는데 실패했습니다: %1\$s",
            stringResourceValue("values/strings.xml", "weather_error_load_failed_detail"),
        )
        assertEquals("북", stringResourceValue("values/strings.xml", "weather_direction_n"))
        assertEquals("북동", stringResourceValue("values/strings.xml", "weather_direction_ne"))
        assertEquals("동", stringResourceValue("values/strings.xml", "weather_direction_e"))
        assertEquals("남동", stringResourceValue("values/strings.xml", "weather_direction_se"))
        assertEquals("남", stringResourceValue("values/strings.xml", "weather_direction_s"))
        assertEquals("남서", stringResourceValue("values/strings.xml", "weather_direction_sw"))
        assertEquals("서", stringResourceValue("values/strings.xml", "weather_direction_w"))
        assertEquals("북서", stringResourceValue("values/strings.xml", "weather_direction_nw"))
        assertEquals("온도 예보 (%1\$s)", stringResourceValue("values/strings.xml", "weather_chart_temperature"))
        assertEquals("풍속 예보 (%1\$s)", stringResourceValue("values/strings.xml", "weather_chart_wind_speed"))
        assertEquals("순간풍속증가량 예보 (%1\$s)", stringResourceValue("values/strings.xml", "weather_chart_gust_difference"))
        assertEquals("강수량 예보 (%1\$s)", stringResourceValue("values/strings.xml", "weather_chart_precipitation"))
        assertEquals("가시거리 예보 (%1\$s)", stringResourceValue("values/strings.xml", "weather_chart_visibility"))
        assertEquals("결로위험지수 예보 (%1\$s)", stringResourceValue("values/strings.xml", "weather_chart_cri"))
        assertEquals("현재", stringResourceValue("values/strings.xml", "weather_chart_current"))
        assertEquals("%1\$d일", stringResourceValue("values/strings.xml", "weather_forecast_days"))
        assertEquals("%1\$d시간", stringResourceValue("values/strings.xml", "weather_forecast_hours"))
        assertEquals("저온 ≤ %1\$d°C", stringResourceValue("values/strings.xml", "weather_legend_low_temp"))
        assertEquals("고온 ≥ %1\$d°C", stringResourceValue("values/strings.xml", "weather_legend_high_temp"))
        assertEquals("주의 ≥ %1\$.1fm/s", stringResourceValue("values/strings.xml", "weather_legend_caution"))
        assertEquals("위험 ≥ %1\$.1fm/s", stringResourceValue("values/strings.xml", "weather_legend_danger"))
        assertEquals("주의 ≥ 40", stringResourceValue("values/strings.xml", "weather_legend_cri_caution"))
        assertEquals("경고 ≥ 70", stringResourceValue("values/strings.xml", "weather_legend_cri_warning"))
        assertEquals("위험 ≤ %1\$dkm", stringResourceValue("values/strings.xml", "weather_legend_poor"))
        assertEquals("양호 ≥ %1\$dkm", stringResourceValue("values/strings.xml", "weather_legend_good"))
        assertEquals("단위: °C", stringResourceValue("values/strings.xml", "weather_unit_celsius"))
        assertEquals("단위: m/s", stringResourceValue("values/strings.xml", "weather_unit_mps"))
        assertEquals("단위: mm/h", stringResourceValue("values/strings.xml", "weather_unit_mmph"))
        assertEquals("단위: km", stringResourceValue("values/strings.xml", "weather_unit_km"))
        assertEquals("단위: 자체단위사용", stringResourceValue("values/strings.xml", "weather_unit_custom"))

        assertEquals("Location-based Information", stringResourceValue("values-en/strings.xml", "weather_navigation_title"))
        assertEquals("Sunrise/Sunset", stringResourceValue("values-en/strings.xml", "weather_section_sunrise_sunset"))
        assertEquals("Current Weather", stringResourceValue("values-en/strings.xml", "weather_section_current"))
        assertEquals("Drone Weight:", stringResourceValue("values-en/strings.xml", "weather_drone_weight_label"))
        assertEquals("Unknown", stringResourceValue("values-en/strings.xml", "weather_unknown"))
        assertEquals("Snowfall", stringResourceValue("values-en/strings.xml", "weather_snowfall"))
        assertEquals("Clear", stringResourceValue("values-en/strings.xml", "weather_condition_clear"))
        assertEquals("Snow Showers", stringResourceValue("values-en/strings.xml", "weather_condition_snow_showers"))
        assertEquals("Thunderstorm with Hail", stringResourceValue("values-en/strings.xml", "weather_condition_thunderstorm_hail"))
        assertEquals("Until sunset", stringResourceValue("values-en/strings.xml", "weather_until_sunset"))
        assertEquals("Until sunrise", stringResourceValue("values-en/strings.xml", "weather_until_sunrise"))
        assertEquals("%1\$d hours %2\$d minutes", stringResourceValue("values-en/strings.xml", "weather_time_hours"))
        assertEquals("%1\$d minutes", stringResourceValue("values-en/strings.xml", "weather_time_minutes"))
        assertEquals("remaining", stringResourceValue("values-en/strings.xml", "weather_remaining"))
        assertEquals(
            "Caution and warning icons are for reference only. Always check on-site conditions before flight.",
            stringResourceValue("values-en/strings.xml", "weather_disclaimer"),
        )
        assertEquals("Last updated: %1\$s", stringResourceValue("values-en/strings.xml", "weather_last_update"))
        assertEquals("Refreshed", stringResourceValue("values-en/strings.xml", "weather_refresh"))
        assertEquals(
            "Failed to fetch weather information: %1\$s",
            stringResourceValue("values-en/strings.xml", "weather_error_load_failed_detail"),
        )
        assertEquals("N", stringResourceValue("values-en/strings.xml", "weather_direction_n"))
        assertEquals("NE", stringResourceValue("values-en/strings.xml", "weather_direction_ne"))
        assertEquals("E", stringResourceValue("values-en/strings.xml", "weather_direction_e"))
        assertEquals("SE", stringResourceValue("values-en/strings.xml", "weather_direction_se"))
        assertEquals("S", stringResourceValue("values-en/strings.xml", "weather_direction_s"))
        assertEquals("SW", stringResourceValue("values-en/strings.xml", "weather_direction_sw"))
        assertEquals("W", stringResourceValue("values-en/strings.xml", "weather_direction_w"))
        assertEquals("NW", stringResourceValue("values-en/strings.xml", "weather_direction_nw"))
        assertEquals("Temperature Forecast (%1\$s)", stringResourceValue("values-en/strings.xml", "weather_chart_temperature"))
        assertEquals("Wind Speed Forecast (%1\$s)", stringResourceValue("values-en/strings.xml", "weather_chart_wind_speed"))
        assertEquals("Gust Difference Forecast (%1\$s)", stringResourceValue("values-en/strings.xml", "weather_chart_gust_difference"))
        assertEquals("Precipitation Forecast (%1\$s)", stringResourceValue("values-en/strings.xml", "weather_chart_precipitation"))
        assertEquals("Visibility Forecast (%1\$s)", stringResourceValue("values-en/strings.xml", "weather_chart_visibility"))
        assertEquals("CRI Forecast (%1\$s)", stringResourceValue("values-en/strings.xml", "weather_chart_cri"))
        assertEquals("Now", stringResourceValue("values-en/strings.xml", "weather_chart_current"))
        assertEquals("%1\$d Days", stringResourceValue("values-en/strings.xml", "weather_forecast_days"))
        assertEquals("%1\$d Hours", stringResourceValue("values-en/strings.xml", "weather_forecast_hours"))
        assertEquals("Low Temp ≤ %1\$d°C", stringResourceValue("values-en/strings.xml", "weather_legend_low_temp"))
        assertEquals("High Temp ≥ %1\$d°C", stringResourceValue("values-en/strings.xml", "weather_legend_high_temp"))
        assertEquals("Caution ≥ %1\$.1fm/s", stringResourceValue("values-en/strings.xml", "weather_legend_caution"))
        assertEquals("Danger ≥ %1\$.1fm/s", stringResourceValue("values-en/strings.xml", "weather_legend_danger"))
        assertEquals("Caution ≥ 40", stringResourceValue("values-en/strings.xml", "weather_legend_cri_caution"))
        assertEquals("Warning ≥ 70", stringResourceValue("values-en/strings.xml", "weather_legend_cri_warning"))
        assertEquals("Poor ≤ %1\$dkm", stringResourceValue("values-en/strings.xml", "weather_legend_poor"))
        assertEquals("Good ≥ %1\$dkm", stringResourceValue("values-en/strings.xml", "weather_legend_good"))
        assertEquals("Unit: °C", stringResourceValue("values-en/strings.xml", "weather_unit_celsius"))
        assertEquals("Unit: m/s", stringResourceValue("values-en/strings.xml", "weather_unit_mps"))
        assertEquals("Unit: mm/h", stringResourceValue("values-en/strings.xml", "weather_unit_mmph"))
        assertEquals("Unit: km", stringResourceValue("values-en/strings.xml", "weather_unit_km"))
        assertEquals("Unit: Custom", stringResourceValue("values-en/strings.xml", "weather_unit_custom"))
    }

    @Test
    fun `weather drone category strings match iOS localizations`() {
        assertEquals("250g 이하", stringResourceValue("values/strings.xml", "weather_drone_category_toy"))
        assertEquals(
            "Mini 시리즈\\n자격증 불필요",
            stringResourceValue("values/strings.xml", "weather_drone_category_toy_description"),
        )
        assertEquals("Mini 2, Mini 3 Pro, Mini 4 Pro", stringResourceValue("values/strings.xml", "weather_drone_category_toy_examples"))
        assertEquals("250g ~ 2kg", stringResourceValue("values/strings.xml", "weather_drone_category_class4"))
        assertEquals(
            "Air 시리즈\\n4급 자격증 필요",
            stringResourceValue("values/strings.xml", "weather_drone_category_class4_description"),
        )
        assertEquals("Air 2S, Air 3, Air 3S", stringResourceValue("values/strings.xml", "weather_drone_category_class4_examples"))
        assertEquals("2kg ~ 7kg", stringResourceValue("values/strings.xml", "weather_drone_category_class3"))
        assertEquals(
            "Mavic, Inspire\\n3급 자격증 필요",
            stringResourceValue("values/strings.xml", "weather_drone_category_class3_description"),
        )
        assertEquals("Mavic 3 Pro, Mavic 4 Pro, Inspire 3", stringResourceValue("values/strings.xml", "weather_drone_category_class3_examples"))
        assertEquals("7kg ~ 25kg", stringResourceValue("values/strings.xml", "weather_drone_category_class2"))
        assertEquals(
            "산업용 드론\\n2급 자격증 필요",
            stringResourceValue("values/strings.xml", "weather_drone_category_class2_description"),
        )
        assertEquals("농업용/산업용 드론", stringResourceValue("values/strings.xml", "weather_drone_category_class2_examples"))

        assertEquals("≤250g", stringResourceValue("values-en/strings.xml", "weather_drone_category_toy"))
        assertEquals(
            "Mini Series\\nNo License Required",
            stringResourceValue("values-en/strings.xml", "weather_drone_category_toy_description"),
        )
        assertEquals("Mini 2, Mini 3 Pro, Mini 4 Pro", stringResourceValue("values-en/strings.xml", "weather_drone_category_toy_examples"))
        assertEquals("250g~2kg", stringResourceValue("values-en/strings.xml", "weather_drone_category_class4"))
        assertEquals(
            "Air Series\\nClass 4 License Required",
            stringResourceValue("values-en/strings.xml", "weather_drone_category_class4_description"),
        )
        assertEquals("Air 2S, Air 3, Air 3S", stringResourceValue("values-en/strings.xml", "weather_drone_category_class4_examples"))
        assertEquals("2kg~7kg", stringResourceValue("values-en/strings.xml", "weather_drone_category_class3"))
        assertEquals(
            "Mavic, Inspire\\nClass 3 License Required",
            stringResourceValue("values-en/strings.xml", "weather_drone_category_class3_description"),
        )
        assertEquals("Mavic 3 Pro, Mavic 4 Pro, Inspire 3", stringResourceValue("values-en/strings.xml", "weather_drone_category_class3_examples"))
        assertEquals("7kg~25kg", stringResourceValue("values-en/strings.xml", "weather_drone_category_class2"))
        assertEquals(
            "Industrial Drones\\nClass 2 License Required",
            stringResourceValue("values-en/strings.xml", "weather_drone_category_class2_description"),
        )
        assertEquals("Agricultural/Industrial Drones", stringResourceValue("values-en/strings.xml", "weather_drone_category_class2_examples"))
    }

    @Test
    fun `drone list and settings entry strings match iOS localizations`() {
        assertEquals("내 드론 관리하기", stringResourceValue("values/strings.xml", "settings_drone_manage"))
        assertEquals("Manage My Drones", stringResourceValue("values-en/strings.xml", "settings_drone_manage"))

        assertEquals("내 드론 관리하기", stringResourceValue("values/strings.xml", "drone_list_title"))
        assertEquals("Manage My Drones", stringResourceValue("values-en/strings.xml", "drone_list_title"))
        assertEquals("내 드론", stringResourceValue("values/strings.xml", "drone_list_section_my"))
        assertEquals("My Drones", stringResourceValue("values-en/strings.xml", "drone_list_section_my"))
        assertEquals("새 드론 추가", stringResourceValue("values/strings.xml", "drone_list_add"))
        assertEquals("Add New Drone", stringResourceValue("values-en/strings.xml", "drone_list_add"))
    }

    @Test
    fun `drone detail and edit strings match iOS localizations`() {
        assertEquals("오렌지", stringResourceValue("values/strings.xml", "palette_color_orange"))
        assertEquals("청록", stringResourceValue("values/strings.xml", "palette_color_teal"))
        assertEquals("Orange", stringResourceValue("values-en/strings.xml", "palette_color_orange"))
        assertEquals("Teal", stringResourceValue("values-en/strings.xml", "palette_color_teal"))

        assertEquals("제작 번호", stringResourceValue("values/strings.xml", "drone_detail_serial_number"))
        assertEquals("이륙 무게", stringResourceValue("values/strings.xml", "drone_detail_takeoff_weight"))
        assertEquals("메모", stringResourceValue("values/strings.xml", "drone_detail_section_memo"))
        assertEquals("메모가 없습니다", stringResourceValue("values/strings.xml", "drone_detail_memo_empty"))
        assertEquals(
            "정말로 '%s'을(를) 삭제하시겠습니까?",
            androidDisplayStringResourceValue("values/strings.xml", "drone_detail_delete_message"),
        )
        assertEquals(
            "'%1\$s'에 연결된 %2\$d개의 도형을 어떻게 처리하시겠습니까?",
            androidDisplayPluralResourceValue("values/strings.xml", "drone_detail_delete_with_shapes_message", "other"),
        )

        assertEquals("한 줄을 넘어가지 않도록 입력해주세요", stringResourceValue("values/strings.xml", "drone_edit_section_basic_footer"))
        assertEquals("제작 번호 입력 (선택)", stringResourceValue("values/strings.xml", "drone_edit_serial_placeholder"))
        assertEquals("제작 번호", stringResourceValue("values/strings.xml", "drone_edit_section_serial"))
        assertEquals(
            "드론의 제작 번호 또는 시리얼 번호를 입력하세요",
            androidDisplayStringResourceValue("values/strings.xml", "drone_edit_section_serial_footer"),
        )
        assertEquals("드론의 이륙 무게와 크기를 입력하세요", stringResourceValue("values/strings.xml", "drone_edit_section_specs_footer"))
        assertEquals("이미 존재하는 드론 이름입니다.", stringResourceValue("values/strings.xml", "drone_edit_alert_name_duplicate"))
        assertEquals("내 드론", stringResourceValue("values/strings.xml", "drone_edit_default_name_first"))

        assertEquals("Serial Number", stringResourceValue("values-en/strings.xml", "drone_detail_serial_number"))
        assertEquals("Takeoff Weight", stringResourceValue("values-en/strings.xml", "drone_detail_takeoff_weight"))
        assertEquals("Memo", stringResourceValue("values-en/strings.xml", "drone_detail_section_memo"))
        assertEquals("Not entered", stringResourceValue("values-en/strings.xml", "drone_detail_not_entered"))
        assertEquals(
            "How would you like to handle %2\$d shape connected to '%1\$s'?",
            androidDisplayPluralResourceValue("values-en/strings.xml", "drone_detail_delete_with_shapes_message", "one"),
        )
        assertEquals(
            "How would you like to handle %2\$d shapes connected to '%1\$s'?",
            androidDisplayPluralResourceValue("values-en/strings.xml", "drone_detail_delete_with_shapes_message", "other"),
        )
        assertEquals("My Drone", stringResourceValue("values-en/strings.xml", "drone_edit_default_name_first"))
        assertEquals("Save Failed", stringResourceValue("values-en/strings.xml", "drone_edit_alert_save_failed"))
        assertEquals("This drone name already exists.", stringResourceValue("values-en/strings.xml", "drone_edit_alert_name_duplicate"))
    }

    @Test
    fun `document and terms strings match iOS localizations`() {
        assertEquals("닫기", stringResourceValue("values/strings.xml", "common_close"))
        assertEquals("다시 시도", stringResourceValue("values/strings.xml", "common_retry"))
        assertEquals("개인정보 취급방침", stringResourceValue("values/strings.xml", "login_terms_privacy"))
        assertEquals("개인정보 취급방침", stringResourceValue("values/strings.xml", "profile_terms_privacy"))
        assertEquals("이용약관", stringResourceValue("values/strings.xml", "document_terms_service_title"))
        assertEquals("개인정보 취급방침", stringResourceValue("values/strings.xml", "document_terms_privacy_title"))
        assertEquals("불러오는 중...", stringResourceValue("values/strings.xml", "document_terms_loading"))
        assertEquals("약관을 불러올 수 없습니다.", stringResourceValue("values/strings.xml", "document_terms_service_error_title"))
        assertEquals("잠시 후 다시 시도해주세요.", stringResourceValue("values/strings.xml", "document_terms_service_error_message"))
        assertEquals(
            "개인정보 취급방침을 불러올 수 없습니다.",
            stringResourceValue("values/strings.xml", "document_terms_privacy_error_title"),
        )
        assertEquals("잠시 후 다시 시도해주세요.", stringResourceValue("values/strings.xml", "document_terms_privacy_error_message"))

        assertEquals("Privacy Policy", stringResourceValue("values-en/strings.xml", "login_terms_privacy"))
        assertEquals("Privacy Policy", stringResourceValue("values-en/strings.xml", "profile_terms_privacy"))
        assertEquals("Terms of Service", stringResourceValue("values-en/strings.xml", "document_terms_service_title"))
        assertEquals("Privacy Policy", stringResourceValue("values-en/strings.xml", "document_terms_privacy_title"))
        assertEquals("Loading...", stringResourceValue("values-en/strings.xml", "document_terms_loading"))
        assertEquals("Unable to load terms", stringResourceValue("values-en/strings.xml", "document_terms_service_error_title"))
        assertEquals("Please try again later", stringResourceValue("values-en/strings.xml", "document_terms_service_error_message"))
        assertEquals(
            "Unable to load privacy policy",
            stringResourceValue("values-en/strings.xml", "document_terms_privacy_error_title"),
        )
        assertEquals("Please try again later", stringResourceValue("values-en/strings.xml", "document_terms_privacy_error_message"))
        assertEquals("Close", stringResourceValue("values-en/strings.xml", "common_close"))
        assertEquals("Retry", stringResourceValue("values-en/strings.xml", "common_retry"))

        assertEquals("패치노트", stringResourceValue("values/strings.xml", "patch_notes_title"))
        assertEquals("불러오는 중...", stringResourceValue("values/strings.xml", "patch_notes_loading"))
        assertEquals("패치노트를 불러올 수 없습니다.", stringResourceValue("values/strings.xml", "patch_notes_error_title"))
        assertEquals("잠시 후 다시 시도해주세요.", stringResourceValue("values/strings.xml", "patch_notes_error_message"))

        assertEquals("Patch Notes", stringResourceValue("values-en/strings.xml", "patch_notes_title"))
        assertEquals("Loading...", stringResourceValue("values-en/strings.xml", "patch_notes_loading"))
        assertEquals("Failed to load patch notes.", stringResourceValue("values-en/strings.xml", "patch_notes_error_title"))
        assertEquals("Please try again later.", stringResourceValue("values-en/strings.xml", "patch_notes_error_message"))
    }

    private fun stringResourceNames(relativePath: String): Set<String> {
        val document = parseXml(relativePath)
        val nodes = document.getElementsByTagName("string")

        return buildSet {
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                val attributes = node.attributes
                val translatable = attributes.getNamedItem("translatable")?.nodeValue
                val name = requireNotNull(attributes.getNamedItem("name")?.nodeValue)
                if (translatable != "false") {
                    add(name)
                }
            }
        }
    }

    private fun pluralResourceNames(relativePath: String): Set<String> {
        val document = parseXml(relativePath)
        val nodes = document.getElementsByTagName("plurals")

        return buildSet {
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                val attributes = node.attributes
                val translatable = attributes.getNamedItem("translatable")?.nodeValue
                val name = requireNotNull(attributes.getNamedItem("name")?.nodeValue)
                if (translatable != "false") {
                    add(name)
                }
            }
        }
    }

    private fun stringResourceValue(relativePath: String, name: String): String {
        val document = parseXml(relativePath)
        val nodes = document.getElementsByTagName("string")

        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            val nodeName = node.attributes.getNamedItem("name")?.nodeValue
            if (nodeName == name) {
                return node.textContent
            }
        }
        error("String resource not found: $name")
    }

    private fun androidDisplayStringResourceValue(relativePath: String, name: String): String {
        return stringResourceValue(relativePath, name)
            .replace("\\'", "'")
    }

    private fun pluralResourceValue(relativePath: String, name: String, quantity: String): String {
        val document = parseXml(relativePath)
        val nodes = document.getElementsByTagName("plurals")

        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            val nodeName = node.attributes.getNamedItem("name")?.nodeValue
            if (nodeName != name) {
                continue
            }

            val items = node.childNodes
            for (itemIndex in 0 until items.length) {
                val item = items.item(itemIndex)
                val itemQuantity = item.attributes?.getNamedItem("quantity")?.nodeValue
                if (itemQuantity == quantity) {
                    return item.textContent
                }
            }
        }
        error("Plural resource not found: $name/$quantity")
    }

    private fun androidDisplayPluralResourceValue(relativePath: String, name: String, quantity: String): String {
        return pluralResourceValue(relativePath, name, quantity)
            .replace("\\'", "'")
    }

    private fun parseXml(relativePath: String) =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(resolveResFile(relativePath))

    private fun resolveResFile(relativePath: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return sequenceOf(
            File(userDir, "src/main/res/$relativePath"),
            File(userDir, "app/src/main/res/$relativePath"),
        ).first { it.exists() }
    }
}

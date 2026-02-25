package com.ScienceFiction.DronePassAndroid.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ScienceFiction.DronePassAndroid.R

/**
 * 패치노트 화면
 *
 * WebDocumentScreen을 재사용하여 Notion 패치노트 페이지를 표시합니다.
 *
 * @param onBack 뒤로가기 콜백
 */
@Composable
fun PatchNotesScreen(
    onBack: () -> Unit
) {
    WebDocumentScreen(
        title = stringResource(R.string.patch_notes_title),
        url = "https://dronepass.notion.site/patch-notes",
        onBack = onBack
    )
}

package com.ScienceFiction.DronePassAndroid.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.feature.document.PatchNotesContent
import com.ScienceFiction.DronePassAndroid.ui.component.SheetLargeTitleHeader
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGroupedBackground

/**
 * 패치노트 풀스크린 (Settings 서브 화면).
 *
 * iOS `PatchNotesView` 정합 — large title + 닫기 액션 + 카드 리스트 (`PatchNotesContent`).
 * 데이터는 자체 서버(`https://sciencefiction.co.kr/dronepass/version-patches.txt`) 에서 fetch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchNotesScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IosSystemGroupedBackground),
    ) {
        SheetLargeTitleHeader(
            title = stringResource(R.string.patch_notes_title),
            closeText = stringResource(R.string.common_close),
            onClose = onBack,
        )
        PatchNotesContent()
    }
}

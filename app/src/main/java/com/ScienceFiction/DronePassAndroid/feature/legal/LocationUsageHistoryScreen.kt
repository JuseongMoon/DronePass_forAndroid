package com.ScienceFiction.DronePassAndroid.feature.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.location.LocationUsageRecord
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedDivider
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedRow
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSection
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSectionSpacing
import com.ScienceFiction.DronePassAndroid.ui.component.SheetLargeTitleContentGap
import com.ScienceFiction.DronePassAndroid.ui.component.SheetLargeTitleHeader
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSecondaryLabel
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGroupedBackground

/** 기록 한 줄: `2026-11-12 · 날씨 조회`. */
internal fun locationUsageRecordText(record: LocationUsageRecord, purposeLabel: String): String =
    "${record.date} · $purposeLabel"

/** 위치정보 이용 기록(최근 순). 기기에만 있는 기록이고 좌표는 없다. */
@Composable
fun LocationUsageHistoryScreen(
    records: List<LocationUsageRecord>,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IosSystemGroupedBackground),
    ) {
        SheetLargeTitleHeader(
            title = stringResource(R.string.settings_location_history),
            closeText = stringResource(R.string.common_close),
            onClose = onClose,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = SheetLargeTitleContentGap, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(InsetGroupedSectionSpacing),
        ) {
            InsetGroupedSection(footer = stringResource(R.string.settings_location_footer)) {
                if (records.isEmpty()) {
                    InsetGroupedRow(
                        title = stringResource(R.string.settings_location_history_empty),
                        titleColor = IosSecondaryLabel,
                    )
                } else {
                    records.forEachIndexed { index, record ->
                        if (index > 0) InsetGroupedDivider()
                        InsetGroupedRow(
                            title = locationUsageRecordText(record, stringResource(record.purpose.labelRes)),
                        )
                    }
                }
            }
        }
    }
}

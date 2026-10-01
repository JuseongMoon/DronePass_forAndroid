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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import com.ScienceFiction.DronePassAndroid.core.location.LocationUsageRecord
import com.ScienceFiction.DronePassAndroid.core.location.locationUsageHourText
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedRowHorizontalPadding
import com.ScienceFiction.DronePassAndroid.ui.theme.IosLabel
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedDivider
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedRow
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSection
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSectionSpacing
import com.ScienceFiction.DronePassAndroid.ui.component.SheetLargeTitleContentGap
import com.ScienceFiction.DronePassAndroid.ui.component.SheetLargeTitleHeader
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSecondaryLabel
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGroupedBackground

/** 기록 한 줄: `2026-11-12 14:00 · 날씨 조회 · 회사 서버 경유 Apple(WeatherKit)`. */
internal fun locationUsageRecordText(record: LocationUsageRecord, purposeLabel: String, recipientLabel: String): String =
    "${locationUsageHourText(record)} · $purposeLabel · $recipientLabel"

/** 위치정보 이용 기록(최근 순). 좌표는 없고, 열람 요청에 쓰는 설치 ID 를 함께 보여 준다. */
@Composable
fun LocationUsageHistoryScreen(
    records: List<LocationUsageRecord>,
    installId: String?,
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
            InsetGroupedSection(footer = stringResource(R.string.location_usage_server_notice)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = InsetGroupedRowHorizontalPadding, vertical = 10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.location_usage_install_id),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = IosSecondaryLabel,
                    )
                    SelectionContainer {
                        Text(text = installId.orEmpty(), fontSize = 15.sp, lineHeight = 20.sp, color = IosLabel)
                    }
                }
            }

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
                            title = locationUsageRecordText(
                                record = record,
                                purposeLabel = stringResource(record.purpose.labelRes),
                                recipientLabel = stringResource(record.recipient.labelRes),
                            ),
                        )
                    }
                }
            }
        }
    }
}

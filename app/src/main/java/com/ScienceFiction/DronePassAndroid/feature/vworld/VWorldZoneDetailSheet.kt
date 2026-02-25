package com.ScienceFiction.DronePassAndroid.feature.vworld

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.DroneZoneFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightRestrictionLevel
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.NotamStatus
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.authorityNameEng
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.authorityNameKor
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.daysRemaining
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.designationNumber
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.designationYear
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.formattedLowerAltitude
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.formattedUpperAltitude
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.fullAddress
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.heritageName
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.heritageZoneName
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.notamEndDate
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.notamStartDate
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.notamStatus
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.operatingInstitution
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.phoneNumber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * 비행구역 상세 정보 BottomSheet
 *
 * 선택한 비행구역의 상세 정보를 표시한다:
 * - 구역 이름 및 코드
 * - 레이어 유형 및 제한 수준
 * - 고도 범위 (formattedUpperAltitude / formattedLowerAltitude)
 * - 레이어별 전용 섹션 (NOTAM, 사전협의, 문화재)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VWorldZoneDetailSheet(
    zone: DroneZoneFeature,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val notamDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm (z)", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Seoul")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // 제한 수준 배지
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(zone.layer.borderColor.toInt()))
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = zone.layer.restrictionLevel.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = getRestrictionLevelColor(zone.layer.restrictionLevel)
                )
            }

            // 구역 이름
            Text(
                text = zone.zoneName ?: zone.layer.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 레이어 유형
            Text(
                text = zone.layer.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // 상세 정보 행들
            if (zone.zoneCode != null) {
                DetailRow(label = stringResource(R.string.zone_detail_code), value = zone.zoneCode)
            }

            // 고도 범위 - formattedUpperAltitude / formattedLowerAltitude 사용
            if (zone.formattedUpperAltitude != null || zone.formattedLowerAltitude != null) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = stringResource(R.string.zone_detail_altitude_range),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    zone.formattedUpperAltitude?.let {
                        Text(
                            text = "${stringResource(R.string.zone_detail_altitude_upper)}: $it",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    zone.formattedLowerAltitude?.let {
                        Text(
                            text = "${stringResource(R.string.zone_detail_altitude_lower)}: $it",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (zone.upperAltitude != null || zone.lowerAltitude != null) {
                // formattedAltitude가 없는 레이어는 기존 방식으로 표시
                val surfaceLabel = stringResource(R.string.zone_detail_surface)
                val unlimitedLabel = stringResource(R.string.zone_detail_unlimited)
                val altitudeText = buildString {
                    zone.lowerAltitude?.let { append("${it.toInt()}ft") }
                        ?: append(surfaceLabel)
                    append(" ~ ")
                    zone.upperAltitude?.let { append("${it.toInt()}ft") }
                        ?: append(unlimitedLabel)
                }
                DetailRow(label = stringResource(R.string.zone_detail_altitude_range), value = altitudeText)
            }

            DetailRow(
                label = stringResource(R.string.zone_detail_restriction_level),
                value = when (zone.layer.restrictionLevel) {
                    FlightRestrictionLevel.PROHIBITED -> stringResource(R.string.zone_detail_prohibited)
                    FlightRestrictionLevel.RESTRICTED -> stringResource(R.string.zone_detail_restricted)
                    FlightRestrictionLevel.CONSULTATION -> stringResource(R.string.zone_detail_consultation)
                    FlightRestrictionLevel.ADVISORY -> stringResource(R.string.zone_detail_advisory)
                }
            )

            // ============================================================
            // 레이어별 전용 섹션
            // ============================================================

            // 임시비행금지구역 전용 - NOTAM 정보
            if (zone.layer == FlightZoneLayer.TEMPORARY_PROHIBITED) {
                SectionHeader(title = stringResource(R.string.zone_detail_notam_title))

                DetailRow(
                    label = stringResource(R.string.zone_detail_notam_status),
                    value = "${zone.notamStatus.emoji} ${zone.notamStatus.displayName}"
                )

                zone.notamStartDate?.let {
                    DetailRow(
                        label = stringResource(R.string.zone_detail_notam_start),
                        value = notamDateFormat.format(it)
                    )
                }

                zone.notamEndDate?.let {
                    DetailRow(
                        label = stringResource(R.string.zone_detail_notam_end),
                        value = notamDateFormat.format(it)
                    )
                }

                zone.daysRemaining?.let { days ->
                    DetailRow(
                        label = stringResource(R.string.zone_detail_notam_remaining),
                        value = stringResource(R.string.zone_detail_notam_days, days),
                        valueColor = if (days <= 7) Color(0xFFFF8C00) else null
                    )
                }
            }

            // 사전협의구역 전용 - 관리기관 정보
            if (zone.layer == FlightZoneLayer.PRIOR_CONSULTATION) {
                SectionHeader(title = stringResource(R.string.zone_detail_authority_title))

                zone.authorityNameKor?.let {
                    DetailRow(
                        label = stringResource(R.string.zone_detail_authority_name),
                        value = it
                    )
                }
                zone.authorityNameEng?.let {
                    DetailRow(
                        label = stringResource(R.string.zone_detail_authority_name_eng),
                        value = it
                    )
                }
                zone.operatingInstitution?.let {
                    DetailRow(
                        label = stringResource(R.string.zone_detail_authority_department),
                        value = it
                    )
                }
                zone.phoneNumber?.let { phone ->
                    PhoneNumberRow(
                        label = stringResource(R.string.zone_detail_authority_contact),
                        phone = phone
                    )
                }
            }

            // 문화재보호구역 전용 - 문화재 정보
            if (zone.layer == FlightZoneLayer.CULTURAL_HERITAGE) {
                SectionHeader(title = stringResource(R.string.zone_detail_heritage_title))

                zone.heritageName?.let {
                    DetailRow(
                        label = stringResource(R.string.zone_detail_heritage_name),
                        value = it
                    )
                }
                zone.fullAddress?.let {
                    DetailRow(
                        label = stringResource(R.string.zone_detail_heritage_address),
                        value = it
                    )
                }
                zone.heritageZoneName?.let {
                    DetailRow(
                        label = stringResource(R.string.zone_detail_heritage_zone),
                        value = it
                    )
                }

                if (zone.designationYear != null && zone.designationNumber != null) {
                    DetailRow(
                        label = stringResource(R.string.zone_detail_heritage_designation),
                        value = stringResource(
                            R.string.zone_detail_heritage_designation_format,
                            zone.designationYear!!,
                            zone.designationNumber!!
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 섹션 헤더 구분선 + 제목
 */
@Composable
private fun SectionHeader(title: String) {
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
}

/**
 * 라벨-값 상세 정보 행
 *
 * @param label 왼쪽 라벨 텍스트
 * @param value 오른쪽 값 텍스트
 * @param valueColor 값 텍스트 색상 (null이면 기본 색상)
 */
@Composable
private fun DetailRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor ?: LocalContentColor.current,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 전화번호 행 (클릭 시 전화 앱 실행)
 */
@Composable
private fun PhoneNumberRow(label: String, phone: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = phone,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF007AFF),
            modifier = Modifier
                .weight(1f)
                .clickable {
                    val intent = Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:${phone.replace("-", "")}")
                    )
                    context.startActivity(intent)
                }
        )
    }
}

@Composable
private fun getRestrictionLevelColor(level: FlightRestrictionLevel): Color {
    return when (level) {
        FlightRestrictionLevel.PROHIBITED -> Color(0xFFFF0000)
        FlightRestrictionLevel.RESTRICTED -> Color(0xFFFF8C00)
        FlightRestrictionLevel.CONSULTATION -> Color(0xFF0000FF)
        FlightRestrictionLevel.ADVISORY -> Color(0xFFFFAA00)
    }
}

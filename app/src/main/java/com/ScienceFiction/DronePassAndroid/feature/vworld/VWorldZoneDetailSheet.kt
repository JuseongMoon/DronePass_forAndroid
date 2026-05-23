package com.ScienceFiction.DronePassAndroid.feature.vworld

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.local.PublicContactInfo
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.DroneZoneFeature
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.FlightZoneLayer
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.authorityNameEng
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.authorityNameKor
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.centerCoordinate
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.daysRemaining
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.designationNumber
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.designationYear
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.formatCoordinate
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

private val SecondaryTextColor: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val InfoBannerAccent = Color(0xFF007AFF)
private val NotamRemainingWarningColor = Color(0xFFFF9500)
private val PhoneLinkColor = Color(0xFF007AFF)

/**
 * 비행구역 상세 정보 BottomSheet (iOS VWorldZoneDetailView 정합)
 *
 * iOS NavigationView + List(insetGrouped) + Section 들의 구조를
 * Compose 의 ModalBottomSheet + Column + 수동 Section 으로 재현한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VWorldZoneDetailSheet(
    zone: DroneZoneFeature,
    findContact: (String?) -> PublicContactInfo? = { null },
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val notamDateFormat = remember {
        SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
    }
    val scrollState = rememberScrollState()

    // 사전협의구역 외 모든 레이어에 대해 publicContact lookup (iOS 정합)
    val publicContact: PublicContactInfo? = remember(zone) {
        if (zone.layer == FlightZoneLayer.PRIOR_CONSULTATION) null
        else findContact(zone.zoneName ?: zone.heritageName)
    }

    // iOS dynamicDetents 흉내: 레이어 타입별 최소 시트 높이
    val estimatedHeight: Dp = remember(zone.layer) {
        when (zone.layer) {
            FlightZoneLayer.CULTURAL_HERITAGE -> 680.dp
            FlightZoneLayer.TEMPORARY_PROHIBITED -> 550.dp
            FlightZoneLayer.PRIOR_CONSULTATION,
            FlightZoneLayer.NATIONAL_PARK -> 480.dp
            FlightZoneLayer.PROHIBITED,
            FlightZoneLayer.RESTRICTED,
            FlightZoneLayer.ALERT,
            FlightZoneLayer.DANGER -> 340.dp
            FlightZoneLayer.CONTROL_ZONE,
            FlightZoneLayer.ULTRALIGHT -> 280.dp
            FlightZoneLayer.LANDING_FIELD,
            FlightZoneLayer.OBSTACLE,
            FlightZoneLayer.ATZ -> 230.dp
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = estimatedHeight)
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp)
        ) {
            // NavigationHeader (iOS NavigationView inline title + xmark.circle.fill)
            NavigationHeader(
                title = stringResource(R.string.zone_detail_navigation_title),
                onDismiss = onDismiss
            )

            // 섹션 영역
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // 기본 정보 섹션 (iOS Section 첫 번째)
                ZoneTypeRow(layer = zone.layer)

                if (zone.zoneCode != null) {
                    DetailRow(
                        label = stringResource(R.string.zone_detail_code),
                        value = zone.zoneCode
                    )
                }

                CoordinateRow(zone = zone)

                if (zone.formattedUpperAltitude != null || zone.formattedLowerAltitude != null) {
                    AltitudeRow(
                        upper = zone.formattedUpperAltitude,
                        lower = zone.formattedLowerAltitude
                    )
                }

                // 임시비행금지구역 — NOTAM 섹션
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
                            valueColor = if (days <= 7) NotamRemainingWarningColor else null
                        )
                    }
                }

                // 사전협의구역 — 관리기관 섹션
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
                    val phone = zone.phoneNumber
                        ?: findContact(zone.authorityNameKor ?: zone.zoneName)?.phoneNumber
                    phone?.let {
                        PhoneNumberRow(
                            label = stringResource(R.string.zone_detail_authority_contact),
                            phone = it
                        )
                    }
                }

                // 문화재보호구역 — 문화재 섹션
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

                // 공공기관 섹션 (사전협의 외 모든 레이어 — iOS publicContact)
                if (zone.layer != FlightZoneLayer.PRIOR_CONSULTATION && publicContact != null) {
                    SectionHeader(title = stringResource(R.string.zone_detail_authority_title))

                    DetailRow(
                        label = stringResource(R.string.zone_detail_authority_name),
                        value = publicContact.organizationName
                    )
                    PhoneNumberRow(
                        label = stringResource(R.string.zone_detail_authority_contact),
                        phone = publicContact.phoneNumber
                    )
                }
            }
        }
    }
}

/**
 * 시트 상단 NavigationHeader (iOS NavigationView inline title + trailing xmark.circle.fill)
 */
@Composable
private fun NavigationHeader(
    title: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(44.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * "구역 유형" 행 — 라벨 + Spacer + (Circle 12 + displayName)
 */
@Composable
private fun ZoneTypeRow(layer: FlightZoneLayer) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.zone_detail_zone_type),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(layer.fillColor.toInt()))
                    .border(width = 1.dp, color = Color(layer.borderColor.toInt()), shape = CircleShape)
            )
            Text(
                text = layer.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryTextColor
            )
        }
    }
}

/**
 * 좌표 행 (iOS formattedCoordinate 매핑)
 */
@Composable
private fun CoordinateRow(zone: DroneZoneFeature) {
    val (lat, lon) = zone.centerCoordinate
    DetailRow(
        label = stringResource(R.string.zone_detail_coordinates),
        value = formatCoordinate(lat, lon)
    )
}

/**
 * 고도 행 — 한 Row + 우측 VStack(End, caption 2줄)
 */
@Composable
private fun AltitudeRow(upper: String?, lower: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.zone_detail_altitude_limit),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            upper?.let {
                Text(
                    text = "${stringResource(R.string.zone_detail_altitude_upper)}: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryTextColor
                )
            }
            lower?.let {
                Text(
                    text = "${stringResource(R.string.zone_detail_altitude_lower)}: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryTextColor
                )
            }
        }
    }
}

/**
 * 섹션 헤더 (iOS Section header — caption 회색, 위 spacing)
 */
@Composable
private fun SectionHeader(title: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = SecondaryTextColor,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

/**
 * 라벨-값 행 (iOS HStack with Spacer 정합 — width 고정 없음)
 */
@Composable
private fun DetailRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor ?: SecondaryTextColor,
            textAlign = TextAlign.End
        )
    }
}

/**
 * 전화번호 행 (탭하면 tel: intent)
 */
@Composable
private fun PhoneNumberRow(label: String, phone: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = phone,
            style = MaterialTheme.typography.bodyMedium,
            color = PhoneLinkColor,
            modifier = Modifier.clickable {
                val intent = Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:${phone.replace("-", "")}")
                )
                context.startActivity(intent)
            }
        )
    }
}

package com.ScienceFiction.DronePassAndroid.feature.kp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.Kp27DayForecast
import com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData
import com.ScienceFiction.DronePassAndroid.domain.model.KpLevel

/**
 * Kp 지수 예보 화면
 *
 * 현재 Kp 수치 + 레벨 카드와 24시간 예보 리스트를 표시한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KpForecastScreen(
    viewModel: KpViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.kp_title),
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                // 로딩 중 다중 클릭으로 코루틴이 누적되어 마지막 결과만 표시되는 race 차단
                IconButton(
                    onClick = { viewModel.loadKpData() },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.common_refresh))
                }
            }
        )

        KpForecastContent(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Kp 지수 예보 본문 Content (BottomSheet에서도 재사용 가능)
 */
@Composable
fun KpForecastContent(
    viewModel: KpViewModel,
    modifier: Modifier = Modifier
) {
    val currentKp by viewModel.currentKp.collectAsStateWithLifecycle()
    val kpLevel by viewModel.kpLevel.collectAsStateWithLifecycle()
    val forecastData by viewModel.forecastData.collectAsStateWithLifecycle()
    val longTermForecast by viewModel.longTermForecast.collectAsStateWithLifecycle()
    val lastUpdated by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    // 화면이 START 일 때만 5분 간격 자동 갱신. ON_STOP 시 중단하여 백그라운드
    // 무한 새로고침으로 인한 배터리/요금 소모를 차단한다.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> viewModel.startAutoRefresh()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> viewModel.stopAutoRefresh()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = modifier
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 현재 Kp 카드
        item {
            CurrentKpCard(
                currentKp = currentKp,
                kpLevel = kpLevel,
                lastUpdated = lastUpdated
            )
        }

        // 에러 메시지
        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // 드론 비행 영향도 카드
        item {
            DroneFlightImpactCard(kpValue = currentKp?.kp)
        }

        // Kp 레벨 설명
        item {
            KpLevelLegend()
        }

        // 48시간 예보 차트
        if (forecastData.isNotEmpty()) {
            item {
                Kp48HourChart(
                    forecastData = forecastData,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // 24시간 예보 헤더
        if (forecastData.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.kp_forecast_data),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 예보 리스트
            items(forecastData) { data ->
                ForecastItem(data = data)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // 27일 장기예보 섹션
        if (longTermForecast.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }

            // 27일 차트
            item {
                Kp27DayChart(
                    longTermForecast = longTermForecast,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Text(
                    text = stringResource(R.string.kp_27day_forecast),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(longTermForecast) { data ->
                LongTermForecastItem(data = data)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CurrentKpCard(
    currentKp: KpIndexData?,
    kpLevel: KpLevel,
    lastUpdated: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(kpLevel.color.toInt()).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.kp_current),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (currentKp != null) {
                Text(
                    text = String.format("%.2f", currentKp.kp),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color(kpLevel.color.toInt())
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Kp 게이지 바
                KpGaugeBar(kpValue = currentKp.kp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = kpLevel.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(kpLevel.color.toInt())
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = currentKp.timeTag,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(R.string.kp_no_data),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (lastUpdated != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.kp_last_updated, lastUpdated),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Kp 값을 0-9 범위로 시각화하는 수평 게이지 바
 */
@Composable
private fun KpGaugeBar(kpValue: Double) {
    val fraction = (kpValue / 9.0).toFloat().coerceIn(0f, 1f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 게이지 바
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // 그라데이션 느낌의 세그먼트
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
            ) {
                // 초록 (0-3)
                Box(
                    modifier = Modifier
                        .weight(3f)
                        .height(12.dp)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.3f))
                )
                // 노랑 (4-5)
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(12.dp)
                        .background(Color(0xFFFFC107).copy(alpha = 0.3f))
                )
                // 주황 (6-7)
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(12.dp)
                        .background(Color(0xFFFF9800).copy(alpha = 0.3f))
                )
                // 빨강 (8-9)
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(12.dp)
                        .background(Color(0xFFF44336).copy(alpha = 0.3f))
                )
            }

            // 현재 값 인디케이터
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Color(KpLevel.fromKp(kpValue).color.toInt()).copy(alpha = 0.8f)
                    )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 숫자 라벨
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                text = "3",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                text = "5",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                text = "7",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                text = "9",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * 드론 비행 영향도 카드
 * Kp 값에 따라 GPS/드론 비행 영향을 설명
 */
@Composable
private fun DroneFlightImpactCard(kpValue: Double?) {
    val kp = kpValue ?: return

    data class ImpactInfo(
        val label: String,
        val description: String,
        val color: Color
    )

    val impact = when {
        kp < 4.0 -> ImpactInfo(
            label = stringResource(R.string.kp_level_stable),
            description = stringResource(R.string.kp_desc_stable),
            color = Color(0xFF4CAF50)
        )
        kp < 6.0 -> ImpactInfo(
            label = stringResource(R.string.kp_level_unstable),
            description = stringResource(R.string.kp_desc_unstable),
            color = Color(0xFFFFC107)
        )
        kp < 8.0 -> ImpactInfo(
            label = stringResource(R.string.kp_level_storm),
            description = stringResource(R.string.kp_desc_storm),
            color = Color(0xFFFF9800)
        )
        else -> ImpactInfo(
            label = stringResource(R.string.kp_level_extreme),
            description = stringResource(R.string.kp_desc_extreme),
            color = Color(0xFFF44336)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = impact.color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 상태 인디케이터
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(impact.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = impact.label.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = impact.color
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = impact.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = impact.color
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = impact.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KpLevelLegend() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.kp_level_guide),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            KpLevel.entries.forEach { level ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(level.color.toInt()))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = level.label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(100.dp)
                    )
                    Text(
                        text = when (level) {
                            KpLevel.NORMAL -> "Kp < 5"
                            KpLevel.G1 -> "Kp 5"
                            KpLevel.G2 -> "Kp 6"
                            KpLevel.G3 -> "Kp 7"
                            KpLevel.G4 -> "Kp 8"
                            KpLevel.G5 -> "Kp 9"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastItem(data: KpIndexData) {
    val level = KpLevel.fromKp(data.kp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 시간
        Text(
            text = formatTimeTag(data.timeTag),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(120.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Kp 값 바
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (data.kp / 9.0).toFloat().coerceIn(0f, 1f))
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(level.color.toInt()).copy(alpha = 0.7f))
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Kp 값
        Text(
            text = String.format("%.1f", data.kp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(level.color.toInt()),
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )

        // 관측 유형
        if (data.observed != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (data.observed) {
                    "observed" -> "O"
                    "estimated" -> "E"
                    "predicted" -> "P"
                    else -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(14.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LongTermForecastItem(data: Kp27DayForecast) {
    val level = KpLevel.fromKp(data.kp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 날짜
        Text(
            text = data.date,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(120.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Kp 값 바
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (data.kp / 9.0).toFloat().coerceIn(0f, 1f))
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(level.color.toInt()).copy(alpha = 0.7f))
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Kp 값
        Text(
            text = String.format("%.0f", data.kp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(level.color.toInt()),
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Ap 값
        Text(
            text = stringResource(R.string.kp_27day_ap_value, data.ap),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(46.dp),
            textAlign = TextAlign.End
        )
    }
}

/**
 * 시간 태그 포매팅 (예: "2026-02-24 12:00:00" -> "02/24 12:00")
 */
private fun formatTimeTag(timeTag: String): String {
    return try {
        val parts = timeTag.split(" ")
        if (parts.size >= 2) {
            val dateParts = parts[0].split("-")
            val timeParts = parts[1].split(":")
            if (dateParts.size >= 3 && timeParts.size >= 2) {
                "${dateParts[1]}/${dateParts[2]} ${timeParts[0]}:${timeParts[1]}"
            } else {
                timeTag
            }
        } else {
            timeTag
        }
    } catch (e: Exception) {
        timeTag
    }
}

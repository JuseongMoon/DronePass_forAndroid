package com.ScienceFiction.DronePassAndroid.feature.kp

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.KpIndexData
import com.ScienceFiction.DronePassAndroid.domain.model.KpLevel
import com.ScienceFiction.DronePassAndroid.feature.settings.KpInfoGuideSheet
import com.ScienceFiction.DronePassAndroid.ui.component.IosToastMessageDurationMs
import com.ScienceFiction.DronePassAndroid.ui.component.IosToastMessageOverlay
import kotlinx.coroutines.delay
import java.util.Locale

internal const val KpCurrentValueFontSizeSp = 60
internal const val KpToolbarShowsLoadingIndicator = false

@Suppress("UNUSED_PARAMETER")
internal fun isKpRefreshActionEnabled(isLoading: Boolean): Boolean = true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KpForecastScreen(
    onBack: (() -> Unit)? = null,
    viewModel: KpViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var showKpInfoSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.kp_navigation_title),
                    fontWeight = FontWeight.SemiBold,
                )
            },
            navigationIcon = {
                IconButton(onClick = { showKpInfoSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.kp_info_button),
                    )
                }
            },
            actions = {
                if (KpToolbarShowsLoadingIndicator && isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                IconButton(
                    onClick = { viewModel.loadKpData() },
                    enabled = isKpRefreshActionEnabled(isLoading),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.common_refresh),
                    )
                }
            },
        )
        KpForecastContent(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize(),
        )
    }

    if (showKpInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showKpInfoSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            KpInfoGuideSheet(onDismiss = { showKpInfoSheet = false })
        }
    }
}

/**
 * ModalBottomSheet 내부에서 표시할 KP 시트 헤더 — iOS `KPForecastView` 의 NavigationView TopBar 정합.
 * 좌측: info 버튼(선택), 중앙: 제목, 우측: 새로고침 버튼.
 */
@Composable
fun KpSheetHeader(
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onInfo: (() -> Unit)? = null,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onInfo != null) {
                IconButton(onClick = onInfo) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.kp_info_button),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Spacer(modifier = Modifier.padding(start = 48.dp))
            }
            Text(
                text = stringResource(R.string.kp_navigation_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onRefresh, enabled = isKpRefreshActionEnabled(isLoading)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.kp_refresh),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/**
 * iOS `KPForecastView` body 정합 — 헤더 없는 순수 콘텐츠.
 * 풀스크린 [KpForecastScreen] 과 ModalBottomSheet 양쪽에서 재사용.
 */
@Composable
fun KpForecastContent(
    viewModel: KpViewModel,
    modifier: Modifier = Modifier,
) {
    val currentKp by viewModel.currentKp.collectAsStateWithLifecycle()
    val kpLevel by viewModel.kpLevel.collectAsStateWithLifecycle()
    val forecastData by viewModel.forecastData.collectAsStateWithLifecycle()
    val longTermForecast by viewModel.longTermForecast.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val errorText = errorMessage?.let { stringResource(it.messageRes) }
    val refreshMessage = stringResource(R.string.weather_refresh)
    var showRefreshToast by remember { mutableStateOf(false) }
    var refreshToastGeneration by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.startAutoRefresh()
                Lifecycle.Event.ON_STOP -> viewModel.stopAutoRefresh()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel, refreshMessage) {
        viewModel.refreshCompleted.collect {
            showRefreshToast = false
            refreshToastGeneration += 1
            showRefreshToast = true
        }
    }

    LaunchedEffect(refreshToastGeneration) {
        if (refreshToastGeneration == 0) return@LaunchedEffect
        delay(IosToastMessageDurationMs)
        showRefreshToast = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ① 현재 Kp 지수 카드 (섹션 헤더 + 좌우 분할)
            item {
                CurrentKpSection(
                    currentKp = currentKp,
                    kpLevel = kpLevel,
                )
            }

            // ② 48시간 예보 차트
            item {
                KpForecastLineChart(
                    forecastData = forecastData,
                    isLoading = isLoading,
                    errorMessage = errorText,
                )
            }

            // ③ 27일 장기 예보 차트
            item {
                Kp27DayChart(
                    longTermForecast = longTermForecast,
                    isLoading = isLoading,
                )
            }
        }

        IosToastMessageOverlay(
            visible = showRefreshToast,
            message = refreshMessage,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * iOS `KPForecastView.currentKPCard` 1:1 정합.
 * 섹션 헤더("현재 Kp 지수") + 카드(좌측 큰 숫자 / 우측 레벨 + 설명 + GFZ 출처).
 */
@Composable
private fun CurrentKpSection(
    currentKp: KpIndexData?,
    kpLevel: KpLevel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section header (카드 바깥)
        Text(
            text = stringResource(R.string.kp_section_current),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(kpLevel.color.toInt()).copy(alpha = 0.1f),
            ),
        ) {
            if (currentKp == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.kp_no_data),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 좌측: 큰 숫자 (iOS .system(size: 60, weight: .bold, design: .rounded))
                    Text(
                        text = "%.1f".format(Locale.ROOT, currentKp.kp),
                        fontSize = KpCurrentValueFontSizeSp.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(kpLevel.color.toInt()),
                    )

                    // 우측: 아이콘+레벨명 → 설명 → 출처
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = kpLevelIcon(kpLevel),
                                contentDescription = null,
                                tint = Color(kpLevel.color.toInt()),
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = stringResource(kpLevelNameRes(kpLevel)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(kpLevel.color.toInt()),
                            )
                        }
                        Text(
                            text = stringResource(kpLevelDescriptionRes(kpLevel)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            KpDataSourceLink(
                                label = stringResource(R.string.kp_data_source_gfz),
                                source = KpDataSource.GFZ_CURRENT,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * iOS `KPLevel.icon` → Material Icons 매핑.
 *  - normal: checkmark.circle.fill → CheckCircle
 *  - g1:     circle.fill → Circle
 *  - g2:     circle.lefthalf.filled → Brightness6 (반원)
 *  - g3:     exclamationmark.circle.fill → Error
 *  - g4:     exclamationmark.triangle.fill → Warning
 *  - g5:     xmark.octagon.fill → Block
 */
private fun kpLevelIcon(level: KpLevel): ImageVector = when (level) {
    KpLevel.NORMAL -> Icons.Default.CheckCircle
    KpLevel.G1 -> Icons.Default.Circle
    KpLevel.G2 -> Icons.Default.Brightness6
    KpLevel.G3 -> Icons.Default.Error
    KpLevel.G4 -> Icons.Default.Warning
    KpLevel.G5 -> Icons.Default.Block
}

@StringRes
internal fun kpLevelNameRes(level: KpLevel): Int = when (level) {
    KpLevel.NORMAL -> R.string.kp_info_level_normal_name
    KpLevel.G1 -> R.string.kp_info_level_g1_name
    KpLevel.G2 -> R.string.kp_info_level_g2_name
    KpLevel.G3 -> R.string.kp_info_level_g3_name
    KpLevel.G4 -> R.string.kp_info_level_g4_name
    KpLevel.G5 -> R.string.kp_info_level_g5_name
}

@StringRes
internal fun kpLevelDescriptionRes(level: KpLevel): Int = when (level) {
    KpLevel.NORMAL -> R.string.kp_level_normal_desc
    KpLevel.G1 -> R.string.kp_level_g1_desc
    KpLevel.G2 -> R.string.kp_level_g2_desc
    KpLevel.G3 -> R.string.kp_level_g3_desc
    KpLevel.G4 -> R.string.kp_level_g4_desc
    KpLevel.G5 -> R.string.kp_level_g5_desc
}

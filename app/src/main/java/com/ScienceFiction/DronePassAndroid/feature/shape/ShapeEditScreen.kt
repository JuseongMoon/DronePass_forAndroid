package com.ScienceFiction.DronePassAndroid.feature.shape

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.remote.NaverGeocodingApi
import com.ScienceFiction.DronePassAndroid.core.util.CoordinateParser
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 도형 편집 화면 — iOS ShapeEditView 1:1 정합.
 *
 * iOS 구조:
 * - NavigationView + Form (Section 그룹)
 * - Toolbar: Leading "취소" / Trailing "저장"
 * - Section 순서: 드론 → 기본정보(제목/좌표/주소/반경/고도) → 비행기간 → 메모
 * - 색상은 드론에 종속 (별도 색상 선택 섹션 없음)
 * - 변경사항 있을 때 취소 → "변경사항 폐기" 다이얼로그
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapeEditScreen(
    shape: ShapeModel? = null,
    initialCoordinate: Coordinate? = null,
    drones: List<DroneModel> = emptyList(),
    reverseGeocodedAddress: String? = null,
    geocodingApi: NaverGeocodingApi? = null,
    isDuplicateMode: Boolean = false,
    onSave: (ShapeModel) -> Unit,
    onDismiss: () -> Unit
) {
    // 복제 모드: shape 전체 데이터 활용하되 신규 ID + " (복사)" suffix + 새 createdAt 으로 저장.
    val isEditMode = shape != null && !isDuplicateMode
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val editKey = if (isDuplicateMode) "duplicate-${shape?.id}" else shape?.id
    val defaultTitle = stringResource(R.string.shape_edit_default_title)
    val duplicateSuffix = stringResource(R.string.shape_detail_copy_suffix)

    // ===== 초기값 보관 (hasChanges 비교용) =====
    val initialTitle = remember(editKey) {
        when {
            isDuplicateMode && shape != null -> "${shape.title} $duplicateSuffix"
            shape != null -> shape.title
            else -> ""
        }
    }
    val initialAddress = remember(editKey) { shape?.address ?: reverseGeocodedAddress ?: "" }
    val initialRadius = remember(editKey) {
        shape?.radius?.let { String.format(Locale.US, "%.0f", it) } ?: "500"
    }
    val initialHeight = remember(editKey) {
        shape?.height?.let { String.format(Locale.US, "%.0f", it) } ?: ""
    }
    val initialMemo = remember(editKey) { shape?.memo ?: "" }
    val initialFlightStart = remember(editKey) { shape?.flightStartDate ?: System.currentTimeMillis() }
    val initialFlightEnd = remember(editKey) { shape?.flightEndDate }
    val initialCoord = remember(editKey) {
        shape?.baseCoordinate ?: initialCoordinate ?: Coordinate(37.5665, 126.9780)
    }
    val initialDroneId = remember(editKey) { shape?.droneId }

    // ===== 편집 상태 =====
    var title by remember(editKey) { mutableStateOf(initialTitle) }
    var address by remember(editKey) { mutableStateOf(initialAddress) }
    var radiusText by remember(editKey) { mutableStateOf(initialRadius) }
    var heightText by remember(editKey) { mutableStateOf(initialHeight) }
    var memo by remember(editKey) { mutableStateOf(initialMemo) }
    var flightStartDate by remember(editKey) { mutableStateOf(initialFlightStart) }
    var flightEndDate by remember(editKey) { mutableStateOf(initialFlightEnd) }
    var coordinate by remember(editKey) { mutableStateOf(initialCoord) }
    var coordinateText by remember(editKey) {
        mutableStateOf(CoordinateParser.formatDecimal(initialCoord))
    }
    var coordinateParseError by remember(editKey) { mutableStateOf(false) }

    var selectedDrone by remember(editKey) {
        mutableStateOf(initialDroneId?.let { id -> drones.find { it.id == id } })
    }
    var showDroneDropdown by remember { mutableStateOf(false) }

    // 역지오코딩 결과가 나중에 도착할 경우 주소 업데이트
    LaunchedEffect(reverseGeocodedAddress) {
        if (!isEditMode && address.isBlank() && reverseGeocodedAddress != null) {
            address = reverseGeocodedAddress
        }
    }

    var showAddressSearch by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var pendingStartDateMillis by remember { mutableStateOf<Long?>(null) }
    var pendingEndDateMillis by remember { mutableStateOf<Long?>(null) }
    var showCancelAlert by remember { mutableStateOf(false) }

    val hasExistingTimeData = remember(editKey) {
        if (shape != null) {
            val startCal = Calendar.getInstance().apply { timeInMillis = shape.flightStartDate }
            val hasStartTime = startCal.get(Calendar.HOUR_OF_DAY) != 0 || startCal.get(Calendar.MINUTE) != 0
            val hasEndTime = shape.flightEndDate?.let { endMillis ->
                val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }
                endCal.get(Calendar.HOUR_OF_DAY) != 0 || endCal.get(Calendar.MINUTE) != 0
            } ?: false
            hasStartTime || hasEndTime
        } else {
            false
        }
    }
    var isDateOnly by remember(editKey) { mutableStateOf(!hasExistingTimeData) }

    val dateFormat = remember { SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA) }
    val dateTimeFormat = remember { SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA) }

    // ===== hasChanges 계산 (iOS ShapeEditViewModel.hasChanges 정합) =====
    val hasChanges = title != initialTitle ||
        address != initialAddress ||
        radiusText != initialRadius ||
        heightText != initialHeight ||
        memo != initialMemo ||
        flightStartDate != initialFlightStart ||
        flightEndDate != initialFlightEnd ||
        coordinate != initialCoord ||
        selectedDrone?.id != initialDroneId

    // 취소 핸들러 — 변경사항 있으면 알림, 없으면 즉시 닫기 (iOS Toolbar 정합)
    val handleCancel: () -> Unit = {
        if (hasChanges) showCancelAlert = true else onDismiss()
    }

    // 저장 핸들러
    val handleSave: () -> Unit = {
        val now = System.currentTimeMillis()
        // 색상은 선택된 드론의 paletteColor 자동 적용 (iOS 정합 — 색상 선택 섹션 없음)
        val droneColor = selectedDrone?.paletteColor ?: PaletteColor.BLUE
        val resultShape = (shape ?: ShapeModel()).copy(
            id = if (isDuplicateMode) java.util.UUID.randomUUID().toString()
                else (shape?.id ?: java.util.UUID.randomUUID().toString()),
            title = title.ifBlank { defaultTitle },
            shapeType = ShapeType.CIRCLE,
            baseCoordinate = coordinate,
            address = address.ifBlank { null },
            radius = radiusText.toDoubleOrNull() ?: 500.0,
            height = heightText.toDoubleOrNull(),
            memo = memo.ifBlank { null },
            color = droneColor.hex,
            droneId = selectedDrone?.id,
            flightStartDate = flightStartDate,
            flightEndDate = flightEndDate,
            createdAt = if (isDuplicateMode) now else (shape?.createdAt ?: now),
            updatedAt = now,
        )
        onSave(resultShape)
    }

    ModalBottomSheet(
        onDismissRequest = handleCancel,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // ===== TopAppBar (iOS NavigationView + Toolbar 정합) =====
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) stringResource(R.string.shape_edit_title_edit)
                            else stringResource(R.string.shape_edit_title_create),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = handleCancel) {
                        Text(stringResource(R.string.shape_edit_navigation_cancel))
                    }
                },
                actions = {
                    TextButton(
                        onClick = handleSave,
                        enabled = !coordinateParseError,
                    ) {
                        Text(
                            text = stringResource(R.string.shape_edit_navigation_save),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            HorizontalDivider()

            // ===== 본문 =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // ===== Section 1: 드론 (iOS DroneSelectionSection 정합) =====
                EditFormRow(label = stringResource(R.string.shape_edit_drone_label)) {
                    Box {
                        Row(
                            modifier = Modifier.clickable { showDroneDropdown = true },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val drone = selectedDrone
                            if (drone != null) {
                                val droneColor = drone.paletteColor?.composeColor
                                    ?: PaletteColor.BLUE.composeColor
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(droneColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(drone.name)
                            } else {
                                Text(
                                    text = stringResource(R.string.shape_edit_drone_placeholder),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = showDroneDropdown,
                            onDismissRequest = { showDroneDropdown = false },
                        ) {
                            // 미할당 옵션
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.shape_edit_drone_placeholder)) },
                                onClick = {
                                    selectedDrone = null
                                    showDroneDropdown = false
                                },
                            )
                            drones.forEach { drone ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val dc = drone.paletteColor?.composeColor
                                                ?: PaletteColor.BLUE.composeColor
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(dc)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(drone.name)
                                        }
                                    },
                                    onClick = {
                                        selectedDrone = drone
                                        showDroneDropdown = false
                                    },
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()

                // ===== Section 2: 기본 정보 (iOS BasicInfoSection 정합) =====

                // 제목 (인라인 편집)
                EditFormTextFieldRow(
                    label = stringResource(R.string.shape_edit_title_label),
                    value = title,
                    onValueChange = { title = it },
                    placeholder = stringResource(R.string.shape_edit_title_placeholder),
                )
                HorizontalDivider()

                // 좌표 (인라인 편집 + Decimal/DMS 파싱)
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.shape_edit_coordinate_label),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = coordinateText,
                            onValueChange = { newValue ->
                                coordinateText = newValue
                                val parsed = CoordinateParser.parse(newValue)
                                if (parsed != null) {
                                    coordinate = parsed
                                    coordinateParseError = false
                                } else {
                                    coordinateParseError = newValue.isNotBlank()
                                }
                            },
                            modifier = Modifier.width(220.dp),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.shape_edit_coordinate_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            isError = coordinateParseError,
                            textStyle = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (coordinateParseError) {
                        Text(
                            text = stringResource(R.string.shape_edit_coordinate_invalid),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        )
                    } else if (coordinateText.isNotBlank()) {
                        Text(
                            text = coordinate.formattedCoordinate,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        )
                    }
                }
                HorizontalDivider()

                // 주소 (인라인 편집 + 검색 아이콘)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .heightIn(min = 44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.shape_edit_address_search_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (geocodingApi != null) {
                        IconButton(
                            onClick = { showAddressSearch = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.shape_edit_search_address),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.width(220.dp),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.shape_edit_placeholder_address),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )
                }
                HorizontalDivider()

                // 반경
                EditFormTextFieldRow(
                    label = stringResource(R.string.shape_edit_radius_label),
                    value = radiusText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            radiusText = newValue
                        }
                    },
                    placeholder = "500",
                    keyboardType = KeyboardType.Decimal,
                )
                HorizontalDivider()

                // 고도
                EditFormTextFieldRow(
                    label = stringResource(R.string.shape_edit_altitude_label),
                    value = heightText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            heightText = newValue
                        }
                    },
                    placeholder = "0",
                    keyboardType = KeyboardType.Decimal,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ===== Section 3: 비행 기간 (iOS DateSection 정합) =====
                Text(
                    text = stringResource(R.string.shape_edit_section_flight_period),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 시작일
                EditFormClickableRow(
                    label = stringResource(R.string.shape_edit_start_date),
                    value = if (isDateOnly) dateFormat.format(Date(flightStartDate))
                        else dateTimeFormat.format(Date(flightStartDate)),
                    onClick = { showStartDatePicker = true },
                )
                HorizontalDivider()

                // 종료일
                EditFormClickableRow(
                    label = stringResource(R.string.shape_edit_end_date),
                    value = flightEndDate?.let {
                        if (isDateOnly) dateFormat.format(Date(it))
                        else dateTimeFormat.format(Date(it))
                    } ?: stringResource(R.string.common_not_set),
                    onClick = { showEndDatePicker = true },
                )
                HorizontalDivider()

                // 일 단위 모드 토글
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .heightIn(min = 44.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.shape_edit_date_only_mode),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Switch(
                        checked = isDateOnly,
                        onCheckedChange = { newValue ->
                            isDateOnly = newValue
                            if (newValue) {
                                val startCal = Calendar.getInstance().apply {
                                    timeInMillis = flightStartDate
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                flightStartDate = startCal.timeInMillis
                                flightEndDate?.let { endMillis ->
                                    val endCal = Calendar.getInstance().apply {
                                        timeInMillis = endMillis
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    flightEndDate = endCal.timeInMillis
                                }
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ===== Section 4: 메모 (iOS MemoSection 정합 — minHeight 170pt → 180dp) =====
                Text(
                    text = stringResource(R.string.shape_edit_section_memo),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    placeholder = { Text(stringResource(R.string.shape_edit_placeholder_memo)) },
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ===== 변경사항 폐기 알림 (iOS .alert(showingCancelAlert) 정합) =====
    if (showCancelAlert) {
        AlertDialog(
            onDismissRequest = { showCancelAlert = false },
            title = { Text(stringResource(R.string.shape_edit_alert_unsaved_title)) },
            text = { Text(stringResource(R.string.shape_edit_alert_unsaved_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelAlert = false
                        onDismiss()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.shape_edit_alert_unsaved_discard),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelAlert = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    // ===== 시작일 DatePickerDialog =====
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = flightStartDate)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                            if (isDateOnly) {
                                flightStartDate = selectedDateMillis
                            } else {
                                pendingStartDateMillis = selectedDateMillis
                                showStartTimePicker = true
                            }
                        }
                        showStartDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ===== 시작일 TimePickerDialog =====
    if (showStartTimePicker) {
        val existingCal = Calendar.getInstance().apply { timeInMillis = flightStartDate }
        val startTimePickerState = rememberTimePickerState(
            initialHour = existingCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = existingCal.get(Calendar.MINUTE),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = {
                showStartTimePicker = false
                pendingStartDateMillis = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingStartDateMillis?.let { dateMillis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = dateMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, cal.get(Calendar.YEAR))
                                set(Calendar.MONTH, cal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, startTimePickerState.hour)
                                set(Calendar.MINUTE, startTimePickerState.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            flightStartDate = localCal.timeInMillis
                        }
                        showStartTimePicker = false
                        pendingStartDateMillis = null
                    },
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showStartTimePicker = false
                        pendingStartDateMillis = null
                    },
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            title = { Text(stringResource(R.string.shape_edit_select_time)) },
            text = { TimePicker(state = startTimePickerState) },
        )
    }

    // ===== 종료일 DatePickerDialog =====
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = flightEndDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                            if (isDateOnly) {
                                flightEndDate = selectedDateMillis
                            } else {
                                pendingEndDateMillis = selectedDateMillis
                                showEndTimePicker = true
                            }
                        }
                        showEndDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ===== 종료일 TimePickerDialog =====
    if (showEndTimePicker) {
        val existingEndCal = Calendar.getInstance().apply {
            timeInMillis = flightEndDate ?: System.currentTimeMillis()
        }
        val endTimePickerState = rememberTimePickerState(
            initialHour = existingEndCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = existingEndCal.get(Calendar.MINUTE),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = {
                showEndTimePicker = false
                pendingEndDateMillis = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingEndDateMillis?.let { dateMillis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = dateMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, cal.get(Calendar.YEAR))
                                set(Calendar.MONTH, cal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
                                set(Calendar.MINUTE, endTimePickerState.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            flightEndDate = localCal.timeInMillis
                        }
                        showEndTimePicker = false
                        pendingEndDateMillis = null
                    },
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEndTimePicker = false
                        pendingEndDateMillis = null
                    },
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            title = { Text(stringResource(R.string.shape_edit_select_time)) },
            text = { TimePicker(state = endTimePickerState) },
        )
    }

    // ===== 주소 검색 시트 =====
    if (showAddressSearch && geocodingApi != null) {
        SearchAddressSheet(
            geocodingApi = geocodingApi,
            onAddressSelected = { result ->
                address = result.address
                coordinate = result.coordinate
                coordinateText = CoordinateParser.formatDecimal(result.coordinate)
                coordinateParseError = false
                showAddressSearch = false
            },
            onDismiss = { showAddressSearch = false },
        )
    }
}

/**
 * iOS Form 행 정합 — Row + bold label + trailing content.
 * height(30)pt → heightIn(min = 44dp) (터치 영역 확보).
 */
@Composable
private fun EditFormRow(
    label: String,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.weight(1f))
        content()
    }
}

/**
 * iOS Form 행 — bold label + trailing TextField. 인라인 편집.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditFormTextFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(220.dp),
            singleLine = true,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * iOS Form 행 — bold label + trailing value + chevron.right.
 * 클릭 가능 (날짜/좌표/주소 시트 트리거 등).
 */
@Composable
private fun EditFormClickableRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

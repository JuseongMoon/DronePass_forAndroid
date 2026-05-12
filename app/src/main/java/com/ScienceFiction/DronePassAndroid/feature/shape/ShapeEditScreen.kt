package com.ScienceFiction.DronePassAndroid.feature.shape

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
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
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShapeEditScreen(
    shape: ShapeModel? = null,
    initialCoordinate: Coordinate? = null,
    drones: List<DroneModel> = emptyList(),
    reverseGeocodedAddress: String? = null,
    geocodingApi: NaverGeocodingApi? = null,
    onSave: (ShapeModel) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditMode = shape != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 상태 초기화 — shape?.id 를 key 로 부여하여 편집 모드에서 외부 shape 이 변경되면
    // (예: ShapeDetailSheet 의 다른 도형으로 전환) 잔존값이 재사용되지 않고 새로 초기화되게 한다.
    val editKey = shape?.id  // 신규 모드는 null
    val defaultTitle = stringResource(R.string.shape_edit_default_title)
    var title by remember(editKey) {
        mutableStateOf(shape?.title ?: defaultTitle)
    }
    // 주소: 편집 모드면 기존 주소, 신규 모드면 역지오코딩 결과 또는 빈 문자열
    var address by remember(editKey) {
        mutableStateOf(shape?.address ?: reverseGeocodedAddress ?: "")
    }
    var radiusText by remember(editKey) {
        mutableStateOf(shape?.radius?.toString() ?: "500")
    }
    var heightText by remember(editKey) {
        mutableStateOf(shape?.height?.toString() ?: "")
    }
    var memo by remember(editKey) {
        mutableStateOf(shape?.memo ?: "")
    }
    var flightStartDate by remember(editKey) {
        mutableStateOf(shape?.flightStartDate ?: System.currentTimeMillis())
    }
    var flightEndDate by remember(editKey) {
        mutableStateOf(shape?.flightEndDate)
    }

    // 좌표 상태 (편집 가능)
    var coordinate by remember(editKey) {
        mutableStateOf(
            shape?.baseCoordinate ?: initialCoordinate ?: Coordinate(37.5665, 126.9780)
        )
    }
    var coordinateText by remember(editKey) {
        mutableStateOf(
            CoordinateParser.formatDecimal(
                shape?.baseCoordinate ?: initialCoordinate ?: Coordinate(37.5665, 126.9780)
            )
        )
    }
    var coordinateParseError by remember(editKey) { mutableStateOf(false) }

    // 드론 선택 상태
    var selectedDrone by remember {
        mutableStateOf(
            shape?.droneId?.let { droneId -> drones.find { it.id == droneId } }
        )
    }
    var showDroneDropdown by remember { mutableStateOf(false) }

    // 색상 선택 상태
    var selectedColor by remember {
        mutableStateOf(
            shape?.paletteColor ?: selectedDrone?.paletteColor ?: PaletteColor.BLUE
        )
    }

    // 역지오코딩 결과가 나중에 도착할 경우 주소 업데이트
    LaunchedEffect(reverseGeocodedAddress) {
        if (!isEditMode && address.isBlank() && reverseGeocodedAddress != null) {
            address = reverseGeocodedAddress
        }
    }

    // 주소 검색 시트 표시 여부
    var showAddressSearch by remember { mutableStateOf(false) }

    // DatePicker 다이얼로그 상태
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // TimePicker 다이얼로그 상태
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    // DatePicker에서 선택된 날짜를 임시 저장 (시간 선택 전)
    var pendingStartDateMillis by remember { mutableStateOf<Long?>(null) }
    var pendingEndDateMillis by remember { mutableStateOf<Long?>(null) }

    // 일 단위 모드 토글 (기존 시간 데이터가 있으면 시간 포함 모드로 시작)
    val hasExistingTimeData = remember {
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
    var isDateOnly by remember { mutableStateOf(!hasExistingTimeData) }

    val dateFormat = remember { SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA) }
    val dateTimeFormat = remember { SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 헤더
            Text(
                text = if (isEditMode) stringResource(R.string.shape_edit_title_edit) else stringResource(R.string.shape_edit_title_create),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ===== 기본 정보 섹션 =====
            SectionHeader(title = stringResource(R.string.shape_edit_section_basic))

            Spacer(modifier = Modifier.height(12.dp))

            // 제목
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.shape_edit_label_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 좌표 (편집 가능)
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
                label = { Text(stringResource(R.string.shape_edit_label_coordinate)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.shape_edit_coordinate_hint)) },
                isError = coordinateParseError,
                supportingText = if (coordinateParseError) {
                    {
                        Text(
                            text = stringResource(R.string.shape_edit_coordinate_invalid),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else if (!coordinateParseError && coordinateText.isNotBlank()) {
                    {
                        Text(
                            text = coordinate.formattedCoordinate,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 주소 + 검색 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.shape_edit_label_address)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.shape_edit_placeholder_address)) }
                )
                // 주소 검색 아이콘 버튼 (geocodingApi가 주입된 경우에만 표시)
                if (geocodingApi != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showAddressSearch = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.shape_edit_search_address),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 반경
            OutlinedTextField(
                value = radiusText,
                onValueChange = { newValue ->
                    // 숫자와 소수점만 허용
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        radiusText = newValue
                    }
                },
                label = { Text(stringResource(R.string.shape_edit_label_radius)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { Text("m") }
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 고도
            OutlinedTextField(
                value = heightText,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        heightText = newValue
                    }
                },
                label = { Text(stringResource(R.string.shape_edit_label_altitude)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { Text("m") }
            )

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 드론 선택 섹션 =====
            if (drones.isNotEmpty()) {
                SectionHeader(title = stringResource(R.string.shape_edit_section_drone))

                Spacer(modifier = Modifier.height(12.dp))

                // 드론 선택 버튼 (드롭다운 트리거)
                Box {
                    OutlinedButton(
                        onClick = { showDroneDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (selectedDrone != null) {
                            // 선택된 드론의 색상 원
                            val droneColor = selectedDrone!!.paletteColor?.composeColor
                                ?: PaletteColor.BLUE.composeColor
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(droneColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(selectedDrone!!.name)
                        } else {
                            Text(stringResource(R.string.shape_edit_no_drone))
                        }
                    }

                    DropdownMenu(
                        expanded = showDroneDropdown,
                        onDismissRequest = { showDroneDropdown = false }
                    ) {
                        // "연결 안 함" 옵션
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.shape_edit_no_drone)) },
                            onClick = {
                                selectedDrone = null
                                showDroneDropdown = false
                            }
                        )
                        // 활성 드론 목록
                        drones.forEach { drone ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    }
                                },
                                onClick = {
                                    selectedDrone = drone
                                    // 드론 선택 시 색상 자동 적용
                                    drone.paletteColor?.let { selectedColor = it }
                                    showDroneDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ===== 색상 선택 섹션 =====
            SectionHeader(title = stringResource(R.string.shape_edit_section_color))

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaletteColor.entries.forEach { paletteColor ->
                    val isSelected = paletteColor == selectedColor
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(paletteColor.composeColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 3.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { selectedColor = paletteColor },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = paletteColor.koreanName,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 비행 기간 섹션 =====
            SectionHeader(title = stringResource(R.string.shape_edit_section_flight_period))

            Spacer(modifier = Modifier.height(12.dp))

            // 일 단위 모드 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.shape_edit_date_only_mode),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = isDateOnly,
                    onCheckedChange = { newValue ->
                        isDateOnly = newValue
                        if (newValue) {
                            // 일 단위 모드로 전환 시 시간 정보 제거 (날짜만 남김)
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
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 시작일
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.shape_edit_start_date),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = { showStartDatePicker = true }) {
                    Text(
                        if (isDateOnly) dateFormat.format(Date(flightStartDate))
                        else dateTimeFormat.format(Date(flightStartDate))
                    )
                }
            }

            // 종료일
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.shape_edit_end_date),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = { showEndDatePicker = true }) {
                    Text(
                        text = flightEndDate?.let {
                            if (isDateOnly) dateFormat.format(Date(it))
                            else dateTimeFormat.format(Date(it))
                        } ?: stringResource(R.string.common_not_set)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 메모 섹션 =====
            SectionHeader(title = stringResource(R.string.shape_edit_section_memo))

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text(stringResource(R.string.shape_edit_label_memo)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = { Text(stringResource(R.string.shape_edit_placeholder_memo)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 하단 버튼 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
                Button(
                    onClick = {
                        val resultShape = (shape ?: ShapeModel()).copy(
                            title = title.ifBlank { defaultTitle },
                            shapeType = ShapeType.CIRCLE,
                            baseCoordinate = coordinate,
                            address = address.ifBlank { null },
                            radius = radiusText.toDoubleOrNull() ?: 500.0,
                            height = heightText.toDoubleOrNull(),
                            memo = memo.ifBlank { null },
                            color = selectedColor.hex,
                            droneId = selectedDrone?.id,
                            flightStartDate = flightStartDate,
                            flightEndDate = flightEndDate,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(resultShape)
                    },
                    modifier = Modifier.weight(1f),
                    // 좌표 파싱 에러가 있는 동안 저장 비활성화 — 사용자가 잘못된 좌표를
                    // 입력한 채 저장해 마지막 유효 좌표로 의도와 다르게 저장되는 버그 차단.
                    enabled = !coordinateParseError,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.common_save))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 시작일 DatePickerDialog
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = flightStartDate
        )
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
                    }
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // 시작일 TimePickerDialog
    if (showStartTimePicker) {
        val existingCal = Calendar.getInstance().apply { timeInMillis = flightStartDate }
        val startTimePickerState = rememberTimePickerState(
            initialHour = existingCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = existingCal.get(Calendar.MINUTE),
            is24Hour = true
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
                            // DatePicker.selectedDateMillis 는 항상 UTC 자정 epoch 를 반환한다.
                            // 사용자가 "한국 시간 기준 2026-05-13" 을 선택하면 UTC 00:00 epoch ms 가 옴.
                            // UTC 캘린더로 풀어 year/month/day 만 추출 후 사용자 로컬 타임존의
                            // year/month/day 로 다시 합성하여 사용자 의도(KST 2026-05-13 hh:mm)를 보존.
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
                    }
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showStartTimePicker = false
                        pendingStartDateMillis = null
                    }
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            title = { Text(stringResource(R.string.shape_edit_select_time)) },
            text = { TimePicker(state = startTimePickerState) }
        )
    }

    // 종료일 DatePickerDialog
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
                    }
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // 종료일 TimePickerDialog
    if (showEndTimePicker) {
        val existingEndCal = Calendar.getInstance().apply {
            timeInMillis = flightEndDate ?: System.currentTimeMillis()
        }
        val endTimePickerState = rememberTimePickerState(
            initialHour = existingEndCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = existingEndCal.get(Calendar.MINUTE),
            is24Hour = true
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
                    }
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEndTimePicker = false
                        pendingEndDateMillis = null
                    }
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            title = { Text(stringResource(R.string.shape_edit_select_time)) },
            text = { TimePicker(state = endTimePickerState) }
        )
    }

    // 주소 검색 시트
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
            onDismiss = { showAddressSearch = false }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

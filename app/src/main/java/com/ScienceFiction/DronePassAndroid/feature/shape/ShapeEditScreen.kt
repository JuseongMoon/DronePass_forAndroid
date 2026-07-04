package com.ScienceFiction.DronePassAndroid.feature.shape

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.remote.NaverGeocodingApi
import com.ScienceFiction.DronePassAndroid.core.data.repository.reverseGeocodingResultsToAddress
import com.ScienceFiction.DronePassAndroid.core.util.CoordinateParser
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel

import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.launch

internal const val CoordinateInputSheetSkipPartiallyExpanded = false
internal const val CoordinateInputSheetInteractiveDismissEnabled = false
internal const val CoordinateInputSheetHeightFraction = 0.85f
internal val CoordinateInputNavigationHeaderHeight = 44.dp
internal const val ShapeDateTimeSelectionSkipPartiallyExpanded = true
internal val CoordinateValidationSuccessColor = Color(0xFF34C759)
internal val CoordinateAddressResultCardCornerRadius = 12.dp
internal val CoordinateAddressResultCardShadowElevation = 5.dp
internal val CoordinateAddressResultCardShadowColor = Color.Black.copy(alpha = 0.10f)
internal val CoordinateResolvingIndicatorHeight = 180.dp
internal val CoordinateGuideCardBackgroundColor = Color(0xFFF2F2F7)
internal val CoordinateGuideCardCornerRadius = 20.dp
internal val CoordinateGuideCardMaxWidth = 500.dp
internal val CoordinateGuideCardPadding = 16.dp
internal val CoordinateGuideCardVerticalSpacing = 8.dp
internal val ShapeEditMemoMinHeight = 170.dp
internal val ShapeEditNavigationHeaderHeight = 44.dp
internal val ShapeEditNavigationActionSlotWidth = 80.dp
internal const val ShapeEditAddressRowValueMaxLines = 1

internal fun shapeDateTimeSelectionUses24HourClock(systemUses24HourClock: Boolean): Boolean {
    return systemUses24HourClock
}

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
    editDefaults: ShapeEditDefaults = ShapeEditDefaults(),
    fallbackSelectedDroneId: String? = null,
    defaultShapeColor: String = PaletteColor.BLUE.hex,
    reverseGeocodedAddress: String? = null,
    geocodingApi: NaverGeocodingApi? = null,
    isDuplicateMode: Boolean = false,
    onPersistEditDefaults: (ShapeEditDefaults) -> Unit = {},
    onDateOnlyModeChanged: (Boolean) -> Unit = {},
    onSave: (ShapeModel, ShapeModel?, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    // 복제 모드: shape 전체 데이터 활용하되 신규 ID + 새 createdAt 으로 저장.
    val isEditMode = isExistingShapeEditMode(
        shape = shape,
        isDuplicateMode = isDuplicateMode,
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val editKey = if (isDuplicateMode) "duplicate-${shape?.id}" else shape?.id
    val defaultTitle = stringResource(R.string.shape_edit_default_title)
    val errorCoordinateRequired = stringResource(R.string.shape_edit_error_coordinate_required)
    val errorNoAddress = stringResource(R.string.shape_edit_error_no_address)
    val errorNoCoordinate = stringResource(R.string.shape_edit_error_no_coordinate)
    val errorRadiusRequired = stringResource(R.string.shape_edit_error_radius_required)
    val coordinateAddressFallback = stringResource(R.string.coordinate_alert_address_not_found_fallback)
    val nowForInitialValues = remember(editKey) { System.currentTimeMillis() }
    val originalShapeAtEditStart = remember(editKey) { shape }
    val coroutineScope = rememberCoroutineScope()

    // ===== 초기값 보관 (hasChanges 비교용) =====
    val initialTitle = remember(editKey) {
        resolveInitialShapeEditTitle(shape)
    }
    val initialAddress = remember(editKey) { shape?.address ?: reverseGeocodedAddress ?: "" }
    val initialRadius = remember(editKey, editDefaults.radius) {
        resolveInitialShapeEditRadius(
            shape = shape,
            editDefaults = editDefaults,
        )
    }
    val initialHeight = remember(editKey, editDefaults.height) {
        resolveInitialShapeEditHeight(
            shape = shape,
            editDefaults = editDefaults,
        )
    }
    val initialMemo = remember(editKey) { shape?.memo ?: "" }
    val initialFlightStart = remember(editKey, editDefaults.startDate) {
        resolveInitialShapeEditFlightStart(
            shape = shape,
            editDefaults = editDefaults,
            now = nowForInitialValues,
        )
    }
    val initialFlightEnd = remember(editKey, editDefaults.endDate) {
        resolveInitialShapeEditFlightEnd(
            shape = shape,
            editDefaults = editDefaults,
            now = nowForInitialValues,
        )
    }
    val initialCoord = remember(editKey) {
        shape?.baseCoordinate ?: initialCoordinate
    }
    val initialDroneId = remember(
        editKey,
        shape?.droneId,
        drones,
        editDefaults.selectedDroneId,
        fallbackSelectedDroneId,
    ) {
        resolveInitialShapeEditDroneId(
            shape = shape,
            activeDrones = drones,
            editDefaults = editDefaults,
            fallbackSelectedDroneId = fallbackSelectedDroneId,
        )
    }

    // ===== 편집 상태 =====
    var title by remember(editKey) { mutableStateOf(initialTitle) }
    var address by remember(editKey) { mutableStateOf(initialAddress) }
    var radiusText by remember(editKey, initialRadius) { mutableStateOf(initialRadius) }
    var heightText by remember(editKey, initialHeight) { mutableStateOf(initialHeight) }
    var memo by remember(editKey) { mutableStateOf(initialMemo) }
    var flightStartDate by remember(editKey, initialFlightStart) { mutableLongStateOf(initialFlightStart) }
    var flightEndDate by remember(editKey, initialFlightEnd) { mutableLongStateOf(initialFlightEnd) }
    var coordinate by remember(editKey) { mutableStateOf(initialCoord) }
    var coordinateText by remember(editKey) {
        mutableStateOf(initialCoord?.let { formatShapeEditCoordinateText(it) }.orEmpty())
    }

    var selectedDroneId by remember(editKey, initialDroneId) {
        mutableStateOf(initialDroneId)
    }
    val selectedDrone = selectedDroneId?.let { id -> drones.find { it.id == id } }
    var showDroneDropdown by remember { mutableStateOf(false) }

    // 역지오코딩 결과가 나중에 도착할 경우 주소 업데이트
    LaunchedEffect(reverseGeocodedAddress) {
        if (!isEditMode && address.isEmpty() && reverseGeocodedAddress != null) {
            address = reverseGeocodedAddress
        }
    }

    var showCoordinateInput by remember { mutableStateOf(false) }
    var coordinateInputText by remember(editKey) { mutableStateOf(coordinateText) }
    var coordinateInputValidation by remember(editKey) { mutableStateOf<Boolean?>(null) }
    var isResolvingCoordinateAddress by remember { mutableStateOf(false) }
    var coordinateResolveRequestId by remember { mutableIntStateOf(0) }
    var coordinateAddressSearchResult by remember(editKey) {
        mutableStateOf<CoordinateAddressSearchResult?>(null)
    }
    var pendingCoordinateWithoutAddress by remember { mutableStateOf<Coordinate?>(null) }
    var showAddressSearch by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showCancelAlert by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isDateOnly by remember(editKey, editDefaults.isDateOnly) {
        mutableStateOf(editDefaults.isDateOnly)
    }
    val showRadiusField = shouldShowShapeEditRadiusField(shape)

    val dateFormat = remember { shapeEditDateOnlyFormat() }
    val dateTimeFormat = remember { localizedShapeDateTimeFormat() }

    // ===== hasChanges 계산 (iOS ShapeEditViewModel.hasChanges 정합) =====
    val hasChanges = hasShapeEditContentChanges(
        title = title,
        initialTitle = initialTitle,
        address = address,
        initialAddress = initialAddress,
        radius = radiusText,
        initialRadius = initialRadius,
        height = heightText,
        initialHeight = initialHeight,
        memo = memo,
        initialMemo = initialMemo,
        coordinate = coordinate,
        initialCoordinate = initialCoord,
        selectedDroneId = selectedDroneId,
        initialDroneId = initialDroneId,
    )

    // 취소 핸들러 — 변경사항 있으면 알림, 없으면 즉시 닫기 (iOS Toolbar 정합)
    val handleCancel: () -> Unit = {
        if (hasChanges) showCancelAlert = true else onDismiss()
    }

    val dismissCoordinateInput: () -> Unit = {
        coordinateResolveRequestId += 1
        showCoordinateInput = false
        coordinateInputValidation = null
        isResolvingCoordinateAddress = false
        coordinateAddressSearchResult = null
        pendingCoordinateWithoutAddress = null
    }

    // 저장 핸들러
    val handleSave: () -> Unit = save@{
        val coordinateForSave = coordinate
        val validationError = validateShapeEditSaveFields(
            coordinate = coordinateForSave,
            address = address,
            radius = radiusText,
            requiresRadius = shouldRequireShapeEditRadius(shape),
        )
        if (validationError != null) {
            errorMessage = when (validationError) {
                ShapeEditSaveValidationError.COORDINATE_REQUIRED -> errorCoordinateRequired
                ShapeEditSaveValidationError.RADIUS_REQUIRED -> errorRadiusRequired
                ShapeEditSaveValidationError.NO_COORDINATE -> errorNoCoordinate
            }
            return@save
        }

        val now = System.currentTimeMillis()
        // 색상은 선택된 드론의 color 문자열을 그대로 적용 (iOS ShapeEditViewModel selectedColor 정합)
        val selectedColor = resolveShapeEditSelectedColor(
            selectedDrone = selectedDrone,
            defaultColor = defaultShapeColor,
        )
        onPersistEditDefaults(
            ShapeEditDefaults(
                selectedDroneId = selectedDroneId,
                radius = radiusText,
                height = heightText,
                startDate = flightStartDate,
                endDate = flightEndDate,
                isDateOnly = isDateOnly,
            )
        )
        val resultCoordinate = coordinateForSave ?: run {
            errorMessage = errorNoCoordinate
            return@save
        }
        val resultShape = buildShapeEditSavedShape(
            originalShape = shape,
            isDuplicateMode = isDuplicateMode,
            generatedId = java.util.UUID.randomUUID().toString(),
            title = title,
            defaultTitle = defaultTitle,
            coordinate = resultCoordinate,
            address = address,
            noAddressFallback = errorNoAddress,
            radius = radiusText,
            height = heightText,
            memo = memo,
            selectedColor = selectedColor,
            selectedDroneId = selectedDroneId,
            flightStartDate = flightStartDate,
            flightEndDate = flightEndDate,
            now = now,
        )
        onSave(
            resultShape,
            if (isEditMode) originalShapeAtEditStart else null,
        ) { saveErrorMessage ->
            errorMessage = saveErrorMessage
        }
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
            ShapeEditNavigationHeader(
                onCancel = handleCancel,
                onSave = handleSave,
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
                            val paletteColor = drone?.paletteColor
                            val selectedPaletteColor = paletteColor?.takeIf(::shouldShowShapeEditDroneColorIndicator)
                            if (selectedPaletteColor != null) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(selectedPaletteColor.composeColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(
                                    painter = painterResource(ShapeEditDronePlaceholderIconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            if (drone != null) {
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
                            drones.forEach { drone ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            drone.paletteColor
                                                ?.takeIf(::shouldShowShapeEditDroneColorIndicator)
                                                ?.let { paletteColor ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(paletteColor.composeColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }
                                            Text(drone.name)
                                            if (shouldShowShapeEditDroneSelectionCheckmark(drone.id, selectedDroneId)) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedDroneId = drone.id
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
                EditFormClickableRow(
                    label = stringResource(R.string.shape_edit_coordinate_label),
                    value = shapeEditDisplayText(
                        value = coordinateText,
                        placeholder = stringResource(R.string.shape_edit_coordinate_placeholder),
                    ),
                    valueColor = if (isShapeEditPlaceholder(coordinateText)) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    onClick = {
                        coordinateInputText = initialCoordinateInputSheetText(coordinateText)
                        coordinateInputValidation = null
                        coordinateAddressSearchResult = null
                        showCoordinateInput = true
                    },
                )
                HorizontalDivider()

                // 주소 (iOS처럼 행 탭 → 주소 검색 화면)
                if (geocodingApi != null) {
                    EditFormClickableRow(
                        label = stringResource(R.string.shape_edit_label_address),
                        value = shapeEditDisplayText(
                            value = address,
                            placeholder = stringResource(R.string.shape_edit_address_search_placeholder),
                        ),
                        valueColor = if (isShapeEditPlaceholder(address)) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        valueMaxLines = ShapeEditAddressRowValueMaxLines,
                        valueOverflow = TextOverflow.Ellipsis,
                        onClick = { showAddressSearch = true },
                    )
                } else {
                    EditFormTextFieldRow(
                        label = stringResource(R.string.shape_edit_label_address),
                        value = address,
                        onValueChange = { address = it },
                        placeholder = stringResource(R.string.shape_edit_placeholder_address),
                    )
                }
                HorizontalDivider()

                // 반경
                if (showRadiusField) {
                    EditFormTextFieldRow(
                        label = stringResource(R.string.shape_edit_radius_label),
                        value = radiusText,
                        onValueChange = { newValue ->
                            radiusText = filterShapeEditNumberInput(newValue)
                        },
                        placeholder = stringResource(R.string.shape_edit_radius_placeholder),
                        keyboardType = KeyboardType.Number,
                    )
                    HorizontalDivider()
                }

                // 고도
                EditFormTextFieldRow(
                    label = stringResource(R.string.shape_edit_altitude_label),
                    value = heightText,
                    onValueChange = { newValue ->
                        heightText = filterShapeEditNumberInput(newValue)
                    },
                    placeholder = stringResource(R.string.shape_edit_altitude_placeholder),
                    keyboardType = KeyboardType.Number,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ===== Section 3: iOS DateSection 정합 — 헤더 없이 날짜 행만 표시 =====
                if (ShowShapeEditFlightPeriodSectionHeader) {
                    Text(
                        text = stringResource(R.string.shape_edit_section_flight_period),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

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
                    value = if (isDateOnly) dateFormat.format(Date(flightEndDate))
                        else dateTimeFormat.format(Date(flightEndDate)),
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
                            val resolvedPeriod = resolveShapeEditFlightPeriodOnDateOnlyModeToggle(
                                startDate = flightStartDate,
                                endDate = flightEndDate,
                                isDateOnly = newValue,
                            )
                            isDateOnly = newValue
                            flightStartDate = resolvedPeriod.startDate
                            flightEndDate = resolvedPeriod.endDate ?: resolvedPeriod.startDate
                            onDateOnlyModeChanged(newValue)
                        },
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ===== Section 4: 메모 (iOS MemoSection 정합 — minHeight 170pt) =====
                Text(
                    text = stringResource(R.string.shape_edit_label_memo),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ShapeEditMemoMinHeight),
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

    if (showCoordinateInput) {
        val selectCoordinateAddressResult: (CoordinateAddressSearchResult) -> Unit = { result ->
            coordinate = result.coordinate
            coordinateText = formatShapeEditCoordinateText(result.coordinate)
            address = result.address
            coordinateInputValidation = null
            showCoordinateInput = false
            coordinateAddressSearchResult = null
        }
        CoordinateInputSheet(
            coordinateText = coordinateInputText,
            isCoordinateValid = coordinateInputValidation == true,
            isCoordinateInvalid = coordinateInputValidation == false,
            isResolvingAddress = isResolvingCoordinateAddress,
            addressSearchResult = coordinateAddressSearchResult,
            onCoordinateTextChange = { newValue ->
                coordinateInputText = newValue
                coordinateResolveRequestId += 1
                isResolvingCoordinateAddress = false
                coordinateAddressSearchResult = null
                coordinateInputValidation = coordinateInputValidationAfterTextChange()
            },
            onSearch = {
                val searchText = coordinateInputText
                val parsed = CoordinateParser.parse(searchText)
                coordinateInputValidation = coordinateInputValidationForSearch(parsed)
                if (parsed != null) {
                    if (geocodingApi == null) {
                        pendingCoordinateWithoutAddress = parsed
                    } else {
                        isResolvingCoordinateAddress = true
                        coordinateAddressSearchResult = null
                        val requestId = ++coordinateResolveRequestId
                        coroutineScope.launch {
                            val resolvedAddress = try {
                                val response = geocodingApi.reverseGeocode(
                                    coords = "${parsed.longitude},${parsed.latitude}",
                                )
                                if (response.status.code == 0) {
                                    reverseGeocodingResultsToAddress(response.results)
                                } else {
                                    ""
                                }
                            } catch (_: Exception) {
                                ""
                            }
                            if (requestId != coordinateResolveRequestId) return@launch

                            isResolvingCoordinateAddress = false
                            if (resolvedAddress.isEmpty()) {
                                pendingCoordinateWithoutAddress = parsed
                            } else {
                                coordinateAddressSearchResult = coordinateAddressSearchResultOrNull(
                                    resolvedAddress = resolvedAddress,
                                    coordinate = parsed,
                                    originalText = searchText,
                                )
                            }
                        }
                    }
                }
            },
            onConfirm = {
                coordinateAddressSearchResult?.let(selectCoordinateAddressResult)
            },
            onResultSelected = selectCoordinateAddressResult,
            onDismiss = dismissCoordinateInput,
        )
    }

    pendingCoordinateWithoutAddress?.let { pendingCoordinate ->
        AlertDialog(
            onDismissRequest = {
                pendingCoordinateWithoutAddress = null
            },
            title = { Text(stringResource(R.string.coordinate_alert_address_not_found_title)) },
            text = { Text(stringResource(R.string.coordinate_alert_address_not_found_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        coordinate = pendingCoordinate
                        coordinateText = formatShapeEditCoordinateText(pendingCoordinate)
                        address = resolveCoordinateAddressForSave(
                            resolvedAddress = "",
                            fallbackAddress = coordinateAddressFallback,
                        )
                        coordinateInputValidation = null
                        showCoordinateInput = false
                        pendingCoordinateWithoutAddress = null
                    },
                ) {
                    Text(stringResource(R.string.common_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCoordinateWithoutAddress = null }) {
                    Text(stringResource(R.string.common_no))
                }
            },
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.shape_edit_alert_error_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
        )
    }

    // ===== 시작일 DateTimeSelectionView 정합 시트 =====
    if (showStartDatePicker) {
        ShapeDateTimeSelectionSheet(
            initialDateMillis = flightStartDate,
            isDateOnly = isDateOnly,
            title = stringResource(R.string.shape_edit_start_date_select),
            onDateSelected = { selectedDate ->
                val newStart = selectedStartShapeEditDate(
                    selectedDate = selectedDate,
                    isDateOnly = isDateOnly,
                )
                flightStartDate = newStart
                flightEndDate = coerceShapeEditEndDateAtOrAfterStart(
                    startDate = newStart,
                    proposedEndDate = flightEndDate,
                    isDateOnly = isDateOnly,
                )
                showStartDatePicker = false
            },
        )
    }

    // ===== 종료일 DateTimeSelectionView 정합 시트 =====
    if (showEndDatePicker) {
        val endDateSelectableDates = remember(flightStartDate) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val localSelectedDay = shapeEditLocalMillisFromDatePicker(
                        dateMillis = utcTimeMillis,
                        hour = 0,
                        minute = 0,
                    )
                    return endOfShapeEditLocalDay(localSelectedDay) >= flightStartDate
                }
            }
        }
        ShapeDateTimeSelectionSheet(
            initialDateMillis = coerceShapeEditEndDateAtOrAfterStart(
                startDate = flightStartDate,
                proposedEndDate = flightEndDate,
                isDateOnly = isDateOnly,
            ),
            isDateOnly = isDateOnly,
            title = stringResource(R.string.shape_edit_end_date_select),
            selectableDates = endDateSelectableDates,
            minimumDateMillis = flightStartDate,
            onDateSelected = { selectedDate ->
                val proposedEnd = selectedEndShapeEditDate(
                    selectedDate = selectedDate,
                    isDateOnly = isDateOnly,
                )
                flightEndDate = coerceShapeEditEndDateAtOrAfterStart(
                    startDate = flightStartDate,
                    proposedEndDate = proposedEnd,
                    isDateOnly = isDateOnly,
                )
                showEndDatePicker = false
            },
        )
    }

    // ===== 주소 검색 시트 =====
    if (showAddressSearch && geocodingApi != null) {
        SearchAddressSheet(
            geocodingApi = geocodingApi,
            onAddressSelected = { result ->
                address = result.address
                result.coordinate?.let { selectedCoordinate ->
                    coordinate = selectedCoordinate
                    coordinateText = formatShapeEditCoordinateText(selectedCoordinate)
                }
                showAddressSearch = false
            },
            onDismiss = { showAddressSearch = false },
        )
    }
}

/**
 * iOS ShapeEditView inline toolbar 정합. 이 화면은 navigation title 을 표시하지 않는다.
 */
@Composable
private fun ShapeEditNavigationHeader(
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ShapeEditNavigationHeaderHeight)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onCancel,
            modifier = Modifier.width(ShapeEditNavigationActionSlotWidth),
        ) {
            Text(stringResource(R.string.shape_edit_navigation_cancel))
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(
            onClick = onSave,
            modifier = Modifier.width(ShapeEditNavigationActionSlotWidth),
        ) {
            Text(
                text = stringResource(R.string.shape_edit_navigation_save),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoordinateInputSheet(
    coordinateText: String,
    isCoordinateValid: Boolean,
    isCoordinateInvalid: Boolean,
    isResolvingAddress: Boolean,
    addressSearchResult: CoordinateAddressSearchResult?,
    onCoordinateTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    onConfirm: () -> Unit,
    onResultSelected: (CoordinateAddressSearchResult) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = CoordinateInputSheetSkipPartiallyExpanded,
    )
    ModalBottomSheet(
        onDismissRequest = {
            if (CoordinateInputSheetInteractiveDismissEnabled) onDismiss()
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(CoordinateInputSheetHeightFraction)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CoordinateInputNavigationHeaderHeight)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.width(80.dp)) {
                    Text(stringResource(R.string.coordinate_cancel))
                }
                Text(
                    text = stringResource(R.string.coordinate_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onConfirm,
                    enabled = canConfirmCoordinateInput(isCoordinateInvalid),
                    modifier = Modifier.width(80.dp),
                ) {
                    Text(stringResource(R.string.coordinate_confirm))
                }
            }
            HorizontalDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoordinateInputSearchField(
                        coordinateText = coordinateText,
                        onCoordinateTextChange = onCoordinateTextChange,
                        onClear = {
                            onCoordinateTextChange("")
                        },
                        onSearch = onSearch,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = onSearch,
                        enabled = shouldEnableSearchAddressSubmit(coordinateText),
                        shape = RoundedCornerShape(SearchAddressBarCornerRadius),
                        contentPadding = PaddingValues(
                            horizontal = SearchAddressSearchButtonHorizontalPadding,
                            vertical = SearchAddressSearchButtonVerticalPadding,
                        ),
                    ) {
                        Text(stringResource(R.string.common_search))
                    }
                }

                when {
                    isCoordinateInvalid -> {
                        Text(
                            text = stringResource(R.string.coordinate_validation_invalid),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    isCoordinateValid -> {
                        Text(
                            text = stringResource(R.string.coordinate_validation_valid),
                            color = CoordinateValidationSuccessColor,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                CoordinateGuideCard(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp),
                )

                if (isResolvingAddress) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CoordinateResolvingIndicatorHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                addressSearchResult
                    ?.takeIf { !isResolvingAddress && !isCoordinateInvalid }
                    ?.let { result ->
                        CoordinateAddressResultCard(
                            result = result,
                            onClick = { onResultSelected(result) },
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
            }
        }
    }
}

@Composable
private fun CoordinateAddressResultCard(
    result: CoordinateAddressSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(CoordinateAddressResultCardCornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = CoordinateAddressResultCardShadowElevation,
                shape = cardShape,
                ambientColor = CoordinateAddressResultCardShadowColor,
                spotColor = CoordinateAddressResultCardShadowColor,
                clip = false,
            )
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = result.address,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = result.originalText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CoordinateInputSearchField(
    coordinateText: String,
    onCoordinateTextChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = coordinateText,
        onValueChange = onCoordinateTextChange,
        modifier = modifier
            .clip(RoundedCornerShape(SearchAddressBarCornerRadius))
            .background(SearchAddressBarBackgroundColor)
            .padding(SearchAddressBarInnerPadding),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = SearchAddressBarIconColor,
                    modifier = Modifier.size(18.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (coordinateText.isEmpty()) {
                        Text(
                            text = stringResource(R.string.coordinate_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SearchAddressBarIconColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (coordinateText.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = stringResource(R.string.common_clear),
                            tint = SearchAddressBarIconColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
    )
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

@Composable
private fun CoordinateGuideCard(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = CoordinateGuideCardMaxWidth)
            .fillMaxWidth()
            .background(
                color = CoordinateGuideCardBackgroundColor,
                shape = RoundedCornerShape(CoordinateGuideCardCornerRadius),
            )
            .padding(CoordinateGuideCardPadding),
        verticalArrangement = Arrangement.spacedBy(CoordinateGuideCardVerticalSpacing),
    ) {
        Text(
            text = stringResource(R.string.coordinate_guide),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.coordinate_format_title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        coordinateGuideExampleResourceIds().forEach { resId ->
            Text(
                text = stringResource(resId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    valueMaxLines: Int = Int.MAX_VALUE,
    valueOverflow: TextOverflow = TextOverflow.Clip,
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
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            maxLines = valueMaxLines,
            overflow = valueOverflow,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShapeDateTimeSelectionSheet(
    initialDateMillis: Long,
    isDateOnly: Boolean,
    title: String,
    selectableDates: SelectableDates? = null,
    minimumDateMillis: Long? = null,
    onDateSelected: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = ShapeDateTimeSelectionSkipPartiallyExpanded,
    )
    val initialCalendar = remember(initialDateMillis) {
        Calendar.getInstance().apply { timeInMillis = initialDateMillis }
    }
    val context = LocalContext.current
    val use24HourClock = shapeDateTimeSelectionUses24HourClock(
        DateFormat.is24HourFormat(context),
    )
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = shapeEditDatePickerMillisFromLocalMillis(initialDateMillis),
        selectableDates = selectableDates ?: object : SelectableDates {},
    )
    val timePickerState = rememberTimePickerState(
        initialHour = initialCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialCalendar.get(Calendar.MINUTE),
        is24Hour = use24HourClock,
    )
    LaunchedEffect(
        minimumDateMillis,
        isDateOnly,
        datePickerState.selectedDateMillis,
        timePickerState.hour,
        timePickerState.minute,
    ) {
        if (!isDateOnly && minimumDateMillis != null) {
            val coercedSelection = coerceShapeEditTimePickerSelectionAtOrAfterMinimum(
                selectedDateMillis = datePickerState.selectedDateMillis,
                initialDateMillis = initialDateMillis,
                hour = timePickerState.hour,
                minute = timePickerState.minute,
                minimumDateMillis = minimumDateMillis,
            )
            if (timePickerState.hour != coercedSelection.hour) {
                timePickerState.hour = coercedSelection.hour
            }
            if (timePickerState.minute != coercedSelection.minute) {
                timePickerState.minute = coercedSelection.minute
            }
        }
    }
    fun applyCurrentSelectionAndDismiss() {
        val selectedLocalMillis = shapeEditSelectedLocalMillisFromDateTimePicker(
            selectedDateMillis = datePickerState.selectedDateMillis,
            initialDateMillis = initialDateMillis,
            hour = timePickerState.hour,
            minute = timePickerState.minute,
            isDateOnly = isDateOnly,
        )
        onDateSelected(selectedLocalMillis)
    }

    ModalBottomSheet(
        onDismissRequest = { applyCurrentSelectionAndDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { applyCurrentSelectionAndDismiss() },
                    modifier = Modifier.width(64.dp),
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(64.dp))
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            DatePicker(state = datePickerState)
            if (!isDateOnly) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TimePicker(state = timePickerState)
                }
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(48.dp),
                onClick = { applyCurrentSelectionAndDismiss() },
            ) {
                Text(stringResource(R.string.date_time_done))
            }
        }
    }
}

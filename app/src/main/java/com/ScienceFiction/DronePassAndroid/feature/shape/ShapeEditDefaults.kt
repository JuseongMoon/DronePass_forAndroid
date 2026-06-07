package com.ScienceFiction.DronePassAndroid.feature.shape

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.PaletteColor
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

internal const val DefaultShapeEditDateOnlyMode = false
internal const val ShowShapeEditFlightPeriodSectionHeader = false

data class ShapeEditDefaults(
    val selectedDroneId: String? = null,
    val radius: String? = null,
    val height: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isDateOnly: Boolean = DefaultShapeEditDateOnlyMode,
)

internal object ShapeEditPreferenceKeys {
    val LAST_SELECTED_DRONE_ID = stringPreferencesKey("lastSelectedDroneId")
    val LAST_RADIUS = stringPreferencesKey("lastRadius")
    val LAST_HEIGHT = stringPreferencesKey("lastHeight")
    val LAST_START_DATE = longPreferencesKey("lastStartDate")
    val LAST_END_DATE = longPreferencesKey("lastEndDate")
    val DATE_ONLY_MODE = booleanPreferencesKey("isDateOnlyMode")

    val LEGACY_LAST_SELECTED_DRONE_ID = stringPreferencesKey("last_selected_drone_id")
    val LEGACY_LAST_RADIUS = stringPreferencesKey("last_radius")
    val LEGACY_LAST_HEIGHT = stringPreferencesKey("last_height")
    val LEGACY_LAST_START_DATE = longPreferencesKey("last_start_date")
    val LEGACY_LAST_END_DATE = longPreferencesKey("last_end_date")
    val LEGACY_DATE_ONLY_MODE = booleanPreferencesKey("is_date_only_mode")
}

internal fun storedShapeEditDefaults(preferences: Preferences): ShapeEditDefaults {
    return ShapeEditDefaults(
        selectedDroneId = preferences[ShapeEditPreferenceKeys.LAST_SELECTED_DRONE_ID]
            ?: preferences[ShapeEditPreferenceKeys.LEGACY_LAST_SELECTED_DRONE_ID],
        radius = (
            preferences[ShapeEditPreferenceKeys.LAST_RADIUS]
                ?: preferences[ShapeEditPreferenceKeys.LEGACY_LAST_RADIUS]
            )?.takeIf { it.isNotEmpty() },
        height = (
            preferences[ShapeEditPreferenceKeys.LAST_HEIGHT]
                ?: preferences[ShapeEditPreferenceKeys.LEGACY_LAST_HEIGHT]
            )?.takeIf { it.isNotEmpty() },
        startDate = preferences[ShapeEditPreferenceKeys.LAST_START_DATE]
            ?: preferences[ShapeEditPreferenceKeys.LEGACY_LAST_START_DATE],
        endDate = preferences[ShapeEditPreferenceKeys.LAST_END_DATE]
            ?: preferences[ShapeEditPreferenceKeys.LEGACY_LAST_END_DATE],
        isDateOnly = preferences[ShapeEditPreferenceKeys.DATE_ONLY_MODE]
            ?: preferences[ShapeEditPreferenceKeys.LEGACY_DATE_ONLY_MODE]
            ?: DefaultShapeEditDateOnlyMode,
    )
}

internal fun MutablePreferences.writeShapeEditDefaults(defaults: ShapeEditDefaults) {
    val selectedDroneId = defaults.selectedDroneId
    if (selectedDroneId == null) {
        remove(ShapeEditPreferenceKeys.LAST_SELECTED_DRONE_ID)
    } else {
        this[ShapeEditPreferenceKeys.LAST_SELECTED_DRONE_ID] = selectedDroneId
    }
    remove(ShapeEditPreferenceKeys.LEGACY_LAST_SELECTED_DRONE_ID)

    val radius = defaults.radius.orEmpty()
    if (radius.isEmpty()) {
        remove(ShapeEditPreferenceKeys.LAST_RADIUS)
    } else {
        this[ShapeEditPreferenceKeys.LAST_RADIUS] = radius
    }
    remove(ShapeEditPreferenceKeys.LEGACY_LAST_RADIUS)

    val height = defaults.height.orEmpty()
    if (height.isEmpty()) {
        remove(ShapeEditPreferenceKeys.LAST_HEIGHT)
    } else {
        this[ShapeEditPreferenceKeys.LAST_HEIGHT] = height
    }
    remove(ShapeEditPreferenceKeys.LEGACY_LAST_HEIGHT)

    defaults.startDate?.let {
        this[ShapeEditPreferenceKeys.LAST_START_DATE] = it
    } ?: remove(ShapeEditPreferenceKeys.LAST_START_DATE)
    remove(ShapeEditPreferenceKeys.LEGACY_LAST_START_DATE)

    defaults.endDate?.let {
        this[ShapeEditPreferenceKeys.LAST_END_DATE] = it
    } ?: remove(ShapeEditPreferenceKeys.LAST_END_DATE)
    remove(ShapeEditPreferenceKeys.LEGACY_LAST_END_DATE)

    writeShapeEditDateOnlyMode(defaults.isDateOnly)
}

internal fun MutablePreferences.writeShapeEditDateOnlyMode(isDateOnly: Boolean) {
    this[ShapeEditPreferenceKeys.DATE_ONLY_MODE] = isDateOnly
    remove(ShapeEditPreferenceKeys.LEGACY_DATE_ONLY_MODE)
}

internal data class ShapeEditFlightPeriod(
    val startDate: Long,
    val endDate: Long?,
)

internal fun resolveShapeEditFlightPeriodOnDateOnlyModeToggle(
    startDate: Long,
    endDate: Long?,
    isDateOnly: Boolean,
): ShapeEditFlightPeriod {
    if (isDateOnly) {
        return ShapeEditFlightPeriod(
            startDate = startOfShapeEditLocalDay(startDate),
            endDate = endOfShapeEditLocalDay(endDate ?: startDate),
        )
    }

    return ShapeEditFlightPeriod(
        startDate = startDate,
        endDate = endDate,
    )
}

internal fun coordinateGuideExampleResourceIds(): List<Int> {
    return listOf(
        R.string.coordinate_example_dms,
        R.string.coordinate_example_decimal_degrees,
        R.string.coordinate_example_simple_decimal,
        R.string.coordinate_example_geo_uri,
    )
}

internal fun resolveInitialShapeEditDroneId(
    shape: ShapeModel?,
    activeDrones: List<DroneModel>,
    editDefaults: ShapeEditDefaults,
    fallbackSelectedDroneId: String?,
): String? {
    if (shape != null && shape.title.isNotEmpty()) {
        return shape.droneId ?: activeDrones.firstOrNull()?.id
    }

    return sequenceOf(editDefaults.selectedDroneId, fallbackSelectedDroneId)
        .filterNotNull()
        .firstOrNull { candidate -> activeDrones.any { it.id == candidate } }
        ?: activeDrones.firstOrNull()?.id
}

internal fun resolveInitialShapeEditTitle(shape: ShapeModel?): String {
    return shape?.title ?: ""
}

internal fun isExistingShapeEditMode(
    shape: ShapeModel?,
    isDuplicateMode: Boolean,
): Boolean {
    return shape != null && shape.title.isNotEmpty() && !isDuplicateMode
}

internal fun resolveShapeEditNavigationTitle(isEditMode: Boolean): String {
    return ""
}

internal fun resolveInitialShapeEditRadius(
    shape: ShapeModel?,
    editDefaults: ShapeEditDefaults,
): String {
    val shapeRadius = shape?.radius?.let { String.format(Locale.US, "%.0f", it) }.orEmpty()
    if (shape != null && shape.title.isNotEmpty()) return shapeRadius
    return editDefaults.radius ?: shapeRadius
}

internal fun resolveInitialShapeEditHeight(
    shape: ShapeModel?,
    editDefaults: ShapeEditDefaults,
): String {
    val shapeHeight = shape?.height?.let { String.format(Locale.US, "%.0f", it) }.orEmpty()
    if (shape != null && shape.title.isNotEmpty()) return shapeHeight
    return editDefaults.height ?: shapeHeight
}

internal fun resolveInitialShapeEditFlightStart(
    shape: ShapeModel?,
    editDefaults: ShapeEditDefaults,
    now: Long,
): Long {
    if (shape != null && shape.title.isNotEmpty()) return shape.flightStartDate
    return editDefaults.startDate ?: now
}

internal fun resolveInitialShapeEditFlightEnd(
    shape: ShapeModel?,
    editDefaults: ShapeEditDefaults,
    now: Long,
): Long {
    if (shape != null && shape.title.isNotEmpty()) {
        return shape.flightEndDate ?: now
    }
    return editDefaults.endDate ?: now
}

internal fun formatShapeEditCoordinateText(coordinate: Coordinate): String {
    return coordinate.formattedCoordinate
}

@Suppress("UNUSED_PARAMETER")
internal fun initialCoordinateInputSheetText(currentCoordinateText: String): String {
    return ""
}

internal fun coordinateInputValidationAfterTextChange(): Boolean? {
    return null
}

internal fun coordinateInputValidationForSearch(parsedCoordinate: Coordinate?): Boolean {
    return parsedCoordinate != null
}

internal data class CoordinateAddressSearchResult(
    val address: String,
    val coordinate: Coordinate,
    val originalText: String,
)

internal fun coordinateAddressSearchResultOrNull(
    resolvedAddress: String,
    coordinate: Coordinate,
    originalText: String,
): CoordinateAddressSearchResult? {
    val address = resolvedAddress.takeIf { it.isNotBlank() } ?: return null
    return CoordinateAddressSearchResult(
        address = address,
        coordinate = coordinate,
        originalText = originalText,
    )
}

internal fun canConfirmCoordinateInput(isCoordinateInvalid: Boolean): Boolean =
    !isCoordinateInvalid

internal fun filterShapeEditNumberInput(value: String): String {
    return value.filter { it.isDigit() }
}

internal fun shouldShowShapeEditDroneColorIndicator(color: PaletteColor?): Boolean = color != null

internal fun shouldShowShapeEditDroneSelectionCheckmark(
    droneId: String,
    selectedDroneId: String?,
): Boolean = droneId == selectedDroneId

internal fun resolveShapeEditSelectedColor(
    selectedDrone: DroneModel?,
    defaultColor: String = PaletteColor.BLUE.hex,
): String {
    return selectedDrone?.color ?: defaultColor
}

internal fun buildShapeEditSavedShape(
    originalShape: ShapeModel?,
    isDuplicateMode: Boolean,
    generatedId: String,
    title: String,
    defaultTitle: String,
    coordinate: Coordinate,
    address: String,
    noAddressFallback: String,
    radius: String,
    height: String,
    memo: String,
    selectedColor: String,
    selectedDroneId: String?,
    flightStartDate: Long,
    flightEndDate: Long,
    now: Long,
): ShapeModel {
    val baseShape = originalShape ?: ShapeModel()
    return baseShape.copy(
        id = if (isDuplicateMode) generatedId else (originalShape?.id ?: generatedId),
        title = title.ifBlank { defaultTitle },
        shapeType = ShapeType.CIRCLE,
        baseCoordinate = coordinate,
        radius = radius.toDoubleOrNull() ?: 0.0,
        secondCoordinate = null,
        polygonCoordinates = null,
        polylineCoordinates = null,
        height = height.toDoubleOrNull(),
        memo = memo.ifBlank { null },
        address = address.ifBlank { noAddressFallback },
        createdAt = if (isDuplicateMode) now else (originalShape?.createdAt ?: now),
        flightStartDate = flightStartDate,
        flightEndDate = flightEndDate,
        color = selectedColor,
        droneId = selectedDroneId,
        updatedAt = now,
    )
}

internal fun startOfShapeEditLocalDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

internal fun endOfShapeEditLocalDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

internal fun shapeEditLocalMillisFromDatePicker(
    dateMillis: Long,
    hour: Int,
    minute: Int,
    second: Int = 0,
    millisecond: Int = 0,
): Long {
    val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = dateMillis
    }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, utcCalendar.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCalendar.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCalendar.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, second)
        set(Calendar.MILLISECOND, millisecond)
    }.timeInMillis
}

internal fun shapeEditDatePickerMillisFromLocalMillis(localMillis: Long): Long {
    val localCalendar = Calendar.getInstance().apply {
        timeInMillis = localMillis
    }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(Calendar.YEAR, localCalendar.get(Calendar.YEAR))
        set(Calendar.MONTH, localCalendar.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, localCalendar.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

internal fun selectedStartShapeEditDate(
    selectedDate: Long,
    isDateOnly: Boolean,
): Long = if (isDateOnly) startOfShapeEditLocalDay(selectedDate) else selectedDate

internal fun selectedEndShapeEditDate(
    selectedDate: Long,
    isDateOnly: Boolean,
): Long = if (isDateOnly) endOfShapeEditLocalDay(selectedDate) else selectedDate

internal fun coerceShapeEditEndDateAtOrAfterStart(
    startDate: Long,
    proposedEndDate: Long,
    isDateOnly: Boolean,
): Long {
    if (proposedEndDate >= startDate) return proposedEndDate
    return if (isDateOnly) endOfShapeEditLocalDay(startDate) else startDate
}

internal fun resolveCoordinateAddressForSave(
    resolvedAddress: String,
    fallbackAddress: String,
): String {
    return resolvedAddress.ifBlank { fallbackAddress }
}

internal enum class ShapeEditSaveValidationError {
    COORDINATE_REQUIRED,
    RADIUS_REQUIRED,
    NO_COORDINATE,
}

internal fun validateShapeEditSaveFields(
    coordinate: Coordinate?,
    address: String,
    radius: String,
): ShapeEditSaveValidationError? {
    if (coordinate == null && address.isEmpty()) {
        return ShapeEditSaveValidationError.COORDINATE_REQUIRED
    }
    if (radius.isEmpty()) {
        return ShapeEditSaveValidationError.RADIUS_REQUIRED
    }
    if (coordinate == null) {
        return ShapeEditSaveValidationError.NO_COORDINATE
    }
    return null
}

internal fun resolveShapeEditConflict(
    editedShape: ShapeModel,
    originalShape: ShapeModel?,
    latestShape: ShapeModel?,
): ShapeModel {
    if (originalShape == null || latestShape == null) return editedShape
    if (editedShape.id != originalShape.id || latestShape.id != originalShape.id) return editedShape

    val geometrySource = if (hasShapeEditGeometryChanges(editedShape, originalShape)) {
        editedShape
    } else {
        latestShape
    }
    val resolvedDroneId = resolveShapeEditField(
        editedValue = editedShape.droneId,
        latestValue = latestShape.droneId,
        originalValue = originalShape.droneId,
    )

    return editedShape.copy(
        title = resolveShapeEditField(
            editedValue = editedShape.title,
            latestValue = latestShape.title,
            originalValue = originalShape.title,
        ),
        memo = resolveShapeEditField(
            editedValue = editedShape.memo,
            latestValue = latestShape.memo,
            originalValue = originalShape.memo,
        ),
        address = resolveShapeEditField(
            editedValue = editedShape.address,
            latestValue = latestShape.address,
            originalValue = originalShape.address,
        ),
        flightStartDate = resolveShapeEditField(
            editedValue = editedShape.flightStartDate,
            latestValue = latestShape.flightStartDate,
            originalValue = originalShape.flightStartDate,
        ),
        flightEndDate = resolveShapeEditField(
            editedValue = editedShape.flightEndDate,
            latestValue = latestShape.flightEndDate,
            originalValue = originalShape.flightEndDate,
        ),
        height = resolveShapeEditField(
            editedValue = editedShape.height,
            latestValue = latestShape.height,
            originalValue = originalShape.height,
        ),
        deletedAt = resolveShapeEditField(
            editedValue = editedShape.deletedAt,
            latestValue = latestShape.deletedAt,
            originalValue = originalShape.deletedAt,
        ),
        shapeType = geometrySource.shapeType,
        baseCoordinate = geometrySource.baseCoordinate,
        radius = geometrySource.radius,
        secondCoordinate = geometrySource.secondCoordinate,
        polygonCoordinates = geometrySource.polygonCoordinates,
        polylineCoordinates = geometrySource.polylineCoordinates,
        droneId = resolvedDroneId,
        color = resolveShapeEditColor(
            editedShape = editedShape,
            latestShape = latestShape,
            originalShape = originalShape,
        ),
    )
}

private fun <T> resolveShapeEditField(
    editedValue: T,
    latestValue: T,
    originalValue: T,
): T {
    if (editedValue != originalValue) return editedValue
    if (latestValue != originalValue) return latestValue
    return editedValue
}

private fun hasShapeEditGeometryChanges(
    editedShape: ShapeModel,
    originalShape: ShapeModel,
): Boolean {
    return editedShape.baseCoordinate != originalShape.baseCoordinate ||
        editedShape.radius != originalShape.radius ||
        editedShape.secondCoordinate != originalShape.secondCoordinate ||
        editedShape.polygonCoordinates != originalShape.polygonCoordinates ||
        editedShape.polylineCoordinates != originalShape.polylineCoordinates
}

private fun resolveShapeEditColor(
    editedShape: ShapeModel,
    latestShape: ShapeModel,
    originalShape: ShapeModel,
): String {
    return when {
        editedShape.droneId != originalShape.droneId -> editedShape.color
        latestShape.droneId != originalShape.droneId -> latestShape.color
        latestShape.color != originalShape.color -> latestShape.color
        else -> editedShape.color
    }
}

internal fun hasShapeEditContentChanges(
    title: String,
    initialTitle: String,
    address: String,
    initialAddress: String,
    radius: String,
    initialRadius: String,
    height: String,
    initialHeight: String,
    memo: String,
    initialMemo: String,
    coordinate: Coordinate?,
    initialCoordinate: Coordinate?,
    selectedDroneId: String?,
    initialDroneId: String?,
): Boolean {
    return title != initialTitle ||
        address != initialAddress ||
        radius != initialRadius ||
        height != initialHeight ||
        memo != initialMemo ||
        coordinate != initialCoordinate ||
        selectedDroneId != initialDroneId
}

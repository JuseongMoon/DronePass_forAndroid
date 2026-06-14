package com.ScienceFiction.DronePassAndroid.feature.shape

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.remote.NaverGeocodingApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.GeocodingAddress
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import kotlinx.coroutines.launch

/**
 * 주소 검색 결과를 담는 데이터 클래스
 */
data class AddressSearchResult(
    val address: String,
    val coordinate: Coordinate
)

internal const val SearchAddressSheetSkipPartiallyExpanded = false
internal const val SearchAddressSheetInteractiveDismissEnabled = false
internal const val SearchAddressSheetHeightFraction = 0.85f
internal val SearchAddressSheetHorizontalPadding = 16.dp
internal val SearchAddressBarCornerRadius = 8.dp
internal val SearchAddressBarInnerPadding = 8.dp
internal val SearchAddressSearchButtonHorizontalPadding = 16.dp
internal val SearchAddressSearchButtonVerticalPadding = 8.dp
internal val SearchAddressBarBackgroundColor = Color(0xFFF2F2F7)
internal val SearchAddressBarIconColor = Color(0xFF8E8E93)
internal val SearchAddressResultCardShadowElevation = 8.dp
internal val SearchAddressResultCardShadowColor = Color.Black.copy(alpha = 0.06f)
internal val SearchAddressGuideCardCornerRadius = 20.dp
internal val SearchAddressGuideCardMaxWidth = 500.dp
internal val SearchAddressGuideCardPadding = 16.dp
internal val SearchAddressGuideCardVerticalSpacing = 8.dp

internal fun resolveSelectedAddressForShapeEdit(address: GeocodingAddress): String =
    address.jibunAddress
        ?: address.roadAddress?.takeIf { it.isNotEmpty() }
        ?: ""

internal enum class AddressDisplayType {
    JIBUN,
    ROAD,
}

internal data class AddressDisplayRow(
    val type: AddressDisplayType,
    val text: String,
)

internal fun addressDisplayRows(address: GeocodingAddress): List<AddressDisplayRow> {
    return buildList {
        address.jibunAddress?.let { text ->
            add(AddressDisplayRow(AddressDisplayType.JIBUN, text))
        }
        address.roadAddress?.let { text ->
            add(AddressDisplayRow(AddressDisplayType.ROAD, text))
        }
    }
}

internal fun addressBuildingName(address: GeocodingAddress): String? {
    return address.addressElements
        ?.firstOrNull { element -> element.types?.contains("BUILDING_NAME") == true }
        ?.longName
        ?.takeIf { it.isNotEmpty() }
}

internal fun shouldShowSearchAddressGuide(
    isLoading: Boolean,
    errorMessage: String?,
    hasResults: Boolean,
): Boolean = !isLoading && errorMessage == null && !hasResults

internal fun shouldEnableSearchAddressSubmit(query: String): Boolean = query.isNotEmpty()

internal fun formatSearchAddressErrorMessage(
    causeMessage: String?,
    prefix: String,
    fallback: String,
): String {
    val detail = causeMessage ?: return fallback
    return "$prefix: $detail"
}

/**
 * 주소 검색 BottomSheet
 *
 * @param geocodingApi 네이버 Geocoding API
 * @param onAddressSelected 주소 선택 시 콜백 (주소 문자열 + 좌표)
 * @param onDismiss 시트 닫기 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAddressSheet(
    geocodingApi: NaverGeocodingApi,
    onAddressSelected: (AddressSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = SearchAddressSheetSkipPartiallyExpanded
    )

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GeocodingAddress>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val searchErrorPrefix = stringResource(R.string.search_address_error_prefix)
    val searchErrorFallback = stringResource(R.string.search_address_error)

    fun submitSearch() {
        if (!shouldEnableSearchAddressSubmit(query)) return

        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = geocodingApi.geocode(query)
                results = if (response.status == "OK") {
                    // x/y 가 없는 결과는 표시해도 클릭 시 좌표 변환이 실패하므로 사전 필터링.
                    response.addresses
                        ?.filter { !it.x.isNullOrBlank() && !it.y.isNullOrBlank() }
                        .orEmpty()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                errorMessage = formatSearchAddressErrorMessage(
                    causeMessage = e.message,
                    prefix = searchErrorPrefix,
                    fallback = searchErrorFallback,
                )
                results = emptyList()
            }
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (SearchAddressSheetInteractiveDismissEnabled) onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(SearchAddressSheetHeightFraction)
                .navigationBarsPadding()
                .padding(horizontal = SearchAddressSheetHorizontalPadding)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
                Text(
                    text = stringResource(R.string.search_address_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchAddressInputField(
                    query = query,
                    onQueryChange = { query = it },
                    onClear = {
                        query = ""
                        errorMessage = null
                    },
                    onSearch = { submitSearch() },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { submitSearch() },
                    enabled = shouldEnableSearchAddressSubmit(query),
                    shape = RoundedCornerShape(SearchAddressBarCornerRadius),
                    contentPadding = PaddingValues(
                        horizontal = SearchAddressSearchButtonHorizontalPadding,
                        vertical = SearchAddressSearchButtonVerticalPadding,
                    ),
                ) {
                    Text(stringResource(R.string.common_search))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 에러 메시지 표시
            errorMessage?.let { message ->
                SearchAddressErrorView(
                    message = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                shouldShowSearchAddressGuide(
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    hasResults = results.isNotEmpty(),
                ) -> {
                    SearchAddressGuideCard(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        items(results) { addressItem ->
                            AddressResultItem(
                                address = addressItem,
                                onClick = {
                                    // x/y 가 nullable 이지만 위 collectLatest 에서 사전 필터링했으므로
                                    // 여기서는 빈 문자열에 대한 toDoubleOrNull null 분기만으로 충분.
                                    val lat = addressItem.y?.toDoubleOrNull() ?: return@AddressResultItem
                                    val lon = addressItem.x?.toDoubleOrNull() ?: return@AddressResultItem
                                    onAddressSelected(
                                        AddressSearchResult(
                                            address = resolveSelectedAddressForShapeEdit(addressItem),
                                            coordinate = Coordinate(lat, lon)
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SearchAddressInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
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
                    contentDescription = stringResource(R.string.shape_edit_search_address),
                    tint = SearchAddressBarIconColor,
                    modifier = Modifier.size(18.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_address_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SearchAddressBarIconColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
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

@Composable
private fun SearchAddressErrorView(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .size(40.dp)
                .padding(bottom = 8.dp),
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun SearchAddressGuideCard(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = SearchAddressGuideCardMaxWidth)
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(SearchAddressGuideCardCornerRadius),
            )
            .padding(SearchAddressGuideCardPadding),
        verticalArrangement = Arrangement.spacedBy(SearchAddressGuideCardVerticalSpacing),
    ) {
        Text(
            text = stringResource(R.string.search_address_guide),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.search_address_example),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        listOf(
            R.string.search_address_example_road_1,
            R.string.search_address_example_jibun_1,
            R.string.search_address_example_road_2,
            R.string.search_address_example_jibun_2,
        ).forEach { resId ->
            Text(
                text = stringResource(resId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 주소 검색 결과 아이템
 */
@Composable
private fun AddressResultItem(
    address: GeocodingAddress,
    onClick: () -> Unit
) {
    val rows = addressDisplayRows(address)
    val cardShape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .shadow(
                elevation = SearchAddressResultCardShadowElevation,
                shape = cardShape,
                ambientColor = SearchAddressResultCardShadowColor,
                spotColor = SearchAddressResultCardShadowColor,
                clip = false,
            )
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, cardShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { row ->
            AddressDisplayRowView(row = row)
        }
        addressBuildingName(address)?.let { buildingName ->
            AddressBuildingNameRow(buildingName = buildingName)
        }
    }
}

@Composable
private fun AddressDisplayRowView(
    row: AddressDisplayRow,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AddressTypeBadge(type = row.type)
        Text(
            text = row.text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AddressBuildingNameRow(
    buildingName: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = buildingName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddressTypeBadge(
    type: AddressDisplayType,
) {
    val (label, color) = when (type) {
        AddressDisplayType.JIBUN -> stringResource(R.string.search_address_jibun) to Color(0xFF007AFF)
        AddressDisplayType.ROAD -> stringResource(R.string.search_address_road) to Color(0xFF34C759)
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = Modifier
            .background(
                color = color,
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

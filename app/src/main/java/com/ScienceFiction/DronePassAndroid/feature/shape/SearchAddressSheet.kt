package com.ScienceFiction.DronePassAndroid.feature.shape

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.core.data.remote.NaverGeocodingApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.GeocodingAddress
import com.ScienceFiction.DronePassAndroid.domain.model.Coordinate
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter

/**
 * 주소 검색 결과를 담는 데이터 클래스
 */
data class AddressSearchResult(
    val address: String,
    val coordinate: Coordinate
)

/**
 * 주소 검색 BottomSheet
 *
 * @param geocodingApi 네이버 Geocoding API
 * @param onAddressSelected 주소 선택 시 콜백 (주소 문자열 + 좌표)
 * @param onDismiss 시트 닫기 콜백
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun SearchAddressSheet(
    geocodingApi: NaverGeocodingApi,
    onAddressSelected: (AddressSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GeocodingAddress>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Debounced 검색: 500ms 지연 후 API 호출
    LaunchedEffect(Unit) {
        snapshotFlow { query }
            .debounce(500L)
            .filter { it.length >= 2 }
            .collectLatest { searchQuery ->
                isLoading = true
                errorMessage = null
                try {
                    val response = geocodingApi.geocode(searchQuery)
                    if (response.status == "OK") {
                        // x/y 가 없는 결과는 표시해도 클릭 시 좌표 변환이 실패하므로 사전 필터링.
                        results = response.addresses
                            ?.filter { !it.x.isNullOrBlank() && !it.y.isNullOrBlank() }
                            .orEmpty()
                    } else {
                        results = emptyList()
                    }
                } catch (e: Exception) {
                    errorMessage = e.message
                    results = emptyList()
                }
                isLoading = false
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // 제목
            Text(
                text = stringResource(R.string.search_address_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 검색 입력 필드
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.search_address_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.shape_edit_search_address)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 에러 메시지 표시
            if (errorMessage != null) {
                Text(
                    text = stringResource(R.string.search_address_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 검색 결과 목록
            if (results.isEmpty() && query.length >= 2 && !isLoading) {
                Text(
                    text = stringResource(R.string.search_address_no_result),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 24.dp)
                )
            } else {
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
                                val displayAddress = addressItem.roadAddress
                                    ?: addressItem.jibunAddress
                                    ?: ""
                                onAddressSelected(
                                    AddressSearchResult(
                                        address = displayAddress,
                                        coordinate = Coordinate(lat, lon)
                                    )
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        // 도로명 주소
        if (!address.roadAddress.isNullOrBlank()) {
            Text(
                text = "${stringResource(R.string.search_address_road)}: ${address.roadAddress}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        // 지번 주소
        if (!address.jibunAddress.isNullOrBlank()) {
            Text(
                text = "${stringResource(R.string.search_address_jibun)}: ${address.jibunAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider()
}

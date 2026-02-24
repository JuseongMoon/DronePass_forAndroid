package com.ScienceFiction.DronePassAndroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.LocationServices
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.util.FusedLocationSource

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var naverMap by remember { mutableStateOf<NaverMap?>(null) }
    var mapReady by remember { mutableStateOf(false) }

    // 위치 권한 상태
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // 권한 요청 다이얼로그 표시 상태
    var showRationaleDialog by remember { mutableStateOf(false) }

    // 권한이 허용되었는지 확인
    val allPermissionsGranted = locationPermissionsState.permissions.all { it.status.isGranted }

    // 권한 요청
    LaunchedEffect(Unit) {
        if (!allPermissionsGranted) {
            if (locationPermissionsState.permissions.any { it.status.shouldShowRationale }) {
                showRationaleDialog = true
            } else {
                locationPermissionsState.launchMultiplePermissionRequest()
            }
        }
    }

    // 권한 설명 다이얼로그
    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text("위치 권한 필요") },
            text = { Text("지도에서 현재 위치를 표시하기 위해 위치 권한이 필요합니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationaleDialog = false
                        locationPermissionsState.launchMultiplePermissionRequest()
                    }
                ) {
                    Text("허용")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 네이버 지도 표시
            AndroidView(
                factory = {
                    mapView.apply {
                        onCreate(null)
                        getMapAsync { map ->
                            naverMap = map
                            mapReady = true

                            Log.d("NaverMapDebug", "네이버 지도 준비 완료")

                            // 기본 카메라 위치 설정 (서울 시청)
                            val defaultPosition = CameraPosition(
                                LatLng(37.5665, 126.9780),
                                13.0
                            )
                            map.cameraPosition = defaultPosition

                            // 위치 권한이 허용된 경우 위치 추적 활성화
                            if (allPermissionsGranted) {
                                setupLocationTracking(map, context)
                            }

                            // 지도 UI 설정
                            map.uiSettings.apply {
                                isLocationButtonEnabled = true
                                isZoomControlEnabled = true
                                isCompassEnabled = true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { _ ->
                    // 권한 상태가 변경되었을 때 위치 추적 재설정
                    if (allPermissionsGranted && naverMap != null) {
                        setupLocationTracking(naverMap!!, context)
                    }
                }
            )

            // 지도 로딩 중 인디케이터
            if (!mapReady) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 권한이 거부된 경우 안내 메시지
            if (!allPermissionsGranted) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("위치 권한이 필요합니다")
                    Button(
                        onClick = { locationPermissionsState.launchMultiplePermissionRequest() }
                    ) {
                        Text("권한 요청")
                    }
                }
            }
        }
    }

    // 생명주기 관리 - LifecycleOwner 연동
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@SuppressLint("MissingPermission")
private fun setupLocationTracking(
    map: NaverMap,
    context: android.content.Context
) {
    try {
        val activity = context as? Activity ?: run {
            Log.e("MapScreen", "Context is not an Activity, cannot setup location tracking")
            return
        }

        val locationSource = FusedLocationSource(activity, LOCATION_PERMISSION_REQUEST_CODE)
        map.locationSource = locationSource
        map.locationTrackingMode = LocationTrackingMode.Follow

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val cameraPosition = CameraPosition(
                    LatLng(it.latitude, it.longitude),
                    15.0
                )
                map.cameraPosition = cameraPosition
            }
        }
    } catch (e: Exception) {
        Log.e("MapScreen", "위치 추적 설정 실패: ${e.message}", e)
    }
}

private const val LOCATION_PERMISSION_REQUEST_CODE = 1000

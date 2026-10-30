package com.ScienceFiction.DronePassAndroid.feature.shape

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.TagModel
import com.ScienceFiction.DronePassAndroid.ui.component.DronePassModalBottomSheet
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedDivider
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedSection
import com.ScienceFiction.DronePassAndroid.ui.component.InsetGroupedToggleRow
import com.ScienceFiction.DronePassAndroid.ui.component.IosNavBarButtonFontSize
import com.ScienceFiction.DronePassAndroid.ui.theme.IosSystemGroupedBackground
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 공유 카드 항목 선택을 기억하는 기기 설정(계정 전환 때 지우지 않는다) */
private const val ShapeSharePreferencesName = "shape_share"
private val ShapeSharePreviewHeaderHeight = 44.dp
private val ShapeSharePreviewHeaderSideWidth = 72.dp
private val ShapeSharePreviewCardShape = RoundedCornerShape(20.dp)
private val ShapeShareSegmentTrack = Color(0x1F767680)
private const val ShapeShareCacheDirectory = "shared_cards"

/**
 * 공유 카드 미리보기 (iOS ShapeSharePreviewView 정합).
 * 지도 구도와 유형을 고른 뒤 카드 이미지(1080×1350 PNG, 모서리 투명)를 만들어 시스템 공유 시트로 내보낸다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShapeSharePreviewSheet(
    shape: ShapeModel,
    drone: DroneModel?,
    tag: TagModel?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val preferences = remember { context.getSharedPreferences(ShapeSharePreferencesName, Context.MODE_PRIVATE) }
    var options by remember {
        mutableStateOf(ShapeShareCardOptions.fromJson(preferences.getString(ShapeShareCardOptions.STORAGE_KEY, null)))
    }
    var showOptions by remember { mutableStateOf(false) }
    val content = remember(shape, drone, tag, options) { buildShapeShareCardContent(shape, drone, tag, options) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val mapController = remember { ShapeShareMapController() }
    val cardLayer = rememberGraphicsLayer()
    var mapStyle by remember { mutableStateOf(ShapeShareMapStyle.BASIC) }
    var isRendering by remember { mutableStateOf(false) }
    // 공유 시점의 지도 스냅샷. 값이 있는 동안 숨긴 3배 카드를 그려 이미지로 기록한다
    var renderSnapshot by remember { mutableStateOf<Bitmap?>(null) }
    var showError by remember { mutableStateOf(false) }
    // 카드 글자는 시스템 글꼴 배율과 상관없이 고정 크기다 (iOS 카드도 고정 pt)
    val cardDensity = Density(density.density, fontScale = 1f)

    fun share() {
        if (isRendering || !mapController.isReady) return
        isRendering = true
        coroutineScope.launch {
            val snapshot = mapController.snapshot()
            if (snapshot == null) {
                isRendering = false
                showError = true
            } else {
                renderSnapshot = snapshot
            }
        }
    }

    LaunchedEffect(renderSnapshot) {
        if (renderSnapshot == null) return@LaunchedEffect
        // 숨긴 카드가 한 번 그려져 레이어에 기록될 때까지 기다린다
        withFrameNanos {}
        withFrameNanos {}
        val uri = runCatching {
            val image = cardLayer.toImageBitmap().asAndroidBitmap()
            withContext(Dispatchers.IO) { writeShapeShareCardPng(context, image) }
        }.getOrNull()
        renderSnapshot = null
        isRendering = false
        if (uri == null) {
            showError = true
        } else {
            startShapeShareChooser(context, uri, content.title)
        }
    }

    DronePassModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = IosSystemGroupedBackground,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ShapeSharePreviewHeader(
                    onOptions = { showOptions = true },
                    onClose = {
                        coroutineScope.launch {
                            runCatching { sheetState.hide() }
                            onDismiss()
                        }
                    },
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CompositionLocalProvider(LocalDensity provides cardDensity) {
                        ShapeShareCard(
                            content = content,
                            modifier = Modifier.shadow(
                                elevation = 10.dp,
                                shape = ShapeSharePreviewCardShape,
                                ambientColor = Color.Black.copy(alpha = 0.12f),
                                spotColor = Color.Black.copy(alpha = 0.12f),
                            ),
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                ShapeShareMap(
                                    shape = shape,
                                    colorHex = content.shapeColorHex,
                                    style = mapStyle,
                                    controller = mapController,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (!mapController.isReady) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                                }
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.shape_share_hint),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    ShapeShareMapStylePicker(
                        selected = mapStyle,
                        onSelect = { mapStyle = it },
                        modifier = Modifier.width(ShapeShareCardWidth),
                    )

                    ShapeShareActionButton(
                        isReady = mapController.isReady,
                        isRendering = isRendering,
                        onClick = ::share,
                        modifier = Modifier.width(ShapeShareCardWidth),
                    )
                }
            }

            // 공유 이미지 렌더링: 지도 스냅샷을 넣은 카드를 3배 밀도(360dp → 1080px)로 화면 밖 레이어에 기록한다
            renderSnapshot?.let { snapshot ->
                ShapeShareCardRenderer(content = content, mapSnapshot = snapshot, layer = cardLayer)
            }
        }
    }

    if (showOptions) {
        ShapeShareOptionsSheet(
            options = options,
            hasDrone = drone != null,
            hasTag = tag != null,
            onChange = { updated ->
                options = updated
                preferences.edit().putString(ShapeShareCardOptions.STORAGE_KEY, updated.toJson()).apply()
            },
            onDismiss = { showOptions = false },
        )
    }

    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            text = { Text(stringResource(R.string.shape_share_error)) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
        )
    }
}

@Composable
private fun ShapeSharePreviewHeader(onOptions: () -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ShapeSharePreviewHeaderHeight)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(ShapeSharePreviewHeaderSideWidth), contentAlignment = Alignment.CenterStart) {
            TextButton(onClick = onClose) {
                Text(
                    text = stringResource(R.string.common_close),
                    fontSize = IosNavBarButtonFontSize,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
        Text(
            text = stringResource(R.string.shape_share_preview_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Box(modifier = Modifier.width(ShapeSharePreviewHeaderSideWidth), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onOptions) {
                Text(
                    text = stringResource(R.string.shape_share_options_button),
                    fontSize = IosNavBarButtonFontSize,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }
}

/** 카드에 넣을 항목 켜기/끄기 (항상 들어가는 항목은 안내만 한다). iOS ShapeShareOptionsView 정합 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShapeShareOptionsSheet(
    options: ShapeShareCardOptions,
    hasDrone: Boolean,
    hasTag: Boolean,
    onChange: (ShapeShareCardOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    DronePassModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = IosSystemGroupedBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ShapeSharePreviewHeaderHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(ShapeSharePreviewHeaderSideWidth))
            Text(
                text = stringResource(R.string.shape_share_options_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.width(ShapeSharePreviewHeaderSideWidth), contentAlignment = Alignment.CenterEnd) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            runCatching { sheetState.hide() }
                            onDismiss()
                        }
                    },
                ) {
                    Text(
                        text = stringResource(R.string.common_done),
                        fontSize = IosNavBarButtonFontSize,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            InsetGroupedSection(
                header = stringResource(R.string.shape_share_options_section_location),
                footer = stringResource(R.string.shape_share_options_section_location_footer),
            ) {
                InsetGroupedToggleRow(
                    title = stringResource(R.string.shape_share_options_address),
                    checked = options.showAddress,
                    onCheckedChange = { onChange(options.copy(showAddress = it)) },
                )
                InsetGroupedDivider()
                InsetGroupedToggleRow(
                    title = stringResource(R.string.shape_share_options_simplify_address),
                    checked = options.simplifyAddress,
                    onCheckedChange = { onChange(options.copy(simplifyAddress = it)) },
                    enabled = options.showAddress,
                )
                InsetGroupedDivider()
                InsetGroupedToggleRow(
                    title = stringResource(R.string.shape_share_options_coordinate),
                    checked = options.showCoordinate,
                    onCheckedChange = { onChange(options.copy(showCoordinate = it)) },
                )
            }
            InsetGroupedSection(
                header = stringResource(R.string.shape_share_options_section_details),
                footer = stringResource(R.string.shape_share_options_footer),
            ) {
                InsetGroupedToggleRow(
                    title = stringResource(R.string.shape_share_options_period),
                    checked = options.showPeriod,
                    onCheckedChange = { onChange(options.copy(showPeriod = it)) },
                )
                InsetGroupedDivider()
                InsetGroupedToggleRow(
                    title = stringResource(R.string.shape_share_options_drone),
                    checked = options.showDrone,
                    onCheckedChange = { onChange(options.copy(showDrone = it)) },
                    enabled = hasDrone,
                )
                InsetGroupedDivider()
                InsetGroupedToggleRow(
                    title = stringResource(R.string.shape_share_options_tag),
                    checked = options.showTag,
                    onCheckedChange = { onChange(options.copy(showTag = it)) },
                    enabled = hasTag,
                )
            }
        }
    }
}

/** iOS segmented Picker 모양: 회색 트랙 위에 고른 항목만 흰 칸 */
@Composable
private fun ShapeShareMapStylePicker(
    selected: ShapeShareMapStyle,
    onSelect: (ShapeShareMapStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(ShapeShareSegmentTrack)
            .padding(2.dp),
    ) {
        ShapeShareMapStyle.entries.forEach { style ->
            val isSelected = style == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (isSelected) {
                            Modifier
                                .shadow(1.dp, RoundedCornerShape(7.dp))
                                .background(Color.White, RoundedCornerShape(7.dp))
                        } else {
                            Modifier
                        },
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(style) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(style.titleRes),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = Color.Black,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ShapeShareActionButton(
    isReady: Boolean,
    isRendering: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = isReady && !isRendering,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        if (isRendering || !isReady) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Icon(imageVector = Icons.Filled.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(if (isReady) R.string.shape_share_action else R.string.shape_share_loading),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * 보이지 않는 크기 0 자리에 3배 밀도 카드를 배치하고, 그리는 대신 [layer] 에 기록만 한다.
 * 카드 바깥 모서리는 칠하지 않으므로 기록된 이미지에서 투명으로 남는다.
 */
@Composable
private fun ShapeShareCardRenderer(
    content: ShapeShareCardContent,
    mapSnapshot: Bitmap,
    layer: GraphicsLayer,
) {
    val mapImage = remember(mapSnapshot) { mapSnapshot.asImageBitmap() }
    Box(
        modifier = Modifier
            .size(0.dp)
            .wrapContentSize(align = Alignment.TopStart, unbounded = true),
    ) {
        CompositionLocalProvider(LocalDensity provides Density(ShapeShareCardRenderScale, fontScale = 1f)) {
            ShapeShareCard(
                content = content,
                modifier = Modifier.drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                },
            ) {
                Image(
                    bitmap = mapImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** 모서리 투명을 지키려고 PNG 로 저장한다. 앱 캐시의 공유 전용 폴더에 한 장만 남긴다 */
private fun writeShapeShareCardPng(context: Context, image: Bitmap): Uri {
    val softwareImage = if (image.config == Bitmap.Config.HARDWARE) {
        image.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        image
    }
    val directory = File(context.cacheDir, ShapeShareCacheDirectory).apply {
        mkdirs()
        listFiles()?.forEach { it.delete() }
    }
    val file = File(directory, "DronePass-${System.currentTimeMillis()}.png")
    file.outputStream().use { output ->
        check(softwareImage.compress(Bitmap.CompressFormat.PNG, 100, output)) { "PNG 인코딩 실패" }
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun startShapeShareChooser(context: Context, uri: Uri, title: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        // 공유 시트 상단 미리보기 제목과 썸네일
        putExtra(Intent.EXTRA_TITLE, title)
        clipData = ClipData.newRawUri(title, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, title))
}

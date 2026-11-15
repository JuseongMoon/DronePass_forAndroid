package com.ScienceFiction.DronePassAndroid.feature.shape

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale
import java.util.TimeZone

/**
 * 지도 없는 공유 카드(D-71)를 내보내기와 같은 3배 밀도로 그려 1080×1350 인지 본다.
 * 계측 인수 `saveShareCardRenders=true` 를 주면 앱 외부 파일 폴더의 share_card_renders/ 에 PNG 로 남긴다(모양 비교용).
 */
@RunWith(AndroidJUnit4::class)
class ShapeShareSchematicRenderTest {
    @get:Rule val compose = createComposeRule()

    private data class Case(
        val name: String,
        val locale: Locale,
        val shapeType: ShapeType = ShapeType.CIRCLE,
        val degrees: Double = 0.0,
        val zoom: Double = 1.0,
        val dark: Boolean = false,
    )

    private val cases = listOf(Locale.KOREA, Locale.US).flatMap { locale ->
        val tag = if (locale == Locale.KOREA) "ko" else "en"
        listOf(
            Case("${tag}_light_north", locale),
            Case("${tag}_light_rot30", locale, degrees = 30.0),
            Case("${tag}_light_rot135", locale, degrees = 135.0),
            Case("${tag}_dark_rot30", locale, degrees = 30.0, dark = true),
            Case("${tag}_light_zoom2", locale, zoom = 2.0),
            Case("${tag}_light_zoom_half", locale, zoom = 0.5),
            Case("${tag}_light_point", locale, shapeType = ShapeType.POLYGON),
        )
    }

    @Test fun schematicCardsRenderAtFullSize() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val save = InstrumentationRegistry.getArguments().getString("saveShareCardRenders") == "true"
        val target = instrumentation.targetContext
        val outDir = File(target.getExternalFilesDir(null), "share_card_renders").apply { mkdirs() }
        var current by mutableStateOf(cases.first())
        compose.setContent {
            val case = current
            Locale.setDefault(case.locale)
            val configuration = Configuration(target.resources.configuration).apply { setLocale(case.locale) }
            val localized = target.createConfigurationContext(configuration)
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(ShapeShareCardRenderScale, fontScale = 1f),
            ) {
                val shape = sampleShape(case)
                val content = buildShapeShareCardContent(
                    shape = shape,
                    drone = DroneModel(id = "D1", name = "Mavic 3", color = "#34C759"),
                    timeZone = TimeZone.getTimeZone("Asia/Seoul"),
                    locale = case.locale,
                )
                ShapeShareCard(content = content, modifier = Modifier.testTag("card")) {
                    ShapeShareSchematic(
                        shape = shape,
                        colorHex = content.shapeColorHex,
                        rotationDegrees = case.degrees,
                        zoom = case.zoom,
                        palette = if (case.dark) {
                            ShapeShareSchematicPalette(ShapeShareSchematicPalette.darkBackground)
                        } else ShapeShareSchematicPalette.light,
                    )
                }
            }
        }
        for (case in cases) {
            current = case
            compose.waitForIdle()
            val image = compose.onNodeWithTag("card").captureToImage().asAndroidBitmap()
            assertEquals(case.name, 1080, image.width)
            assertEquals(case.name, 1350, image.height)
            if (save) File(outDir, "${case.name}.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun sampleShape(case: Case): ShapeModel {
        val start = 1_796_257_800_000L // 2026-12-03 09:30 KST
        return ShapeModel(
            id = "S1",
            title = if (case.locale == Locale.KOREA) "양평 강변 촬영" else "Yangpyeong riverside shoot",
            shapeType = case.shapeType,
            address = "경기도 양평군 양평읍 양근리 1",
            radius = 500.0,
            height = 120.0,
            color = "#FF3B30",
            droneId = "D1",
            flightStartDate = start,
            flightEndDate = start + 3 * 60 * 60 * 1000L,
        )
    }
}

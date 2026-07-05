package com.ScienceFiction.DronePassAndroid.core.res

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LaunchScreenContractTest {

    @Test
    fun `Android launch screen 은 iOS LaunchScreen 배경색과 중앙 300dp 로고를 사용한다`() {
        val colors = parseResourceXml("values/colors.xml")
        val dimens = parseResourceXml("values/dimens.xml")
        val launchScreen = parseResourceXml("drawable/launch_screen.xml")

        assertEquals("#FFFFFE", colors.findNamedElement("color", "launch_screen_background").textContent)
        assertEquals("300dp", dimens.findNamedElement("dimen", "launch_logo_size").textContent)

        val items = launchScreen.documentElement.childElementsByTagName("item").toList()
        assertEquals(2, items.size)

        val backgroundShape = items[0].childElementsByTagName("shape").first()
        val solid = backgroundShape.childElementsByTagName("solid").first()
        assertEquals("@color/launch_screen_background", solid.getAttribute("android:color"))

        val logoItem = items[1]
        assertEquals("@dimen/launch_logo_size", logoItem.getAttribute("android:width"))
        assertEquals("@dimen/launch_logo_size", logoItem.getAttribute("android:height"))
        assertEquals("center", logoItem.getAttribute("android:gravity"))
        val bitmap = logoItem.childElementsByTagName("bitmap").first()
        assertEquals("@drawable/login_logo", bitmap.getAttribute("android:src"))
        assertEquals("fill", bitmap.getAttribute("android:gravity"))
    }

    @Test
    fun `Android 12 이상 splash 도 iOS LaunchLogo 와 같은 이미지를 사용한다`() {
        val themesV31 = parseResourceXml("values-v31/themes.xml")
        val launchStyle = themesV31.findStyle("Theme.DronePassAndroid.Launch")

        assertEquals("Theme.DronePassAndroid", launchStyle.getAttribute("parent"))
        assertEquals("@drawable/launch_screen", launchStyle.findItem("android:windowBackground"))
        assertEquals(
            "@color/launch_screen_background",
            launchStyle.findItem("android:windowSplashScreenBackground"),
        )
        assertEquals("@drawable/login_logo", launchStyle.findItem("android:windowSplashScreenAnimatedIcon"))
    }

    @Test
    fun `MainActivity 는 launch theme 다음 실제 앱 theme 로 즉시 전환한다`() {
        val source = resolveProjectFile(
            "src/main/java/com/ScienceFiction/DronePassAndroid/MainActivity.kt",
            "app/src/main/java/com/ScienceFiction/DronePassAndroid/MainActivity.kt",
        ).readText()

        assertAppearsInOrder(
            source = source,
            tokens = listOf(
                "override fun onCreate(savedInstanceState: Bundle?)",
                "setTheme(R.style.Theme_DronePassAndroid)",
                "super.onCreate(savedInstanceState)",
            ),
        )
    }

    @Test
    fun `Android launch logo asset 은 iOS LaunchLogo 와 같은 원본 파일이다`() {
        val androidLogo = resolveProjectFile(
            "src/main/res/drawable-nodpi/login_logo.png",
            "app/src/main/res/drawable-nodpi/login_logo.png",
        )
        val iosLaunchLogo = File(
            "/Users/david/Development/Swift/myProjects/DronePass/DronePass/Assets.xcassets/LaunchLogo.imageset/DronePass_AppIcon_v2.2_BG.png",
        )

        if (iosLaunchLogo.exists()) {
            assertTrue(androidLogo.readBytes().contentEquals(iosLaunchLogo.readBytes()))
        }
    }

    private fun Document.findNamedElement(tagName: String, name: String): Element {
        val elements = getElementsByTagName(tagName)
        for (index in 0 until elements.length) {
            val element = elements.item(index) as Element
            if (element.getAttribute("name") == name) {
                return element
            }
        }
        error("$tagName not found: $name")
    }

    private fun Document.findStyle(name: String): Element =
        findNamedElement(tagName = "style", name = name)

    private fun Element.findItem(name: String): String =
        childElementsByTagName("item")
            .firstOrNull { item -> item.getAttribute("name") == name }
            ?.textContent
            ?: error("style item not found: $name")

    private fun Element.childElementsByTagName(tagName: String): Sequence<Element> {
        return childNodes.asSequence()
            .filterIsInstance<Element>()
            .filter { element -> element.tagName == tagName }
    }

    private fun parseResourceXml(relativePath: String): Document =
        parseXml(resolveProjectFile("src/main/res/$relativePath", "app/src/main/res/$relativePath"))

    private fun parseXml(file: File): Document =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)

    private fun assertAppearsInOrder(source: String, tokens: List<String>) {
        var previousIndex = -1
        tokens.forEach { token ->
            val index = source.indexOf(token, startIndex = previousIndex + 1)
            assertTrue(
                "Expected token '$token' after index $previousIndex",
                index > previousIndex,
            )
            previousIndex = index
        }
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?: error("Project file not found: ${candidates.joinToString()}")
    }

    private fun org.w3c.dom.NodeList.asSequence(): Sequence<Node> {
        return (0 until length).asSequence().map(::item)
    }
}

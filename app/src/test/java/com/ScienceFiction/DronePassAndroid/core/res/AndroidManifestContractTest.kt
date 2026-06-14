package com.ScienceFiction.DronePassAndroid.core.res

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AndroidManifestContractTest {

    @Test
    fun `Manifest는 알림과 위치 기반 핵심 권한을 유지한다`() {
        val manifest = parseManifest()

        assertTrue(manifest.hasPermission("android.permission.INTERNET"))
        assertTrue(manifest.hasPermission("android.permission.ACCESS_FINE_LOCATION"))
        assertTrue(manifest.hasPermission("android.permission.ACCESS_COARSE_LOCATION"))
        assertTrue(manifest.hasPermission("android.permission.POST_NOTIFICATIONS"))
        assertTrue(manifest.hasPermission("android.permission.SCHEDULE_EXACT_ALARM"))
        assertTrue(manifest.hasPermission("android.permission.RECEIVE_BOOT_COMPLETED"))
    }

    @Test
    fun `MainActivity 는 알림과 외부 실행에서 기존 top 인스턴스를 재사용한다`() {
        val manifest = parseManifest()
        val activity = manifest.findActivity(".MainActivity")

        assertEquals("singleTop", activity.getAttribute("android:launchMode"))
        assertEquals("true", activity.getAttribute("android:exported"))
    }

    @Test
    fun `FCM 과 로컬 알림 리시버는 외부에서 직접 실행되지 않는다`() {
        val manifest = parseManifest()

        assertEquals("false", manifest.findService(".service.FcmService").getAttribute("android:exported"))
        assertEquals(
            "false",
            manifest.findReceiver(".service.NotificationReceiver").getAttribute("android:exported"),
        )
    }

    @Test
    fun `BootCompletedReceiver 는 재부팅 알림 복구를 받을 수 있다`() {
        val manifest = parseManifest()
        val receiver = manifest.findReceiver(".service.BootCompletedReceiver")

        assertEquals("true", receiver.getAttribute("android:exported"))
        assertTrue(receiver.hasIntentAction("android.intent.action.BOOT_COMPLETED"))
    }

    private fun parseManifest(): Document =
        parseXml(resolveProjectFile("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml"))

    private fun Document.hasPermission(name: String): Boolean {
        val permissions = getElementsByTagName("uses-permission")
        for (index in 0 until permissions.length) {
            val permission = permissions.item(index) as Element
            if (permission.getAttribute("android:name") == name) {
                return true
            }
        }
        return false
    }

    private fun Document.findActivity(name: String): Element {
        return findElementByAndroidName(tagName = "activity", name = name)
    }

    private fun Document.findService(name: String): Element {
        return findElementByAndroidName(tagName = "service", name = name)
    }

    private fun Document.findReceiver(name: String): Element {
        return findElementByAndroidName(tagName = "receiver", name = name)
    }

    private fun Document.findElementByAndroidName(tagName: String, name: String): Element {
        val elements = getElementsByTagName(tagName)
        for (index in 0 until elements.length) {
            val element = elements.item(index) as Element
            if (element.getAttribute("android:name") == name) {
                return element
            }
        }
        error("$tagName not found in manifest: $name")
    }

    private fun Element.hasIntentAction(name: String): Boolean {
        return descendantsByTagName("action")
            .any { action -> action.getAttribute("android:name") == name }
    }

    private fun Element.descendantsByTagName(tagName: String): Sequence<Element> {
        return childNodes.asSequence()
            .flatMap { node ->
                if (node is Element) {
                    sequenceOf(node) + node.descendantsByTagName(tagName)
                } else {
                    emptySequence()
                }
            }
            .filter { element -> element.tagName == tagName }
    }

    private fun parseXml(file: File): Document =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .asSequence()
            .map { File(userDir, it) }
            .first { it.exists() }
    }

    private fun org.w3c.dom.NodeList.asSequence(): Sequence<Node> {
        return (0 until length).asSequence().map(::item)
    }
}

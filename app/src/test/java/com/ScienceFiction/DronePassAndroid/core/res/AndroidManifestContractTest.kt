package com.ScienceFiction.DronePassAndroid.core.res

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AndroidManifestContractTest {

    @Test
    fun `MainActivity 는 알림과 외부 실행에서 기존 top 인스턴스를 재사용한다`() {
        val manifest = parseManifest()
        val activity = manifest.findActivity(".MainActivity")

        assertEquals("singleTop", activity.getAttribute("android:launchMode"))
    }

    private fun parseManifest(): Document =
        parseXml(resolveProjectFile("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml"))

    private fun Document.findActivity(name: String): Element {
        val activities = getElementsByTagName("activity")
        for (index in 0 until activities.length) {
            val activity = activities.item(index) as Element
            if (activity.getAttribute("android:name") == name) {
                return activity
            }
        }
        error("Activity not found in manifest: $name")
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
}

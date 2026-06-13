package com.ScienceFiction.DronePassAndroid.core.res

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class BackupRulesTest {

    @Test
    fun `Manifest는 OS Auto Backup을 비활성화한다`() {
        val document = parseXml(resolveProjectFile("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml"))
        val application = document.getElementsByTagName("application").item(0) as Element

        assertEquals("false", application.getAttribute("android:allowBackup"))
    }

    @Test
    fun `Manifest는 방어적 백업 제외 규칙을 계속 참조한다`() {
        val document = parseXml(resolveProjectFile("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml"))
        val application = document.getElementsByTagName("application").item(0) as Element

        assertEquals("@xml/backup_rules", application.getAttribute("android:fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", application.getAttribute("android:dataExtractionRules"))
    }

    @Test
    fun `API 30 이하 Auto Backup 규칙은 로컬 저장소 전체를 제외한다`() {
        val document = parseResXml("xml/backup_rules.xml")

        assertNoIncludes(document)
        assertExcludedDomains(
            root = document.documentElement,
            expectedDomains = LocalStorageBackupDomains,
        )
    }

    @Test
    fun `API 31 이상 데이터 추출 규칙은 클라우드 백업과 기기 전송에서 로컬 저장소 전체를 제외한다`() {
        val document = parseResXml("xml/data_extraction_rules.xml")

        assertNoIncludes(document)
        listOf("cloud-backup", "device-transfer").forEach { tagName ->
            val section = document.getElementsByTagName(tagName).item(0) as Element
            assertExcludedDomains(
                root = section,
                expectedDomains = LocalStorageBackupDomains,
            )
        }
    }

    private fun assertNoIncludes(document: Document) {
        assertEquals(0, document.getElementsByTagName("include").length)
    }

    private fun assertExcludedDomains(root: Element, expectedDomains: Set<String>) {
        val excludedDomains = mutableSetOf<String>()
        val excludes = root.getElementsByTagName("exclude")
        for (index in 0 until excludes.length) {
            val element = excludes.item(index) as Element
            if (element.getAttribute("path") == ".") {
                excludedDomains += element.getAttribute("domain")
            }
        }

        assertEquals(expectedDomains, excludedDomains)
    }

    private fun parseResXml(relativePath: String): Document =
        parseXml(resolveProjectFile("src/main/res/$relativePath", "app/src/main/res/$relativePath"))

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

    private companion object {
        val LocalStorageBackupDomains = setOf("root", "file", "database", "sharedpref", "external")
    }
}

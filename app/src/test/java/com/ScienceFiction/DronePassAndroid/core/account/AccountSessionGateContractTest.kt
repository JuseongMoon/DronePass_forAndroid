package com.ScienceFiction.DronePassAndroid.core.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 동기화 관문이 구조로 지켜지는지 소스로 고정한다(기기 데이터 주인 재설계, 3.6.0).
 * 계정 데이터(`users/{uid}/…` 도형·스케치·드론·metadata)의 읽기·쓰기는 모두 [SyncTicket] 을 거쳐야 한다.
 */
class AccountSessionGateContractTest {

    private val root: File = listOf("src/main/java", "app/src/main/java")
        .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
        .first { it.isDirectory }
    private val pkg = File(root, "com/ScienceFiction/DronePassAndroid")

    private fun source(path: String) = File(pkg, path).readText()
    private fun allSources(): Map<String, String> = pkg.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .associate { it.relativeTo(pkg).path to it.readText() }

    @Test
    fun `sync tickets are created only by AccountSession`() {
        val creators = allSources().filterValues { Regex("""\bSyncTicket\(""").containsMatchIn(it) }.keys
        assertEquals(setOf("core/account/AccountSession.kt"), creators)
    }

    @Test
    fun `flow tickets are issued only by the logout flush and import flows`() {
        // 관문이 닫힌 채 서버를 읽고 쓰는 곳: 로그아웃 ① 업로드, 가져오기(후보를 세는 서버 읽기와 후보 업로드).
        val callers = allSources()
            .filterKeys { it != "core/account/AccountSession.kt" }
            .filterValues { it.contains("issueFlowTicket(") }
        assertTrue(callers.keys.all { it == "core/account/AccountSessionFlows.kt" })
        val flows = source("core/account/AccountSessionFlows.kt")
        val issuingFunctions = Regex("""issueFlowTicket\(""").findAll(flows).map { match ->
            Regex("""(?:private |internal )?suspend fun (\w+)\(""")
                .findAll(flows.substring(0, match.range.first)).last().groupValues[1]
        }.toSet()
        assertTrue(issuingFunctions.toString(), issuingFunctions.all { it in setOf("flushBeforeLogout", "readImportCandidates", "uploadImportCandidates") })
    }

    @Test
    fun `account data stores take a ticket on every request`() {
        listOf(
            "core/data/remote/firebase/ShapeFirebaseStore.kt",
            "core/data/remote/firebase/SketchFirebaseStore.kt",
            "core/data/remote/firebase/DroneFirebaseStore.kt",
        ).forEach { path ->
            val text = source(path)
            val classBody = text.substringAfter("@Singleton")
            Regex("""\n    suspend fun \w+\(([^)]*)\)""").findAll(classBody).forEach { match ->
                assertTrue("$path ${match.value.trim()}", match.groupValues[1].contains("ticket: SyncTicket"))
            }
            assertFalse(path, classBody.contains("userId: String) {") && classBody.contains("suspend fun load"))
            assertTrue(path, classBody.contains("session.remote(ticket)"))
        }
    }

    @Test
    fun `repositories and realtime sync never read the signed in uid themselves`() {
        listOf(
            "core/data/repository/ShapeRepository.kt",
            "core/data/repository/SketchRepository.kt",
            "core/data/repository/DroneRepository.kt",
            "core/data/sync/RealtimeSyncManager.kt",
        ).forEach { path ->
            val text = source(path)
            assertFalse(path, text.contains("currentUser"))
            assertFalse(path, text.contains("import com.google.firebase.auth.FirebaseAuth"))
            assertFalse(path, text.contains("cloudBackupEnabled") || text.contains("isCloudSyncEnabled"))
        }
    }

    @Test
    fun `only the gated stores, realtime sync and account metadata writers build users paths`() {
        val writers = allSources().filterValues { it.contains("collection(\"users\")") || it.contains("collection(USERS_COLLECTION)") }.keys
        assertEquals(
            setOf(
                // 계정 데이터(관문 안)
                "core/data/remote/firebase/ShapeFirebaseStore.kt",
                "core/data/remote/firebase/SketchFirebaseStore.kt",
                "core/data/remote/firebase/DroneFirebaseStore.kt",
                "core/data/sync/RealtimeSyncManager.kt",
                // 계정 메타데이터(관문 밖): users/{uid} 문서, devices, 활동 시각
                "feature/auth/AuthRepository.kt",
                "service/FcmService.kt",
                "core/analytics/UserActivityTracker.kt",
            ),
            writers,
        )
    }

    @Test
    fun `room writes after a network read are re-checked against the ticket`() {
        listOf(
            "core/data/repository/ShapeRepository.kt",
            "core/data/repository/SketchRepository.kt",
            "core/data/repository/DroneRepository.kt",
        ).forEach { path ->
            val fullSync = source(path).substringAfter("suspend fun performFullSync(ticket: SyncTicket)")
            assertTrue(path, fullSync.indexOf("IncludingDeleted(ticket)") < fullSync.indexOf("session.commit(ticket)"))
        }
    }
}

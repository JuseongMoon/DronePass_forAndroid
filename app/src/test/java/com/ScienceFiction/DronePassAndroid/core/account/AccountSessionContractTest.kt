package com.ScienceFiction.DronePassAndroid.core.account

import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.Instant

/** iOS 와 같은 공유 fixture(team/fixtures/account-session-cases.json)로 기기 데이터 주인 판단을 확인한다. */
class AccountSessionContractTest {
    private val adapter = Moshi.Builder().build().adapter(Map::class.java)

    private fun fixture(): Map<*, *> {
        val directory = System.getProperty("dronepass.iosFixtureDirectory") ?: "../dronepass-ios/team/fixtures"
        val file = File(directory, "account-session-cases.json")
        val missing = "Shared account session fixture is missing: $file"
        if (System.getProperty("dronepass.requireSharedFixtures") == "true") {
            check(file.isFile) { missing }
        } else {
            assumeTrue(missing, file.isFile)
        }
        return requireNotNull(adapter.fromJson(file.readText()) as? Map<*, *>)
    }

    private fun Map<*, *>.required(key: String): Any = requireNotNull(this[key]) { "Missing $key" }
    private fun Map<*, *>.string(key: String): String = required(key) as? String ?: error("Invalid $key")
    private fun Map<*, *>.list(key: String): List<*> = required(key) as? List<*> ?: error("Invalid $key")
    private fun Map<*, *>.map(key: String): Map<*, *> = required(key) as? Map<*, *> ?: error("Invalid $key")
    private fun Map<*, *>.maps(key: String): List<Map<*, *>> = list(key).map { it as Map<*, *> }
    private fun Map<*, *>.millis(key: String): Long? = (this[key] as? String)?.let { Instant.parse(it).toEpochMilli() }

    private fun Map<*, *>.shape() = ShapeModel(
        id = string("id"),
        droneId = this["droneId"] as? String,
        createdAt = requireNotNull(millis("updatedAt")),
        updatedAt = requireNotNull(millis("updatedAt")),
        deletedAt = millis("deletedAt"),
    )

    private fun Map<*, *>.sketch() = SketchModel(
        id = string("id"),
        createdAt = requireNotNull(millis("updatedAt")),
        updatedAt = requireNotNull(millis("updatedAt")),
        deletedAt = millis("deletedAt"),
    )

    private fun Map<*, *>.drone() = DroneModel(
        id = string("id"),
        name = string("name"),
        color = string("color"),
        serialNumber = this["serialNumber"] as? String,
        takeoffWeight = this["takeoffWeight"] as? String,
        size = this["size"] as? String,
        memo = this["memo"] as? String,
        createdAt = requireNotNull(millis("createdAt")),
        updatedAt = requireNotNull(millis("updatedAt")),
        deletedAt = millis("deletedAt"),
    )

    private fun Map<*, *>.journal(): AccountJournal? {
        val journal = this["journal"] as? Map<*, *> ?: return null
        return when (journal.string("type")) {
            "logout" -> AccountJournal.Logout(journal.string("uid"), requireNotNull(LogoutStep.parse(journal.string("step"))))
            "deleting" -> AccountJournal.Deleting(journal.string("uid"))
            else -> error("Unknown journal type")
        }
    }

    private val actionNames = mapOf(
        "deferUntilProtectedData" to AccountSessionAction.DEFER_UNTIL_PROTECTED_DATA,
        "runMigration" to AccountSessionAction.RUN_MIGRATION,
        "guestIdle" to AccountSessionAction.GUEST_IDLE,
        "openGate" to AccountSessionAction.OPEN_GATE,
        "adoptEmpty" to AccountSessionAction.ADOPT_EMPTY,
        "evaluateImport" to AccountSessionAction.EVALUATE_IMPORT,
        "confirmReplaceOtherAccountData" to AccountSessionAction.CONFIRM_REPLACE_OTHER_ACCOUNT_DATA,
        "sessionLost" to AccountSessionAction.SESSION_LOST,
        "abortLogout" to AccountSessionAction.ABORT_LOGOUT,
        "finishLogoutWipe" to AccountSessionAction.FINISH_LOGOUT_WIPE,
        "checkDeletedAccount" to AccountSessionAction.CHECK_DELETED_ACCOUNT,
        "finishDeletionWipe" to AccountSessionAction.FINISH_DELETION_WIPE,
    )

    private val promptNames = mapOf(
        "guest" to ImportPromptKind.GUEST,
        "legacySameAccount" to ImportPromptKind.LEGACY_SAME_ACCOUNT,
        "legacyOtherAccount" to ImportPromptKind.LEGACY_OTHER_ACCOUNT,
    )

    @Test fun `fixture has 65 cases and every action and prompt kind is known`() {
        val root = fixture()
        assertEquals(1.0, (root.required("schemaVersion") as Number).toDouble(), 0.0)
        val counts = listOf(
            "sessionCases", "migrationCases", "deviceHasDataCases", "importCandidateCases", "deletionCheckCases",
            "reloadClassificationCases",
        ).map { root.list(it).size }
        assertEquals(listOf(26, 5, 5, 11, 9, 9), counts)
        assertEquals(65, counts.sum())
        assertEquals(DELETION_CHECK_GRACE_MINUTES.toDouble(), (root.required("deletionCheckGraceMinutes") as Number).toDouble(), 0.0)
        assertEquals(actionNames.keys, root.map("actions").keys)
        assertEquals(promptNames.keys, root.map("promptKinds").keys)
    }

    @Test fun `session decisions match the shared fixture`() {
        fixture().maps("sessionCases").forEach { row ->
            val name = row.string("name")
            val input = row.map("input")
            val expected = row.map("expected")
            val decision = decideAccountSession(
                AccountSessionInput(
                    protectedDataAvailable = input.required("protectedDataAvailable") as Boolean,
                    owner = (input["owner"] as? String)?.let { requireNotNull(decodeLocalDataOwner(it)) { it } },
                    authUid = input["authUid"] as? String,
                    journal = input.journal(),
                    pendingImportUid = input["pendingImportUid"] as? String,
                    deviceHasData = input.required("deviceHasData") as Boolean,
                    accountDeletedUid = input["accountDeletedUid"] as? String,
                ),
            )
            assertEquals(name, actionNames.getValue(expected.string("action")), decision.action)
            assertEquals(name, expected.required("gateOpen") as Boolean, decision.gateOpen)
            assertEquals(name, (expected["promptKind"] as? String)?.let(promptNames::getValue), decision.promptKind)
            assertEquals(name, expected["thenReevaluate"] as? Boolean ?: false, decision.thenReevaluate)
        }
    }

    @Test fun `deletion journal checks match the shared fixture`() {
        val results = mapOf(
            "deleted" to DeletionReloadResult.DELETED,
            "exists" to DeletionReloadResult.EXISTS,
            "unknown" to DeletionReloadResult.UNKNOWN,
        )
        val actions = mapOf(
            "finishDeletionWipe" to DeletionCheckAction.FINISH_DELETION_WIPE,
            "keepJournalRetryLater" to DeletionCheckAction.KEEP_JOURNAL_RETRY_LATER,
            "clearJournalThenReevaluate" to DeletionCheckAction.CLEAR_JOURNAL_THEN_REEVALUATE,
        )
        fixture().maps("deletionCheckCases").forEach { row ->
            val input = row.map("input")
            val action = decideDeletionCheck(
                result = results.getValue(input.string("reloadResult")),
                minutesSinceStart = (input["minutesSinceStart"] as? Number)?.toLong(),
            )
            assertEquals(row.string("name"), actions.getValue(row.map("expected").string("action")), action)
        }
    }

    /**
     * 계정 확인 분류(reloadClassificationCases): "사용자 없음"만 탈퇴다. Android 는 분류를 reloadAccountStatus 와 두 호출부가
     * 나눠 맡으므로(순수 함수로 떼어 내지 않았다) 각 사례가 어느 코드로 처리되는지 소스로 고정한다.
     */
    @Test fun `reload classification cases are handled the same way by the Android call sites`() {
        val cases = fixture().maps("reloadClassificationCases")
        assertEquals(9, cases.size)
        val deleted = cases.filter { it["expected"] == "deleted" }
        assertEquals(1, deleted.size)
        assertEquals("userNotFound", (deleted.single().map("input")["error"] as Map<*, *>).string("code"))

        val service = source("feature/profile/AccountDeletionService.kt")
        val flows = source("core/account/AccountSessionFlows.kt")
        // 사용자 없음 → DELETED. 그 밖의 FirebaseAuthInvalidUserException(토큰 만료·무효, 비활성화)은 다시 던진다.
        assertTrue(service.contains("private const val USER_NOT_FOUND_ERROR_CODE = \"ERROR_USER_NOT_FOUND\""))
        assertTrue(service.contains("if (isUserNotFoundErrorCode(error.errorCode)) AccountReloadStatus.DELETED else throw error"))
        // 다시 던진 오류·네트워크 오류·Auth 밖 오류는 두 호출부에서 모두 unknown(데이터 유지)이다.
        assertEquals(2, Regex("""runCatching \{ reloadAccountStatus\(user\) \}\.getOrNull\(\)""").findAll(flows).count())
        assertTrue(flows.contains("null -> DeletionReloadResult.UNKNOWN"))
        assertTrue(flows.contains("null -> Log.w(TAG, \"계정 확인 실패. 다음 실행까지 동기화를 멈춥니다.\")"))
        // 쓰기 거부 뒤 확인: 현재 사용자가 없거나 다른 계정이면 확인하지 않는다(unknown, 데이터 유지).
        assertTrue(flows.contains("val user = auth.currentUser?.takeIf { it.uid == uid } ?: return"))
        // accountDeletedUid 는 DELETED 일 때만 세운다.
        val denied = flows.substringAfter("private suspend fun onPermissionDenied(")
        assertEquals(1, Regex("""accountDeletedUid = uid""").findAll(denied).count())
        assertTrue(denied.indexOf("AccountReloadStatus.DELETED ->") < denied.indexOf("accountDeletedUid = uid"))
    }

    private fun source(path: String): String {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        return File(root, "com/ScienceFiction/DronePassAndroid/$path").readText()
    }

    @Test fun `migration owners match the shared fixture`() {
        fixture().maps("migrationCases").forEach { row ->
            val input = row.map("input")
            val owner = decideMigrationOwner(input["authUid"] as? String, input["savedUid"] as? String)
            assertEquals(row.string("name"), row.map("expected").string("owner"), owner.encode())
        }
    }

    @Test fun `device data presence matches the shared fixture`() {
        fixture().maps("deviceHasDataCases").forEach { row ->
            val local = row.map("local")
            val hasData = deviceHasAccountData(
                shapes = local.maps("shapes").map { it.shape() },
                sketches = local.maps("sketches").map { it.sketch() },
                drones = local.maps("drones").map { it.drone() },
            )
            assertEquals(row.string("name"), row.required("expected") as Boolean, hasData)
        }
    }

    @Test fun `import candidates match the shared fixture`() {
        val now = Instant.parse("2026-10-02T00:00:00Z").toEpochMilli()
        fixture().maps("importCandidateCases").forEach { row ->
            val name = row.string("name")
            val local = row.map("local")
            val server = row.map("server")
            val expected = row.map("expected")
            val candidates = computeImportCandidates(
                localShapes = local.maps("shapes").map { it.shape() },
                localSketches = local.maps("sketches").map { it.sketch() },
                localDrones = local.maps("drones").map { it.drone() },
                serverShapes = server.maps("shapes").map { it.shape() },
                serverSketches = server.maps("sketches").map { it.sketch() },
                serverDrones = server.maps("drones").map { it.drone() },
                nowMillis = now,
            )
            assertEquals(name, expected.list("shapeIds").toSet(), candidates.shapes.map { it.id }.toSet())
            assertEquals(name, expected.list("sketchIds").toSet(), candidates.sketches.map { it.id }.toSet())
            assertEquals(name, expected.list("droneIds").toSet(), candidates.drones.map { it.id }.toSet())
            assertEquals(
                name,
                expected.map("shapeDroneIdRewrites").entries.associate { (k, v) -> k as String to v as String? },
                candidates.shapeDroneIdRewrites,
            )
        }
    }
}

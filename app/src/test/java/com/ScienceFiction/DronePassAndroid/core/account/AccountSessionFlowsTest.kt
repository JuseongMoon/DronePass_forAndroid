package com.ScienceFiction.DronePassAndroid.core.account

import com.ScienceFiction.DronePassAndroid.core.data.sync.SyncPreferenceKeys
import com.ScienceFiction.DronePassAndroid.core.location.testPreferencesDataStore
import com.ScienceFiction.DronePassAndroid.feature.account.ImportPromptButton
import com.ScienceFiction.DronePassAndroid.feature.account.importPromptButtons
import com.ScienceFiction.DronePassAndroid.feature.drone.DroneSelectionPreferenceKeys
import com.ScienceFiction.DronePassAndroid.feature.profile.ProfilePreferenceKeys
import com.ScienceFiction.DronePassAndroid.feature.shape.ShapeEditPreferenceKeys
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** 기기 데이터 주인 관문의 동작과 로그아웃·가져오기·탈퇴 흐름의 순서(3.6.0 재설계). */
class AccountSessionFlowsTest {

    @get:Rule
    val folder = TemporaryFolder()

    private var signedInUid: String? = "A"
    private fun newSession() = AccountSession(testPreferencesDataStore(folder.root)) { signedInUid }

    // region 관문

    @Test
    fun `gate opens only for the signed in owner and closing it invalidates every earlier ticket`() = runBlocking {
        val session = newSession()
        assertNull(session.syncTicket())

        session.openGate("A")
        val ticket = requireNotNull(session.syncTicket())
        assertTrue(session.isValid(ticket))

        session.closeGate()
        assertFalse(session.isValid(ticket))
        assertNull(session.syncTicket())
    }

    @Test
    fun `a ticket from before an account change can never commit to room`() = runBlocking {
        val session = newSession()
        session.openGate("A")
        val ticket = requireNotNull(session.syncTicket())

        signedInUid = "B"
        assertFalse(session.isValid(ticket))
        var committed = false
        val result = runCatching { session.commit(ticket) { committed = true } }

        assertTrue(result.exceptionOrNull() is StaleSyncTicketException)
        assertFalse(committed)
    }

    @Test
    fun `permission denied closes the gate, invalidates the ticket and is not reopened in this process`() = runBlocking {
        val session = newSession()
        session.openGate("A")
        val ticket = requireNotNull(session.syncTicket())

        session.reportPermissionDenied(ticket)

        assertFalse(session.isValid(ticket))
        assertNull(session.syncTicket())
        // 다시 열어도 같은 계정은 막는다(같은 오류를 되풀이하지 않는다). 다시 판단해 동기화를 시작할 때만 풀린다.
        session.openGate("A")
        assertNull(session.syncTicket())
        session.clearPermissionDenied()
        session.openGate("A")
        assertNotNull(session.syncTicket())
    }

    @Test
    fun `flow tickets work while the gate is closed but die when the gate changes`() = runBlocking {
        val session = newSession()
        val flush = session.issueFlowTicket("A", SyncTicketPurpose.LOGOUT_FLUSH)
        assertTrue(session.isValid(flush))

        session.closeGate()
        assertFalse(session.isValid(flush))
        assertTrue(runCatching { session.issueFlowTicket("A", SyncTicketPurpose.SYNC) }.isFailure)
    }

    @Test
    fun `stores report permission denied to the gate before rethrowing`() {
        val text = source("core/account/AccountSession.kt").substringAfter("suspend fun <T> remote(")
            .substringBefore("internal fun reportPermissionDenied")
        assertOrder(text, listOf("requireValid(ticket)", "request(ticket.uid)", "if (e.isPermissionDenied()) reportPermissionDenied(ticket)", "throw e"))
    }

    // endregion

    // region 저장 표기

    @Test
    fun `owner and journal encodings round trip and reject garbage`() {
        listOf(LocalDataOwner.Guest, LocalDataOwner.Account("A"), LocalDataOwner.Legacy("B")).forEach {
            assertEquals(it, decodeLocalDataOwner(it.encode()))
        }
        assertEquals("uid:A", LocalDataOwner.Account("A").encode())
        assertNull(decodeLocalDataOwner("uid:"))
        assertNull(decodeLocalDataOwner("someone"))

        listOf(
            AccountJournal.Logout("A", LogoutStep.FLUSHING),
            AccountJournal.Logout("A", LogoutStep.SIGNING_OUT),
            AccountJournal.Logout("A", LogoutStep.WIPING),
            AccountJournal.Deleting("A"),
        ).forEach { assertEquals(it, decodeAccountJournal(it.encode())) }
        assertNull(decodeAccountJournal("logout|nope|A"))
        assertNull(decodeAccountJournal(null))
    }

    @Test
    fun `owner, pending import, journal and epoch are kept in DataStore`() = runBlocking {
        val session = newSession()
        assertNull(session.state().owner)
        session.updateState {
            writeAccountOwner(LocalDataOwner.Account("A"))
            writePendingImportUid("B")
            writeAccountJournal(AccountJournal.Logout("A", LogoutStep.SIGNING_OUT))
            bumpAccountEpoch()
            bumpAccountEpoch()
        }
        val state = session.state()
        assertEquals(LocalDataOwner.Account("A"), state.owner)
        assertEquals("B", state.pendingImportUid)
        assertEquals(AccountJournal.Logout("A", LogoutStep.SIGNING_OUT), state.journal)
        assertEquals(2L, state.epoch)
    }

    // endregion

    // region 로그아웃 ① 업로드 대상

    private data class Item(val id: String, val updatedAt: Long, val deletedAt: Long? = null)

    @Test
    fun `logout uploads only items missing on the server or newer than it`() {
        val local = listOf(
            Item("only-local", 5),
            Item("only-local-deleted", 5, deletedAt = 5),
            Item("newer", 9),
            Item("newer-deleted", 9, deletedAt = 9),
            Item("same", 5),
            Item("older", 3),
        )
        val server = listOf(Item("newer", 5), Item("newer-deleted", 5), Item("same", 5), Item("older", 7))

        val upload = logoutFlushItems(local, server, Item::id, Item::updatedAt, Item::deletedAt)

        // 다른 기기에서 고친 더 새로운 서버 값은 덮어쓰지 않는다(예전의 "활성 항목 전부 업로드"와 다르다).
        assertEquals(listOf("only-local", "newer", "newer-deleted"), upload.map { it.id })
    }

    // endregion

    // region 흐름 순서(소스 계약)

    @Test
    fun `logout writes a journal at each step and signs out before wiping`() {
        val logout = source("core/account/AccountSessionFlows.kt")
            .substringAfter("private suspend fun logoutLocked(").substringBefore("private suspend fun flushBeforeLogout(")
        assertOrder(
            logout,
            listOf(
                "AccountJournal.Logout(uid, LogoutStep.FLUSHING)",
                "session.closeGate()",
                "realtimeSyncManager.stopListeningAndWait()",
                "sketchRepository.cancelAllPendingSyncsAndWait()",
                "withTimeout(LOGOUT_FLUSH_TIMEOUT_MS) { flushBeforeLogout(uid) }",
                "LogoutResult.NEEDS_OFFLINE_CONFIRMATION",
                "AccountJournal.Logout(uid, LogoutStep.SIGNING_OUT)",
                "FcmService.deactivateTokenAndWait(context)",
                "auth.signOut()",
                "LogoutResult.FAILED",
                "finishLogoutWipe()",
                "analyticsLogger.logLogout()",
            ),
        )
        assertEquals(10_000L, LOGOUT_FLUSH_TIMEOUT_MS)
        val flush = source("core/account/AccountSessionFlows.kt")
            .substringAfter("private suspend fun flushBeforeLogout(").substringBefore("private suspend fun uploadAndMark(")
        assertOrder(flush, listOf("Source.SERVER", "logoutFlushItems(", "uploadAndMark(", "firestore.waitForPendingWrites().await()"))
    }

    @Test
    fun `logout and account deletion run in the app scope so closing the screen does not cut them off`() {
        val flows = source("core/account/AccountSessionFlows.kt")
        assertTrue(flows.contains("scope.async { evaluationMutex.withLock { logoutLocked(proceedWithoutUpload) } }.await()"))
        assertTrue(flows.contains("scope.async { evaluationMutex.withLock { deleteAccountLocked(activity) } }.await()"))
    }

    @Test
    fun `wiping closes the gate and waits for running syncs before deleting device data`() {
        val wipe = source("core/account/AccountSessionFlows.kt")
            .substringAfter("private suspend fun wipeDeviceAccountData()").substringBefore("private fun markNeedsFirestoreClear()")
        assertOrder(
            wipe,
            listOf(
                "session.closeGate()",
                "realtimeSyncManager.stopListeningAndWait()",
                "sketchRepository.cancelAllPendingSyncsAndWait()",
                // ShapeRepository.deleteAllShapes 가 도형 종료 알람과 예약 목록도 정리한다.
                "shapeRepository.deleteAllShapes()",
                "droneRepository.deleteAllDrones()",
                "sketchRepository.deleteAllSketchesLocally()",
                "droneSelectionState.resetForAccountSwitch()",
                "realtimeSyncManager.resetSyncTrackingForAccountSwitch()",
                "DEVICE_ACCOUNT_DATA_PREFERENCE_KEYS",
                "encryptedPrefsHelper.clearAccountKeys()",
                "NotificationManagerCompat.from(context).cancelAll()",
            ),
        )
        val finish = source("core/account/AccountSessionFlows.kt")
            .substringAfter("private suspend fun finishLogoutWipe()").substringBefore("// endregion")
        assertOrder(
            finish,
            listOf(
                "LogoutStep.WIPING",
                "wipeDeviceAccountData()",
                "markNeedsFirestoreClear()",
                "writeAccountOwner(LocalDataOwner.Guest)",
                "writeAccountJournal(null)",
                "bumpAccountEpoch()",
                "droneRepository.ensureDefaultDroneIfNeeded()",
            ),
        )
    }

    @Test
    fun `device data keys cover sync, backup, drone selection and last selected drone`() {
        assertEquals(
            setOf(
                SyncPreferenceKeys.LAST_SYNC_TIME,
                SyncPreferenceKeys.LAST_LOCAL_MODIFICATION_TIME,
                SyncPreferenceKeys.LAST_LOCAL_DRONE_MODIFICATION_TIME,
                SyncPreferenceKeys.SYNCED_SHAPE_BASELINE,
                SyncPreferenceKeys.LAST_SKETCH_SYNC_TIME,
                SyncPreferenceKeys.LAST_LOCAL_SKETCH_MODIFICATION_TIME,
                ProfilePreferenceKeys.LAST_BACKUP_TIME,
                ProfilePreferenceKeys.LEGACY_LAST_BACKUP_TIME,
                ProfilePreferenceKeys.CLOUD_BACKUP_ENABLED,
                ProfilePreferenceKeys.LEGACY_CLOUD_BACKUP_ENABLED,
                DroneSelectionPreferenceKeys.SELECTED_DRONE_ID,
                DroneSelectionPreferenceKeys.SELECTED_DRONE_IDS,
                DroneSelectionPreferenceKeys.LEGACY_SELECTED_DRONE_IDS,
                ShapeEditPreferenceKeys.LAST_SELECTED_DRONE_ID,
                ShapeEditPreferenceKeys.LEGACY_LAST_SELECTED_DRONE_ID,
            ),
            DEVICE_ACCOUNT_DATA_PREFERENCE_KEYS.toSet(),
        )
        assertEquals(
            listOf("selected_drone_ids", "last_selected_drone_id", "last_backup_time"),
            listOf(
                DroneSelectionPreferenceKeys.LEGACY_SELECTED_DRONE_IDS.name,
                ShapeEditPreferenceKeys.LEGACY_LAST_SELECTED_DRONE_ID.name,
                ProfilePreferenceKeys.LEGACY_LAST_BACKUP_TIME.name,
            ),
        )
    }

    @Test
    fun `account deletion records its journal before calling the server and keeps the device id`() {
        val delete = source("core/account/AccountSessionFlows.kt")
            .substringAfter("private suspend fun deleteAccountLocked(").substringBefore("private suspend fun checkDeletedAccount()")
        assertOrder(
            delete,
            listOf(
                "writeAccountJournal(AccountJournal.Deleting(uid))",
                "session.closeGate()",
                "accountDeletionService.deleteCurrentAccount(activity)",
                "if (result.isFailure)",
                "writeAccountJournal(null)",
                "finishDeletionWipe()",
            ),
        )
        // DeviceUUID·fcm_device_id 는 같은 EncryptedPrefs 파일에 있으므로 파일 전체를 지우지 않는다.
        val prefs = source("core/data/local/EncryptedPrefsHelper.kt")
        assertFalse(prefs.contains("clear()"))
        assertTrue(prefs.contains("fun clearAccountKeys()"))
    }

    @Test
    fun `firestore cache is cleared on the next launch before the first account evaluation`() {
        val app = source("app/DronePassApplication.kt")
        assertOrder(app, listOf("clearFirestorePersistenceIfNeeded(this)", "accountSessionFlows.start()"))
    }

    @Test
    fun `import, replace and session lost prompts treat dismissal as cancel`() {
        val dialogs = source("feature/account/AccountSessionDialogs.kt")
        assertTrue(dialogs.contains("onDismissRequest = { viewModel.cancelImport(current.uid) }"))
        assertTrue(dialogs.contains("onDismissRequest = { viewModel.cancelReplace(current.uid) }"))
        assertTrue(dialogs.contains("onDismissRequest = { viewModel.acknowledgeSessionLost() }"))
        // 삭제는 개수를 보여 주며 한 번 더 묻는다.
        assertTrue(dialogs.contains("R.string.account_import_delete_confirm_message, itemCount"))
    }

    @Test
    fun `answers are checked against the pending question so a repeated answer does nothing`() {
        val flows = source("core/account/AccountSessionFlows.kt")
        listOf("fun chooseImport(", "fun chooseDeleteDeviceDataForImport(", "fun cancelImport(").forEach { name ->
            val body = flows.substringAfter(name).substringBefore("\n    }\n")
            assertTrue(name, body.contains("if (!isAskingImport(uid)) return@withLock"))
        }
    }

    @Test
    fun `possibly another account's data defaults to delete`() {
        assertEquals(ImportPromptButton.DELETE, importPromptButtons(ImportPromptKind.LEGACY_OTHER_ACCOUNT).first())
        assertEquals(ImportPromptButton.IMPORT, importPromptButtons(ImportPromptKind.GUEST).first())
        assertEquals(ImportPromptButton.IMPORT, importPromptButtons(ImportPromptKind.LEGACY_SAME_ACCOUNT).first())
    }

    // endregion

    private fun source(path: String): String {
        val root = listOf("src/main/java", "app/src/main/java")
            .map { File(File(requireNotNull(System.getProperty("user.dir"))), it) }
            .first { it.isDirectory }
        return File(root, "com/ScienceFiction/DronePassAndroid/$path").readText()
    }

    private fun assertOrder(text: String, tokens: List<String>) {
        var previous = -1
        tokens.forEach { token ->
            val index = text.indexOf(token, previous + 1)
            assertTrue("$token should appear after index $previous", index > previous)
            previous = index
        }
    }
}

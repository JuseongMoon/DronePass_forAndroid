package com.ScienceFiction.DronePassAndroid.core.account

import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneSyncMergeResult
import com.ScienceFiction.DronePassAndroid.core.data.repository.isUntouchedDefaultDrone
import com.ScienceFiction.DronePassAndroid.core.data.repository.mergeDronesForFullSync
import com.ScienceFiction.DronePassAndroid.core.data.repository.reconcileShapeColorsWithServer
import com.ScienceFiction.DronePassAndroid.core.data.sync.isNewerUpdatedAt
import com.ScienceFiction.DronePassAndroid.domain.model.DroneModel
import com.ScienceFiction.DronePassAndroid.domain.model.ShapeModel
import com.ScienceFiction.DronePassAndroid.domain.model.SketchModel
import com.ScienceFiction.DronePassAndroid.domain.model.TagModel
import com.ScienceFiction.DronePassAndroid.domain.model.mergeTags

/*
 * 기기 데이터 주인 판단(3.6.0). 계약: iOS team/DATA_CONTRACT.md §기기 데이터 주인,
 * 테스트 벡터: team/fixtures/account-session-cases.json. 여기의 함수는 모두 순수 함수다.
 */

/** 기기 저장소의 주인. 저장 표기는 "guest", "uid:<uid>", "legacy:<uid>"(3.5.x에서 로그아웃 상태로 쓰던 기기). */
internal sealed interface LocalDataOwner {
    data object Guest : LocalDataOwner
    data class Account(val uid: String) : LocalDataOwner
    data class Legacy(val uid: String) : LocalDataOwner
}

internal fun LocalDataOwner.encode(): String = when (this) {
    LocalDataOwner.Guest -> "guest"
    is LocalDataOwner.Account -> "uid:$uid"
    is LocalDataOwner.Legacy -> "legacy:$uid"
}

internal fun decodeLocalDataOwner(value: String?): LocalDataOwner? = when {
    value == null -> null
    value == "guest" -> LocalDataOwner.Guest
    value.startsWith("uid:") && value.length > 4 -> LocalDataOwner.Account(value.removePrefix("uid:"))
    value.startsWith("legacy:") && value.length > 7 -> LocalDataOwner.Legacy(value.removePrefix("legacy:"))
    else -> null
}

/** 로그아웃 단계. signOut 전(flushing·signingOut)에 멈추면 로그아웃을 취소할 수 있다. */
internal enum class LogoutStep(val wireValue: String) {
    FLUSHING("flushing"),
    SIGNING_OUT("signingOut"),
    WIPING("wiping"),
    ;

    companion object {
        fun parse(value: String?): LogoutStep? = entries.firstOrNull { it.wireValue == value }
    }
}

/** 로그아웃·탈퇴 도중 앱이 멈춰도 다음 실행에서 이어 가기 위한 기록. */
internal sealed interface AccountJournal {
    val uid: String

    data class Logout(override val uid: String, val step: LogoutStep) : AccountJournal
    /** [startedAtMillis]: 서버 탈퇴 호출 직전 시각. 이전 빌드가 남긴 journal 이나 호출 전에 멈춘 경우에는 없다. */
    data class Deleting(override val uid: String, val startedAtMillis: Long? = null) : AccountJournal
}

internal fun AccountJournal.encode(): String = when (this) {
    is AccountJournal.Logout -> "logout|${step.wireValue}|$uid"
    is AccountJournal.Deleting -> if (startedAtMillis == null) "deleting|$uid" else "deleting|$uid|$startedAtMillis"
}

internal fun decodeAccountJournal(value: String?): AccountJournal? {
    val parts = value?.split('|') ?: return null
    return when {
        parts.size == 3 && parts[0] == "logout" && parts[2].isNotEmpty() ->
            LogoutStep.parse(parts[1])?.let { AccountJournal.Logout(parts[2], it) }
        parts.size == 2 && parts[0] == "deleting" && parts[1].isNotEmpty() -> AccountJournal.Deleting(parts[1])
        parts.size == 3 && parts[0] == "deleting" && parts[1].isNotEmpty() ->
            parts[2].toLongOrNull()?.let { AccountJournal.Deleting(parts[1], it) }
        else -> null
    }
}

internal enum class AccountSessionAction {
    DEFER_UNTIL_PROTECTED_DATA,
    RUN_MIGRATION,
    GUEST_IDLE,
    OPEN_GATE,
    ADOPT_EMPTY,
    EVALUATE_IMPORT,
    CONFIRM_REPLACE_OTHER_ACCOUNT_DATA,
    SESSION_LOST,
    ABORT_LOGOUT,
    FINISH_LOGOUT_WIPE,
    CHECK_DELETED_ACCOUNT,
    FINISH_DELETION_WIPE,
}

/** 가져오기 확인창의 종류. */
enum class ImportPromptKind {
    /** 로그인 전에 이 기기에서 만든 데이터. 기본 선택 없음. */
    GUEST,

    /** 3.5.x에서 같은 계정으로 쓰던 기기. 문구·동작은 [GUEST]와 같다. */
    LEGACY_SAME_ACCOUNT,

    /** 3.5.x에서 다른 계정으로 쓰던 기기. "다른 계정의 데이터일 수 있습니다", 기본 선택 [삭제]. */
    LEGACY_OTHER_ACCOUNT,
}

internal data class AccountSessionInput(
    val owner: LocalDataOwner?,
    val authUid: String?,
    val journal: AccountJournal?,
    val pendingImportUid: String?,
    val deviceHasData: Boolean,
    /**
     * 이 기기가 그 uid 계정의 삭제를 확인했을 때만 값이 있다(user.reload 가 ERROR_USER_NOT_FOUND 로 실패 — 흔히
     * 쓰기가 PERMISSION_DENIED 로 거부된 직후 확인). 단순 세션 만료·토큰 폐기·비활성화는 삭제 확인이 아니다.
     */
    val accountDeletedUid: String? = null,
    /** iOS 첫 잠금해제 전 실행. Android 는 항상 true. */
    val protectedDataAvailable: Boolean = true,
)

internal data class AccountSessionDecision(
    val action: AccountSessionAction,
    val gateOpen: Boolean = false,
    val promptKind: ImportPromptKind? = null,
    /** journal 을 처리한 뒤 journal 을 지우고 다시 판단한다. */
    val thenReevaluate: Boolean = false,
)

/**
 * 판단 순서: ① 첫 잠금해제 전이면 미룬다 ② journal 이 있으면 journal 규칙 ③ 주인 계정의 삭제가 확인됐으면
 * 탈퇴 정리 ④ 주인 표시가 없으면 이전 ⑤ 주인 × 로그인 계정 × 기기 데이터 유무 표.
 */
internal fun decideAccountSession(input: AccountSessionInput): AccountSessionDecision {
    if (!input.protectedDataAvailable) {
        return AccountSessionDecision(AccountSessionAction.DEFER_UNTIL_PROTECTED_DATA)
    }
    val authUid = input.authUid
    when (val journal = input.journal) {
        is AccountJournal.Logout -> return when {
            journal.step == LogoutStep.WIPING ->
                AccountSessionDecision(AccountSessionAction.FINISH_LOGOUT_WIPE, thenReevaluate = true)
            authUid != null ->
                AccountSessionDecision(AccountSessionAction.ABORT_LOGOUT, thenReevaluate = true)
            journal.step == LogoutStep.SIGNING_OUT ->
                AccountSessionDecision(AccountSessionAction.FINISH_LOGOUT_WIPE, thenReevaluate = true)
            // 업로드 중에 세션이 사라졌다. 미동기화분이 있을 수 있으므로 지우지 않는다.
            else -> AccountSessionDecision(AccountSessionAction.SESSION_LOST)
        }
        is AccountJournal.Deleting -> return if (authUid != null) {
            AccountSessionDecision(AccountSessionAction.CHECK_DELETED_ACCOUNT)
        } else {
            AccountSessionDecision(AccountSessionAction.FINISH_DELETION_WIPE, thenReevaluate = true)
        }
        null -> Unit
    }
    // 다른 기기(또는 웹)에서 탈퇴한 계정: 토큰이 아직 살아 있어도 이 기기 데이터를 지우고 로그아웃한다.
    val deletedUid = input.accountDeletedUid
    if (deletedUid != null && input.owner == LocalDataOwner.Account(deletedUid)) {
        return AccountSessionDecision(AccountSessionAction.FINISH_DELETION_WIPE, thenReevaluate = true)
    }
    val owner = input.owner ?: return AccountSessionDecision(AccountSessionAction.RUN_MIGRATION)

    if (authUid == null) {
        return when (owner) {
            is LocalDataOwner.Account -> AccountSessionDecision(AccountSessionAction.SESSION_LOST)
            LocalDataOwner.Guest, is LocalDataOwner.Legacy -> AccountSessionDecision(AccountSessionAction.GUEST_IDLE)
        }
    }
    if (owner == LocalDataOwner.Account(authUid)) {
        return AccountSessionDecision(AccountSessionAction.OPEN_GATE, gateOpen = true)
    }
    if (!input.deviceHasData) {
        return AccountSessionDecision(AccountSessionAction.ADOPT_EMPTY)
    }
    return when (owner) {
        LocalDataOwner.Guest ->
            AccountSessionDecision(AccountSessionAction.EVALUATE_IMPORT, promptKind = ImportPromptKind.GUEST)
        is LocalDataOwner.Legacy -> AccountSessionDecision(
            AccountSessionAction.EVALUATE_IMPORT,
            promptKind = if (owner.uid == authUid) {
                ImportPromptKind.LEGACY_SAME_ACCOUNT
            } else {
                ImportPromptKind.LEGACY_OTHER_ACCOUNT
            },
        )
        is LocalDataOwner.Account -> AccountSessionDecision(AccountSessionAction.CONFIRM_REPLACE_OTHER_ACCOUNT_DATA)
    }
}

/** 탈퇴 journal 이 남은 채 세션이 있을 때 user.reload 결과. 오프라인 등으로 확인하지 못하면 [UNKNOWN]. */
internal enum class DeletionReloadResult { DELETED, EXISTS, UNKNOWN }

internal enum class DeletionCheckAction {
    FINISH_DELETION_WIPE,

    /** journal 유지·관문 닫힘. 앱 복귀나 다음 실행 때 다시 확인한다. */
    KEEP_JOURNAL_RETRY_LATER,

    /** 탈퇴가 일어나지 않은 것으로 보고 journal 을 지운 뒤 다시 판단한다. */
    CLEAR_JOURNAL_THEN_REEVALUATE,
}

/** 서버가 아직 지우는 중일 수 있는 시간(Scheduler 5분 주기 + 함수 제한 540초 + 여유). */
internal const val DELETION_CHECK_GRACE_MINUTES = 15L

/**
 * 탈퇴 journal 확인(계약 deletionCheckRules). 지워졌으면 정리하고, 확인하지 못하면 journal 을 지킨다.
 * 계정이 남아 있어도 서버 호출 뒤 [DELETION_CHECK_GRACE_MINUTES] 안이면 아직 지우는 중일 수 있으므로 journal 을 지킨다.
 */
internal fun decideDeletionCheck(result: DeletionReloadResult, minutesSinceStart: Long?): DeletionCheckAction = when (result) {
    DeletionReloadResult.DELETED -> DeletionCheckAction.FINISH_DELETION_WIPE
    DeletionReloadResult.UNKNOWN -> DeletionCheckAction.KEEP_JOURNAL_RETRY_LATER
    DeletionReloadResult.EXISTS ->
        if (minutesSinceStart != null && minutesSinceStart < DELETION_CHECK_GRACE_MINUTES) {
            DeletionCheckAction.KEEP_JOURNAL_RETRY_LATER
        } else {
            DeletionCheckAction.CLEAR_JOURNAL_THEN_REEVALUATE
        }
}

/** 탈퇴 시작 뒤 지난 분(내림). 시계가 뒤로 갔으면 0으로 본다. */
internal fun minutesSinceDeletionStart(startedAtMillis: Long?, nowMillis: Long): Long? =
    startedAtMillis?.let { ((nowMillis - it).coerceAtLeast(0L)) / 60_000L }

/**
 * 3.5.x(또는 106 이하)에서 처음 올라왔을 때 한 번만 주인을 정한다. [savedUid] 는 예전 EncryptedPrefs 의
 * "마지막 로그인 uid" 이다. 이전이 끝나면 그 키를 지운다.
 */
internal fun decideMigrationOwner(authUid: String?, savedUid: String?): LocalDataOwner = when {
    authUid != null && (savedUid == null || savedUid == authUid) -> LocalDataOwner.Account(authUid)
    savedUid != null -> LocalDataOwner.Legacy(savedUid)
    else -> LocalDataOwner.Guest
}

/** 기기에 계정으로 옮길 만한 데이터가 있는지. 삭제 표시와 손대지 않은 기본 드론은 데이터로 치지 않는다(태그는 기본값이 없다). */
internal fun deviceHasAccountData(
    shapes: List<ShapeModel>,
    sketches: List<SketchModel>,
    drones: List<DroneModel>,
    tags: List<TagModel>,
): Boolean =
    shapes.any { it.deletedAt == null } ||
        sketches.any { it.deletedAt == null } ||
        drones.any { it.deletedAt == null && !isUntouchedDefaultDrone(it) } ||
        tags.any { !it.isDeleted }

/** 도형·스케치 공통 후보 규칙: 기기 쪽이 삭제 표시가 아니고, 서버에 없거나(삭제 표시 포함) 서버보다 허용 오차를 넘게 새롭다. */
internal fun isImportCandidate(
    localUpdatedAt: Long,
    localDeletedAt: Long?,
    serverUpdatedAt: Long?,
    serverDeletedAt: Long?,
    serverExists: Boolean,
): Boolean {
    if (localDeletedAt != null) return false
    if (!serverExists) return true
    if (serverDeletedAt != null) return false
    return isNewerUpdatedAt(localUpdatedAt, than = requireNotNull(serverUpdatedAt))
}

/**
 * 가져오기 후보. [shapes] 는 드론 재지정과 droneId·tagId 정리를 반영한 업로드용 값이다.
 * [shapeDroneIdRewrites] 는 후보 도형 중 droneId 가 바뀐 것(없앤 경우 null), [shapeTagIdRewrites] 는 tagId 를 없앤 것(값은 항상 null)이다.
 */
internal data class ImportCandidates(
    val shapes: List<ShapeModel>,
    val sketches: List<SketchModel>,
    val drones: List<DroneModel>,
    val tags: List<TagModel>,
    val shapeDroneIdRewrites: Map<String, String?>,
    val shapeTagIdRewrites: Map<String, String?>,
    val droneMerge: DroneSyncMergeResult,
) {
    val count: Int get() = shapes.size + sketches.size + drones.size + tags.size
}

/**
 * 태그도 드론과 같은 기준이다: 후보는 태그 병합의 toUpload(이름이 같아도 id 가 다르면 따로 둔다),
 * 계정에 없을 태그를 가리키는 후보 도형은 tagId 를 null 로 바꾼다.
 * [serverTags] 가 null 이면 서버 태그를 읽지 못한 것(보안 규칙 배포 전 거절)이다. 태그는 후보에서 빼고 tagId 도 그대로 둔다.
 */
internal fun computeImportCandidates(
    localShapes: List<ShapeModel>,
    localSketches: List<SketchModel>,
    localDrones: List<DroneModel>,
    localTags: List<TagModel>,
    serverShapes: List<ShapeModel>,
    serverSketches: List<SketchModel>,
    serverDrones: List<DroneModel>,
    serverTags: List<TagModel>?,
    nowMillis: Long,
): ImportCandidates {
    val droneMerge = mergeDronesForFullSync(
        localDrones = localDrones,
        serverDrones = serverDrones,
        localShapeDroneIds = localShapes.map { it.droneId },
    )
    val droneCandidates = droneMerge.toUpload
    val accountDroneIds = serverDrones.filter { it.deletedAt == null }.map { it.id }.toSet() +
        droneCandidates.filter { it.deletedAt == null }.map { it.id }
    val tagCandidates = serverTags?.let { mergeTags(localTags, it).toUpload }.orEmpty()
    val accountTagIds = serverTags?.let { server ->
        server.filter { !it.isDeleted }.map { it.id }.toSet() + tagCandidates.filter { !it.isDeleted }.map { it.id }
    }

    val reassigned = localShapes.map { shape ->
        val target = shape.droneId?.let(droneMerge.reassignedShapeDroneIds::get)
        if (target == null) shape else shape.copy(droneId = target, updatedAt = nowMillis)
    }
    val serverShapesById = serverShapes.associateBy { it.id }
    val rewrites = linkedMapOf<String, String?>()
    val shapeCandidates = reassigned
        .filter { shape ->
            val server = serverShapesById[shape.id]
            isImportCandidate(shape.updatedAt, shape.deletedAt, server?.updatedAt, server?.deletedAt, server != null)
        }
        .map { shape ->
            val droneId = shape.droneId
            if (droneId != null && droneId !in accountDroneIds) shape.copy(droneId = null, updatedAt = nowMillis) else shape
        }
        .map { shape ->
            val tagId = shape.tagId
            if (tagId != null && accountTagIds != null && tagId !in accountTagIds) {
                shape.copy(tagId = null, updatedAt = nowMillis)
            } else {
                shape
            }
        }
    val originalDroneIds = localShapes.associate { it.id to it.droneId }
    val originalTagIds = localShapes.associate { it.id to it.tagId }
    val tagRewrites = linkedMapOf<String, String?>()
    shapeCandidates.forEach { shape ->
        if (shape.droneId != originalDroneIds[shape.id]) rewrites[shape.id] = shape.droneId
        if (shape.tagId != originalTagIds[shape.id]) tagRewrites[shape.id] = shape.tagId
    }

    val serverSketchesById = serverSketches.associateBy { it.id }
    val sketchCandidates = localSketches.filter { sketch ->
        val server = serverSketchesById[sketch.id]
        isImportCandidate(sketch.updatedAt, sketch.deletedAt, server?.updatedAt, server?.deletedAt, server != null)
    }

    return ImportCandidates(
        shapes = shapeCandidates,
        sketches = sketchCandidates,
        drones = droneCandidates,
        tags = tagCandidates,
        shapeDroneIdRewrites = rewrites,
        shapeTagIdRewrites = tagRewrites,
        droneMerge = droneMerge,
    )
}

/** 서버 태그를 읽지 못해 가져오기 후보를 셀 수 없다([importBlockedByUnreadableTags]). 후보 확인 실패로 처리한다. */
internal class ServerTagsUnreadableException : IllegalStateException("Server tags are unreadable while device tags exist")

/**
 * 서버 태그를 읽지 못했는데(보안 규칙 배포 전 거절, [serverTags] null) 기기에 활성 태그가 있으면 가져오기를 진행하지 않는다.
 * 태그를 빼고 가져오면 기기 정리 때 태그가 사라지므로, 후보 확인 실패로 다시 시도하게 한다.
 */
internal fun importBlockedByUnreadableTags(localTags: List<TagModel>, serverTags: List<TagModel>?): Boolean =
    serverTags == null && localTags.any { !it.isDeleted }

/**
 * 로그아웃 ① 태그 업로드 대상. 서버 태그를 읽었으면 병합의 toUpload 이고, 읽지 못했으면(거절) 올리지 않는 대신
 * 기기의 활성 태그 수를 "저장할 수 없는 항목"으로 센다(로그아웃 뒤 정리에서 사라지므로 먼저 묻는다).
 */
internal data class LogoutTagFlush(val toUpload: List<TagModel>, val unsavableCount: Int)

internal fun logoutTagFlush(localTags: List<TagModel>, serverTags: List<TagModel>?): LogoutTagFlush =
    if (serverTags == null) {
        LogoutTagFlush(toUpload = emptyList(), unsavableCount = localTags.count { !it.isDeleted })
    } else {
        LogoutTagFlush(toUpload = mergeTags(localTags, serverTags).toUpload, unsavableCount = 0)
    }

/**
 * 로그아웃 ① 도형 업로드 대상(D-50 레드팀, iOS `ShapeColorRepair.shapesToUpload`): 서버보다 새롭거나 서버에 없는 기기 도형,
 * 그리고 서버 값과 병합한 뒤 드론 색과 다른 도형(색을 고치고 updatedAt 을 올린 병합 사본).
 * 기기의 옛 사본은 색이 달라도 올리지 않는다(서버 값을 고쳐 올린다). 예전처럼 병합 전 기기 사본에 색을 맞추면
 * updatedAt 만 올라 아직 받지 못한 서버의 새 제목·메모·tagId 를 덮는다.
 */
internal fun logoutShapesToUpload(
    local: List<ShapeModel>,
    server: List<ShapeModel>,
    drones: List<DroneModel>,
    nowMillis: Long,
): List<ShapeModel> {
    val base = logoutFlushItems(local, server, ShapeModel::id, ShapeModel::updatedAt, ShapeModel::deletedAt)
    val fixed = reconcileShapeColorsWithServer(local, server, drones, nowMillis).toUpload.associateBy { it.id }
    val baseIds = base.mapTo(HashSet()) { it.id }
    return base.map { fixed[it.id] ?: it } + fixed.values.filter { it.id !in baseIds }
}

/**
 * 로그아웃 ① 업로드 대상: 서버와 비교해 기기에만 있거나 기기 쪽이 허용 오차를 넘게 새로운 항목(삭제 표시 포함).
 * 기존의 "활성 항목 전부 업로드"와 달리 다른 기기에서 고친 더 새로운 서버 값을 덮어쓰지 않는다.
 * 서버에 없는 기기 삭제 표시는 올리지 않는다(올릴 것이 없다).
 */
internal fun <T> logoutFlushItems(
    local: List<T>,
    server: List<T>,
    idOf: (T) -> String,
    updatedAtOf: (T) -> Long,
    deletedAtOf: (T) -> Long?,
): List<T> {
    val serverById = server.associateBy(idOf)
    return local.filter { item ->
        val remote = serverById[idOf(item)]
        if (remote == null) deletedAtOf(item) == null else isNewerUpdatedAt(updatedAtOf(item), than = updatedAtOf(remote))
    }
}

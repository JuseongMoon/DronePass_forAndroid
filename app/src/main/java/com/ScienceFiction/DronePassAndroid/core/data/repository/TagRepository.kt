package com.ScienceFiction.DronePassAndroid.core.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.room.withTransaction
import com.ScienceFiction.DronePassAndroid.core.account.AccountSession
import com.ScienceFiction.DronePassAndroid.core.account.SyncTicket
import com.ScienceFiction.DronePassAndroid.core.data.local.room.DronePassDatabase
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.TagDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toDomain
import com.ScienceFiction.DronePassAndroid.core.data.local.room.mapper.toEntity
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.TagFirebaseStore
import com.ScienceFiction.DronePassAndroid.core.data.sync.idsChangedLocallyDuringSync
import com.ScienceFiction.DronePassAndroid.domain.model.TagFilter
import com.ScienceFiction.DronePassAndroid.domain.model.TagModel
import com.ScienceFiction.DronePassAndroid.domain.model.activeTags
import com.ScienceFiction.DronePassAndroid.domain.model.mergeTags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

object TagPreferenceKeys {
    /** 숨길 태그 필터 키 (태그 id 또는 [TagFilter.UNTAGGED_KEY]). 로그아웃·계정 전환 때 지운다 */
    val HIDDEN_FILTER_KEYS = stringSetPreferencesKey("hidden_tag_filter_keys")
}

/** 지도·저장 목록이 쓰는 태그 필터 상태 */
data class TagFilterState(
    val activeTags: List<TagModel> = emptyList(),
    val hiddenKeys: Set<String> = emptySet(),
) {
    val activeTagIds: Set<String> = activeTags.mapTo(HashSet()) { it.id }

    /** 태그 필터를 보일 수 있는가 (활성 태그가 하나 이상) */
    val hasFilterableTags: Boolean get() = activeTags.isNotEmpty()

    /** 실제로 거르고 있는가 */
    val isFiltering: Boolean get() = TagFilter.isFiltering(activeTagIds, hiddenKeys)

    fun isKeyVisible(key: String): Boolean = key !in hiddenKeys

    fun isVisible(tagId: String?): Boolean = TagFilter.isVisible(tagId, activeTagIds, hiddenKeys)
}

/**
 * 태그 저장소 (기기 저장 + Firebase 동기화). DroneRepository 를 축소한 구조이며 iOS `TagRepository`·`TagManager` 정합.
 * 무료 한도(QuotaAction.ADD_TAG)는 호출부가 SubscriptionManager 로 먼저 판정한다.
 * 태그 동기화 실패(보안 규칙 배포 전 거절 포함)는 기기 태그를 그대로 두고 도형 동기화에 영향을 주지 않는다.
 */
@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao,
    private val tagFirebaseStore: TagFirebaseStore,
    private val shapeRepository: ShapeRepository,
    private val session: AccountSession,
    private val dataStore: DataStore<Preferences>,
    private val database: DronePassDatabase,
) {
    companion object {
        private const val TAG = "TagRepository"
    }

    /** 삭제 표시 포함 */
    fun getAllTags(): Flow<List<TagModel>> = tagDao.getAllTags().map { entities -> entities.map { it.toDomain() } }

    /** 활성 태그 (이름 가나다순) */
    fun getActiveTags(): Flow<List<TagModel>> {
        val locale = Locale.getDefault()
        return getAllTags().map { it.activeTags(locale) }
    }

    val hiddenFilterKeys: Flow<Set<String>> =
        dataStore.data.map { it[TagPreferenceKeys.HIDDEN_FILTER_KEYS].orEmpty() }

    val filterState: Flow<TagFilterState> =
        combine(getActiveTags(), hiddenFilterKeys) { tags, hidden -> TagFilterState(tags, hidden) }

    suspend fun activeTagCount(): Int = tagDao.getAllTagsOnce().count { it.deletedAt == null }

    /** 같은 이름의 활성 태그가 있는가 (대소문자 무시) */
    suspend fun isDuplicateName(name: String, excludingId: String? = null): Boolean =
        tagDao.getAllTagsOnce().any {
            it.deletedAt == null && it.id != excludingId && it.name.lowercase() == name.lowercase()
        }

    // ===== CRUD =====

    suspend fun addTag(name: String): TagModel {
        val tag = TagModel(name = name)
        saveTag(tag)
        Log.d(TAG, "태그 추가: id=${tag.id}")
        return tag
    }

    suspend fun renameTag(id: String, name: String) {
        val tag = tagDao.getTagById(id)?.toDomain() ?: return
        saveTag(tag.rename(name))
    }

    /** 태그를 지우고 이 태그를 단 활성 도형에서 태그를 뺀다(도형은 그대로). 필터에서 숨긴 키도 지운다 */
    suspend fun deleteTag(id: String) {
        val tag = tagDao.getTagById(id)?.toDomain() ?: return
        saveTag(tag.softDelete())
        val cleared = shapeRepository.clearTagFromShapes(id)
        dataStore.edit { preferences ->
            val hidden = preferences[TagPreferenceKeys.HIDDEN_FILTER_KEYS].orEmpty()
            if (id in hidden) preferences[TagPreferenceKeys.HIDDEN_FILTER_KEYS] = hidden - id
        }
        Log.d(TAG, "태그 삭제: id=$id, 도형 ${cleared}개에서 태그 뺌")
    }

    /** 기기에 먼저 쓰고, 관문이 열려 있으면 서버에도 쓴다. 서버 실패는 다음 동기화가 다시 올린다 */
    private suspend fun saveTag(tag: TagModel) {
        tagDao.insertTag(tag.toEntity())
        val ticket = session.syncTicket() ?: return
        try {
            tagFirebaseStore.saveTag(ticket, tag)
        } catch (e: Exception) {
            Log.w(TAG, "태그 Firebase 저장 실패: id=${tag.id}", e)
        }
    }

    // ===== 필터 =====

    suspend fun toggleFilterKey(key: String) {
        dataStore.edit { preferences ->
            val hidden = preferences[TagPreferenceKeys.HIDDEN_FILTER_KEYS].orEmpty()
            preferences[TagPreferenceKeys.HIDDEN_FILTER_KEYS] = if (key in hidden) hidden - key else hidden + key
        }
    }

    // ===== 동기화 =====

    /**
     * 서버와 병합하고 기기 쪽 승자를 올린다([mergeTags]). 동기화 중 기기에서 바뀐 태그는 이번 반영에서 뺀다.
     * 실패하면 예외를 던지며, 기기 태그는 그대로다. 올린 태그 수를 돌려준다.
     */
    suspend fun performFullSync(ticket: SyncTicket): Int {
        val localEntities = tagDao.getAllTagsOnce()
        val snapshotUpdatedAt = localEntities.associate { it.id to it.updatedAt }
        val server = tagFirebaseStore.loadAllTagsIncludingDeleted(ticket)
        val result = mergeTags(localEntities.map { it.toDomain() }, server)
        val changedIds = session.commit(ticket) {
            database.withTransaction {
                val changed = idsChangedLocallyDuringSync(
                    snapshotUpdatedAt,
                    tagDao.getAllTagsOnce().associate { it.id to it.updatedAt },
                )
                tagDao.insertTags(result.merged.filter { it.id !in changed }.map { it.toEntity() })
                changed
            }
        }
        val toUpload = result.toUpload.filter { it.id !in changedIds }
        if (toUpload.isNotEmpty()) tagFirebaseStore.saveTags(ticket, toUpload)
        Log.d(TAG, "태그 동기화 완료: 서버=${server.size}, 머지=${result.merged.size}, 업로드=${toUpload.size}")
        return toUpload.size
    }

    /** 실시간 리스너가 받은 서버 태그를 반영한다. 기기에만 있는 태그는 유지한다(업로드는 전체 동기화가 맡는다) */
    suspend fun applyServerTags(ticket: SyncTicket, server: List<TagModel>) {
        session.commit(ticket) {
            database.withTransaction {
                val merged = mergeTags(tagDao.getAllTagsOnce().map { it.toDomain() }, server).merged
                tagDao.insertTags(merged.map { it.toEntity() })
            }
        }
    }

    /** 로그아웃·계정 전환: 기기의 태그 기록을 지운다 (필터 키는 계정 데이터 DataStore 키와 함께 지운다) */
    suspend fun deleteAllTagsLocally() {
        tagDao.deleteAllTags()
    }
}

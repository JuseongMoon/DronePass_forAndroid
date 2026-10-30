package com.ScienceFiction.DronePassAndroid.core.data.remote.firebase

import android.util.Log
import com.ScienceFiction.DronePassAndroid.core.account.AccountSession
import com.ScienceFiction.DronePassAndroid.core.account.SyncTicket
import com.ScienceFiction.DronePassAndroid.domain.model.TagModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** iOS 는 태그 문서 전체를 덮어쓴다(`setData(from:)`). 같은 형태로 쓰므로 선택 필드는 값이 있을 때만 넣는다. */
internal fun tagToFirestoreDocumentData(tag: TagModel): Map<String, Any> {
    val data = mutableMapOf<String, Any>(
        "id" to tag.id,
        "name" to tag.name,
        "createdAt" to Timestamp(Date(tag.createdAt)),
        "updatedAt" to Timestamp(Date(tag.updatedAt)),
    )
    tag.deletedAt?.let { data["deletedAt"] = Timestamp(Date(it)) }
    return data
}

internal fun tagFromFirestoreDocument(documentId: String, data: Map<String, Any?>): TagModel? {
    val id = data["id"] as? String ?: return null
    if (id != documentId || runCatching { UUID.fromString(id) }.isFailure) return null
    val name = (data["name"] as? String)?.takeIf { it.isNotBlank() } ?: return null
    val createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: return null
    val updatedAt = (data["updatedAt"] as? Timestamp)?.toDate()?.time ?: return null
    if (hasInvalidFirestoreTimestampField(data, "deletedAt")) return null
    return TagModel(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = (data["deletedAt"] as? Timestamp)?.toDate()?.time,
    )
}

/**
 * Firestore 의 tags 컬렉션과 통신하는 Store. 경로: users/{uid}/tags/{tagId}.
 * 거절(PERMISSION_DENIED)은 관문을 닫지 않는다([AccountSession.remoteIgnoringDenial]). 메타데이터 문서는 쓰지 않고
 * 다른 기기는 컬렉션 리스너로 받는다.
 */
@Singleton
class TagFirebaseStore @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val session: AccountSession,
) {
    companion object {
        private const val TAG = "TagFirebaseStore"
    }

    private fun tagsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("tags")

    /** 삭제 표시까지 모두 로드한다 (병합에서 다른 기기의 삭제가 덮이지 않도록) */
    suspend fun loadAllTagsIncludingDeleted(ticket: SyncTicket, source: Source = Source.DEFAULT): List<TagModel> {
        val snapshot = session.remoteIgnoringDenial(ticket) { uid -> tagsCollection(uid).get(source).await() }
        return snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            tagFromFirestoreDocument(doc.id, data).also {
                if (it == null) Log.w(TAG, "필수 필드 누락/손상으로 태그 스킵: docId=${doc.id}")
            }
        }
    }

    suspend fun saveTag(ticket: SyncTicket, tag: TagModel) {
        session.remoteIgnoringDenial(ticket) { uid ->
            tagsCollection(uid).document(tag.id).set(tagToFirestoreDocumentData(tag)).await()
        }
    }

    suspend fun saveTags(ticket: SyncTicket, tags: List<TagModel>) {
        firestoreWriteChunks(tags).forEach { chunk ->
            session.remoteIgnoringDenial(ticket) { uid ->
                val batch = firestore.batch()
                chunk.forEach { tag -> batch.set(tagsCollection(uid).document(tag.id), tagToFirestoreDocumentData(tag)) }
                batch.commit().await()
            }
        }
    }
}

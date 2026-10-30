package com.ScienceFiction.DronePassAndroid.feature.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.TagRepository
import com.ScienceFiction.DronePassAndroid.core.util.launchCatchingStorageErrors
import com.ScienceFiction.DronePassAndroid.domain.model.TagModel
import com.ScienceFiction.DronePassAndroid.subscription.QuotaAction
import com.ScienceFiction.DronePassAndroid.subscription.SubscriptionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** 태그 이름 한 줄 폭 상한(dp). 저장 목록 오른쪽 위 배지로 쓰므로 드론 이름보다 짧게 둔다 (iOS TagEditView.maxNameWidth) */
internal const val TAG_NAME_MAX_WIDTH_DP = 140f

/** 무료 한도 판정에 쓰는 paywall source (SUBSCRIPTION_CONTRACT §10) */
internal const val TAG_ADD_PAYWALL_SOURCE = "tag_add"

/**
 * 설정 태그 관리와 도형 편집 "새 태그 만들기"가 함께 쓰는 ViewModel. iOS TagManager 의 CRUD 부분 정합.
 */
@HiltViewModel
class TagViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    shapeRepository: ShapeRepository,
    private val subscriptionManager: SubscriptionManager,
) : ViewModel() {

    /** 활성 태그 (이름 가나다순) */
    val activeTags: StateFlow<List<TagModel>> = tagRepository.getActiveTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 태그별 활성 도형 수 */
    val shapeCounts: StateFlow<Map<String, Int>> = shapeRepository.getActiveShapes()
        .map { shapes -> shapes.mapNotNull { it.tagId }.groupingBy { it }.eachCount() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** 같은 이름의 활성 태그가 있는가 (대소문자 무시) */
    fun isDuplicateName(name: String, excludingId: String?): Boolean =
        activeTags.value.any { it.id != excludingId && it.name.lowercase() == name.lowercase() }

    /** 무료 한도: 이름 입력 전에 미리 안내한다 (최종 판정은 저장 시). 막히면 페이월이 뜬다 */
    fun requestAdd(onAllowed: () -> Unit) {
        viewModelScope.launchCatchingStorageErrors {
            if (subscriptionManager.allow(QuotaAction.ADD_TAG, TAG_ADD_PAYWALL_SOURCE)) onAllowed()
        }
    }

    /** 새 태그를 저장한다. 한도에 막히면 저장하지 않고 [onBlocked] (입력 화면에 머문다) */
    fun addTag(name: String, onAdded: (TagModel) -> Unit, onBlocked: () -> Unit = {}) {
        viewModelScope.launchCatchingStorageErrors {
            var created: TagModel? = null
            val allowed = subscriptionManager.withCreationPermit(QuotaAction.ADD_TAG, TAG_ADD_PAYWALL_SOURCE) {
                created = tagRepository.addTag(name)
            }
            val tag = created
            if (allowed && tag != null) onAdded(tag) else onBlocked()
        }
    }

    fun renameTag(id: String, name: String) {
        viewModelScope.launchCatchingStorageErrors { tagRepository.renameTag(id, name) }
    }

    fun deleteTag(id: String) {
        viewModelScope.launchCatchingStorageErrors { tagRepository.deleteTag(id) }
    }
}

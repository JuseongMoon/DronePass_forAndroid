package com.ScienceFiction.DronePassAndroid.feature.tag

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ScienceFiction.DronePassAndroid.R
import com.ScienceFiction.DronePassAndroid.domain.model.TagModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TalkBack 은 초점을 둔 노드의 동작만 메뉴에 보여 준다. 태그 행의 '삭제'(D-71 ⑤)가 TalkBack 초점 노드,
 * 곧 이름·도형 수를 합쳐 읽는 클릭 노드에 붙어 있어야 한다. 바깥 스와이프 상자에 두면 메뉴에 나오지 않는다.
 */
@RunWith(AndroidJUnit4::class)
class TagRowAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    private val deleteLabel = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.tag_list_delete)

    private fun hasDeleteAction() = SemanticsMatcher("custom action '$deleteLabel'") { node ->
        node.config.getOrNull(SemanticsActions.CustomActions).orEmpty().any { it.label == deleteLabel }
    }

    @Test
    fun deleteActionSitsOnTheNodeTalkBackFocuses() {
        var clicks = 0
        var deleteRequests = 0
        compose.setContent {
            TagRow(
                tag = TagModel(id = "t1", name = "현장"),
                shapeCount = 2,
                onClick = { clicks += 1 },
                onDeleteRequest = { deleteRequests += 1 },
            )
        }

        // 합친 트리에서 이름을 찾으면 TalkBack 이 초점을 두는 클릭 노드가 나온다
        val row = compose.onNodeWithText("현장")
        row.assert(hasClickAction()).assert(hasDeleteAction())

        // 다른 노드에 같은 동작이 따로 있으면 TalkBack 에서 보이지 않는 중복이 된다
        compose.onAllNodes(hasDeleteAction(), useUnmergedTree = true).assertCountEquals(1)
        compose.onAllNodesWithText("현장").assertCountEquals(1)

        val action = row.fetchSemanticsNode().config[SemanticsActions.CustomActions].single { it.label == deleteLabel }
        compose.runOnIdle { action.action() }
        compose.runOnIdle {
            assertEquals(1, deleteRequests)
            assertEquals(0, clicks)
        }
    }
}

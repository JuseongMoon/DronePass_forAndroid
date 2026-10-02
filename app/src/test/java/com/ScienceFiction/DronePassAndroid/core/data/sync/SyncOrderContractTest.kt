package com.ScienceFiction.DronePassAndroid.core.data.sync

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 도형은 드론 id 를 가리키므로 로그인·실시간 동기화 모두 드론을 먼저 올린다(iOS 로그인 동기화와 같은 순서).
 * 도형이 먼저 올라가면 드론 동기화가 실패했을 때 서버에 드론 없는 도형이 남는다.
 */
class SyncOrderContractTest {

    @Test
    fun `login full sync uploads drones before shapes`() {
        // 로그인 뒤 첫 전체 동기화는 기기 데이터 주인 판단(AccountSessionFlows)이 관문을 연 뒤에 한다.
        assertDroneSyncBeforeShapeSync(
            "src/main/java/com/ScienceFiction/DronePassAndroid/core/account/AccountSessionFlows.kt",
            fromToken = "private suspend fun runInitialSync(",
        )
    }

    @Test
    fun `realtime full sync uploads drones before shapes`() {
        assertDroneSyncBeforeShapeSync(
            "src/main/java/com/ScienceFiction/DronePassAndroid/core/data/sync/RealtimeSyncManager.kt",
        )
    }

    private fun assertDroneSyncBeforeShapeSync(path: String, fromToken: String? = null) {
        val fullSource = resolveProjectFile(path, "app/$path").readText()
        val source = fromToken?.let { fullSource.substring(fullSource.indexOf(it)) } ?: fullSource
        val droneIndex = source.indexOf("droneRepository.performFullSync(ticket)")
        val shapeIndex = source.indexOf("shapeRepository.performFullSync(ticket)")
        assertTrue("drone sync missing in $path", droneIndex >= 0)
        assertTrue("shape sync missing in $path", shapeIndex >= 0)
        assertTrue("drone sync must run before shape sync in $path", droneIndex < shapeIndex)
    }

    private fun resolveProjectFile(vararg candidates: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return candidates
            .map { File(userDir, it) }
            .firstOrNull { it.exists() }
            ?: error("Project file not found: ${candidates.joinToString()}")
    }
}

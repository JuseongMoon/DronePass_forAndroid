package com.ScienceFiction.DronePassAndroid.core.data.remote.vworld

import com.google.firebase.functions.FirebaseFunctions
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * 원격 V-World 키의 출처. 키가 만료되면 앱을 다시 내지 않고 출처의 값만 바꿔 키를 교체한다.
 *
 * 받지 못했으면(오프라인, 시간 초과, App Check 실패, 함수 오류) 예외를 던진다. 호출자는 저장된 키를 유지한다.
 */
fun interface VWorldKeyConfigSource {
    suspend fun fetch(): VWorldKeyConfigResult
}

internal const val VWorldKeyCallable = "getVWorldApiKey"
private const val VWorldKeyCallableTimeoutSeconds = 15L

/**
 * App Check 로 보호한 callable `getVWorldApiKey`(asia-northeast3). 로그인 없이 빈 객체로 부른다.
 * 응답 `{schemaVersion: 1, apiKey: "<UUID>"}` 에 원격 키 계약 v1 규칙을 적용한다.
 */
class VWorldKeyCallableSource(
    private val functions: FirebaseFunctions,
) : VWorldKeyConfigSource {
    override suspend fun fetch(): VWorldKeyConfigResult {
        val result = functions.getHttpsCallable(VWorldKeyCallable)
            .apply { setTimeout(VWorldKeyCallableTimeoutSeconds, TimeUnit.SECONDS) }
            .call(emptyMap<String, Any>())
            .await()
        return interpretVWorldKeyConfig(result.data as? Map<*, *>)
    }
}

/** 원격 설정을 읽은 결과. 저장된 원격 키를 어떻게 다룰지 정한다. */
sealed interface VWorldKeyConfigResult {
    /** 형식이 맞는 키가 있다. 저장한다. */
    data class Key(val apiKey: String) : VWorldKeyConfigResult

    /** schemaVersion 1 응답인데 apiKey 가 없거나 형식이 틀렸다. 저장된 원격 키를 지운다. */
    data object MissingKey : VWorldKeyConfigResult

    /** 다른 schemaVersion 이거나 객체가 아니다. 응답 전체를 무시하고 저장된 키를 유지한다. */
    data object Ignored : VWorldKeyConfigResult
}

internal const val VWorldKeyConfigSchemaVersion = 1

private val VWorldKeyPattern =
    Regex("^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$")

/** V-World 인증키 형식(UUID 8-4-4-4-12)인지 본다. 앞뒤 공백도 형식 오류다. */
internal fun isWellFormedVWorldKey(value: String?): Boolean =
    value != null && VWorldKeyPattern.matches(value)

/** 계약 v1 을 응답 객체에 적용한다. 모르는 필드는 무시한다. */
internal fun interpretVWorldKeyConfig(root: Map<*, *>?): VWorldKeyConfigResult {
    if (root == null) return VWorldKeyConfigResult.Ignored
    val schemaVersion = root["schemaVersion"] as? Number
    if (schemaVersion == null || schemaVersion.toDouble() != VWorldKeyConfigSchemaVersion.toDouble()) {
        return VWorldKeyConfigResult.Ignored
    }
    val apiKey = root["apiKey"] as? String
    return if (apiKey != null && isWellFormedVWorldKey(apiKey)) {
        VWorldKeyConfigResult.Key(apiKey)
    } else {
        VWorldKeyConfigResult.MissingKey
    }
}

/** JSON 본문(공유 fixture 의 응답 사례)에 계약 v1 을 적용한다. */
internal fun parseVWorldKeyConfig(body: String, moshi: Moshi): VWorldKeyConfigResult {
    val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    val root = runCatching { moshi.adapter<Map<String, Any?>>(type).fromJson(body) }.getOrNull()
    return interpretVWorldKeyConfig(root)
}

private val FeatureCollectionTypePattern =
    Regex("\"type\"\\s*:\\s*\"FeatureCollection\"")

/**
 * 정상 GeoJSON FeatureCollection 응답인지 본다. V-World 는 인증·서비스 오류를 HTTP 200 으로
 * XML `<ServiceException>`, JSON 오류 객체, 점검 HTML 등으로 돌려준다.
 */
internal fun isVWorldFeatureCollectionBody(body: String): Boolean =
    body.trimStart().startsWith("{") && FeatureCollectionTypePattern.containsMatchIn(body)

private val ErrorCodePatterns = listOf(
    // XML 속성: <ServiceException code="EXPIRE_KEY">. exceptionCode 같은 다른 이름은 제외한다.
    Regex("(?<![A-Za-z0-9_:-])code\\s*=\\s*\"([^\"]*)\""),
    // XML 요소: <code>INCORRECT_KEY</code>
    Regex("<code>\\s*([^<]*?)\\s*</code>"),
    // JSON: "code":"INVALID_KEY"
    Regex("\"code\"\\s*:\\s*\"([^\"]*)\""),
)

/**
 * 키 오류인지 판정한다. 정상 FeatureCollection 이 아닌 응답에서만 본다.
 * code 속성, `<code>` 요소, JSON "code" 값에 KEY 가 들어 있거나(EXPIRE_KEY, INVALID_KEY 등)
 * 본문에 '인증키'가 있으면 키 오류다.
 */
internal fun isVWorldKeyErrorBody(body: String): Boolean {
    if (isVWorldFeatureCollectionBody(body)) return false
    val codes = ErrorCodePatterns.flatMap { pattern -> pattern.findAll(body).map { it.groupValues[1] } }
    return codes.any { it.contains("KEY") } || body.contains("인증키")
}

package com.ScienceFiction.DronePassAndroid.core.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET

/**
 * VWorld 공공기관 연락처 데이터를 S3 에서 다운로드한다.
 *
 * iOS `VWorldContactManager` 와 동일한 텍스트 파일을 공유한다:
 * - 한 줄 = 기관명, 다음 줄 = 전화번호
 * - 빈 줄과 `#` 접두 라인은 주석으로 무시
 */
interface VWorldContactsApi {
    @GET("dronepass/vworld-contacts.txt")
    suspend fun fetchContacts(): ResponseBody
}

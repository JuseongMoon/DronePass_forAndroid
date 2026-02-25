package com.ScienceFiction.DronePassAndroid.core.data.remote

import com.ScienceFiction.DronePassAndroid.core.data.remote.model.GeocodingResponse
import com.ScienceFiction.DronePassAndroid.core.data.remote.model.ReverseGeocodingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NaverGeocodingApi {

    /**
     * 주소 -> 좌표 (Geocoding)
     */
    @GET("map-geocode/v2/geocode")
    suspend fun geocode(
        @Query("query") address: String
    ): GeocodingResponse

    /**
     * 좌표 -> 주소 (Reverse Geocoding)
     * @param coords "longitude,latitude" 형식 (네이버 API 규격: 경도,위도 순서)
     */
    @GET("map-reversegeocode/v2/gc")
    suspend fun reverseGeocode(
        @Query("coords") coords: String,
        @Query("orders") orders: String = "roadaddr,addr",
        @Query("output") output: String = "json"
    ): ReverseGeocodingResponse
}

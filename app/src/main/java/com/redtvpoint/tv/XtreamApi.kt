package com.redtvpoint.tv

import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamApi {
    @GET("player_api.php") suspend fun seriesInfo(@Query("username")u:String,@Query("password")p:String,@Query("series_id")id:Int,@Query("action")a:String="get_series_info"):SeriesInfo
    @GET("player_api.php") suspend fun auth(@Query("username") u:String,@Query("password") p:String): AuthResponse
    @GET("player_api.php") suspend fun liveCategories(@Query("username")u:String,@Query("password")p:String,@Query("action")a:String="get_live_categories"):List<Category>
    @GET("player_api.php") suspend fun live(@Query("username")u:String,@Query("password")p:String,@Query("action")a:String="get_live_streams"):List<LiveStream>
    @GET("player_api.php") suspend fun vodCategories(@Query("username")u:String,@Query("password")p:String,@Query("action")a:String="get_vod_categories"):List<Category>
    @GET("player_api.php") suspend fun vod(@Query("username")u:String,@Query("password")p:String,@Query("action")a:String="get_vod_streams"):List<VodStream>
    @GET("player_api.php") suspend fun seriesCategories(@Query("username")u:String,@Query("password")p:String,@Query("action")a:String="get_series_categories"):List<Category>
    @GET("player_api.php") suspend fun series(@Query("username")u:String,@Query("password")p:String,@Query("action")a:String="get_series"):List<SeriesItem>
}

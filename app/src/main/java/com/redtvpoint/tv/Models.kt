package com.redtvpoint.tv

data class Category(val category_id:String="", val category_name:String="")
data class LiveStream(val stream_id:Int=0, val name:String="", val stream_icon:String?=null, val category_id:String?=null)
data class VodStream(val stream_id:Int=0, val name:String="", val stream_icon:String?=null, val category_id:String?=null, val container_extension:String?="mp4", val rating:String?=null)
data class SeriesItem(val series_id:Int=0, val name:String="", val cover:String?=null, val category_id:String?=null, val rating:String?=null)
data class AuthResponse(val user_info:UserInfo?=null)
data class UserInfo(val auth:Int=0, val status:String?=null, val exp_date:String?=null)
data class Credentials(val server:String, val username:String, val password:String)
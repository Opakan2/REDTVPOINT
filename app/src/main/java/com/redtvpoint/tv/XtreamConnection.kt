package com.redtvpoint.tv

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonParseException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.ConnectException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

object XtreamConnection {
    /** Bare Xtream host:port entries use HTTP, except an explicit port 443. */
    fun normalizeServer(value:String):String {
        val input=value.trim()
        require(input.isNotEmpty() && input.none { it.isWhitespace() })
        val candidate=if("://" in input) input else {
            val authority=input.substringBefore('/').substringBefore('?').substringBefore('#')
            (if(authority.endsWith(":443")) "https://" else "http://")+input
        }
        val url=requireNotNull(candidate.toHttpUrlOrNull())
        require(url.username.isEmpty() && url.password.isEmpty())
        val segments=url.encodedPathSegments.toMutableList()
        while(segments.lastOrNull()=="") segments.removeAt(segments.lastIndex)
        if(segments.lastOrNull()?.lowercase() in listOf("player_api.php","get.php","xmltv.php")) segments.removeAt(segments.lastIndex)
        return url.newBuilder().encodedPath("/"+segments.joinToString("/")+if(segments.isEmpty()) "" else "/")
            .query(null).fragment(null).build().toString()
    }

    val gson=GsonBuilder().registerTypeAdapter(UserInfo::class.java,JsonDeserializer<UserInfo> { json, _, _ ->
        if(!json.isJsonObject) throw JsonParseException("Invalid user_info")
        val obj=json.asJsonObject
        fun field(name:String)=obj.get(name)?.takeIf { it.isJsonPrimitive }?.asString
        val auth=when(field("auth")?.trim()?.lowercase()) { "1","true" -> 1; else -> 0 }
        UserInfo(auth,field("status"),field("exp_date"))
    }).create()

    val client=OkHttpClient.Builder()
        .connectTimeout(30,TimeUnit.SECONDS).readTimeout(60,TimeUnit.SECONDS)
        .callTimeout(90,TimeUnit.SECONDS)
        .addInterceptor { chain -> chain.proceed(chain.request().newBuilder()
            .header("Accept","application/json")
            .header("User-Agent","REDTVPOINT/0.3.1 (Android TV)").build()) }
        .build()

    fun api(server:String,httpClient:OkHttpClient=client):XtreamApi=Retrofit.Builder().baseUrl(normalizeServer(server))
        .client(httpClient).addConverterFactory(GsonConverterFactory.create(gson)).build().create(XtreamApi::class.java)

    fun loginError(error:Exception):String=when(error) {
        is IllegalArgumentException -> "Dirección inválida. Usa dominio:puerto o http(s)://dominio:puerto."
        is UnknownHostException -> "No se encuentra el servidor. Revisa el dominio y la conexión a Internet."
        is SocketTimeoutException -> "El servidor tardó demasiado en responder. Vuelve a intentar."
        is ConnectException -> "No se puede conectar al servidor. Revisa el puerto y el protocolo HTTP/HTTPS."
        is SSLException -> "Falló la conexión HTTPS. Comprueba que el protocolo y el puerto coincidan con tu proveedor."
        is HttpException -> when(error.code()) {
            401 -> "El servidor rechazó el acceso (HTTP 401). Revisa las credenciales."
            403 -> "El servidor bloqueó la solicitud (HTTP 403). Consulta al proveedor."
            404 -> "No se encuentra player_api.php (HTTP 404). Revisa la dirección del servidor."
            else -> "El servidor devolvió un error HTTP ${error.code()}."
        }
        is JsonParseException -> "El servidor no devolvió una respuesta Xtream válida. Revisa la dirección."
        else -> "No se pudo completar la conexión. Comprueba Internet y vuelve a intentar."
    }
}

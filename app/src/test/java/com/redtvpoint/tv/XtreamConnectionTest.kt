package com.redtvpoint.tv

import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import okhttp3.Protocol
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

class XtreamConnectionTest {
    @Test fun loginUsesCorrectEndpointAndEncodesCredentials() = runBlocking {
        var requested=false
        val client=XtreamConnection.client.newBuilder().addInterceptor { chain ->
            val request=chain.request()
            assertEquals("/player_api.php",request.url.encodedPath)
            assertEquals(8080,request.url.port)
            assertEquals("name+test",request.url.queryParameter("username"))
            assertEquals("p&ss?# word",request.url.queryParameter("password"))
            requested=true
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("{\"user_info\":{\"auth\":\"1\",\"status\":\"Active\"}}".toResponseBody("application/json".toMediaType())).build()
        }.build()
        val response=XtreamConnection.api("example.com:8080",client).auth("name+test","p&ss?# word")
        assertTrue(requested)
        assertEquals(1,response.user_info!!.auth)
    }
    @Test fun acceptsBareHostAndPort() {
        assertEquals("http://example.com:8080/",XtreamConnection.normalizeServer(" example.com:8080 "))
        assertEquals("https://example.com/",XtreamConnection.normalizeServer("example.com:443"))
        assertEquals("https://example.com:8443/",XtreamConnection.normalizeServer("https://example.com:8443"))
    }
    @Test fun normalizesApiAndPlaylistWithoutDuplicatingPaths() {
        assertEquals("http://example.com:8080/",XtreamConnection.normalizeServer("example.com:8080/get.php?username=test&password=test&type=m3u_plus"))
        assertEquals("https://example.com/panel/",XtreamConnection.normalizeServer("https://example.com/panel/player_api.php"))
        assertEquals("https://example.com/panel/",XtreamConnection.normalizeServer("https://example.com/panel/"))
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsInvalidAddress() { XtreamConnection.normalizeServer("ftp://example.com") }
    @Test fun acceptsExplicitPositiveAuthFormatsOnly() {
        for(value in listOf("1","\"1\"","true","\"true\"")) {
            val response=XtreamConnection.gson.fromJson("{\"user_info\":{\"auth\":$value}}",AuthResponse::class.java)
            assertEquals(1,response.user_info!!.auth)
        }
        for(value in listOf("0","\"0\"","false","null","\"unknown\"")) {
            val response=XtreamConnection.gson.fromJson("{\"user_info\":{\"auth\":$value}}",AuthResponse::class.java)
            assertEquals(0,response.user_info!!.auth)
        }
        assertEquals(0,XtreamConnection.gson.fromJson("{\"status\":\"Active\"}",UserInfo::class.java).auth)
    }
}

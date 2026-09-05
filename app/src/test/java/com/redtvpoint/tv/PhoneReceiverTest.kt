package com.redtvpoint.tv

import org.junit.Test
import org.junit.Assert.*
import java.net.Socket
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PhoneReceiverTest {
    private fun request(server:PhoneReceiver, method:String, path:String, body:ByteArray=byteArrayOf(), code:String=server.code, extra:String="", declared:Long=body.size.toLong()):String {
        return Socket("127.0.0.1",server.port).use { socket ->
            socket.soTimeout=5000
            socket.getOutputStream().apply {
                write(("$method $path HTTP/1.1\r\nHost: 127.0.0.1:${server.port}\r\nX-Pair-Code: $code\r\nContent-Length: $declared\r\n$extra\r\n").toByteArray())
                write(body); flush()
            }
            socket.getInputStream().bufferedReader().readText()
        }
    }
    @Test fun rejectsWrongCodeAndCrossOrigin() {
        val folder=Files.createTempDirectory("receiver-test").toFile()
        val server=PhoneReceiver(folder,"page",{}, { fail("Must not play") })
        try {
            server.start()
            assertTrue(request(server,"POST","/link", "https://example.com/a.mp4".toByteArray(), "wrong").startsWith("HTTP/1.1 403"))
            assertTrue(request(server,"POST","/link", "https://example.com/a.mp4".toByteArray(),extra="Origin: https://unrelated.example\r\n").startsWith("HTTP/1.1 403"))
        } finally { server.close(); folder.deleteRecursively() }
    }
    @Test fun acceptsLinkAndServesPage() {
        val folder=Files.createTempDirectory("receiver-test").toFile()
        val received=CountDownLatch(1)
        var result=""
        val server=PhoneReceiver(folder,"<h1>Send</h1>",{}, { result=it; received.countDown() })
        try {
            server.start()
            assertTrue(request(server,"GET","/").contains("<h1>Send</h1>"))
            assertTrue(request(server,"POST","/link","https://example.com/video.mp4".toByteArray()).startsWith("HTTP/1.1 200"))
            assertTrue(received.await(2,TimeUnit.SECONDS))
            assertEquals("https://example.com/video.mp4",result)
        } finally { server.close(); folder.deleteRecursively() }
    }
    @Test fun rejectsOversizeAndNonHttpLink() {
        val folder=Files.createTempDirectory("receiver-test").toFile()
        val server=PhoneReceiver(folder,"page",{}, {})
        try {
            server.start()
            assertTrue(request(server,"POST","/video",declared=501L*1024*1024).startsWith("HTTP/1.1 413"))
            assertTrue(request(server,"POST","/link","file:///private/file".toByteArray()).startsWith("HTTP/1.1 400"))
        } finally { server.close(); folder.deleteRecursively() }
    }
    @Test fun storesExactBytesWithoutServingFiles() {
        val folder=Files.createTempDirectory("receiver-test").toFile()
        val received=CountDownLatch(1)
        var result=""
        val server=PhoneReceiver(folder,"page",{}, { result=it; received.countDown() })
        try {
            server.start()
            val bytes=ByteArray(200000) { (it%251).toByte() }
            assertTrue(request(server,"POST","/video",bytes).startsWith("HTTP/1.1 200"))
            assertTrue(received.await(2,TimeUnit.SECONDS))
            assertArrayEquals(bytes,java.io.File(java.net.URI(result)).readBytes())
            assertTrue(request(server,"GET","/phone-video.mp4").startsWith("HTTP/1.1 404"))
        } finally { server.close(); folder.deleteRecursively() }
    }
}

package com.redtvpoint.tv

import java.io.BufferedInputStream
import java.io.File
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.security.SecureRandom
import java.util.Collections
import kotlin.concurrent.thread

/** Foreground-only, single-transfer local receiver. Never serves uploaded files. */
class PhoneReceiver(private val directory:File, private val page:String,
    private val status:(String)->Unit, private val ready:(String)->Unit):AutoCloseable {
    val code = (100000 + SecureRandom().nextInt(900000)).toString()
    private val server=ServerSocket(0)
    val port:Int get()=server.localPort
    @Volatile private var running=true
    @Volatile private var client:Socket?=null
    private var failures=0

    fun addresses():List<String> = Collections.list(NetworkInterface.getNetworkInterfaces())
        .filter { it.isUp && !it.isLoopback }
        .flatMap { Collections.list(it.inetAddresses) }
        .filter { it is Inet4Address && it.isSiteLocalAddress }
        .map { "http://${it.hostAddress}:$port" }

    fun start() {
        directory.mkdirs()
        directory.listFiles()?.forEach { if(it.isFile) it.delete() }
        thread(name="REDTVPOINT-receiver",isDaemon=true) {
            while(running) {
                try {
                    val socket=server.accept()
                    client=socket
                    socket.use { handle(it) }
                } catch(e:Exception) { if(running) status("Envío interrumpido. Vuelve a intentarlo desde Safari.") }
                finally { client=null }
            }
        }
    }

    private fun line(input:BufferedInputStream):String {
        val bytes=java.io.ByteArrayOutputStream()
        while(bytes.size()<8192) {
            val b=input.read()
            require(b>=0) { "Incomplete request" }
            if(b==10) return bytes.toString("US-ASCII").trimEnd('\r')
            bytes.write(b)
        }
        error("Header too long")
    }
    private fun reply(out:OutputStream, code:Int, text:String, html:Boolean=false) {
        val bytes=text.toByteArray(Charsets.UTF_8)
        val headers="HTTP/1.1 $code Result\r\nContent-Type: ${if(html) "text/html" else "text/plain"}; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY\r\n\r\n"
        out.write(headers.toByteArray(Charsets.US_ASCII)); out.write(bytes); out.flush()
    }
    private fun handle(socket:Socket) {
        socket.soTimeout=15000
        val input=BufferedInputStream(socket.getInputStream())
        val output=socket.getOutputStream()
        val request=line(input).split(' ')
        if(request.size!=3) { reply(output,400,"Solicitud inválida"); return }
        val headers=mutableMapOf<String,String>()
        var total=0
        while(true) {
            val row=line(input)
            total+=row.length
            require(total<=16384)
            if(row.isEmpty()) break
            val split=row.indexOf(':'); require(split>0)
            val key=row.substring(0,split).lowercase()
            require(key !in headers)
            headers[key]=row.substring(split+1).trim()
        }
        if(request[0]=="GET" && request[1]=="/") { reply(output,200,page,true); return }
        if(request[0]!="POST" || request[1] !in listOf("/video","/link")) { reply(output,404,"No encontrado"); return }
        if(failures>=10) { reply(output,429,"Reinicia la recepción en la TV."); return }
        if(headers["x-pair-code"]!=code) { failures++; reply(output,403,"Código incorrecto. Mira el código de la TV."); return }
        // No CORS: other web origins cannot submit the required custom header.
        val origin=headers["origin"]
        if(origin!=null && origin!="http://${headers["host"]}") { reply(output,403,"Origen no permitido"); return }
        val size=headers["content-length"]?.toLongOrNull()
        val maximum=if(request[1]=="/link") 8192L else 500L*1024*1024
        if(size==null || size<=0 || size>maximum || headers.containsKey("transfer-encoding")) { reply(output,413,"Vídeo demasiado grande (máximo 500 MB) o envío no compatible."); return }
        if(request[1]=="/link") {
            val bytes=ByteArray(size.toInt())
            var offset=0
            while(offset<bytes.size) { val n=input.read(bytes,offset,bytes.size-offset); require(n>0); offset+=n }
            val url=bytes.toString(Charsets.UTF_8).trim()
            val uri=try { URI(url) } catch(e:Exception) { null }
            if(uri==null || uri.scheme !in listOf("http","https") || uri.host.isNullOrBlank() || uri.userInfo!=null) { reply(output,400,"Introduce un enlace directo http:// o https:// válido."); return }
            reply(output,200,"Enlace enviado. Comprueba la TV.")
            ready(url)
            return
        }
        if(directory.usableSpace<size+32L*1024*1024) { reply(output,507,"No hay espacio suficiente en el Fire Stick."); return }
        val file=File(directory,"phone-video.mp4")
        var complete=false
        try {
            socket.soTimeout=60000
            status("Recibiendo vídeo del iPhone…")
            file.outputStream().use { destination ->
                val buffer=ByteArray(64*1024)
                var remaining=size
                while(remaining>0) { val n=input.read(buffer,0,minOf(buffer.size.toLong(),remaining).toInt()); require(n>0); destination.write(buffer,0,n); remaining-=n }
            }
            complete=true
            reply(output,200,"Vídeo recibido. Comenzando en la TV.")
            ready(file.toURI().toString())
        } finally { if(!complete) file.delete() }
    }
    override fun close() { running=false; runCatching { server.close() }; runCatching { client?.close() } }
}

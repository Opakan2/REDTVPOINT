package com.redtvpoint.tv

import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlin.concurrent.thread

@Composable fun PhoneReceiverScreen(back:()->Unit, play:(String)->Unit) {
    BackHandler(onBack=back)
    val context=LocalContext.current
    var enabled by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Conecta el iPhone y el Fire Stick a la misma Wi-Fi.") }
    val currentPlay by rememberUpdatedState(play)
    if(enabled) DisposableEffect(Unit) {
        val handler=Handler(Looper.getMainLooper())
        val lock=Any()
        var active=true
        var receiver:PhoneReceiver?=null
        val lifecycle=(context as? androidx.activity.ComponentActivity)?.lifecycle
        val observer=androidx.lifecycle.LifecycleEventObserver { _, event ->
            if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                synchronized(lock) { active=false; receiver?.close() }
                enabled=false
            }
        }
        lifecycle?.addObserver(observer)
        fun update(action:()->Unit) { handler.post { synchronized(lock) { if(active) action() } } }
        thread(name="REDTVPOINT-start-receiver",isDaemon=true) {
            try {
                val page=context.assets.open("phone.html").bufferedReader().use { it.readText() }
                synchronized(lock) {
                    if(active) {
                        val instance=PhoneReceiver(File(context.cacheDir,"phone-receiver"),page,
                            { text -> update { message=text } }, { url -> update { currentPlay(url) } })
                        receiver=instance
                        val urls=instance.addresses()
                        if(urls.isEmpty()) { instance.close(); update { enabled=false; message="No se encontró una dirección Wi-Fi local. Comprueba la conexión." } }
                        else { instance.start(); update { address=urls.joinToString("\n"); code=instance.code; message="Abre esta dirección en Safari e introduce el código. Si hay varias direcciones, usa la de tu Wi-Fi." } }
                    }
                }
            } catch(e:Exception) { update { enabled=false; message="No se pudo activar la recepción. Comprueba la red y vuelve a intentarlo." } }
        }
        onDispose { lifecycle?.removeObserver(observer); synchronized(lock) { active=false; receiver?.close() } }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF08090D)).verticalScroll(rememberScrollState()).padding(32.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Desde iPhone",color=Color.White,fontSize=30.sp)
        Text(message,color=Color.LightGray)
        if(enabled && code.isNotEmpty()) {
            Text(address,color=Color.White,fontSize=24.sp)
            Text("Código: $code",color=Color(0xFFFF3344),fontSize=32.sp)
        }
        Button(onClick={enabled=!enabled; address=""; code=""},modifier=Modifier.remoteFocus()) { Text(if(enabled) "Detener recepción" else "Activar recepción") }
        Text("Safari permite seleccionar un vídeo de Fotos/Archivos o pegar un enlace directo. Máximo 500 MB; el vídeo se copia primero al Fire Stick. Mantén Safari abierto hasta que termine.",color=Color.LightGray)
        Text("La recepción se detiene al salir o comenzar la reproducción. Vuelve a activarla para enviar otro vídeo. No es AirPlay ni duplicación de pantalla. La compatibilidad de MOV/HEVC depende del Fire Stick; MP4 H.264 suele ser la opción más compatible.",color=Color.LightGray)
        Button(onClick=back,modifier=Modifier.remoteFocus()) { Text("Volver") }
    }
}

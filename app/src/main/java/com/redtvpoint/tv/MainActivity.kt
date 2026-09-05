package com.redtvpoint.tv

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import android.app.AlertDialog
import android.widget.EditText
import android.text.InputType
import android.view.WindowManager
import android.net.Uri
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val Bg=Color(0xFF050608); private val Panel=Color(0xFF111318); private val Red=Color(0xFFE50914)

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { RedTvPoint() } }
}

@Composable fun RedTvPoint(){
    var creds by remember { mutableStateOf<Credentials?>(null) }
    var section by remember { mutableStateOf("Inicio") }
    var playUrl by remember { mutableStateOf<String?>(null) }
    var receiving by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme=darkColorScheme(primary=Red,background=Bg,surface=Panel)) {
        if(playUrl!=null) Player(playUrl!!){playUrl=null}
        else if(receiving) PhoneReceiverScreen({receiving=false},{playUrl=it})
        else if(creds==null) Login(onReceive={receiving=true}) { creds=it }
        else Dashboard(creds!!,section,{if(it=="Desde iPhone") receiving=true else section=it},{playUrl=it}) { creds=null; section="Inicio" }
    }
}

@Composable fun Login(onReceive:()->Unit, onLogin:(Credentials)->Unit){
    var server by remember{mutableStateOf("")}; var user by remember{mutableStateOf("")}; var pass by remember{mutableStateOf("")}
    var msg by remember{mutableStateOf("")}; val scope= rememberCoroutineScope()
    var connecting by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    val usernameFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val connectFocus = remember { FocusRequester() }
    val receiveFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocus() }
    Box(Modifier.fillMaxSize().background(Bg), contentAlignment=Alignment.Center){
        Column(Modifier.width(560.dp).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("▶ REDTVPOINT", color=Red,fontSize=32.sp,fontWeight=FontWeight.Black)
            Text("Elige un campo con ↑ ↓ y pulsa OK para editar.",color=Color.LightGray)
            RemoteField("Servidor / URL", server, modifier=Modifier.focusRequester(first).focusProperties { up=FocusRequester.Cancel; down=usernameFocus }, enabled=!connecting) { server=it }
            RemoteField("Usuario", user, modifier=Modifier.focusRequester(usernameFocus).focusProperties { up=first; down=passwordFocus }, enabled=!connecting) { user=it }
            RemoteField("Contraseña", pass, secret=true, modifier=Modifier.focusRequester(passwordFocus).focusProperties { up=usernameFocus; down=connectFocus }, enabled=!connecting) { pass=it }
            Button(onClick={
                scope.launch {
                    if (connecting) return@launch
                    if(server.isBlank() || user.isBlank() || pass.isEmpty()) { msg="Completa servidor, usuario y contraseña"; return@launch }
                    connecting=true; msg=""
                    try {
                        val base=server.trim().trimEnd('/')+"/"
                        require(Uri.parse(base).scheme in listOf("http", "https") && !Uri.parse(base).host.isNullOrBlank())
                        val api=Retrofit.Builder().baseUrl(base).addConverterFactory(GsonConverterFactory.create()).build().create(XtreamApi::class.java)
                        val r=api.auth(user.trim(),pass)
                        if(r.user_info?.auth==1) onLogin(Credentials(base,user.trim(),pass)) else msg="Credenciales no válidas"
                    } catch(e:CancellationException){ throw e }
                    catch(e:Exception){ msg="No se pudo conectar. Revisa la URL (http:// o https://), la conexión y las credenciales." }
                    finally { connecting=false }
                }
            },enabled=!connecting,modifier=Modifier.focusRequester(connectFocus).focusProperties { up=passwordFocus; down=receiveFocus }.fillMaxWidth().remoteFocus()){Text(if(connecting) "CONECTANDO…" else "CONECTAR")}
            Button(onClick=onReceive,modifier=Modifier.focusRequester(receiveFocus).focusProperties { up=connectFocus; down=FocusRequester.Cancel }.fillMaxWidth().remoteFocus()) { Text("Enviar vídeo desde iPhone") }
            if(msg.isNotBlank()) Text(msg,color=Color(0xFFFF8080))
            Text("REDTVPOINT no incluye contenido. Usa únicamente servicios que estés autorizado a utilizar.",color=Color.Gray,fontSize=12.sp)
        }
    }
}

// Only the selected field opens an editor. D-pad navigation never enters an IME.
@Composable fun RemoteField(label:String, value:String, secret:Boolean=false, modifier:Modifier=Modifier, enabled:Boolean=true, change:(String)->Unit) {
    val context=LocalContext.current
    var editing by remember { mutableStateOf(false) }
    val fieldFocus=remember { FocusRequester() }
    Button(onClick={editing=true}, enabled=enabled, modifier=modifier.focusRequester(fieldFocus).fillMaxWidth().remoteFocus(), colors=ButtonDefaults.buttonColors(containerColor=Panel)) {
        Text("$label: ${if(value.isEmpty()) "Pulsa OK" else if(secret) "••••••••" else value}", modifier=Modifier.fillMaxWidth(), maxLines=1)
    }
    if(editing) DisposableEffect(Unit) {
        val input=EditText(context).apply {
            setSingleLine(true)
            inputType=if(secret) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
            setText(value); setSelection(text.length)
        }
        val dialog=AlertDialog.Builder(context).setTitle(label).setView(input)
            .setPositiveButton("Guardar") { _, _ -> change(input.text.toString()) }
            .setNegativeButton("Cancelar", null).create()
        dialog.setOnDismissListener { editing=false; fieldFocus.requestFocus() }
        dialog.setOnShowListener { input.requestFocus(); dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE) }
        dialog.show()
        onDispose { dialog.setOnDismissListener(null); dialog.dismiss() }
    }
}

@Composable fun Modifier.remoteFocus():Modifier {
    var focused by remember { mutableStateOf(false) }
    return this.onFocusChanged { focused=it.hasFocus }.border(if(focused) 3.dp else 0.dp, if(focused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
}

@Composable fun Dashboard(c:Credentials, section:String, nav:(String)->Unit, play:(String)->Unit, logout:()->Unit){
    Row(Modifier.fillMaxSize().background(Bg)){
        Column(Modifier.width(210.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("REDTVPOINT",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Black,modifier=Modifier.padding(bottom=22.dp))
            listOf("Inicio","En Vivo","Películas","Series","TV Guide","Favoritos","Buscar","Desde iPhone","Ajustes").forEach {
                Button(onClick={nav(it)},colors=ButtonDefaults.buttonColors(containerColor=if(it==section) Red else Panel),modifier=Modifier.fillMaxWidth().remoteFocus()){Text(it)}
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight().padding(24.dp)){ Content(c,section,play,logout) }
    }
}

@Composable fun Content(c:Credentials, section:String, play:(String)->Unit, logout:()->Unit){
    val scope=rememberCoroutineScope()
    val context=LocalContext.current
    val prefs=remember { context.getSharedPreferences("favorites", android.content.Context.MODE_PRIVATE) }
    val account=remember(c) { java.security.MessageDigest.getInstance("SHA-256").digest((c.server+"|"+c.username).toByteArray()).joinToString("") { "%02x".format(it) } }
    var favorites by remember(account) { mutableStateOf(prefs.getStringSet(account, emptySet())!!.toSet()) }
    var query by remember { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<Triple<String,String,String>?>(null) }
    var episodes by remember { mutableStateOf<List<Episode>?>(null) }
    val api=remember(c) { Retrofit.Builder().baseUrl(c.server).addConverterFactory(GsonConverterFactory.create()).build().create(XtreamApi::class.java) }
    fun stream(kind:String,id:String,extension:String)= "${c.server.trimEnd('/')}/$kind/${Uri.encode(c.username)}/${Uri.encode(c.password)}/${Uri.encode(id)}.${Uri.encode(extension)}"
    var live by remember{mutableStateOf<List<LiveStream>>(emptyList())}
    var vod by remember{mutableStateOf<List<VodStream>>(emptyList())}
    var series by remember{mutableStateOf<List<SeriesItem>>(emptyList())}
    var loading by remember{mutableStateOf(false)}
    var err by remember{mutableStateOf("")}
    LaunchedEffect(c,refresh){
        loading=true; err=""
        try {
            // A missing catalog must not prevent the other catalogs from loading.
            suspend fun fetch(block:suspend ()->Unit) { try { block() } catch(e:CancellationException) { throw e } catch(e:Exception) { err="Parte del catálogo no se pudo cargar. Pulsa Actualizar para reintentar." } }
            fetch { live=api.live(c.username,c.password) }
            fetch { vod=api.vod(c.username,c.password) }
            fetch { series=api.series(c.username,c.password) }
        } finally {loading=false}
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text(section,color=Color.White,fontSize=34.sp,fontWeight=FontWeight.Bold)
        if(section=="Ajustes") {
            Text("REDTVPOINT 0.3.0 · ${Uri.parse(c.server).host.orEmpty()}",color=Color.LightGray)
            Text("Las credenciales solo se mantienen durante esta sesión.",color=Color.LightGray)
            Button(onClick=logout,modifier=Modifier.remoteFocus()) { Text("Cerrar sesión") }
        }
        if(section=="Buscar") RemoteField("Buscar en el catálogo",query) { query=it }
        Button(onClick={refresh++},enabled=!loading,modifier=Modifier.remoteFocus()) { Text(if(loading) "Cargando…" else "Actualizar catálogo") }
        if(section=="Inicio"){
            Box(Modifier.fillMaxWidth().height(190.dp).background(Color(0xFF26070A),RoundedCornerShape(18.dp)).padding(28.dp)){
                Column{Text("TV EN VIVO",color=Color.White,fontSize=40.sp,fontWeight=FontWeight.Black);Text("Todo tu contenido, organizado en una sola experiencia.",color=Color.LightGray)}
            }
        }
        if(loading) CircularProgressIndicator()
        if(err.isNotBlank()) Text(err,color=Color(0xFFFF8080))
        if(section in listOf("Inicio","En Vivo","Buscar","Favoritos")) MediaRow("En Vivo Ahora",live.filter { (section!="Buscar" || it.name.contains(query,true)) && (section!="Favoritos" || "live:${it.stream_id}" in favorites) },{it.name}, {it.stream_icon}) {
            selected=Triple("live:${it.stream_id}",it.name,stream("live",it.stream_id.toString(),"m3u8"))
        }
        if(section in listOf("Inicio","Películas","Buscar","Favoritos")) MediaRow("Películas",vod.filter { (section!="Buscar" || it.name.contains(query,true)) && (section!="Favoritos" || "vod:${it.stream_id}" in favorites) },{it.name},{it.stream_icon}) {
            selected=Triple("vod:${it.stream_id}",it.name,stream("movie",it.stream_id.toString(),it.container_extension ?: "mp4"))
        }
        if(section in listOf("Inicio","Series","Buscar")) MediaRow("Series",series.filter { section!="Buscar" || it.name.contains(query,true) },{it.name},{it.cover}) { item ->
            scope.launch {
                try { episodes=api.seriesInfo(c.username,c.password,item.series_id).episodes.orEmpty().entries.sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }.flatMap { entry -> entry.value.map { it.copy(title="T${entry.key} · ${it.title}") } } }
                catch(e:CancellationException) { throw e }
                catch(e:Exception) { err="No se pudieron cargar los episodios." }
            }
        }
        if(section=="TV Guide") Text("La guía EPG todavía no está disponible en esta versión.",color=Color.LightGray)
        if(section=="Favoritos") Text("Abre un canal o película y selecciona Añadir a favoritos.",color=Color.LightGray)
    }
    selected?.let { item ->
        androidx.compose.material3.AlertDialog(onDismissRequest={selected=null},title={Text(item.second)},
            confirmButton={Button(onClick={selected=null; play(item.third)},modifier=Modifier.remoteFocus()) { Text("Reproducir") }},
            dismissButton={Button(onClick={favorites=if(item.first in favorites) favorites-item.first else favorites+item.first; prefs.edit().putStringSet(account,favorites).apply(); selected=null},modifier=Modifier.remoteFocus()) { Text(if(item.first in favorites) "Quitar favorito" else "Añadir a favoritos") }})
    }
    episodes?.let { list ->
        androidx.compose.material3.AlertDialog(onDismissRequest={episodes=null},title={Text("Episodios")},text={
            androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max=280.dp)) {
                if(list.isEmpty()) item { Text("El proveedor no devolvió episodios.") }
                items(list) { episode -> Button(onClick={episodes=null; play(stream("series",episode.id,episode.container_extension ?: "mp4"))},modifier=Modifier.fillMaxWidth().remoteFocus()) { Text(episode.title) } }
            }
        },confirmButton={Button(onClick={episodes=null}) { Text("Cerrar") }})
    }
}

@Composable fun <T> MediaRow(title:String,data:List<T>,name:(T)->String,image:(T)->String?,click:(T)->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text(title,color=Color.White,fontSize=20.sp,fontWeight=FontWeight.SemiBold)
        if(data.isEmpty()) Text("Sin resultados",color=Color.Gray)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            items(data){ item ->
                Card(onClick={click(item)},modifier=Modifier.width(155.dp).height(115.dp).remoteFocus()){
                    Box{
                        AsyncImage(model=image(item),contentDescription=name(item),modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                        Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Color(0xB0000000)).padding(8.dp)){Text(name(item),color=Color.White,maxLines=2)}
                    }
                }
            }
        }
    }
}

@Composable fun Player(url:String,onBack:()->Unit){
    BackHandler(onBack=onBack)
    val context=LocalContext.current
    val player=remember(url){ExoPlayer.Builder(context).build().apply{setMediaItem(MediaItem.fromUri(url));prepare();playWhenReady=true}}
    var playbackError by remember(url) { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener=object: androidx.media3.common.Player.Listener {
            override fun onPlayerError(error:androidx.media3.common.PlaybackException) { playbackError=true }
        }
        player.addListener(listener)
        val lifecycle=(context as? ComponentActivity)?.lifecycle
        val observer=androidx.lifecycle.LifecycleEventObserver { _, event -> if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP) player.pause() }
        lifecycle?.addObserver(observer)
        onDispose { player.removeListener(listener); lifecycle?.removeObserver(observer) }
    }
    DisposableEffect(player){onDispose{
        player.release()
        if(Uri.parse(url).scheme=="file") runCatching {
            val temporary=java.io.File(java.net.URI(url))
            if(temporary.canonicalFile.parentFile==java.io.File(context.cacheDir,"phone-receiver").canonicalFile) temporary.delete()
        }
    }}
    Box(Modifier.fillMaxSize().background(Color.Black)){
        AndroidView(factory={PlayerView(it).apply{this.player=player;useController=true;layoutParams=FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)}},modifier=Modifier.fillMaxSize())
        Button(onClick=onBack,modifier=Modifier.padding(20.dp).align(Alignment.TopStart)){Text("← Volver")}
        if(playbackError) Column(Modifier.align(Alignment.Center).background(Panel).padding(24.dp)) {
            Text("No se pudo reproducir este contenido.",color=Color.White)
            Button(onClick={playbackError=false; player.prepare(); player.play()},modifier=Modifier.remoteFocus()) { Text("Reintentar") }
        }
    }
}

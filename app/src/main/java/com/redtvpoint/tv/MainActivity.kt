package com.redtvpoint.tv

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
    MaterialTheme(colorScheme=darkColorScheme(primary=Red,background=Bg,surface=Panel)) {
        if(creds==null) Login { creds=it }
        else if(playUrl!=null) Player(playUrl!!){playUrl=null}
        else Dashboard(creds!!,section,{section=it},{playUrl=it})
    }
}

@Composable fun Login(onLogin:(Credentials)->Unit){
    var server by remember{mutableStateOf("")}; var user by remember{mutableStateOf("")}; var pass by remember{mutableStateOf("")}
    var msg by remember{mutableStateOf("")}; val scope= rememberCoroutineScope()
    Box(Modifier.fillMaxSize().background(Bg), contentAlignment=Alignment.Center){
        Column(Modifier.width(560.dp), verticalArrangement=Arrangement.spacedBy(16.dp)){
            Text("REDTVPOINT", color=Color.White,fontSize=48.sp,fontWeight=FontWeight.Black)
            Text("YOUR ENTERTAINMENT. ONE PLACE.",color=Color.LightGray)
            OutlinedTextField(server,{server=it},label={Text("Servidor / URL")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(user,{user=it},label={Text("Usuario")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(pass,{pass=it},label={Text("Contraseña")},modifier=Modifier.fillMaxWidth())
            Button(onClick={
                scope.launch {
                    try {
                        val base=server.trim().trimEnd('/')+"/"
                        val api=Retrofit.Builder().baseUrl(base).addConverterFactory(GsonConverterFactory.create()).build().create(XtreamApi::class.java)
                        val r=api.auth(user,pass)
                        if(r.user_info?.auth==1) onLogin(Credentials(base,user,pass)) else msg="Credenciales no válidas"
                    } catch(e:Exception){ msg="No se pudo conectar: ${e.message ?: "error"}" }
                }
            },modifier=Modifier.fillMaxWidth()){Text("CONECTAR")}
            if(msg.isNotBlank()) Text(msg,color=Color(0xFFFF8080))
            Text("REDTVPOINT no incluye contenido. Usa únicamente servicios que estés autorizado a utilizar.",color=Color.Gray,fontSize=12.sp)
        }
    }
}

@Composable fun Dashboard(c:Credentials, section:String, nav:(String)->Unit, play:(String)->Unit){
    Row(Modifier.fillMaxSize().background(Bg)){
        Column(Modifier.width(210.dp).fillMaxHeight().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("REDTVPOINT",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Black,modifier=Modifier.padding(bottom=22.dp))
            listOf("Inicio","En Vivo","Películas","Series","TV Guide","Favoritos","Buscar","Ajustes").forEach {
                Button(onClick={nav(it)},colors=ButtonDefaults.buttonColors(containerColor=if(it==section) Red else Panel),modifier=Modifier.fillMaxWidth()){Text(it)}
            }
        }
        Box(Modifier.weight(1f).fillMaxHeight().padding(24.dp)){ Content(c,section,play) }
    }
}

@Composable fun Content(c:Credentials, section:String, play:(String)->Unit){
    val scope=rememberCoroutineScope()
    var live by remember{mutableStateOf<List<LiveStream>>(emptyList())}
    var vod by remember{mutableStateOf<List<VodStream>>(emptyList())}
    var series by remember{mutableStateOf<List<SeriesItem>>(emptyList())}
    var loading by remember{mutableStateOf(false)}
    var err by remember{mutableStateOf("")}
    LaunchedEffect(section){
        loading=true; err=""
        try {
            val api=Retrofit.Builder().baseUrl(c.server).addConverterFactory(GsonConverterFactory.create()).build().create(XtreamApi::class.java)
            when(section){
                "Inicio" -> { live=api.live(c.username,c.password).take(12); vod=api.vod(c.username,c.password).take(20); series=api.series(c.username,c.password).take(20) }
                "En Vivo" -> live=api.live(c.username,c.password)
                "Películas" -> vod=api.vod(c.username,c.password)
                "Series" -> series=api.series(c.username,c.password)
            }
        } catch(e:Exception){err=e.message?:"Error"} finally {loading=false}
    }
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text(section,color=Color.White,fontSize=34.sp,fontWeight=FontWeight.Bold)
        if(section=="Inicio"){
            Box(Modifier.fillMaxWidth().height(190.dp).background(Color(0xFF26070A),RoundedCornerShape(18.dp)).padding(28.dp)){
                Column{Text("TV EN VIVO",color=Color.White,fontSize=40.sp,fontWeight=FontWeight.Black);Text("Todo tu contenido, organizado en una sola experiencia.",color=Color.LightGray)}
            }
        }
        if(loading) CircularProgressIndicator()
        if(err.isNotBlank()) Text(err,color=Color(0xFFFF8080))
        if(section=="Inicio" || section=="En Vivo") MediaRow("En Vivo Ahora",live,{it.name}, {it.stream_icon}) {
            play("${c.server.trimEnd('/')}/live/${c.username}/${c.password}/${it.stream_id}.m3u8")
        }
        if(section=="Inicio" || section=="Películas") MediaRow("Películas",vod,{it.name},{it.stream_icon}) {
            play("${c.server.trimEnd('/')}/movie/${c.username}/${c.password}/${it.stream_id}.${it.container_extension ?: "mp4"}")
        }
        if(section=="Inicio" || section=="Series") MediaRow("Series",series,{it.name},{it.cover}) { }
        if(section in listOf("TV Guide","Favoritos","Buscar","Ajustes")) Text("Módulo preparado para la siguiente build.",color=Color.LightGray)
    }
}

@Composable fun <T> MediaRow(title:String,data:List<T>,name:(T)->String,image:(T)->String?,click:(T)->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text(title,color=Color.White,fontSize=20.sp,fontWeight=FontWeight.SemiBold)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            items(data.take(50)){ item ->
                Card(onClick={click(item)},modifier=Modifier.width(155.dp).height(115.dp)){
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
    val context=LocalContext.current
    val player=remember(url){ExoPlayer.Builder(context).build().apply{setMediaItem(MediaItem.fromUri(url));prepare();playWhenReady=true}}
    DisposableEffect(player){onDispose{player.release()}}
    Box(Modifier.fillMaxSize().background(Color.Black)){
        AndroidView(factory={PlayerView(it).apply{this.player=player;useController=true;layoutParams=FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)}},modifier=Modifier.fillMaxSize())
        Button(onClick=onBack,modifier=Modifier.padding(20.dp).align(Alignment.TopStart)){Text("← Volver")}
    }
}
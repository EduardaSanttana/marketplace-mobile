package edu.ifsp.marketplace.poc.geofencing

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val ID_GEOFENCE = "ponto-de-entrega"

private fun concedida(context: Context, permissao: String) =
    ContextCompat.checkSelfPermission(context, permissao) == PackageManager.PERMISSION_GRANTED

private fun intentPendente(context: Context): PendingIntent =
    PendingIntent.getBroadcast(
        context, 0, Intent(context, GeofenceReceiver::class.java),
        // O Play Services escreve o evento no intent: precisa ser mutável
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )

@SuppressLint("MissingPermission")
private suspend fun ativarPerimetro(context: Context, lat: Double, lng: Double, raio: Float): String {
    val geofence = Geofence.Builder()
        .setRequestId(ID_GEOFENCE)
        .setCircularRegion(lat, lng, raio)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
        .build()
    val request = GeofencingRequest.Builder()
        // Já dentro do perímetro ao ativar: dispara a entrada na hora
        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .addGeofence(geofence)
        .build()
    return try {
        LocationServices.getGeofencingClient(context).addGeofences(request, intentPendente(context)).await()
        "Perímetro ativo: raio de ${raio.toInt()} m em ($lat, $lng)."
    } catch (e: ApiException) {
        // 1000 = localização desligada; 1004 = falta "Permitir o tempo todo"
        "Falhou: " + GeofenceStatusCodes.getStatusCodeString(e.statusCode)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Tela() } }
    }
}

@Composable
private fun Tela() {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    var latitude by rememberSaveable { mutableStateOf("-23.55052") }
    var longitude by rememberSaveable { mutableStateOf("-46.633308") }
    var raio by rememberSaveable { mutableStateOf("500") }
    var status by rememberSaveable { mutableStateOf("Conceda as permissões e ative o perímetro.") }
    var historico by remember { mutableStateOf(Historico.ler(context)) }

    var temLocal by remember { mutableStateOf(concedida(context, Manifest.permission.ACCESS_FINE_LOCATION)) }
    var temSegundoPlano by remember { mutableStateOf(concedida(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) }
    var temNotificacao by remember { mutableStateOf(concedida(context, Manifest.permission.POST_NOTIFICATIONS)) }

    // O histórico é escrito pelo receiver: relê enquanto a tela está visível
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            temSegundoPlano = concedida(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            while (true) {
                historico = Historico.ler(context)
                delay(1_000)
            }
        }
    }

    val pedirLocal = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        temLocal = concedida(context, Manifest.permission.ACCESS_FINE_LOCATION)
    }
    // Só pode ser pedida depois da localização em primeiro plano; o Android 11+ abre as configurações
    val pedirSegundoPlano = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        temSegundoPlano = it
    }
    val pedirNotificacao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        temNotificacao = it
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("PoC 7 · Geofencing", style = MaterialTheme.typography.headlineSmall)
            Text("Bruno Possar · Marketplace · 18/11/2026")

            Text("1. Permissões", style = MaterialTheme.typography.titleMedium)
            Button(enabled = !temLocal, onClick = {
                pedirLocal.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }) { Text(if (temLocal) "Localização: ok" else "Permitir localização") }
            Button(enabled = temLocal && !temSegundoPlano, onClick = {
                pedirSegundoPlano.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }) { Text(if (temSegundoPlano) "Segundo plano: ok" else "Permitir o tempo todo") }
            Button(enabled = !temNotificacao, onClick = {
                pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text(if (temNotificacao) "Notificações: ok" else "Permitir notificações") }

            Text("2. Ponto de entrega", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(latitude, { latitude = it }, label = { Text("Latitude") })
            OutlinedTextField(longitude, { longitude = it }, label = { Text("Longitude") })
            OutlinedTextField(raio, { raio = it }, label = { Text("Raio (m)") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = temSegundoPlano, onClick = {
                    val lat = latitude.replace(',', '.').toDoubleOrNull()
                    val lng = longitude.replace(',', '.').toDoubleOrNull()
                    val r = raio.replace(',', '.').toFloatOrNull()
                    if (lat == null || lng == null || r == null || lat !in -90.0..90.0 || lng !in -180.0..180.0 || r < 100f) {
                        status = "Dados inválidos. O raio mínimo recomendado é 100 m."
                    } else escopo.launch { status = ativarPerimetro(context, lat, lng, r) }
                }) { Text("Ativar perímetro") }
                OutlinedButton(onClick = {
                    LocationServices.getGeofencingClient(context).removeGeofences(listOf(ID_GEOFENCE))
                    status = "Perímetro removido."
                }) { Text("Remover") }
            }
            Text(status)

            Text("3. Eventos", style = MaterialTheme.typography.titleMedium)
            Text(historico.ifBlank { "Nenhum evento ainda. Mova a localização do emulador para dentro do raio." })
            TextButton(onClick = { Historico.limpar(context); historico = "" }) { Text("Limpar histórico") }
        }
    }
}

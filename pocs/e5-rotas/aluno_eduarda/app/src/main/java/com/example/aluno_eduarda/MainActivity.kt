package com.example.aluno_eduarda

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.aluno_eduarda.ui.theme.PocRotasTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * PoC — Geracao de rotas (Entrega 6 / Semana 13)
 * Aluna: Eduarda Santana
 *
 * Objetivo: provar que o app consegue, a partir da localizacao atual do
 * dispositivo, tracar uma rota ate um ponto escolhido no mapa.
 *
 * O que a PoC exercita:
 *   1. Pedir permissao de localizacao em tempo de execucao;
 *   2. Capturar a posicao atual do aparelho (FusedLocationProviderClient);
 *   3. Deixar o usuario escolher o destino tocando no mapa;
 *   4. Chamar a API de rotas do OSRM;
 *   5. Decodificar a polyline devolvida e desenhar o traçado no mapa;
 *   6. Exibir distancia e tempo estimado.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocRotasTheme {
                TelaRotas()
            }
        }
    }
}

/** Posicao usada enquanto a localizacao real nao chega (centro de Sao Paulo). */
private val POSICAO_PADRAO = LatLng(-23.5505, -46.6333)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaRotas() {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()

    var origem by remember { mutableStateOf<LatLng?>(null) }
    var destino by remember { mutableStateOf<LatLng?>(null) }
    var rota by remember { mutableStateOf<Rota?>(null) }
    var calculando by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("Toque no mapa para escolher o destino.") }
    var temPermissao by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(POSICAO_PADRAO, 12f)
    }

    // Captura a posicao atual do aparelho e leva o mapa ate ela
    suspend fun atualizarOrigem() {
        val cliente = LocationServices.getFusedLocationProviderClient(context)
        val posicao = suspendCancellableCoroutine { continuacao ->
            cliente.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { local ->
                continuacao.resume(local?.let { LatLng(it.latitude, it.longitude) })
            }.addOnFailureListener {
                continuacao.resume(null)
            }
        }

        if (posicao == null) {
            mensagem = "Nao consegui obter a localizacao. Verifique se o GPS esta ligado."
            return
        }
        origem = posicao
        cameraState.animate(CameraUpdateFactory.newLatLngZoom(posicao, 15f))
        mensagem = "Toque no mapa para escolher o destino."
    }

    val pedirPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        temPermissao = concedida
        if (concedida) escopo.launch { atualizarOrigem() }
        else mensagem = "Sem permissao de localizacao nao da para tracar a rota."
    }

    LaunchedEffect(Unit) {
        if (temPermissao) atualizarOrigem()
        else pedirPermissao.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun gerarRota() {
        val partida = origem
        val chegada = destino
        if (partida == null || chegada == null) return

        escopo.launch {
            calculando = true
            mensagem = "Calculando a rota..."
            val resultado = RotaService.calcularRota(partida, chegada)
            calculando = false

            if (resultado == null) {
                rota = null
                mensagem = "Nao foi possivel calcular a rota. Verifique a conexao."
                return@launch
            }

            rota = resultado
            mensagem = "Rota de ${resultado.distanciaFormatada()}, cerca de ${resultado.duracaoFormatada()}."

            // Enquadra a rota inteira na tela
            val limites = LatLngBounds.builder()
                .apply { resultado.pontos.forEach { include(it) } }
                .build()
            cameraState.animate(CameraUpdateFactory.newLatLngBounds(limites, 120))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PoC — Geracao de rotas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            Text(
                text = mensagem,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraState,
                    properties = MapProperties(isMyLocationEnabled = temPermissao),
                    onMapClick = { ponto ->
                        destino = ponto
                        rota = null
                        mensagem = "Destino marcado. Toque em Gerar rota."
                    }
                ) {
                    origem?.let { ponto ->
                        Marker(
                            state = rememberMarkerState(position = ponto),
                            title = "Origem",
                            snippet = "Sua localizacao atual",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_AZURE
                            )
                        )
                    }

                    destino?.let { ponto ->
                        Marker(
                            state = rememberMarkerState(position = ponto),
                            title = "Destino",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_RED
                            )
                        )
                    }

                    // O traçado devolvido pelo OSRM
                    rota?.let { tracado ->
                        Polyline(
                            points = tracado.pontos,
                            color = Color(0xFF1650B0),
                            width = 14f
                        )
                    }
                }

                if (calculando) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            rota?.let { tracado ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column {
                        Text("Distancia", style = MaterialTheme.typography.labelSmall)
                        Text(
                            tracado.distanciaFormatada(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Column {
                        Text("Tempo estimado", style = MaterialTheme.typography.labelSmall)
                        Text(
                            tracado.duracaoFormatada(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Column {
                        Text("Pontos no traçado", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "${tracado.pontos.size}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { escopo.launch { atualizarOrigem() } },
                    enabled = temPermissao && !calculando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Minha localizacao")
                }
                Button(
                    onClick = { gerarRota() },
                    enabled = origem != null && destino != null && !calculando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Gerar rota")
                }
            }
        }
    }
}

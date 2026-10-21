package edu.ifsp.marketplace.ui.entregador

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import edu.ifsp.marketplace.data.Pedido
import edu.ifsp.marketplace.util.Rota
import edu.ifsp.marketplace.util.RotaService
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private val POSICAO_PADRAO = LatLng(-23.5505, -46.6333)

/**
 * Tela de rota do entregador.
 *
 * Traça o caminho da posição atual do aparelho até o ponto de entrega do
 * pedido. Veio da PoC `pocs/e5-rotas/aluno_eduarda`: a chamada ao serviço de
 * rotas e a decodificação da polyline estão em [RotaService].
 */
@SuppressLint("MissingPermission")
@Composable
fun RotaEntregaScreen(
    pedido: Pedido,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()

    val destino = remember(pedido.id) {
        val lat = pedido.latitudeEntrega
        val lng = pedido.longitudeEntrega
        if (lat != null && lng != null) LatLng(lat, lng) else null
    }

    var origem by remember { mutableStateOf<LatLng?>(null) }
    var rota by remember { mutableStateOf<Rota?>(null) }
    var calculando by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("Obtendo sua localização...") }
    var temPermissao by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(destino ?: POSICAO_PADRAO, 13f)
    }

    suspend fun capturarOrigem(): LatLng? {
        val cliente = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { continuacao ->
            cliente.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { local ->
                continuacao.resume(local?.let { LatLng(it.latitude, it.longitude) })
            }.addOnFailureListener { continuacao.resume(null) }
        }
    }

    fun gerarRota() {
        val chegada = destino
        if (chegada == null) {
            mensagem = "Este pedido ainda não tem ponto de entrega no mapa."
            return
        }
        escopo.launch {
            calculando = true
            mensagem = "Calculando a rota..."

            val partida = origem ?: capturarOrigem()
            if (partida == null) {
                calculando = false
                mensagem = "Não consegui obter sua localização. Verifique o GPS."
                return@launch
            }
            origem = partida

            val resultado = RotaService.calcularRota(partida, chegada)
            calculando = false

            if (resultado == null) {
                rota = null
                mensagem = "Não foi possível calcular a rota. Verifique a conexão."
                return@launch
            }

            rota = resultado
            mensagem = "${resultado.distanciaFormatada()} até o destino, cerca de ${resultado.duracaoFormatada()}."

            val limites = LatLngBounds.builder()
                .apply { resultado.pontos.forEach { include(it) } }
                .build()
            cameraState.animate(CameraUpdateFactory.newLatLngBounds(limites, 120))
        }
    }

    val pedirPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        temPermissao = concedida
        if (concedida) gerarRota()
        else mensagem = "Sem permissão de localização não dá para gerar a rota."
    }

    LaunchedEffect(pedido.id) {
        if (temPermissao) gerarRota()
        else pedirPermissao.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = "Entrega de ${pedido.produtoNome}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Para ${pedido.compradorNome}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = pedido.enderecoEntrega.ifBlank { "Endereço não informado" },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = mensagem,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                properties = MapProperties(isMyLocationEnabled = temPermissao)
            ) {
                origem?.let { ponto ->
                    Marker(
                        state = rememberMarkerState(position = ponto),
                        title = "Você está aqui",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE
                        )
                    )
                }
                destino?.let { ponto ->
                    Marker(
                        state = rememberMarkerState(position = ponto),
                        title = "Ponto de entrega",
                        snippet = pedido.enderecoEntrega.ifBlank { null },
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED
                        )
                    )
                }
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
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Column {
                    Text("Distância", style = MaterialTheme.typography.labelSmall)
                    Text(tracado.distanciaFormatada(), style = MaterialTheme.typography.titleMedium)
                }
                Column {
                    Text("Tempo estimado", style = MaterialTheme.typography.labelSmall)
                    Text(tracado.duracaoFormatada(), style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onVoltar, modifier = Modifier.weight(1f)) {
                Text("Voltar")
            }
            Button(
                onClick = { gerarRota() },
                enabled = !calculando && destino != null,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (rota == null) "Gerar rota" else "Recalcular")
            }
        }
    }
}

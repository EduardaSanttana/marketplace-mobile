package edu.ifsp.marketplace.ui.mapa

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import edu.ifsp.marketplace.data.Pedido
import edu.ifsp.marketplace.data.StatusPedido
import edu.ifsp.marketplace.data.sync.RastreioEntregador
import edu.ifsp.marketplace.util.distanciaEmMetros
import kotlinx.coroutines.flow.flowOf

/** Tempo que o marcador leva para deslizar até a nova posição recebida. */
private const val DESLIZE_MS = 1_800

/**
 * Mostra no mapa o destino de um pedido e, em transporte, o entregador se
 * aproximando ao vivo (ver [RastreioEntregador]).
 *
 * O destino é o do pedido, e não o endereço atual do comprador: um pedido em
 * transporte mantém o ponto mesmo que o comprador marque outro na aba Entrega.
 */
@Composable
fun MapaPedidoDialog(pedido: Pedido, onFechar: () -> Unit) {
    val lat = pedido.latitudeEntrega ?: return
    val lng = pedido.longitudeEntrega ?: return
    val destino = LatLng(lat, lng)

    // Só um pedido em transporte tem entregador a caminho
    val idNuvem = pedido.firestoreId?.takeIf { pedido.situacao == StatusPedido.EM_TRANSPORTE }
    val rastreio by remember(idNuvem) {
        idNuvem?.let { RastreioEntregador.acompanhar(it) } ?: flowOf(null)
    }.collectAsState(initial = null)

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(destino, 16f)
    }
    val marcadorEntregador = rememberMarkerState(position = destino)
    var entregadorVisivel by remember { mutableStateOf(false) }

    // Cada posição recebida faz o marcador deslizar da anterior até ela,
    // em vez de pular: dá a impressão de movimento contínuo
    LaunchedEffect(rastreio?.posicao) {
        val nova = rastreio?.posicao ?: return@LaunchedEffect
        if (!entregadorVisivel) {
            marcadorEntregador.position = nova
            entregadorVisivel = true
        } else {
            val de = marcadorEntregador.position
            val progresso = Animatable(0f)
            progresso.animateTo(1f, tween(DESLIZE_MS, easing = LinearEasing)) {
                marcadorEntregador.position = LatLng(
                    de.latitude + (nova.latitude - de.latitude) * value,
                    de.longitude + (nova.longitude - de.longitude) * value
                )
            }
        }
        // Enquadra entregador e destino; perto do ponto, foca no destino
        val distancia = distanciaEmMetros(lat, lng, nova.latitude, nova.longitude)
        val camera = if (distancia < 80) {
            CameraUpdateFactory.newLatLngZoom(destino, 17f)
        } else {
            CameraUpdateFactory.newLatLngBounds(
                LatLngBounds.builder().include(destino).include(nova).build(), 160
            )
        }
        runCatching { cameraState.animate(camera, 800) }
    }

    Dialog(
        onDismissRequest = onFechar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Fechar fica no topo: na base, a barra de navegação cobre o botão
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Entrega de ${pedido.produtoNome}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onFechar) {
                        Text("Fechar")
                    }
                }
                Text(
                    pedido.enderecoEntrega.ifBlank { "Endereço pendente" },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp, 4.dp)
                )
                SituacaoEntregador(pedido, rastreio?.posicao, destino)

                GoogleMap(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    cameraPositionState = cameraState
                ) {
                    Marker(
                        state = rememberMarkerState(position = destino),
                        title = "Ponto de entrega",
                        snippet = pedido.enderecoEntrega
                    )
                    if (idNuvem != null) {
                        // O perímetro que dispara o aviso de "entregador próximo"
                        Circle(
                            center = destino,
                            radius = Pedido.RAIO_ENTREGADOR_PROXIMO_METROS.toDouble(),
                            strokeColor = Color(0x881650B0),
                            strokeWidth = 3f,
                            fillColor = Color(0x151650B0)
                        )
                    }
                    if (entregadorVisivel) {
                        Polyline(
                            points = listOf(marcadorEntregador.position, destino),
                            color = Color(0xFF1650B0),
                            width = 8f,
                            pattern = listOf(Dash(30f), Gap(20f))
                        )
                        Marker(
                            state = marcadorEntregador,
                            title = "Entregador",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            zIndex = 1f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SituacaoEntregador(pedido: Pedido, entregador: LatLng?, destino: LatLng) {
    val texto = when {
        pedido.situacao == StatusPedido.ENTREGUE -> "Pedido entregue."
        pedido.situacao == StatusPedido.AGUARDANDO -> "Aguardando o entregador retirar o pacote."
        entregador == null -> "Aguardando a localização do entregador..."
        else -> {
            val metros = distanciaEmMetros(
                destino.latitude, destino.longitude, entregador.latitude, entregador.longitude
            )
            when {
                metros < 50 -> "O entregador chegou ao ponto de entrega!"
                metros < 1_000 -> "Entregador a ${metros.toInt()} m do ponto de entrega"
                else -> "Entregador a %.1f km do ponto de entrega".format(metros / 1_000)
            }
        }
    }
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    )
}

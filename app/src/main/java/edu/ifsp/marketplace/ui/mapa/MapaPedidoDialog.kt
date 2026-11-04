package edu.ifsp.marketplace.ui.mapa

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import edu.ifsp.marketplace.data.Pedido

/**
 * Mostra no mapa o destino de um pedido, só para consulta.
 *
 * O destino é o do pedido, e não o endereço atual do comprador: um pedido em
 * transporte mantém o ponto mesmo que o comprador marque outro na aba Entrega.
 */
@Composable
fun MapaPedidoDialog(pedido: Pedido, onFechar: () -> Unit) {
    val lat = pedido.latitudeEntrega ?: return
    val lng = pedido.longitudeEntrega ?: return
    val destino = LatLng(lat, lng)

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
                Text(
                    "Coordenadas: %.5f, %.5f".format(lat, lng),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )

                GoogleMap(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(destino, 16f)
                    }
                ) {
                    Marker(
                        state = rememberMarkerState(position = destino),
                        title = "Ponto de entrega",
                        snippet = pedido.enderecoEntrega
                    )
                }
            }
        }
    }
}

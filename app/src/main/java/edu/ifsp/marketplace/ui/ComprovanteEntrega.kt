package edu.ifsp.marketplace.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.ifsp.marketplace.data.Pedido
import edu.ifsp.marketplace.util.formatarDistancia
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatarData(instante: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(instante))

/**
 * Comprovação registrada pelo entregador, vista igual pelos dois perfis: quando,
 * onde (endereço e coordenadas) e a que distância do ponto marcado pelo comprador.
 */
@Composable
fun ComprovanteEntrega(pedido: Pedido, modifier: Modifier = Modifier) {
    val instante = pedido.entregueEm ?: return

    Column(modifier = modifier) {
        Text(
            "Entregue em ${formatarData(instante)}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            "Local: ${pedido.enderecoEntregue?.ifBlank { null } ?: "endereço pendente, será preenchido com conexão"}",
            style = MaterialTheme.typography.bodySmall
        )
        val lat = pedido.latitudeEntregue
        val lng = pedido.longitudeEntregue
        if (lat != null && lng != null) {
            Text(
                "Coordenadas: %.5f, %.5f".format(lat, lng),
                style = MaterialTheme.typography.bodySmall
            )
        }
        pedido.distanciaDoDestinoMetros?.let { metros ->
            Text(
                "A ${formatarDistancia(metros)} do ponto de entrega marcado",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

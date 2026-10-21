package edu.ifsp.marketplace.ui.entregador

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import edu.ifsp.marketplace.data.Pedido
import edu.ifsp.marketplace.data.StatusPedido
import edu.ifsp.marketplace.ui.CadastroViewModel
import edu.ifsp.marketplace.util.criarArquivoParaCaptura
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Converte coordenadas em endereço legível (reverse geocoding).
 *
 * Usa o [Geocoder] do próprio Android, que no aparelho real é atendido pelo
 * serviço do Google Play. O método síncrono está marcado como deprecado a
 * partir da API 33, mas continua funcionando e é o caminho mais simples aqui.
 */
@Suppress("DEPRECATION")
private suspend fun enderecoDasCoordenadas(
    context: android.content.Context,
    latitude: Double,
    longitude: Double
): String? = withContext(Dispatchers.IO) {
    try {
        Geocoder(context, Locale("pt", "BR"))
            .getFromLocation(latitude, longitude, 1)
            ?.firstOrNull()
            ?.let { endereco ->
                listOfNotNull(
                    endereco.thoroughfare,
                    endereco.subThoroughfare,
                    endereco.subLocality,
                    endereco.locality,
                    endereco.adminArea
                ).joinToString(", ").ifBlank { null }
            }
    } catch (e: Exception) {
        null
    }
}

private fun formatarData(instante: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(instante))

/**
 * Lista de entregas do perfil Entregador.
 *
 * Cada pedido pode ser retirado, roteirizado e, por fim, registrado como
 * entregue — com foto, coordenadas e endereço obtido por geocodificação reversa,
 * como pede o manual do curso.
 */
@SuppressLint("MissingPermission")
@Composable
fun EntregasScreen(
    viewModel: CadastroViewModel,
    onAbrirRota: (Pedido) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val pedidos by viewModel.pedidos.collectAsState()

    // Pedido cuja foto de comprovação está sendo tirada agora
    var pedidoEmRegistro by remember { mutableStateOf<Pedido?>(null) }
    var caminhoFoto by remember { mutableStateOf<String?>(null) }

    fun concluirRegistro(pedido: Pedido, fotoPath: String?) {
        escopo.launch {
            val temPermissao = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            var latitude: Double? = null
            var longitude: Double? = null

            if (temPermissao) {
                val cliente = LocationServices.getFusedLocationProviderClient(context)
                val local = suspendCancellableCoroutine { continuacao ->
                    cliente.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        CancellationTokenSource().token
                    ).addOnSuccessListener { continuacao.resume(it) }
                        .addOnFailureListener { continuacao.resume(null) }
                }
                latitude = local?.latitude
                longitude = local?.longitude
            }

            val endereco = if (latitude != null && longitude != null) {
                enderecoDasCoordenadas(context, latitude, longitude)
            } else null

            viewModel.registrarEntrega(pedido, fotoPath, latitude, longitude, endereco)

            Toast.makeText(
                context,
                if (latitude == null) "Entrega registrada (sem coordenadas)"
                else "Entrega registrada com foto e localização",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val tirarFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { sucesso ->
        val pedido = pedidoEmRegistro
        val foto = caminhoFoto
        pedidoEmRegistro = null
        caminhoFoto = null
        if (sucesso && pedido != null) concluirRegistro(pedido, foto)
    }

    val pedirCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        val pedido = pedidoEmRegistro
        if (!concedida || pedido == null) {
            pedidoEmRegistro = null
            Toast.makeText(context, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val (caminho, uri) = criarArquivoParaCaptura(context)
        caminhoFoto = caminho
        tirarFoto.launch(uri)
    }

    fun registrarEntrega(pedido: Pedido) {
        pedidoEmRegistro = pedido
        val temCamera = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (temCamera) {
            val (caminho, uri) = criarArquivoParaCaptura(context)
            caminhoFoto = caminho
            tirarFoto.launch(uri)
        } else {
            pedirCamera.launch(Manifest.permission.CAMERA)
        }
    }

    if (pedidos.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Nenhuma entrega disponível.", style = MaterialTheme.typography.titleMedium)
            Text(
                "Os pedidos aparecem aqui assim que um negociante comprar um produto.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(pedidos, key = { it.id }) { pedido ->
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(pedido.produtoNome, style = MaterialTheme.typography.titleMedium)
                        AssistChip(
                            onClick = {},
                            label = { Text(pedido.situacao.rotulo) }
                        )
                    }

                    Text(
                        "Para ${pedido.compradorNome} · ${pedido.quantidade} un",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        pedido.enderecoEntrega.ifBlank { "Endereço não informado pelo comprador" },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    if (!pedido.temDestino) {
                        Text(
                            "Sem ponto no mapa — o comprador precisa marcar o destino.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    pedido.entregueEm?.let { instante ->
                        Text(
                            "Entregue em ${formatarData(instante)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        pedido.enderecoEntregue?.let { endereco ->
                            Text(
                                "Local da entrega: $endereco",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        val lat = pedido.latitudeEntregue
                        val lng = pedido.longitudeEntregue
                        if (lat != null && lng != null) {
                            Text(
                                "Coordenadas: %.5f, %.5f".format(lat, lng),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (pedido.situacao != StatusPedido.ENTREGUE) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (pedido.situacao == StatusPedido.AGUARDANDO) {
                                OutlinedButton(
                                    onClick = { viewModel.iniciarTransporte(pedido) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Retirar")
                                }
                            }

                            OutlinedButton(
                                onClick = { onAbrirRota(pedido) },
                                enabled = pedido.temDestino,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Filled.Route,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text("Rota")
                            }

                            Button(
                                onClick = { registrarEntrega(pedido) },
                                enabled = pedido.situacao == StatusPedido.EM_TRANSPORTE,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Filled.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text("Entregar")
                            }
                        }
                    }
                }
            }
        }
    }
}

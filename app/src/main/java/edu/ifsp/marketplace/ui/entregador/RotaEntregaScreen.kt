package edu.ifsp.marketplace.ui.entregador

import android.annotation.SuppressLint
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import edu.ifsp.marketplace.data.Pedido
import edu.ifsp.marketplace.data.StatusPedido
import edu.ifsp.marketplace.ui.CadastroViewModel
import edu.ifsp.marketplace.util.GeocodificacaoService
import edu.ifsp.marketplace.util.LocalizacaoService
import edu.ifsp.marketplace.util.Rota
import edu.ifsp.marketplace.util.RotaService
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val POSICAO_PADRAO = LatLng(-23.5505, -46.6333)

/**
 * Tela de rota do entregador.
 *
 * O percurso tem duas etapas: da posição atual até o vendedor, para retirar o
 * pacote, e depois até o ponto de entrega que o comprador marcou no mapa. Veio da
 * PoC `pocs/e5-rotas/aluno_eduarda`: a chamada ao serviço de rotas e a decodificação
 * da polyline estão em [RotaService].
 *
 * O percurso termina aqui mesmo: a tela permite retirar o pacote e, ao chegar,
 * registrar a entrega, cujo endereço vem de [GeocodificacaoService].
 */
@SuppressLint("MissingPermission")
@Composable
fun RotaEntregaScreen(
    pedido: Pedido,
    viewModel: CadastroViewModel,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val registro = rememberRegistroEntrega(viewModel, onRegistrada = onVoltar)
    val negociantes by viewModel.negociantes.collectAsState()

    val indoRetirar = pedido.rotaAteRetirada
    val latAlvo = if (indoRetirar) pedido.latitudeRetirada else pedido.latitudeEntrega
    val lngAlvo = if (indoRetirar) pedido.longitudeRetirada else pedido.longitudeEntrega
    val alvo = remember(latAlvo, lngAlvo) {
        if (latAlvo != null && lngAlvo != null) LatLng(latAlvo, lngAlvo) else null
    }
    val enderecoAlvo = if (indoRetirar) pedido.enderecoRetirada else pedido.enderecoEntrega

    var calculo by remember { mutableStateOf<Job?>(null) }
    var origem by remember { mutableStateOf<LatLng?>(null) }
    var enderecoOrigem by remember { mutableStateOf<String?>(null) }
    var rota by remember { mutableStateOf<Rota?>(null) }
    var calculando by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("Obtendo sua localização...") }
    var temPermissao by remember { mutableStateOf(LocalizacaoService.temPermissao(context)) }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(alvo ?: POSICAO_PADRAO, 13f)
    }

    fun gerarRota() {
        // Retirar o pacote troca o alvo: um cálculo em andamento já não interessa
        calculo?.cancel()
        val chegada = alvo
        if (chegada == null) {
            calculando = false
            rota = null
            mensagem = if (indoRetirar) {
                "Este pedido não tem endereço de retirada. Ao retirar o pacote, a rota segue até o comprador."
            } else {
                "Este pedido ainda não tem ponto de entrega no mapa."
            }
            return
        }
        calculo = escopo.launch {
            calculando = true
            mensagem = "Calculando a rota..."

            // Recalcular parte de onde o entregador está agora, não de onde começou
            val partida = LocalizacaoService.posicaoAtual(context) ?: origem
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
                mensagem = "Não foi possível calcular a rota. Verifique a conexão e a localização do aparelho."
                return@launch
            }

            rota = resultado
            val chegadaTexto = if (indoRetirar) "a retirada" else "o destino"
            mensagem = "${resultado.distanciaFormatada()} até $chegadaTexto, cerca de ${resultado.duracaoFormatada()}."

            val limites = LatLngBounds.builder()
                .apply { resultado.pontos.forEach { include(it) } }
                .build()
            cameraState.animate(CameraUpdateFactory.newLatLngBounds(limites, 120))
        }
    }

    val pedirPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        val concedida = permissoes.values.any { it }
        temPermissao = concedida
        if (concedida) gerarRota()
        else mensagem = "Sem permissão de localização não dá para gerar a rota."
    }

    // A troca de etapa recalcula sozinha, partindo de onde o pacote foi retirado
    LaunchedEffect(pedido.id, indoRetirar) {
        if (temPermissao) gerarRota()
        else pedirPermissao.launch(LocalizacaoService.PERMISSOES)
    }

    // O endereço de partida é só informativo: não segura o cálculo da rota
    LaunchedEffect(origem) {
        val ponto = origem ?: return@LaunchedEffect
        GeocodificacaoService.buscar(context, ponto.latitude, ponto.longitude)?.let {
            enderecoOrigem = it
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = "${if (indoRetirar) "Retirada" else "Entrega"} de ${pedido.produtoNome}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (indoRetirar && pedido.vendedorNome.isNotBlank()) {
                    "Com ${pedido.vendedorNome}, para ${pedido.compradorNome}"
                } else {
                    "Para ${pedido.compradorNome}"
                },
                style = MaterialTheme.typography.bodySmall
            )
            enderecoOrigem?.let { endereco ->
                Text(
                    text = "Saindo de: $endereco",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (indoRetirar && pedido.temRetirada) {
                Text(
                    text = "Retirar em: ${pedido.enderecoRetirada.ifBlank { "endereço pendente, ponto marcado no mapa" }}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = "${if (indoRetirar) "Depois, entregar em" else "Destino"}: ${pedido.enderecoEntrega.ifBlank { "endereço não informado" }}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = mensagem,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            // Quem atende no ponto para onde a rota leva agora
            if (pedido.situacao != StatusPedido.ENTREGUE) {
                BotaoLigar(
                    papel = if (indoRetirar) "vendedor" else "comprador",
                    negociante = negociantes.firstOrNull {
                        it.id == if (indoRetirar) pedido.vendedorId else pedido.compradorId
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                properties = MapProperties(isMyLocationEnabled = temPermissao)
            ) {
                origem?.let { ponto ->
                    key(ponto) {
                        Marker(
                            state = rememberMarkerState(position = ponto),
                            title = "Você está aqui",
                            snippet = enderecoOrigem,
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_AZURE
                            )
                        )
                    }
                }
                alvo?.let { ponto ->
                    key(ponto, indoRetirar) {
                        Marker(
                            state = rememberMarkerState(position = ponto),
                            title = if (indoRetirar) "Retirada do pacote" else "Ponto de entrega",
                            snippet = enderecoAlvo.ifBlank { null },
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (indoRetirar) BitmapDescriptorFactory.HUE_GREEN
                                else BitmapDescriptorFactory.HUE_RED
                            )
                        )
                    }
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

        when (pedido.situacao) {
            StatusPedido.AGUARDANDO -> Button(
                onClick = { viewModel.iniciarTransporte(pedido) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 8.dp)
            ) {
                Text("Retirar pacote")
            }

            StatusPedido.EM_TRANSPORTE -> Button(
                onClick = { registro.iniciar(pedido) },
                enabled = !registro.obtendoPosicao,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 8.dp)
            ) {
                Text(if (registro.obtendoPosicao) "Obtendo sua posição..." else "Cheguei: registrar entrega")
            }

            StatusPedido.ENTREGUE -> Unit
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onVoltar, modifier = Modifier.weight(1f)) {
                Text("Voltar")
            }
            OutlinedButton(
                onClick = { gerarRota() },
                enabled = !calculando && alvo != null,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (rota == null) "Gerar rota" else "Recalcular")
            }
        }
    }
}

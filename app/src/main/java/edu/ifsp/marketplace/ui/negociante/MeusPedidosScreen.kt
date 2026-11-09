package edu.ifsp.marketplace.ui.negociante

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import edu.ifsp.marketplace.data.Negociante
import edu.ifsp.marketplace.data.Pedido
import edu.ifsp.marketplace.data.StatusPedido
import edu.ifsp.marketplace.ui.CadastroViewModel
import edu.ifsp.marketplace.ui.ComprovanteEntrega
import edu.ifsp.marketplace.ui.mapa.MapaPedidoDialog
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun moeda(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

private fun horario(instante: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date(instante))

/**
 * Linha do tempo do pedido: as três etapas, com a atual destacada e o horário
 * em que cada uma aconteceu.
 *
 * É o "rastreamento de pacotes" pedido no manual para o perfil Negociante.
 */
@Composable
private fun LinhaDoTempo(pedido: Pedido, modifier: Modifier = Modifier) {
    val atual = pedido.situacao
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        StatusPedido.entries.forEach { etapa ->
            val alcancada = etapa.ordinal <= atual.ordinal
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (alcancada) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(14.dp)
                ) {}
                Text(
                    etapa.rotulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (alcancada) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                // Pedidos de antes deste registro não têm o horário: a etapa aparece sem ele
                pedido.horarioDa(etapa)?.takeIf { alcancada }?.let {
                    Text(
                        horario(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private enum class FiltroPedidos(val rotulo: String) {
    TODOS("Todos"),
    COMPRAS("Compras"),
    VENDAS("Vendas")
}

/**
 * Compras e vendas do negociante ligado à conta, com o estágio de cada entrega
 * e a comprovação registrada pelo entregador quando já concluída.
 */
@Composable
fun MeusPedidosScreen(viewModel: CadastroViewModel, eu: Negociante, modifier: Modifier = Modifier) {
    val pedidos by viewModel.pedidos.collectAsState()

    var filtro by remember { mutableStateOf(FiltroPedidos.TODOS) }
    val meusPedidos = pedidos.filter { pedido ->
        val compra = pedido.compradorId == eu.id
        val venda = pedido.vendedorId == eu.id
        when (filtro) {
            FiltroPedidos.TODOS -> compra || venda
            FiltroPedidos.COMPRAS -> compra
            FiltroPedidos.VENDAS -> venda
        }
    }

    var pedidoParaCancelarId by remember { mutableStateOf<Long?>(null) }
    pedidos.firstOrNull { it.id == pedidoParaCancelarId }?.let { pedido ->
        AlertDialog(
            onDismissRequest = { pedidoParaCancelarId = null },
            title = { Text("Cancelar pedido") },
            text = {
                Text(
                    "Cancelar o pedido de ${pedido.produtoNome}? ${pedido.quantidade} un voltam " +
                        "ao estoque do produto e o pedido sai da lista, para toda a equipe."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelarPedido(pedido)
                    pedidoParaCancelarId = null
                }) {
                    Text("Cancelar pedido", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pedidoParaCancelarId = null }) { Text("Voltar") }
            }
        )
    }

    var pedidoNoMapaId by remember { mutableStateOf<Long?>(null) }
    pedidos.firstOrNull { it.id == pedidoNoMapaId }?.let { pedido ->
        MapaPedidoDialog(pedido, onFechar = { pedidoNoMapaId = null })
    }

    var pedidoParaApagarId by remember { mutableStateOf<Long?>(null) }
    pedidos.firstOrNull { it.id == pedidoParaApagarId }?.let { pedido ->
        AlertDialog(
            onDismissRequest = { pedidoParaApagarId = null },
            title = { Text("Apagar pedido") },
            text = {
                Text(
                    "Apagar o pedido de ${pedido.produtoNome} de ${pedido.compradorNome}? " +
                        "Ele sai deste aparelho e da nuvem, para toda a equipe."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.excluirPedido(pedido)
                    pedidoParaApagarId = null
                }) {
                    Text("Apagar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pedidoParaApagarId = null }) { Text("Cancelar") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FiltroPedidos.entries) { opcao ->
                FilterChip(
                    selected = filtro == opcao,
                    onClick = { filtro = opcao },
                    label = { Text(opcao.rotulo) }
                )
            }
        }

        if (meusPedidos.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Nenhum pedido ainda.", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Compre um produto na aba Vitrine para acompanhar a entrega aqui.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(meusPedidos, key = { it.id }) { pedido ->
                Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                pedido.produtoNome,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                moeda(pedido.valorTotal),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            "${pedido.quantidade} un · " + when {
                                pedido.compradorId == eu.id && pedido.vendedorNome.isNotBlank() ->
                                    "sua compra de ${pedido.vendedorNome}"
                                pedido.compradorId == eu.id -> "sua compra"
                                else -> "sua venda para ${pedido.compradorNome}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        LinhaDoTempo(
                            pedido = pedido,
                            modifier = Modifier.padding(top = 14.dp)
                        )

                        Text(
                            "Entrega em: ${pedido.enderecoEntrega.ifBlank { "endereço não definido" }}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 14.dp)
                        )

                        if (pedido.latitudeEntrega != null && pedido.longitudeEntrega != null) {
                            OutlinedButton(
                                onClick = { pedidoNoMapaId = pedido.id },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Ver no mapa", modifier = Modifier.padding(start = 8.dp))
                            }
                        }

                        // Comprovação enviada pelo entregador
                        ComprovanteEntrega(pedido)
                        if (pedido.entregueEm != null) {
                            val foto = pedido.fotoEntregaPath?.let(::File)?.takeIf { it.exists() }
                            if (foto != null) {
                                AsyncImage(
                                    model = foto,
                                    contentDescription = "Foto da entrega",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                // Fotos ficam só no aparelho que as tirou, como pede o manual
                                Text(
                                    "Foto registrada no aparelho do entregador",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // Antes da retirada o pedido ainda pode ser desfeito, devolvendo o estoque
                        if (pedido.situacao == StatusPedido.AGUARDANDO) {
                            OutlinedButton(
                                onClick = { pedidoParaCancelarId = pedido.id },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Cancelar pedido",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        // Só pedidos concluídos: os em transporte ainda estão com o entregador
                        if (pedido.situacao == StatusPedido.ENTREGUE) {
                            OutlinedButton(
                                onClick = { pedidoParaApagarId = pedido.id },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "Apagar",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

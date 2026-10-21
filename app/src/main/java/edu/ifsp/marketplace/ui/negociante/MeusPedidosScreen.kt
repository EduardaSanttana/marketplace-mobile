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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import edu.ifsp.marketplace.data.StatusPedido
import edu.ifsp.marketplace.ui.CadastroViewModel
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun moeda(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

private fun formatarData(instante: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(instante))

/**
 * Linha do tempo do pedido: as três etapas, com a atual destacada.
 *
 * É o "rastreamento de pacotes" pedido no manual para o perfil Negociante.
 */
@Composable
private fun LinhaDoTempo(atual: StatusPedido, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
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
            }
        }
    }
}

/**
 * Pedidos feitos pelo negociante selecionado, com o estágio de cada entrega
 * e a comprovação registrada pelo entregador quando já concluída.
 */
@Composable
fun MeusPedidosScreen(viewModel: CadastroViewModel, modifier: Modifier = Modifier) {
    val pedidos by viewModel.pedidos.collectAsState()
    val negociantes by viewModel.negociantes.collectAsState()

    var compradorId by remember { mutableStateOf<Long?>(null) }
    val meusPedidos = pedidos.filter { compradorId == null || it.compradorId == compradorId }

    Column(modifier = modifier.fillMaxSize()) {
        if (negociantes.isNotEmpty()) {
            Text(
                "Filtrar por negociante:",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = compradorId == null,
                        onClick = { compradorId = null },
                        label = { Text("Todos") }
                    )
                }
                items(negociantes, key = { it.id }) { n ->
                    FilterChip(
                        selected = n.id == compradorId,
                        onClick = { compradorId = n.id },
                        label = { Text(n.nome) }
                    )
                }
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
                            "${pedido.quantidade} un · comprado por ${pedido.compradorNome}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        LinhaDoTempo(
                            atual = pedido.situacao,
                            modifier = Modifier.padding(top = 14.dp)
                        )

                        Text(
                            "Entrega em: ${pedido.enderecoEntrega.ifBlank { "endereço não definido" }}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 14.dp)
                        )

                        // Comprovação enviada pelo entregador
                        pedido.entregueEm?.let { instante ->
                            Text(
                                "Entregue em ${formatarData(instante)}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            pedido.enderecoEntregue?.let { endereco ->
                                Text(
                                    "Local: $endereco",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            pedido.fotoEntregaPath?.let { caminho ->
                                AsyncImage(
                                    model = File(caminho),
                                    contentDescription = "Foto da entrega",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

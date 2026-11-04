package edu.ifsp.marketplace.ui.entregador

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.ifsp.marketplace.data.Pedido
import edu.ifsp.marketplace.data.StatusPedido
import edu.ifsp.marketplace.ui.CadastroViewModel
import edu.ifsp.marketplace.ui.ComprovanteEntrega

/**
 * Lista de entregas do perfil Entregador.
 *
 * Cada pedido pode ser retirado, roteirizado e, por fim, registrado como
 * entregue — com foto, coordenadas e endereço obtido por geocodificação reversa,
 * como pede o manual do curso.
 */
@Composable
fun EntregasScreen(
    viewModel: CadastroViewModel,
    onAbrirRota: (Pedido) -> Unit,
    modifier: Modifier = Modifier
) {
    val pedidos by viewModel.pedidos.collectAsState()
    val registro = rememberRegistroEntrega(viewModel)

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
                        pedido.enderecoEntrega.ifBlank {
                            if (pedido.temDestino) "Endereço do destino pendente, ponto marcado no mapa"
                            else "Endereço não informado pelo comprador"
                        },
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

                    ComprovanteEntrega(pedido)

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
                                onClick = { registro.iniciar(pedido) },
                                enabled = pedido.situacao == StatusPedido.EM_TRANSPORTE &&
                                    !registro.obtendoPosicao,
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

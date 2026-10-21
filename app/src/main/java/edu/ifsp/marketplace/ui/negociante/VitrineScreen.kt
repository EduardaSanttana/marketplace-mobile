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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import edu.ifsp.marketplace.data.Produto
import edu.ifsp.marketplace.ui.CadastroViewModel
import java.io.File
import java.text.NumberFormat
import java.util.Locale

private fun moeda(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

/**
 * Vitrine do marketplace.
 *
 * O negociante escolhe com qual conta está comprando e então adquire produtos
 * anunciados pelos outros. Produtos do próprio comprador não aparecem na lista,
 * e os sem estoque ficam bloqueados.
 */
@Composable
fun VitrineScreen(viewModel: CadastroViewModel, modifier: Modifier = Modifier) {
    val produtos by viewModel.produtos.collectAsState()
    val negociantes by viewModel.negociantes.collectAsState()

    var compradorId by remember { mutableStateOf<Long?>(null) }
    val comprador = negociantes.firstOrNull { it.id == compradorId }
    var produtoEmCompra by remember { mutableStateOf<Produto?>(null) }

    // Ninguém compra do próprio anúncio
    val disponiveis = produtos.filter { it.negocianteId != compradorId }

    if (negociantes.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Nenhum negociante cadastrado.", style = MaterialTheme.typography.titleMedium)
            Text(
                "Cadastre um negociante na aba Negociantes para poder comprar.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "Comprando como:",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(negociantes, key = { it.id }) { n ->
                FilterChip(
                    selected = n.id == compradorId,
                    onClick = { compradorId = n.id },
                    label = { Text(n.nome) }
                )
            }
        }

        if (comprador != null && comprador.latitude == null) {
            Text(
                "Este negociante ainda não tem ponto de entrega no mapa. " +
                    "Defina na aba Entrega, senão o entregador não consegue gerar a rota.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (disponiveis.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Nenhum produto à venda.", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Cadastre produtos na aba Meus Produtos.",
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
            items(disponiveis, key = { it.id }) { produto ->
                val vendedor = negociantes.firstOrNull { it.id == produto.negocianteId }
                Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        produto.fotoPath?.let { caminho ->
                            AsyncImage(
                                model = File(caminho),
                                contentDescription = produto.nome,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(produto.nome, style = MaterialTheme.typography.titleMedium)
                            if (produto.descricao.isNotBlank()) {
                                Text(
                                    produto.descricao,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                "Vendido por ${vendedor?.nome ?: "—"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                moeda(produto.preco),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                if (produto.quantidade > 0) "${produto.quantidade} em estoque"
                                else "Sem estoque",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = { produtoEmCompra = produto },
                            enabled = comprador != null && produto.quantidade > 0
                        ) {
                            Text("Comprar")
                        }
                    }
                }
            }
        }
    }

    val emCompra = produtoEmCompra
    if (emCompra != null && comprador != null) {
        AlertDialog(
            onDismissRequest = { produtoEmCompra = null },
            title = { Text("Confirmar compra") },
            text = {
                Column {
                    Text("${emCompra.nome} por ${moeda(emCompra.preco)}")
                    Text(
                        "Entrega em: ${comprador.endereco.ifBlank { "endereço não definido" }}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.comprarProduto(emCompra, comprador, quantidade = 1)
                    produtoEmCompra = null
                }) {
                    Text("Comprar")
                }
            },
            dismissButton = {
                TextButton(onClick = { produtoEmCompra = null }) { Text("Cancelar") }
            }
        )
    }
}

package edu.ifsp.marketplace.ui.negociante

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import edu.ifsp.marketplace.data.Negociante
import edu.ifsp.marketplace.data.Produto
import edu.ifsp.marketplace.ui.CadastroViewModel
import edu.ifsp.marketplace.ui.cadastro.FormularioNegociante
import java.io.File
import java.text.Normalizer
import java.text.NumberFormat
import java.util.Locale

private fun moeda(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

/** Minúsculas e sem acento: "Canéta" e "caneta" são a mesma busca. */
private fun normalizar(texto: String): String =
    Normalizer.normalize(texto.lowercase(Locale("pt", "BR")), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

/** A busca vale para nome, descrição e vendedor; vazia, mostra tudo. */
internal fun produtoCombinaComBusca(produto: Produto, vendedorNome: String?, busca: String): Boolean {
    val termo = normalizar(busca.trim())
    if (termo.isEmpty()) return true
    return listOf(produto.nome, produto.descricao, vendedorNome.orEmpty())
        .any { normalizar(it).contains(termo) }
}

/**
 * Vitrine do marketplace.
 *
 * O negociante ligado à conta compra produtos anunciados pelos outros. Os
 * próprios anúncios não aparecem na lista, e os sem estoque ficam bloqueados.
 */
@Composable
fun VitrineScreen(
    viewModel: CadastroViewModel,
    eu: Negociante,
    onDesvincular: () -> Unit,
    modifier: Modifier = Modifier
) {
    val produtos by viewModel.produtos.collectAsState()
    val negociantes by viewModel.negociantes.collectAsState()

    val avaliacoes by viewModel.avaliacoes.collectAsState()

    val comprador = eu
    var produtoEmCompra by remember { mutableStateOf<Produto?>(null) }
    var quantidadeEmCompra by remember { mutableIntStateOf(1) }
    var confirmandoDesvinculo by remember { mutableStateOf(false) }
    var editandoMeusDados by remember { mutableStateOf(false) }

    var busca by rememberSaveable { mutableStateOf("") }

    // Ninguém compra do próprio anúncio
    val anunciados = produtos.filter { it.negocianteId != eu.id }
    val disponiveis = anunciados.filter { produto ->
        produtoCombinaComBusca(
            produto,
            negociantes.firstOrNull { it.id == produto.negocianteId }?.nome,
            busca
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Comprando como ${eu.nome}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { editandoMeusDados = true }) { Text("Meus dados") }
            TextButton(onClick = { confirmandoDesvinculo = true }) { Text("Não sou eu") }
        }

        if (comprador.latitude == null) {
            Text(
                "Você ainda não tem ponto de entrega no mapa. " +
                    "Defina na aba Entrega para o entregador poder gerar a rota.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (anunciados.isNotEmpty()) {
            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                placeholder = { Text("Buscar produto ou vendedor") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (busca.isNotEmpty()) {
                        IconButton(onClick = { busca = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 16.dp)
            )
        }

        if (disponiveis.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (anunciados.isEmpty()) {
                    Text("Nenhum produto à venda.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Quando outro negociante anunciar um produto, ele aparece aqui.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    Text("Nada encontrado para \"${busca.trim()}\".", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { busca = "" }) { Text("Limpar busca") }
                }
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
                        // A foto fica só no aparelho do vendedor; nos outros, o ícone ocupa o lugar
                        val foto = produto.fotoPath?.let(::File)?.takeIf { it.exists() }
                        if (foto != null) {
                            AsyncImage(
                                model = foto,
                                contentDescription = produto.nome,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.ShoppingBag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
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
                            val notas = avaliacoes.filter { it.produtoId == produto.id }.map { it.nota }
                            if (notas.isNotEmpty()) {
                                Text(
                                    "★ %.1f · %d %s".format(
                                        Locale("pt", "BR"),
                                        notas.average(),
                                        notas.size,
                                        if (notas.size == 1) "avaliação" else "avaliações"
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
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
                            onClick = {
                                quantidadeEmCompra = 1
                                produtoEmCompra = produto
                            },
                            enabled = produto.quantidade > 0
                        ) {
                            Text("Comprar")
                        }
                    }
                }
            }
        }
    }

    if (confirmandoDesvinculo) {
        AlertDialog(
            onDismissRequest = { confirmandoDesvinculo = false },
            title = { Text("Esta conta não é ${eu.nome}?") },
            text = {
                Text("A conta deixa de agir como ${eu.nome} em todos os aparelhos, e você escolhe de novo quem é.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoDesvinculo = false
                    onDesvincular()
                }) {
                    Text("Desfazer vínculo")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoDesvinculo = false }) { Text("Cancelar") }
            }
        )
    }

    if (editandoMeusDados) {
        Dialog(onDismissRequest = { editandoMeusDados = false }) {
            Surface(shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Meus dados", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "O endereço fica na aba Entrega. Pedidos já feitos guardam o nome da época.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    FormularioNegociante(
                        textoBotao = "Salvar",
                        nomeInicial = eu.nome,
                        emailInicial = eu.email,
                        telefoneInicial = eu.telefone,
                        onSalvar = { nome, email, telefone ->
                            viewModel.atualizarNegociante(eu, nome, email, telefone)
                            editandoMeusDados = false
                        },
                        onCancelar = { editandoMeusDados = false }
                    )
                }
            }
        }
    }

    val emCompra = produtoEmCompra
    if (emCompra != null) {
        AlertDialog(
            onDismissRequest = { produtoEmCompra = null },
            title = { Text("Confirmar compra") },
            text = {
                Column {
                    Text(emCompra.nome, style = MaterialTheme.typography.titleMedium)
                    // Não dá para pedir mais do que o vendedor tem em estoque
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { quantidadeEmCompra-- },
                            enabled = quantidadeEmCompra > 1
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Menos uma unidade")
                        }
                        Text("$quantidadeEmCompra un", style = MaterialTheme.typography.titleMedium)
                        IconButton(
                            onClick = { quantidadeEmCompra++ },
                            enabled = quantidadeEmCompra < emCompra.quantidade
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Mais uma unidade")
                        }
                    }
                    Text(
                        "Total: ${moeda(emCompra.preco * quantidadeEmCompra)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Entrega em: ${comprador.endereco.ifBlank { "endereço não definido" }}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.comprarProduto(emCompra, comprador, quantidade = quantidadeEmCompra)
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

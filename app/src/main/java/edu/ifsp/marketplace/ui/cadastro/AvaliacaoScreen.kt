package edu.ifsp.marketplace.ui.cadastro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.ifsp.marketplace.data.Negociante
import edu.ifsp.marketplace.data.Produto
import edu.ifsp.marketplace.ui.CadastroViewModel

/** Avaliações de produtos; o negociante da conta avalia só o que comprou, como pede o manual. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliacaoScreen(viewModel: CadastroViewModel, eu: Negociante, modifier: Modifier = Modifier) {
    var produtoSelecionado by remember { mutableStateOf<Produto?>(null) }
    var menuExpandido by remember { mutableStateOf(false) }
    // 0 = ainda sem nota; as estrelas só permitem de 1 a 5
    var nota by remember { mutableIntStateOf(0) }
    var comentario by remember { mutableStateOf("") }

    val produtos by viewModel.produtos.collectAsState()
    val pedidos by viewModel.pedidos.collectAsState()
    val avaliacoes by viewModel.avaliacoes.collectAsState()
    val comprados = produtos.filter { produto ->
        pedidos.any { it.compradorId == eu.id && it.produtoId == produto.id }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SecaoFormulario(titulo = "Nova avaliação", icone = Icons.Filled.RateReview) {
                ExposedDropdownMenuBox(
                    expanded = menuExpandido,
                    onExpandedChange = { menuExpandido = it }
                ) {
                    OutlinedTextField(
                        value = produtoSelecionado?.nome ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Produto") },
                        placeholder = { Text("Selecione um produto") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpandido) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    DropdownMenu(
                        expanded = menuExpandido,
                        onDismissRequest = { menuExpandido = false }
                    ) {
                        if (comprados.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Compre um produto na Vitrine para avaliá-lo") },
                                onClick = { menuExpandido = false },
                                enabled = false
                            )
                        }
                        comprados.forEach { produto ->
                            DropdownMenuItem(
                                text = { Text(produto.nome) },
                                onClick = {
                                    produtoSelecionado = produto
                                    menuExpandido = false
                                }
                            )
                        }
                    }
                }
                Text("Nota", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    (1..5).forEach { valor ->
                        IconButton(onClick = { nota = valor }) {
                            Icon(
                                imageVector = if (valor <= nota) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Nota $valor",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it },
                    label = { Text("Comentário") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                BotaoSalvar(
                    habilitado = produtoSelecionado != null && nota in 1..5,
                    onClick = {
                        viewModel.cadastrarAvaliacao(
                            produto = produtoSelecionado ?: return@BotaoSalvar,
                            nota = nota,
                            comentario = comentario
                        )
                        produtoSelecionado = null
                        nota = 0
                        comentario = ""
                    }
                )
            }

            Spacer12()
            TituloLista("Avaliações cadastradas", avaliacoes.size)
        }

        if (avaliacoes.isEmpty()) {
            item {
                EstadoVazio(
                    icone = Icons.Filled.RateReview,
                    mensagem = "Nenhuma avaliação cadastrada ainda"
                )
            }
        }

        items(avaliacoes, key = { it.id }) { avaliacao ->
            val nomeProduto = produtos.find { it.id == avaliacao.produtoId }?.nome
                ?: "Produto removido"

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            nomeProduto,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        EstrelasNota(nota = avaliacao.nota)
                    }
                    if (avaliacao.comentario.isNotBlank()) {
                        Text(
                            avaliacao.comentario,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EstrelasNota(nota: Int) {
    Row {
        repeat(5) { indice ->
            Icon(
                imageVector = if (indice < nota) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

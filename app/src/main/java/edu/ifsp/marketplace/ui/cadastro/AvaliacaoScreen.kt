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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import edu.ifsp.marketplace.data.Produto
import edu.ifsp.marketplace.ui.CadastroViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvaliacaoScreen(viewModel: CadastroViewModel, modifier: Modifier = Modifier) {
    var produtoSelecionado by remember { mutableStateOf<Produto?>(null) }
    var menuExpandido by remember { mutableStateOf(false) }
    var nota by remember { mutableStateOf("") }
    var comentario by remember { mutableStateOf("") }

    val produtos by viewModel.produtos.collectAsState()
    val avaliacoes by viewModel.avaliacoes.collectAsState()

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
                        if (produtos.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Nenhum produto cadastrado") },
                                onClick = { menuExpandido = false },
                                enabled = false
                            )
                        }
                        produtos.forEach { produto ->
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
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota (1 a 5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it },
                    label = { Text("Comentário") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                BotaoSalvar(
                    habilitado = produtoSelecionado != null && nota.isNotBlank(),
                    onClick = {
                        viewModel.cadastrarAvaliacao(
                            produtoId = produtoSelecionado?.id ?: 0L,
                            nota = nota.toIntOrNull() ?: 0,
                            comentario = comentario
                        )
                        produtoSelecionado = null
                        nota = ""
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
                ?: "Produto #${avaliacao.produtoId}"

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
                imageVector = if (indice < nota) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

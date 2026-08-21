package com.example.aluno_melissaa.ui.cadastro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.aluno_melissaa.ui.CadastroViewModel

@Composable
fun AvaliacaoScreen(viewModel: CadastroViewModel, modifier: Modifier = Modifier) {
    var produtoId by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    var comentario by remember { mutableStateOf("") }

    val avaliacoes by viewModel.avaliacoes.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Avaliação de Produto")
            OutlinedTextField(
                value = produtoId,
                onValueChange = { produtoId = it },
                label = { Text("ID do produto") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nota,
                onValueChange = { nota = it },
                label = { Text("Nota (1 a 5)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it },
                label = { Text("Comentário") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.cadastrarAvaliacao(
                        produtoId = produtoId.toLongOrNull() ?: 0L,
                        nota = nota.toIntOrNull() ?: 0,
                        comentario = comentario
                    )
                    produtoId = ""
                    nota = ""
                    comentario = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar")
            }
            Text("Avaliações cadastradas")
        }
        items(avaliacoes) { avaliacao ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Produto: ${avaliacao.produtoId}")
                    Text("Nota: ${avaliacao.nota}")
                    Text(avaliacao.comentario)
                }
            }
        }
    }
}

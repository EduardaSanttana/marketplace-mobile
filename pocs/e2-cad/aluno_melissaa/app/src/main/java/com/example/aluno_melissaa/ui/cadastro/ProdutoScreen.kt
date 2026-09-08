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
fun ProdutoScreen(viewModel: CadastroViewModel, modifier: Modifier = Modifier) {
    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var preco by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf("") }
    var negocianteId by remember { mutableStateOf("") }

    val produtos by viewModel.produtos.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Cadastro de Produto")
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = preco,
                onValueChange = { preco = it },
                label = { Text("Preço") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = quantidade,
                onValueChange = { quantidade = it },
                label = { Text("Quantidade") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = negocianteId,
                onValueChange = { negocianteId = it },
                label = { Text("ID do negociante") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    viewModel.cadastrarProduto(
                        nome = nome,
                        descricao = descricao,
                        preco = preco.toDoubleOrNull() ?: 0.0,
                        quantidade = quantidade.toIntOrNull() ?: 0,
                        negocianteId = negocianteId.toLongOrNull() ?: 0L
                    )
                    nome = ""
                    descricao = ""
                    preco = ""
                    quantidade = ""
                    negocianteId = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar")
            }
            Text("Produtos cadastrados")
        }
        items(produtos) { produto ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(produto.nome)
                    Text(produto.descricao)
                    Text("Preço: ${produto.preco}")
                    Text("Quantidade: ${produto.quantidade}")
                    Text("Negociante: ${produto.negocianteId}")
                    Text(if (produto.pendingSync) "⏳ Pendente de sincronização" else "☁ Sincronizado")
                    Button(
                        onClick = { viewModel.excluirProduto(produto) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Excluir")
                    }
                }
            }
        }
    }
}

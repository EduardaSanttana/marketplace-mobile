package com.example.aluno_melissaa.ui.cadastro

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aluno_melissaa.ui.CadastroViewModel

private val abas = listOf("Produtos", "Negociantes", "Avaliações", "Sincronização")

@Composable
fun CadastroApp() {
    val viewModel: CadastroViewModel = viewModel()
    var abaSelecionada by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                abas.forEachIndexed { index, titulo ->
                    NavigationBarItem(
                        selected = abaSelecionada == index,
                        onClick = { abaSelecionada = index },
                        icon = {},
                        label = { Text(titulo) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (abaSelecionada) {
            0 -> ProdutoScreen(viewModel, Modifier.padding(innerPadding))
            1 -> NegocianteScreen(viewModel, Modifier.padding(innerPadding))
            2 -> AvaliacaoScreen(viewModel, Modifier.padding(innerPadding))
            3 -> SyncScreen(viewModel, Modifier.padding(innerPadding))
        }
    }
}

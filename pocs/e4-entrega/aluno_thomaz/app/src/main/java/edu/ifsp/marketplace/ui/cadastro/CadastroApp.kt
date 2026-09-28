package edu.ifsp.marketplace.ui.cadastro

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.ifsp.marketplace.ui.CadastroViewModel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.LocationOn
import edu.ifsp.marketplace.ui.mapa.MapaEntregaScreen

private data class AbaInfo(
    val titulo: String,
    val iconeSelecionado: androidx.compose.ui.graphics.vector.ImageVector,
    val iconeNaoSelecionado: androidx.compose.ui.graphics.vector.ImageVector
)

private val abas = listOf(
    AbaInfo("Produtos", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag),
    AbaInfo("Negociantes", Icons.Filled.Storefront, Icons.Outlined.Storefront),
    AbaInfo("Avaliações", Icons.Filled.RateReview, Icons.Outlined.RateReview),
    AbaInfo("Entrega", Icons.Filled.LocationOn, Icons.Outlined.LocationOn)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroApp(
    usuarioEmail: String? = null,
    onLogout: () -> Unit = {}
) {
    val viewModel: CadastroViewModel = viewModel()
    var abaSelecionada by remember { mutableIntStateOf(0) }
    var mostrarDialogoSaida by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "InFraTech",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { mostrarDialogoSaida = true }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Sair da conta")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                abas.forEachIndexed { index, aba ->
                    val selecionada = abaSelecionada == index
                    NavigationBarItem(
                        selected = selecionada,
                        onClick = { abaSelecionada = index },
                        icon = {
                            Icon(
                                imageVector = if (selecionada) aba.iconeSelecionado else aba.iconeNaoSelecionado,
                                contentDescription = aba.titulo
                            )
                        },
                        label = { Text(aba.titulo) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (abaSelecionada) {
            0 -> ProdutoScreen(viewModel, Modifier.padding(innerPadding))
            1 -> NegocianteScreen(viewModel, Modifier.padding(innerPadding))
            2 -> AvaliacaoScreen(viewModel, Modifier.padding(innerPadding))
            3 -> MapaEntregaScreen(Modifier.padding(innerPadding))
        }
    }

    if (mostrarDialogoSaida) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSaida = false },
            title = { Text("Sair da conta") },
            text = {
                Text(
                    if (usuarioEmail != null) "Deseja sair da conta $usuarioEmail?"
                    else "Deseja sair da sua conta?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoSaida = false
                    onLogout()
                }) {
                    Text("Sair")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoSaida = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

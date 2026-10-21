package edu.ifsp.marketplace.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Receipt
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.ifsp.marketplace.data.Pedido
import edu.ifsp.marketplace.ui.cadastro.AvaliacaoScreen
import edu.ifsp.marketplace.ui.cadastro.NegocianteScreen
import edu.ifsp.marketplace.ui.cadastro.ProdutoScreen
import edu.ifsp.marketplace.ui.entregador.EntregasScreen
import edu.ifsp.marketplace.ui.entregador.RotaEntregaScreen
import edu.ifsp.marketplace.ui.mapa.MapaEntregaScreen
import edu.ifsp.marketplace.ui.negociante.MeusPedidosScreen
import edu.ifsp.marketplace.ui.negociante.VitrineScreen

/**
 * Os dois perfis de usuário previstos no projeto #2 do manual.
 *
 * A troca é feita pelo botão na barra superior. Em um sistema real o perfil
 * viria do cadastro do usuário autenticado; aqui ele é escolhido na interface
 * para que a mesma instalação consiga demonstrar os dois lados do marketplace.
 */
enum class PerfilUsuario(val rotulo: String) {
    NEGOCIANTE("Negociante"),
    ENTREGADOR("Entregador")
}

private data class AbaInfo(
    val titulo: String,
    val iconeSelecionado: ImageVector,
    val iconeNaoSelecionado: ImageVector
)

private val abasNegociante = listOf(
    AbaInfo("Vitrine", Icons.Filled.Storefront, Icons.Outlined.Storefront),
    AbaInfo("Meus Produtos", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag),
    AbaInfo("Pedidos", Icons.Filled.Receipt, Icons.Outlined.Receipt),
    AbaInfo("Entrega", Icons.Filled.LocationOn, Icons.Outlined.LocationOn),
    AbaInfo("Avaliações", Icons.Filled.RateReview, Icons.Outlined.RateReview)
)

private val abasEntregador = listOf(
    AbaInfo("Entregas", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping),
    AbaInfo("Negociantes", Icons.Filled.Storefront, Icons.Outlined.Storefront)
)

/**
 * Casca do aplicativo: barra superior, navegação inferior e troca de perfil.
 *
 * O conteúdo muda conforme o perfil ativo:
 *
 *  - **Negociante** compra na vitrine, anuncia produtos, acompanha os pedidos,
 *    define o ponto de entrega no mapa e avalia o que comprou.
 *  - **Entregador** vê as entregas disponíveis, gera a rota até o destino e
 *    registra a entrega com foto, coordenadas e endereço.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceApp(
    usuarioEmail: String? = null,
    onLogout: () -> Unit = {}
) {
    val viewModel: CadastroViewModel = viewModel()

    var perfil by remember { mutableStateOf(PerfilUsuario.NEGOCIANTE) }
    var abaSelecionada by remember { mutableIntStateOf(0) }
    var mostrarDialogoSaida by remember { mutableStateOf(false) }

    // Quando um pedido está aberto para roteirização, a tela de rota ocupa a área inteira
    var pedidoEmRota by remember { mutableStateOf<Pedido?>(null) }

    val abas = when (perfil) {
        PerfilUsuario.NEGOCIANTE -> abasNegociante
        PerfilUsuario.ENTREGADOR -> abasEntregador
    }

    fun trocarPerfil() {
        perfil = when (perfil) {
            PerfilUsuario.NEGOCIANTE -> PerfilUsuario.ENTREGADOR
            PerfilUsuario.ENTREGADOR -> PerfilUsuario.NEGOCIANTE
        }
        abaSelecionada = 0
        pedidoEmRota = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "InFraTech · ${perfil.rotulo}",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { trocarPerfil() }) {
                        Icon(
                            Icons.Filled.SwapHoriz,
                            contentDescription = "Trocar de perfil"
                        )
                    }
                    IconButton(onClick = { mostrarDialogoSaida = true }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Sair da conta")
                    }
                }
            )
        },
        bottomBar = {
            // A tela de rota é imersiva: esconde a navegação até o entregador voltar
            if (pedidoEmRota == null) {
                NavigationBar {
                    abas.forEachIndexed { index, aba ->
                        val selecionada = abaSelecionada == index
                        NavigationBarItem(
                            selected = selecionada,
                            onClick = { abaSelecionada = index },
                            icon = {
                                Icon(
                                    imageVector = if (selecionada) aba.iconeSelecionado
                                    else aba.iconeNaoSelecionado,
                                    contentDescription = aba.titulo
                                )
                            },
                            label = { Text(aba.titulo) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val conteudo = Modifier.padding(innerPadding)
        val emRota = pedidoEmRota

        if (emRota != null) {
            RotaEntregaScreen(
                pedido = emRota,
                onVoltar = { pedidoEmRota = null },
                modifier = conteudo
            )
            return@Scaffold
        }

        when (perfil) {
            PerfilUsuario.NEGOCIANTE -> when (abaSelecionada) {
                0 -> VitrineScreen(viewModel, conteudo)
                1 -> ProdutoScreen(viewModel, conteudo)
                2 -> MeusPedidosScreen(viewModel, conteudo)
                3 -> MapaEntregaScreen(viewModel, conteudo)
                4 -> AvaliacaoScreen(viewModel, conteudo)
            }

            PerfilUsuario.ENTREGADOR -> when (abaSelecionada) {
                0 -> EntregasScreen(
                    viewModel = viewModel,
                    onAbrirRota = { pedido -> pedidoEmRota = pedido },
                    modifier = conteudo
                )

                1 -> NegocianteScreen(viewModel, conteudo)
            }
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

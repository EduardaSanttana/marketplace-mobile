package edu.ifsp.marketplace.ui

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import edu.ifsp.marketplace.data.sync.EstadoSincronizacao
import edu.ifsp.marketplace.data.sync.observarSeEhEntregador
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import edu.ifsp.marketplace.geofencing.NotificacaoEntrega
import edu.ifsp.marketplace.ui.negociante.EscolherNegocianteScreen
import edu.ifsp.marketplace.ui.cadastro.AvaliacaoScreen
import edu.ifsp.marketplace.ui.cadastro.ProdutoScreen
import edu.ifsp.marketplace.ui.entregador.EntregasScreen
import edu.ifsp.marketplace.ui.entregador.RotaEntregaScreen
import edu.ifsp.marketplace.ui.mapa.MapaEntregaScreen
import edu.ifsp.marketplace.ui.negociante.MeusPedidosScreen
import edu.ifsp.marketplace.ui.negociante.VitrineScreen

/**
 * Os dois perfis de usuário previstos no projeto #2 do manual.
 *
 * O perfil vem da conta: ligada a um negociante, ela é negociante; quem tocou
 * em "Sou só entregador" é entregador. Não há troca dentro do app: para usar o
 * outro perfil, sai-se da conta e entra-se com a conta daquele papel.
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
    AbaInfo("Produtos", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag),
    AbaInfo("Pedidos", Icons.Filled.Receipt, Icons.Outlined.Receipt),
    AbaInfo("Entrega", Icons.Filled.LocationOn, Icons.Outlined.LocationOn),
    AbaInfo("Avaliações", Icons.Filled.RateReview, Icons.Outlined.RateReview)
)

// Negociantes se cadastram pela própria conta, em "Quem é você?": o entregador só entrega
private val abasEntregador = listOf(
    AbaInfo("Entregas", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping)
)

/**
 * Faixa sob a barra superior: sem conexão, conta o que ficou salvo no aparelho
 * esperando envio; com tudo na nuvem, some.
 */
@Composable
private fun AvisoSincronizacao(estado: EstadoSincronizacao) {
    val aviso = estado.aviso ?: return
    Surface(
        color = if (estado.problema) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (estado.conectado) Icons.Filled.CloudUpload else Icons.Filled.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                aviso,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

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
    usuarioUid: String,
    usuarioEmail: String? = null,
    onLogout: () -> Unit = {}
) {
    val viewModel: CadastroViewModel = viewModel()
    val context = LocalContext.current

    // Entregador é quem a equipe pôs na coleção "entregadores" do Firestore; a
    // própria conta não escolhe. Null enquanto a primeira resposta não chega.
    val ehEntregador by remember(usuarioEmail) { observarSeEhEntregador(usuarioEmail) }
        .collectAsState(initial = null)

    // Sobrevivem à recriação da Activity, para a volta da câmera cair na mesma tela
    var abaSelecionada by rememberSaveable { mutableIntStateOf(0) }
    var mostrarDialogoSaida by remember { mutableStateOf(false) }

    val negociantes by viewModel.negociantes.collectAsState()
    val negociantesCarregados by viewModel.negociantesCarregados.collectAsState()
    val eu = negociantes.firstOrNull { it.usuarioUid == usuarioUid }

    val perfil = if (ehEntregador == true) PerfilUsuario.ENTREGADOR else PerfilUsuario.NEGOCIANTE
    // Conta que não é entregador nem está ligada a um negociante cria o próprio cadastro
    val escolhendoNegociante = perfil == PerfilUsuario.NEGOCIANTE && eu == null

    // Quando um pedido está aberto para roteirização, a tela de rota ocupa a área inteira.
    // Guarda só o id: a tela sempre recebe o pedido atual, com status e destino em dia.
    var pedidoEmRotaId by rememberSaveable { mutableStateOf<Long?>(null) }
    val pedidos by viewModel.pedidos.collectAsState()
    val emRota = pedidos.firstOrNull { it.id == pedidoEmRotaId }

    val abas = when (perfil) {
        PerfilUsuario.NEGOCIANTE -> abasNegociante
        PerfilUsuario.ENTREGADOR -> abasEntregador
    }

    fun sairDaConta() = onLogout()

    val estadoSincronizacao by viewModel.estadoSincronizacao.collectAsState()

    // O aviso de "entregador próximo" é uma notificação: no Android 13+ ela pede permissão
    val pedirNotificacoes = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        NotificacaoEntrega.criarCanal(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificacaoEntrega.podeNotificar(context)) {
            pedirNotificacoes.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "InFraTech · ${if (perfil == PerfilUsuario.NEGOCIANTE) eu?.nome ?: perfil.rotulo else perfil.rotulo}",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    actions = {
                        // Para usar o outro perfil, sai-se da conta: o "Sair" fica escrito
                        TextButton(
                            onClick = { mostrarDialogoSaida = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.semantics { contentDescription = "Sair da conta" }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Sair", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                )
                AvisoSincronizacao(estadoSincronizacao)
            }
        },
        bottomBar = {
            // A tela de rota é imersiva: esconde a navegação até o entregador voltar
            // Com uma aba só, a barra não teria para onde levar
            if (emRota == null && !escolhendoNegociante && abas.size > 1) {
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
                            label = { Text(aba.titulo, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val conteudo = Modifier.padding(innerPadding)

        if (emRota != null) {
            RotaEntregaScreen(
                pedido = emRota,
                viewModel = viewModel,
                onVoltar = { pedidoEmRotaId = null },
                modifier = conteudo
            )
            return@Scaffold
        }

        when (perfil) {
            PerfilUsuario.NEGOCIANTE -> if (eu == null) {
                // Antes da primeira leitura, a escolha piscaria para quem já está ligado
                if (negociantesCarregados && ehEntregador == false) EscolherNegocianteScreen(
                    negociantes = negociantes,
                    emailDaConta = usuarioEmail,
                    onEscolher = { viewModel.vincularConta(it, usuarioUid) },
                    onCriar = { nome, email, telefone ->
                        viewModel.criarNegocianteDaConta(nome, email, telefone, usuarioUid)
                    },
                    modifier = conteudo
                )
            } else key(eu.id) {
                when (abaSelecionada) {
                    0 -> VitrineScreen(
                        viewModel = viewModel,
                        eu = eu,
                        onDesvincular = { viewModel.desvincularConta(eu) },
                        modifier = conteudo
                    )
                    1 -> ProdutoScreen(viewModel, eu, conteudo)
                    2 -> MeusPedidosScreen(viewModel, eu, conteudo)
                    3 -> MapaEntregaScreen(viewModel, eu, conteudo)
                    4 -> AvaliacaoScreen(viewModel, eu, conteudo)
                }
            }

            // O entregador não tem negociante: vê só as entregas
            PerfilUsuario.ENTREGADOR -> EntregasScreen(
                viewModel = viewModel,
                onAbrirRota = { pedido -> pedidoEmRotaId = pedido.id },
                modifier = conteudo
            )
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
                    sairDaConta()
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

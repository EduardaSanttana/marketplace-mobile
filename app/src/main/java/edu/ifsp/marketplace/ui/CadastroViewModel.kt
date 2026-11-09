package edu.ifsp.marketplace.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.ifsp.marketplace.data.AppDatabase
import edu.ifsp.marketplace.data.Avaliacao
import edu.ifsp.marketplace.data.Negociante
import edu.ifsp.marketplace.data.Pedido
import edu.ifsp.marketplace.data.StatusPedido
import edu.ifsp.marketplace.data.Produto
import edu.ifsp.marketplace.data.sync.EstadoSincronizacao
import edu.ifsp.marketplace.data.sync.SyncManager
import edu.ifsp.marketplace.data.sync.observarConectividade
import edu.ifsp.marketplace.util.GeocodificacaoService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class CadastroViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val syncManager = SyncManager(db, viewModelScope)

    val produtos: StateFlow<List<Produto>> = db.produtoDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val negociantes: StateFlow<List<Negociante>> = db.negocianteDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Falso até a primeira leitura do banco: lista vazia antes disso não quer dizer nada. */
    val negociantesCarregados: StateFlow<Boolean> = db.negocianteDao().getAll()
        .map { true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val pedidos: StateFlow<List<Pedido>> = db.pedidoDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val avaliacoes: StateFlow<List<Avaliacao>> = db.avaliacaoDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Verdadeiro enquanto o endereço do ponto marcado no mapa está sendo consultado. */
    private val _buscandoEnderecoDoPonto = MutableStateFlow(false)
    val buscandoEnderecoDoPonto: StateFlow<Boolean> = _buscandoEnderecoDoPonto.asStateFlow()

    private var definicaoDePonto: Job? = null

    private val conectado: StateFlow<Boolean> = observarConectividade(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Mostra na tela o "salvar no aparelho, enviar depois" que o manual pede. */
    val estadoSincronizacao: StateFlow<EstadoSincronizacao> = combine(
        conectado,
        db.sincronizacaoDao().contarPendentes(),
        syncManager.status
    ) { conectado, pendentes, status -> EstadoSincronizacao(conectado, pendentes, status) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EstadoSincronizacao())

    init {
        syncManager.startRealtimeSync()

        viewModelScope.launch {
            conectado.filter { it }.collect {
                resolverEnderecosPendentes()
                syncManager.syncAgora()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncManager.stopRealtimeSync()
    }

    fun cadastrarProduto(
        nome: String,
        descricao: String,
        preco: Double,
        quantidade: Int,
        vendedor: Negociante,
        fotoPath: String? = null
    ) {
        viewModelScope.launch {
            db.produtoDao().insert(
                Produto(
                    nome = nome,
                    descricao = descricao,
                    preco = preco,
                    quantidade = quantidade,
                    negocianteId = vendedor.id,
                    negocianteFirestoreId = vendedor.firestoreId,
                    fotoPath = fotoPath
                )
            )
            syncManager.syncAgora()
        }
    }

    /**
     * Grava a edição de um produto do próprio negociante. A foto antiga sai do
     * aparelho quando é trocada, já que nada mais aponta para ela.
     */
    fun atualizarProduto(
        original: Produto,
        nome: String,
        descricao: String,
        preco: Double,
        quantidade: Int,
        vendedor: Negociante,
        fotoPath: String?
    ) {
        viewModelScope.launch {
            db.produtoDao().update(
                original.copy(
                    nome = nome,
                    descricao = descricao,
                    preco = preco,
                    quantidade = quantidade,
                    negocianteId = vendedor.id,
                    // Quem edita é o dono: a edição também conserta um vínculo antigo
                    negocianteFirestoreId = vendedor.firestoreId ?: original.negocianteFirestoreId,
                    fotoPath = fotoPath,
                    updatedAt = System.currentTimeMillis(),
                    pendingSync = true
                )
            )
            if (original.fotoPath != null && original.fotoPath != fotoPath) {
                File(original.fotoPath).delete()
            }
            syncManager.syncAgora()
        }
    }

    /** Liga a conta logada a um negociante, soltando o que ela tinha antes. */
    fun vincularConta(negociante: Negociante, usuarioUid: String) {
        viewModelScope.launch {
            val agora = System.currentTimeMillis()
            db.negocianteDao().soltarOutrosDoUsuario(usuarioUid, negociante.id, agora)
            db.negocianteDao().definirUsuario(negociante.id, usuarioUid, agora)
            syncManager.syncAgora()
        }
    }

    /** Desfaz o vínculo; "" segue para a nuvem e desfaz nos outros aparelhos também. */
    fun desvincularConta(negociante: Negociante) {
        viewModelScope.launch {
            db.negocianteDao().definirUsuario(negociante.id, "", System.currentTimeMillis())
            syncManager.syncAgora()
        }
    }

    /** O negociante se cadastra pela própria conta, já ligado a ela. */
    fun criarNegocianteDaConta(nome: String, email: String, telefone: String, usuarioUid: String) {
        viewModelScope.launch {
            val agora = System.currentTimeMillis()
            // Uma conta age como um negociante só: solta o que ela tivesse antes
            db.negocianteDao().soltarOutrosDoUsuario(usuarioUid, manterId = -1, updatedAt = agora)
            db.negocianteDao().insert(
                Negociante(nome = nome, email = email, telefone = telefone, usuarioUid = usuarioUid, updatedAt = agora)
            )
            syncManager.syncAgora()
        }
    }

    fun cadastrarAvaliacao(produto: Produto, nota: Int, comentario: String) {
        viewModelScope.launch {
            db.avaliacaoDao().insert(
                Avaliacao(
                    produtoId = produto.id,
                    produtoFirestoreId = produto.firestoreId,
                    nota = nota,
                    comentario = comentario
                )
            )
            syncManager.syncAgora()
        }
    }

    fun excluirProduto(produto: Produto) {
        viewModelScope.launch {
            db.produtoDao().markPendingDelete(produto.id)
            syncManager.syncAgora()
        }
    }

    /**
     * Registra a compra de um produto por um negociante.
     *
     * O destino do pedido é copiado do ponto de entrega já cadastrado pelo
     * comprador. Se ele ainda não marcou o ponto no mapa, o pedido nasce sem
     * coordenadas e a rota fica indisponível até o ponto ser definido, quando
     * [definirPontoEntrega] atualiza os pedidos em aberto.
     *
     * O local de retirada é copiado do mesmo cadastro, agora o do vendedor.
     */
    fun comprarProduto(produto: Produto, comprador: Negociante, quantidade: Int) {
        viewModelScope.launch {
            val vendedor = db.negocianteDao().getById(produto.negocianteId)
            val agora = System.currentTimeMillis()
            db.pedidoDao().insert(
                Pedido(
                    criadoEm = agora,
                    updatedAt = agora,
                    produtoId = produto.id,
                    produtoNome = produto.nome,
                    compradorId = comprador.id,
                    compradorNome = comprador.nome,
                    quantidade = quantidade,
                    valorTotal = produto.preco * quantidade,
                    status = StatusPedido.AGUARDANDO.name,
                    enderecoEntrega = comprador.endereco,
                    latitudeEntrega = comprador.latitude,
                    longitudeEntrega = comprador.longitude,
                    vendedorId = produto.negocianteId,
                    vendedorNome = vendedor?.nome.orEmpty(),
                    enderecoRetirada = vendedor?.endereco.orEmpty(),
                    latitudeRetirada = vendedor?.latitude,
                    longitudeRetirada = vendedor?.longitude,
                    produtoFirestoreId = produto.firestoreId,
                    compradorFirestoreId = comprador.firestoreId,
                    vendedorFirestoreId = vendedor?.firestoreId ?: produto.negocianteFirestoreId
                )
            )
            // Baixa do estoque do vendedor
            val restante = (produto.quantidade - quantidade).coerceAtLeast(0)
            db.produtoDao().update(
                produto.copy(
                    quantidade = restante,
                    updatedAt = System.currentTimeMillis(),
                    pendingSync = true
                )
            )
            syncManager.syncAgora()
        }
    }

    /** O entregador retirou o pacote: o pedido passa a ser rastreável. */
    fun iniciarTransporte(pedido: Pedido) {
        viewModelScope.launch {
            db.pedidoDao().registrarRetirada(pedido.id, System.currentTimeMillis())
            syncManager.syncAgora()
        }
    }

    /**
     * Fecha a entrega com a comprovação exigida pelo manual: foto, coordenadas
     * do local e endereço obtido por geocodificação reversa.
     *
     * A foto e as coordenadas são gravadas antes da consulta do endereço, para
     * que a falta de conexão não impeça o registro: nesse caso o endereço é
     * preenchido por [resolverEnderecosPendentes] quando a conexão voltar.
     */
    fun registrarEntrega(pedidoId: Long, fotoPath: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val agora = System.currentTimeMillis()
            db.pedidoDao().registrarEntrega(
                id = pedidoId,
                status = StatusPedido.ENTREGUE.name,
                fotoPath = fotoPath,
                latitude = latitude,
                longitude = longitude,
                endereco = null,
                entregueEm = agora,
                updatedAt = agora
            )
            GeocodificacaoService.buscar(getApplication(), latitude, longitude)?.let { endereco ->
                db.pedidoDao().atualizarEnderecoEntregue(
                    pedidoId, endereco, System.currentTimeMillis()
                )
            }
            syncManager.syncAgora()
        }
    }

    /**
     * Cancela um pedido que ainda espera a retirada e devolve as unidades ao estoque.
     *
     * O produto é achado pelo id do Firestore; num pedido antigo, sem ele, pelo id
     * local, mas só se o nome confere, para não somar estoque ao produto errado.
     */
    fun cancelarPedido(pedido: Pedido) {
        if (pedido.situacao != StatusPedido.AGUARDANDO) return
        viewModelScope.launch {
            val produto = pedido.produtoFirestoreId
                ?.let { db.produtoDao().getByFirestoreId(it) }
                ?: db.produtoDao().getById(pedido.produtoId)
                    ?.takeIf { pedido.produtoFirestoreId == null && it.nome == pedido.produtoNome }
            produto?.let {
                db.produtoDao().devolverEstoque(it.id, pedido.quantidade, System.currentTimeMillis())
            }
            db.pedidoDao().markPendingDelete(pedido.id)
            syncManager.syncAgora()
        }
    }

    fun excluirPedido(pedido: Pedido) {
        viewModelScope.launch {
            db.pedidoDao().markPendingDelete(pedido.id)
            // A foto da entrega só existe neste aparelho e não serve a mais nada
            pedido.fotoEntregaPath?.let { File(it).delete() }
            syncManager.syncAgora()
        }
    }

    /**
     * Define o ponto de entrega do negociante e o leva para os pedidos em aberto.
     *
     * Sem [endereco] (toque no mapa), o ponto é gravado primeiro e o endereço vem
     * da geocodificação reversa em seguida. Um novo ponto cancela a consulta do
     * anterior, que já não interessa.
     */
    fun definirPontoEntrega(
        negociante: Negociante,
        endereco: String?,
        latitude: Double,
        longitude: Double
    ) {
        definicaoDePonto?.cancel()
        definicaoDePonto = viewModelScope.launch {
            _buscandoEnderecoDoPonto.value = endereco.isNullOrBlank()
            val agora = System.currentTimeMillis()
            db.negocianteDao().atualizarPontoEntrega(
                negociante.id, endereco.orEmpty(), latitude, longitude, agora
            )
            levarEnderecoAosPedidos(negociante, endereco.orEmpty(), latitude, longitude, agora)
            if (endereco.isNullOrBlank()) {
                preencherEnderecoDoPonto(negociante, latitude, longitude)
                _buscandoEnderecoDoPonto.value = false
            }
            syncManager.syncAgora()
        }
    }

    private suspend fun preencherEnderecoDoPonto(
        negociante: Negociante,
        latitude: Double,
        longitude: Double
    ) {
        val endereco = GeocodificacaoService.buscar(getApplication(), latitude, longitude) ?: return
        val agora = System.currentTimeMillis()
        val atualizado = db.negocianteDao().preencherEnderecoDoPonto(
            negociante.id, endereco, latitude, longitude, agora
        )
        if (atualizado > 0) {
            levarEnderecoAosPedidos(negociante, endereco, latitude, longitude, agora)
        }
    }

    /** O endereço do negociante vale como destino das compras e como retirada das vendas em aberto. */
    private suspend fun levarEnderecoAosPedidos(
        negociante: Negociante,
        endereco: String,
        latitude: Double,
        longitude: Double,
        agora: Long
    ) {
        db.pedidoDao().atualizarDestinoEmAberto(
            negociante.firestoreId, negociante.id, negociante.nome, endereco, latitude, longitude, agora
        )
        db.pedidoDao().atualizarRetiradaEmAberto(
            negociante.firestoreId, negociante.id, negociante.nome, endereco, latitude, longitude, agora
        )
    }

    /** Corrige o cadastro de um negociante; os pedidos já feitos guardam o nome da época. */
    fun atualizarNegociante(negociante: Negociante, nome: String, email: String, telefone: String) {
        viewModelScope.launch {
            db.negocianteDao().atualizarDados(
                negociante.id, nome, email, telefone, System.currentTimeMillis()
            )
            syncManager.syncAgora()
        }
    }

    /** Completa os endereços que ficaram para trás por falta de conexão. */
    private suspend fun resolverEnderecosPendentes() {
        for (negociante in db.negocianteDao().getPontosSemEndereco()) {
            val latitude = negociante.latitude ?: continue
            val longitude = negociante.longitude ?: continue
            preencherEnderecoDoPonto(negociante, latitude, longitude)
        }
        for (pedido in db.pedidoDao().getEntreguesSemEndereco()) {
            val latitude = pedido.latitudeEntregue ?: continue
            val longitude = pedido.longitudeEntregue ?: continue
            val endereco = GeocodificacaoService.buscar(getApplication(), latitude, longitude)
                ?: continue
            db.pedidoDao().atualizarEnderecoEntregue(
                pedido.id, endereco, System.currentTimeMillis()
            )
        }
    }
}

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
import edu.ifsp.marketplace.data.sync.SyncManager
import edu.ifsp.marketplace.data.sync.observarConectividade
import edu.ifsp.marketplace.util.GeocodificacaoService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CadastroViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val syncManager = SyncManager(db, viewModelScope)

    val produtos: StateFlow<List<Produto>> = db.produtoDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val negociantes: StateFlow<List<Negociante>> = db.negocianteDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pedidos: StateFlow<List<Pedido>> = db.pedidoDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val avaliacoes: StateFlow<List<Avaliacao>> = db.avaliacaoDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Verdadeiro enquanto o endereço do ponto marcado no mapa está sendo consultado. */
    private val _buscandoEnderecoDoPonto = MutableStateFlow(false)
    val buscandoEnderecoDoPonto: StateFlow<Boolean> = _buscandoEnderecoDoPonto.asStateFlow()

    private var definicaoDePonto: Job? = null

    init {
        syncManager.startRealtimeSync()

        viewModelScope.launch {
            observarConectividade(application).filter { conectado -> conectado }.collect {
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
        negocianteId: Long,
        fotoPath: String? = null
    ) {
        viewModelScope.launch {
            db.produtoDao().insert(
                Produto(
                    nome = nome,
                    descricao = descricao,
                    preco = preco,
                    quantidade = quantidade,
                    negocianteId = negocianteId,
                    fotoPath = fotoPath
                )
            )
            syncManager.syncAgora()
        }
    }

    fun cadastrarNegociante(nome: String, email: String, telefone: String) {
        viewModelScope.launch {
            db.negocianteDao().insert(
                Negociante(nome = nome, email = email, telefone = telefone)
            )
            syncManager.syncAgora()
        }
    }

    fun cadastrarAvaliacao(produtoId: Long, nota: Int, comentario: String) {
        viewModelScope.launch {
            db.avaliacaoDao().insert(
                Avaliacao(produtoId = produtoId, nota = nota, comentario = comentario)
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
     */
    fun comprarProduto(produto: Produto, comprador: Negociante, quantidade: Int) {
        viewModelScope.launch {
            db.pedidoDao().insert(
                Pedido(
                    produtoId = produto.id,
                    produtoNome = produto.nome,
                    compradorId = comprador.id,
                    compradorNome = comprador.nome,
                    quantidade = quantidade,
                    valorTotal = produto.preco * quantidade,
                    status = StatusPedido.AGUARDANDO.name,
                    enderecoEntrega = comprador.endereco,
                    latitudeEntrega = comprador.latitude,
                    longitudeEntrega = comprador.longitude
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
            db.pedidoDao().atualizarStatus(
                pedido.id, StatusPedido.EM_TRANSPORTE.name, System.currentTimeMillis()
            )
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

    fun excluirPedido(pedido: Pedido) {
        viewModelScope.launch {
            db.pedidoDao().markPendingDelete(pedido.id)
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
            db.pedidoDao().atualizarDestinoEmAberto(
                negociante.id, negociante.nome, endereco.orEmpty(), latitude, longitude, agora
            )
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
            db.pedidoDao().atualizarDestinoEmAberto(
                negociante.id, negociante.nome, endereco, latitude, longitude, agora
            )
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

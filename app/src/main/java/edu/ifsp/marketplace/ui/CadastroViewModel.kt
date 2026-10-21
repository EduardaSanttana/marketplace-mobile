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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    init {
        syncManager.startRealtimeSync()

        viewModelScope.launch {
            observarConectividade(application).filter { conectado -> conectado }.collect {
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
     * coordenadas e o entregador não consegue gerar a rota — por isso a tela
     * de compra avisa quando isso acontece.
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
     */
    fun registrarEntrega(
        pedido: Pedido,
        fotoPath: String?,
        latitude: Double?,
        longitude: Double?,
        endereco: String?
    ) {
        viewModelScope.launch {
            val agora = System.currentTimeMillis()
            db.pedidoDao().registrarEntrega(
                id = pedido.id,
                status = StatusPedido.ENTREGUE.name,
                fotoPath = fotoPath,
                latitude = latitude,
                longitude = longitude,
                endereco = endereco,
                entregueEm = agora,
                updatedAt = agora
            )
            syncManager.syncAgora()
        }
    }

    fun excluirPedido(pedido: Pedido) {
        viewModelScope.launch {
            db.pedidoDao().markPendingDelete(pedido.id)
            syncManager.syncAgora()
        }
    }

    fun definirPontoEntrega(negociante: Negociante, endereco: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            db.negocianteDao().atualizarPontoEntrega(
                negociante.id, endereco, latitude, longitude, System.currentTimeMillis()
            )
            syncManager.syncAgora()
        }
    }
}

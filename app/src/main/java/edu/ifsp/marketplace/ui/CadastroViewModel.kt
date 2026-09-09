package edu.ifsp.marketplace.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.ifsp.marketplace.data.AppDatabase
import edu.ifsp.marketplace.data.Avaliacao
import edu.ifsp.marketplace.data.Negociante
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

    fun cadastrarProduto(nome: String, descricao: String, preco: Double, quantidade: Int, negocianteId: Long) {
        viewModelScope.launch {
            db.produtoDao().insert(
                Produto(
                    nome = nome,
                    descricao = descricao,
                    preco = preco,
                    quantidade = quantidade,
                    negocianteId = negocianteId
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
}

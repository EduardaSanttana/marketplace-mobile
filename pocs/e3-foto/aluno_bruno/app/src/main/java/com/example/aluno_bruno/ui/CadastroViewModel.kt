package com.example.aluno_bruno.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aluno_bruno.data.AppDatabase
import com.example.aluno_bruno.data.Avaliacao
import com.example.aluno_bruno.data.Negociante
import com.example.aluno_bruno.data.Produto
import com.example.aluno_bruno.data.sync.SyncManager
import com.example.aluno_bruno.data.sync.SyncStatus
import com.example.aluno_bruno.data.sync.observarConectividade
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

    val syncStatus: StateFlow<SyncStatus> = syncManager.status
    val ultimaSincronizacao: StateFlow<Long?> = syncManager.ultimaSincronizacao

    init {
        // Nuvem -> SQLite: assina mudanças em tempo real assim que o app abre.
        syncManager.startRealtimeSync()

        // SQLite -> nuvem: sempre que a conectividade voltar, envia o que ficou pendente offline.
        viewModelScope.launch {
            observarConectividade(application).filter { conectado -> conectado }.collect {
                syncManager.syncAgora()
            }
        }
    }

    fun sincronizarAgora() {
        viewModelScope.launch {
            syncManager.syncAgora()
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
}

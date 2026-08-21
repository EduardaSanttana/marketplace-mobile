package com.example.aluno_melissaa.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aluno_melissaa.data.AppDatabase
import com.example.aluno_melissaa.data.Avaliacao
import com.example.aluno_melissaa.data.Negociante
import com.example.aluno_melissaa.data.Produto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CadastroViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    val produtos: StateFlow<List<Produto>> = db.produtoDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val negociantes: StateFlow<List<Negociante>> = db.negocianteDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val avaliacoes: StateFlow<List<Avaliacao>> = db.avaliacaoDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        }
    }

    fun cadastrarNegociante(nome: String, email: String, telefone: String) {
        viewModelScope.launch {
            db.negocianteDao().insert(
                Negociante(nome = nome, email = email, telefone = telefone)
            )
        }
    }

    fun cadastrarAvaliacao(produtoId: Long, nota: Int, comentario: String) {
        viewModelScope.launch {
            db.avaliacaoDao().insert(
                Avaliacao(produtoId = produtoId, nota = nota, comentario = comentario)
            )
        }
    }
}

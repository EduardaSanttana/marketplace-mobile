package com.example.aluno_melissaa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produtos")
data class Produto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String = "",
    val descricao: String = "",
    val preco: Double = 0.0,
    val quantidade: Int = 0,
    val negocianteId: Long = 0,
    val firestoreId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val pendingSync: Boolean = true,
    val pendingDelete: Boolean = false
)

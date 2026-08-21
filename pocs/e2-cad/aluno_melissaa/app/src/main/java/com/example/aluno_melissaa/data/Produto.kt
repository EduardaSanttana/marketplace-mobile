package com.example.aluno_melissaa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produtos")
data class Produto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val descricao: String,
    val preco: Double,
    val quantidade: Int,
    val negocianteId: Long
)

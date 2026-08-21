package com.example.aluno_melissaa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "avaliacoes")
data class Avaliacao(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val produtoId: Long,
    val nota: Int,
    val comentario: String
)

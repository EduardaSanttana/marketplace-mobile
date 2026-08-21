package com.example.aluno_melissaa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "negociantes")
data class Negociante(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val email: String,
    val telefone: String
)

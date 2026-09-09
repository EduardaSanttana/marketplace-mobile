package edu.ifsp.marketplace.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "avaliacoes")
data class Avaliacao(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val produtoId: Long = 0,
    val nota: Int = 0,
    val comentario: String = "",
    val firestoreId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val pendingSync: Boolean = true,
    val pendingDelete: Boolean = false
)

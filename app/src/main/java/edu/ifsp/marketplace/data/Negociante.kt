package edu.ifsp.marketplace.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "negociantes")
data class Negociante(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String = "",
    val email: String = "",
    val telefone: String = "",
    val endereco: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    // Conta do Firebase Auth que age como este negociante. Vazio ("") desfaz o
    // vínculo; null só significa que este aparelho ainda não sabe de nenhum
    val usuarioUid: String? = null,
    val firestoreId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val pendingSync: Boolean = true,
    val pendingDelete: Boolean = false
) {
    val temConta: Boolean get() = !usuarioUid.isNullOrBlank()
}

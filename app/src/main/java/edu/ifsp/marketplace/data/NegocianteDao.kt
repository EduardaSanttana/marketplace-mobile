package edu.ifsp.marketplace.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NegocianteDao {
    @Insert
    suspend fun insert(negociante: Negociante): Long

    @Update
    suspend fun update(negociante: Negociante)

    @Delete
    suspend fun delete(negociante: Negociante)

    @Query("SELECT * FROM negociantes ORDER BY id DESC")
    fun getAll(): Flow<List<Negociante>>

    @Query("SELECT * FROM negociantes WHERE pendingSync = 1 AND pendingDelete = 0")
    suspend fun getPendingSync(): List<Negociante>

    @Query("SELECT * FROM negociantes WHERE pendingDelete = 1")
    suspend fun getPendingDelete(): List<Negociante>

    @Query("SELECT * FROM negociantes WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getByFirestoreId(firestoreId: String): Negociante?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFromRemote(negociante: Negociante): Long

    @Query("DELETE FROM negociantes WHERE id = :id")
    suspend fun hardDeleteById(id: Long)

    @Query("DELETE FROM negociantes WHERE firestoreId = :firestoreId")
    suspend fun hardDeleteByFirestoreId(firestoreId: String)

    @Query("UPDATE negociantes SET pendingDelete = 1, pendingSync = 1 WHERE id = :id")
    suspend fun markPendingDelete(id: Long)

    @Query("UPDATE negociantes SET endereco = :endereco, latitude = :latitude, longitude = :longitude, updatedAt = :updatedAt, pendingSync = 1 WHERE id = :id")
    suspend fun atualizarPontoEntrega(id: Long, endereco: String, latitude: Double, longitude: Double, updatedAt: Long)

    /** Pontos marcados sem conexão: já têm coordenadas, falta o endereço. */
    @Query("SELECT * FROM negociantes WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND endereco = '' AND pendingDelete = 0")
    suspend fun getPontosSemEndereco(): List<Negociante>

    /**
     * Grava o endereço de um ponto, a menos que o pino já tenha sido movido.
     *
     * @return quantas linhas mudaram: zero indica que o endereço ficou obsoleto
     */
    @Query("UPDATE negociantes SET endereco = :endereco, updatedAt = :updatedAt, pendingSync = 1 WHERE id = :id AND latitude = :latitude AND longitude = :longitude")
    suspend fun preencherEnderecoDoPonto(id: Long, endereco: String, latitude: Double, longitude: Double, updatedAt: Long): Int

    /** Só libera a pendência se o negociante não mudou enquanto era enviado à nuvem. */
    @Query("UPDATE negociantes SET firestoreId = :firestoreId, pendingSync = CASE WHEN updatedAt = :updatedAt THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun marcarSincronizado(id: Long, firestoreId: String, updatedAt: Long)
}

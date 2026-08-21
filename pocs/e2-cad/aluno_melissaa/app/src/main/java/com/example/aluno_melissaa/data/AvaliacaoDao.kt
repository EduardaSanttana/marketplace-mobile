package com.example.aluno_melissaa.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AvaliacaoDao {
    @Insert
    suspend fun insert(avaliacao: Avaliacao): Long

    @Delete
    suspend fun delete(avaliacao: Avaliacao)

    @Query("SELECT * FROM avaliacoes ORDER BY id DESC")
    fun getAll(): Flow<List<Avaliacao>>
}

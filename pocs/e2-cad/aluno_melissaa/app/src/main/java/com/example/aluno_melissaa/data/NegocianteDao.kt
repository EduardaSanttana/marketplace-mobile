package com.example.aluno_melissaa.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NegocianteDao {
    @Insert
    suspend fun insert(negociante: Negociante): Long

    @Delete
    suspend fun delete(negociante: Negociante)

    @Query("SELECT * FROM negociantes ORDER BY id DESC")
    fun getAll(): Flow<List<Negociante>>
}

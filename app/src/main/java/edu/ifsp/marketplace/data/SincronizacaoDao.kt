package edu.ifsp.marketplace.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SincronizacaoDao {

    /** Alterações gravadas no aparelho que ainda não chegaram à nuvem, somadas as quatro tabelas. */
    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM produtos WHERE pendingSync = 1) +
            (SELECT COUNT(*) FROM negociantes WHERE pendingSync = 1) +
            (SELECT COUNT(*) FROM avaliacoes WHERE pendingSync = 1) +
            (SELECT COUNT(*) FROM pedidos WHERE pendingSync = 1 OR pendingDelete = 1)
        """
    )
    fun contarPendentes(): Flow<Int>
}

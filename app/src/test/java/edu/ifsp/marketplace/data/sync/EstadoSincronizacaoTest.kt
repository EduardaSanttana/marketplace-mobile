package edu.ifsp.marketplace.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EstadoSincronizacaoTest {

    @Test
    fun `tudo enviado e com conexao nao mostra aviso`() {
        assertNull(EstadoSincronizacao(conectado = true, pendentes = 0).aviso)
    }

    @Test
    fun `sem conexao conta o que espera envio`() {
        val estado = EstadoSincronizacao(conectado = false, pendentes = 3)
        assertEquals("Sem conexão · 3 alterações salvas no aparelho, aguardando envio", estado.aviso)
        assertTrue(estado.problema)
    }

    @Test
    fun `uma alteracao fica no singular`() {
        assertEquals(
            "Enviando 1 alteração para a nuvem...",
            EstadoSincronizacao(conectado = true, pendentes = 1, status = SyncStatus.SINCRONIZANDO).aviso
        )
    }

    @Test
    fun `falha com pendencias pede atencao`() {
        val estado = EstadoSincronizacao(conectado = true, pendentes = 2, status = SyncStatus.ERRO)
        assertTrue(estado.problema)
    }

    @Test
    fun `enviando nao e problema`() {
        assertFalse(EstadoSincronizacao(conectado = true, pendentes = 2).problema)
    }
}

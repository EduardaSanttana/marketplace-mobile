package edu.ifsp.marketplace.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class VinculosTest {

    @Test
    fun `o id do Firestore manda quando o registro ja esta aqui`() {
        // O aparelho de origem usava o id 1; aqui o mesmo negociante tem o id 3
        assertEquals(3L, idLocalDoVinculo("abc", idPeloFirestore = 3L, idLocalAtual = 1L, idRemoto = 1L))
    }

    @Test
    fun `registro ainda nao chegou fica sem id ate religar`() {
        assertEquals(0L, idLocalDoVinculo("abc", idPeloFirestore = null, idLocalAtual = 7L, idRemoto = 1L))
    }

    @Test
    fun `documento antigo mantem o que este aparelho ja sabia`() {
        assertEquals(7L, idLocalDoVinculo(null, idPeloFirestore = null, idLocalAtual = 7L, idRemoto = 1L))
    }

    @Test
    fun `documento antigo e novo aqui usa o id de origem como antes`() {
        assertEquals(1L, idLocalDoVinculo(null, idPeloFirestore = null, idLocalAtual = null, idRemoto = 1L))
    }
}

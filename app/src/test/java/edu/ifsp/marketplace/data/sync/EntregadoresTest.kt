package edu.ifsp.marketplace.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntregadoresTest {

    @Test
    fun `id e o e-mail em minusculas e sem espacos`() {
        assertEquals("entregador.teste@gmail.com", idDoEntregador("  Entregador.Teste@Gmail.com "))
    }

    @Test
    fun `conta sem e-mail nao tem id`() {
        assertNull(idDoEntregador(null))
        assertNull(idDoEntregador("   "))
    }
}

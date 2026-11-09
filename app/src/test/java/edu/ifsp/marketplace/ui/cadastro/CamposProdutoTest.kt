package edu.ifsp.marketplace.ui.cadastro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CamposProdutoTest {

    @Test
    fun `preco aceita virgula e ponto`() {
        assertEquals(15.9, lerPreco("15,90")!!, 0.001)
        assertEquals(15.9, lerPreco(" 15.90 ")!!, 0.001)
    }

    @Test
    fun `preco zero negativo ou texto e invalido`() {
        assertNull(lerPreco("0"))
        assertNull(lerPreco("-3"))
        assertNull(lerPreco("abc"))
    }

    @Test
    fun `email vazio e opcional e formato basico e exigido`() {
        assertTrue(emailValido(""))
        assertTrue(emailValido("melissa@gmail.com"))
        assertFalse(emailValido("melissa@gmail"))
        assertFalse(emailValido("meli ssa@gmail.com"))
    }

    @Test
    fun `quantidade aceita zero e recusa negativa ou fracionada`() {
        assertEquals(0, lerQuantidade("0"))
        assertNull(lerQuantidade("-1"))
        assertNull(lerQuantidade("2,5"))
    }
}

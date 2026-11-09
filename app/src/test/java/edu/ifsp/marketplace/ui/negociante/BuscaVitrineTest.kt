package edu.ifsp.marketplace.ui.negociante

import edu.ifsp.marketplace.data.Produto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuscaVitrineTest {

    private val caneta = Produto(nome = "Caneta", descricao = "Azul, ponta fina")

    @Test
    fun `busca vazia mostra tudo`() {
        assertTrue(produtoCombinaComBusca(caneta, "Bruno Posar", "  "))
    }

    @Test
    fun `ignora maiusculas e acentos`() {
        assertTrue(produtoCombinaComBusca(caneta, null, "CANÉTA"))
    }

    @Test
    fun `procura na descricao e no vendedor`() {
        assertTrue(produtoCombinaComBusca(caneta, null, "ponta"))
        assertTrue(produtoCombinaComBusca(caneta, "Bruno Posar", "bruno"))
    }

    @Test
    fun `termo ausente nao combina`() {
        assertFalse(produtoCombinaComBusca(caneta, "Bruno Posar", "caderno"))
    }
}

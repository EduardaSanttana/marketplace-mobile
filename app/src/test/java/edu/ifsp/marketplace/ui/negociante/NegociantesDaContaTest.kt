package edu.ifsp.marketplace.ui.negociante

import edu.ifsp.marketplace.data.Negociante
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NegociantesDaContaTest {

    private val melissa = Negociante(id = 1, nome = "Melissa", email = "melissa@exemplo.com")
    private val bruno = Negociante(id = 2, nome = "Bruno", email = "bruno@exemplo.com", usuarioUid = "uid-bruno")
    private val todos = listOf(melissa, bruno)

    @Test
    fun `mostra so o cadastro com o e-mail da conta`() {
        assertEquals(listOf(melissa), negociantesDaConta(todos, "melissa@exemplo.com"))
    }

    @Test
    fun `nao mostra cadastro de outra pessoa`() {
        assertTrue(negociantesDaConta(todos, "outra@exemplo.com").isEmpty())
    }

    @Test
    fun `ignora maiusculas e espacos no e-mail`() {
        assertEquals(listOf(melissa), negociantesDaConta(todos, "  MELISSA@Exemplo.com "))
    }

    @Test
    fun `cadastro ja ligado a uma conta nao aparece`() {
        assertTrue(negociantesDaConta(todos, "bruno@exemplo.com").isEmpty())
    }

    @Test
    fun `conta sem e-mail nao ve ninguem`() {
        assertTrue(negociantesDaConta(todos, null).isEmpty())
        assertTrue(negociantesDaConta(todos, " ").isEmpty())
    }

    @Test
    fun `cadastro desvinculado volta a aparecer para o dono`() {
        val desvinculado = bruno.copy(usuarioUid = "")
        assertEquals(listOf(desvinculado), negociantesDaConta(listOf(desvinculado), "bruno@exemplo.com"))
    }
}

package edu.ifsp.marketplace.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PedidoTest {

    private val comRetirada = Pedido(latitudeRetirada = -21.79, longitudeRetirada = -48.17)
    private val comDestino = Pedido(latitudeEntrega = -22.56, longitudeEntrega = -47.40)

    @Test
    fun `rota vai ate a retirada so antes de retirar o pacote`() {
        assertTrue(Pedido(status = StatusPedido.AGUARDANDO.name).rotaAteRetirada)
        assertFalse(Pedido(status = StatusPedido.EM_TRANSPORTE.name).rotaAteRetirada)
    }

    @Test
    fun `aguardando abre a rota so com o ponto de retirada`() {
        assertTrue(comRetirada.podeAbrirRota)
    }

    @Test
    fun `depois de retirar o ponto de retirada nao basta`() {
        assertFalse(comRetirada.copy(status = StatusPedido.EM_TRANSPORTE.name).podeAbrirRota)
    }

    @Test
    fun `sem nenhum ponto a rota fica indisponivel`() {
        assertFalse(Pedido().podeAbrirRota)
    }

    @Test
    fun `com destino a rota abre em transporte`() {
        assertTrue(comDestino.copy(status = StatusPedido.EM_TRANSPORTE.name).podeAbrirRota)
    }

    @Test
    fun `perto do destino a entrega nao pede confirmacao`() {
        // Cerca de 110 m ao norte do ponto marcado
        assertFalse(comDestino.longeDoDestino(-22.559, -47.40))
    }

    @Test
    fun `longe do destino a entrega pede confirmacao`() {
        // Cerca de 1,1 km ao norte do ponto marcado
        assertTrue(comDestino.longeDoDestino(-22.55, -47.40))
    }

    @Test
    fun `cada etapa tem o seu horario`() {
        val pedido = Pedido(criadoEm = 1L, retiradoEm = 2L, entregueEm = 3L)
        assertEquals(1L, pedido.horarioDa(StatusPedido.AGUARDANDO))
        assertEquals(2L, pedido.horarioDa(StatusPedido.EM_TRANSPORTE))
        assertEquals(3L, pedido.horarioDa(StatusPedido.ENTREGUE))
    }

    @Test
    fun `sem destino marcado nao ha o que comparar`() {
        assertFalse(Pedido().longeDoDestino(-22.55, -47.40))
    }
}

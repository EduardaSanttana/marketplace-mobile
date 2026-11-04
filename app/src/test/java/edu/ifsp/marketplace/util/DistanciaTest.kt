package edu.ifsp.marketplace.util

import edu.ifsp.marketplace.data.Pedido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistanciaTest {

    @Test
    fun `mesmo ponto tem distancia zero`() {
        assertEquals(0.0, distanciaEmMetros(-23.5505, -46.6333, -23.5505, -46.6333), 0.001)
    }

    @Test
    fun `um grau de latitude mede cerca de 111 km`() {
        assertEquals(111_195.0, distanciaEmMetros(-23.0, -46.0, -24.0, -46.0), 50.0)
    }

    @Test
    fun `formata metros sem casas decimais`() {
        assertEquals("850 m", formatarDistancia(850.7))
    }

    @Test
    fun `pedido compara o local da entrega com o destino`() {
        val pedido = Pedido(
            latitudeEntrega = -23.0,
            longitudeEntrega = -46.0,
            latitudeEntregue = -24.0,
            longitudeEntregue = -46.0
        )
        assertEquals(111_195.0, pedido.distanciaDoDestinoMetros!!, 50.0)
    }

    @Test
    fun `pedido sem destino ou sem entrega nao tem distancia`() {
        assertNull(Pedido(latitudeEntregue = -24.0, longitudeEntregue = -46.0).distanciaDoDestinoMetros)
        assertNull(Pedido(latitudeEntrega = -23.0, longitudeEntrega = -46.0).distanciaDoDestinoMetros)
    }
}

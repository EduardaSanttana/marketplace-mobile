package edu.ifsp.marketplace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import edu.ifsp.marketplace.util.distanciaEmMetros

/**
 * Situação de um pedido dentro do marketplace.
 *
 * O fluxo é sempre AGUARDANDO -> EM_TRANSPORTE -> ENTREGUE:
 * o negociante compra, o entregador retira e, por fim, registra a entrega.
 */
enum class StatusPedido(val rotulo: String) {
    AGUARDANDO("Aguardando retirada"),
    EM_TRANSPORTE("Em transporte"),
    ENTREGUE("Entregue");

    companion object {
        fun de(valor: String?): StatusPedido =
            entries.firstOrNull { it.name == valor } ?: AGUARDANDO
    }
}

/**
 * Um pedido feito por um negociante no marketplace.
 *
 * O nome do produto e o do comprador são copiados para cá no momento da compra.
 * Isso é proposital: se o produto for excluído depois, o histórico do pedido
 * continua legível.
 *
 * Os campos de entrega são preenchidos pelo perfil Entregador no momento em que
 * ele registra a entrega — foto, coordenadas e endereço obtido por geocodificação.
 */
@Entity(tableName = "pedidos")
data class Pedido(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val produtoId: Long = 0,
    val produtoNome: String = "",
    val compradorId: Long = 0,
    val compradorNome: String = "",

    // Os ids locais só valem neste aparelho; os do Firestore valem em todos
    // e corrigem os locais quando o pedido chega de outro aparelho
    val produtoFirestoreId: String? = null,
    val compradorFirestoreId: String? = null,
    val vendedorFirestoreId: String? = null,

    val quantidade: Int = 1,
    val valorTotal: Double = 0.0,
    val status: String = StatusPedido.AGUARDANDO.name,

    // Destino informado pelo negociante na aba de ponto de entrega
    val enderecoEntrega: String = "",
    val latitudeEntrega: Double? = null,
    val longitudeEntrega: Double? = null,

    // Onde o entregador retira o pacote: o endereço do vendedor
    val vendedorId: Long = 0,
    val vendedorNome: String = "",
    val enderecoRetirada: String = "",
    val latitudeRetirada: Double? = null,
    val longitudeRetirada: Double? = null,

    // Horários da linha do tempo; pedidos de antes deste campo ficam sem eles
    val criadoEm: Long? = null,
    val retiradoEm: Long? = null,

    // Comprovação registrada pelo entregador
    val fotoEntregaPath: String? = null,
    val latitudeEntregue: Double? = null,
    val longitudeEntregue: Double? = null,
    val enderecoEntregue: String? = null,
    val entregueEm: Long? = null,

    val firestoreId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val pendingSync: Boolean = true,
    val pendingDelete: Boolean = false
) {
    val situacao: StatusPedido get() = StatusPedido.de(status)

    /** Quando o pedido chegou a cada etapa, se este aparelho souber. */
    fun horarioDa(etapa: StatusPedido): Long? = when (etapa) {
        StatusPedido.AGUARDANDO -> criadoEm
        StatusPedido.EM_TRANSPORTE -> retiradoEm
        StatusPedido.ENTREGUE -> entregueEm
    }

    /** O pedido só pode ser roteirizado se o negociante já marcou o destino. */
    val temDestino: Boolean get() = latitudeEntrega != null && longitudeEntrega != null

    val temRetirada: Boolean get() = latitudeRetirada != null && longitudeRetirada != null

    /** Antes da retirada, a rota leva ao vendedor; depois, ao comprador. */
    val rotaAteRetirada: Boolean get() = situacao == StatusPedido.AGUARDANDO

    /** Há um ponto para roteirizar agora, ou um que a retirada vai liberar. */
    val podeAbrirRota: Boolean get() = temDestino || (rotaAteRetirada && temRetirada)

    /** Distância entre onde a entrega foi registrada e o ponto marcado pelo comprador. */
    val distanciaDoDestinoMetros: Double?
        get() {
            val latEntregue = latitudeEntregue ?: return null
            val lngEntregue = longitudeEntregue ?: return null
            return distanciaAteDestinoMetros(latEntregue, lngEntregue)
        }

    /** Distância de uma posição até o ponto marcado pelo comprador, se houver um. */
    fun distanciaAteDestinoMetros(latitude: Double, longitude: Double): Double? {
        val latDestino = latitudeEntrega ?: return null
        val lngDestino = longitudeEntrega ?: return null
        return distanciaEmMetros(latDestino, lngDestino, latitude, longitude)
    }

    /** O entregador está tão longe do destino que a entrega pede confirmação. */
    fun longeDoDestino(latitude: Double, longitude: Double): Boolean =
        (distanciaAteDestinoMetros(latitude, longitude) ?: 0.0) > DISTANCIA_PARA_CONFIRMAR_METROS

    companion object {
        /** Folga para a imprecisão do GPS e para o ponto marcado na calçada errada. */
        const val DISTANCIA_PARA_CONFIRMAR_METROS = 300.0
    }
}

package edu.ifsp.marketplace.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val quantidade: Int = 1,
    val valorTotal: Double = 0.0,
    val status: String = StatusPedido.AGUARDANDO.name,

    // Destino informado pelo negociante na aba de ponto de entrega
    val enderecoEntrega: String = "",
    val latitudeEntrega: Double? = null,
    val longitudeEntrega: Double? = null,

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

    /** O pedido só pode ser roteirizado se o negociante já marcou o destino. */
    val temDestino: Boolean get() = latitudeEntrega != null && longitudeEntrega != null
}

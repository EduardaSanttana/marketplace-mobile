package edu.ifsp.marketplace.data.sync

/**
 * O que a interface precisa saber da sincronização: se há conexão, quantas
 * alterações gravadas no aparelho ainda não chegaram à nuvem e como foi o último envio.
 */
data class EstadoSincronizacao(
    val conectado: Boolean = true,
    val pendentes: Int = 0,
    val status: SyncStatus = SyncStatus.OCIOSO
) {
    /** Texto do aviso; null quando está tudo na nuvem e não há o que avisar. */
    val aviso: String?
        get() {
            val alteracoes = if (pendentes == 1) "1 alteração" else "$pendentes alterações"
            return when {
                !conectado && pendentes > 0 -> "Sem conexão · $alteracoes salvas no aparelho, aguardando envio"
                !conectado -> "Sem conexão · o que você fizer fica salvo no aparelho"
                pendentes == 0 -> null
                status == SyncStatus.ERRO -> "Falha ao enviar $alteracoes · tenta de novo ao reconectar"
                else -> "Enviando $alteracoes para a nuvem..."
            }
        }

    /** Sem conexão ou com falha, o aviso pede atenção; enviando, é só informativo. */
    val problema: Boolean get() = !conectado || (pendentes > 0 && status == SyncStatus.ERRO)
}

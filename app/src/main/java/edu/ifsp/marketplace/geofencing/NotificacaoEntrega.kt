package edu.ifsp.marketplace.geofencing

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import edu.ifsp.marketplace.MainActivity
import edu.ifsp.marketplace.R
import edu.ifsp.marketplace.data.AppDatabase
import edu.ifsp.marketplace.data.Pedido
import edu.ifsp.marketplace.data.StatusPedido

/**
 * Notificação "o entregador está chegando", mostrada no aparelho do comprador.
 *
 * O sinal sai do aparelho do entregador e chega pelo Firestore: o listener de
 * pedidos percebe `entregadorProximoEm` preenchido e chama [avisarSeForComprador].
 * Assim não é preciso Firebase Cloud Messaging, que exigiria um servidor.
 */
object NotificacaoEntrega {

    private const val CANAL = "entregador_proximo"

    /** Um aviso velho (aparelho que ficou dias offline) já não interessa. */
    private const val VALIDADE_MS = 30 * 60 * 1000L

    fun criarCanal(context: Context) {
        val canal = NotificationChannel(
            CANAL,
            "Entregador próximo",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisa quando o entregador está perto do seu endereço de entrega"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun podeNotificar(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    /** A conta logada neste aparelho é a do comprador do pedido? */
    private suspend fun contaEhDoComprador(db: AppDatabase, pedido: Pedido): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val comprador = pedido.compradorFirestoreId
            ?.let { db.negocianteDao().getByFirestoreId(it) }
            ?: db.negocianteDao().getById(pedido.compradorId)
        return comprador?.usuarioUid == uid
    }

    suspend fun avisarSeForComprador(context: Context, db: AppDatabase, pedido: Pedido) {
        val chegada = pedido.entregadorProximoEm ?: return
        if (pedido.situacao != StatusPedido.EM_TRANSPORTE) return
        if (System.currentTimeMillis() - chegada > VALIDADE_MS) return
        if (!contaEhDoComprador(db, pedido)) return
        mostrar(context, pedido)
    }

    private fun mostrar(context: Context, pedido: Pedido) {
        if (!podeNotificar(context)) return
        criarCanal(context)

        val abrirApp = PendingIntent.getActivity(
            context, pedido.id.toInt(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val raio = Pedido.RAIO_ENTREGADOR_PROXIMO_METROS.toInt()
        val notificacao = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacao_entrega)
            .setContentTitle("Seu entregador está chegando")
            .setContentText("${pedido.produtoNome}: a menos de $raio m do seu endereço")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "O entregador do pedido de ${pedido.produtoNome} está a menos de $raio m de " +
                        pedido.enderecoEntrega.ifBlank { "seu ponto de entrega" } + "."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(pedido.id.toInt(), notificacao)
        } catch (e: SecurityException) {
            // Permissão revogada entre a checagem e o envio
        }
    }
}

package edu.ifsp.marketplace.poc.geofencing

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Recebe do Play Services as transições do perímetro, mesmo com a PoC fechada.
 * Cada evento vira uma notificação e uma linha no histórico mostrado na tela.
 */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val evento = GeofencingEvent.fromIntent(intent) ?: return
        if (evento.hasError()) {
            Historico.adicionar(context, "Erro do geofence: " + GeofenceStatusCodes.getStatusCodeString(evento.errorCode))
            return
        }
        val texto = when (evento.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "Entregador próximo: entrou no perímetro"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "Entregador saiu do perímetro"
            else -> return
        }
        Historico.adicionar(context, texto)
        notificar(context, texto)
    }

    private fun notificar(context: Context, texto: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CANAL, "Geofencing", NotificationManager.IMPORTANCE_HIGH)
        )
        val notificacao = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("PoC Geofencing")
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notificacao)
    }

    companion object {
        const val CANAL = "geofencing"
    }
}

/** Últimos eventos, guardados para a tela mostrar o que aconteceu com ela fechada. */
object Historico {
    private const val ARQUIVO = "historico"
    private const val CHAVE = "eventos"

    fun adicionar(context: Context, texto: String) {
        val hora = SimpleDateFormat("HH:mm:ss", Locale("pt", "BR")).format(Date())
        val prefs = context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
        val atual = prefs.getString(CHAVE, "").orEmpty()
        val novo = ("$hora  $texto\n" + atual).lines().take(20).joinToString("\n")
        prefs.edit().putString(CHAVE, novo).apply()
    }

    fun ler(context: Context): String =
        context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).getString(CHAVE, "").orEmpty()

    fun limpar(context: Context) {
        context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

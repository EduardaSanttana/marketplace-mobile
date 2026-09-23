package com.example.aluno_bruno.ui.cadastro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aluno_bruno.data.sync.SyncStatus
import com.example.aluno_bruno.ui.CadastroViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncScreen(viewModel: CadastroViewModel, modifier: Modifier = Modifier) {
    val status by viewModel.syncStatus.collectAsState()
    val ultimaSincronizacao by viewModel.ultimaSincronizacao.collectAsState()
    val produtos by viewModel.produtos.collectAsState()
    val negociantes by viewModel.negociantes.collectAsState()
    val avaliacoes by viewModel.avaliacoes.collectAsState()

    val pendentes = produtos.count { it.pendingSync } +
        negociantes.count { it.pendingSync } +
        avaliacoes.count { it.pendingSync }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sincronização com o Firestore")

        Text(
            when (status) {
                SyncStatus.OCIOSO -> "Status: ocioso"
                SyncStatus.SINCRONIZANDO -> "Status: sincronizando..."
                SyncStatus.ERRO -> "Status: erro ao sincronizar (sem conexão?)"
            }
        )

        Text("Registros pendentes de envio: $pendentes")

        Text(
            ultimaSincronizacao?.let { timestamp ->
                "Última sincronização: ${formatarData(timestamp)}"
            } ?: "Ainda não sincronizado nesta sessão"
        )

        Button(
            onClick = { viewModel.sincronizarAgora() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sincronizar agora")
        }

        Text(
            "As fotos permanecem apenas no armazenamento local do dispositivo e " +
                "não são enviadas ao Firestore. Os demais dados sincronizam " +
                "automaticamente: mudanças no Firestore chegam em tempo real, e " +
                "mudanças feitas offline no app são enviadas assim que a internet voltar."
        )
    }
}

private fun formatarData(timestamp: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.Builder().setLanguage("pt").setRegion("BR").build())
        .format(Date(timestamp))

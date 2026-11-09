package edu.ifsp.marketplace.ui.entregador

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import edu.ifsp.marketplace.data.Negociante

/**
 * Abre o discador com o telefone de um negociante, para o entregador combinar a
 * retirada ou avisar que chegou. Só abre o discador: a ligação é do usuário, sem
 * permissão de telefone. Sem telefone cadastrado, não aparece.
 */
@Composable
fun BotaoLigar(papel: String, negociante: Negociante?, modifier: Modifier = Modifier) {
    val telefone = negociante?.telefone?.filter { it.isDigit() || it == '+' }.orEmpty()
    if (telefone.isEmpty()) return
    val context = LocalContext.current

    AssistChip(
        onClick = {
            try {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telefone")))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Este aparelho não faz ligações", Toast.LENGTH_SHORT).show()
            }
        },
        leadingIcon = {
            Icon(
                Icons.Filled.Phone,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        label = { Text("Ligar para $papel (${negociante?.nome})") },
        modifier = modifier.padding(end = 8.dp)
    )
}

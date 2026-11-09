package edu.ifsp.marketplace.ui.negociante

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.ifsp.marketplace.data.Negociante
import edu.ifsp.marketplace.ui.cadastro.FormularioNegociante

/**
 * Primeiro acesso como negociante: a conta diz qual negociante ela é, ou cria
 * o próprio cadastro. O entregador não cadastra negociantes; cada um se cadastra.
 *
 * O vínculo vai para o cadastro do negociante e segue para a nuvem, então vale
 * em qualquer aparelho. Negociantes já ligados a outra conta aparecem bloqueados.
 */
@Composable
fun EscolherNegocianteScreen(
    negociantes: List<Negociante>,
    emailDaConta: String?,
    onEscolher: (Negociante) -> Unit,
    onCriar: (nome: String, email: String, telefone: String) -> Unit,
    onSouEntregador: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Sem negociantes, o formulário já abre: não há o que escolher
    var criando by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Quem é você?", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Esta conta compra, vende e marca o endereço como um negociante só, " +
                    "em qualquer aparelho.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
            )
        }

        item {
            if (criando || negociantes.isEmpty()) {
                Text("Novo cadastro", style = MaterialTheme.typography.titleMedium)
                FormularioNegociante(
                    textoBotao = "Criar meu cadastro",
                    emailInicial = emailDaConta.orEmpty(),
                    onSalvar = onCriar,
                    onCancelar = if (negociantes.isEmpty()) null else ({ criando = false }),
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                OutlinedButton(onClick = { criando = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ainda não tenho cadastro")
                }
            }
        }

        if (!criando && negociantes.isNotEmpty()) {
            item {
                Text(
                    "Já tenho cadastro",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        if (!criando) items(negociantes, key = { it.id }) { n ->
            Button(
                onClick = { onEscolher(n) },
                enabled = !n.temConta,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Sou ${n.nome}")
                    if (n.temConta) {
                        Text("já ligado a outra conta", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSouEntregador,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Sou só entregador")
            }
        }
    }
}

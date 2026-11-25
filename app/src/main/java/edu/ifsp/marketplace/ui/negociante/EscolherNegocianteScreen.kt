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
 * o próprio cadastro. Contas de entregador (coleção "entregadores" no Firestore)
 * não passam por aqui.
 *
 * O vínculo vai para o cadastro do negociante e segue para a nuvem, então vale
 * em qualquer aparelho. Só aparecem os cadastros com o mesmo e-mail da conta
 * logada e ainda sem conta, para ninguém assumir o negociante de outra pessoa.
 */
@Composable
fun EscolherNegocianteScreen(
    negociantes: List<Negociante>,
    emailDaConta: String?,
    onEscolher: (Negociante) -> Unit,
    onCriar: (nome: String, email: String, telefone: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val negociantes = negociantesDaConta(negociantes, emailDaConta)

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
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Sou ${n.nome}")
                    Text(n.email, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Entregador não se declara aqui: a equipe o cadastra no Firestore
        item {
            Text(
                "É entregador? Peça à equipe para liberar sua conta.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

/**
 * Cadastros que a conta logada pode assumir: ainda sem conta e com o mesmo
 * e-mail dela (sem diferenciar maiúsculas). Sem e-mail na conta, nenhum.
 */
internal fun negociantesDaConta(negociantes: List<Negociante>, emailDaConta: String?): List<Negociante> {
    val email = emailDaConta?.trim().orEmpty()
    if (email.isEmpty()) return emptyList()
    return negociantes.filter { !it.temConta && it.email.trim().equals(email, ignoreCase = true) }
}

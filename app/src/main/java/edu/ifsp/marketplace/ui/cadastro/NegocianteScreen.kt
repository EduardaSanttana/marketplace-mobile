package edu.ifsp.marketplace.ui.cadastro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Formato mínimo de e-mail: algo@algo.algo, sem espaços. Vazio é aceito, o campo é opcional. */
internal fun emailValido(email: String): Boolean =
    email.isBlank() || Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())

/**
 * Dados de um negociante: nome, e-mail e telefone.
 *
 * O próprio negociante preenche, ao criar o cadastro em "Quem é você?" ou ao
 * corrigir em "Meus dados". O entregador não cadastra negociantes.
 */
@Composable
fun FormularioNegociante(
    textoBotao: String,
    onSalvar: (nome: String, email: String, telefone: String) -> Unit,
    modifier: Modifier = Modifier,
    nomeInicial: String = "",
    emailInicial: String = "",
    telefoneInicial: String = "",
    onCancelar: (() -> Unit)? = null
) {
    var nome by remember { mutableStateOf(nomeInicial) }
    var email by remember { mutableStateOf(emailInicial) }
    var telefone by remember { mutableStateOf(telefoneInicial) }
    val emailComErro = !emailValido(email)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            isError = emailComErro,
            supportingText = if (emailComErro) {
                { Text("Use o formato nome@dominio.com") }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = telefone,
            onValueChange = { telefone = it },
            label = { Text("Telefone") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        androidx.compose.material3.Button(
            onClick = { onSalvar(nome.trim(), email.trim(), telefone.trim()) },
            enabled = nome.isNotBlank() && !emailComErro,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(textoBotao)
        }
        onCancelar?.let {
            OutlinedButton(
                onClick = it,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}

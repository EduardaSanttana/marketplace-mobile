package edu.ifsp.marketplace

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.auth
import edu.ifsp.marketplace.ui.auth.LoginScreen
import edu.ifsp.marketplace.ui.MarketplaceApp
import edu.ifsp.marketplace.ui.theme.MarketplaceTheme

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = Firebase.auth

        setContent {
            MarketplaceTheme {
                var usuarioLogado by remember { mutableStateOf(auth.currentUser != null) }
                var entrando by remember { mutableStateOf(false) }
                var erroLogin by remember { mutableStateOf<String?>(null) }

                val uid = auth.currentUser?.uid
                if (usuarioLogado && uid != null) {
                    MarketplaceApp(
                        usuarioUid = uid,
                        usuarioEmail = auth.currentUser?.email,
                        onLogout = {
                            auth.signOut()
                            usuarioLogado = false
                        }
                    )
                } else {
                    LoginScreen(
                        entrando = entrando,
                        erro = erroLogin,
                        onLogin = { email, senha ->
                            entrando = true
                            erroLogin = null
                            login(email, senha) { erro ->
                                entrando = false
                                erroLogin = erro
                                usuarioLogado = erro == null
                            }
                        }
                    )
                }
            }
        }
    }

    /** Entra com e-mail e senha; [onResult] recebe null no sucesso ou a mensagem do erro. */
    private fun login(email: String, senha: String, onResult: (String?) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Login realizado: ${auth.currentUser?.email}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onResult(null)
                } else {
                    onResult(mensagemDeErroDoLogin(task.exception))
                }
            }
    }
}

/** Traduz os erros do Firebase Auth, que chegam em inglês e técnicos. */
private fun mensagemDeErroDoLogin(erro: Exception?): String = when (erro) {
    is FirebaseNetworkException -> "Sem conexão com a internet. O login precisa de rede."
    is FirebaseTooManyRequestsException -> "Muitas tentativas seguidas. Aguarde um pouco e tente de novo."
    is FirebaseAuthInvalidUserException -> "Conta não encontrada ou desativada."
    is FirebaseAuthInvalidCredentialsException ->
        if (erro.errorCode == "ERROR_INVALID_EMAIL") "E-mail inválido." else "E-mail ou senha incorretos."
    else -> "Não foi possível entrar. Tente de novo."
}

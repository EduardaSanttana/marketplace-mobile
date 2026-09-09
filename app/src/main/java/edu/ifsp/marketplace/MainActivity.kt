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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import edu.ifsp.marketplace.ui.auth.LoginScreen
import edu.ifsp.marketplace.ui.cadastro.CadastroApp
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

                if (usuarioLogado) {
                    CadastroApp()
                } else {
                    LoginScreen(
                        onLogin = { email, senha ->
                            login(email, senha) { sucesso -> usuarioLogado = sucesso }
                        }
                    )
                }
            }
        }
    }

    private fun login(email: String, senha: String, onResult: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Login realizado: ${auth.currentUser?.email}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onResult(true)
                } else {
                    Toast.makeText(
                        this,
                        "Erro: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    onResult(false)
                }
            }
    }
}

package com.example.aluno_bruno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.aluno_bruno.ui.cadastro.CadastroApp
import com.example.aluno_bruno.ui.theme.Aluno_brunoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Aluno_brunoTheme {
                CadastroApp()
            }
        }
    }
}

package com.example.aluno_melissaa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.aluno_melissaa.ui.cadastro.CadastroApp
import com.example.aluno_melissaa.ui.theme.Aluno_melissaaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Aluno_melissaaTheme {
                CadastroApp()
            }
        }
    }
}

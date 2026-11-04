package edu.ifsp.marketplace.poc.geocoding

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var latitude by rememberSaveable { mutableStateOf("-23.55052") }
                var longitude by rememberSaveable { mutableStateOf("-46.633308") }
                var resultado by rememberSaveable { mutableStateOf("Informe coordenadas e consulte o endereço.") }
                var buscando by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("PoC 6 · Geocodificação reversa", style = MaterialTheme.typography.headlineSmall)
                        Text("Thomaz Segreto · Marketplace · 04/11/2026")
                        OutlinedTextField(latitude, { latitude = it }, label = { Text("Latitude") }, enabled = !buscando)
                        OutlinedTextField(longitude, { longitude = it }, label = { Text("Longitude") }, enabled = !buscando)
                        Button(enabled = !buscando, onClick = {
                            val lat = latitude.replace(',', '.').toDoubleOrNull()
                            val lon = longitude.replace(',', '.').toDoubleOrNull()
                            if (lat == null || lon == null || !lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                                resultado = "Coordenadas inválidas: latitude -90 a 90; longitude -180 a 180."
                            } else scope.launch {
                                buscando = true
                                resultado = "Consultando…"
                                try {
                                    resultado = GeocodificacaoService.buscar(this@MainActivity, lat, lon)
                                        ?: "Endereço indisponível. Verifique conexão e serviço de geocodificação do aparelho e tente novamente."
                                } finally { buscando = false }
                            }
                        }) { Text(if (buscando) "Consultando…" else "Consultar endereço") }
                        Text(resultado)
                        Text("Alternativa: Photon · Dados © OpenStreetMap contributors", style = MaterialTheme.typography.bodySmall)
                        Text("A PoC usa coordenadas digitadas. Não exige GPS, login, chave do Maps ou Firebase.")
                    }
                }
            }
        }
    }
}

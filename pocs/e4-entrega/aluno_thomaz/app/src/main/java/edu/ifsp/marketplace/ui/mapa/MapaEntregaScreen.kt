package edu.ifsp.marketplace.ui.mapa

import android.content.Context
import android.location.Geocoder
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import edu.ifsp.marketplace.ui.CadastroViewModel
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private data class EnderecoCep(
    val logradouro: String,
    val bairro: String,
    val cidade: String,
    val uf: String
)

// Consulta o CEP no ViaCEP (gratuito, sem chave)
private suspend fun consultarCep(cep: String): EnderecoCep? = withContext(Dispatchers.IO) {
    try {
        val conexao = URL("https://viacep.com.br/ws/$cep/json/").openConnection() as HttpURLConnection
        conexao.connectTimeout = 8000
        conexao.readTimeout = 8000
        val texto = conexao.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(texto)
        if (json.optBoolean("erro", false)) null
        else EnderecoCep(
            logradouro = json.optString("logradouro"),
            bairro = json.optString("bairro"),
            cidade = json.optString("localidade"),
            uf = json.optString("uf")
        )
    } catch (e: Exception) {
        null
    }
}

// Converte um texto de endereço em coordenadas
@Suppress("DEPRECATION")
private suspend fun localizar(context: Context, consulta: String): LatLng? = withContext(Dispatchers.IO) {
    try {
        Geocoder(context, Locale("pt", "BR"))
            .getFromLocationName(consulta, 1)
            ?.firstOrNull()
            ?.let { LatLng(it.latitude, it.longitude) }
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaEntregaScreen(viewModel: CadastroViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val negociantes by viewModel.negociantes.collectAsState()

    var selecionadoId by remember { mutableStateOf<Long?>(null) }
    val selecionado = negociantes.firstOrNull { it.id == selecionadoId }

    var cep by remember(selecionadoId) { mutableStateOf("") }
    var numero by remember(selecionadoId) { mutableStateOf("") }
    // Valores recém-definidos: o pino e o texto mudam na hora, sem esperar o banco
    var pontoNovo by remember(selecionadoId) { mutableStateOf<LatLng?>(null) }
    var enderecoNovo by remember(selecionadoId) { mutableStateOf<String?>(null) }
    val enderecoAtual = enderecoNovo ?: selecionado?.endereco.orEmpty()

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-23.5505, -46.6333), 11f)
    }

    fun salvarPonto(pos: LatLng, textoEndereco: String = enderecoAtual) {
        val n = selecionado ?: return
        pontoNovo = pos
        enderecoNovo = textoEndereco
        viewModel.definirPontoEntrega(n, textoEndereco, pos.latitude, pos.longitude)
        Toast.makeText(context, "Ponto de entrega de ${n.nome} atualizado", Toast.LENGTH_SHORT).show()
    }

    fun buscarPorCep() {
        scope.launch {
            val cepLimpo = cep.filter { it.isDigit() }
            if (cepLimpo.length != 8) {
                Toast.makeText(context, "O CEP precisa ter 8 números", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val dados = consultarCep(cepLimpo)
            if (dados == null) {
                Toast.makeText(context, "CEP não encontrado (ou sem internet)", Toast.LENGTH_LONG).show()
                return@launch
            }

            val cepFormatado = "${cepLimpo.take(5)}-${cepLimpo.drop(5)}"
            val rua = listOf(dados.logradouro, numero.trim()).filter { it.isNotBlank() }.joinToString(", ")
            val textoEndereco = listOf(rua, dados.bairro, "${dados.cidade} - ${dados.uf}", "CEP $cepFormatado")
                .filter { it.isNotBlank() }
                .joinToString(" | ")

            // Tenta do mais preciso para o menos preciso
            val tentativas = listOf(
                listOf(rua, dados.bairro, dados.cidade, dados.uf, "Brasil"),
                listOf(dados.logradouro, dados.cidade, dados.uf, "Brasil"),
                listOf(cepFormatado, "Brasil")
            ).map { partes -> partes.filter { it.isNotBlank() }.joinToString(", ") }

            var pos: LatLng? = null
            for (consulta in tentativas) {
                pos = localizar(context, consulta)
                if (pos != null) break
            }

            if (pos != null) {
                cameraState.animate(CameraUpdateFactory.newLatLngZoom(pos, 17f))
                salvarPonto(pos, textoEndereco)
            } else {
                enderecoNovo = textoEndereco
                Toast.makeText(
                    context,
                    "Não consegui localizar no mapa. Toque no mapa para marcar o ponto.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Ao escolher um negociante que já tem ponto, leva o mapa até ele
    LaunchedEffect(selecionadoId) {
        val lat = selecionado?.latitude
        val lng = selecionado?.longitude
        if (lat != null && lng != null) {
            cameraState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (negociantes.isEmpty()) {
            Text(
                "Cadastre um negociante na aba Negociantes para definir o ponto de entrega.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(negociantes, key = { it.id }) { n ->
                    FilterChip(
                        selected = n.id == selecionadoId,
                        onClick = { selecionadoId = n.id },
                        label = { Text(n.nome) }
                    )
                }
            }

            Text(
                text = if (selecionado == null) "Escolha um negociante."
                else "Informe o CEP e o número e toque na lupa, ou toque no mapa para mover o pino de ${selecionado.nome}.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (selecionado != null) {
                val novo = pontoNovo
                val lat = novo?.latitude ?: selecionado.latitude
                val lng = novo?.longitude ?: selecionado.longitude

                Text(
                    text = if (enderecoAtual.isNotBlank()) "Endereço: $enderecoAtual"
                    else "Sem endereço definido.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
                Text(
                    text = if (lat != null && lng != null) "Coordenadas: %.5f, %.5f".format(lat, lng)
                    else "Este negociante ainda não tem ponto de entrega.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = cep,
                        onValueChange = { cep = it.filter(Char::isDigit).take(8) },
                        label = { Text("CEP") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = numero,
                        onValueChange = { numero = it.take(8) },
                        label = { Text("Nº") },
                        singleLine = true,
                        modifier = Modifier.width(90.dp)
                    )
                    Button(onClick = { buscarPorCep() }, enabled = cep.length == 8) {
                        Icon(Icons.Filled.Search, contentDescription = "Buscar CEP")
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                onMapClick = { pos -> salvarPonto(pos) }
            ) {
                val novo = pontoNovo
                negociantes.forEach { n ->
                    val eSelecionado = n.id == selecionadoId
                    val lat = n.latitude
                    val lng = n.longitude
                    val pos: LatLng? = when {
                        eSelecionado && novo != null -> novo
                        lat != null && lng != null -> LatLng(lat, lng)
                        else -> null
                    }
                    if (pos != null) {
                        key(n.id) {
                            val markerState = rememberMarkerState(position = pos)
                            SideEffect {
                                if (markerState.position != pos) markerState.position = pos
                            }
                            Marker(
                                state = markerState,
                                title = n.nome,
                                snippet = (if (eSelecionado) enderecoAtual else n.endereco)
                                    .ifBlank { "Ponto de entrega" },
                                icon = BitmapDescriptorFactory.defaultMarker(
                                    if (eSelecionado) BitmapDescriptorFactory.HUE_AZURE
                                    else BitmapDescriptorFactory.HUE_RED
                                )
                            )
                        }
                    }
                }
            }

            // Cruz pequena que só indica o centro do mapa (usada pelo botão de baixo)
            if (selecionado != null) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Centro do mapa",
                    tint = Color(0xFF1565C0).copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                )
            }
        }

        Button(
            onClick = { salvarPonto(cameraState.position.target) },
            enabled = selecionado != null,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Text("Informe o endereço de entrega")
        }
    }
}
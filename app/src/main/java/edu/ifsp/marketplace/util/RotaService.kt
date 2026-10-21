package edu.ifsp.marketplace.util

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resultado de um calculo de rota.
 *
 * @param pontos lista de coordenadas que desenham o traçado da rota no mapa
 * @param distanciaMetros distancia total do percurso
 * @param duracaoSegundos tempo estimado de viagem
 */
data class Rota(
    val pontos: List<LatLng>,
    val distanciaMetros: Double,
    val duracaoSegundos: Double
) {
    /** Distancia formatada: "850 m" ou "12,4 km". */
    fun distanciaFormatada(): String =
        if (distanciaMetros < 1000) "${distanciaMetros.toInt()} m"
        else "%.1f km".format(distanciaMetros / 1000)

    /** Duracao formatada: "8 min" ou "1 h 20 min". */
    fun duracaoFormatada(): String {
        val minutos = (duracaoSegundos / 60).toInt()
        return if (minutos < 60) "$minutos min"
        else "${minutos / 60} h ${minutos % 60} min"
    }
}

/**
 * Gera rotas usando o OSRM (Open Source Routing Machine), servico publico
 * baseado em dados do OpenStreetMap.
 *
 * Foi escolhido por nao exigir chave de API nem cartao de credito, ao contrario
 * da Directions API do Google. O manual do curso permite escolher entre Google
 * Maps e OpenStreetMap para as funcionalidades de mapa e endereco.
 *
 * O servidor de demonstracao (router.project-osrm.org) tem limite de uso e nao
 * deve ser usado em producao.
 */
object RotaService {

    private const val BASE_URL = "https://router.project-osrm.org/route/v1/driving"

    /**
     * Calcula a rota entre dois pontos.
     *
     * @return a [Rota] encontrada, ou null se nao houver rota ou a chamada falhar
     */
    suspend fun calcularRota(origem: LatLng, destino: LatLng): Rota? =
        withContext(Dispatchers.IO) {
            try {
                // O OSRM espera as coordenadas na ordem longitude,latitude
                val coordenadas =
                    "${origem.longitude},${origem.latitude};${destino.longitude},${destino.latitude}"
                val url = URL("$BASE_URL/$coordenadas?overview=full&geometries=polyline")

                val conexao = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    requestMethod = "GET"
                }

                val texto = conexao.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(texto)

                if (json.optString("code") != "Ok") return@withContext null

                val rotas = json.optJSONArray("routes") ?: return@withContext null
                if (rotas.length() == 0) return@withContext null

                val primeira = rotas.getJSONObject(0)
                val geometria = primeira.optString("geometry")
                if (geometria.isBlank()) return@withContext null

                Rota(
                    pontos = decodificarPolyline(geometria),
                    distanciaMetros = primeira.optDouble("distance", 0.0),
                    duracaoSegundos = primeira.optDouble("duration", 0.0)
                )
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Decodifica uma polyline codificada no formato do Google (precisao 5).
     *
     * O OSRM devolve o traçado da rota como um texto compactado em vez de uma
     * lista de coordenadas, para economizar banda. Cada coordenada é guardada
     * como a diferenca em relacao a anterior, em blocos de 5 bits.
     *
     * Implementado a mao para entender o formato; a biblioteca
     * android-maps-utils tem a funcao PolyUtil.decode() pronta.
     */
    fun decodificarPolyline(codificada: String): List<LatLng> {
        val pontos = mutableListOf<LatLng>()
        var indice = 0
        var lat = 0
        var lng = 0

        while (indice < codificada.length) {
            // Le um valor por vez: primeiro a variacao da latitude, depois a da longitude
            var deslocamento = 0
            var resultado = 0
            var byte: Int
            do {
                byte = codificada[indice++].code - 63
                resultado = resultado or ((byte and 0x1f) shl deslocamento)
                deslocamento += 5
            } while (byte >= 0x20)
            // O bit menos significativo indica o sinal do numero
            lat += if (resultado and 1 != 0) (resultado shr 1).inv() else resultado shr 1

            deslocamento = 0
            resultado = 0
            do {
                byte = codificada[indice++].code - 63
                resultado = resultado or ((byte and 0x1f) shl deslocamento)
                deslocamento += 5
            } while (byte >= 0x20)
            lng += if (resultado and 1 != 0) (resultado shr 1).inv() else resultado shr 1

            pontos.add(LatLng(lat / 1e5, lng / 1e5))
        }

        return pontos
    }
}

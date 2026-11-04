package edu.ifsp.marketplace.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Converte coordenadas em endereço legível (reverse geocoding).
 *
 * Veio da PoC `pocs/e6-geocoding/aluno_thomaz`. Tenta primeiro o [Geocoder] do
 * Android e, se o aparelho não tiver provedor ou ele não responder, consulta o
 * Photon (dados do OpenStreetMap), que não exige chave de API.
 *
 * Devolve null quando nenhum dos dois responde: quem chama decide o que fazer,
 * já que a falta de conexão é um caso normal e não um erro.
 */
object GeocodificacaoService {

    suspend fun buscar(context: Context, latitude: Double, longitude: Double): String? {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return buscarNoAndroid(context, latitude, longitude) ?: buscarNoPhoton(latitude, longitude)
    }

    private suspend fun buscarNoAndroid(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context.applicationContext, Locale("pt", "BR"))
        return try {
            withTimeoutOrNull(15_000) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuacao ->
                        geocoder.getFromLocation(
                            latitude, longitude, 1,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(enderecos: MutableList<Address>) {
                                    if (continuacao.isActive) {
                                        continuacao.resume(formatar(enderecos.firstOrNull()))
                                    }
                                }

                                override fun onError(mensagem: String?) {
                                    if (continuacao.isActive) continuacao.resume(null)
                                }
                            }
                        )
                    }
                } else {
                    // Antes da API 33 só existe a versão síncrona
                    withContext(Dispatchers.IO) {
                        @Suppress("DEPRECATION")
                        formatar(geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull())
                    }
                }
            }
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private suspend fun buscarNoPhoton(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            val url = URL("https://photon.komoot.io/reverse?lat=$latitude&lon=$longitude&limit=1")
            val conexao = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "Marketplace-DDM2/1.0 (projeto academico)")
            }
            try {
                if (conexao.responseCode != 200) return@withContext null
                val json = conexao.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                val campos = json.optJSONArray("features")
                    ?.optJSONObject(0)
                    ?.optJSONObject("properties")
                    ?: return@withContext null

                listOf("name", "street", "housenumber", "district", "city", "state", "postcode")
                    .map { campos.optString(it) }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(", ")
                    .ifBlank { null }
            } catch (e: IOException) {
                null
            } catch (e: JSONException) {
                null
            } finally {
                conexao.disconnect()
            }
        }

    private fun formatar(endereco: Address?): String? = endereco?.let {
        it.getAddressLine(0)?.takeIf(String::isNotBlank)
            ?: listOfNotNull(
                it.thoroughfare, it.subThoroughfare, it.subLocality,
                it.locality, it.adminArea, it.postalCode
            ).filter(String::isNotBlank).distinct().joinToString(", ").ifBlank { null }
    }
}

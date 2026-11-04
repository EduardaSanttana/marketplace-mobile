package edu.ifsp.marketplace.util

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val RAIO_DA_TERRA_METROS = 6_371_000.0

/** Distância em linha reta entre dois pontos (fórmula de Haversine). */
fun distanciaEmMetros(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return 2 * RAIO_DA_TERRA_METROS * asin(sqrt(a))
}

/** Distância formatada: "850 m" ou "12,4 km". */
fun formatarDistancia(metros: Double): String =
    if (metros < 1000) "${metros.toInt()} m"
    else "%.1f km".format(metros / 1000)

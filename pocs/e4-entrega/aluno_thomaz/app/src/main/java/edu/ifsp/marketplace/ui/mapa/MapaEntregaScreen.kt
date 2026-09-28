package edu.ifsp.marketplace.ui.mapa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapaEntregaScreen(
    modifier: Modifier = Modifier,
    latitude: Double = -23.5505,   // valor fixo só para a PoC (São Paulo)
    longitude: Double = -46.6333,
    titulo: String = "Ponto de entrega"
) {
    val ponto = LatLng(latitude, longitude)
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(ponto, 15f)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraState
    ) {
        Marker(
            state = MarkerState(position = ponto),
            title = titulo
        )
    }
}
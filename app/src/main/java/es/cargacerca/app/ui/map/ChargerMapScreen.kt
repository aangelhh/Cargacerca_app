package es.cargacerca.app.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import es.cargacerca.app.model.ChargingStation
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.Locale

private const val MAP_STYLE = "https://demotiles.maplibre.org/style.json"
private val Panel = Color(0xEE081522)
private val CardBackground = Color(0xF20C1B2C)
private val Muted = Color(0xFF91A4B8)
private val Success = Color(0xFF41E29A)

@Composable
fun ChargerMapScreen(
    stations: List<ChargingStation>,
    onBack: () -> Unit,
    onStationClick: (ChargingStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    val mapView = remember {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            centerOnLastKnownLocation(context, mapInstance)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        mapInstance = map
                        map.setStyle(MAP_STYLE) {
                            stations.forEach { station ->
                                map.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(station.latitude, station.longitude))
                                        .title(station.name)
                                        .snippet(station.id)
                                )
                            }
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(40.4168, -3.7038))
                                .zoom(11.7)
                                .build()
                        }
                        @Suppress("DEPRECATION")
                        map.setOnMarkerClickListener { marker ->
                            val station = stations.firstOrNull { it.id == marker.snippet }
                            if (station != null) {
                                onStationClick(station)
                                true
                            } else {
                                false
                            }
                        }
                    }
                }
            }
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Panel,
                shadowElevation = 8.dp
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "Volver a lista",
                        tint = Color.White
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Panel,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "Mapa de cargadores",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "${stations.size} estaciones demo",
                        color = Muted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 10.dp
        ) {
            IconButton(
                onClick = {
                    if (hasLocationPermission(context)) {
                        centerOnLastKnownLocation(context, mapInstance)
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            ) {
                Icon(
                    Icons.Rounded.MyLocation,
                    contentDescription = "Mi ubicación",
                    tint = Color(0xFF04101B)
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(stations, key = { it.id }) { station ->
                MapStationCard(
                    station = station,
                    onClick = {
                        mapInstance?.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(station.latitude, station.longitude))
                            .zoom(14.5)
                            .build()
                    },
                    onOpen = { onStationClick(station) }
                )
            }
        }
    }
}

@Composable
private fun MapStationCard(
    station: ChargingStation,
    onClick: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(width = 255.dp, height = 132.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF17314C))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        station.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 14.sp
                    )
                    Text(station.operator, color = Muted, fontSize = 10.sp)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF123A2B)) {
                    Text(
                        "${station.available} libres",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        color = Success,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(Modifier.height(11.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetric(Icons.Rounded.Bolt, "${station.powerKw} kW")
                SmallMetric(
                    Icons.Rounded.Bolt,
                    station.pricePerKwh?.let { String.format(Locale.US, "%.2f €/kWh", it) } ?: "Sin precio"
                )
                SmallMetric(Icons.Rounded.LocationOn, String.format(Locale.US, "%.1f km", station.distanceKm))
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Ver detalle →",
                modifier = Modifier.clickable(onClick = onOpen),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SmallMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.size(4.dp))
        Text(text, color = Color.White, fontSize = 9.sp, maxLines = 1)
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
private fun centerOnLastKnownLocation(context: Context, map: MapLibreMap?) {
    if (map == null || !hasLocationPermission(context)) return

    val locationManager = context.getSystemService(LocationManager::class.java) ?: return
    val location = lastKnownLocation(locationManager) ?: return

    map.cameraPosition = CameraPosition.Builder()
        .target(LatLng(location.latitude, location.longitude))
        .zoom(14.5)
        .build()

    @Suppress("DEPRECATION")
    map.addMarker(
        MarkerOptions()
            .position(LatLng(location.latitude, location.longitude))
            .title("Tu ubicación")
            .snippet("user-location")
    )
}

@SuppressLint("MissingPermission")
private fun lastKnownLocation(locationManager: LocationManager): Location? {
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )

    return providers
        .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
}

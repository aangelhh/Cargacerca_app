package es.cargacerca.app.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.location.Location
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import es.cargacerca.app.data.OpenStreetMapChargingStationRepository
import es.cargacerca.app.model.ChargingStation
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.util.Locale
import kotlin.math.roundToInt

private const val MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val MADRID_LATITUDE = 40.4168
private const val MADRID_LONGITUDE = -3.7038
private val Panel = Color(0xEE081522)
private val CardBackground = Color(0xF20C1B2C)
private val Muted = Color(0xFF91A4B8)
private val Success = Color(0xFF41E29A)

private data class MapSearchArea(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int
)

@Composable
@SuppressLint("MissingPermission")
fun ChargerMapScreen(
    stations: List<ChargingStation>,
    onBack: () -> Unit,
    onStationClick: (ChargingStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { OpenStreetMapChargingStationRepository() }
    val initialLocation = remember { lastKnownLocationIfAllowed(context) }
    val initialZoom = if (initialLocation != null) 14.5 else 11.7

    var searchArea by remember {
        mutableStateOf(
            MapSearchArea(
                latitude = initialLocation?.latitude ?: MADRID_LATITUDE,
                longitude = initialLocation?.longitude ?: MADRID_LONGITUDE,
                radiusMeters = radiusForZoom(initialZoom)
            )
        )
    }
    var userLocation by remember { mutableStateOf(initialLocation) }
    var locationAccuracyMeters by remember {
        mutableStateOf(initialLocation?.takeIf { it.hasAccuracy() }?.accuracy)
    }
    var locating by remember { mutableStateOf(false) }
    var mapStations by remember { mutableStateOf(stations) }
    var loadingRealData by remember { mutableStateOf(true) }
    var realDataLoaded by remember { mutableStateOf(false) }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }
    var locationPuckActivated by remember { mutableStateOf(false) }

    val currentStations by rememberUpdatedState(mapStations)
    val currentSearchArea by rememberUpdatedState(searchArea)

    fun acceptLocation(location: Location) {
        userLocation = location
        locationAccuracyMeters = location.takeIf { it.hasAccuracy() }?.accuracy
        searchArea = MapSearchArea(
            latitude = location.latitude,
            longitude = location.longitude,
            radiusMeters = radiusForZoom(16.5)
        )
    }

    fun locatePrecisely() {
        if (!hasLocationPermission(context)) return
        locating = true
        requestFreshLocation(
            context = context,
            onLocation = ::acceptLocation,
            onFinished = { locating = false }
        )
    }

    LaunchedEffect(searchArea) {
        loadingRealData = true
        val realStations = repository.loadNearby(
            latitude = searchArea.latitude,
            longitude = searchArea.longitude,
            radiusMeters = searchArea.radiusMeters
        )

        if (realStations.isNotEmpty()) {
            mapStations = realStations
            realDataLoaded = true
        } else if (realDataLoaded) {
            // A successful move into an area without returned chargers must not keep stale pins.
            mapStations = emptyList()
        }
        loadingRealData = false
    }

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

    LaunchedEffect(mapStations, mapInstance, loadedStyle) {
        val map = mapInstance ?: return@LaunchedEffect
        if (loadedStyle == null) return@LaunchedEffect
        @Suppress("DEPRECATION")
        map.clear()
        mapStations.forEach { station ->
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(station.latitude, station.longitude))
                    .title(station.name)
                    .snippet(station.id)
            )
        }
    }

    LaunchedEffect(userLocation, mapInstance, loadedStyle) {
        val location = userLocation ?: return@LaunchedEffect
        val map = mapInstance ?: return@LaunchedEffect
        val style = loadedStyle ?: return@LaunchedEffect

        if (!locationPuckActivated && hasLocationPermission(context)) {
            val blue = AndroidColor.rgb(30, 112, 255)
            val options = LocationComponentOptions.builder(context)
                .foregroundTintColor(blue)
                .backgroundTintColor(AndroidColor.WHITE)
                .accuracyColor(blue)
                .accuracyAlpha(0.18f)
                .pulseEnabled(true)
                .pulseColor(blue)
                .pulseAlpha(0.25f)
                .pulseMaxRadius(34f)
                .build()

            val activationOptions = LocationComponentActivationOptions
                .builder(context, style)
                .locationComponentOptions(options)
                .useDefaultLocationEngine(false)
                .build()

            map.locationComponent.activateLocationComponent(activationOptions)
            map.locationComponent.isLocationComponentEnabled = true
            locationPuckActivated = true
        }

        if (locationPuckActivated) {
            map.locationComponent.forceLocationUpdate(location)
        }

        map.cameraPosition = CameraPosition.Builder()
            .target(LatLng(location.latitude, location.longitude))
            .zoom(16.5)
            .build()
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            locatePrecisely()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            locatePrecisely()
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
                        map.setStyle(MAP_STYLE) { style ->
                            loadedStyle = style
                            val area = currentSearchArea
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(area.latitude, area.longitude))
                                .zoom(initialZoom)
                                .build()
                        }

                        // Only query after the user finishes moving/zooming the map. This keeps
                        // network traffic low while making pins follow the visible area.
                        map.addOnCameraIdleListener {
                            val target = map.cameraPosition.target
                            val newRadius = radiusForZoom(map.cameraPosition.zoom)
                            val area = currentSearchArea
                            val movedMeters = distanceMeters(
                                area.latitude,
                                area.longitude,
                                target.latitude,
                                target.longitude
                            )
                            val refreshDistance = maxOf(900f, area.radiusMeters * 0.32f)

                            if (movedMeters >= refreshDistance || newRadius != area.radiusMeters) {
                                searchArea = MapSearchArea(
                                    latitude = target.latitude,
                                    longitude = target.longitude,
                                    radiusMeters = newRadius
                                )
                            }
                        }

                        @Suppress("DEPRECATION")
                        map.setOnMarkerClickListener { marker ->
                            val station = currentStations.firstOrNull { it.id == marker.snippet }
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
            Surface(shape = CircleShape, color = Panel, shadowElevation = 8.dp) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = "Volver a lista",
                        tint = Color.White
                    )
                }
            }

            Surface(shape = RoundedCornerShape(18.dp), color = Panel, shadowElevation = 8.dp) {
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
                        when {
                            loadingRealData -> "Actualizando cargadores de esta zona…"
                            realDataLoaded -> "${mapStations.size} puntos reales · mueve el mapa para actualizar"
                            else -> "${mapStations.size} estaciones demo · sin conexión"
                        },
                        color = Muted,
                        fontSize = 10.sp
                    )
                    Text(
                        "Búsqueda eficiente · radio ${searchArea.radiusMeters / 1000} km",
                        color = Muted,
                        fontSize = 9.sp
                    )
                    Text(
                        when {
                            locating -> "Buscando ubicación GPS precisa…"
                            userLocation != null && locationAccuracyMeters != null ->
                                "Tu ubicación · precisión ±${locationAccuracyMeters!!.roundToInt()} m"
                            userLocation != null -> "Tu ubicación encontrada"
                            else -> "Pulsa el botón GPS para centrarte"
                        },
                        color = if (userLocation != null && !locating) Success else Muted,
                        fontSize = 9.sp
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
                enabled = !locating,
                onClick = {
                    if (hasLocationPermission(context)) {
                        locatePrecisely()
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
                    contentDescription = "Mi ubicación precisa",
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
            items(mapStations, key = { it.id }) { station ->
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

private fun radiusForZoom(zoom: Double): Int = when {
    zoom >= 15.0 -> 3_000
    zoom >= 13.0 -> 7_000
    zoom >= 11.0 -> 15_000
    else -> 25_000
}

private fun distanceMeters(
    latitudeA: Double,
    longitudeA: Double,
    latitudeB: Double,
    longitudeB: Double
): Float {
    val result = FloatArray(1)
    Location.distanceBetween(latitudeA, longitudeA, latitudeB, longitudeB, result)
    return result[0]
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
                    Text(station.operator, color = Muted, fontSize = 10.sp, maxLines = 1)
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (station.availabilityKnown) Color(0xFF123A2B) else Color(0xFF10243A)
                ) {
                    Text(
                        if (station.availabilityKnown) "${station.available} libres" else "Sin estado",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        color = if (station.availabilityKnown) Success else Muted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(Modifier.size(11.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallMetric(
                    Icons.Rounded.Bolt,
                    if (station.powerKw > 0) "${station.powerKw} kW" else "Potencia n/d"
                )
                SmallMetric(
                    Icons.Rounded.Bolt,
                    station.pricePerKwh?.let { String.format(Locale.US, "%.2f €/kWh", it) } ?: "Sin precio"
                )
                SmallMetric(
                    Icons.Rounded.LocationOn,
                    String.format(Locale.US, "%.1f km", station.distanceKm)
                )
            }

            Spacer(Modifier.size(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ver detalle →",
                    modifier = Modifier.clickable(onClick = onOpen),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    station.connector,
                    color = Muted,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
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

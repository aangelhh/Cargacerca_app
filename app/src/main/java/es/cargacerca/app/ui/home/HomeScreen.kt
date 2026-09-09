package es.cargacerca.app.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.cargacerca.app.data.OpenStreetMapChargingStationRepository
import es.cargacerca.app.model.ChargingStation
import es.cargacerca.app.ui.compare.ComparisonScreen
import es.cargacerca.app.ui.detail.ChargerDetailScreen
import es.cargacerca.app.ui.map.ChargerMapScreen
import es.cargacerca.app.ui.map.lastKnownLocationIfAllowed
import java.util.Locale

private val Success = Color(0xFF41E29A)
private val Warning = Color(0xFFFFC857)
private val Muted = Color(0xFF91A4B8)
private val CardBorder = Color(0xFF17314C)
private val CardBackground = Color(0xFF0C1B2C)
private val ChipBackground = Color(0xFF10243A)

private const val MADRID_LATITUDE = 40.4168
private const val MADRID_LONGITUDE = -3.7038
private const val LIST_RADIUS_METERS = 12_000

private data class BottomDestination(val label: String, val icon: ImageVector)

@Composable
fun CargaCercaHome() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedStation by remember { mutableStateOf<ChargingStation?>(null) }

    val station = selectedStation
    if (station != null) {
        ChargerDetailScreen(
            station = station,
            onBack = { selectedStation = null }
        )
        return
    }

    val destinations = remember {
        listOf(
            BottomDestination("Explorar", Icons.Rounded.Map),
            BottomDestination("Comparar", Icons.Rounded.CompareArrows),
            BottomDestination("Actividad", Icons.Rounded.History),
            BottomDestination("Perfil", Icons.Rounded.Person)
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF081522), tonalElevation = 0.dp) {
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color(0xFF102C45),
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> ExploreScreen(
                modifier = Modifier.padding(innerPadding),
                onStationClick = { selectedStation = it }
            )
            1 -> ComparisonScreen(
                modifier = Modifier.padding(innerPadding),
                onStationClick = { selectedStation = it }
            )
            else -> ComingSoonScreen(
                modifier = Modifier.padding(innerPadding),
                title = destinations[selectedTab].label
            )
        }
    }
}

@Composable
private fun ExploreScreen(
    modifier: Modifier = Modifier,
    onStationClick: (ChargingStation) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { OpenStreetMapChargingStationRepository() }
    val initialLocation = remember { lastKnownLocationIfAllowed(context) }

    var query by remember { mutableStateOf("") }
    var fastOnly by remember { mutableStateOf(false) }
    var ultraFastOnly by remember { mutableStateOf(false) }
    var ccsOnly by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(true) }
    var nearbyStations by remember { mutableStateOf<List<ChargingStation>>(emptyList()) }
    var loadingStations by remember { mutableStateOf(false) }
    var listLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(showMap) {
        if (!showMap && !listLoaded) {
            loadingStations = true
            val location = lastKnownLocationIfAllowed(context) ?: initialLocation
            val latitude = location?.latitude ?: MADRID_LATITUDE
            val longitude = location?.longitude ?: MADRID_LONGITUDE
            nearbyStations = repository.loadNearby(
                latitude = latitude,
                longitude = longitude,
                radiusMeters = LIST_RADIUS_METERS
            )
            listLoaded = true
            loadingStations = false
        }
    }

    val filteredStations = nearbyStations.filter { station ->
        val matchesQuery = query.isBlank() ||
            station.name.contains(query, ignoreCase = true) ||
            station.operator.contains(query, ignoreCase = true) ||
            station.address.contains(query, ignoreCase = true)
        val matchesFast = !fastOnly || station.powerKw >= 50
        val matchesUltraFast = !ultraFastOnly || station.powerKw >= 150
        val matchesConnector = !ccsOnly || station.connector.contains("CCS", ignoreCase = true)
        matchesQuery && matchesFast && matchesUltraFast && matchesConnector
    }

    if (showMap) {
        ChargerMapScreen(
            modifier = modifier,
            stations = emptyList(),
            onBack = { showMap = false },
            onStationClick = onStationClick
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 18.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Header(usingLocation = initialLocation != null) }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buscar estación, operador o zona") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = { Icon(Icons.Rounded.Tune, contentDescription = "Filtros") },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0A1928),
                    unfocusedContainerColor = Color(0xFF0A1928),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = CardBorder,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    unfocusedLeadingIconColor = Muted,
                    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                    unfocusedTrailingIconColor = Muted,
                    focusedPlaceholderColor = Muted,
                    unfocusedPlaceholderColor = Muted
                )
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    ModernFilterChip(
                        label = "50+ kW",
                        selected = fastOnly,
                        onClick = { fastOnly = !fastOnly }
                    )
                }
                item {
                    ModernFilterChip(
                        label = "150+ kW",
                        selected = ultraFastOnly,
                        onClick = { ultraFastOnly = !ultraFastOnly }
                    )
                }
                item {
                    ModernFilterChip(
                        label = "CCS2",
                        selected = ccsOnly,
                        onClick = { ccsOnly = !ccsOnly }
                    )
                }
            }
        }
        item {
            MapEntryCard(
                stationCount = nearbyStations.size,
                onClick = { showMap = true }
            )
        }
        item {
            DataSourceHero(
                stationCount = nearbyStations.size,
                loading = loadingStations
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Cargadores cercanos",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        "Ordenados por distancia · ubicación y características reales",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
                if (!loadingStations) {
                    Text(
                        "${filteredStations.size} cerca",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
        if (loadingStations) {
            item {
                Text(
                    "Buscando cargadores cercanos…",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
        } else if (listLoaded && filteredStations.isEmpty()) {
            item {
                EmptyStationsCard()
            }
        } else {
            items(filteredStations, key = { it.id }) { station ->
                StationCard(station = station, onClick = { onStationClick(station) })
            }
        }
        item { AdPlaceholder() }
    }
}

@Composable
private fun Header(usingLocation: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "CargaCerca",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text("Encuentra tu mejor carga", color = Muted, fontSize = 13.sp)
        }
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF10243A)) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    if (usingLocation) "Cerca de ti" else "Madrid",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MapEntryCard(stationCount: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0B2135),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16466C))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF123C5E), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Map,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        "Volver al mapa",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (stationCount > 0) "$stationCount puntos cargados · mapa interactivo + GPS" else "Mapa interactivo + GPS",
                        color = Muted,
                        fontSize = 10.sp
                    )
                }
            }
            Text(
                "ABRIR",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ModernFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            containerColor = ChipBackground,
            labelColor = Muted,
            selectedContainerColor = Color(0xFF11395B),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
        ),
        border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = CardBorder,
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun DataSourceHero(stationCount: Int, loading: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B2135)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16466C))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFF123C5E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when {
                        loading -> "Actualizando puntos reales…"
                        stationCount > 0 -> "$stationCount puntos reales cercanos"
                        else -> "Puntos de recarga reales"
                    },
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    "OpenStreetMap · disponibilidad y precios en vivo aún no integrados",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF123A2B)) {
                Text(
                    "REAL",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    color = Success,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyStationsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "No hay resultados para esta búsqueda",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Prueba a quitar filtros o vuelve al mapa y desplázate a otra zona.",
                color = Muted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StationCard(station: ChargingStation, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (station.isRecommended) Color(0xFF1F6A9B) else CardBorder
        )
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF112B43), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                station.name,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (station.isRecommended) {
                                Text(
                                    "  CERCA",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(station.operator, color = Muted, fontSize = 12.sp)
                    }
                }
                StatusPill(station)
            }

            Spacer(Modifier.height(13.dp))
            Text(
                station.address,
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    if (station.powerKw > 0) "${station.powerKw} kW" else "Potencia n/d",
                    Icons.Rounded.Bolt,
                    Modifier.weight(1f)
                )
                MetricPill(
                    station.pricePerKwh?.let {
                        String.format(Locale.getDefault(), "%.2f €/kWh", it)
                    } ?: "Sin precio",
                    Icons.Rounded.Bolt,
                    Modifier.weight(1.2f)
                )
                MetricPill(
                    String.format(Locale.getDefault(), "%.1f km", station.distanceKm),
                    Icons.Rounded.LocationOn,
                    Modifier.weight(0.9f)
                )
            }

            Spacer(Modifier.height(13.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (station.availabilityKnown) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AvailabilityLabel("${station.available} libres", Success)
                        AvailabilityLabel("${station.occupied} ocupados", Warning)
                    }
                } else {
                    AvailabilityLabel("Ocupación no disponible", Muted)
                }
                Text(
                    station.connector,
                    color = Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatusPill(station: ChargingStation) {
    val text: String
    val color: Color
    val background: Color

    if (!station.availabilityKnown) {
        text = "SIN ESTADO"
        color = Muted
        background = Color(0xFF10243A)
    } else if (station.available > 0) {
        text = "LIBRE"
        color = Success
        background = Color(0xFF123A2B)
    } else {
        text = "LLENO"
        color = Color(0xFFFF6B6B)
        background = Color(0xFF402125)
    }

    Surface(shape = RoundedCornerShape(12.dp), color = background) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun MetricPill(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(13.dp), color = Color(0xFF10243A)) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.size(5.dp))
            Text(
                text,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AvailabilityLabel(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(7.dp).background(color, CircleShape))
        Spacer(Modifier.size(5.dp))
        Text(text, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun AdPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = Color(0xFF091725),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF122B42))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Publicidad", color = Color(0xFF5F7489), fontSize = 10.sp)
            Text("Quitar anuncios · 3,99 €", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ComingSoonScreen(modifier: Modifier, title: String) {
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text("Lo construiremos en otra feature", color = Muted, fontSize = 13.sp)
        }
    }
}

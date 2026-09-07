package es.cargacerca.app.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.EvStation
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.cargacerca.app.model.ChargingStation
import java.util.Locale

private val Success = Color(0xFF41E29A)
private val Warning = Color(0xFFFFC857)
private val Danger = Color(0xFFFF6B6B)
private val Muted = Color(0xFF91A4B8)
private val CardBorder = Color(0xFF17314C)
private val CardBackground = Color(0xFF0C1B2C)

@Composable
fun ChargerDetailScreen(
    station: ChargingStation,
    onBack: () -> Unit,
    onNavigate: () -> Unit = {},
    onFavorite: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DetailTopBar(onBack = onBack, onFavorite = onFavorite)
        StationHero(station)
        AvailabilityCard(station)
        KeyMetrics(station)
        ConnectorCard(station)
        LocationCard(station)
        LiveInfoCard()
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onNavigate,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color(0xFF03111E)
            )
        ) {
            Icon(Icons.Rounded.Directions, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Ir ahora", fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        Text(
            text = "Los estados y precios son datos demo en esta fase del desarrollo.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            color = Muted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit, onFavorite: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = Color(0xFF10243A)) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Text(
            text = "Detalle del cargador",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Surface(shape = CircleShape, color = Color(0xFF10243A)) {
            IconButton(onClick = onFavorite) {
                Icon(
                    Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StationHero(station: ChargingStation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B2135)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16466C))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFF123C5E), RoundedCornerShape(17.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.EvStation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(31.dp)
                    )
                }
                Spacer(Modifier.size(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (station.isRecommended) {
                        Text(
                            text = "MEJOR OPCIÓN",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = station.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(station.operator, color = Muted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(station.address, color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AvailabilityCard(station: ChargingStation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Disponibilidad",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text("Estado de los conectores", color = Muted, fontSize = 11.sp)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (station.available > 0) Color(0xFF123A2B) else Color(0xFF402125)
                ) {
                    Text(
                        text = if (station.available > 0) "DISPONIBLE" else "LLENO",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = if (station.available > 0) Success else Danger,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AvailabilityMetric("${station.available}", "Libres", Success, Modifier.weight(1f))
                AvailabilityMetric("${station.occupied}", "Ocupados", Warning, Modifier.weight(1f))
                AvailabilityMetric("${station.outOfService}", "Fuera", Danger, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AvailabilityMetric(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF10243A)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text(label, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun KeyMetrics(station: ChargingStation) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailMetric(
            icon = Icons.Rounded.Bolt,
            value = "${station.powerKw} kW",
            label = "Potencia",
            modifier = Modifier.weight(1f)
        )
        DetailMetric(
            icon = Icons.Rounded.EvStation,
            value = station.pricePerKwh?.let { String.format(Locale.US, "%.2f €", it) } ?: "--",
            label = "por kWh",
            modifier = Modifier.weight(1f)
        )
        DetailMetric(
            icon = Icons.Rounded.LocationOn,
            value = String.format(Locale.US, "%.1f km", station.distanceKm),
            label = "Distancia",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DetailMetric(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(7.dp))
            Text(
                value,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(label, color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ConnectorCard(station: ChargingStation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Text(
                "Conectores",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
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
                Spacer(Modifier.size(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        station.connector,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Hasta ${station.powerKw} kW", color = Muted, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.size(5.dp))
                    Text("${station.available} libres", color = Success, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun LocationCard(station: ChargingStation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Text(
                "Ubicación",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(station.address, color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null)
                Spacer(Modifier.size(7.dp))
                Text("Ver en el mapa")
            }
        }
    }
}

@Composable
private fun LiveInfoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0B2135),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16466C))
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Información en vivo",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text("Última actualización · hace unos segundos", color = Muted, fontSize = 10.sp)
            }
            Text("LIVE", color = Success, fontWeight = FontWeight.Black, fontSize = 10.sp)
        }
    }
}

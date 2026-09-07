package es.cargacerca.app.ui.compare

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Euro
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.cargacerca.app.model.ChargingStation
import es.cargacerca.app.model.demoStations
import java.util.Locale

private val Success = Color(0xFF41E29A)
private val Warning = Color(0xFFFFC857)
private val Muted = Color(0xFF91A4B8)
private val CardBorder = Color(0xFF17314C)
private val CardBackground = Color(0xFF0C1B2C)
private val ChipBackground = Color(0xFF10243A)

private enum class CompareMode(val label: String) {
    CHEAPEST("Más barato"),
    FASTEST("Más rápido"),
    NEAREST("Más cerca")
}

@Composable
fun ComparisonScreen(
    modifier: Modifier = Modifier,
    onStationClick: (ChargingStation) -> Unit
) {
    var mode by remember { mutableStateOf(CompareMode.CHEAPEST) }

    val stations = when (mode) {
        CompareMode.CHEAPEST -> demoStations.sortedWith(
            compareBy<ChargingStation> { it.pricePerKwh ?: Double.MAX_VALUE }
                .thenByDescending { it.available }
        )
        CompareMode.FASTEST -> demoStations.sortedWith(
            compareByDescending<ChargingStation> { it.powerKw }
                .thenByDescending { it.available }
        )
        CompareMode.NEAREST -> demoStations.sortedBy { it.distanceKm }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 18.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Comparar",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Encuentra la mejor opción para esta carga",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompareMode.entries.forEach { option ->
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = mode == option,
                        onClick = { mode = option },
                        label = {
                            Text(
                                text = option.label,
                                maxLines = 1,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ChipBackground,
                            labelColor = Muted,
                            selectedContainerColor = Color(0xFF11395B),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = mode == option,
                            borderColor = CardBorder,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        item {
            RankingHero(mode = mode, topStation = stations.first())
        }

        itemsIndexed(stations, key = { _, station -> station.id }) { index, station ->
            ComparisonCard(
                rank = index + 1,
                station = station,
                mode = mode,
                onClick = { onStationClick(station) }
            )
        }
    }
}

@Composable
private fun RankingHero(mode: CompareMode, topStation: ChargingStation) {
    val summary = when (mode) {
        CompareMode.CHEAPEST -> topStation.pricePerKwh?.let {
            "${String.format(Locale.US, "%.2f", it)} €/kWh · ${topStation.available} libres"
        } ?: "Precio no disponible"
        CompareMode.FASTEST -> "${topStation.powerKw} kW · ${topStation.available} libres"
        CompareMode.NEAREST -> "${String.format(Locale.US, "%.1f", topStation.distanceKm)} km · ${topStation.available} libres"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B2135)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F6A9B))
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
                    imageVector = when (mode) {
                        CompareMode.CHEAPEST -> Icons.Rounded.Euro
                        CompareMode.FASTEST -> Icons.Rounded.Bolt
                        CompareMode.NEAREST -> Icons.Rounded.LocationOn
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(27.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mejor ahora: ${topStation.name}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(summary, color = Muted, fontSize = 12.sp)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF123A2B)) {
                Text(
                    text = "#1",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Success,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ComparisonCard(
    rank: Int,
    station: ChargingStation,
    mode: CompareMode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (rank == 1) Color(0xFF1F6A9B) else CardBorder
        )
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = if (rank == 1) Color(0xFF123C5E) else Color(0xFF10243A)
                ) {
                    Box(
                        modifier = Modifier.size(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#$rank",
                            color = if (rank == 1) MaterialTheme.colorScheme.primary else Muted,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(station.operator, color = Muted, fontSize = 12.sp)
                }

                val availabilityColor = if (station.available > 0) Success else Warning
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (station.available > 0) Color(0xFF123A2B) else Color(0xFF3B2E16)
                ) {
                    Text(
                        text = "${station.available} libres",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        color = availabilityColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompareMetric(
                    title = "Precio",
                    value = station.pricePerKwh?.let { String.format(Locale.US, "%.2f €", it) } ?: "—",
                    highlighted = mode == CompareMode.CHEAPEST,
                    modifier = Modifier.weight(1f)
                )
                CompareMetric(
                    title = "Potencia",
                    value = "${station.powerKw} kW",
                    highlighted = mode == CompareMode.FASTEST,
                    modifier = Modifier.weight(1f)
                )
                CompareMetric(
                    title = "Distancia",
                    value = String.format(Locale.US, "%.1f km", station.distanceKm),
                    highlighted = mode == CompareMode.NEAREST,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = station.address,
                color = Muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompareMetric(
    title: String,
    value: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = if (highlighted) Color(0xFF11395B) else Color(0xFF10243A),
        border = if (highlighted) {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F6A9B))
        } else null
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Muted, fontSize = 9.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

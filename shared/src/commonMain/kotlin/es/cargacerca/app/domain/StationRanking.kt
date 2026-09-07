package es.cargacerca.app.domain

import es.cargacerca.app.model.ChargingStation

enum class StationSortMode {
    CHEAPEST,
    FASTEST,
    NEAREST
}

fun rankStations(
    stations: List<ChargingStation>,
    mode: StationSortMode
): List<ChargingStation> = when (mode) {
    StationSortMode.CHEAPEST -> stations.sortedWith(
        compareBy<ChargingStation> { it.pricePerKwh ?: Double.MAX_VALUE }
            .thenByDescending { it.available }
    )
    StationSortMode.FASTEST -> stations.sortedWith(
        compareByDescending<ChargingStation> { it.powerKw }
            .thenByDescending { it.available }
    )
    StationSortMode.NEAREST -> stations.sortedBy { it.distanceKm }
}

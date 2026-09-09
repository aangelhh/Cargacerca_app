package es.cargacerca.shared

import es.cargacerca.app.data.OpenStreetMapChargingStationRepository
import es.cargacerca.app.model.ChargingStation
import es.cargacerca.app.model.demoStations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Swift-friendly facade over the shared charging-station model and repository.
 *
 * Android and iOS now use the same model and the same remote charging-station
 * loader. The facade keeps Swift interop deliberately small while the UI is
 * migrated to Compose Multiplatform.
 */
class CargaCercaCatalog {
    private val repository = OpenStreetMapChargingStationRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var stations: List<ChargingStation> = demoStations

    fun stationCount(): Int = stations.size

    fun stationName(index: Int): String = station(index).name

    fun stationOperator(index: Int): String = station(index).operator

    fun stationAddress(index: Int): String = station(index).address

    fun stationPowerKw(index: Int): Int = station(index).powerKw

    fun stationDistanceKm(index: Int): Double = station(index).distanceKm

    fun stationConnector(index: Int): String = station(index).connector

    fun stationLatitude(index: Int): Double = station(index).latitude

    fun stationLongitude(index: Int): Double = station(index).longitude

    fun stationDataSource(index: Int): String = station(index).dataSource

    fun stationAvailabilityLabel(index: Int): String {
        val item = station(index)
        return if (item.availabilityKnown) {
            if (item.available > 0) "${item.available} libres" else "Sin libres"
        } else {
            "Sin estado en vivo"
        }
    }

    fun loadNearby(
        latitude: Double,
        longitude: Double,
        onComplete: (Boolean) -> Unit
    ) {
        scope.launch {
            val loaded = repository.loadNearby(latitude, longitude)
            if (loaded.isNotEmpty()) {
                stations = loaded
            }
            onComplete(loaded.isNotEmpty())
        }
    }

    fun resetToDemo() {
        stations = demoStations
    }

    fun isUsingRemoteData(): Boolean =
        stations.firstOrNull()?.dataSource == "OpenStreetMap"

    fun sharedVersion(): String = "0.7.0"

    private fun station(index: Int): ChargingStation {
        val active = stations.ifEmpty { demoStations }
        return active.getOrElse(index) { active.first() }
    }
}

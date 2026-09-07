package es.cargacerca.shared

import es.cargacerca.app.model.ChargingStation
import es.cargacerca.app.model.demoStations

/**
 * Small Swift-friendly facade over the shared charging-station model.
 *
 * This is intentionally simple for the first iOS host: it proves that the
 * iPhone app is reading the same Kotlin data that Android uses. As more UI is
 * migrated to Compose Multiplatform this facade can shrink or disappear.
 */
class CargaCercaCatalog {
    fun stationCount(): Int = demoStations.size

    fun stationName(index: Int): String = station(index).name

    fun stationOperator(index: Int): String = station(index).operator

    fun stationAddress(index: Int): String = station(index).address

    fun stationPowerKw(index: Int): Int = station(index).powerKw

    fun stationDistanceKm(index: Int): Double = station(index).distanceKm

    fun stationConnector(index: Int): String = station(index).connector

    fun stationAvailabilityLabel(index: Int): String {
        val item = station(index)
        return if (item.availabilityKnown) {
            if (item.available > 0) "${item.available} libres" else "Sin libres"
        } else {
            "Sin estado en vivo"
        }
    }

    fun sharedVersion(): String = "0.7.0"

    private fun station(index: Int): ChargingStation =
        demoStations.getOrElse(index) { demoStations.first() }
}

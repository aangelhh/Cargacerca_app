package es.cargacerca.app.model

data class ChargingStation(
    val id: String,
    val name: String,
    val operator: String,
    val address: String,
    val distanceKm: Double,
    val powerKw: Int,
    val pricePerKwh: Double?,
    val available: Int,
    val occupied: Int,
    val outOfService: Int,
    val connector: String,
    val isRecommended: Boolean = false
)

val demoStations = listOf(
    ChargingStation(
        id = "zunder-atocha",
        name = "Zunder Atocha",
        operator = "Zunder",
        address = "Paseo de la Infanta Isabel, Madrid",
        distanceKm = 1.2,
        powerKw = 360,
        pricePerKwh = 0.39,
        available = 5,
        occupied = 1,
        outOfService = 0,
        connector = "CCS2",
        isRecommended = true
    ),
    ChargingStation(
        id = "iberdrola-embajadores",
        name = "Iberdrola Embajadores",
        operator = "Iberdrola",
        address = "Glorieta de Embajadores, Madrid",
        distanceKm = 2.0,
        powerKw = 150,
        pricePerKwh = 0.45,
        available = 2,
        occupied = 2,
        outOfService = 0,
        connector = "CCS2"
    ),
    ChargingStation(
        id = "tesla-bernabeu",
        name = "Tesla Supercharger",
        operator = "Tesla",
        address = "Paseo de la Castellana, Madrid",
        distanceKm = 4.8,
        powerKw = 250,
        pricePerKwh = 0.36,
        available = 7,
        occupied = 5,
        outOfService = 0,
        connector = "CCS2"
    ),
    ChargingStation(
        id = "repsol-m30",
        name = "Repsol M-30",
        operator = "Repsol",
        address = "Avenida del Mediterráneo, Madrid",
        distanceKm = 5.6,
        powerKw = 50,
        pricePerKwh = 0.49,
        available = 1,
        occupied = 1,
        outOfService = 1,
        connector = "CCS2"
    )
)

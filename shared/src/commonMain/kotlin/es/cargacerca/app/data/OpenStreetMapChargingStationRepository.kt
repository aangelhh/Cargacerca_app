package es.cargacerca.app.data

import es.cargacerca.app.model.ChargingStation
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class OpenStreetMapChargingStationRepository(
    private val endpoint: String = "https://overpass-api.de/api/interpreter",
    private val client: HttpClient = HttpClient()
) {
    suspend fun loadNearby(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 20000
    ): List<ChargingStation> = runCatching {
        val query = """
            [out:json][timeout:20];
            (
              node["amenity"="charging_station"](around:$radiusMeters,$latitude,$longitude);
              way["amenity"="charging_station"](around:$radiusMeters,$latitude,$longitude);
              relation["amenity"="charging_station"](around:$radiusMeters,$latitude,$longitude);
            );
            out center tags;
        """.trimIndent()

        val response = client.get(endpoint) {
            parameter("data", query)
            header(HttpHeaders.UserAgent, "CargaCerca/0.7 (Android+iOS)")
            header(HttpHeaders.Accept, "application/json")
        }

        if (response.status.value !in 200..299) return@runCatching emptyList()
        parseResponse(response.bodyAsText(), latitude, longitude)
    }.getOrDefault(emptyList())

    private fun parseResponse(
        body: String,
        originLatitude: Double,
        originLongitude: Double
    ): List<ChargingStation> {
        val root = Json.parseToJsonElement(body).jsonObject
        val elements = root["elements"]?.jsonArray ?: return emptyList()

        return elements.mapNotNull { raw ->
            val element = raw.jsonObject
            val tags = element["tags"]?.jsonObject ?: JsonObject(emptyMap())
            val coordinates = coordinatesOf(element) ?: return@mapNotNull null
            val lat = coordinates.first
            val lon = coordinates.second

            val operator = firstNonBlank(
                text(tags, "operator"),
                text(tags, "network"),
                text(tags, "brand"),
                "Operador no indicado"
            )
            val name = firstNonBlank(
                text(tags, "name"),
                text(tags, "ref"),
                operator.takeIf { it != "Operador no indicado" },
                "Punto de recarga"
            )
            val type = text(element, "type").ifBlank { "element" }
            val id = element["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null

            ChargingStation(
                id = "osm-$type-$id",
                name = name,
                operator = operator,
                address = addressOf(tags),
                distanceKm = haversineKm(originLatitude, originLongitude, lat, lon),
                powerKw = powerKw(tags),
                pricePerKwh = null,
                available = 0,
                occupied = 0,
                outOfService = 0,
                connector = connectorLabel(tags),
                latitude = lat,
                longitude = lon,
                availabilityKnown = false,
                dataSource = "OpenStreetMap"
            )
        }
            .distinctBy { it.id }
            .sortedBy { it.distanceKm }
            .mapIndexed { index, station -> station.copy(isRecommended = index == 0) }
    }

    private fun coordinatesOf(element: JsonObject): Pair<Double, Double>? {
        val lat = element["lat"]?.jsonPrimitive?.doubleOrNull
        val lon = element["lon"]?.jsonPrimitive?.doubleOrNull
        if (lat != null && lon != null) return lat to lon

        val center = element["center"]?.jsonObject ?: return null
        val centerLat = center["lat"]?.jsonPrimitive?.doubleOrNull ?: return null
        val centerLon = center["lon"]?.jsonPrimitive?.doubleOrNull ?: return null
        return centerLat to centerLon
    }

    private fun addressOf(tags: JsonObject): String {
        val street = text(tags, "addr:street")
        val number = text(tags, "addr:housenumber")
        val city = text(tags, "addr:city")
        val postcode = text(tags, "addr:postcode")
        val firstLine = listOf(street, number).filter { it.isNotBlank() }.joinToString(" ")
        val secondLine = listOf(postcode, city).filter { it.isNotBlank() }.joinToString(" ")
        return listOf(firstLine, secondLine)
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { "Ubicación registrada en OpenStreetMap" }
    }

    private fun connectorLabel(tags: JsonObject): String {
        val connectors = buildList {
            if (hasSocket(tags, "socket:type2_combo") || hasSocket(tags, "socket:ccs")) add("CCS2")
            if (hasSocket(tags, "socket:type2")) add("Type 2")
            if (hasSocket(tags, "socket:chademo")) add("CHAdeMO")
            if (hasSocket(tags, "socket:tesla_supercharger")) add("Tesla")
            if (hasSocket(tags, "socket:schuko")) add("Schuko")
        }
        return connectors.distinct().joinToString(" · ").ifBlank { "Conector no indicado" }
    }

    private fun hasSocket(tags: JsonObject, key: String): Boolean {
        val value = text(tags, key)
        if (value.isNotBlank() && value != "0" && !value.equals("no", ignoreCase = true)) return true
        return tags.containsKey("$key:output")
    }

    private fun powerKw(tags: JsonObject): Int {
        val keys = listOf(
            "socket:type2_combo:output",
            "socket:ccs:output",
            "socket:chademo:output",
            "socket:tesla_supercharger:output",
            "socket:type2:output",
            "charging_station:output",
            "max_power"
        )
        return keys.mapNotNull { key -> parsePower(text(tags, key)) }.maxOrNull() ?: 0
    }

    private fun parsePower(raw: String): Int? {
        if (raw.isBlank()) return null
        val kw = Regex("([0-9]+(?:[.,][0-9]+)?)\\s*kW", RegexOption.IGNORE_CASE)
            .find(raw)?.groupValues?.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()
        if (kw != null) return kw.toInt().coerceAtLeast(1)

        val watts = Regex("([0-9]+(?:[.,][0-9]+)?)\\s*W", RegexOption.IGNORE_CASE)
            .find(raw)?.groupValues?.getOrNull(1)?.replace(',', '.')?.toDoubleOrNull()
        return watts?.div(1000.0)?.toInt()?.coerceAtLeast(1)
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = degreesToRadians(lat2 - lat1)
        val dLon = degreesToRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(degreesToRadians(lat1)) * cos(degreesToRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * earthRadiusKm * asin(sqrt(a))
    }

    private fun degreesToRadians(value: Double): Double = value * kotlin.math.PI / 180.0

    private fun text(objectValue: JsonObject, key: String): String =
        objectValue[key]?.jsonPrimitive?.content.orEmpty().trim()

    private fun firstNonBlank(vararg values: String?): String =
        values.firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()
}

package es.cargacerca.app.data

import es.cargacerca.app.model.ChargingStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class OpenStreetMapChargingStationRepository(
    private val endpoint: String = "https://overpass-api.de/api/interpreter"
) {
    suspend fun loadNearby(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 20000
    ): List<ChargingStation> = withContext(Dispatchers.IO) {
        runCatching {
            val query = """
                [out:json][timeout:20];
                (
                  node["amenity"="charging_station"](around:$radiusMeters,$latitude,$longitude);
                  way["amenity"="charging_station"](around:$radiusMeters,$latitude,$longitude);
                  relation["amenity"="charging_station"](around:$radiusMeters,$latitude,$longitude);
                );
                out center tags;
            """.trimIndent()

            val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
            val connection = (URL("$endpoint?data=$encoded").openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 25_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "CargaCerca-Android/0.6")
                setRequestProperty("Accept", "application/json")
            }

            try {
                if (connection.responseCode !in 200..299) return@runCatching emptyList()
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parseResponse(body, latitude, longitude)
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(emptyList())
    }

    private fun parseResponse(
        body: String,
        originLatitude: Double,
        originLongitude: Double
    ): List<ChargingStation> {
        val root = JSONObject(body)
        val elements = root.optJSONArray("elements") ?: return emptyList()
        val stations = ArrayList<ChargingStation>(elements.length())

        for (index in 0 until elements.length()) {
            val element = elements.optJSONObject(index) ?: continue
            val tags = element.optJSONObject("tags") ?: JSONObject()
            val coordinates = coordinatesOf(element) ?: continue
            val lat = coordinates.first
            val lon = coordinates.second

            val operator = firstNonBlank(
                tags.optString("operator"),
                tags.optString("network"),
                tags.optString("brand"),
                "Operador no indicado"
            )
            val name = firstNonBlank(
                tags.optString("name"),
                tags.optString("ref"),
                operator.takeIf { it != "Operador no indicado" },
                "Punto de recarga"
            )

            stations += ChargingStation(
                id = "osm-${element.optString("type")}-${element.optLong("id")}",
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

        return stations
            .distinctBy { it.id }
            .sortedBy { it.distanceKm }
            .mapIndexed { index, station -> station.copy(isRecommended = index == 0) }
    }

    private fun coordinatesOf(element: JSONObject): Pair<Double, Double>? {
        if (element.has("lat") && element.has("lon")) {
            return element.optDouble("lat") to element.optDouble("lon")
        }
        val center = element.optJSONObject("center") ?: return null
        if (!center.has("lat") || !center.has("lon")) return null
        return center.optDouble("lat") to center.optDouble("lon")
    }

    private fun addressOf(tags: JSONObject): String {
        val street = tags.optString("addr:street").trim()
        val number = tags.optString("addr:housenumber").trim()
        val city = tags.optString("addr:city").trim()
        val postcode = tags.optString("addr:postcode").trim()
        val firstLine = listOf(street, number).filter { it.isNotBlank() }.joinToString(" ")
        val secondLine = listOf(postcode, city).filter { it.isNotBlank() }.joinToString(" ")
        return listOf(firstLine, secondLine)
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { "Ubicación registrada en OpenStreetMap" }
    }

    private fun connectorLabel(tags: JSONObject): String {
        val connectors = buildList {
            if (hasSocket(tags, "socket:type2_combo") || hasSocket(tags, "socket:ccs")) add("CCS2")
            if (hasSocket(tags, "socket:type2")) add("Type 2")
            if (hasSocket(tags, "socket:chademo")) add("CHAdeMO")
            if (hasSocket(tags, "socket:tesla_supercharger")) add("Tesla")
            if (hasSocket(tags, "socket:schuko")) add("Schuko")
        }
        return connectors.distinct().joinToString(" · ").ifBlank { "Conector no indicado" }
    }

    private fun hasSocket(tags: JSONObject, key: String): Boolean {
        val value = tags.optString(key).trim()
        if (value.isNotBlank() && value != "0" && !value.equals("no", ignoreCase = true)) return true
        return tags.has("$key:output")
    }

    private fun powerKw(tags: JSONObject): Int {
        val keys = listOf(
            "socket:type2_combo:output",
            "socket:ccs:output",
            "socket:chademo:output",
            "socket:tesla_supercharger:output",
            "socket:type2:output",
            "charging_station:output",
            "max_power"
        )
        return keys.mapNotNull { key -> parsePower(tags.optString(key)) }.maxOrNull() ?: 0
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
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * earthRadiusKm * asin(sqrt(a))
    }

    private fun firstNonBlank(vararg values: String?): String {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()
    }
}

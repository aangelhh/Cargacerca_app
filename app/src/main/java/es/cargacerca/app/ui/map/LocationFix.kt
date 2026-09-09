package es.cargacerca.app.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper

private const val FRESH_LOCATION_TIMEOUT_MS = 10_000L
private const val GOOD_GPS_ACCURACY_METERS = 35f

fun hasLocationPermission(context: Context): Boolean {
    return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
fun lastKnownLocationIfAllowed(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(LocationManager::class.java) ?: return null
    return bestLastKnownLocation(manager)
}

@SuppressLint("MissingPermission")
fun requestFreshLocation(
    context: Context,
    onLocation: (Location) -> Unit,
    onFinished: () -> Unit
) {
    if (!hasLocationPermission(context)) {
        onFinished()
        return
    }

    val manager = context.getSystemService(LocationManager::class.java)
    if (manager == null) {
        onFinished()
        return
    }

    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    ).filter { provider ->
        runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
    }

    var bestLocation = bestLastKnownLocation(manager)

    if (providers.isEmpty()) {
        bestLocation?.let(onLocation)
        onFinished()
        return
    }

    val handler = Handler(Looper.getMainLooper())
    var finished = false
    lateinit var listener: LocationListener

    fun stop(deliverFallback: Boolean = false) {
        if (finished) return
        finished = true
        handler.removeCallbacksAndMessages(null)
        runCatching { manager.removeUpdates(listener) }
        if (deliverFallback) bestLocation?.let(onLocation)
        onFinished()
    }

    listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (isBetterLocation(location, bestLocation)) {
                bestLocation = location
                onLocation(location)
            }

            if (
                location.provider == LocationManager.GPS_PROVIDER &&
                location.hasAccuracy() &&
                location.accuracy <= GOOD_GPS_ACCURACY_METERS
            ) {
                stop()
            }
        }

        @Deprecated("Deprecated in Android SDK")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

    providers.forEach { provider ->
        runCatching {
            manager.requestLocationUpdates(
                provider,
                0L,
                0f,
                listener,
                Looper.getMainLooper()
            )
        }
    }

    handler.postDelayed(
        { stop(deliverFallback = true) },
        FRESH_LOCATION_TIMEOUT_MS
    )
}

@SuppressLint("MissingPermission")
private fun bestLastKnownLocation(locationManager: LocationManager): Location? {
    return listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )
        .mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
        .fold<Location?>(null) { best, candidate ->
            if (isBetterLocation(candidate, best)) candidate else best
        }
}

private fun isBetterLocation(candidate: Location, current: Location?): Boolean {
    if (current == null) return true

    val timeDelta = candidate.time - current.time
    if (timeDelta > 120_000L) return true
    if (timeDelta < -120_000L) return false

    val candidateAccuracy = if (candidate.hasAccuracy()) candidate.accuracy else Float.MAX_VALUE
    val currentAccuracy = if (current.hasAccuracy()) current.accuracy else Float.MAX_VALUE

    return candidateAccuracy < currentAccuracy ||
        (timeDelta > 0L && candidateAccuracy <= currentAccuracy + 50f)
}

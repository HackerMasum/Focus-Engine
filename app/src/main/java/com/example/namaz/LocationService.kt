package com.example.namaz

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getPreciseGpsLocation(): DetectedLocation? = withContext(Dispatchers.IO) {
        // Try high accuracy GPS location fix first via FusedLocationProviderClient
        val location = fetchCurrentLocationFromFused()
            ?: fetchLastLocationFromFused()
            ?: fetchFromSystemLocationManager()

        if (location == null) return@withContext null

        val lat = location.latitude
        val lon = location.longitude
        val tz = TimeZone.getDefault()
        val tzHours = tz.rawOffset / (1000.0 * 60.0 * 60.0)

        var cityName = "GPS Location"
        var countryName = "Auto-detected"

        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                cityName = addr.locality
                    ?: addr.subAdminArea
                    ?: addr.adminArea
                    ?: "GPS (${String.format("%.2f", lat)}, ${String.format("%.2f", lon)})"
                countryName = addr.countryName ?: ""
            }
        } catch (_: Exception) {
            cityName = "GPS (${String.format("%.2f", lat)}, ${String.format("%.2f", lon)})"
        }

        DetectedLocation(
            cityName = cityName,
            countryName = countryName,
            latitude = lat,
            longitude = lon,
            timezoneHours = tzHours
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchCurrentLocationFromFused(): Location? =
        suspendCancellableCoroutine { continuation ->
            val cts = CancellationTokenSource()
            continuation.invokeOnCancellation { cts.cancel() }

            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).addOnSuccessListener { location ->
                    if (continuation.isActive) continuation.resume(location)
                }.addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLastLocationFromFused(): Location? =
        suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private fun fetchFromSystemLocationManager(): Location? {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) return null

        var location: Location? = null
        if (isGpsEnabled) {
            try {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } catch (_: Exception) {}
        }
        if (location == null && isNetworkEnabled) {
            try {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (_: Exception) {}
        }
        return location
    }
}

package com.example.namaz

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import java.util.Locale
import java.util.TimeZone

data class DetectedLocation(
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneHours: Double
)

object LocationHelper {

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context): DetectedLocation? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            return null
        }

        var bestLocation: Location? = null

        if (isGpsEnabled) {
            try {
                val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (loc != null) bestLocation = loc
            } catch (_: Exception) {}
        }

        if (bestLocation == null && isNetworkEnabled) {
            try {
                val loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) bestLocation = loc
            } catch (_: Exception) {}
        }

        val location = bestLocation ?: return null
        val lat = location.latitude
        val lon = location.longitude

        val tz = TimeZone.getDefault()
        val tzHours = tz.rawOffset / (1000.0 * 60.0 * 60.0)

        var cityName = "GPS Location"
        var countryName = "Auto-detected"

        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Synchronous fallback
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    cityName = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea ?: "My Location"
                    countryName = addresses[0].countryName ?: ""
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    cityName = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea ?: "My Location"
                    countryName = addresses[0].countryName ?: ""
                }
            }
        } catch (_: Exception) {}

        return DetectedLocation(
            cityName = cityName,
            countryName = countryName,
            latitude = lat,
            longitude = lon,
            timezoneHours = tzHours
        )
    }
}

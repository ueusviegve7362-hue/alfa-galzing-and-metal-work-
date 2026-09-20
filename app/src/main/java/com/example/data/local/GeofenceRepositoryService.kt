package com.example.data.local

import com.example.data.local.entity.CompanyProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.*

data class GeofenceConfig(
    val isEnabled: Boolean = true,
    val latitude: Double = 28.6139,
    val longitude: Double = 77.2090,
    val radiusMeters: Int = 200,
    val addressName: String = "Alfa Glazing Sector 62 Work Site"
)

class GeofenceRepositoryService(companyProfileFlow: Flow<CompanyProfileEntity?>) {

    val geofenceConfigFlow: Flow<GeofenceConfig> = companyProfileFlow.map { profile ->
        if (profile != null) {
            GeofenceConfig(
                isEnabled = profile.isGeofenceEnabled,
                latitude = profile.jobSiteLatitude,
                longitude = profile.jobSiteLongitude,
                radiusMeters = profile.geofenceRadiusMeters,
                addressName = profile.jobSiteAddressName
            )
        } else {
            GeofenceConfig()
        }
    }

    fun calculateDistanceMeters(userLat: Double, userLng: Double, targetLat: Double, targetLng: Double): Int {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(targetLat - userLat)
        val dLng = Math.toRadians(targetLng - userLng)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(userLat)) * cos(Math.toRadians(targetLat)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadius * c).roundToInt()
    }

    fun isWithinGeofence(userLat: Double, userLng: Double, config: GeofenceConfig): Boolean {
        if (!config.isEnabled) return true
        val distance = calculateDistanceMeters(userLat, userLng, config.latitude, config.longitude)
        return distance <= config.radiusMeters
    }
}

package sg.edu.sit.attendance.location

import android.location.Location
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object LocationFence {

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a =
            sin(dLat / 2).pow(2.0) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun withinFence(
        current: Location,
        fenceLat: Double,
        fenceLng: Double,
        radiusM: Float
    ): Boolean {
        val dist = distanceMeters(current.latitude, current.longitude, fenceLat, fenceLng)
        return dist <= radiusM
    }
}
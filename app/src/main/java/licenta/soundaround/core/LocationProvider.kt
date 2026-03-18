package licenta.soundaround.core

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.android.gms.location.LocationServices
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LocationProvider(context: Context) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Pair<Double, Double>? {
        return try {
            suspendCoroutine { continuation ->
                fusedClient.lastLocation
                    .addOnSuccessListener { location ->
                        continuation.resume(
                            if (location != null) Pair(location.latitude, location.longitude) else null
                        )
                    }
                    .addOnFailureListener {
                        Log.w("LocationProvider", "getLastLocation failed: ${it.message}")
                        continuation.resume(null)
                    }
            }
        } catch (e: SecurityException) {
            Log.w("LocationProvider", "Location permission not granted")
            null
        } catch (e: Exception) {
            Log.e("LocationProvider", "Unexpected error: ${e.message}")
            null
        }
    }
}

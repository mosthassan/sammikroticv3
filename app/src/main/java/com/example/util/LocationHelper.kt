package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? = suspendCancellableCoroutine { continuation ->
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        if (continuation.isActive) continuation.resume(location)
                    } else {
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc ->
                                if (continuation.isActive) continuation.resume(lastLoc)
                            }
                            .addOnFailureListener {
                                if (continuation.isActive) continuation.resume(null)
                            }
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }

            continuation.invokeOnCancellation {
                cts.cancel()
            }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resume(null)
        }
    }
}

package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TrainLocationTracker(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _speedKmH = MutableStateFlow(0f)
    val speedKmH: StateFlow<Float> = _speedKmH.asStateFlow()

    private val _hasGpsFix = MutableStateFlow(false)
    val hasGpsFix: StateFlow<Boolean> = _hasGpsFix.asStateFlow()

    private var fusedCallback: LocationCallback? = null
    private var systemLocationListener: LocationListener? = null

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (_isTracking.value) return

        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                3000L
            ).apply {
                setMinUpdateIntervalMillis(1500L)
                setMinUpdateDistanceMeters(3f)
                setWaitForAccurateLocation(false)
            }.build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    processNewLocation(location)
                }
            }
            fusedCallback = callback

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            ).addOnSuccessListener {
                _isTracking.value = true
                Log.d("TrainLocationTracker", "Fused location updates started")
            }.addOnFailureListener { e ->
                Log.w("TrainLocationTracker", "Fused location updates failed, falling back to LocationManager: ${e.message}")
                startSystemLocationFallback()
            }

            // Also request last known location immediately for instant initial display
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && _currentLocation.value == null) {
                    processNewLocation(loc)
                }
            }
        } catch (e: SecurityException) {
            Log.e("TrainLocationTracker", "Missing location permission: ${e.message}")
        } catch (e: Exception) {
            Log.e("TrainLocationTracker", "Error starting fused updates, fallback: ${e.message}")
            startSystemLocationFallback()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startSystemLocationFallback() {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                _isTracking.value = false
                return
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    processNewLocation(location)
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            systemLocationListener = listener

            val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else {
                LocationManager.NETWORK_PROVIDER
            }

            locationManager.requestLocationUpdates(
                provider,
                3000L,
                3f,
                listener,
                Looper.getMainLooper()
            )
            _isTracking.value = true
        } catch (e: Exception) {
            Log.e("TrainLocationTracker", "System location updates failed: ${e.message}")
            _isTracking.value = false
        }
    }

    private fun processNewLocation(location: Location) {
        _currentLocation.value = location
        _hasGpsFix.value = true

        // Speed in m/s converted to km/h
        val speedKmh = if (location.hasSpeed()) {
            location.speed * 3.6f
        } else {
            0f
        }
        _speedKmH.value = speedKmh
    }

    fun stopTracking() {
        fusedCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            fusedCallback = null
        }
        systemLocationListener?.let {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            lm?.removeUpdates(it)
            systemLocationListener = null
        }
        _isTracking.value = false
        _hasGpsFix.value = false
    }
}

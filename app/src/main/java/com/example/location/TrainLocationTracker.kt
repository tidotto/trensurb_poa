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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    private val _isProjectingByTime = MutableStateFlow(false)
    val isProjectingByTime: StateFlow<Boolean> = _isProjectingByTime.asStateFlow()

    private var fusedCallback: LocationCallback? = null
    private var systemLocationListener: LocationListener? = null
    private var smoothedSpeedKmh: Float = 0f
    private var lastLocationTimeMs: Long = 0L

    private val trackerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var watchdogJob: Job? = null

    companion object {
        const val UPDATE_INTERVAL_MS = 2500L
        const val FASTEST_INTERVAL_MS = 1500L
        const val MIN_DISPLACEMENT_METERS = 1.0f
        const val STOPPED_SPEED_THRESHOLD_KMH = 4.0f // Filtro antirruído: < 4 km/h considera parado (0 km/h)
        const val SIGNAL_TIMEOUT_MS = 14000L // Tempo sem GPS para ativar projeção temporal (Dead Reckoning)

        fun applySpeedFilter(rawSpeedKmh: Float, currentSmoothed: Float = 0f): Float {
            if (rawSpeedKmh < STOPPED_SPEED_THRESHOLD_KMH) {
                return 0f
            }
            // Suavização por Média Móvel Exponencial (EMA)
            return if (currentSmoothed <= 0f) {
                rawSpeedKmh
            } else {
                (currentSmoothed * 0.4f + rawSpeedKmh * 0.6f)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (_isTracking.value) return

        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                UPDATE_INTERVAL_MS
            ).apply {
                setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
                setMinUpdateDistanceMeters(MIN_DISPLACEMENT_METERS)
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
                startSignalWatchdog()
                Log.d("TrainLocationTracker", "Fused location updates started (interval: 2500ms)")
            }.addOnFailureListener { e ->
                Log.w("TrainLocationTracker", "Fused location updates failed, falling back to LocationManager: ${e.message}")
                startSystemLocationFallback()
            }

            // Solicitamos a última localização imediatamente para exibição sem espera
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

    private fun startSignalWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = trackerScope.launch {
            while (isActive) {
                delay(3000L)
                if (_isTracking.value && lastLocationTimeMs > 0L) {
                    val timeSinceLast = System.currentTimeMillis() - lastLocationTimeMs
                    if (timeSinceLast > SIGNAL_TIMEOUT_MS) {
                        if (!_isProjectingByTime.value) {
                            Log.i("TrainLocationTracker", "GPS signal timeout (${timeSinceLast}ms). Activating time-based dead reckoning.")
                            _isProjectingByTime.value = true
                            _hasGpsFix.value = false
                        }
                    }
                }
            }
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
                UPDATE_INTERVAL_MS,
                MIN_DISPLACEMENT_METERS,
                listener,
                Looper.getMainLooper()
            )
            _isTracking.value = true
            startSignalWatchdog()
        } catch (e: Exception) {
            Log.e("TrainLocationTracker", "System location updates failed: ${e.message}")
            _isTracking.value = false
        }
    }

    private fun processNewLocation(location: Location) {
        lastLocationTimeMs = System.currentTimeMillis()
        _currentLocation.value = location
        _hasGpsFix.value = true
        if (_isProjectingByTime.value) {
            _isProjectingByTime.value = false
        }

        // Velocidade em m/s convertida para km/h
        val rawSpeedKmh = if (location.hasSpeed()) {
            location.speed * 3.6f
        } else {
            0f
        }

        // Aplica filtro antirruído (< 4 km/h = 0 km/h) e suavização EMA
        smoothedSpeedKmh = applySpeedFilter(rawSpeedKmh, smoothedSpeedKmh)
        _speedKmH.value = smoothedSpeedKmh
    }

    fun stopTracking() {
        watchdogJob?.cancel()
        watchdogJob = null

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
        _isProjectingByTime.value = false
        smoothedSpeedKmh = 0f
        _speedKmH.value = 0f
    }
}

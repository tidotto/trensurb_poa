package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.location.TrainLocationTracker
import com.example.model.DiagramThemeMode
import com.example.model.Station
import com.example.model.StationStatus
import com.example.model.TrainDirection
import com.example.model.TrensurbData
import com.example.service.TrainTrackingService
import com.example.util.AlertHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class UserPreferencesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("trensurb_preferences", Context.MODE_PRIVATE)

    fun getThemeMode(): DiagramThemeMode {
        val raw = prefs.getString("diagram_theme_mode", DiagramThemeMode.DARK.name)
        return try {
            DiagramThemeMode.valueOf(raw ?: DiagramThemeMode.DARK.name)
        } catch (e: Exception) {
            DiagramThemeMode.DARK
        }
    }

    fun setThemeMode(mode: DiagramThemeMode) {
        prefs.edit().putString("diagram_theme_mode", mode.name).apply()
    }

    fun isSoundEnabled(): Boolean = prefs.getBoolean("sound_enabled", true)
    fun setSoundEnabled(enabled: Boolean) = prefs.edit().putBoolean("sound_enabled", enabled).apply()

    fun isVibrationEnabled(): Boolean = prefs.getBoolean("vibration_enabled", true)
    fun setVibrationEnabled(enabled: Boolean) = prefs.edit().putBoolean("vibration_enabled", enabled).apply()
}

enum class DestinationAlertStage {
    NONE,
    APPROACHING,
    ARRIVED
}

data class TripUiState(
    val hasPermission: Boolean = false,
    val isTracking: Boolean = false,
    val hasGpsFix: Boolean = false,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val speedKmH: Float = 0f,
    val effectiveSpeedKmH: Float = 45f,
    val currentStation: Station? = null,
    val currentStationDistanceMeters: Float? = null,
    val currentStatus: StationStatus = StationStatus.IN_TRANSIT,
    val nextStation: Station? = null,
    val nextStationDistanceMeters: Float? = null,
    val estimatedMinutesToNext: Int? = null,
    val nextStationEtaFormatted: String = "",
    val stationEtas: Map<Int, String> = emptyMap(),
    val direction: TrainDirection = TrainDirection.UNKNOWN,
    val destinationStation: Station? = null,
    val stopsRemaining: Int? = null,
    val distanceToDestinationMeters: Float? = null,
    val destinationEtaFormatted: String = "",
    val arrivalAlertTriggered: Boolean = false,
    val destinationAlertStage: DestinationAlertStage = DestinationAlertStage.NONE,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val selectedStationDetails: Station? = null,
    val recentStationPassedId: Int? = null,
    val diagramThemeMode: DiagramThemeMode = DiagramThemeMode.DARK,
    val isHighContrastMode: Boolean = false,
    val isPreciseLocationGranted: Boolean = true,
    val isBackgroundTrackingEnabled: Boolean = false
)

class TripViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepo = UserPreferencesRepository(application)
    private val locationTracker = TrainLocationTracker(application)
    private val alertHelper = AlertHelper(application)

    private val _uiState = MutableStateFlow(TripUiState())
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()

    private var previousLocation: Location? = null
    private var lastApproachingChimedStationId: Int? = null
    private var alertTriggeredForStationId: Int? = null
    private var approachingTriggeredForStationId: Int? = null
    private var lockedStation: Station? = null

    init {
        // Load persisted preferences
        val initialTheme = preferencesRepo.getThemeMode()
        val initialSound = preferencesRepo.isSoundEnabled()
        val initialVibration = preferencesRepo.isVibrationEnabled()
        _uiState.update {
            it.copy(
                diagramThemeMode = initialTheme,
                isHighContrastMode = initialTheme == DiagramThemeMode.HIGH_CONTRAST,
                soundEnabled = initialSound,
                vibrationEnabled = initialVibration
            )
        }

        viewModelScope.launch {
            locationTracker.currentLocation.collect { location ->
                if (location != null) {
                    handleNewLocation(location)
                }
            }
        }
        viewModelScope.launch {
            locationTracker.isTracking.collect { tracking ->
                _uiState.update { it.copy(isTracking = tracking) }
            }
        }
        viewModelScope.launch {
            locationTracker.hasGpsFix.collect { fix ->
                _uiState.update { it.copy(hasGpsFix = fix) }
            }
        }
        viewModelScope.launch {
            locationTracker.speedKmH.collect { speed ->
                _uiState.update { it.copy(speedKmH = speed) }
            }
        }
        viewModelScope.launch {
            TrainTrackingService.isServiceRunning.collect { running ->
                _uiState.update { it.copy(isBackgroundTrackingEnabled = running) }
            }
        }
    }

    fun onPermissionGranted(isPrecise: Boolean = true) {
        _uiState.update { it.copy(hasPermission = true, isPreciseLocationGranted = isPrecise) }
        locationTracker.startTracking()
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(hasPermission = false) }
    }

    fun setDestination(station: Station) {
        alertTriggeredForStationId = null
        approachingTriggeredForStationId = null
        _uiState.update { current ->
            val cur = current.currentStation
            val dir = when {
                cur != null && station.id > cur.id -> TrainDirection.TO_NOVO_HAMBURGO
                cur != null && station.id < cur.id -> TrainDirection.TO_MERCADO
                else -> current.direction
            }
            current.copy(
                destinationStation = station,
                arrivalAlertTriggered = false,
                destinationAlertStage = DestinationAlertStage.NONE,
                direction = if (dir != TrainDirection.UNKNOWN) dir else current.direction,
                selectedStationDetails = null
            )
        }
        previousLocation?.let { handleNewLocation(it) }
    }

    fun clearDestination() {
        alertTriggeredForStationId = null
        approachingTriggeredForStationId = null
        _uiState.update {
            it.copy(
                destinationStation = null,
                stopsRemaining = null,
                distanceToDestinationMeters = null,
                destinationEtaFormatted = "",
                arrivalAlertTriggered = false,
                destinationAlertStage = DestinationAlertStage.NONE
            )
        }
        if (_uiState.value.isBackgroundTrackingEnabled) {
            updateLiveTrackingNotification(_uiState.value)
        }
    }

    fun dismissArrivalAlert() {
        _uiState.update { it.copy(arrivalAlertTriggered = false) }
    }

    fun toggleSound() {
        val newSound = !_uiState.value.soundEnabled
        preferencesRepo.setSoundEnabled(newSound)
        _uiState.update { it.copy(soundEnabled = newSound) }
    }

    fun toggleVibration() {
        val newVibration = !_uiState.value.vibrationEnabled
        preferencesRepo.setVibrationEnabled(newVibration)
        _uiState.update { it.copy(vibrationEnabled = newVibration) }
    }

    fun setDiagramThemeMode(mode: DiagramThemeMode) {
        preferencesRepo.setThemeMode(mode)
        _uiState.update {
            it.copy(
                diagramThemeMode = mode,
                isHighContrastMode = mode == DiagramThemeMode.HIGH_CONTRAST
            )
        }
    }

    fun toggleHighContrastMode() {
        val current = _uiState.value.diagramThemeMode
        val next = if (current == DiagramThemeMode.HIGH_CONTRAST) DiagramThemeMode.DARK else DiagramThemeMode.HIGH_CONTRAST
        setDiagramThemeMode(next)
    }

    fun testAlert() {
        val destName = _uiState.value.destinationStation?.name ?: "Mercado"
        alertHelper.playArrivalAlert(
            stationName = destName,
            soundEnabled = _uiState.value.soundEnabled,
            vibrationEnabled = _uiState.value.vibrationEnabled
        )
    }

    fun showStationDetails(station: Station) {
        _uiState.update { it.copy(selectedStationDetails = station) }
    }

    fun dismissStationDetails() {
        _uiState.update { it.copy(selectedStationDetails = null) }
    }

    private fun handleNewLocation(location: Location) {
        val stations = TrensurbData.STATIONS
        if (stations.isEmpty()) return

        // 1. Calculate distances to all stations
        val sortedByDistance = stations.map { station ->
            val dist = station.distanceTo(location.latitude, location.longitude)
            station to dist
        }.sortedBy { it.second }

        val closestPair = sortedByDistance.firstOrNull() ?: return
        val closestStation = closestPair.first
        val closestDistance = closestPair.second

        // Live speed from tracker or directly from location
        val rawSpeedKmh = if (location.hasSpeed()) location.speed * 3.6f else locationTracker.speedKmH.value

        // Histerese e travamento de estação:
        // Entrada: <= 250m imediato, ou <= 350m com velocidade baixa (<= 15 km/h)
        // Saída: > 420m ou (> 350m com velocidade de cruzeiro > 15 km/h)
        val isCurrentlyAtStation = _uiState.value.currentStatus == StationStatus.AT_STATION
        val currentlyLocked = lockedStation

        val currentStatus: StationStatus
        val activeStation: Station
        val activeStationDistance: Float

        if (isCurrentlyAtStation && currentlyLocked != null) {
            val distToLocked = currentlyLocked.distanceTo(location.latitude, location.longitude)
            if (distToLocked > 420f || (distToLocked > 350f && rawSpeedKmh > 15f)) {
                currentStatus = StationStatus.IN_TRANSIT
                lockedStation = null
                activeStation = closestStation
                activeStationDistance = closestDistance
            } else {
                currentStatus = StationStatus.AT_STATION
                activeStation = currentlyLocked
                activeStationDistance = distToLocked
            }
        } else {
            if (closestDistance <= 250f || (closestDistance <= 350f && rawSpeedKmh <= 15f)) {
                currentStatus = StationStatus.AT_STATION
                lockedStation = closestStation
                activeStation = closestStation
                activeStationDistance = closestDistance
            } else {
                currentStatus = StationStatus.IN_TRANSIT
                lockedStation = null
                activeStation = closestStation
                activeStationDistance = closestDistance
            }
        }

        val isAtStation = currentStatus == StationStatus.AT_STATION

        // 2. Determine Train Direction
        var detectedDirection = _uiState.value.direction

        val destination = _uiState.value.destinationStation
        if (destination != null) {
            detectedDirection = if (destination.id > activeStation.id) {
                TrainDirection.TO_NOVO_HAMBURGO
            } else if (destination.id < activeStation.id) {
                TrainDirection.TO_MERCADO
            } else {
                detectedDirection
            }
        } else {
            if (activeStation.id == 1) {
                detectedDirection = TrainDirection.TO_NOVO_HAMBURGO
            } else if (activeStation.id == 22) {
                detectedDirection = TrainDirection.TO_MERCADO
            } else {
                previousLocation?.let { prev ->
                    val latDelta = location.latitude - prev.latitude
                    if (latDelta > 0.0003) {
                        detectedDirection = TrainDirection.TO_NOVO_HAMBURGO
                    } else if (latDelta < -0.0003) {
                        detectedDirection = TrainDirection.TO_MERCADO
                    }
                }
            }
        }
        previousLocation = location

        // 3. Determine Next Station along the line
        val nextStation: Station? = when (detectedDirection) {
            TrainDirection.TO_NOVO_HAMBURGO -> {
                if (isAtStation) {
                    stations.find { it.id == activeStation.id + 1 } ?: activeStation
                } else {
                    if (location.latitude > activeStation.latitude) {
                        stations.find { it.id == activeStation.id + 1 } ?: activeStation
                    } else {
                        activeStation
                    }
                }
            }
            TrainDirection.TO_MERCADO -> {
                if (isAtStation) {
                    stations.find { it.id == activeStation.id - 1 } ?: activeStation
                } else {
                    if (location.latitude < activeStation.latitude) {
                        stations.find { it.id == activeStation.id - 1 } ?: activeStation
                    } else {
                        activeStation
                    }
                }
            }
            TrainDirection.UNKNOWN -> {
                if (isAtStation) {
                    stations.find { it.id == activeStation.id + 1 } ?: sortedByDistance.getOrNull(1)?.first ?: activeStation
                } else {
                    sortedByDistance.getOrNull(1)?.first ?: activeStation
                }
            }
        }

        val nextStationDistance = nextStation?.distanceTo(location.latitude, location.longitude)

        // 4. Precise ETA Calculation using GPS Speed and Distance
        val gpsSpeedMps = if (location.hasSpeed() && location.speed > 1.1f) location.speed else 0f
        val isMovingAtSpeed = gpsSpeedMps >= 3.5f // ~12.6 km/h or more

        // Smooth blended speed: 70% live GPS speed + 30% nominal cruising speed (12.5 m/s = 45 km/h)
        val effectiveSpeedMps = if (isMovingAtSpeed) {
            (gpsSpeedMps * 0.7f + 12.5f * 0.3f).coerceIn(4.0f, 22.0f)
        } else {
            12.5f // Nominal average commercial metro cruising speed
        }
        val effectiveSpeedKmH = effectiveSpeedMps * 3.6f

        // Next station ETA formatting
        val nextStationEtaFormatted: String
        val estimatedMinutesToNext: Int?

        if (nextStation != null && nextStationDistance != null) {
            if (nextStationDistance < 150f || (isAtStation && activeStation.id == nextStation.id)) {
                nextStationEtaFormatted = "Chegando"
                estimatedMinutesToNext = 1
            } else {
                val totalSeconds = (nextStationDistance / effectiveSpeedMps).roundToInt()
                estimatedMinutesToNext = (totalSeconds / 60f).roundToInt().coerceAtLeast(1)
                nextStationEtaFormatted = if (totalSeconds < 60) {
                    "< 1 min"
                } else {
                    "~$estimatedMinutesToNext min"
                }
            }
        } else {
            nextStationEtaFormatted = ""
            estimatedMinutesToNext = null
        }

        // 5. Calculate ETAs for all upcoming stations along the route in the linear diagram
        val etasMap = mutableMapOf<Int, String>()
        if (nextStation != null && nextStationDistance != null) {
            var cumulativeSeconds = (nextStationDistance / effectiveSpeedMps).roundToInt()
            etasMap[nextStation.id] = nextStationEtaFormatted

            when (detectedDirection) {
                TrainDirection.TO_NOVO_HAMBURGO -> {
                    var prevStation: Station = nextStation
                    for (stId in (nextStation.id + 1)..22) {
                        val st = stations.find { it.id == stId } ?: continue
                        val distBetween = st.distanceTo(prevStation.latitude, prevStation.longitude)
                        val legSeconds = (distBetween / 12.5f).roundToInt() + 45
                        cumulativeSeconds += legSeconds
                        val minutes = (cumulativeSeconds / 60f).roundToInt()
                        etasMap[stId] = "~$minutes min"
                        prevStation = st
                    }
                }
                TrainDirection.TO_MERCADO -> {
                    var prevStation: Station = nextStation
                    for (stId in (nextStation.id - 1) downTo 1) {
                        val st = stations.find { it.id == stId } ?: continue
                        val distBetween = st.distanceTo(prevStation.latitude, prevStation.longitude)
                        val legSeconds = (distBetween / 12.5f).roundToInt() + 45
                        cumulativeSeconds += legSeconds
                        val minutes = (cumulativeSeconds / 60f).roundToInt()
                        etasMap[stId] = "~$minutes min"
                        prevStation = st
                    }
                }
                TrainDirection.UNKNOWN -> {
                }
            }
        }

        // Approaching chime for regular intermediate stations (within 350m of next station)
        if (nextStation != null && nextStationDistance != null && nextStationDistance <= 350f && nextStationDistance > 50f) {
            if (lastApproachingChimedStationId != nextStation.id && nextStation.id != destination?.id) {
                lastApproachingChimedStationId = nextStation.id
                alertHelper.playStationApproachingChime(
                    soundEnabled = _uiState.value.soundEnabled,
                    vibrationEnabled = _uiState.value.vibrationEnabled
                )
            }
        }

        // 6. Calculate Destination & Two-Stage Arrival/Approaching Alert
        var arrivalTriggered = _uiState.value.arrivalAlertTriggered
        var alertStage = _uiState.value.destinationAlertStage
        var stopsRemaining: Int? = null
        var distanceToDest: Float? = null
        var destEtaFormatted = ""

        if (destination != null) {
            distanceToDest = destination.distanceTo(location.latitude, location.longitude)
            stopsRemaining = if (isAtStation && activeStation.id == destination.id) {
                0
            } else {
                (destination.id - activeStation.id).let { kotlin.math.abs(it) }
            }
            destEtaFormatted = etasMap[destination.id] ?: run {
                val sec = (distanceToDest / effectiveSpeedMps).roundToInt()
                if (sec < 60) "< 1 min" else "~${(sec / 60f).roundToInt()} min"
            }

            if (distanceToDest <= 400f || (isAtStation && activeStation.id == destination.id)) {
                if (alertTriggeredForStationId != destination.id) {
                    alertTriggeredForStationId = destination.id
                    alertStage = DestinationAlertStage.ARRIVED
                    arrivalTriggered = true
                    alertHelper.playArrivalAlert(
                        stationName = destination.name,
                        soundEnabled = _uiState.value.soundEnabled,
                        vibrationEnabled = _uiState.value.vibrationEnabled
                    )
                }
            } else if ((distanceToDest <= 900f || (nextStation?.id == destination.id && activeStation.id != destination.id)) &&
                alertStage == DestinationAlertStage.NONE
            ) {
                if (approachingTriggeredForStationId != destination.id) {
                    approachingTriggeredForStationId = destination.id
                    alertStage = DestinationAlertStage.APPROACHING
                    alertHelper.playApproachingAlert(
                        stationName = destination.name,
                        soundEnabled = _uiState.value.soundEnabled,
                        vibrationEnabled = _uiState.value.vibrationEnabled
                    )
                }
            }
        }

        _uiState.update {
            it.copy(
                currentLatitude = location.latitude,
                currentLongitude = location.longitude,
                currentStation = activeStation,
                currentStationDistanceMeters = activeStationDistance,
                currentStatus = currentStatus,
                nextStation = nextStation,
                nextStationDistanceMeters = nextStationDistance,
                estimatedMinutesToNext = estimatedMinutesToNext,
                nextStationEtaFormatted = nextStationEtaFormatted,
                stationEtas = etasMap,
                effectiveSpeedKmH = effectiveSpeedKmH,
                direction = detectedDirection,
                stopsRemaining = stopsRemaining,
                distanceToDestinationMeters = distanceToDest,
                destinationEtaFormatted = destEtaFormatted,
                arrivalAlertTriggered = arrivalTriggered,
                destinationAlertStage = alertStage
            )
        }

        if (_uiState.value.isBackgroundTrackingEnabled) {
            updateLiveTrackingNotification(_uiState.value)
        }
    }

    fun startBackgroundTracking() {
        TrainTrackingService.start(getApplication())
        _uiState.update { it.copy(isBackgroundTrackingEnabled = true) }
        updateLiveTrackingNotification(_uiState.value)
    }

    fun stopBackgroundTracking() {
        TrainTrackingService.stop(getApplication())
        _uiState.update { it.copy(isBackgroundTrackingEnabled = false) }
    }

    fun toggleBackgroundTracking() {
        if (_uiState.value.isBackgroundTrackingEnabled) {
            stopBackgroundTracking()
        } else {
            startBackgroundTracking()
        }
    }

    private fun updateLiveTrackingNotification(state: TripUiState) {
        val title = when {
            state.currentStatus == StationStatus.AT_STATION && state.currentStation != null ->
                "🚉 Na Estação: ${state.currentStation.name}"
            state.nextStation != null ->
                "🚆 A caminho: ${state.nextStation.name} (${state.nextStationEtaFormatted.ifEmpty { "~2 min" }})"
            state.recentStationPassedId != null -> {
                val passed = TrensurbData.STATIONS.firstOrNull { it.id == state.recentStationPassedId }
                if (passed != null) "🚆 Em trânsito (passou por ${passed.name})" else "🚆 Trensurb em Trânsito"
            }
            else -> "🛰️ Trensurb: Rastreando viagem"
        }

        val text = buildString {
            if (state.nextStation != null) {
                append("Próxima: ${state.nextStation.name}")
                if (state.nextStationEtaFormatted.isNotEmpty()) {
                    append(" • ETA: ${state.nextStationEtaFormatted}")
                }
            } else if (state.currentStation != null) {
                append("Estação: ${state.currentStation.name}")
            } else {
                append("Monitorando localização")
            }
            append(" • ${state.speedKmH.roundToInt()} km/h")
        }

        val bigText = buildString {
            if (state.currentStatus == StationStatus.AT_STATION && state.currentStation != null) {
                append("🚉 Na plataforma: ${state.currentStation.name} (${state.currentStation.city})\n")
            } else if (state.recentStationPassedId != null) {
                val passed = TrensurbData.STATIONS.firstOrNull { it.id == state.recentStationPassedId }
                if (passed != null) {
                    append("📍 Última estação que passou: ${passed.name}\n")
                }
            }
            if (state.nextStation != null) {
                append("➡️ Próxima parada: ${state.nextStation.name}")
                val dist = state.nextStationDistanceMeters
                if (dist != null) {
                    val km = String.format(java.util.Locale.US, "%.1f", dist / 1000f)
                    append(" (a $km km)")
                }
                if (state.nextStationEtaFormatted.isNotEmpty()) {
                    append(" • ETA: ${state.nextStationEtaFormatted}")
                }
                append("\n")
            }
            if (state.destinationStation != null) {
                append("🎯 Destino: ${state.destinationStation.name}")
                if (state.stopsRemaining != null) {
                    append(" (faltam ${state.stopsRemaining} ${if (state.stopsRemaining == 1) "estação" else "estações"})")
                }
                if (state.destinationEtaFormatted.isNotEmpty()) {
                    append(" • Chegada: ~${state.destinationEtaFormatted}")
                }
                append("\n")
            }
            append("⚡ Velocidade do trem: ${state.speedKmH.roundToInt()} km/h")
        }

        TrainTrackingService.updateLiveTrip(getApplication(), title, text, bigText)
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopTracking()
    }
}

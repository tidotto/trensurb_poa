package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.location.TrainLocationTracker
import com.example.model.StationStatus
import com.example.model.TrainDirection
import com.example.model.TrensurbData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Trensurb POA", appName)
    }

    @Test
    fun `verify 22 official stations in Trensurb catalogue with recalibrated platform coordinates`() {
        val stations = TrensurbData.STATIONS
        assertEquals(22, stations.size)
        assertEquals("Mercado", stations.first().name)
        assertEquals("Novo Hamburgo", stations.last().name)
        assertTrue(stations.first().isTerminal)
        assertTrue(stations.last().isTerminal)

        // Verify calibrated coordinates
        val mercado = stations.first { it.id == 1 }
        assertEquals(-30.02624, mercado.latitude, 0.0001)
        assertEquals(-51.22817, mercado.longitude, 0.0001)

        val saoPedro = stations.first { it.id == 3 }
        assertEquals(-30.00630, saoPedro.latitude, 0.0001)
        assertEquals(-51.20937, saoPedro.longitude, 0.0001)

        val luizPasteur = stations.first { it.id == 14 }
        assertEquals(-29.83287, luizPasteur.latitude, 0.0001)
        assertEquals(-51.16565, luizPasteur.longitude, 0.0001)
    }

    @Test
    fun `verify station distance calculation`() {
        val mercado = TrensurbData.STATIONS.first { it.id == 1 }
        // Distance from itself should be ~0 meters
        val dist = mercado.distanceTo(mercado.latitude, mercado.longitude)
        assertTrue("Distance should be near zero", dist < 1.0f)
    }

    @Test
    fun `verify speed deadband filter under 4 kmh`() {
        // Speeds under 4.0 km/h must return 0f to eliminate GPS jitter when stopped
        assertEquals(0f, TrainLocationTracker.applySpeedFilter(0f), 0.001f)
        assertEquals(0f, TrainLocationTracker.applySpeedFilter(1.5f), 0.001f)
        assertEquals(0f, TrainLocationTracker.applySpeedFilter(2.8f), 0.001f)
        assertEquals(0f, TrainLocationTracker.applySpeedFilter(3.9f), 0.001f)

        // Speeds >= 4.0 km/h are preserved or smoothed
        assertTrue(TrainLocationTracker.applySpeedFilter(4.5f) > 0f)
        assertTrue(TrainLocationTracker.applySpeedFilter(35.0f) > 10f)
    }

    @Test
    fun `verify precise vs coarse location permission state`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.TripViewModel(app)

        vm.onPermissionGranted(isPrecise = false)
        assertTrue(vm.uiState.value.hasPermission)
        assertEquals(false, vm.uiState.value.isPreciseLocationGranted)

        vm.onPermissionGranted(isPrecise = true)
        assertTrue(vm.uiState.value.hasPermission)
        assertEquals(true, vm.uiState.value.isPreciseLocationGranted)
    }

    @Test
    fun `verify high contrast mode toggle`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.TripViewModel(app)
        assertEquals(false, vm.uiState.value.isHighContrastMode)
        vm.toggleHighContrastMode()
        assertEquals(true, vm.uiState.value.isHighContrastMode)
        vm.toggleHighContrastMode()
        assertEquals(false, vm.uiState.value.isHighContrastMode)
    }

    @Test
    fun `verify sound and vibration toggles in TripViewModel`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.TripViewModel(app)
        assertTrue(vm.uiState.value.soundEnabled)
        assertTrue(vm.uiState.value.vibrationEnabled)

        vm.toggleSound()
        assertEquals(false, vm.uiState.value.soundEnabled)
        vm.toggleSound()
        assertEquals(true, vm.uiState.value.soundEnabled)

        vm.toggleVibration()
        assertEquals(false, vm.uiState.value.vibrationEnabled)
        vm.toggleVibration()
        assertEquals(true, vm.uiState.value.vibrationEnabled)
    }

    @Test
    fun `verify destination setting and alert reset`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.TripViewModel(app)
        val canoas = TrensurbData.STATIONS.first { it.id == 9 }

        vm.setDestination(canoas)
        assertEquals(canoas, vm.uiState.value.destinationStation)
        assertEquals(com.example.viewmodel.DestinationAlertStage.NONE, vm.uiState.value.destinationAlertStage)
        assertEquals(false, vm.uiState.value.arrivalAlertTriggered)

        vm.clearDestination()
        assertEquals(null, vm.uiState.value.destinationStation)
    }

    @Test
    fun `verify testAlert runs cleanly`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.TripViewModel(app)
        vm.testAlert()
    }

    @Test
    fun `verify ETA calculation, next station and hysteresis`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.TripViewModel(app)

        val mercado = TrensurbData.STATIONS.first { it.id == 1 }

        // 1. Train stopped right at Mercado
        val locationAtStation = android.location.Location("gps").apply {
            latitude = mercado.latitude
            longitude = mercado.longitude
            speed = 0.5f // ~1.8 km/h (stopped jitter)
        }

        val handleLocMethod = vm.javaClass.getDeclaredMethod("handleNewLocation", android.location.Location::class.java)
        handleLocMethod.isAccessible = true
        handleLocMethod.invoke(vm, locationAtStation)

        val stateAtStation = vm.uiState.value
        assertEquals("Mercado", stateAtStation.currentStation?.name)
        assertEquals(StationStatus.AT_STATION, stateAtStation.currentStatus)
        // Next station should be Rodoviária, not Mercado itself!
        assertEquals("Rodoviária", stateAtStation.nextStation?.name)
        assertEquals(TrainDirection.TO_NOVO_HAMBURGO, stateAtStation.direction)
        assertTrue(stateAtStation.nextStationEtaFormatted.isNotEmpty())
        assertTrue(stateAtStation.stationEtas.isNotEmpty())

        // 2. Train leaves Mercado but still within hysteresis zone (e.g. 200m away, slow speed)
        val locationLeaving = android.location.Location("gps").apply {
            latitude = mercado.latitude + 0.0015
            longitude = mercado.longitude
            speed = 3.0f // ~10 km/h
        }
        handleLocMethod.invoke(vm, locationLeaving)
        // Should remain AT_STATION due to hysteresis
        assertEquals(StationStatus.AT_STATION, vm.uiState.value.currentStatus)
        assertEquals("Mercado", vm.uiState.value.currentStation?.name)
    }

    @Test
    fun `verify background tracking service lifecycle and notification update`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.TripViewModel(app)

        vm.startBackgroundTracking()
        assertTrue(vm.uiState.value.isBackgroundTrackingEnabled)

        com.example.service.TrainTrackingService.updateLiveTrip(
            context = app,
            title = "Na Estação: Mercado",
            text = "Próxima: Rodoviária • ~2 min",
            bigText = "Trajeto ativo"
        )

        vm.stopBackgroundTracking()
        assertFalse(vm.uiState.value.isBackgroundTrackingEnabled)
    }

    @Test
    fun `verify alertHelper screen wake up runs without crashing`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val alertHelper = com.example.util.AlertHelper(app)
        alertHelper.wakeUpScreen()
        alertHelper.playStationApproachingChime(soundEnabled = false, vibrationEnabled = false)
    }
}

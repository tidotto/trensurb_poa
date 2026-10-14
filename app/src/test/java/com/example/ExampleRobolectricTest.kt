package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.TrensurbData
import org.junit.Assert.assertEquals
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
    fun `verify 22 official stations in Trensurb catalogue`() {
        val stations = TrensurbData.STATIONS
        assertEquals(22, stations.size)
        assertEquals("Mercado", stations.first().name)
        assertEquals("Novo Hamburgo", stations.last().name)
        assertTrue(stations.first().isTerminal)
        assertTrue(stations.last().isTerminal)
    }

    @Test
    fun `verify station distance calculation`() {
        val mercado = TrensurbData.STATIONS.first { it.id == 1 }
        // Distance from itself should be ~0 meters
        val dist = mercado.distanceTo(mercado.latitude, mercado.longitude)
        assertTrue("Distance should be near zero", dist < 1.0f)
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
        // Should execute smoothly without throwing exceptions
        vm.testAlert()
    }

    @Test
    fun `verify ETA calculation and stationEtas map`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.TripViewModel(app)

        val location = android.location.Location("gps").apply {
            latitude = -30.0278
            longitude = -51.2287
            speed = 13.8f // ~50 km/h
        }

        val handleLocMethod = vm.javaClass.getDeclaredMethod("handleNewLocation", android.location.Location::class.java)
        handleLocMethod.isAccessible = true
        handleLocMethod.invoke(vm, location)

        val state = vm.uiState.value
        assertEquals("Mercado", state.currentStation?.name)
        assertNotNull(state.nextStation)
        assertTrue(state.nextStationEtaFormatted.isNotEmpty())
        assertTrue(state.stationEtas.isNotEmpty())
    }
}

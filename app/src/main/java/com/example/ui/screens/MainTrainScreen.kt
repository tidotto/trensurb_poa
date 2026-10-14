package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Station
import com.example.ui.components.ArrivalAlertBanner
import com.example.ui.components.CurrentStationCard
import com.example.ui.components.DestinationCard
import com.example.ui.components.DestinationPickerDialog
import com.example.ui.components.NextStationCard
import com.example.ui.components.PermissionRequestCard
import com.example.ui.components.StationDetailBottomSheet
import com.example.ui.components.TrainLineDiagram
import com.example.ui.components.TrensurbTopBar
import com.example.viewmodel.TripViewModel

@Composable
fun MainTrainScreen(
    viewModel: TripViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showDestinationPicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    // Check permissions on start
    LaunchedEffect(Unit) {
        val fineCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fineCheck == PackageManager.PERMISSION_GRANTED || coarseCheck == PackageManager.PERMISSION_GRANTED) {
            viewModel.onPermissionGranted()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_train_screen"),
        topBar = {
            TrensurbTopBar(
                hasGpsFix = uiState.hasGpsFix,
                speedKmH = uiState.speedKmH,
                direction = uiState.direction,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Permission Request Card if location is not granted
            if (!uiState.hasPermission) {
                item {
                    PermissionRequestCard(
                        onRequestPermission = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )
                }
            }

            // Current Station Hero Card
            item {
                CurrentStationCard(
                    station = uiState.currentStation,
                    distanceMeters = uiState.currentStationDistanceMeters,
                    status = uiState.currentStatus
                )
            }

            // Next Station Card
            item {
                NextStationCard(
                    nextStation = uiState.nextStation,
                    distanceMeters = uiState.nextStationDistanceMeters,
                    estimatedMinutes = uiState.estimatedMinutesToNext,
                    isDestination = uiState.destinationStation != null && uiState.nextStation?.id == uiState.destinationStation?.id,
                    etaFormatted = uiState.nextStationEtaFormatted
                )
            }

            // Destination Status & Action Card
            item {
                DestinationCard(
                    destination = uiState.destinationStation,
                    stopsRemaining = uiState.stopsRemaining,
                    distanceMeters = uiState.distanceToDestinationMeters,
                    alertStage = uiState.destinationAlertStage,
                    soundEnabled = uiState.soundEnabled,
                    vibrationEnabled = uiState.vibrationEnabled,
                    onToggleSound = { viewModel.toggleSound() },
                    onToggleVibration = { viewModel.toggleVibration() },
                    onTestAlert = { viewModel.testAlert() },
                    onSelectDestinationClick = { showDestinationPicker = true },
                    onClearDestinationClick = { viewModel.clearDestination() }
                )
            }

            // Interactive Train Line Diagram (All 22 stations)
            item {
                TrainLineDiagram(
                    currentStation = uiState.currentStation,
                    currentStatus = uiState.currentStatus,
                    nextStation = uiState.nextStation,
                    destinationStation = uiState.destinationStation,
                    direction = uiState.direction,
                    stationEtas = uiState.stationEtas,
                    themeMode = uiState.diagramThemeMode,
                    onThemeModeSelected = { viewModel.setDiagramThemeMode(it) },
                    onStationClick = { station ->
                        viewModel.showStationDetails(station)
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // Modals & Sheets
        if (uiState.arrivalAlertTriggered && uiState.destinationStation != null) {
            ArrivalAlertBanner(
                destination = uiState.destinationStation!!,
                onDismiss = { viewModel.dismissArrivalAlert() }
            )
        }

        if (showDestinationPicker) {
            DestinationPickerDialog(
                currentStationId = uiState.currentStation?.id,
                onDestinationSelected = { station ->
                    viewModel.setDestination(station)
                },
                onDismiss = { showDestinationPicker = false }
            )
        }

        uiState.selectedStationDetails?.let { selected ->
            StationDetailBottomSheet(
                station = selected,
                currentLatitude = uiState.currentLatitude,
                currentLongitude = uiState.currentLongitude,
                isCurrentDestination = uiState.destinationStation?.id == selected.id,
                onSetDestination = { station ->
                    viewModel.setDestination(station)
                },
                onClearDestination = {
                    viewModel.clearDestination()
                },
                onDismiss = {
                    viewModel.dismissStationDetails()
                }
            )
        }
    }
}

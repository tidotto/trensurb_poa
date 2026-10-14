package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Station
import com.example.ui.components.ArrivalAlertBanner
import com.example.ui.components.BackgroundTrackingCard
import com.example.ui.components.CurrentStationCard
import com.example.ui.components.DestinationCard
import com.example.ui.components.DestinationPickerDialog
import com.example.ui.components.NextStationCard
import com.example.ui.components.PermissionRequestCard
import com.example.ui.components.StationDetailBottomSheet
import com.example.ui.components.TrainLineDiagram
import com.example.ui.components.TrensurbTopBar
import com.example.ui.theme.TrensurbBlue
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.TripViewModel

@Composable
fun MainTrainScreen(
    viewModel: TripViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showDestinationPicker by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startBackgroundTracking()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted) {
            viewModel.onPermissionGranted(isPrecise = true)
        } else if (coarseGranted) {
            viewModel.onPermissionGranted(isPrecise = false)
        } else {
            viewModel.onPermissionDenied()
        }
    }

    fun handleToggleBackground() {
        if (!uiState.isBackgroundTrackingEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotif = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasNotif) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return
                }
            }
            viewModel.startBackgroundTracking()
        } else {
            viewModel.stopBackgroundTracking()
        }
    }

    fun handleMinimizeApp() {
        if (!uiState.isBackgroundTrackingEnabled) {
            handleToggleBackground()
        }
        (context as? Activity)?.moveTaskToBack(true)
    }

    // Check permissions on start
    LaunchedEffect(Unit) {
        val fineCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineCheck) {
            viewModel.onPermissionGranted(isPrecise = true)
        } else if (coarseCheck) {
            viewModel.onPermissionGranted(isPrecise = false)
        } else {
            val permissions = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            permissionLauncher.launch(permissions)
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
            } else if (!uiState.isPreciseLocationGranted) {
                // Warning card if only approximate location was granted
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("approximate_location_warning_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        ),
                        border = BorderStroke(1.dp, WarningAmber)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Modo de Alta Precisão Desativado",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "O Android está fornecendo localização aproximada. Para detectar as plataformas e as paradas do trem com precisão métrica, ative a 'Localização Exata'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(
                                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = TrensurbBlue)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GpsFixed,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ativar Exata", fontSize = 13.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Configurações", fontSize = 13.sp)
                                }
                            }
                        }
                    }
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
                    isDestination = uiState.nextStation?.id == uiState.destinationStation?.id,
                    etaFormatted = uiState.nextStationEtaFormatted
                )
            }

            // Destination Tracker or Set Destination CTA
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

            // Modo Segundo Plano e Tela de Bloqueio
            item {
                BackgroundTrackingCard(
                    isEnabled = uiState.isBackgroundTrackingEnabled,
                    onToggle = { handleToggleBackground() },
                    onMinimizeApp = { handleMinimizeApp() }
                )
            }

            // Linear Metro diagram with 22 stations
            item {
                TrainLineDiagram(
                    currentStation = uiState.currentStation,
                    currentStatus = uiState.currentStatus,
                    nextStation = uiState.nextStation,
                    destinationStation = uiState.destinationStation,
                    direction = uiState.direction,
                    stationEtas = uiState.stationEtas,
                    themeMode = uiState.diagramThemeMode,
                    onThemeModeSelected = { mode -> viewModel.setDiagramThemeMode(mode) },
                    onStationClick = { station ->
                        viewModel.showStationDetails(station)
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // Arrival Alert Banner (when approaching or arrived at destination)
        if (uiState.arrivalAlertTriggered && uiState.destinationStation != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                ArrivalAlertBanner(
                    destination = uiState.destinationStation!!,
                    onDismiss = { viewModel.dismissArrivalAlert() }
                )
            }
        }

        // Station Details Modal
        uiState.selectedStationDetails?.let { station ->
            StationDetailBottomSheet(
                station = station,
                currentLatitude = uiState.currentLatitude,
                currentLongitude = uiState.currentLongitude,
                isCurrentDestination = station.id == uiState.destinationStation?.id,
                onSetDestination = { s -> viewModel.setDestination(s) },
                onClearDestination = { viewModel.clearDestination() },
                onDismiss = { viewModel.dismissStationDetails() }
            )
        }

        // Destination Station Picker
        if (showDestinationPicker) {
            DestinationPickerDialog(
                currentStationId = uiState.currentStation?.id,
                onDestinationSelected = { station ->
                    viewModel.setDestination(station)
                    showDestinationPicker = false
                },
                onDismiss = { showDestinationPicker = false }
            )
        }
    }
}

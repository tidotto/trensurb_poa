package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DiagramThemeMode
import com.example.model.Station
import com.example.model.StationStatus
import com.example.model.TrainDirection
import com.example.model.TrensurbData
import com.example.ui.theme.HighContrastBlack
import com.example.ui.theme.HighContrastCyan
import com.example.ui.theme.HighContrastGreen
import com.example.ui.theme.HighContrastTextMuted
import com.example.ui.theme.HighContrastTrackMuted
import com.example.ui.theme.HighContrastWhite
import com.example.ui.theme.HighContrastYellow
import com.example.ui.theme.RailGreen
import com.example.ui.theme.SurfaceDarkCard
import com.example.ui.theme.TrensurbBlue
import com.example.ui.theme.TrensurbBlueLight
import com.example.ui.theme.WarningAmber

@Composable
fun TrainLineDiagram(
    currentStation: Station?,
    currentStatus: StationStatus,
    nextStation: Station?,
    destinationStation: Station?,
    direction: TrainDirection,
    stationEtas: Map<Int, String> = emptyMap(),
    themeMode: DiagramThemeMode = DiagramThemeMode.DARK,
    onThemeModeSelected: (DiagramThemeMode) -> Unit = {},
    onStationClick: (Station) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "marker_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val isHighContrast = themeMode == DiagramThemeMode.HIGH_CONTRAST

    val cardBg = when (themeMode) {
        DiagramThemeMode.LIGHT -> Color(0xFFFFFFFF)
        DiagramThemeMode.DARK -> SurfaceDarkCard
        DiagramThemeMode.HIGH_CONTRAST -> HighContrastBlack
    }

    val cardBorder = when (themeMode) {
        DiagramThemeMode.LIGHT -> BorderStroke(1.dp, Color(0xFFE2E8F0))
        DiagramThemeMode.DARK -> BorderStroke(1.dp, Color(0xFF26354A))
        DiagramThemeMode.HIGH_CONTRAST -> BorderStroke(2.dp, HighContrastCyan)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("train_line_diagram"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighContrast) 6.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Title and Theme Selector Segmented Control
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (themeMode) {
                                DiagramThemeMode.HIGH_CONTRAST -> "DIAGRAMA DA LINHA 1 (ALTO CONTRASTE)"
                                DiagramThemeMode.LIGHT -> "DIAGRAMA DA LINHA 1 (MODO CLARO)"
                                DiagramThemeMode.DARK -> "DIAGRAMA DA LINHA 1"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = when (themeMode) {
                                DiagramThemeMode.HIGH_CONTRAST -> HighContrastCyan
                                DiagramThemeMode.LIGHT -> TrensurbBlue
                                DiagramThemeMode.DARK -> MaterialTheme.colorScheme.primary
                            }
                        )
                        Text(
                            text = "Toque em qualquer estação para ver conexões ou definir destino",
                            style = MaterialTheme.typography.bodySmall,
                            color = when (themeMode) {
                                DiagramThemeMode.HIGH_CONTRAST -> HighContrastWhite
                                DiagramThemeMode.LIGHT -> Color(0xFF64748B)
                                DiagramThemeMode.DARK -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Theme Mode Selector Tabs (Light / Dark / High Contrast)
                ThemeSelectorPillGroup(
                    selectedMode = themeMode,
                    onSelectMode = onThemeModeSelected
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Display all 22 stations along the metro timeline
            val stations = TrensurbData.STATIONS
            stations.forEachIndexed { index, station ->
                val isCurrent = currentStation?.id == station.id
                val isNext = nextStation?.id == station.id
                val isDestination = destinationStation?.id == station.id
                val isLast = index == stations.size - 1

                val isPassed = if (currentStation != null && direction != TrainDirection.UNKNOWN) {
                    when (direction) {
                        TrainDirection.TO_NOVO_HAMBURGO -> station.id < currentStation.id
                        TrainDirection.TO_MERCADO -> station.id > currentStation.id
                        else -> false
                    }
                } else false

                val stationEta = stationEtas[station.id]

                StationTimelineRow(
                    station = station,
                    isCurrent = isCurrent,
                    isNext = isNext,
                    isDestination = isDestination,
                    isPassed = isPassed,
                    isLast = isLast,
                    currentStatus = currentStatus,
                    pulseScale = pulseScale,
                    themeMode = themeMode,
                    stationEta = stationEta,
                    onClick = { onStationClick(station) }
                )
            }
        }
    }
}

@Composable
private fun ThemeSelectorPillGroup(
    selectedMode: DiagramThemeMode,
    onSelectMode: (DiagramThemeMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = when (selectedMode) {
            DiagramThemeMode.HIGH_CONTRAST -> Color(0xFF111111)
            DiagramThemeMode.LIGHT -> Color(0xFFF1F5F9)
            DiagramThemeMode.DARK -> Color(0xFF0F1724)
        },
        border = BorderStroke(
            1.dp,
            if (selectedMode == DiagramThemeMode.HIGH_CONTRAST) HighContrastCyan.copy(alpha = 0.5f) else Color(0xFF334155).copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("theme_selector_group")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ThemeOptionTab(
                mode = DiagramThemeMode.LIGHT,
                label = "Claro",
                icon = Icons.Default.LightMode,
                isSelected = selectedMode == DiagramThemeMode.LIGHT,
                onClick = { onSelectMode(DiagramThemeMode.LIGHT) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("theme_tab_light")
            )
            Spacer(modifier = Modifier.width(4.dp))
            ThemeOptionTab(
                mode = DiagramThemeMode.DARK,
                label = "Escuro",
                icon = Icons.Default.DarkMode,
                isSelected = selectedMode == DiagramThemeMode.DARK,
                onClick = { onSelectMode(DiagramThemeMode.DARK) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("theme_tab_dark")
            )
            Spacer(modifier = Modifier.width(4.dp))
            ThemeOptionTab(
                mode = DiagramThemeMode.HIGH_CONTRAST,
                label = "Alto Contraste",
                icon = Icons.Default.Contrast,
                isSelected = selectedMode == DiagramThemeMode.HIGH_CONTRAST,
                onClick = { onSelectMode(DiagramThemeMode.HIGH_CONTRAST) },
                modifier = Modifier
                    .weight(1.3f)
                    .testTag("theme_tab_high_contrast")
            )
        }
    }
}

@Composable
private fun ThemeOptionTab(
    mode: DiagramThemeMode,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        isSelected && mode == DiagramThemeMode.HIGH_CONTRAST -> HighContrastYellow
        isSelected && mode == DiagramThemeMode.LIGHT -> TrensurbBlue
        isSelected && mode == DiagramThemeMode.DARK -> Color(0xFF2563EB)
        else -> Color.Transparent
    }

    val contentColor = when {
        isSelected && mode == DiagramThemeMode.HIGH_CONTRAST -> Color.Black
        isSelected -> Color.White
        else -> Color.Gray
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        modifier = modifier
            .height(42.dp)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Tema $label${if (isSelected) ", selecionado" else ""}"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = contentColor
            )
        }
    }
}

@Composable
private fun StationTimelineRow(
    station: Station,
    isCurrent: Boolean,
    isNext: Boolean,
    isDestination: Boolean,
    isPassed: Boolean,
    isLast: Boolean,
    currentStatus: StationStatus,
    pulseScale: Float,
    themeMode: DiagramThemeMode,
    stationEta: String?,
    onClick: () -> Unit
) {
    val isHighContrast = themeMode == DiagramThemeMode.HIGH_CONTRAST
    val isLight = themeMode == DiagramThemeMode.LIGHT

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = if (isHighContrast) 4.dp else 3.dp)
            .testTag("station_row_${station.id}"),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: Node Icon and Track line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(if (isHighContrast) 40.dp else 36.dp)
        ) {
            // Node circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(if (isHighContrast) 32.dp else 28.dp)
            ) {
                when {
                    isCurrent -> {
                        val ringColor = when (themeMode) {
                            DiagramThemeMode.HIGH_CONTRAST -> HighContrastGreen.copy(alpha = 0.5f)
                            DiagramThemeMode.LIGHT -> RailGreen.copy(alpha = 0.25f)
                            DiagramThemeMode.DARK -> RailGreen.copy(alpha = 0.35f)
                        }
                        Box(
                            modifier = Modifier
                                .size(if (isHighContrast) 32.dp else 28.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(ringColor)
                        )
                        val centerColor = if (isHighContrast) HighContrastGreen else RailGreen
                        Box(
                            modifier = Modifier
                                .size(if (isHighContrast) 24.dp else 20.dp)
                                .clip(CircleShape)
                                .background(centerColor)
                                .then(
                                    if (isHighContrast) Modifier.border(2.dp, HighContrastWhite, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Train,
                                contentDescription = "Você está aqui na estação ${station.name}",
                                tint = if (isHighContrast) HighContrastBlack else Color.White,
                                modifier = Modifier.size(if (isHighContrast) 14.dp else 12.dp)
                            )
                        }
                    }
                    isDestination -> {
                        val destColor = if (isHighContrast) HighContrastYellow else WarningAmber
                        Box(
                            modifier = Modifier
                                .size(if (isHighContrast) 28.dp else 24.dp)
                                .clip(CircleShape)
                                .background(destColor)
                                .then(
                                    if (isHighContrast) Modifier.border(2.5.dp, HighContrastWhite, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Destino final ${station.name}",
                                tint = Color.Black,
                                modifier = Modifier.size(if (isHighContrast) 16.dp else 14.dp)
                            )
                        }
                    }
                    isNext -> {
                        val nextBorderColor = if (isHighContrast) HighContrastYellow else WarningAmber
                        val nextBg = when (themeMode) {
                            DiagramThemeMode.HIGH_CONTRAST -> HighContrastBlack
                            DiagramThemeMode.LIGHT -> Color.White
                            DiagramThemeMode.DARK -> SurfaceDarkCard
                        }
                        Box(
                            modifier = Modifier
                                .size(if (isHighContrast) 24.dp else 20.dp)
                                .border(
                                    if (isHighContrast) 4.dp else 3.dp,
                                    nextBorderColor,
                                    CircleShape
                                )
                                .background(nextBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isHighContrast) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(HighContrastYellow)
                                )
                            }
                        }
                    }
                    isPassed -> {
                        val passedBg = when (themeMode) {
                            DiagramThemeMode.HIGH_CONTRAST -> Color(0xFF2A2A2A)
                            DiagramThemeMode.LIGHT -> Color(0xFFE2E8F0)
                            DiagramThemeMode.DARK -> TrensurbBlue.copy(alpha = 0.35f)
                        }
                        Box(
                            modifier = Modifier
                                .size(if (isHighContrast) 18.dp else 14.dp)
                                .clip(CircleShape)
                                .background(passedBg)
                                .then(
                                    if (isHighContrast) Modifier.border(1.5.dp, HighContrastWhite, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Estação ${station.name} percorrida",
                                tint = if (isHighContrast) HighContrastWhite else if (isLight) TrensurbBlue else Color.White,
                                modifier = Modifier.size(if (isHighContrast) 10.dp else 8.dp)
                            )
                        }
                    }
                    station.isTerminal -> {
                        val termBorder = when (themeMode) {
                            DiagramThemeMode.HIGH_CONTRAST -> HighContrastWhite
                            DiagramThemeMode.LIGHT -> TrensurbBlue
                            DiagramThemeMode.DARK -> TrensurbBlueLight
                        }
                        val termInner = when (themeMode) {
                            DiagramThemeMode.HIGH_CONTRAST -> HighContrastCyan
                            DiagramThemeMode.LIGHT -> TrensurbBlue
                            DiagramThemeMode.DARK -> TrensurbBlueLight
                        }
                        val termBg = when (themeMode) {
                            DiagramThemeMode.HIGH_CONTRAST -> HighContrastBlack
                            DiagramThemeMode.LIGHT -> Color.White
                            DiagramThemeMode.DARK -> SurfaceDarkCard
                        }
                        Box(
                            modifier = Modifier
                                .size(if (isHighContrast) 24.dp else 20.dp)
                                .border(3.dp, termBorder, CircleShape)
                                .background(termBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isHighContrast) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(termInner)
                            )
                        }
                    }
                    else -> {
                        val dotBorder = when (themeMode) {
                            DiagramThemeMode.HIGH_CONTRAST -> HighContrastWhite
                            DiagramThemeMode.LIGHT -> TrensurbBlue
                            DiagramThemeMode.DARK -> TrensurbBlueLight
                        }
                        val dotBg = when (themeMode) {
                            DiagramThemeMode.HIGH_CONTRAST -> HighContrastBlack
                            DiagramThemeMode.LIGHT -> Color.White
                            DiagramThemeMode.DARK -> SurfaceDarkCard
                        }
                        Box(
                            modifier = Modifier
                                .size(if (isHighContrast) 16.dp else 12.dp)
                                .border(if (isHighContrast) 3.dp else 2.dp, dotBorder, CircleShape)
                                .background(dotBg)
                        )
                    }
                }
            }

            // Connecting Track Line below the circle
            if (!isLast) {
                val trackColor = when {
                    isHighContrast && isPassed -> HighContrastTrackMuted
                    isHighContrast -> HighContrastCyan
                    isLight && isPassed -> TrensurbBlue.copy(alpha = 0.2f)
                    isLight -> TrensurbBlue
                    isPassed -> TrensurbBlueLight.copy(alpha = 0.3f)
                    else -> TrensurbBlueLight
                }
                Box(
                    modifier = Modifier
                        .width(if (isHighContrast) 6.dp else 4.dp)
                        .height(if (isHighContrast) 36.dp else 34.dp)
                        .background(trackColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Column: Station Details with ETA Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLast) 0.dp else 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val titleColor = when {
                        isHighContrast && isCurrent -> HighContrastGreen
                        isHighContrast && isDestination -> HighContrastYellow
                        isHighContrast && isNext -> HighContrastWhite
                        isHighContrast && isPassed -> HighContrastTextMuted
                        isHighContrast -> HighContrastWhite
                        isCurrent -> RailGreen
                        isDestination -> WarningAmber
                        isNext && isLight -> Color(0xFF0F172A)
                        isNext -> Color.White
                        isPassed && isLight -> Color(0xFF94A3B8)
                        isPassed -> Color(0xFF90A4AE).copy(alpha = 0.6f)
                        isLight -> Color(0xFF1E293B)
                        else -> Color(0xFFF0F4F8)
                    }

                    Text(
                        text = "${station.id}. ${station.name}",
                        style = when {
                            isHighContrast -> MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            isCurrent -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                            isNext || isDestination -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            else -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        },
                        color = titleColor
                    )

                    // Badges for Current / Next / Destination
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val badgeBg = when (themeMode) {
                            DiagramThemeMode.HIGH_CONTRAST -> HighContrastGreen
                            DiagramThemeMode.LIGHT -> RailGreen.copy(alpha = 0.15f)
                            DiagramThemeMode.DARK -> RailGreen.copy(alpha = 0.2f)
                        }
                        val badgeText = when (themeMode) {
                            DiagramThemeMode.HIGH_CONTRAST -> Color.Black
                            else -> RailGreen
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeBg,
                            border = if (isHighContrast) BorderStroke(1.dp, HighContrastWhite) else null
                        ) {
                            Text(
                                text = "AQUI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = badgeText
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isNext) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val badgeBg = when (themeMode) {
                            DiagramThemeMode.HIGH_CONTRAST -> HighContrastYellow
                            DiagramThemeMode.LIGHT -> WarningAmber.copy(alpha = 0.15f)
                            DiagramThemeMode.DARK -> WarningAmber.copy(alpha = 0.2f)
                        }
                        val badgeText = when (themeMode) {
                            DiagramThemeMode.HIGH_CONTRAST -> Color.Black
                            else -> WarningAmber
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeBg,
                            border = if (isHighContrast) BorderStroke(1.dp, HighContrastWhite) else null
                        ) {
                            Text(
                                text = "PRÓXIMA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = badgeText
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isDestination) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val badgeBg = if (isHighContrast) HighContrastYellow else WarningAmber
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeBg,
                            border = if (isHighContrast) BorderStroke(1.dp, HighContrastWhite) else null
                        ) {
                            Text(
                                text = "SEU DESTINO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                val cityColor = when {
                    isHighContrast && isPassed -> HighContrastTextMuted
                    isHighContrast -> HighContrastCyan
                    isPassed && isLight -> Color(0xFFCBD5E1)
                    isPassed -> Color(0xFF90A4AE).copy(alpha = 0.5f)
                    isLight -> Color(0xFF64748B)
                    else -> Color(0xFF90A4AE)
                }

                Text(
                    text = station.city,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isHighContrast) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = cityColor
                )
            }

            // Right Items: ETA Badge and Connection Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ETA Badge for upcoming stations
                if (!isPassed && !isCurrent && !stationEta.isNullOrEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isHighContrast && isNext -> HighContrastYellow.copy(alpha = 0.2f)
                            isHighContrast -> HighContrastBlack
                            isLight && isNext -> WarningAmber.copy(alpha = 0.12f)
                            isLight -> Color(0xFFF1F5F9)
                            isNext -> WarningAmber.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        },
                        border = when {
                            isHighContrast -> BorderStroke(1.dp, if (isNext) HighContrastYellow else HighContrastCyan)
                            isNext -> BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                            else -> null
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Tempo estimado de chegada",
                                tint = when {
                                    isHighContrast && isNext -> HighContrastYellow
                                    isHighContrast -> HighContrastCyan
                                    isNext -> WarningAmber
                                    isLight -> TrensurbBlue
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = stationEta,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = when {
                                    isHighContrast && isNext -> HighContrastYellow
                                    isHighContrast -> HighContrastWhite
                                    isNext -> WarningAmber
                                    isLight -> Color(0xFF1E293B)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }

                // Connection Icons
                if (station.id == 5) {
                    val iconTint = when (themeMode) {
                        DiagramThemeMode.HIGH_CONTRAST -> HighContrastCyan
                        DiagramThemeMode.LIGHT -> TrensurbBlue
                        DiagramThemeMode.DARK -> TrensurbBlueLight
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (isHighContrast) HighContrastCyan.copy(alpha = 0.2f) else Color.Transparent
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flight,
                            contentDescription = "Aeroporto Salgado Filho e Aeromóvel",
                            tint = iconTint,
                            modifier = Modifier
                                .padding(if (isHighContrast) 2.dp else 0.dp)
                                .size(if (isHighContrast) 18.dp else 16.dp)
                        )
                    }
                }
                if (station.id in listOf(2, 4, 10, 21)) {
                    val iconTint = when (themeMode) {
                        DiagramThemeMode.HIGH_CONTRAST -> HighContrastYellow
                        DiagramThemeMode.LIGHT -> Color(0xFF64748B)
                        DiagramThemeMode.DARK -> Color(0xFF90A4AE)
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (isHighContrast) HighContrastYellow.copy(alpha = 0.2f) else Color.Transparent
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = "Terminal Rodoviário e Integração Ônibus",
                            tint = iconTint,
                            modifier = Modifier
                                .padding(if (isHighContrast) 2.dp else 0.dp)
                                .size(if (isHighContrast) 18.dp else 16.dp)
                        )
                    }
                }
                if (station.id in listOf(9, 11, 16)) {
                    val iconTint = when (themeMode) {
                        DiagramThemeMode.HIGH_CONTRAST -> HighContrastWhite
                        DiagramThemeMode.LIGHT -> Color(0xFF64748B)
                        DiagramThemeMode.DARK -> Color(0xFF90A4AE)
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (isHighContrast) HighContrastWhite.copy(alpha = 0.2f) else Color.Transparent
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Universidade",
                            tint = iconTint,
                            modifier = Modifier
                                .padding(if (isHighContrast) 2.dp else 0.dp)
                                .size(if (isHighContrast) 18.dp else 16.dp)
                        )
                    }
                }
            }
        }
    }
}

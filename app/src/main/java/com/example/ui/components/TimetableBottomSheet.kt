package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DayType
import com.example.model.ScheduleInterval
import com.example.model.TrainDirection
import com.example.model.TrensurbTimetableData
import com.example.ui.theme.RailGreen
import com.example.ui.theme.TrensurbBlue
import com.example.ui.theme.WarningAmber
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialDirection: TrainDirection = TrainDirection.TO_NOVO_HAMBURGO
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val now = remember { Calendar.getInstance() }
    val currentDayType = remember { TrensurbTimetableData.getCurrentDayType(now) }
    val currentMinuteOfDay = remember { now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) }
    val isOperating = remember { TrensurbTimetableData.isOperatingNow(now) }

    var selectedDayType by remember { mutableStateOf(currentDayType) }
    var selectedDirection by remember {
        mutableStateOf(
            if (initialDirection == TrainDirection.TO_MERCADO) TrainDirection.TO_MERCADO
            else TrainDirection.TO_NOVO_HAMBURGO
        )
    }

    val activeTimetable = remember(selectedDayType, selectedDirection) {
        TrensurbTimetableData.getTimetable(selectedDayType, selectedDirection)
    }

    val currentActiveInterval = remember(selectedDayType, selectedDirection, currentMinuteOfDay) {
        if (selectedDayType == currentDayType && isOperating) {
            activeTimetable.getCurrentInterval(currentMinuteOfDay)
        } else {
            null
        }
    }

    val upcomingDepartures = remember(selectedDayType, selectedDirection, currentMinuteOfDay) {
        if (selectedDayType == currentDayType) {
            activeTimetable.getUpcomingDepartures(currentMinuteOfDay, count = 5)
        } else {
            // Se for outro dia, mostra as primeiras do dia
            activeTimetable.generateAllDepartures().take(5)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("timetable_bottom_sheet"),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header with Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(TrensurbBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = TrensurbBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Horários Oficiais Trensurb",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Linha 1 • Frequências e Partidas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_timetable_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar horários",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Current Live Status Card
                item {
                    LiveStatusCard(
                        isOperating = isOperating,
                        currentInterval = currentActiveInterval,
                        currentTimeFormatted = ScheduleInterval.formatMinutes(currentMinuteOfDay)
                    )
                }

                // 2. Day Type Selector (Dias Úteis, Sábados, Domingos)
                item {
                    Column {
                        Text(
                            text = "Dia da Semana",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DayType.values().forEach { day ->
                                val isSelected = selectedDayType == day
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedDayType = day },
                                    label = { Text(day.label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TrensurbBlue,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 3. Direction Selector (Terminal Origem)
                item {
                    Column {
                        Text(
                            text = "Terminal de Partida",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isMercado = selectedDirection == TrainDirection.TO_NOVO_HAMBURGO
                            FilterChip(
                                selected = isMercado,
                                onClick = { selectedDirection = TrainDirection.TO_NOVO_HAMBURGO },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Train,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text("Mercado ➔ NH", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TrensurbBlue,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            val isNH = selectedDirection == TrainDirection.TO_MERCADO
                            FilterChip(
                                selected = isNH,
                                onClick = { selectedDirection = TrainDirection.TO_MERCADO },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Train,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text("Novo Hamburgo ➔ Mercado", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TrensurbBlue,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 4. Upcoming Departures Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upcoming_departures_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Train,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Partidas de ${activeTimetable.terminalName}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "05h às 23h",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (selectedDayType == currentDayType) "Próximas partidas programadas a partir de agora:"
                                else "Primeiras partidas programadas do dia:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                upcomingDepartures.forEach { timeStr ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = timeStr,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Schedule Table of Intervals (Headway)
                item {
                    Text(
                        text = "Intervalos entre Trens (Headway Oficial)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(activeTimetable.intervals) { interval ->
                    val isCurrent = (selectedDayType == currentDayType) && isOperating && (interval == currentActiveInterval)
                    IntervalRowItem(
                        interval = interval,
                        isCurrent = isCurrent
                    )
                }

                // 6. Practical Info & Official Explanations
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Como funciona o monitoramento oficial?",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• Tempo total de viagem: ~53 a 55 minutos entre Mercado e Novo Hamburgo.\n" +
                                                "• Abertura das estações: 04:50 • Encerramento: 23:00.\n" +
                                                "• Os letreiros das estações físicas são gerenciados pelo Centro de Controle Operacional (CCO) por meio de sensores nos trilhos. Para consultar a posição dos trens na linha você também pode usar o app oficial Trensurb Mobile ou o Trensurb BOT no Telegram.",
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun LiveStatusCard(
    isOperating: Boolean,
    currentInterval: ScheduleInterval?,
    currentTimeFormatted: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOperating) RailGreen.copy(alpha = 0.12f) else WarningAmber.copy(alpha = 0.12f)
        ),
        border = BorderStroke(
            1.dp,
            if (isOperating) RailGreen.copy(alpha = 0.4f) else WarningAmber.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isOperating) RailGreen else WarningAmber)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isOperating) "Metrô em Operação Normal" else "Metrô Fora de Operação",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isOperating) RailGreen else WarningAmber
                        )
                    )
                    Text(
                        text = "• $currentTimeFormatted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isOperating) {
                        if (currentInterval != null) {
                            "Intervalo programado agora: a cada ${currentInterval.intervalMinutes} min" +
                                    if (currentInterval.isPeak) " (Horário de Pico)" else ""
                        } else {
                            "Viagens partindo a cada 10 a 15 min conforme a grade oficial"
                        }
                    } else {
                        "A Linha 1 opera diariamente das 05:00 às 23:00. Primeira partida às 05:00."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun IntervalRowItem(
    interval: ScheduleInterval,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = if (isCurrent) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else if (interval.isPeak) {
            BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Time range & note
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${interval.startTimeFormatted} às ${interval.endTimeFormatted}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "AGORA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                if (interval.note.isNotEmpty()) {
                    Text(
                        text = interval.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (interval.isPeak) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Frequency Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    interval.isPeak -> WarningAmber.copy(alpha = 0.18f)
                    isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surface
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "A cada ${interval.intervalMinutes} min",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = when {
                                interval.isPeak -> WarningAmber
                                isCurrent -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    )
                }
            }
        }
    }
}

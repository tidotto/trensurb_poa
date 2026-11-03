package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AlertRed
import com.example.ui.theme.RailGreen
import com.example.ui.theme.WarningAmber

enum class LineOperationStatus(val label: String, val color: Color) {
    NORMAL("Operação Normal", RailGreen),
    ATTENTION("Operação com Restrições", WarningAmber),
    SUSPENDED("Operação Suspensa", AlertRed)
}

enum class AeromovelStatus(val label: String, val isOperational: Boolean) {
    OPERATIONAL("Operando Normalmente", true),
    INOPERATIVE("Inoperante", false),
    MAINTENANCE("Em Manutenção", false)
}

data class LiveTrain(
    val trainId: String,
    val isAirConditioned: Boolean = true,
    val direction: TrainDirection = TrainDirection.TO_NOVO_HAMBURGO,
    val stationId: Int? = null,
    val betweenStations: Pair<Int, Int>? = null,
    val statusDescription: String = ""
)

data class LiveStationArrival(
    val stationId: Int,
    val minutesRemaining: Int,
    val direction: TrainDirection,
    val isAirConditioned: Boolean = true,
    val trainPrefix: String = ""
)

data class LiveSystemInfo(
    val lineStatus: LineOperationStatus = LineOperationStatus.NORMAL,
    val statusMessage: String = "Linha 1 operando em intervalos regulares.",
    val affectedSection: String? = null,
    val aeromovelStatus: AeromovelStatus = AeromovelStatus.OPERATIONAL,
    val lastUpdatedFormatted: String = ""
)

object OfficialContacts {
    const val TELEUSER_PHONE = "0800 644 0107"
    const val EMERGENCY_PHONE = "(51) 3363-8026"
    const val OMBUDSMAN_EMAIL = "ouvidoria@trensurb.gov.br"
    const val WEBSITE_URL = "https://www.trensurb.gov.br"
}


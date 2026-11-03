package com.example.model

import java.util.Calendar

enum class DayType(val label: String, val shortLabel: String) {
    WEEKDAY("Dias Úteis", "Seg-Sex"),
    SATURDAY("Sábados", "Sáb"),
    SUNDAY_HOLIDAY("Domingos e Feriados", "Dom/Fer")
}

data class ScheduleInterval(
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val intervalMinutes: Int,
    val isPeak: Boolean = false,
    val note: String = ""
) {
    val startTimeFormatted: String = formatMinutes(startMinuteOfDay)
    val endTimeFormatted: String = formatMinutes(endMinuteOfDay)

    fun contains(minuteOfDay: Int): Boolean {
        return minuteOfDay in startMinuteOfDay until endMinuteOfDay
    }

    companion object {
        fun formatMinutes(totalMinutes: Int): String {
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            return "%02d:%02d".format(h, m)
        }

        fun parseTimeString(time: String): Int {
            val parts = time.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return h * 60 + m
        }
    }
}

data class TerminalTimetable(
    val terminalName: String,
    val destinationName: String,
    val direction: TrainDirection,
    val dayType: DayType,
    val intervals: List<ScheduleInterval>,
    val firstDepartureTime: String = "05:00",
    val lastDepartureTime: String = "23:00"
) {
    /**
     * Gera a lista completa de horários de partida programados a partir dos intervalos.
     */
    fun generateAllDepartures(): List<String> {
        val departures = mutableListOf<Int>()
        var current = ScheduleInterval.parseTimeString(firstDepartureTime)
        val endLimit = ScheduleInterval.parseTimeString(lastDepartureTime)

        while (current <= endLimit) {
            departures.add(current)
            val interval = intervals.firstOrNull { it.contains(current) }?.intervalMinutes
                ?: intervals.lastOrNull()?.intervalMinutes
                ?: 15
            current += interval
        }
        return departures.map { ScheduleInterval.formatMinutes(it) }
    }

    fun getCurrentInterval(currentMinuteOfDay: Int): ScheduleInterval? {
        return intervals.firstOrNull { it.contains(currentMinuteOfDay) }
    }

    fun getUpcomingDepartures(currentMinuteOfDay: Int, count: Int = 4): List<String> {
        val all = generateAllDepartures()
        val currentStr = ScheduleInterval.formatMinutes(currentMinuteOfDay)
        val future = all.filter { it >= currentStr }
        return if (future.isNotEmpty()) {
            future.take(count)
        } else {
            listOf("Encerrado hoje (Próxima partida às $firstDepartureTime)")
        }
    }
}

object TrensurbTimetableData {

    private fun time(h: Int, m: Int): Int = h * 60 + m

    // 1. DIAS ÚTEIS - MERCADO -> NOVO HAMBURGO
    private val WEEKDAY_MERCADO_INTERVALS = listOf(
        ScheduleInterval(time(5, 0), time(6, 0), 14, false, "Início da operação matinal"),
        ScheduleInterval(time(6, 0), time(7, 31), 10, false, "Aumento de oferta"),
        ScheduleInterval(time(7, 31), time(16, 16), 12, false, "Intervalo padrão diurno"),
        ScheduleInterval(time(16, 16), time(17, 6), 10, false, "Pré-pico da tarde"),
        ScheduleInterval(time(17, 6), time(18, 36), 8, true, "Horário de Pico da Tarde"),
        ScheduleInterval(time(18, 36), time(19, 36), 10, false, "Descompressão pós-pico"),
        ScheduleInterval(time(19, 36), time(20, 0), 12, false, "Transição noturna"),
        ScheduleInterval(time(20, 0), time(22, 5), 15, false, "Operação noturna"),
        ScheduleInterval(time(22, 5), time(23, 1), 20, false, "Encerramento da noite")
    )

    // 2. DIAS ÚTEIS - NOVO HAMBURGO -> MERCADO
    private val WEEKDAY_NH_INTERVALS = listOf(
        ScheduleInterval(time(5, 0), time(5, 28), 12, false, "Primeiras viagens do dia"),
        ScheduleInterval(time(5, 28), time(6, 28), 10, false, "Aumento de oferta matinal"),
        ScheduleInterval(time(6, 28), time(7, 40), 8, true, "Horário de Pico da Manhã"),
        ScheduleInterval(time(7, 40), time(8, 12), 10, false, "Descompressão pós-pico"),
        ScheduleInterval(time(8, 12), time(16, 36), 12, false, "Intervalo padrão diurno"),
        ScheduleInterval(time(16, 36), time(18, 48), 10, false, "Reforço da tarde"),
        ScheduleInterval(time(18, 48), time(20, 15), 12, false, "Transição noturna"),
        ScheduleInterval(time(20, 15), time(20, 45), 15, false, "Operação noturna"),
        ScheduleInterval(time(20, 45), time(23, 1), 20, false, "Encerramento da noite")
    )

    // 3. SÁBADOS - MERCADO -> NOVO HAMBURGO
    private val SATURDAY_MERCADO_INTERVALS = listOf(
        ScheduleInterval(time(5, 0), time(16, 43), 12, false, "Frequência regular de sábado"),
        ScheduleInterval(time(16, 43), time(18, 12), 11, false, "Reforço comercial da tarde"),
        ScheduleInterval(time(18, 12), time(19, 0), 12, false, "Transição noturna"),
        ScheduleInterval(time(19, 0), time(21, 45), 15, false, "Operação noturna"),
        ScheduleInterval(time(21, 45), time(23, 1), 20, false, "Últimas partidas da noite")
    )

    // 4. SÁBADOS - NOVO HAMBURGO -> MERCADO
    private val SATURDAY_NH_INTERVALS = listOf(
        ScheduleInterval(time(5, 2), time(6, 14), 12, false, "Primeiras viagens de sábado"),
        ScheduleInterval(time(6, 14), time(7, 25), 10, false, "Reforço matinal de sábado"),
        ScheduleInterval(time(7, 25), time(17, 48), 12, false, "Frequência regular diurna"),
        ScheduleInterval(time(17, 48), time(20, 45), 15, false, "Operação noturna"),
        ScheduleInterval(time(20, 45), time(23, 1), 20, false, "Últimas partidas da noite")
    )

    // 5. DOMINGOS E FERIADOS - MERCADO -> NOVO HAMBURGO
    private val SUNDAY_MERCADO_INTERVALS = listOf(
        ScheduleInterval(time(5, 0), time(23, 1), 20, false, "Frequência uniforme de domingo")
    )

    // 6. DOMINGOS E FERIADOS - NOVO HAMBURGO -> MERCADO
    private val SUNDAY_NH_INTERVALS = listOf(
        ScheduleInterval(time(5, 0), time(21, 15), 20, false, "Frequência regular de domingo"),
        ScheduleInterval(time(21, 15), time(23, 1), 22, false, "Últimas partidas da noite")
    )

    fun getTimetable(dayType: DayType, direction: TrainDirection): TerminalTimetable {
        return when (dayType) {
            DayType.WEEKDAY -> {
                if (direction == TrainDirection.TO_MERCADO) {
                    TerminalTimetable("Novo Hamburgo", "Mercado", direction, dayType, WEEKDAY_NH_INTERVALS)
                } else {
                    TerminalTimetable("Mercado", "Novo Hamburgo", direction, dayType, WEEKDAY_MERCADO_INTERVALS)
                }
            }
            DayType.SATURDAY -> {
                if (direction == TrainDirection.TO_MERCADO) {
                    TerminalTimetable("Novo Hamburgo", "Mercado", direction, dayType, SATURDAY_NH_INTERVALS, firstDepartureTime = "05:02")
                } else {
                    TerminalTimetable("Mercado", "Novo Hamburgo", direction, dayType, SATURDAY_MERCADO_INTERVALS)
                }
            }
            DayType.SUNDAY_HOLIDAY -> {
                if (direction == TrainDirection.TO_MERCADO) {
                    TerminalTimetable("Novo Hamburgo", "Mercado", direction, dayType, SUNDAY_NH_INTERVALS)
                } else {
                    TerminalTimetable("Mercado", "Novo Hamburgo", direction, dayType, SUNDAY_MERCADO_INTERVALS)
                }
            }
        }
    }

    fun getCurrentDayType(calendar: Calendar = Calendar.getInstance()): DayType {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> DayType.SATURDAY
            Calendar.SUNDAY -> DayType.SUNDAY_HOLIDAY
            else -> DayType.WEEKDAY
        }
    }

    fun isOperatingNow(calendar: Calendar = Calendar.getInstance()): Boolean {
        val h = calendar.get(Calendar.HOUR_OF_DAY)
        val m = calendar.get(Calendar.MINUTE)
        val minuteOfDay = h * 60 + m
        return minuteOfDay in (5 * 60)..(23 * 60)
    }
}

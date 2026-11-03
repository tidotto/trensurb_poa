package com.example

import com.example.model.DayType
import com.example.model.TrainDirection
import com.example.model.TrensurbTimetableData
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testTimetableIntervalsWeekdayPeak() {
        val timetable = TrensurbTimetableData.getTimetable(DayType.WEEKDAY, TrainDirection.TO_NOVO_HAMBURGO)
        // 17:30 está no horário de pico da tarde (17:06 às 18:36)
        val interval = timetable.getCurrentInterval(17 * 60 + 30)
        assertNotNull(interval)
        assertEquals(8, interval?.intervalMinutes)
        assertTrue(interval?.isPeak == true)
    }

    @Test
    fun testTimetableUpcomingDepartures() {
        val timetable = TrensurbTimetableData.getTimetable(DayType.WEEKDAY, TrainDirection.TO_NOVO_HAMBURGO)
        val departures = timetable.getUpcomingDepartures(6 * 60, count = 3)
        assertEquals(3, departures.size)
        // Partidas a partir das 06:00
        assertTrue(departures.first() >= "06:00")
    }

    @Test
    fun testTimetableSundayIntervals() {
        val timetable = TrensurbTimetableData.getTimetable(DayType.SUNDAY_HOLIDAY, TrainDirection.TO_NOVO_HAMBURGO)
        val interval = timetable.getCurrentInterval(12 * 60)
        assertNotNull(interval)
        assertEquals(20, interval?.intervalMinutes)
    }
}

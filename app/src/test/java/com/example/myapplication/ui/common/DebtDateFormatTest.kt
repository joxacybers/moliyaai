package com.example.myapplication.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DebtDateFormatTest {

    private val utc = ZoneId.of("UTC")

    @Test
    fun `formatDateTime formats date and time cleanly`() {
        val millis = LocalDateTime.of(2026, 11, 26, 8, 15)
            .atZone(utc)
            .toInstant()
            .toEpochMilli()

        assertEquals("26.11.2026, 08:15", formatDateTime(millis, utc))
    }

    @Test
    fun `calculateDaysBetween calculates calendar day intervals accurately`() {
        val start = LocalDateTime.of(2026, 11, 10, 22, 30)
            .atZone(utc)
            .toInstant()
            .toEpochMilli()
        val sameDay = LocalDateTime.of(2026, 11, 10, 23, 55)
            .atZone(utc)
            .toInstant()
            .toEpochMilli()
        val nextDay = LocalDateTime.of(2026, 11, 11, 6, 0)
            .atZone(utc)
            .toInstant()
            .toEpochMilli()
        val sixteenDaysLater = LocalDateTime.of(2026, 11, 26, 8, 15)
            .atZone(utc)
            .toInstant()
            .toEpochMilli()

        assertEquals(0L, calculateDaysBetween(start, sameDay, utc))
        assertEquals(1L, calculateDaysBetween(start, nextDay, utc))
        assertEquals(16L, calculateDaysBetween(start, sixteenDaysLater, utc))
        // Earlier end timestamp clamps cleanly to 0
        assertEquals(0L, calculateDaysBetween(sixteenDaysLater, start, utc))
    }

    @Test
    fun `ThousandsSeparatorVisualTransformation has structural equality`() {
        assertEquals(
            ThousandsSeparatorVisualTransformation(),
            ThousandsSeparatorVisualTransformation(),
        )
    }
}

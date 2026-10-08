package be.tvgids.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class DagNaamTest {

    private fun ms(tekst: String): Long = ZonedDateTime.parse(tekst).toInstant().toEpochMilli()

    // Donderdag 8 oktober 2026, 's avonds in Brussel.
    private val nu = ms("2026-10-08T21:00+02:00[Europe/Brussels]")

    @Test
    fun benoemtGisterenVandaagEnMorgen() {
        assertEquals("Gisteren", dagNaam(ms("2026-10-07T23:59+02:00[Europe/Brussels]"), nu))
        assertEquals("Vandaag", dagNaam(ms("2026-10-08T00:00+02:00[Europe/Brussels]"), nu))
        assertEquals("Vandaag", dagNaam(ms("2026-10-08T23:59+02:00[Europe/Brussels]"), nu))
        assertEquals("Morgen", dagNaam(ms("2026-10-09T00:00+02:00[Europe/Brussels]"), nu))
    }

    @Test
    fun verderWegToontDeDatum() {
        assertEquals(true, dagNaam(ms("2026-10-10T12:00+02:00[Europe/Brussels]"), nu).startsWith("Za 10 okt"))
    }

    @Test
    fun rekentInBrusselseTijd() {
        // 23:30 UTC is in Brussel al 01:30 de volgende dag.
        assertEquals("Morgen", dagNaam(ms("2026-10-08T23:30Z"), nu))
    }
}

package be.tvgids.app

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class KlokTest {

    @After
    fun herstel() = Klok.meet(System.currentTimeMillis())

    @Test
    fun groteAfwijkingWordtGecorrigeerd() {
        val toestel = 1_800_000_000_000L
        Klok.meet(serverTijd = toestel + 50 * 24 * UUR, toestelTijd = toestel)
        assertEquals(50 * 24 * UUR, Klok.afwijking.value)
    }

    @Test
    fun kleinVerschilIsNetwerkvertraging() {
        val toestel = 1_800_000_000_000L
        Klok.meet(serverTijd = toestel + 30_000, toestelTijd = toestel)
        assertEquals(0L, Klok.afwijking.value)
    }

    @Test
    fun ontbrekendeDateHeaderVeranderdNiets() {
        val toestel = 1_800_000_000_000L
        Klok.meet(serverTijd = toestel - 10 * UUR, toestelTijd = toestel)
        Klok.meet(serverTijd = 0)
        assertEquals(-10 * UUR, Klok.afwijking.value)
    }

    @Test
    fun nachtelijkeVerversingOmHalfVier() {
        val avond = ZonedDateTime.of(2026, 11, 20, 22, 0, 0, 0, BRUSSEL)
        assertEquals(5 * 60 + 30L, VernieuwWerker.totVolgende(avond).toMinutes())
        val nacht = ZonedDateTime.of(2026, 11, 21, 3, 30, 0, 0, BRUSSEL)
        assertEquals(24 * 60L, VernieuwWerker.totVolgende(nacht).toMinutes())
    }
}

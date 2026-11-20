package be.tvgids.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigatieTest {

    private val t0 = 1_800_000_000_000L

    private fun prog(start: Long, stop: Long, titel: String) =
        Programma("een", t0 + start * MIN, t0 + stop * MIN, titel, null, null, null)

    @Test
    fun omhoogOmlaagBlijftOpHetzelfdeTijdstipDankzijOpvulling() {
        // Een zender die pas na twee uur begint: zonder opvulling sprong de focus naar "laat".
        val rij = listOf(prog(120, 180, "laat"))
        val gevuld = metOpvulling(rij, "een", t0, t0 + 240 * MIN)
        assertEquals("laat", rondAnker(rij, t0 + 30 * MIN)?.titel)
        val gekozen = rondAnker(gevuld, t0 + 30 * MIN)!!
        assertEquals(true, gekozen.opvulling)
        assertEquals(t0, gekozen.start)
    }

    @Test
    fun korteProgrammasWordenOvergeslagen() {
        val a = prog(0, 30, "a")
        val lotto = prog(30, 33, "lotto")
        val b = prog(33, 60, "b")
        val rij = listOf(a, lotto, b)
        assertEquals(b, naastLiggend(rij, a, 1))
        assertEquals(a, naastLiggend(rij, b, -1))
        // Wie toch op het korte blok staat, kan er gewoon vanaf.
        assertEquals(b, naastLiggend(rij, lotto, 1))
        // Ook verticaal komt de focus niet op het korte blok terecht, maar op het dichtstbijzijnde.
        assertEquals(a, rondAnker(rij, t0 + 31 * MIN))
    }

    @Test
    fun randVanDeRijGeeftNull() {
        val a = prog(0, 30, "a")
        assertNull(naastLiggend(listOf(a), a, -1))
        assertNull(naastLiggend(listOf(a), a, 1))
    }

    @Test
    fun legeRijenWordenOvergeslagen() {
        val vol = listOf(prog(0, 30, "x"))
        val perRij = listOf(vol, emptyList(), emptyList(), vol)
        assertEquals(3, volgendeRij(perRij, 0, 1))
        assertEquals(0, volgendeRij(perRij, 3, -1))
        assertNull(volgendeRij(perRij, 3, 1))
        assertNull(volgendeRij(perRij, 0, -1))
    }

    @Test
    fun omlaagKiestGeenProgrammaDatNetNaDeLinkerrandEindigt() {
        // Beeld van 14:00 tot 18:00; de focus stond op een lang programma, anker = linkerrand.
        val beeldStart = t0 + 14 * 60 * MIN
        val beeldEind = t0 + 18 * 60 * MIN
        val vroeg = prog(13 * 60L, 14 * 60L + 5, "vroeg begonnen")
        val daarna = prog(14 * 60L + 5, 15 * 60L, "daarna")
        val rij = listOf(vroeg, daarna)
        assertEquals(vroeg, rondAnker(rij, beeldStart))
        assertEquals(daarna, rondAnker(rij, ankerInBeeld(beeldStart, beeldStart, beeldEind)))
    }

    @Test
    fun ankerMiddenInBeeldBlijftOngemoeid() {
        val anker = t0 + 15 * 60 * MIN
        assertEquals(anker, ankerInBeeld(anker, t0 + 14 * 60 * MIN, t0 + 18 * 60 * MIN))
    }

    @Test
    fun zichtbaarBlokLaatHetBeeldStaan() {
        // Beeld 600..900 (minuten in het raster).
        assertNull(nieuweScroll(start = 650f, eind = 700f, huidig = 600f, breedte = 300f, maxScroll = 5000f))
        // Lang programma dat het hele beeld vult.
        assertNull(nieuweScroll(start = 100f, eind = 2000f, huidig = 600f, breedte = 300f, maxScroll = 5000f))
        // Al bezig maar ruim zichtbaar: niet terug naar het begin.
        assertNull(nieuweScroll(start = 300f, eind = 700f, huidig = 600f, breedte = 300f, maxScroll = 5000f))
    }

    @Test
    fun blokLinksBuitenBeeldSchuiftZoWeinigMogelijk() {
        // Past in beeld: helemaal tonen, met 15 minuten ervoor.
        assertEquals(485f, nieuweScroll(start = 500f, eind = 590f, huidig = 600f, breedte = 300f, maxScroll = 5000f))
        // Dagen "Geen uitzending": enkel het einde tonen, niet dagen terug.
        assertEquals(345f, nieuweScroll(start = 0f, eind = 600f, huidig = 600f, breedte = 300f, maxScroll = 5000f))
    }

    @Test
    fun blokRechtsBuitenBeeldKomtVanLinksInBeeld() {
        assertEquals(935f, nieuweScroll(start = 950f, eind = 1000f, huidig = 600f, breedte = 300f, maxScroll = 5000f))
        assertEquals(830f, nieuweScroll(start = 860f, eind = 1000f, huidig = 600f, breedte = 300f, maxScroll = 5000f))
        // Nooit voorbij het einde van het raster.
        assertEquals(700f, nieuweScroll(start = 950f, eind = 1000f, huidig = 600f, breedte = 300f, maxScroll = 700f))
    }
}

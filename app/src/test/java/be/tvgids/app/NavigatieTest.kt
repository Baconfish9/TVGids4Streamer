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
}

package be.tvgids.app

import org.junit.Assert.assertEquals
import org.junit.Test

class OpvullingTest {

    private val t0 = 1_800_000_000_000L

    private fun prog(start: Long, stop: Long, titel: String) =
        Programma("een", t0 + start, t0 + stop, titel, null, null, null)

    private fun beschrijf(rij: List<Programma>) =
        rij.map { if (it.opvulling) "leeg ${(it.start - t0) / MIN}-${(it.stop - t0) / MIN}" else it.titel }

    @Test
    fun gatenVoorTussenEnNaWordenOpgevuld() {
        val rij = listOf(prog(60 * MIN, 90 * MIN, "a"), prog(120 * MIN, 150 * MIN, "b"))
        val uit = metOpvulling(rij, "een", t0, t0 + 180 * MIN)
        assertEquals(listOf("leeg 0-60", "a", "leeg 90-120", "b", "leeg 150-180"), beschrijf(uit))
        assertEquals("Geen uitzending", uit.first().titel)
    }

    @Test
    fun korteGatenBlijvenLeeg() {
        val rij = listOf(prog(0, 30 * MIN, "a"), prog(32 * MIN, 60 * MIN, "b"))
        assertEquals(listOf("a", "b"), beschrijf(metOpvulling(rij, "een", t0, t0 + 60 * MIN)))
    }

    @Test
    fun legeRijBlijftLeeg() {
        assertEquals(emptyList<Programma>(), metOpvulling(emptyList(), "een", t0, t0 + 60 * MIN))
    }
}

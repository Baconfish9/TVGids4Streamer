package be.tvgids.app

import org.junit.Assert.assertEquals
import org.junit.Test

class GeschiedenisTest {

    private val nu = 1_800_000_000_000L

    private fun prog(zender: String, start: Long, stop: Long, titel: String = "t") =
        Programma(zender, start, stop, titel, null, null, null)

    private fun resultaat(vararg p: Programma) =
        EpgResultaat(p.groupBy { it.zenderKey }, emptyList(), emptyList(), nu - UUR)

    @Test
    fun voorbijeProgrammasBlijvenBewaard() {
        val gisteren = prog("een", nu - 30 * UUR, nu - 29 * UUR, "gisteren")
        val nieuw = mapOf("een" to listOf(prog("een", nu - 10 * UUR, nu - 9 * UUR, "vandaag")))
        val uit = metGeschiedenis(nieuw, resultaat(gisteren), nu)
        assertEquals(listOf("gisteren", "vandaag"), uit.getValue("een").map { it.titel })
    }

    @Test
    fun nieuweGegevensWinnenWaarZeOverlappen() {
        val oud = resultaat(
            prog("een", nu - 12 * UUR, nu - 10 * UUR, "oud ervoor"),
            prog("een", nu - 10 * UUR, nu - 9 * UUR, "oude versie"),
        )
        val nieuw = mapOf("een" to listOf(prog("een", nu - 10 * UUR, nu - 9 * UUR, "nieuwe versie")))
        val uit = metGeschiedenis(nieuw, oud, nu)
        assertEquals(listOf("oud ervoor", "nieuwe versie"), uit.getValue("een").map { it.titel })
    }

    @Test
    fun ouderDanEenWeekVerdwijnt() {
        val oud = resultaat(
            prog("een", nu - GESCHIEDENIS - 2 * UUR, nu - GESCHIEDENIS - UUR, "te oud"),
            prog("een", nu - GESCHIEDENIS + UUR, nu - GESCHIEDENIS + 2 * UUR, "net op tijd"),
        )
        val nieuw = mapOf("een" to listOf(prog("een", nu, nu + UUR, "nu")))
        val uit = metGeschiedenis(nieuw, oud, nu)
        assertEquals(listOf("net op tijd", "nu"), uit.getValue("een").map { it.titel })
    }

    @Test
    fun zenderZonderNieuweGegevensHoudtZijnOudeGids() {
        val oud = resultaat(prog("canvas", nu - 5 * UUR, nu - 4 * UUR, "oud"))
        val uit = metGeschiedenis(emptyMap(), oud, nu)
        assertEquals(listOf("oud"), uit.getValue("canvas").map { it.titel })
    }
}

package be.tvgids.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ZendersTest {

    @Test
    fun nieuweZendersKomenOnderaanEnVerdwenenWordenGenegeerd() {
        val alle = listOf("a", "b", "c", "d")
        assertEquals(listOf("c", "a", "b", "d"), samenvoegen(listOf("c", "weg", "a"), alle))
        assertEquals(alle, samenvoegen(emptyList(), alle))
        assertEquals(listOf("b", "a", "c", "d"), samenvoegen(listOf("b", "b", "a"), alle))
    }

    @Test
    fun verplaatsenWisseltMetDeBuur() {
        val z = ZenderInstelling(listOf("a", "b", "c"), emptySet())
        assertEquals(listOf("b", "a", "c"), z.verplaats("b", -1).volgorde)
        assertEquals(listOf("a", "c", "b"), z.verplaats("b", 1).volgorde)
    }

    @Test
    fun verborgenZendersWordenInEenKeerOvergeslagen() {
        val z = ZenderInstelling(listOf("a", "h", "b"), setOf("h"))
        assertEquals(listOf("b", "a", "h"), z.verplaats("b", -1).volgorde)
        assertEquals(listOf("h", "b", "a"), z.verplaats("a", 1).volgorde)
        // De verborgen zender blijft bestaan en komt terug op zijn plaats.
        assertEquals(listOf("b", "a"), z.verplaats("b", -1).zichtbaar)
    }

    @Test
    fun aanDeRandGebeurtNiets() {
        val z = ZenderInstelling(listOf("a", "b"), emptySet())
        assertEquals(z, z.verplaats("a", -1))
        assertEquals(z, z.verplaats("b", 1))
        assertEquals(z, z.verplaats("onbekend", 1))
    }

    @Test
    fun standaardIsEerstVlaamsDanNederlands() {
        val landen = ZenderInstelling.STANDAARD.map { key -> Config.ZENDERS.first { it.key == key }.land }
        assertEquals(landen.sortedBy { it.ordinal }, landen)
        assertEquals(Config.ZENDERS.size, ZenderInstelling.STANDAARD.size)
    }
}

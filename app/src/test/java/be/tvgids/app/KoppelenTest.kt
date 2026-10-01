package be.tvgids.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class KoppelenTest {

    @Test
    fun normaliseerHaaltAccentenEnAchtervoegselsWeg() {
        assertEquals("een", normaliseer("Eén HD"))
        assertEquals("een", normaliseer("een.be"))
        assertEquals("een", normaliseer("EEN"))
        assertEquals("vtm2", normaliseer("VTM 2 HD"))
        assertEquals("canvas", normaliseer("Canvas (Vlaanderen)"))
        assertEquals("npo1", normaliseer("NPO 1 FHD"))
    }

    @Test
    fun aliassenVanVerschillendeZendersBotsenNiet() {
        val gezien = HashMap<String, String>()
        for (z in Config.ZENDERS) {
            for (naam in z.aliassen + z.naam) {
                val n = normaliseer(naam)
                assertTrue("lege naam voor '$naam'", n.isNotEmpty())
                val ander = gezien.put(n, z.key)
                assertTrue("'$naam' ($n) van ${z.key} botst met $ander", ander == null || ander == z.key)
            }
        }
    }

    @Test
    fun zenderSleutelsZijnUniek() {
        val keys = Config.ZENDERS.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun parseTijdMetZone() {
        assertEquals(
            Instant.parse("2026-09-30T18:30:00Z").toEpochMilli(),
            XmltvParser.parseTijd("20260930203000 +0200"),
        )
    }

    @Test
    fun parseTijdZonderZoneIsBrusselseTijd() {
        // In januari is het in Brussel UTC+1.
        assertEquals(
            Instant.parse("2026-01-15T19:00:00Z").toEpochMilli(),
            XmltvParser.parseTijd("20260115200000"),
        )
    }

    @Test
    fun parseTijdVultOntbrekendeSecondenAan() {
        assertEquals(
            Instant.parse("2026-09-30T18:30:00Z").toEpochMilli(),
            XmltvParser.parseTijd("202609302030 +0200"),
        )
    }

    @Test
    fun parseTijdOngeldig() {
        assertNull(XmltvParser.parseTijd(null))
        assertNull(XmltvParser.parseTijd(""))
        assertNull(XmltvParser.parseTijd("geen tijd"))
        assertNull(XmltvParser.parseTijd("20260930203000 +99"))
    }
}

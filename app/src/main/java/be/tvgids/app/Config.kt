package be.tvgids.app

import java.text.Normalizer

enum class Land(val label: String) { BE("Vlaams"), NL("Nederlands") }

data class ZenderDef(
    /** Interne sleutel, moet uniek zijn. */
    val key: String,
    /** Naam zoals getoond in de gids. */
    val naam: String,
    val land: Land,
    /** Accentkleur (ARGB). */
    val kleur: Long,
    /** Namen waaronder de zender in XMLTV-bestanden kan voorkomen. */
    val aliassen: List<String>,
    /** App die geopend wordt bij "Kijken" (optioneel). */
    val appNaam: String? = null,
    val appPakket: String? = null,
)

object Config {

    /**
     * XMLTV-bronnen (gewoon .xml of .xml.gz). Ze worden na elkaar ingelezen;
     * een bron die niet bestaat of faalt wordt overgeslagen en staat dan als
     * fout in het statusscherm. Voeg gerust je eigen bron toe, bijvoorbeeld
     * een bestand dat je zelf met iptv-org/epg genereert.
     */
    val EPG_BRONNEN: List<String> = listOf(
        "https://www.open-epg.com/files/belgium1.xml",
        "https://www.open-epg.com/files/belgium2.xml",
        "https://www.open-epg.com/files/netherlands1.xml",
        "https://www.open-epg.com/files/netherlands2.xml",
    )

    // Pakketnamen: controleer ze op je Streamer met
    //   adb shell pm list packages | grep -i -E "vrt|vtm|npo|uitzending"
    private const val VRT_MAX = "be.vrt.vrtnu"
    private const val VTM_GO = "be.vmma.vtm.zenderapp"
    private const val NPO_START = "nl.uitzendinggemist"

    val ZENDERS: List<ZenderDef> = listOf(
        // Vlaanderen
        ZenderDef("vrt1", "VRT 1", Land.BE, 0xFF4A90E2, listOf("VRT 1", "Eén", "Een", "VRT Een"), "VRT MAX", VRT_MAX),
        ZenderDef("canvas", "VRT Canvas", Land.BE, 0xFFE0524A, listOf("VRT Canvas", "Canvas"), "VRT MAX", VRT_MAX),
        ZenderDef("ketnet", "Ketnet", Land.BE, 0xFFF5C542, listOf("Ketnet", "VRT Ketnet"), "VRT MAX", VRT_MAX),
        ZenderDef("vtm", "VTM", Land.BE, 0xFFE8457A, listOf("VTM"), "VTM GO", VTM_GO),
        ZenderDef("vtm2", "VTM 2", Land.BE, 0xFF9B6BE8, listOf("VTM 2", "Q2"), "VTM GO", VTM_GO),
        ZenderDef("vtm3", "VTM 3", Land.BE, 0xFF5CC8A8, listOf("VTM 3", "Vitaya"), "VTM GO", VTM_GO),
        ZenderDef("vtm4", "VTM 4", Land.BE, 0xFFF08A3C, listOf("VTM 4", "CAZ"), "VTM GO", VTM_GO),
        ZenderDef("vtmgold", "VTM GOLD", Land.BE, 0xFFD4AF37, listOf("VTM GOLD"), "VTM GO", VTM_GO),
        // Play-zenders (sinds de nieuwe namen: Play4 werd Play, enz.)
        ZenderDef("play", "Play", Land.BE, 0xFF2EC4D6, listOf("Play", "Play4", "Play 4", "VIER")),
        ZenderDef("playfictie", "Play Fictie", Land.BE, 0xFF2EC4D6, listOf("Play Fictie")),
        ZenderDef("playactie", "Play Actie", Land.BE, 0xFF2EC4D6, listOf("Play Actie")),
        ZenderDef("playcrime", "Play Crime", Land.BE, 0xFF2EC4D6, listOf("Play Crime")),
        ZenderDef("playreality", "Play Reality", Land.BE, 0xFF2EC4D6, listOf("Play Reality")),
        // Nederland (NPO)
        ZenderDef("npo1", "NPO 1", Land.NL, 0xFFFF7A1A, listOf("NPO 1", "Nederland 1"), "NPO Start", NPO_START),
        ZenderDef("npo2", "NPO 2", Land.NL, 0xFFFF7A1A, listOf("NPO 2", "Nederland 2"), "NPO Start", NPO_START),
        ZenderDef("npo3", "NPO 3", Land.NL, 0xFFFF7A1A, listOf("NPO 3", "Nederland 3"), "NPO Start", NPO_START),
    )
}

private val TE_SCHRAPPEN = listOf("hd", "fhd", "uhd", "sd", "be", "nl", "vlaanderen", "belgie", "belgium", "nederland", "netherlands")

/**
 * Maakt van "Eén HD", "een.be" of "EEN" allemaal "een", zodat namen uit
 * verschillende bronnen met elkaar vergeleken kunnen worden.
 */
fun normaliseer(invoer: String): String {
    var n = Normalizer.normalize(invoer, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .replace("&", "en")
        .replace(Regex("[^a-z0-9]"), "")
    for (v in listOf("be", "vl", "nl")) {
        if (n.startsWith(v) && n.length > v.length + 2) {
            val rest = n.substring(v.length)
            // alleen als het restant zelf een gekende zendernaam kan zijn
            if (rest.first().isLetter()) n = rest
        }
    }
    var veranderd = true
    while (veranderd) {
        veranderd = false
        for (s in TE_SCHRAPPEN) {
            if (n.length > s.length + 1 && n.endsWith(s)) {
                n = n.dropLast(s.length)
                veranderd = true
            }
        }
    }
    return n
}

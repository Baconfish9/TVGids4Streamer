package be.tvgids.app

import android.content.Context
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.TreeSet
import java.util.zip.GZIPInputStream

const val MIN = 60_000L
const val UUR = 60 * MIN

data class Programma(
    val zenderKey: String,
    val start: Long,
    val stop: Long,
    val titel: String,
    val subtitel: String?,
    val beschrijving: String?,
    val categorie: String?,
)

data class BronStatus(
    val url: String,
    val ok: Boolean,
    val melding: String,
)

data class EpgResultaat(
    val programmas: Map<String, List<Programma>>,
    val bronnen: List<BronStatus>,
    /** Zendernamen uit de bronnen die aan geen enkele zender gekoppeld werden. */
    val nietGekoppeld: List<String>,
    val opgehaald: Long,
)

/** Wat er in één bron gevonden werd, voor het statusscherm. */
class ParseInfo {
    var zenders = 0
    var gekoppeld = 0
    var programmas = 0
    var voorGekoppeld = 0
    var inVenster = 0
    var vroegste = Long.MAX_VALUE
    var laatste = Long.MIN_VALUE
}

/** Tijdzone van de gids; los van de toestelinstelling (een emulator staat vaak op UTC). */
val BRUSSEL: ZoneId = ZoneId.of("Europe/Brussels")

private val FMT_KORT = DateTimeFormatter.ofPattern("dd/MM HH:mm")
private fun kort(ms: Long): String =
    java.time.Instant.ofEpochMilli(ms).atZone(BRUSSEL).format(FMT_KORT)

fun ParseInfo.samenvatting(): String {
    val periode = if (programmas > 0) " Gegevens van ${kort(vroegste)} tot ${kort(laatste)}." else ""
    return "$gekoppeld van $zenders zenders gekoppeld. $programmas programma's in bron, " +
        "$voorGekoppeld voor onze zenders, waarvan $inVenster in de komende dag.$periode"
}

// ---------------------------------------------------------------------------
// XMLTV-parser (streaming, zodat grote bestanden geen geheugenproblemen geven)
// ---------------------------------------------------------------------------

object XmltvParser {

    private val BASIS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    /** XMLTV-tijd zoals "20260930203000 +0200" naar epoch-milliseconden. */
    fun parseTijd(s: String?): Long? {
        if (s.isNullOrBlank()) return null
        val delen = s.trim().split(Regex("\\s+"))
        val cijfers = delen[0].filter { it.isDigit() }.padEnd(14, '0').take(14)
        return try {
            val ldt = LocalDateTime.parse(cijfers, BASIS)
            val zone: ZoneId = if (delen.size > 1) ZoneOffset.of(delen[1]) else BRUSSEL
            ldt.atZone(zone).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    fun parse(
        input: InputStream,
        koppel: (String, List<String>) -> String?,
        vanaf: Long,
        tot: Long,
        uit: MutableMap<String, MutableMap<Long, Programma>>,
        nietGekoppeld: MutableSet<String>,
    ): ParseInfo {
        val info = ParseInfo()
        val parser = Xml.newPullParser()
        parser.setInput(input, null)
        val idNaarKey = HashMap<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "channel" -> leesZender(parser, koppel, idNaarKey, nietGekoppeld, info)
                    "programme" -> leesProgramma(parser, idNaarKey, vanaf, tot, uit, info)
                }
            }
            event = parser.next()
        }
        info.gekoppeld = idNaarKey.size
        return info
    }

    private fun leesZender(
        p: XmlPullParser,
        koppel: (String, List<String>) -> String?,
        idNaarKey: MutableMap<String, String>,
        nietGekoppeld: MutableSet<String>,
        info: ParseInfo,
    ) {
        info.zenders++
        val id = p.getAttributeValue(null, "id") ?: ""
        val namen = ArrayList<String>()
        val diepte = p.depth
        while (true) {
            val e = p.next()
            if (e == XmlPullParser.END_DOCUMENT) break
            if (e == XmlPullParser.END_TAG && p.depth == diepte) break
            if (e == XmlPullParser.START_TAG && p.name == "display-name") {
                val t = tekst(p)
                if (t.isNotEmpty()) namen.add(t)
            }
        }
        val key = koppel(id, namen)
        if (key != null) {
            idNaarKey[id] = key
        } else {
            nietGekoppeld.add(namen.firstOrNull() ?: id)
        }
    }

    private fun leesProgramma(
        p: XmlPullParser,
        idNaarKey: Map<String, String>,
        vanaf: Long,
        tot: Long,
        uit: MutableMap<String, MutableMap<Long, Programma>>,
        info: ParseInfo,
    ) {
        info.programmas++
        val key = idNaarKey[p.getAttributeValue(null, "channel") ?: ""]
        val start = parseTijd(p.getAttributeValue(null, "start"))
        if (start != null) {
            if (start < info.vroegste) info.vroegste = start
            if (start > info.laatste) info.laatste = start
        }
        if (key == null || start == null) {
            sla(p)
            return
        }
        info.voorGekoppeld++
        val stop = parseTijd(p.getAttributeValue(null, "stop")) ?: (start + 30 * MIN)
        if (stop <= vanaf || start >= tot) {
            sla(p)
            return
        }
        info.inVenster++
        var titel: String? = null
        var sub: String? = null
        var desc: String? = null
        var cat: String? = null
        val diepte = p.depth
        while (true) {
            val e = p.next()
            if (e == XmlPullParser.END_DOCUMENT) break
            if (e == XmlPullParser.END_TAG && p.depth == diepte) break
            if (e == XmlPullParser.START_TAG) {
                when (p.name) {
                    "title" -> { val t = tekst(p); if (titel.isNullOrEmpty()) titel = t }
                    "sub-title" -> { val t = tekst(p); if (sub.isNullOrEmpty()) sub = t }
                    "desc" -> { val t = tekst(p); if (desc.isNullOrEmpty()) desc = t }
                    "category" -> { val t = tekst(p); if (cat.isNullOrEmpty()) cat = t }
                }
            }
        }
        val prog = Programma(
            zenderKey = key,
            start = start,
            stop = stop,
            titel = if (titel.isNullOrEmpty()) "Onbekend programma" else titel!!,
            subtitel = sub?.ifBlank { null },
            beschrijving = desc?.ifBlank { null },
            categorie = cat?.ifBlank { null },
        )
        uit.getOrPut(key) { HashMap() }.putIfAbsent(start, prog)
    }

    /** Leest alle tekst binnen het huidige element en eindigt op zijn END_TAG. */
    private fun tekst(p: XmlPullParser): String {
        val d = p.depth
        val sb = StringBuilder()
        while (true) {
            val e = p.next()
            if (e == XmlPullParser.TEXT) sb.append(p.text)
            if (e == XmlPullParser.END_DOCUMENT) break
            if (e == XmlPullParser.END_TAG && p.depth == d) break
        }
        return sb.toString().trim()
    }

    /** Slaat het huidige element met alle kinderen over. */
    private fun sla(p: XmlPullParser) {
        val d = p.depth
        while (true) {
            val e = p.next()
            if (e == XmlPullParser.END_DOCUMENT) return
            if (e == XmlPullParser.END_TAG && p.depth == d) return
        }
    }
}

// ---------------------------------------------------------------------------
// Ophalen en cachen
// ---------------------------------------------------------------------------

class EpgRepository(context: Context) {

    private val cacheBestand = File(context.filesDir, "epg-cache.json")

    private val aliasIndex: Map<String, String> = buildMap {
        for (z in Config.ZENDERS) {
            for (naam in z.aliassen + z.naam) put(normaliseer(naam), z.key)
        }
    }

    fun koppel(id: String, namen: List<String>): String? {
        for (n in namen + id) {
            aliasIndex[normaliseer(n)]?.let { return it }
        }
        return null
    }

    suspend fun laadCache(): EpgResultaat? = withContext(Dispatchers.IO) {
        try {
            if (cacheBestand.exists()) vanJson(cacheBestand.readText()) else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun haalOp(vorige: EpgResultaat?): EpgResultaat = withContext(Dispatchers.IO) {
        val nu = System.currentTimeMillis()
        val vanaf = nu - 3 * UUR
        val tot = nu + 30 * UUR

        // Alle bronnen tegelijk ophalen, elk in een eigen verzameling.
        val perBron = Config.EPG_BRONNEN.map { url ->
            async {
                val progs: MutableMap<String, MutableMap<Long, Programma>> = HashMap()
                val nietHier = HashSet<String>()
                val status = try {
                    open(url).use { ins ->
                        val info = XmltvParser.parse(ins, this@EpgRepository::koppel, vanaf, tot, progs, nietHier)
                        BronStatus(url, info.gekoppeld > 0 && info.inVenster > 0, info.samenvatting())
                    }
                } catch (e: Exception) {
                    BronStatus(url, false, e.message ?: e.javaClass.simpleName)
                }
                Triple(progs, nietHier, status)
            }
        }.awaitAll()

        // Samenvoegen in de volgorde van EPG_BRONNEN: bij hetzelfde begintijdstip wint de eerste bron.
        val verzameld: MutableMap<String, MutableMap<Long, Programma>> = HashMap()
        val niet = TreeSet<String>(String.CASE_INSENSITIVE_ORDER)
        val statussen = ArrayList<BronStatus>()
        for ((progs, nietHier, status) in perBron) {
            for ((key, m) in progs) {
                val doel = verzameld.getOrPut(key) { HashMap() }
                for ((start, p) in m) doel.putIfAbsent(start, p)
            }
            niet.addAll(nietHier)
            statussen.add(status)
        }

        val programmas = verzameld.mapValues { (_, m) -> opschonen(m.values) }
        val totaal = programmas.values.sumOf { it.size }

        if (totaal == 0 && vorige != null) {
            // Niets nieuws gevonden: oude gids houden, wel de status tonen.
            vorige.copy(bronnen = statussen, nietGekoppeld = niet.toList())
        } else {
            val r = EpgResultaat(programmas, statussen, niet.toList(), nu)
            if (totaal > 0) {
                try {
                    // Eerst naar een tijdelijk bestand, zodat een onderbroken
                    // schrijfactie de bestaande cache niet beschadigt.
                    val tijdelijk = File(cacheBestand.path + ".tmp")
                    tijdelijk.writeText(naarJson(r))
                    if (!tijdelijk.renameTo(cacheBestand)) tijdelijk.delete()
                } catch (e: Exception) {
                }
            }
            r
        }
    }

    private fun open(adres: String): InputStream {
        val c = URL(adres).openConnection() as HttpURLConnection
        c.connectTimeout = 20_000
        c.readTimeout = 90_000
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "TVGids-GoogleTV/1.0")
        val code = c.responseCode
        if (code !in 200..299) {
            c.disconnect()
            throw IOException("HTTP $code")
        }
        val buf = BufferedInputStream(c.inputStream, 64 * 1024)
        buf.mark(4)
        val b1 = buf.read()
        val b2 = buf.read()
        buf.reset()
        return if (b1 == 0x1f && b2 == 0x8b) BufferedInputStream(GZIPInputStream(buf), 64 * 1024) else buf
    }

    /** Sorteert en haalt overlappingen weg (sommige bronnen zijn slordig). */
    private fun opschonen(lijst: Collection<Programma>): List<Programma> {
        val gesorteerd = lijst.sortedBy { it.start }
        val uit = ArrayList<Programma>(gesorteerd.size)
        for (i in gesorteerd.indices) {
            var p = gesorteerd[i]
            val volgende = gesorteerd.getOrNull(i + 1)
            if (volgende != null && p.stop > volgende.start) p = p.copy(stop = volgende.start)
            if (p.stop > p.start) uit.add(p)
        }
        return uit
    }

    private fun naarJson(r: EpgResultaat): String {
        val o = JSONObject()
        o.put("opgehaald", r.opgehaald)
        val progs = JSONObject()
        for ((k, lijst) in r.programmas) {
            val a = JSONArray()
            for (p in lijst) {
                val j = JSONObject()
                j.put("s", p.start)
                j.put("e", p.stop)
                j.put("t", p.titel)
                if (p.subtitel != null) j.put("st", p.subtitel)
                if (p.beschrijving != null) j.put("d", p.beschrijving)
                if (p.categorie != null) j.put("c", p.categorie)
                a.put(j)
            }
            progs.put(k, a)
        }
        o.put("programmas", progs)
        val bronnen = JSONArray()
        for (b in r.bronnen) {
            val j = JSONObject()
            j.put("url", b.url)
            j.put("ok", b.ok)
            j.put("melding", b.melding)
            bronnen.put(j)
        }
        o.put("bronnen", bronnen)
        val niet = JSONArray()
        for (n in r.nietGekoppeld) niet.put(n)
        o.put("niet", niet)
        return o.toString()
    }

    private fun vanJson(tekst: String): EpgResultaat {
        val o = JSONObject(tekst)
        val progsJ = o.getJSONObject("programmas")
        val programmas = HashMap<String, List<Programma>>()
        val keys = progsJ.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val a = progsJ.getJSONArray(k)
            val lijst = ArrayList<Programma>(a.length())
            for (i in 0 until a.length()) {
                val j = a.getJSONObject(i)
                lijst.add(
                    Programma(
                        zenderKey = k,
                        start = j.getLong("s"),
                        stop = j.getLong("e"),
                        titel = j.getString("t"),
                        subtitel = if (j.has("st")) j.getString("st") else null,
                        beschrijving = if (j.has("d")) j.getString("d") else null,
                        categorie = if (j.has("c")) j.getString("c") else null,
                    )
                )
            }
            programmas[k] = lijst
        }
        val bronnen = ArrayList<BronStatus>()
        val bJ = o.optJSONArray("bronnen") ?: JSONArray()
        for (i in 0 until bJ.length()) {
            val j = bJ.getJSONObject(i)
            bronnen.add(BronStatus(j.getString("url"), j.getBoolean("ok"), j.getString("melding")))
        }
        val niet = ArrayList<String>()
        val nJ = o.optJSONArray("niet") ?: JSONArray()
        for (i in 0 until nJ.length()) niet.add(nJ.getString(i))
        return EpgResultaat(programmas, bronnen, niet, o.getLong("opgehaald"))
    }
}

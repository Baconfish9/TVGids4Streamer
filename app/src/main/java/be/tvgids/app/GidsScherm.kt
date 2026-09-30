package be.tvgids.app

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Vormgeving: een donkere avond-look, met "studiolicht"-amber voor focus
// ---------------------------------------------------------------------------

object Kleuren {
    val achtergrond = Color(0xFF0F1622)
    val paneel = Color(0xFF172131)
    val blok = Color(0xFF1F2B3E)
    val blokVoorbij = Color(0xFF162030)
    val live = Color(0xFF2C3D5A)
    val focus = Color(0xFFFFC857)
    val opFocus = Color(0xFF1A1405)
    val knop = Color(0xFF223047)
    val actief = Color(0xFF3A5580)
    val tekst = Color(0xFFF0ECE2)
    val tekstZacht = Color(0xFF93A0B4)
    val nuLijn = Color(0xFFFF6B5A)
    val fout = Color(0xFFFF8A80)
    val ok = Color(0xFF7FD1A0)
}

private val DP_PER_MIN = 5.dp
private val RIJ_HOOGTE = 58.dp
private val ZENDER_BREEDTE = 170.dp
private val TIJDBALK_HOOGTE = 30.dp
private const val RASTER_UREN = 30

private val NL_BE: Locale = Locale.forLanguageTag("nl-BE")
private val FMT_UUR = DateTimeFormatter.ofPattern("HH:mm")
private val FMT_DAG = DateTimeFormatter.ofPattern("EEE d MMM", NL_BE)
private val FMT_DATUM = DateTimeFormatter.ofPattern("EEEE d MMMM", NL_BE)

private fun zone(): ZoneId = ZoneId.systemDefault()
fun uurMin(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone()).format(FMT_UUR)
private fun datumLang(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(zone()).format(FMT_DATUM).replaceFirstChar { it.uppercase() }

private fun tijdbalkLabel(ms: Long): String {
    val z = Instant.ofEpochMilli(ms).atZone(zone())
    return if (z.hour == 0 && z.minute == 0) z.format(FMT_DAG) else z.format(FMT_UUR)
}

private fun halfUurOmlaag(ms: Long): Long = ms - (ms % (30 * MIN))

private fun zenderVan(key: String): ZenderDef? = Config.ZENDERS.firstOrNull { it.key == key }

// ---------------------------------------------------------------------------
// Hoofdscherm
// ---------------------------------------------------------------------------

@Composable
fun GidsScherm(
    state: GidsState,
    onVernieuw: () -> Unit,
    onFilter: (Land?) -> Unit,
    onStatus: () -> Unit,
) {
    val nu by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(20_000)
            value = System.currentTimeMillis()
        }
    }
    val res = state.resultaat
    val rasterStart = remember(res?.opgehaald) { halfUurOmlaag(System.currentTimeMillis() - 2 * UUR) }
    val rasterEind = rasterStart + RASTER_UREN * UUR
    val totaalMin = (RASTER_UREN * 60).toFloat()
    val zenders = remember(state.filter) {
        Config.ZENDERS.filter { state.filter == null || it.land == state.filter }
    }

    var gefocust by remember { mutableStateOf<Programma?>(null) }
    var detail by remember { mutableStateOf<Programma?>(null) }
    var scrollMin by remember(rasterStart) {
        mutableFloatStateOf(max(0f, (System.currentTimeMillis() - rasterStart) / MIN.toFloat() - 30f))
    }
    val scrollGeanimeerd by animateFloatAsState(scrollMin, animationSpec = tween(220), label = "scroll")
    val pxPerMin = with(LocalDensity.current) { DP_PER_MIN.toPx() }

    val focusNu = remember { FocusRequester() }
    val focusKnop = remember { FocusRequester() }
    val eersteLiveKey = remember(res, zenders) {
        val t = System.currentTimeMillis()
        zenders.firstOrNull { z ->
            res?.programmas?.get(z.key)?.any { it.start <= t && it.stop > t } == true
        }?.key
    }
    LaunchedEffect(eersteLiveKey, state.filter) {
        delay(150)
        if (eersteLiveKey != null) {
            runCatching { focusNu.requestFocus() }
        } else {
            runCatching { focusKnop.requestFocus() }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 40.dp, end = 40.dp, top = 22.dp, bottom = 16.dp)
    ) {
        Kop(nu, state, focusKnop, onVernieuw, onFilter, onStatus)
        Spacer(Modifier.height(12.dp))
        InfoPaneel(gefocust, nu, state)
        Spacer(Modifier.height(10.dp))

        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val zichtbaarMin = (maxWidth - ZENDER_BREEDTE) / DP_PER_MIN
            val maxScroll = max(0f, totaalMin - zichtbaarMin)

            // Houdt het gefocuste programma in beeld door de tijdlijn te verschuiven.
            fun volg(p: Programma) {
                val s = max(0f, (p.start - rasterStart) / MIN.toFloat())
                val e = min(totaalMin, (p.stop - rasterStart) / MIN.toFloat())
                val zichtbaar = e > scrollMin + 15f && s < scrollMin + zichtbaarMin - 15f
                if (!zichtbaar) {
                    scrollMin = (s - 15f).coerceIn(0f, maxScroll)
                } else if (s < scrollMin && e > scrollMin + zichtbaarMin) {
                    // lang programma dat het hele scherm vult: laat staan
                } else if (s > scrollMin + zichtbaarMin - 45f) {
                    scrollMin = (s - 30f).coerceIn(0f, maxScroll)
                }
            }

            Column(Modifier.fillMaxSize()) {
                // Tijdbalk
                Row(Modifier.fillMaxWidth().height(TIJDBALK_HOOGTE)) {
                    Spacer(Modifier.width(ZENDER_BREEDTE))
                    Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
                        Box(
                            Modifier
                                .wrapContentWidth(Alignment.Start, unbounded = true)
                                .width(DP_PER_MIN * totaalMin)
                                .fillMaxHeight()
                                .offset { IntOffset(-(scrollGeanimeerd * pxPerMin).roundToInt(), 0) }
                        ) {
                            val stappen = RASTER_UREN * 2
                            for (i in 0 until stappen) {
                                val t = rasterStart + i * 30 * MIN
                                Row(
                                    Modifier.offset(x = DP_PER_MIN * (i * 30)).fillMaxHeight(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.width(1.dp).height(12.dp).background(Kleuren.tekstZacht.copy(alpha = 0.5f)))
                                    Text(
                                        tijdbalkLabel(t),
                                        modifier = Modifier.padding(start = 6.dp),
                                        color = Kleuren.tekstZacht,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                // Zenders met programma's, plus de rode "nu"-lijn eroverheen
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(zenders, key = { it.key }) { z ->
                            ZenderRij(
                                zender = z,
                                programmas = res?.programmas?.get(z.key).orEmpty(),
                                rasterStart = rasterStart,
                                rasterEind = rasterEind,
                                nu = nu,
                                pxPerMin = pxPerMin,
                                scroll = { scrollGeanimeerd },
                                focusRequester = if (z.key == eersteLiveKey) focusNu else null,
                                laden = state.laden && res == null,
                                onFocus = { p ->
                                    gefocust = p
                                    volg(p)
                                },
                                onKlik = { p -> detail = p },
                            )
                        }
                    }
                    val nuMin = (nu - rasterStart) / MIN.toFloat()
                    Box(
                        Modifier
                            .matchParentSize()
                            .padding(start = ZENDER_BREEDTE)
                            .clipToBounds()
                    ) {
                        Box(
                            Modifier
                                .offset { IntOffset(((nuMin - scrollGeanimeerd) * pxPerMin).roundToInt(), 0) }
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Kleuren.nuLijn)
                        )
                    }
                }
            }
        }
    }

    val d = detail
    if (d != null) {
        Dialog(onDismissRequest = { detail = null }) {
            DetailKaart(d, nu, onSluit = { detail = null })
        }
    }
}

// ---------------------------------------------------------------------------
// Onderdelen
// ---------------------------------------------------------------------------

@Composable
private fun Kop(
    nu: Long,
    state: GidsState,
    focusKnop: FocusRequester,
    onVernieuw: () -> Unit,
    onFilter: (Land?) -> Unit,
    onStatus: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(uurMin(nu), color = Kleuren.tekst, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(16.dp))
        Text(datumLang(nu), color = Kleuren.tekstZacht, fontSize = 17.sp)
        Spacer(Modifier.weight(1f))
        TvKnop("Alle zenders", actief = state.filter == null) { onFilter(null) }
        TvKnop("Vlaams", actief = state.filter == Land.BE) { onFilter(Land.BE) }
        TvKnop("Nederlands", actief = state.filter == Land.NL) { onFilter(Land.NL) }
        Spacer(Modifier.width(20.dp))
        TvKnop(
            if (state.laden) "Bezig met laden" else "Vernieuwen",
            modifier = Modifier.focusRequester(focusKnop),
        ) { onVernieuw() }
        TvKnop("Status") { onStatus() }
    }
}

@Composable
private fun InfoPaneel(p: Programma?, nu: Long, state: GidsState) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Kleuren.paneel)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        if (p == null) {
            val boodschap = when {
                state.fout != null -> state.fout ?: ""
                state.laden && state.resultaat == null -> "De gids wordt voor het eerst opgehaald. Dat kan even duren."
                else -> "Gebruik de pijltjestoetsen om door de gids te bladeren. Druk op OK voor meer info."
            }
            Text(
                boodschap,
                color = if (state.fout != null) Kleuren.fout else Kleuren.tekstZacht,
                fontSize = 17.sp,
            )
        } else {
            val zender = zenderVan(p.zenderKey)
            val live = nu >= p.start && nu < p.stop
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(zender?.naam ?: "", color = Color(zender?.kleur ?: 0xFFFFFFFF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp))
                Text("${uurMin(p.start)} tot ${uurMin(p.stop)}", color = Kleuren.tekstZacht, fontSize = 14.sp)
                Spacer(Modifier.width(12.dp))
                Text("${(p.stop - p.start) / MIN} min", color = Kleuren.tekstZacht, fontSize = 14.sp)
                if (live) {
                    Spacer(Modifier.width(12.dp))
                    Text("Nu op tv", color = Kleuren.nuLijn, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                if (p.categorie != null) {
                    Spacer(Modifier.width(12.dp))
                    Text(p.categorie, color = Kleuren.tekstZacht, fontSize = 14.sp)
                }
            }
            Text(
                p.titel,
                color = Kleuren.tekst,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val extra = listOfNotNull(p.subtitel, p.beschrijving).joinToString(". ")
            if (extra.isNotEmpty()) {
                Text(
                    extra,
                    color = Kleuren.tekstZacht,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ZenderRij(
    zender: ZenderDef,
    programmas: List<Programma>,
    rasterStart: Long,
    rasterEind: Long,
    nu: Long,
    pxPerMin: Float,
    scroll: () -> Float,
    focusRequester: FocusRequester?,
    laden: Boolean,
    onFocus: (Programma) -> Unit,
    onKlik: (Programma) -> Unit,
) {
    val zichtbaar = remember(programmas, rasterStart) {
        programmas.filter { it.stop > rasterStart && it.start < rasterEind }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(RIJ_HOOGTE)
            .padding(vertical = 3.dp)
    ) {
        ZenderLabel(zender)
        Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
            if (zichtbaar.isEmpty()) {
                Text(
                    if (laden) "Laden" else "Geen programmagegevens voor deze zender",
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
                    color = Kleuren.tekstZacht.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                )
            } else {
                Box(
                    Modifier
                        .wrapContentWidth(Alignment.Start, unbounded = true)
                        .width(DP_PER_MIN * (RASTER_UREN * 60))
                        .fillMaxHeight()
                        .offset { IntOffset(-(scroll() * pxPerMin).roundToInt(), 0) }
                ) {
                    for (p in zichtbaar) {
                        key(p.start) {
                            val s = max(p.start, rasterStart)
                            val e = min(p.stop, rasterEind)
                            val isLive = nu >= p.start && nu < p.stop
                            ProgrammaBlok(
                                p = p,
                                nu = nu,
                                focusRequester = if (isLive) focusRequester else null,
                                onFocus = { onFocus(p) },
                                onKlik = { onKlik(p) },
                                modifier = Modifier
                                    .offset(x = DP_PER_MIN * ((s - rasterStart) / MIN.toFloat()))
                                    .width(DP_PER_MIN * ((e - s) / MIN.toFloat()))
                                    .fillMaxHeight()
                                    .padding(horizontal = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZenderLabel(z: ZenderDef) {
    Row(
        Modifier
            .width(ZENDER_BREEDTE)
            .fillMaxHeight()
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Kleuren.paneel)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(5.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(z.kleur))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            z.naam,
            color = Kleuren.tekst,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun ProgrammaBlok(
    p: Programma,
    nu: Long,
    focusRequester: FocusRequester?,
    onFocus: () -> Unit,
    onKlik: () -> Unit,
    modifier: Modifier,
) {
    var focus by remember { mutableStateOf(false) }
    val live = nu >= p.start && nu < p.stop
    val voorbij = p.stop <= nu
    val achtergrond = when {
        focus -> Kleuren.focus
        live -> Kleuren.live
        voorbij -> Kleuren.blokVoorbij
        else -> Kleuren.blok
    }
    val tekstKleur = when {
        focus -> Kleuren.opFocus
        voorbij -> Kleuren.tekstZacht
        else -> Kleuren.tekst
    }
    val fr = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
    Box(
        modifier
            .then(fr)
            .onFocusChanged {
                focus = it.isFocused
                if (it.isFocused) onFocus()
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onKlik,
            )
            .clip(RoundedCornerShape(7.dp))
            .background(achtergrond)
    ) {
        if (live && !focus) {
            // Voortgangsbalk onderaan het blok dat nu uitgezonden wordt
            val fractie = ((nu - p.start).toFloat() / (p.stop - p.start)).coerceIn(0f, 1f)
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(fractie)
                    .height(3.dp)
                    .background(Kleuren.nuLijn)
            )
        }
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                p.titel,
                color = tekstKleur,
                fontSize = 15.sp,
                fontWeight = if (focus || live) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                uurMin(p.start),
                color = if (focus) Kleuren.opFocus.copy(alpha = 0.75f) else Kleuren.tekstZacht,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun TvKnop(
    tekst: String,
    modifier: Modifier = Modifier,
    actief: Boolean = false,
    onClick: () -> Unit,
) {
    var focus by remember { mutableStateOf(false) }
    val achtergrond = when {
        focus -> Kleuren.focus
        actief -> Kleuren.actief
        else -> Kleuren.knop
    }
    Box(
        modifier
            .padding(horizontal = 4.dp)
            .onFocusChanged { focus = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .clip(RoundedCornerShape(20.dp))
            .background(achtergrond)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            tekst,
            color = if (focus) Kleuren.opFocus else Kleuren.tekst,
            fontSize = 15.sp,
            fontWeight = if (focus) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ---------------------------------------------------------------------------
// Detailvenster
// ---------------------------------------------------------------------------

private fun openApp(context: Context, pakket: String): Boolean {
    val pm = context.packageManager
    val intent = pm.getLeanbackLaunchIntentForPackage(pakket)
        ?: pm.getLaunchIntentForPackage(pakket)
        ?: return false
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}

@Composable
private fun DetailKaart(p: Programma, nu: Long, onSluit: () -> Unit) {
    val context = LocalContext.current
    val zender = zenderVan(p.zenderKey)
    val eersteKnop = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { eersteKnop.requestFocus() }
    }
    val live = nu >= p.start && nu < p.stop

    Column(
        Modifier
            .width(640.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Kleuren.paneel)
            .border(1.dp, Kleuren.knop, RoundedCornerShape(16.dp))
            .padding(28.dp)
    ) {
        Text(zender?.naam ?: "", color = Color(zender?.kleur ?: 0xFFFFFFFF), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(p.titel, color = Kleuren.tekst, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        if (p.subtitel != null) {
            Text(p.subtitel, color = Kleuren.tekst, fontSize = 17.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${datumLang(p.start)}, ${uurMin(p.start)} tot ${uurMin(p.stop)} (${(p.stop - p.start) / MIN} min)" +
                if (live) ". Nu op tv." else "",
            color = Kleuren.tekstZacht,
            fontSize = 15.sp,
        )
        if (p.beschrijving != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                p.beschrijving,
                color = Kleuren.tekst,
                fontSize = 16.sp,
                lineHeight = 23.sp,
                maxLines = 9,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(22.dp))
        Row {
            val pakket = zender?.appPakket
            val appNaam = zender?.appNaam ?: "de app"
            if (pakket != null) {
                TvKnop("Kijken in $appNaam", modifier = Modifier.focusRequester(eersteKnop)) {
                    if (openApp(context, pakket)) {
                        onSluit()
                    } else {
                        Toast.makeText(context, "$appNaam is niet geïnstalleerd op dit toestel", Toast.LENGTH_LONG).show()
                    }
                }
                TvKnop("Sluiten") { onSluit() }
            } else {
                TvKnop("Sluiten", modifier = Modifier.focusRequester(eersteKnop)) { onSluit() }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Statusscherm: welke bronnen werkten en welke zenders werden gevonden
// ---------------------------------------------------------------------------

@Composable
fun StatusScherm(state: GidsState, onTerug: () -> Unit) {
    BackHandler(onBack = onTerug)
    val res = state.resultaat
    val terug = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { terug.requestFocus() }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Status van de gids", color = Kleuren.tekst, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TvKnop("Terug naar de gids", modifier = Modifier.focusRequester(terug)) { onTerug() }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (res == null) "Nog niet opgehaald." else "Laatst opgehaald: ${datumLang(res.opgehaald)} om ${uurMin(res.opgehaald)}",
            color = Kleuren.tekstZacht,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(14.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            item { Sectie("Bronnen") }
            items(res?.bronnen.orEmpty()) { b ->
                Regel(b.url, b.melding, if (b.ok) Kleuren.ok else Kleuren.fout)
            }
            item { Sectie("Zenders in de gids") }
            items(Config.ZENDERS) { z ->
                val n = res?.programmas?.get(z.key)?.size ?: 0
                Regel(z.naam, if (n == 0) "Geen gegevens gevonden" else "$n programma's", if (n == 0) Kleuren.fout else Kleuren.ok)
            }
            item {
                Sectie("Andere zenders in de bronnen (${res?.nietGekoppeld?.size ?: 0})")
                Text(
                    "Staat een zender hierboven zonder gegevens? Zoek hier onder welke naam hij in de bron staat en voeg die naam toe aan de aliassen in Config.kt.",
                    color = Kleuren.tekstZacht,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(res?.nietGekoppeld.orEmpty()) { naam ->
                Regel(naam, "", Kleuren.tekstZacht)
            }
        }
    }
}

@Composable
private fun Sectie(titel: String) {
    Text(
        titel,
        color = Kleuren.focus,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun Regel(titel: String, detail: String, detailKleur: Color) {
    var focus by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focus = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(6.dp))
            .background(if (focus) Kleuren.knop else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            titel,
            color = Kleuren.tekst,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (detail.isNotEmpty()) {
            Spacer(Modifier.width(16.dp))
            Text(detail, color = detailKleur, fontSize = 15.sp)
        }
    }
}

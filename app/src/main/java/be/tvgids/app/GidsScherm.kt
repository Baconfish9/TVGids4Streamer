package be.tvgids.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
    val opvulling = Color(0xFF131B28)
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
// Smallere zenderkolom voor een telefoon in liggende stand.
private val ZENDER_BREEDTE_COMPACT = 110.dp
/** Schermen lager dan dit (telefoons) krijgen de compacte layout zonder infopaneel. */
private val COMPACT_ONDER = 480.dp
private val TIJDBALK_HOOGTE = 30.dp
/** Hoe ver het raster vooruit loopt, gerekend vanaf twee uur geleden. */
private const val RASTER_UREN = 30
/** Minimale breedte die de meeschuivende titel aan het einde van een blok houdt. */
private val TEKST_MIN_BREEDTE = 60.dp
/** Wie langer dan dit in een andere app zat, komt terug bij "nu" in plaats van op de oude plek. */
private const val TERUGZETTEN_NA = 5 * MIN

private val NL_BE: Locale = Locale.forLanguageTag("nl-BE")
private val FMT_UUR = DateTimeFormatter.ofPattern("HH:mm")
private val FMT_DAG = DateTimeFormatter.ofPattern("EEE d MMM", NL_BE)
private val FMT_DATUM = DateTimeFormatter.ofPattern("EEEE d MMMM", NL_BE)

private fun zone(): ZoneId = BRUSSEL
fun uurMin(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone()).format(FMT_UUR)
private fun datumLang(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(zone()).format(FMT_DATUM).replaceFirstChar { it.uppercase() }

private fun isMiddernacht(ms: Long): Boolean {
    val z = Instant.ofEpochMilli(ms).atZone(zone())
    return z.hour == 0 && z.minute == 0
}

private fun tijdbalkLabel(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(zone()).format(if (isMiddernacht(ms)) FMT_DAG else FMT_UUR)

/** "Gisteren", "Vandaag", "Morgen" of anders bv. "Za 10 okt", gezien vanaf [nu]. */
internal fun dagNaam(ms: Long, nu: Long): String {
    val dag = Instant.ofEpochMilli(ms).atZone(zone()).toLocalDate()
    val vandaag = Instant.ofEpochMilli(nu).atZone(zone()).toLocalDate()
    return when (ChronoUnit.DAYS.between(vandaag, dag)) {
        -1L -> "Gisteren"
        0L -> "Vandaag"
        1L -> "Morgen"
        else -> dag.format(FMT_DAG).replaceFirstChar { it.uppercase() }
    }
}

private fun halfUurOmlaag(ms: Long): Long = ms - (ms % (30 * MIN))

private fun zenderVan(key: String): ZenderDef? = Config.ZENDERS.firstOrNull { it.key == key }

/** Gaten korter dan dit blijven gewoon leeg. */
private const val MIN_GAT = 5 * MIN

/**
 * Vult de gaten in een rij (ook voor het eerste en na het laatste programma) met
 * grijze blokken, zodat omhoog en omlaag op hetzelfde tijdstip blijven in plaats
 * van naar het eerste programma van die zender te springen.
 */
internal fun metOpvulling(rij: List<Programma>, zenderKey: String, van: Long, tot: Long): List<Programma> {
    if (rij.isEmpty()) return rij
    val uit = ArrayList<Programma>(rij.size + 4)
    fun gat(s: Long, e: Long) {
        if (e - s >= MIN_GAT) uit.add(Programma(zenderKey, s, e, "Geen uitzending", null, null, null, opvulling = true))
    }
    var vorige = van
    for (p in rij) {
        gat(vorige, p.start)
        uit.add(p)
        vorige = max(vorige, p.stop)
    }
    gat(vorige, tot)
    return uit
}

// ---------------------------------------------------------------------------
// Hoofdscherm
// ---------------------------------------------------------------------------

@Composable
fun GidsScherm(
    state: GidsState,
    onVernieuw: () -> Unit,
    onStatus: () -> Unit,
    zenderActies: ZenderActies,
) {
    // Verandert als blijkt dat de klok van het toestel niet klopt; dan schuift alles mee.
    val afwijking by Klok.afwijking.collectAsState()
    val nu by produceState(Klok.nu(), afwijking) {
        value = Klok.nu()
        while (true) {
            delay(20_000)
            value = Klok.nu()
        }
    }
    val res = state.resultaat
    // Het raster begint bij het oudste bewaarde programma (hoogstens een week
    // terug), zodat je ook voorbije programma's kan opzoeken om terug te kijken.
    val rasterEind = remember(res?.opgehaald, afwijking) { halfUurOmlaag(Klok.nu() - 2 * UUR) + RASTER_UREN * UUR }
    val rasterStart = remember(res?.opgehaald, afwijking) {
        val t = Klok.nu()
        val vroegste = res?.programmas?.values?.mapNotNull { it.firstOrNull()?.start }?.minOrNull() ?: t
        halfUurOmlaag(max(vroegste, t - GESCHIEDENIS).coerceAtMost(t - 2 * UUR))
    }
    val halfUren = ((rasterEind - rasterStart) / (30 * MIN)).toInt()
    val totaalMin = (halfUren * 30).toFloat()
    // De zichtbare zenders, in de volgorde die je zelf koos (standaard eerst de Vlaamse).
    val zenders = remember(state.zenders) { state.zenders.zichtbaar.mapNotNull { zenderVan(it) } }
    // Zender waarvan het menu open staat (OK lang ingedrukt), en de zender die je verplaatst.
    var menuZender by remember { mutableStateOf<String?>(null) }
    var verplaatst by remember { mutableStateOf<String?>(null) }

    var gefocust by remember { mutableStateOf<Programma?>(null) }
    var detail by remember { mutableStateOf<Programma?>(null) }
    // De horizontale positie telt in minuten vanaf een vast vertrekpunt, zodat ze
    // niet verspringt wanneer het raster na een verversing opschuift.
    val oorsprong = rememberSaveable { rasterStart }
    var beeldMin by rememberSaveable {
        mutableFloatStateOf(max(0f, (Klok.nu() - oorsprong) / MIN.toFloat() - 30f))
    }
    // Tijdens het vegen volgt de tijdlijn de vinger meteen, zonder na te ijlen.
    var vegen by remember { mutableStateOf(false) }
    val beeldGeanimeerd by animateFloatAsState(
        beeldMin,
        animationSpec = if (vegen) snap() else tween(220),
        label = "scroll",
    )
    val rasterMin = (rasterStart - oorsprong) / MIN.toFloat()
    // Positie binnen het huidige raster, in minuten na rasterStart.
    fun scrollMin(): Float = max(0f, beeldMin - rasterMin)
    fun scrollGeanimeerd(): Float = max(0f, beeldGeanimeerd - rasterMin)
    val pxPerMin = with(LocalDensity.current) { DP_PER_MIN.toPx() }

    val context = LocalContext.current
    val view = LocalView.current
    // Telefoon of tablet (geen Android TV): bedienen met de vinger.
    val touch = remember { !context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }

    val focusKnop = remember { FocusRequester() }
    val lijst = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Programma's per zender die binnen het raster vallen (zelfde volgorde als de rijen).
    val perRij = remember(res, rasterStart, zenders) {
        zenders.map { z ->
            val rij = res?.programmas?.get(z.key).orEmpty().filter { it.stop > rasterStart && it.start < rasterEind }
            metOpvulling(rij, z.key, rasterStart, rasterEind)
        }
    }

    // Het programma dat de focus moet krijgen. Elke verhoging van focusVerzoek
    // vraagt het bijhorende blok om de focus (opnieuw) te nemen.
    var doel by remember { mutableStateOf<Programma?>(null) }
    var focusVerzoek by remember { mutableIntStateOf(0) }
    // Zender en begintijd van het laatst gekozen programma. Die overleven een
    // verversing en een bezoek aan het statusscherm, het Programma zelf niet.
    var doelZender by rememberSaveable { mutableStateOf<String?>(null) }
    var doelStart by rememberSaveable { mutableLongStateOf(0L) }
    // Tijdstip waarrond omhoog/omlaag een programma gezocht wordt, zodat je
    // bij verticaal bladeren niet stilaan naar links of rechts afdrijft.
    var ankerTijd by rememberSaveable { mutableLongStateOf(Klok.nu()) }
    var ankerVast by remember { mutableStateOf(false) }
    // Of de focus in het raster staat (en dus niet op een knop bovenaan).
    var rasterFocus by remember { mutableStateOf(false) }
    // Of de focus sinds het openen van dit scherm al eens geplaatst werd.
    var geplaatst by remember { mutableStateOf(false) }
    // Na het verbergen van een zender moet de focus naar de buurzender, ook vanuit het menu.
    var herplaats by remember { mutableStateOf(false) }
    // Laatste zender die vanuit het menu weer zichtbaar werd: daar gaat de focus heen.
    var teruggezet by remember { mutableStateOf<String?>(null) }
    // OK werd lang ingedrukt: de klik bij het loslaten negeren.
    var langIngedrukt by remember { mutableStateOf(false) }
    // Wanneer de app naar de achtergrond ging, en een teller die de plaatsing
    // opnieuw laat lopen als je na een tijd terugkeert.
    var wegSinds by rememberSaveable { mutableLongStateOf(0L) }
    var terugkeer by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        wegSinds = Klok.nu()
    }
    // Lang weg geweest (bv. een programma bekeken): terug naar "nu" op dezelfde zender.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        val t = Klok.nu()
        if (wegSinds > 0 && t - wegSinds >= TERUGZETTEN_NA) {
            detail = null
            ankerTijd = t
            doelStart = 0L
            beeldMin = max(0f, (t - oorsprong) / MIN.toFloat() - 30f)
            geplaatst = false
            terugkeer++
        }
        wegSinds = 0L
    }
    // Pas gemerkt dat de klok van het toestel verkeerd staat: naar het echte "nu".
    var vorigeAfwijking by remember { mutableLongStateOf(afwijking) }
    LaunchedEffect(afwijking) {
        if (afwijking == vorigeAfwijking) return@LaunchedEffect
        vorigeAfwijking = afwijking
        val t = Klok.nu()
        ankerTijd = t
        doelStart = 0L
        beeldMin = max(0f, (t - oorsprong) / MIN.toFloat() - 30f)
        geplaatst = false
        terugkeer++
    }

    fun geefFocus(p: Programma) {
        doel = p
        focusVerzoek++
    }

    // Onthoudt het gekozen programma, zodat het een verversing overleeft.
    fun kies(p: Programma) {
        gefocust = p
        doel = p
        doelZender = p.zenderKey
        doelStart = p.start
    }

    // Zorgt dat rij `index` volledig in beeld staat (en dus opgebouwd is).
    fun toonRij(index: Int) {
        val info = lijst.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index }
        scope.launch {
            when {
                item == null -> {
                    val eerste = info.visibleItemsInfo.firstOrNull()?.index ?: 0
                    val aantal = max(1, info.visibleItemsInfo.size - 1)
                    lijst.scrollToItem(if (index < eerste) index else max(0, index - aantal + 1))
                }
                item.offset < 0 -> lijst.animateScrollToItem(index)
                item.offset + item.size > info.viewportEndOffset ->
                    lijst.animateScrollBy((item.offset + item.size - info.viewportEndOffset).toFloat())
            }
        }
    }

    // Bij nieuwe gegevens (eerste keer, na een verversing of terug van het
    // statusscherm) het vorige programma terugzoeken; anders het eerste live-programma.
    LaunchedEffect(perRij, terugkeer) {
        delay(150)
        val t = Klok.nu()
        val rijIndex = zenders.indexOfFirst { it.key == doelZender }
        val terug = perRij.getOrNull(rijIndex)?.let { rij -> rij.firstOrNull { it.start == doelStart } ?: rondAnker(rij, ankerTijd) }
        val nieuw = terug ?: perRij.firstNotNullOfOrNull { rij -> rij.firstOrNull { !it.opvulling && it.start <= t && it.stop > t } }
        val moetFocus = herplaats
        herplaats = false
        when {
            nieuw == null -> if (!geplaatst) runCatching { focusKnop.requestFocus() }
            // Een zender werd verborgen: de focus naar de buurzender, ook al stond het menu open.
            moetFocus -> {
                toonRij(zenders.indexOfFirst { it.key == nieuw.zenderKey })
                geefFocus(nieuw)
            }
            // Focus staat op een knop of in een venster: niet afpakken,
            // maar wel klaarzetten voor wanneer je terug naar het raster gaat.
            geplaatst && (!rasterFocus || detail != null || menuZender != null) -> {
                doel = nieuw
                gefocust = nieuw
            }
            geplaatst && nieuw == gefocust -> {}
            else -> {
                if (terug == null) {
                    ankerTijd = t
                    ankerVast = true
                }
                toonRij(zenders.indexOfFirst { it.key == nieuw.zenderKey })
                geefFocus(nieuw)
            }
        }
        geplaatst = true
    }

    // Afhandeling van de pijltjestoetsen binnen het raster. Geeft false terug als
    // de standaard-focusnavigatie het mag overnemen (omhoog vanuit de bovenste rij).
    fun navigeer(key: Key): Boolean {
        val p = gefocust ?: return false
        val rij = zenders.indexOfFirst { it.key == p.zenderKey }
        if (rij < 0) return false
        when (key) {
            Key.DirectionLeft, Key.DirectionRight -> {
                val nieuw = naastLiggend(perRij[rij], p, if (key == Key.DirectionLeft) -1 else 1) ?: return true
                geefFocus(nieuw)
            }
            Key.DirectionUp, Key.DirectionDown -> {
                val r = volgendeRij(perRij, rij, if (key == Key.DirectionUp) -1 else 1)
                    ?: return key == Key.DirectionDown
                val nieuw = rondAnker(perRij[r], ankerTijd) ?: return true
                ankerVast = true
                toonRij(r)
                geefFocus(nieuw)
            }
            else -> return false
        }
        return true
    }

    fun openMenu(key: String) {
        menuZender = key
        if (state.toonTip) zenderActies.tipGezien()
    }

    // Schuift de opgepakte zender één rij op; de rij en de focus blijven in beeld.
    fun verplaatsRij(key: String, stap: Int) {
        val i = zenders.indexOfFirst { it.key == key }
        if (i + stap !in zenders.indices) return
        // Een LazyColumn houdt normaal de bovenste rij in beeld, ook als die verschuift;
        // dan lijkt de rest te verspringen. Hier blijft de lijst staan en zakt de rij.
        lijst.requestScrollToItem(lijst.firstVisibleItemIndex, lijst.firstVisibleItemScrollOffset)
        zenderActies.verplaatsZender(key, stap)
        val p = gefocust
        scope.launch {
            delay(50)
            toonRij(i + stap)
            if (p != null) geefFocus(p)
        }
    }

    fun verberg(key: String) {
        val i = zenders.indexOfFirst { it.key == key }
        val buur = zenders.getOrNull(i + 1) ?: zenders.getOrNull(i - 1)
        if (buur != null) {
            doelZender = buur.key
            doelStart = 0L
            herplaats = true
        }
        zenderActies.verbergZender(key)
    }

    // Alle toetsen in het raster. OK zelf afhandelen, zodat lang indrukken het menu
    // opent in plaats van het detailvenster; tijdens het verplaatsen gaan omhoog en
    // omlaag naar de zender in plaats van naar de focus.
    fun verwerkToets(e: KeyEvent): Boolean {
        val opgepakt = verplaatst
        if (opgepakt != null) {
            when {
                e.key == Key.DirectionUp || e.key == Key.DirectionDown ->
                    if (e.type == KeyEventType.KeyDown) verplaatsRij(opgepakt, if (e.key == Key.DirectionUp) -1 else 1)
                isOk(e.key) -> if (e.type == KeyEventType.KeyUp) verplaatst = null
                // Terug: via de BackHandler hieronder.
                e.key == Key.Back -> return false
            }
            return true
        }
        if (isOk(e.key)) {
            val p = gefocust ?: return false
            if (e.type == KeyEventType.KeyDown) {
                val herhaling = e.nativeKeyEvent.repeatCount
                if (herhaling == 0) langIngedrukt = false
                if (herhaling >= 1 && !langIngedrukt) {
                    langIngedrukt = true
                    Geluid.klik(view)
                    openMenu(p.zenderKey)
                }
            } else if (e.type == KeyEventType.KeyUp && !langIngedrukt) {
                Geluid.klik(view)
                kies(p)
                if (!p.opvulling) detail = p
            }
            return true
        }
        return e.type == KeyEventType.KeyDown && navigeer(e.key)
    }

    BackHandler(enabled = verplaatst != null) { verplaatst = null }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < COMPACT_ONDER
        val zenderBreedte = if (compact) ZENDER_BREEDTE_COMPACT else ZENDER_BREEDTE
        Column(
            Modifier
                .fillMaxSize()
                .then(
                    if (compact) Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)
                    else Modifier.padding(start = 40.dp, end = 40.dp, top = 22.dp, bottom = 16.dp)
                )
        ) {
            Kop(
                nu, state, focusKnop, onVernieuw, onStatus,
                compact = compact,
                tip = when {
                    !state.toonTip || zenders.isEmpty() -> null
                    touch -> "Tip: hou een programma ingedrukt om die zender te verplaatsen of te verbergen."
                    else -> "Tip: hou OK ingedrukt om een zender te verplaatsen of te verbergen."
                },
                // Na het vegen snel terug naar het huidige uur (op TV volstaan de pijltjes).
                onNu = if (touch) {
                    { beeldMin = max(0f, (Klok.nu() - oorsprong) / MIN.toFloat() - 30f) }
                } else {
                    null
                },
                onNaarRaster = {
                    val d = doel
                    if (d != null && perRij.any { d in it }) {
                        focusVerzoek++
                        true
                    } else {
                        false
                    }
                },
            )
            if (compact) {
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(12.dp))
                InfoPaneel(gefocust, nu, state, touch)
                Spacer(Modifier.height(10.dp))
            }

            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val zichtbaarMin = (maxWidth - zenderBreedte) / DP_PER_MIN
                val maxScroll = max(0f, totaalMin - zichtbaarMin)

                // Horizontaal vegen verschuift de tijdlijn (met naloop); verticaal
                // scrollen doet de LazyColumn zelf.
                val veeg = rememberScrollableState { delta ->
                    val oud = beeldMin.coerceIn(rasterMin, rasterMin + maxScroll)
                    val nieuw = (oud - delta / pxPerMin).coerceIn(rasterMin, rasterMin + maxScroll)
                    beeldMin = nieuw
                    (oud - nieuw) * pxPerMin
                }
                LaunchedEffect(veeg) {
                    snapshotFlow { veeg.isScrollInProgress }.collect { vegen = it }
                }

                // Houdt het gefocuste programma in beeld door de tijdlijn te verschuiven.
                fun volg(p: Programma) {
                    val s = max(0f, (p.start - rasterStart) / MIN.toFloat())
                    val e = min(totaalMin, (p.stop - rasterStart) / MIN.toFloat())
                    val huidig = scrollMin()
                    val zichtbaar = e > huidig + 15f && s < huidig + zichtbaarMin - 15f
                    if (!zichtbaar) {
                        beeldMin = rasterMin + (s - 15f).coerceIn(0f, maxScroll)
                    } else if (s < huidig && e > huidig + zichtbaarMin) {
                        // lang programma dat het hele scherm vult: laat staan
                    } else if (s > huidig + zichtbaarMin - 45f) {
                        beeldMin = rasterMin + (s - 30f).coerceIn(0f, maxScroll)
                    }
                }

                Column(Modifier.fillMaxSize().scrollable(veeg, Orientation.Horizontal)) {
                    // Tijdbalk
                    Row(Modifier.fillMaxWidth().height(TIJDBALK_HOOGTE)) {
                        // De dag aan de linkerrand van het beeld, zodat je bij het
                        // scrollen altijd weet of 20:00 vanavond of morgen is.
                        val dag by remember(rasterStart, nu) {
                            derivedStateOf { dagNaam(rasterStart + (scrollGeanimeerd() * MIN).toLong(), nu) }
                        }
                        Box(Modifier.width(zenderBreedte).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                            Text(
                                dag,
                                modifier = Modifier.padding(start = 4.dp),
                                color = Kleuren.focus,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                        Box(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
                            Box(
                                Modifier
                                    .wrapContentWidth(Alignment.Start, unbounded = true)
                                    .width(DP_PER_MIN * totaalMin)
                                    .fillMaxHeight()
                                    .offset { IntOffset(-(scrollGeanimeerd() * pxPerMin).roundToInt(), 0) }
                            ) {
                                for (i in 0 until halfUren) {
                                    val t = rasterStart + i * 30 * MIN
                                    val middernacht = isMiddernacht(t)
                                    Row(
                                        Modifier.offset(x = DP_PER_MIN * (i * 30)).fillMaxHeight(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            Modifier
                                                .width(if (middernacht) 2.dp else 1.dp)
                                                .height(12.dp)
                                                .background(if (middernacht) Kleuren.focus else Kleuren.tekstZacht.copy(alpha = 0.5f))
                                        )
                                        Text(
                                            tijdbalkLabel(t),
                                            modifier = Modifier.padding(start = 6.dp),
                                            color = if (middernacht) Kleuren.focus else Kleuren.tekstZacht,
                                            fontSize = 13.sp,
                                            fontWeight = if (middernacht) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Zenders met programma's, plus de rode "nu"-lijn eroverheen
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .onFocusChanged { rasterFocus = it.hasFocus }
                            .onPreviewKeyEvent { e -> verwerkToets(e) }
                    ) {
                        // Een week aan blokken per rij is te zwaar om in één keer op te
                        // bouwen: enkel wat rond het beeld ligt, in stappen van twee uur.
                        val vak by remember(rasterStart) { derivedStateOf { (scrollGeanimeerd() / 120f).toInt() } }
                        val vensterVan = rasterStart + (vak - 2) * 2 * UUR
                        val vensterTot = rasterStart + (vak + 5) * 2 * UUR
                        if (zenders.isEmpty() && res != null) {
                            Column(Modifier.padding(start = 12.dp, top = 24.dp)) {
                                Text("Alle zenders zijn verborgen.", color = Kleuren.tekstZacht, fontSize = 17.sp)
                                Spacer(Modifier.height(12.dp))
                                TvKnop("Alle zenders weer tonen") { zenderActies.toonAlleZenders() }
                            }
                        }
                        LazyColumn(Modifier.fillMaxSize(), state = lijst) {
                            itemsIndexed(zenders, key = { _, z -> z.key }) { i, z ->
                                ZenderRij(
                                    modifier = Modifier.animateItem(),
                                    zender = z,
                                    opgepakt = z.key == verplaatst,
                                    onLangKlik = { openMenu(z.key) },
                                    zichtbaar = perRij[i],
                                    zenderBreedte = zenderBreedte,
                                    rasterStart = rasterStart,
                                    rasterEind = rasterEind,
                                    vensterVan = vensterVan,
                                    vensterTot = vensterTot,
                                    nu = nu,
                                    pxPerMin = pxPerMin,
                                    scroll = { scrollGeanimeerd() },
                                    doel = doel,
                                    focusVerzoek = focusVerzoek,
                                    laden = state.laden && res == null,
                                    onFocus = { p ->
                                        kies(p)
                                        volg(p)
                                        if (ankerVast) {
                                            ankerVast = false
                                        } else {
                                            val beeldStart = rasterStart + (scrollMin() * MIN).toLong()
                                            ankerTijd = max(p.start, min(beeldStart, p.stop - 1))
                                        }
                                    },
                                    onKlik = { p ->
                                        kies(p)
                                        if (!p.opvulling) detail = p
                                    },
                                )
                            }
                        }
                        val nuMin = (nu - rasterStart) / MIN.toFloat()
                        Box(
                            Modifier
                                .matchParentSize()
                                .padding(start = zenderBreedte)
                                .clipToBounds()
                        ) {
                            // Gedempte lijn op elke middernacht: hier begint een nieuwe dag.
                            for (i in 0 until halfUren) {
                                val t = rasterStart + i * 30 * MIN
                                if (!isMiddernacht(t)) continue
                                Box(
                                    Modifier
                                        .offset { IntOffset(((i * 30 - scrollGeanimeerd()) * pxPerMin).roundToInt(), 0) }
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .background(Kleuren.focus.copy(alpha = 0.35f))
                                )
                            }
                            Box(
                                Modifier
                                    .offset { IntOffset(((nuMin - scrollGeanimeerd()) * pxPerMin).roundToInt(), 0) }
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(Kleuren.nuLijn)
                            )
                        }
                    }
                }
            }
        }
        val vz = verplaatst?.let { zenderVan(it) }
        if (vz != null) {
            VerplaatsBalk(
                zender = vz,
                touch = touch,
                onStap = { verplaatsRij(vz.key, it) },
                onKlaar = { verplaatst = null },
            )
        }
    }

    val d = detail
    if (d != null) {
        val sluit: () -> Unit = {
            detail = null
            focusVerzoek++
        }
        Dialog(onDismissRequest = sluit) {
            DetailKaart(d, nu, onSluit = sluit)
        }
    }

    val mz = menuZender?.let { zenderVan(it) }
    if (mz != null) {
        val sluit: () -> Unit = {
            menuZender = null
            val terug = teruggezet
            if (terug != null) {
                // Zo zie je meteen waar de zender teruggekomen is.
                teruggezet = null
                doelZender = terug
                doelStart = 0L
                herplaats = true
                terugkeer++
            } else {
                focusVerzoek++
            }
        }
        Dialog(onDismissRequest = sluit) {
            ZenderMenu(
                zender = mz,
                verborgen = state.zenders.volgorde.filter { it in state.zenders.verborgen }.mapNotNull { zenderVan(it) },
                onVerplaats = {
                    verplaatst = mz.key
                    sluit()
                },
                onVerberg = {
                    menuZender = null
                    verberg(mz.key)
                },
                onToon = {
                    teruggezet = it
                    zenderActies.toonZender(it)
                },
                onSluit = sluit,
            )
        }
    }
}

private fun isOk(k: Key) = k == Key.DirectionCenter || k == Key.Enter || k == Key.NumPadEnter

/** Het menu van een zender, na OK lang indrukken (of lang tikken). */
@Composable
private fun ZenderMenu(
    zender: ZenderDef,
    verborgen: List<ZenderDef>,
    onVerplaats: () -> Unit,
    onVerberg: () -> Unit,
    onToon: (String) -> Unit,
    onSluit: () -> Unit,
) {
    var toonVerborgen by remember { mutableStateOf(false) }
    if (verborgen.isEmpty()) toonVerborgen = false
    val eerste = remember { FocusRequester() }
    // Na het tonen van een zender verdwijnt zijn knop: de focus terug bovenaan.
    LaunchedEffect(toonVerborgen, verborgen.size) {
        delay(100)
        runCatching { eerste.requestFocus() }
    }
    // OK kan nog ingedrukt zijn van het lang indrukken: pas een nieuwe druk telt.
    var versGedrukt by remember { mutableStateOf(false) }

    Column(
        Modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .onPreviewKeyEvent { e ->
                Geluid.toets(e)
                if (isOk(e.key) && !versGedrukt) {
                    if (e.type == KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount == 0) {
                        versGedrukt = true
                        false
                    } else {
                        true
                    }
                } else {
                    false
                }
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Kleuren.paneel)
            .border(1.dp, Kleuren.knop, RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        Text(zender.naam, color = Color(zender.kleur), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        val knop = Modifier.fillMaxWidth().padding(vertical = 3.dp)
        if (!toonVerborgen) {
            TvKnop("Verplaatsen", modifier = knop.focusRequester(eerste)) { onVerplaats() }
            TvKnop("Verbergen", modifier = knop) { onVerberg() }
            if (verborgen.isNotEmpty()) {
                TvKnop("Verborgen zenders (${verborgen.size})", modifier = knop) { toonVerborgen = true }
            }
            TvKnop("Sluiten", modifier = knop) { onSluit() }
        } else {
            Text("Kies een zender om weer te tonen", color = Kleuren.tekstZacht, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                verborgen.forEachIndexed { i, z ->
                    TvKnop(
                        "Tonen: ${z.naam}",
                        modifier = if (i == 0) knop.focusRequester(eerste) else knop,
                    ) { onToon(z.key) }
                }
            }
            TvKnop("Terug", modifier = knop) { toonVerborgen = false }
        }
    }
}

/** Onderaan tijdens het verplaatsen: uitleg, en op een aanraakscherm de knoppen. */
@Composable
private fun BoxScope.VerplaatsBalk(zender: ZenderDef, touch: Boolean, onStap: (Int) -> Unit, onKlaar: () -> Unit) {
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Kleuren.focus)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (touch) "${zender.naam} verplaatsen" else "${zender.naam}: omhoog of omlaag om te verplaatsen, OK om neer te zetten",
            color = Kleuren.opFocus,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (touch) {
            Spacer(Modifier.width(12.dp))
            TvKnop("▲") { onStap(-1) }
            TvKnop("▼") { onStap(1) }
            TvKnop("Klaar") { onKlaar() }
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
    onStatus: () -> Unit,
    onNaarRaster: () -> Boolean,
    compact: Boolean,
    tip: String?,
    onNu: (() -> Unit)?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { e ->
                e.type == KeyEventType.KeyDown && e.key == Key.DirectionDown && onNaarRaster()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(uurMin(nu), color = Kleuren.tekst, fontSize = if (compact) 24.sp else 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(if (compact) 12.dp else 16.dp))
        Text(datumLang(nu), color = Kleuren.tekstZacht, fontSize = if (compact) 15.sp else 17.sp)
        // Een waarschuwing gaat voor op de tip.
        val melding = state.waarschuwing ?: tip
        if (melding != null) {
            // Ook zichtbaar als er een programma gefocust is (dan toont het infopaneel geen fouten).
            Text(
                melding,
                modifier = Modifier.weight(1f).padding(horizontal = if (compact) 12.dp else 20.dp),
                color = if (state.waarschuwing != null) Kleuren.fout else Kleuren.tekstZacht,
                fontSize = if (compact) 13.sp else 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (onNu != null) {
            TvKnop("Nu") { onNu() }
        }
        TvKnop(
            if (state.laden) "Bezig met laden" else "Vernieuwen",
            modifier = Modifier.focusRequester(focusKnop),
        ) { onVernieuw() }
        TvKnop("Status") { onStatus() }
    }
}

@Composable
private fun InfoPaneel(p: Programma?, nu: Long, state: GidsState, touch: Boolean) {
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
                touch -> "Veeg om door de gids te bladeren. Tik op een programma voor meer info."
                else -> "Gebruik de pijltjestoetsen om door de gids te bladeren. Druk op OK voor meer info."
            }
            Text(
                boodschap,
                color = if (state.fout != null) Kleuren.fout else Kleuren.tekstZacht,
                fontSize = 17.sp,
            )
        } else {
            val zender = zenderVan(p.zenderKey)
            val live = !p.opvulling && nu >= p.start && nu < p.stop
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(zender?.naam ?: "", color = Color(zender?.kleur ?: 0xFFFFFFFF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp))
                // Enkel bij een andere dag dan vandaag, zodat het paneel rustig blijft.
                val dag = dagNaam(p.start, nu).takeIf { it != "Vandaag" }?.let { "$it " }.orEmpty()
                Text("$dag${uurMin(p.start)} tot ${uurMin(p.stop)}", color = Kleuren.tekstZacht, fontSize = 14.sp)
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
    modifier: Modifier,
    zender: ZenderDef,
    /** Deze zender wordt verplaatst: de hele rij licht op. */
    opgepakt: Boolean,
    onLangKlik: () -> Unit,
    zichtbaar: List<Programma>,
    zenderBreedte: Dp,
    rasterStart: Long,
    rasterEind: Long,
    vensterVan: Long,
    vensterTot: Long,
    nu: Long,
    pxPerMin: Float,
    scroll: () -> Float,
    doel: Programma?,
    focusVerzoek: Int,
    laden: Boolean,
    onFocus: (Programma) -> Unit,
    onKlik: (Programma) -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(RIJ_HOOGTE)
            .padding(vertical = 3.dp)
            .then(if (opgepakt) Modifier.border(2.dp, Kleuren.focus, RoundedCornerShape(8.dp)) else Modifier)
    ) {
        ZenderLabel(zender, zenderBreedte, opgepakt, onLangKlik)
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
                        .width(DP_PER_MIN * ((rasterEind - rasterStart) / MIN.toFloat()))
                        .fillMaxHeight()
                        .offset { IntOffset(-(scroll() * pxPerMin).roundToInt(), 0) }
                ) {
                    for (p in zichtbaar) {
                        // Het doel altijd opbouwen, anders kan het de focus niet krijgen.
                        if (p != doel && (p.stop <= vensterVan || p.start >= vensterTot)) continue
                        key(p.start) {
                            val s = max(p.start, rasterStart)
                            val e = min(p.stop, rasterEind)
                            val startMin = (s - rasterStart) / MIN.toFloat()
                            ProgrammaBlok(
                                p = p,
                                nu = nu,
                                startMin = startMin,
                                pxPerMin = pxPerMin,
                                scroll = scroll,
                                focusVerzoek = if (p == doel) focusVerzoek else -1,
                                onFocus = { onFocus(p) },
                                onKlik = { onKlik(p) },
                                onLangKlik = onLangKlik,
                                modifier = Modifier
                                    .offset(x = DP_PER_MIN * startMin)
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
private fun ZenderLabel(z: ZenderDef, breedte: Dp, opgepakt: Boolean, onLangKlik: () -> Unit) {
    Row(
        Modifier
            .width(breedte)
            .fillMaxHeight()
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (opgepakt) Kleuren.focus else Kleuren.paneel)
            // Aanraakscherm: lang tikken op de naam opent het menu (niet focusbaar op tv).
            .pointerInput(onLangKlik) { detectTapGestures(onLongPress = { onLangKlik() }) }
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
            color = if (opgepakt) Kleuren.opFocus else Kleuren.tekst,
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
    startMin: Float,
    pxPerMin: Float,
    scroll: () -> Float,
    focusVerzoek: Int,
    onFocus: () -> Unit,
    onKlik: () -> Unit,
    onLangKlik: () -> Unit,
    modifier: Modifier,
) {
    var focus by remember { mutableStateOf(false) }
    val view = LocalView.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusVerzoek) {
        if (focusVerzoek >= 0) runCatching { focusRequester.requestFocus() }
    }
    val live = !p.opvulling && nu >= p.start && nu < p.stop
    val voorbij = p.stop <= nu
    val achtergrond = when {
        focus -> Kleuren.focus
        p.opvulling -> Kleuren.opvulling
        live -> Kleuren.live
        voorbij -> Kleuren.blokVoorbij
        else -> Kleuren.blok
    }
    val tekstKleur = when {
        focus -> Kleuren.opFocus
        voorbij || p.opvulling -> Kleuren.tekstZacht
        else -> Kleuren.tekst
    }
    Box(
        modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                focus = it.isFocused
                if (it.isFocused) {
                    Geluid.focus(view)
                    onFocus()
                }
            }
            // Tikken en lang tikken; OK op de afstandsbediening handelt het raster zelf af.
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    Geluid.klik(view)
                    onKlik()
                },
                onLongClick = onLangKlik,
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
        Column(
            Modifier
                // Titel en uur schuiven mee met de linkerrand van het beeld, zodat
                // ze ook zichtbaar blijven bij een programma dat al lang bezig is.
                .layout { measurable, constraints ->
                    val maxVerschuif = max(0, constraints.maxWidth - TEKST_MIN_BREEDTE.roundToPx())
                    val verschuif = ((scroll() - startMin) * pxPerMin).roundToInt().coerceIn(0, maxVerschuif)
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth - verschuif)
                    )
                    layout(constraints.maxWidth, placeable.height) { placeable.place(verschuif, 0) }
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                p.titel,
                color = tekstKleur,
                fontSize = 15.sp,
                fontWeight = if (focus || live) FontWeight.Bold else FontWeight.Medium,
                fontStyle = if (p.opvulling) FontStyle.Italic else FontStyle.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!p.opvulling) {
                Text(
                    uurMin(p.start),
                    color = if (focus) Kleuren.opFocus.copy(alpha = 0.75f) else Kleuren.tekstZacht,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
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
    val view = LocalView.current
    val achtergrond = when {
        focus -> Kleuren.focus
        actief -> Kleuren.actief
        else -> Kleuren.knop
    }
    Box(
        modifier
            .padding(horizontal = 4.dp)
            .onFocusChanged {
                focus = it.isFocused
                if (it.isFocused) Geluid.focus(view)
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    Geluid.klik(view)
                    onClick()
                },
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
    val voorbij = p.stop <= nu

    Column(
        Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            // Een Dialog is een apart venster: de toetsen komen niet langs App.
            .onPreviewKeyEvent { Geluid.toets(it); false }
            .clip(RoundedCornerShape(16.dp))
            .background(Kleuren.paneel)
            .border(1.dp, Kleuren.knop, RoundedCornerShape(16.dp))
            .padding(28.dp)
    ) {
        // Op een telefoon past niet alles in de hoogte: dan scrolt de tekst,
        // en blijven de knoppen eronder altijd in beeld.
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
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
        }
        Spacer(Modifier.height(22.dp))
        Row {
            // Een programma dat nog moet beginnen kan je nergens bekijken; een
            // voorbij programma kan je in de meeste apps wel terugkijken.
            val pakket = zender?.appPakket?.takeIf { live || voorbij }
            val appNaam = zender?.appNaam ?: "de app"
            if (pakket != null) {
                val label = if (live) "Kijken in $appNaam" else "Terugkijken in $appNaam"
                TvKnop(label, modifier = Modifier.focusRequester(eersteKnop)) {
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
    val context = LocalContext.current
    val versie = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
    val terug = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { terug.requestFocus() }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Status van de gids", color = Kleuren.tekst, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            if (versie != null) {
                Spacer(Modifier.width(16.dp))
                Text("versie $versie", color = Kleuren.tekstZacht, fontSize = 15.sp)
            }
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
    val view = LocalView.current
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged {
                focus = it.isFocused
                if (it.isFocused) Geluid.focus(view)
            }
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
            modifier = Modifier.weight(0.45f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (detail.isNotEmpty()) {
            Spacer(Modifier.width(16.dp))
            Text(detail, color = detailKleur, fontSize = 15.sp, modifier = Modifier.weight(0.55f))
        }
    }
}

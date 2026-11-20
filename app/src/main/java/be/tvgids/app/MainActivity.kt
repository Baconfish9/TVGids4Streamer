package be.tvgids.app

import android.app.Application
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GidsState(
    val laden: Boolean = true,
    val fout: String? = null,
    val resultaat: EpgResultaat? = null,
    /** Er is wel een gids, maar ze kon niet ververst worden. */
    val waarschuwing: String? = null,
    val zenders: ZenderInstelling = ZenderInstelling(ZenderInstelling.STANDAARD, emptySet()),
    /** Of de tip over lang indrukken nog getoond moet worden. */
    val toonTip: Boolean = false,
)

class GidsViewModel(app: Application) : AndroidViewModel(app), ZenderActies {

    private val repo = EpgRepository(app)
    private val voorkeuren = ZenderVoorkeuren(app)
    private val _state = MutableStateFlow(GidsState(zenders = voorkeuren.laad(), toonTip = !voorkeuren.tipGezien))
    val state: StateFlow<GidsState> = _state.asStateFlow()
    private var bezig = false
    /** Tijdstip van de cache die nu getoond wordt, om een nieuwere te herkennen. */
    private var cacheTijd = 0L

    private val cacheGeladen = viewModelScope.launch {
        cacheTijd = repo.cacheTijd()
        val cache = repo.laadCache()
        if (cache != null) _state.update { it.copy(resultaat = cache, laden = false) }
    }

    init {
        // Klopt de klok van het toestel niet, dan weten we dat best voor de gids getekend wordt.
        viewModelScope.launch { repo.meetKlok() }
    }

    /**
     * Ververst de gids zodra ze verouderd is. Loopt enkel zolang de app in beeld
     * is; het ophalen zelf gebeurt in viewModelScope, zodat het niet halverwege
     * afgebroken wordt als je de app verlaat.
     */
    suspend fun bewaak() {
        cacheGeladen.join()
        while (true) {
            herlaadCache()
            if (verouderd(_state.value.resultaat)) vernieuw()
            delay(15 * MIN)
        }
    }

    private fun verouderd(r: EpgResultaat?): Boolean =
        r == null || Klok.nu() - r.opgehaald > 6 * UUR

    /** Neemt een gids over die de achtergrondtaak intussen ophaalde. */
    private suspend fun herlaadCache() {
        val t = repo.cacheTijd()
        if (bezig || t <= cacheTijd) return
        val cache = repo.laadCache() ?: return
        cacheTijd = t
        _state.update { it.copy(resultaat = cache, laden = false, fout = null, waarschuwing = null) }
    }

    fun vernieuw() {
        viewModelScope.launch { vernieuwNu() }
    }

    private fun wijzigZenders(nieuw: (ZenderInstelling) -> ZenderInstelling) {
        val z = nieuw(_state.value.zenders)
        voorkeuren.bewaar(z)
        _state.update { it.copy(zenders = z) }
    }

    override fun verplaatsZender(key: String, stap: Int) = wijzigZenders { it.verplaats(key, stap) }

    override fun verbergZender(key: String) = wijzigZenders { it.copy(verborgen = it.verborgen + key) }

    override fun toonZender(key: String) {
        wijzigZenders { it.copy(verborgen = it.verborgen - key) }
        // Zat de zender enkel in een reservebron, dan werd die voor hem niet opgehaald.
        if (_state.value.resultaat?.programmas?.get(key).isNullOrEmpty()) vernieuw()
    }

    override fun toonAlleZenders() = wijzigZenders { it.copy(verborgen = emptySet()) }

    override fun tipGezien() {
        voorkeuren.tipGezien = true
        _state.update { it.copy(toonTip = false) }
    }

    private suspend fun vernieuwNu() {
        if (bezig) return
        bezig = true
        _state.update { it.copy(laden = true, fout = null) }
        try {
            val r = repo.haalOp()
            cacheTijd = repo.cacheTijd()
            val leeg = r.programmas.values.all { it.isEmpty() }
            val onbereikbaar = r.bronnen.none { it.ok }
            _state.update {
                it.copy(
                    laden = false,
                    resultaat = r,
                    waarschuwing = if (!leeg && onbereikbaar) "Vernieuwen mislukt, gids van ${dagNaam(r.opgehaald, Klok.nu()).lowercase()} ${uurMin(r.opgehaald)}" else null,
                    fout = when {
                        !leeg -> null
                        r.bronnen.any { b -> b.klokFout } ->
                            "Geen programma's: de datum en tijd van dit toestel lijken niet te kloppen. Zet ze juist en vernieuw."
                        else -> "Geen programma's gevonden. Open Status om de bronnen te controleren."
                    },
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    laden = false,
                    fout = "Vernieuwen mislukt: ${e.message ?: "onbekende fout"}",
                    waarschuwing = if (it.resultaat != null) "Vernieuwen mislukt" else null,
                )
            }
        } finally {
            bezig = false
        }
    }
}

class MainActivity : ComponentActivity() {
    private val vm: GidsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Vanaf Android 15 tekent een app altijd tot onder de systeembalken. Lichte
        // pictogrammen op de donkere gids; de inhoud blijft er via safeDrawing vanaf.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        VernieuwWerker.plan(this)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { vm.bewaak() }
        }
        setContent { App(vm) }
    }
}

@Composable
fun App(vm: GidsViewModel) {
    val state by vm.state.collectAsState()
    var toonStatus by remember { mutableStateOf(false) }
    // Bewaart de positie in de gids terwijl het statusscherm open staat.
    val bewaarder = rememberSaveableStateHolder()

    MaterialTheme(colorScheme = darkColorScheme()) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Kleuren.achtergrond)
                // Onthoudt de pijltjestoets voor het navigatiegeluid (zie Geluid).
                .onPreviewKeyEvent { Geluid.toets(it); false }
                // Weg van statusbalk, navigatiebalk en camera-uitsparing (op TV is dit 0).
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            if (maxHeight > maxWidth) {
                // Telefoon of tablet rechtop: de gids past enkel liggend.
                DraaiMelding()
            } else if (toonStatus) {
                StatusScherm(state = state, onTerug = { toonStatus = false })
            } else {
                bewaarder.SaveableStateProvider("gids") {
                    GidsScherm(
                        state = state,
                        onVernieuw = { vm.vernieuw() },
                        onStatus = { toonStatus = true },
                        zenderActies = vm,
                    )
                }
            }
        }
    }
}

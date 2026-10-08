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
)

class GidsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = EpgRepository(app)
    private val _state = MutableStateFlow(GidsState())
    val state: StateFlow<GidsState> = _state.asStateFlow()
    private var bezig = false

    private val cacheGeladen = viewModelScope.launch {
        val cache = repo.laadCache()
        if (cache != null) _state.update { it.copy(resultaat = cache, laden = false) }
    }

    /**
     * Ververst de gids zodra ze verouderd is. Loopt enkel zolang de app in beeld
     * is; het ophalen zelf gebeurt in viewModelScope, zodat het niet halverwege
     * afgebroken wordt als je de app verlaat.
     */
    suspend fun bewaak() {
        cacheGeladen.join()
        while (true) {
            if (verouderd(_state.value.resultaat)) vernieuw()
            delay(15 * MIN)
        }
    }

    private fun verouderd(r: EpgResultaat?): Boolean =
        r == null || System.currentTimeMillis() - r.opgehaald > 6 * UUR

    fun vernieuw() {
        viewModelScope.launch { vernieuwNu() }
    }

    private suspend fun vernieuwNu() {
        if (bezig) return
        bezig = true
        _state.update { it.copy(laden = true, fout = null) }
        try {
            val r = repo.haalOp(_state.value.resultaat)
            val leeg = r.programmas.values.all { it.isEmpty() }
            _state.update {
                it.copy(
                    laden = false,
                    resultaat = r,
                    fout = if (leeg) "Geen programma's gevonden. Open Status om de bronnen te controleren." else null,
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(laden = false, fout = "Vernieuwen mislukt: ${e.message ?: "onbekende fout"}") }
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
                    )
                }
            }
        }
    }
}

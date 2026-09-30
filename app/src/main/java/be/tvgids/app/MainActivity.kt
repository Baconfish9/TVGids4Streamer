package be.tvgids.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val filter: Land? = null,
)

class GidsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = EpgRepository(app)
    private val _state = MutableStateFlow(GidsState())
    val state: StateFlow<GidsState> = _state.asStateFlow()
    private var bezig = false

    init {
        viewModelScope.launch {
            val cache = repo.laadCache()
            if (cache != null) _state.update { it.copy(resultaat = cache, laden = false) }
            if (verouderd(cache)) vernieuwNu()
            while (true) {
                delay(15 * MIN)
                if (verouderd(_state.value.resultaat)) vernieuwNu()
            }
        }
    }

    private fun verouderd(r: EpgResultaat?): Boolean =
        r == null || System.currentTimeMillis() - r.opgehaald > 6 * UUR

    fun vernieuw() {
        viewModelScope.launch { vernieuwNu() }
    }

    fun zetFilter(land: Land?) {
        _state.update { it.copy(filter = land) }
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
fun App(vm: GidsViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var toonStatus by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(Modifier.fillMaxSize().background(Kleuren.achtergrond)) {
            if (toonStatus) {
                StatusScherm(state = state, onTerug = { toonStatus = false })
            } else {
                GidsScherm(
                    state = state,
                    onVernieuw = { vm.vernieuw() },
                    onFilter = { vm.zetFilter(it) },
                    onStatus = { toonStatus = true },
                )
            }
        }
    }
}

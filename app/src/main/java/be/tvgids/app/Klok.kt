package be.tvgids.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * De tijd volgens de gids. Staat de klok van het toestel verkeerd (een emulator
 * uit een oude snapshot, een tv zonder netwerktijd), dan rekenen we met de tijd
 * uit de Date-header van de servers. Zo valt de gids niet op de verkeerde dag.
 */
object Klok {

    /** Kleinere verschillen zijn netwerkvertraging of afronding van de Date-header. */
    private const val DREMPEL = 2 * MIN

    private val _afwijking = MutableStateFlow(0L)
    /** Hoeveel de servertijd voorloopt op de toestelklok, in ms (0 als de klok klopt). */
    val afwijking: StateFlow<Long> = _afwijking.asStateFlow()

    @Volatile
    var gemeten = false
        private set

    fun nu(): Long = System.currentTimeMillis() + _afwijking.value

    /** Verwerkt de Date-header van een antwoord (0 als die ontbreekt). */
    fun meet(serverTijd: Long, toestelTijd: Long = System.currentTimeMillis()) {
        if (serverTijd <= 0) return
        val verschil = serverTijd - toestelTijd
        _afwijking.value = if (abs(verschil) >= DREMPEL) verschil else 0L
        gemeten = true
    }
}

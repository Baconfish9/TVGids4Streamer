package be.tvgids.app

import android.content.Context
import androidx.core.content.edit

/** Volgorde en zichtbaarheid van de zenders, zoals ingesteld vanuit de gids. */
data class ZenderInstelling(
    /** Alle zendersleutels, ook de verborgen, zodat die op hun plaats terugkomen. */
    val volgorde: List<String>,
    val verborgen: Set<String>,
) {
    val zichtbaar: List<String> get() = volgorde.filter { it !in verborgen }

    companion object {
        /** Eerst de Vlaamse, dan de Nederlandse zenders, zoals in [Config.ZENDERS]. */
        val STANDAARD: List<String> = Config.ZENDERS.sortedBy { it.land.ordinal }.map { it.key }
    }
}

/**
 * Houdt de opgeslagen volgorde aan, voor zover die zenders nog bestaan, en zet
 * zenders die er later in [alle] bijkwamen onderaan. Zo blijft een instelling
 * bruikbaar na een update met meer of minder zenders.
 */
internal fun samenvoegen(opgeslagen: List<String>, alle: List<String>): List<String> {
    val bekend = alle.toSet()
    val behouden = opgeslagen.filter { it in bekend }.distinct()
    return behouden + alle.filter { it !in behouden }
}

/**
 * Verplaatst [key] één zichtbare plaats omhoog ([stap] -1) of omlaag (+1).
 * Verborgen zenders tellen niet mee: die sla je in één keer over.
 */
internal fun ZenderInstelling.verplaats(key: String, stap: Int): ZenderInstelling {
    val zicht = zichtbaar
    val i = zicht.indexOf(key)
    if (i < 0) return this
    val buur = zicht.getOrNull(i + stap) ?: return this
    val lijst = volgorde.toMutableList()
    lijst.remove(key)
    val j = lijst.indexOf(buur)
    lijst.add(if (stap < 0) j else j + 1, key)
    return copy(volgorde = lijst)
}

/** Bewaart de [ZenderInstelling] op het toestel. */
class ZenderVoorkeuren(context: Context) {

    private val prefs = context.getSharedPreferences("zenders", Context.MODE_PRIVATE)

    fun laad(): ZenderInstelling {
        val opgeslagen = prefs.getString(VOLGORDE, null)?.split(',')?.filter { it.isNotEmpty() }.orEmpty()
        val alle = ZenderInstelling.STANDAARD
        return ZenderInstelling(
            volgorde = samenvoegen(opgeslagen, alle),
            verborgen = prefs.getStringSet(VERBORGEN, emptySet()).orEmpty().filter { it in alle }.toSet(),
        )
    }

    fun bewaar(z: ZenderInstelling) {
        prefs.edit {
            putString(VOLGORDE, z.volgorde.joinToString(","))
            putStringSet(VERBORGEN, z.verborgen)
        }
    }

    /** Of de tip over lang indrukken al niet meer nodig is. */
    var tipGezien: Boolean
        get() = prefs.getBoolean(TIP, false)
        set(waarde) = prefs.edit { putBoolean(TIP, waarde) }

    private companion object {
        const val VOLGORDE = "volgorde"
        const val VERBORGEN = "verborgen"
        const val TIP = "tip_gezien"
    }
}

/** Wat de gids met de zenders kan doen; de ViewModel bewaart het. */
interface ZenderActies {
    fun verplaatsZender(key: String, stap: Int)
    fun verbergZender(key: String)
    fun toonZender(key: String)
    fun toonAlleZenders()
    fun tipGezien()
}

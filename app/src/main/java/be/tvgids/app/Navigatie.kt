package be.tvgids.app

import kotlin.math.abs
import kotlin.math.min

// Navigatie met de pijltjestoetsen door het raster, los van Compose zodat ze te testen is.

/**
 * Heel korte opvullers (Keno, Lotto, ...) zijn amper zichtbaar; die slaan we
 * over bij het navigeren, behalve het blok waar de focus nu op staat.
 */
internal fun navigeerbaar(rij: List<Programma>, huidig: Programma?): List<Programma> =
    rij.filter { it == huidig || it.stop - it.start >= 5 * MIN }.ifEmpty { rij }

/** Het programma in een rij dat [anker] bevat, of anders het dichtstbijzijnde. */
internal fun rondAnker(alles: List<Programma>, anker: Long): Programma? {
    val rij = navigeerbaar(alles, null)
    return rij.firstOrNull { it.start <= anker && it.stop > anker }
        ?: rij.minByOrNull { min(abs(it.start - anker), abs(it.stop - anker)) }
}

/** Het vorige ([stap] -1) of volgende (+1) blok in dezelfde rij, of null aan de rand. */
internal fun naastLiggend(rij: List<Programma>, huidig: Programma, stap: Int): Programma? {
    val progs = navigeerbaar(rij, huidig)
    val i = progs.indexOf(huidig)
    if (i < 0) return null
    return progs.getOrNull(i + stap)
}

/** De eerstvolgende rij met programma's boven ([stap] -1) of onder (+1) [rij], of null. */
internal fun volgendeRij(perRij: List<List<Programma>>, rij: Int, stap: Int): Int? {
    var r = rij + stap
    while (r in perRij.indices && perRij[r].isEmpty()) r += stap
    return r.takeIf { it in perRij.indices }
}

package be.tvgids.app

import kotlin.math.abs
import kotlin.math.max
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

/** Zoveel minuten van een blok moet in beeld staan om als zichtbaar te tellen. */
internal const val RAND_MIN = 15f

/**
 * Het tijdstip waarrond omhoog/omlaag zoekt, binnen het beeld gehouden. Stond de
 * focus op een lang programma dat al bezig was, dan ligt het anker op de linkerrand;
 * zonder deze grens koos de volgende rij soms een programma dat net daarna eindigt,
 * en schoof de tijdlijn helemaal terug naar het begin daarvan.
 */
internal fun ankerInBeeld(anker: Long, beeldStart: Long, beeldEind: Long): Long {
    val marge = (RAND_MIN * MIN).toLong()
    val van = beeldStart + marge
    val tot = beeldEind - marge
    return if (van >= tot) anker else anker.coerceIn(van, tot)
}

/**
 * Nieuwe beginpositie van het beeld (in minuten na het begin van het raster) om het
 * gefocuste blok van [start] tot [eind] te tonen, of null als het beeld mag blijven.
 * Een blok links buiten beeld wordt zo weinig mogelijk teruggeschoven: helemaal als
 * het past, anders enkel het laatste stuk (de titel schuift toch mee met de rand).
 */
internal fun nieuweScroll(start: Float, eind: Float, huidig: Float, breedte: Float, maxScroll: Float): Float? {
    val doel = when {
        // Links buiten beeld
        eind <= huidig + RAND_MIN -> max(start - RAND_MIN, eind - breedte + 3 * RAND_MIN)
        // Rechts buiten beeld
        start >= huidig + breedte - RAND_MIN -> start - RAND_MIN
        // Lang programma dat het hele beeld vult: laten staan
        start < huidig && eind > huidig + breedte -> return null
        // Begint vlak voor de rechterrand: een stuk opschuiven
        start > huidig + breedte - 3 * RAND_MIN -> start - 2 * RAND_MIN
        else -> return null
    }
    return doel.coerceIn(0f, maxScroll)
}

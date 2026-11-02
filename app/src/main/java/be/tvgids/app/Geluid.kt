package be.tvgids.app

import android.os.SystemClock
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

// De systeemgeluidjes van Android TV bij het navigeren met de afstandsbediening.
// Android speelt ze enkel als "Geluidseffecten" in de systeeminstellingen aan staat.
object Geluid {
    // Richting van de laatst ingedrukte pijltjestoets. Het geluid volgt pas als de
    // focus ook echt verspringt, zodat je aan de rand van het raster niets hoort.
    private var richting = 0
    private var tijdstip = 0L

    fun toets(e: KeyEvent) {
        if (e.type != KeyEventType.KeyDown) return
        richting = when (e.key) {
            Key.DirectionLeft -> SoundEffectConstants.NAVIGATION_LEFT
            Key.DirectionRight -> SoundEffectConstants.NAVIGATION_RIGHT
            Key.DirectionUp -> SoundEffectConstants.NAVIGATION_UP
            Key.DirectionDown -> SoundEffectConstants.NAVIGATION_DOWN
            else -> 0
        }
        tijdstip = SystemClock.uptimeMillis()
    }

    // Aanroepen wanneer een onderdeel de focus krijgt. Focus die de app zelf
    // verplaatst (bij het openen of na het sluiten van een venster) blijft stil.
    fun focus(view: View) {
        if (richting == 0 || SystemClock.uptimeMillis() - tijdstip > 500) return
        view.playSoundEffect(richting)
        richting = 0
    }

    fun klik(view: View) = view.playSoundEffect(SoundEffectConstants.CLICK)
}

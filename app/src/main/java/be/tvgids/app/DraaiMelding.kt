package be.tvgids.app

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Getoond wanneer een telefoon of tablet rechtop gehouden wordt: een toestel
 * dat telkens van staand naar liggend kantelt, met uitleg eronder.
 */
@Composable
fun DraaiMelding() {
    val lus = rememberInfiniteTransition(label = "draai")
    val hoek by lus.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 2400
                0f at 0
                0f at 500
                90f at 1100 using LinearOutSlowInEasing
                90f at 1900
                0f at 2400
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "hoek",
    )

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            // Het toestel: een rand met een "scherm" erin en een streepje als luidspreker.
            Box(
                Modifier
                    .size(width = 70.dp, height = 124.dp)
                    .graphicsLayer { rotationZ = hoek }
                    .clip(RoundedCornerShape(14.dp))
                    .border(4.dp, Kleuren.focus, RoundedCornerShape(14.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 9.dp, vertical = 14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Kleuren.live)
                )
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                        .size(width = 16.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Kleuren.focus.copy(alpha = 0.6f))
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Draai je toestel",
            color = Kleuren.tekst,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "De gids werkt enkel in liggende stand. Staat automatisch draaien uit? Zet het dan aan.",
            color = Kleuren.tekstZacht,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
    }
}

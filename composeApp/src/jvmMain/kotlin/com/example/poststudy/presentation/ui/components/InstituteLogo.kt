package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poststudy.presentation.theme.Signal
import org.jetbrains.compose.resources.painterResource
import poststudy.composeapp.generated.resources.Res
import poststudy.composeapp.generated.resources.markaz_logo

/** Emblem of the Institute of ICT and Military Communications; [glow] adds a cyan halo on dark screens. */
@Composable
fun InstituteLogo(size: Dp = 160.dp, modifier: Modifier = Modifier, glow: Boolean = false) {
    Image(
        painter = painterResource(Res.drawable.markaz_logo),
        contentDescription = "Axborot-kommunikatsiya texnologiyalari va harbiy aloqa instituti",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .then(
                if (glow) Modifier.drawBehind {
                    val r = this.size.minDimension * 0.75f
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Signal.Cyan.copy(alpha = 0.45f), Color.Transparent), center = center, radius = r),
                        radius = r,
                        center = center
                    )
                } else Modifier
            )
    )
}

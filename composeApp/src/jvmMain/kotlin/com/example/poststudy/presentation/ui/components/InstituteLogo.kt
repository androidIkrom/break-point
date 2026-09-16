package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import poststudy.composeapp.generated.resources.Res
import poststudy.composeapp.generated.resources.markaz_logo

/** Emblem of the Institute of ICT and Military Communications. */
@Composable
fun InstituteLogo(size: Dp = 160.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.markaz_logo),
        contentDescription = "Axborot-kommunikatsiya texnologiyalari va harbiy aloqa instituti",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

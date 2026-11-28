package com.example.roomie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.roomie.ui.theme.GlassBorder
import com.example.roomie.ui.theme.GlassEnd
import com.example.roomie.ui.theme.GlassStart
import com.example.roomie.ui.theme.ShadowWarm

/** White borders thinner than this disappear against the cream background. */
val WhiteBorderWidth = 2.dp

/** Warm translucent card from the onboarding refresh: tan gradient, light border, soft shadow. */
fun Modifier.glassSurface(
    shape: Shape,
    borderWidth: Dp = WhiteBorderWidth,
    borderColor: Color = GlassBorder,
): Modifier = this
    .shadow(3.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.06f), spotColor = ShadowWarm.copy(alpha = 0.06f))
    .clip(shape)
    .background(Brush.horizontalGradient(listOf(GlassStart, GlassEnd)))
    .border(borderWidth, borderColor, shape)

package com.swordfish.touchinput.radial.settings

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/** Placement changes only: original PadKit input processing stays intact. */
data class ControlLayoutBinding(
    val positions: Map<String, TouchControllerSettingsManager.ControlPosition>,
    val reportBounds: (String, Rect) -> Unit,
)

val LocalControlLayoutBinding = compositionLocalOf<ControlLayoutBinding?> { null }

@Composable
fun Modifier.controlLayout(key: String): Modifier {
    val binding = LocalControlLayoutBinding.current ?: return this
    val density = LocalDensity.current.density
    val shift = binding.positions[key] ?: TouchControllerSettingsManager.ControlPosition()
    return this.offset {
        IntOffset(
            (shift.dxDp * density).roundToInt(),
            (shift.dyDp * density).roundToInt(),
        )
    }.onGloballyPositioned { binding.reportBounds(key, it.boundsInRoot()) }
}

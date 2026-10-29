package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private fun label(key: String) = when(key) {
    "dpad" -> "D-pad group"
    "right_dpad" -> "Right D-pad"
    "face_buttons" -> "Face buttons group"
    "start" -> "Start"
    "select" -> "Select"
    "menu" -> "Menu"
    else -> key.uppercase()
}

/** Gesture handles sit over native controls and do not replace their click/touch code. */
@Composable
fun ControlDragEditor(
    bounds: Map<String, Rect>,
    onDrag: (String, Offset) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
) {
    val density = LocalDensity.current
    val currentOnDrag by androidx.compose.runtime.rememberUpdatedState(onDrag)
    Box(Modifier.fillMaxSize()) {
        bounds.forEach { (key,rect) ->
            if (rect.width <= 0f || rect.height <= 0f) return@forEach
            val width = with(density) { rect.width.toDp() }
            val height = with(density) { rect.height.toDp() }
            Box(
                modifier = Modifier
                    .offset { IntOffset(rect.left.roundToInt(),rect.top.roundToInt()) }
                    .size(width,height)
                    .border(2.dp,Color(0xFFFFCB47),RoundedCornerShape(10.dp))
                    .background(Color(0x22FFCB47),RoundedCornerShape(10.dp))
                    .pointerInput(key) {
                        detectDragGestures(
                            onDrag = { change, delta ->
                                change.consume()
                                currentOnDrag(key, delta)
                            },
                        )
                    },
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = label(key),
                    color = Color.Black,
                    modifier = Modifier.background(Color(0xFFFFCB47),RoundedCornerShape(3.dp))
                        .padding(horizontal=4.dp,vertical=1.dp),
                )
            }
        }
        Row(
            modifier = Modifier.align(Alignment.TopCenter).padding(top=12.dp),
            horizontalArrangement=Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick=onReset) { Text("Reset controls") }
            Button(onClick=onDone) { Text("Done") }
        }
    }
}

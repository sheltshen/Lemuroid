package com.swordfish.lemuroid.app.mobile.feature.game

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/** Optional shortcuts: native gamepad art and console-specific controls are untouched. */
@Composable
fun CustomShortcutOverlay(
    viewModel: com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel,
    isLandscape: Boolean,
    editing: Boolean,
    controllerId: String,
    resetRevision: Int,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("custom_shortcuts_v1", Context.MODE_PRIVATE) }
    val orientation = if (isLandscape) "landscape" else "portrait"
    val defaults = remember(isLandscape) {
        if (isLandscape) mapOf("save" to Offset(.74f,.04f),"load" to Offset(.82f,.04f),"cheats" to Offset(.90f,.04f))
        else mapOf("save" to Offset(.57f,.04f),"load" to Offset(.70f,.04f),"cheats" to Offset(.83f,.04f))
    }
    // Each console type and orientation has independent shortcut positions.
    // resetRevision re-reads only the current profile after Reset controls.
    val positions = remember(controllerId, isLandscape, resetRevision) {
        migrateLegacyGbaShortcuts(prefs, controllerId, orientation)
        mutableStateMapOf<String, Offset>().apply {
            defaults.forEach { (id, point) ->
                this[id] = Offset(
                    prefs.getFloat(shortcutKey(controllerId, orientation, id, "x"), point.x),
                    prefs.getFloat(shortcutKey(controllerId, orientation, id, "y"), point.y),
                )
            }
        }
    }
    var cheatsVisible by remember { mutableStateOf(false) }
    val actions = listOf("save" to Icons.Default.Save,"load" to Icons.Default.FolderOpen,"cheats" to Icons.Default.Code)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val screenHeight = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val pxSize = with(LocalDensity.current) { 46.dp.toPx() }
        actions.forEach { (id,icon) ->
            val p = positions[id] ?: defaults.getValue(id)
            val x = (p.x * screenWidth).roundToInt().coerceIn(0,(screenWidth-pxSize).coerceAtLeast(0f).roundToInt())
            val y = (p.y * screenHeight).roundToInt().coerceIn(0,(screenHeight-pxSize).coerceAtLeast(0f).roundToInt())
            Box(
                modifier = Modifier
                    .offset { IntOffset(x,y) }
                    .size(46.dp)
                    .then(if(editing) Modifier.pointerInput(id,orientation,controllerId) {
                        detectDragGestures(
                            onDragEnd = {
                                val current = positions[id] ?: defaults.getValue(id)
                                prefs.edit()
                                    .putFloat(shortcutKey(controllerId, orientation, id, "x"), current.x)
                                    .putFloat(shortcutKey(controllerId, orientation, id, "y"), current.y)
                                    .apply()
                            },
                            onDrag = { change,delta ->
                                change.consume()
                                val current = positions[id] ?: defaults.getValue(id)
                                positions[id] = Offset(
                                    (current.x+delta.x/screenWidth).coerceIn(0f,(1f-pxSize/screenWidth).coerceAtLeast(0f)),
                                    (current.y+delta.y/screenHeight).coerceIn(0f,(1f-pxSize/screenHeight).coerceAtLeast(0f)),
                                )
                            },
                        )
                    } else Modifier.clickable {
                        when(id) {
                            "save" -> if(viewModel.customSaveStatesSupported) viewModel.saveQuickSave()
                                else Toast.makeText(context,"Save states not supported",Toast.LENGTH_SHORT).show()
                            "load" -> if(viewModel.customSaveStatesSupported) viewModel.loadQuickSave()
                                else Toast.makeText(context,"Save states not supported",Toast.LENGTH_SHORT).show()
                            else -> cheatsVisible = true
                        }
                    }),
                contentAlignment = Alignment.Center,
            ) {
                Surface(Modifier.fillMaxSize(),shape=CircleShape,color=Color.White.copy(alpha=.52f),shadowElevation=2.dp) {
                    Box(contentAlignment=Alignment.Center) {
                        Icon(icon, contentDescription=when(id) { "save" -> "Quick Save"; "load" -> "Quick Load"; else -> "Cheats" },
                            tint=Color.DarkGray,modifier=Modifier.size(24.dp))
                    }
                }
            }
        }
    }
    if(cheatsVisible) CheatCodeDialog(viewModel,prefs,onDismiss={cheatsVisible=false})
}

/** Profile keys are scoped by console type and orientation, not shared globally. */
private fun shortcutKey(
    controllerId: String,
    orientation: String,
    id: String,
    axis: String,
): String = "console_${controllerId}_${orientation}_${id}_${axis}"

/**
 * Older builds used orientation-only keys. Preserve the previous GBA arrangement
 * once, without giving other consoles the same shared values.
 */
private fun migrateLegacyGbaShortcuts(
    prefs: android.content.SharedPreferences,
    controllerId: String,
    orientation: String,
) {
    if (controllerId != "GBA") return
    val migrationKey = "migration_shortcuts_GBA_${orientation}_v2"
    if (prefs.getBoolean(migrationKey, false)) return
    val editor = prefs.edit()
    for (id in listOf("save", "load", "cheats")) {
        for (axis in listOf("x", "y")) {
            val oldKey = "${orientation}_${id}_${axis}"
            val newKey = shortcutKey(controllerId, orientation, id, axis)
            if (prefs.contains(oldKey) && !prefs.contains(newKey)) {
                editor.putFloat(newKey, prefs.getFloat(oldKey, 0f))
            }
        }
    }
    editor.putBoolean(migrationKey, true).apply()
}

/** Reset the shortcut positions of the SELECTED console/orientation only. */
fun resetConsoleShortcuts(
    context: Context,
    controllerId: String,
    isLandscape: Boolean,
) {
    val orientation = if (isLandscape) "landscape" else "portrait"
    val prefs = context.getSharedPreferences("custom_shortcuts_v1", Context.MODE_PRIVATE)
    val editor = prefs.edit()
    for (id in listOf("save", "load", "cheats")) {
        editor.remove(shortcutKey(controllerId, orientation, id, "x"))
        editor.remove(shortcutKey(controllerId, orientation, id, "y"))
    }
    // Prevent a reset GBA layout from importing legacy positions again.
    if (controllerId == "GBA") editor.putBoolean("migration_shortcuts_GBA_${orientation}_v2", true)
    editor.apply()
}

private data class CheatCode(val name:String,val code:String,val enabled:Boolean)

@Composable
private fun CheatCodeDialog(
    viewModel: com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel,
    prefs: android.content.SharedPreferences,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val key = "cheats_${viewModel.customCheatGameId}"
    val codes = remember(key) {
        mutableStateListOf<CheatCode>().apply {
            runCatching {
                val json = JSONArray(prefs.getString(key,"[]") ?: "[]")
                for(i in 0 until json.length()) {
                    val item=json.getJSONObject(i)
                    add(CheatCode(item.optString("name"),item.optString("code"),item.optBoolean("enabled")))
                }
            }
        }
    }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    fun persist() {
        val data=JSONArray()
        codes.forEach { data.put(JSONObject().put("name",it.name).put("code",it.code).put("enabled",it.enabled)) }
        prefs.edit().putString(key,data.toString()).apply()
    }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Cheat codes")},
        text={
            Column(Modifier.fillMaxWidth().heightIn(max=440.dp).verticalScroll(rememberScrollState())) {
                Text("Cheat formats depend on the active emulator core.")
                codes.forEachIndexed { index,entry ->
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(checked=entry.enabled,onCheckedChange={ enabled ->
                            if(viewModel.setCustomCheat(index,enabled,entry.code)) {
                                codes[index]=entry.copy(enabled=enabled)
                                persist()
                            } else Toast.makeText(context,"Could not apply this cheat",Toast.LENGTH_SHORT).show()
                        })
                        Column(Modifier.weight(1f)) {
                            Text(entry.name.ifBlank { "Cheat ${index+1}" })
                            Text(entry.code,maxLines=2)
                        }
                        TextButton(onClick={
                            viewModel.setCustomCheat(index,false,entry.code)
                            codes.removeAt(index)
                            codes.forEachIndexed { i,c -> viewModel.setCustomCheat(i,c.enabled,c.code) }
                            persist()
                        }) { Text("Delete") }
                    }
                }
                OutlinedTextField(value=name,onValueChange={name=it},label={Text("Name")},singleLine=true)
                OutlinedTextField(value=code,onValueChange={code=it},label={Text("Cheat code")})
                TextButton(onClick={
                    if(code.isNotBlank()) {
                        val newCode=CheatCode(name.trim(),code.trim(),false)
                        codes.add(newCode);persist();name="";code=""
                    }
                }) { Text("Add code") }
            }
        },
        confirmButton={TextButton(onClick=onDismiss){Text("Done")}},
    )
}

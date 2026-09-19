package com.felix.greengriffin.board.presentation.components

import android.content.ClipData
import android.util.Log
import com.felix.greengriffin.board.domain.model.StoneData
import kotlinx.serialization.json.Json

/** Drag and drop carries a stone as the serialized form of [StoneData]. */
fun StoneData.asClipData(): ClipData =
    ClipData.newPlainText("${letter}_$value", Json.encodeToString(this))

/** The dragged stone, or `null` when the clip data does not hold one. */
fun stoneFromClipData(clipData: ClipData): StoneData? = try {
    Json.decodeFromString<StoneData>(clipData.getItemAt(0).text.toString())
} catch (e: Exception) {
    Log.w("StoneClipData", "Failed to parse dragged stone", e)
    null
}

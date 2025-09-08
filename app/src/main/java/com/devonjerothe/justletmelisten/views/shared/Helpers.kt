package com.devonjerothe.justletmelisten.views.shared

import androidx.compose.runtime.Composable
import java.util.Locale

@Composable
fun formatTime(seconds: Float, tile: Boolean = false): String {
    if (seconds.isInfinite() || seconds.isNaN()) {
        return "00:00"
    }

    val totalSeconds = seconds.toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val remainingSeconds = totalSeconds % 60

    return if (hours > 0) {
        val format = if (tile) "%2d hour %02d min" else "%2d:%02d:%02d"
        String.format(Locale.US, format, hours, minutes, remainingSeconds)
    } else {
        val format = if (tile) "%02d min" else "%02d:%02d"
        String.format(Locale.US, format, minutes, remainingSeconds)
    }
}

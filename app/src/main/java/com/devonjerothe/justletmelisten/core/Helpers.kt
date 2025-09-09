package com.devonjerothe.justletmelisten.core

import androidx.compose.runtime.Composable
import androidx.core.text.HtmlCompat
import java.text.SimpleDateFormat
import java.util.Date
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

fun parseDuration(duration: String?): Long {
    if (duration.isNullOrBlank()) return 0L

    val asSeconds = duration.toLongOrNull()
    if (asSeconds != null) return asSeconds

    try {
        val parts = duration.split(":").map { it.toLong() }.reversed()
        var totalSeconds = 0L
        if (parts.isNotEmpty()) totalSeconds += parts[0]
        if (parts.size > 1) totalSeconds += parts[1] * 60
        if (parts.size > 2) totalSeconds += parts[2] * 3600
        return totalSeconds
    } catch (e: Exception) {
        return 0L
    }
}

fun stripHtml(html: String?): String {
    if (html.isNullOrBlank()) return ""
    val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    return spanned.toString()
}

fun parseDate(dateString: String): Long? {
    return try {
        // Example format: Mon, 01 Sep 2025 14:45:00 +0000
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
        format.parse(dateString)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

fun toDisplayDate(dateString: String): String? {
    val mili = parseDate(dateString)
    return mili?.let {
        try {
            val format = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
            format.format(Date(it))
        } catch (e: Exception) {
            null
        }
    }
}

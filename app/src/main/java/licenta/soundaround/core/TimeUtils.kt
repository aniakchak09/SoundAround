package licenta.soundaround.core

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

fun formatRelativeTime(utcTimestamp: String): String {
    return try {
        val time = utcFormat.parse(utcTimestamp)?.time ?: return ""
        val diffMs = System.currentTimeMillis() - time
        val diffMin = diffMs / 60_000
        when {
            diffMin < 1 -> "just now"
            diffMin < 60 -> "$diffMin min ago"
            diffMin < 1440 -> "${diffMin / 60}h ago"
            else -> "${diffMin / 1440}d ago"
        }
    } catch (e: Exception) {
        ""
    }
}

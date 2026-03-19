package com.calpoly.fleecedlogin.util

import java.util.Calendar
import java.util.TimeZone

/**
 * Returns true if today falls within the 2025 NFL regular season
 * (Week 1 = Sept 4, 2025 through end of Week 18)
 */
fun isNflInSeason(): Boolean {
    val now = Calendar.getInstance()
    val seasonStart = Calendar.getInstance(TimeZone.getTimeZone("America/New_York")).apply {
        set(2025, Calendar.SEPTEMBER, 4, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    // 18 weeks × 7 days = 126 days after Sept 4 → Jan 8, 2026 (end of Week 18)
    val seasonEnd = Calendar.getInstance(TimeZone.getTimeZone("America/New_York")).apply {
        set(2026, Calendar.JANUARY, 8, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return now.timeInMillis in seasonStart.timeInMillis..seasonEnd.timeInMillis
}

/** Returns the current NFL regular-season week (1–18) */
fun getCurrentNflWeek(): Int {
    // NFL 2025 regular season: Week 1 began Thursday Sept 4, 2025
    val seasonStart = Calendar.getInstance(TimeZone.getTimeZone("America/New_York")).apply {
        set(2025, Calendar.SEPTEMBER, 4, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val now = Calendar.getInstance()
    val daysDiff = ((now.timeInMillis - seasonStart.timeInMillis) / 86_400_000L).toInt()
    if (daysDiff < 0) return 1
    return ((daysDiff / 7) + 1).coerceIn(1, 18)
}

fun getRelativeTimeString(createdAt: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - createdAt

    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> "${weeks}w"
    }
}

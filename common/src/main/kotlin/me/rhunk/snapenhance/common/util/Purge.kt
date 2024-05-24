package me.rhunk.snapenhance.common.util

val PURGE_VALUES = arrayOf("1_hour", "3_hours", "6_hours", "12_hours", "1_day", "3_days", "1_week", "2_weeks", "1_month", "3_months", "6_months")
const val PURGE_TRANSLATION_KEY = "features.options.auto_purge"
const val PURGE_DISABLED_KEY = "never"

fun getPurgeTime(
    value: String?
): Long? {
    return when (value) {
        "1_hour" -> 3600000L
        "3_hours" -> 10800000L
        "6_hours" -> 21600000L
        "12_hours" -> 43200000L
        "1_day" -> 86400000L
        "3_days" -> 259200000L
        "1_week" -> 604800000L
        "2_weeks" -> 1209600000L
        "1_month" -> 2592000000L
        "3_months" -> 7776000000L
        "6_months" -> 15552000000L
        else -> null
    }
}
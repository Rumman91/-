package com.example.util

import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToLong

object BengaliFormatter {

  private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

  private val bengaliMonths =
    arrayOf(
      "জানুয়ারি",
      "ফেব্রুয়ারি",
      "মার্চ",
      "এপ্রিল",
      "মে",
      "জুন",
      "জুলাই",
      "আগস্ট",
      "সেপ্টেম্বর",
      "অক্টোবর",
      "নভেম্বর",
      "ডিসেম্বর",
    )

  private val bengaliWeekdays =
    arrayOf(
      "রবিবার",
      "সোমবার",
      "মঙ্গলবার",
      "বুধবার",
      "বৃহস্পতিবার",
      "শুক্রবার",
      "শনিবার",
    )

  fun toBengaliDigits(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
      if (ch in '0'..'9') {
        sb.append(bengaliDigits[ch - '0'])
      } else {
        sb.append(ch)
      }
    }
    return sb.toString()
  }

  fun toEnglishDigits(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
      val idx = bengaliDigits.indexOf(ch)
      if (idx >= 0) {
        sb.append(('0'.code + idx).toChar())
      } else {
        sb.append(ch)
      }
    }
    return sb.toString()
  }

  fun parseAmountInput(input: String): Double? {
    val normalized = toEnglishDigits(input).replace(",", "").replace(" ", "").trim()
    if (normalized.isEmpty()) return null
    val parsed = normalized.toDoubleOrNull() ?: return null
    return if (parsed > 0.0 && parsed.isFinite()) parsed else null
  }

  fun formatInt(value: Int): String = toBengaliDigits(value.toString())

  fun formatTaka(amount: Double, showSymbol: Boolean = true): String {
    val isNegative = amount < -0.005
    val absVal = abs(amount)
    val totalPoisha = (absVal * 100.0).roundToLong()
    val whole = totalPoisha / 100
    val poisha = (totalPoisha % 100).toInt()

    val groupedWhole = formatIndianGrouping(whole)
    val numberPart =
      if (poisha == 0) {
        groupedWhole
      } else {
        val poishaStr = poisha.toString().padStart(2, '0')
        "$groupedWhole.$poishaStr"
      }

    val bengaliNumber = toBengaliDigits(numberPart)
    val prefix = if (isNegative) "-" else ""
    return if (showSymbol) "${prefix}৳ $bengaliNumber" else "$prefix$bengaliNumber"
  }

  private fun formatIndianGrouping(number: Long): String {
    val s = number.toString()
    if (s.length <= 3) return s
    val lastThree = s.substring(s.length - 3)
    var remaining = s.substring(0, s.length - 3)
    val sb = StringBuilder()
    while (remaining.length > 2) {
      sb.insert(0, "," + remaining.substring(remaining.length - 2))
      remaining = remaining.substring(0, remaining.length - 2)
    }
    sb.insert(0, remaining)
    sb.append(",").append(lastThree)
    return sb.toString()
  }

  fun formatBengaliDate(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = bengaliMonths[cal.get(Calendar.MONTH)]
    val year = cal.get(Calendar.YEAR)
    return toBengaliDigits("$day $month $year")
  }

  fun formatBengaliDateWithWeekday(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val weekday = bengaliWeekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]
    return "$weekday, ${formatBengaliDate(timestamp)}"
  }

  fun formatBengaliTime(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val hour24 = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val period =
      when (hour24) {
        in 4..11 -> "সকাল"
        in 12..15 -> "দুপুর"
        in 16..17 -> "বিকাল"
        in 18..19 -> "সন্ধ্যা"
        else -> "রাত"
      }
    val hour12 =
      when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
      }
    val minStr = minute.toString().padStart(2, '0')
    return "$period ${toBengaliDigits("$hour12:$minStr")}"
  }

  fun formatBengaliTimeWithSeconds(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val hour24 = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)
    val period =
      when (hour24) {
        in 4..11 -> "সকাল"
        in 12..15 -> "দুপুর"
        in 16..17 -> "বিকাল"
        in 18..19 -> "সন্ধ্যা"
        else -> "রাত"
      }
    val hour12 =
      when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
      }
    val minStr = minute.toString().padStart(2, '0')
    val secStr = second.toString().padStart(2, '0')
    return "$period ${toBengaliDigits("$hour12:$minStr:$secStr")}"
  }

  fun formatBengaliDateTime(timestamp: Long): String {
    return "${formatBengaliDate(timestamp)}, ${formatBengaliTime(timestamp)}"
  }

  fun formatBengaliDateTimeWithSeconds(timestamp: Long): String {
    return "${formatBengaliDate(timestamp)}, ${formatBengaliTimeWithSeconds(timestamp)}"
  }

  fun formatRelativeDayLabel(timestamp: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val targetDay = startOfDayMillis(timestamp)
    val today = startOfDayMillis(nowMillis)
    val yesterday = today - 24L * 60L * 60L * 1000L
    return when (targetDay) {
      today -> "আজ • ${formatBengaliDate(timestamp)}"
      yesterday -> "গতকাল • ${formatBengaliDate(timestamp)}"
      else -> formatBengaliDateWithWeekday(timestamp)
    }
  }

  fun startOfDayMillis(timestamp: Long): Long {
    val cal =
      Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    return cal.timeInMillis
  }

  fun endOfDayMillis(timestamp: Long): Long {
    val cal =
      Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
      }
    return cal.timeInMillis
  }

  fun utcPickerMillisToLocalDayStart(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = utcMillis
    }
    val localCal =
      Calendar.getInstance().apply {
        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    return localCal.timeInMillis
  }

  fun utcPickerMillisToLocalDayEnd(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = utcMillis
    }
    val localCal =
      Calendar.getInstance().apply {
        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
      }
    return localCal.timeInMillis
  }

  fun formatDateRangeLabel(startLocalMillis: Long?, endLocalMillis: Long?): String {
    return when {
      startLocalMillis != null && endLocalMillis != null -> {
        if (startOfDayMillis(startLocalMillis) == startOfDayMillis(endLocalMillis)) {
          formatBengaliDate(startLocalMillis)
        } else {
          "${formatBengaliDate(startLocalMillis)} - ${formatBengaliDate(endLocalMillis)}"
        }
      }
      startLocalMillis != null -> formatBengaliDate(startLocalMillis)
      else -> "তারিখ নির্বাচন"
    }
  }

  fun isToday(timestamp: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
    return startOfDayMillis(timestamp) == startOfDayMillis(nowMillis)
  }

  fun formatBackupFileTimestamp(timestamp: Long = System.currentTimeMillis()): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val year = cal.get(Calendar.YEAR)
    val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    val day = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    val hour = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val min = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "${year}${month}${day}_${hour}${min}"
  }
}

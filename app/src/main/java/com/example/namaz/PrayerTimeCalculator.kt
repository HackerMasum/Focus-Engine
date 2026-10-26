package com.example.namaz

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.*

enum class PrayerType {
    FARDH,
    SUNNAH_NAFL,
    FORBIDDEN_TIME
}

enum class PrayerName(
    val displayName: String,
    val arabicName: String,
    val iconRes: String,
    val type: PrayerType,
    val description: String
) {
    TAHAJJUD("Tahajjud", "التهجد", "🌌", PrayerType.SUNNAH_NAFL, "Last 1/3rd of the night (Qiyam al-Layl)"),
    SEHRI_END("Sehri End", "نهاية السحور", "🥣", PrayerType.SUNNAH_NAFL, "Suhoor cutoff time for fasting"),
    FAJR("Fajr", "الفجر", "🌅", PrayerType.FARDH, "Dawn prayer (Starts at true dawn, ends at sunrise)"),
    SUNRISE("Sunrise", "الشروق", "☀️", PrayerType.FORBIDDEN_TIME, "Forbidden to pray for ~15 mins as sun rises"),
    ISHRAQ("Ishraq", "الإشراق", "🌤️", PrayerType.SUNNAH_NAFL, "15 mins after sunrise until Chasht"),
    CHASHT("Chasht / Duha", "الضحى", "🌞", PrayerType.SUNNAH_NAFL, "Mid-morning prayer until Zawal"),
    ZAWAL("Zawal (Solar Noon)", "الزوال", "⚠️", PrayerType.FORBIDDEN_TIME, "Forbidden to pray right at solar zenith"),
    DHUHR("Dhuhr", "الظهر", "☀️", PrayerType.FARDH, "Noon prayer (Starts at solar noon, ends at Asr)"),
    ASR("Asr", "العصر", "⛅", PrayerType.FARDH, "Late afternoon prayer (Ends at sunset)"),
    PRE_SUNSET("Pre-Sunset Warning", "الغروب", "⚠️", PrayerType.FORBIDDEN_TIME, "Makruh to offer nafl ~15 mins before sunset"),
    MAGHRIB("Maghrib / Iftar", "المغرب", "🌇", PrayerType.FARDH, "Sunset prayer & fasting break"),
    ISHA("Isha", "العشاء", "🌙", PrayerType.FARDH, "Night prayer (Preferred before Islamic midnight)"),
    ISLAMIC_MIDNIGHT("Islamic Midnight", "نصف الليل", "🕛", PrayerType.SUNNAH_NAFL, "Exact midpoint between Maghrib and Fajr")
}

data class PrayerWaktItem(
    val prayer: PrayerName,
    val startTimeFormatted: String,
    val endTimeFormatted: String? = null,
    val startTimestampMillis: Long,
    val endTimestampMillis: Long? = null,
    val isCurrentlyActive: Boolean = false,
    val isNext: Boolean = false,
    val isPast: Boolean = false,
    val isAlarmEnabled: Boolean = true
)

data class WaktPomodoroAnalysis(
    val currentWaktName: String,
    val currentWaktArabic: String,
    val startTimeFormatted: String,
    val endTimeFormatted: String,
    val remainingMinutesInWakt: Int,
    val totalWaktMinutes: Int,
    val waktProgress: Float,
    val recommendedNamazBreakMinutes: Int,
    val availablePomodorosCount: Int
)

data class CityPreset(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneHours: Double
)

object CityPresets {
    val CITIES = listOf(
        CityPreset("Dhaka", "Bangladesh", 23.8103, 90.4125, 6.0),
        CityPreset("Chittagong", "Bangladesh", 22.3569, 91.7832, 6.0),
        CityPreset("Sylhet", "Bangladesh", 24.8949, 91.8687, 6.0),
        CityPreset("Rajshahi", "Bangladesh", 24.3745, 88.6042, 6.0),
        CityPreset("Khulna", "Bangladesh", 22.8456, 89.5403, 6.0),
        CityPreset("Mecca", "Saudi Arabia", 21.4225, 39.8262, 3.0),
        CityPreset("Medina", "Saudi Arabia", 24.4672, 39.6111, 3.0),
        CityPreset("Dubai", "UAE", 25.2048, 55.2708, 4.0),
        CityPreset("Karachi", "Pakistan", 24.8607, 67.0011, 5.0),
        CityPreset("Lahore", "Pakistan", 31.5204, 74.3587, 5.0),
        CityPreset("Cairo", "Egypt", 30.0444, 31.2357, 2.0),
        CityPreset("Istanbul", "Turkey", 41.0082, 28.9784, 3.0),
        CityPreset("London", "UK", 51.5074, -0.1278, 0.0),
        CityPreset("New York", "USA", 40.7128, -74.0060, -5.0),
        CityPreset("Toronto", "Canada", 43.6532, -79.3832, -5.0),
        CityPreset("Kuala Lumpur", "Malaysia", 3.1390, 101.6869, 8.0),
        CityPreset("Jakarta", "Indonesia", -6.2088, 106.8456, 7.0),
        CityPreset("Sydney", "Australia", -33.8688, 151.2093, 10.0)
    )
}

enum class JuristicMethod(val displayName: String, val shadowRatio: Double) {
    STANDARD("Shafi / Maliki / Hanbali", 1.0),
    HANAFI("Hanafi", 2.0)
}

class PrayerTimeCalculator(
    var latitude: Double = 23.8103,
    var longitude: Double = 90.4125,
    var timezone: Double = 6.0,
    var juristicMethod: JuristicMethod = JuristicMethod.HANAFI,
    // Custom minute offsets
    var fajrOffsetMin: Int = 0,
    var dhuhrOffsetMin: Int = 0,
    var asrOffsetMin: Int = 0,
    var maghribOffsetMin: Int = 0,
    var ishaOffsetMin: Int = 0
) {
    private val fajrAngle = 18.0 // Muslim World League standard
    private val ishaAngle = 18.0

    fun calculateAllWaktItems(date: Date = Date()): List<PrayerWaktItem> {
        val calendar = Calendar.getInstance().apply { time = date }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val julianDate = toJulianDate(year, month, day) - longitude / (15.0 * 24.0)

        // Solar parameters
        val d = julianDate - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(degToRad(g)) + 0.020 * sin(degToRad(2 * g)))

        val e = 23.439 - 0.00000036 * d
        val declination = radToDeg(asin(sin(degToRad(e)) * sin(degToRad(l))))
        val ra = radToDeg(atan2(cos(degToRad(e)) * sin(degToRad(l)), cos(degToRad(l)))) / 15.0
        val equationOfTime = (q / 15.0) - fixHour(ra)

        // Solar noon
        val noon = fixHour(12.0 + timezone - (longitude / 15.0) - equationOfTime)

        val sunriseDiff = sunAngleDiff(0.833, declination)
        val sunriseHour = noon - sunriseDiff
        val sunsetHour = noon + sunriseDiff

        val fajrDiff = sunAngleDiff(fajrAngle, declination)
        val fajrHour = noon - fajrDiff

        val ishaDiff = sunAngleDiff(ishaAngle, declination)
        val ishaHour = noon + ishaDiff

        val asrAngle = -radToDeg(atan(1.0 / (juristicMethod.shadowRatio + tan(degToRad(abs(latitude - declination))))))
        val asrDiff = sunAngleDiff(-asrAngle, declination)
        val asrHour = noon + asrDiff

        // Base calendar for today
        val todayBase = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        fun toTimestamp(hourVal: Double, offsetMinutes: Int = 0): Long {
            val totalMin = (fixHour(hourVal) * 60).roundToInt() + offsetMinutes
            val h = (totalMin / 60) % 24
            val m = totalMin % 60
            val cal = (todayBase.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
            }
            return cal.timeInMillis
        }

        val fajrMillis = toTimestamp(fajrHour, fajrOffsetMin)
        val sunriseMillis = toTimestamp(sunriseHour)
        val noonMillis = toTimestamp(noon, dhuhrOffsetMin)
        val asrMillis = toTimestamp(asrHour, asrOffsetMin)
        val sunsetMillis = toTimestamp(sunsetHour, maghribOffsetMin)
        val ishaMillis = toTimestamp(ishaHour, ishaOffsetMin)

        // Computed special and forbidden times
        val ishraqMillis = sunriseMillis + (18 * 60 * 1000L) // 18m after sunrise
        val chashtMillis = ishraqMillis + (45 * 60 * 1000L) // mid-morning
        val zawalStartMillis = noonMillis - (12 * 60 * 1000L) // 12m before Dhuhr
        val preSunsetMillis = sunsetMillis - (15 * 60 * 1000L) // 15m before sunset

        // Night calculations (Tahajjud & Islamic Midnight)
        val nextFajrMillis = fajrMillis + (24 * 3600 * 1000L)
        val nightDurationMillis = nextFajrMillis - sunsetMillis
        val islamicMidnightMillis = sunsetMillis + (nightDurationMillis / 2)
        val tahajjudStartMillis = sunsetMillis + ((nightDurationMillis * 2) / 3) // Last 1/3 of the night

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val now = System.currentTimeMillis()

        data class RawWakt(
            val prayer: PrayerName,
            val startMillis: Long,
            val endMillis: Long?
        )

        val rawList = listOf(
            RawWakt(PrayerName.TAHAJJUD, tahajjudStartMillis, fajrMillis),
            RawWakt(PrayerName.SEHRI_END, fajrMillis - (10 * 60 * 1000L), fajrMillis),
            RawWakt(PrayerName.FAJR, fajrMillis, sunriseMillis),
            RawWakt(PrayerName.SUNRISE, sunriseMillis, ishraqMillis),
            RawWakt(PrayerName.ISHRAQ, ishraqMillis, chashtMillis),
            RawWakt(PrayerName.CHASHT, chashtMillis, zawalStartMillis),
            RawWakt(PrayerName.ZAWAL, zawalStartMillis, noonMillis),
            RawWakt(PrayerName.DHUHR, noonMillis, asrMillis),
            RawWakt(PrayerName.ASR, asrMillis, preSunsetMillis),
            RawWakt(PrayerName.PRE_SUNSET, preSunsetMillis, sunsetMillis),
            RawWakt(PrayerName.MAGHRIB, sunsetMillis, ishaMillis),
            RawWakt(PrayerName.ISHA, ishaMillis, islamicMidnightMillis),
            RawWakt(PrayerName.ISLAMIC_MIDNIGHT, islamicMidnightMillis, nextFajrMillis)
        )

        var foundNext = false
        val items = mutableListOf<PrayerWaktItem>()

        for (raw in rawList) {
            val isPast = (raw.endMillis ?: raw.startMillis) < now
            val isCurrent = raw.endMillis != null && now >= raw.startMillis && now < raw.endMillis
            val isNext = !isPast && !isCurrent && !foundNext && raw.prayer.type == PrayerType.FARDH
            if (isNext) foundNext = true

            items.add(
                PrayerWaktItem(
                    prayer = raw.prayer,
                    startTimeFormatted = timeFormat.format(Date(raw.startMillis)),
                    endTimeFormatted = raw.endMillis?.let { timeFormat.format(Date(it)) },
                    startTimestampMillis = raw.startMillis,
                    endTimestampMillis = raw.endMillis,
                    isCurrentlyActive = isCurrent,
                    isNext = isNext,
                    isPast = isPast
                )
            )
        }

        return items
    }

    // Pomodoro Wakt Alignment Calculator
    fun getWaktPomodoroAnalysis(date: Date = Date()): WaktPomodoroAnalysis {
        val allWakts = calculateAllWaktItems(date)
        val now = System.currentTimeMillis()

        // Find current active fardh wakt (Fajr, Dhuhr, Asr, Maghrib, Isha)
        val fardhWakts = allWakts.filter { it.prayer.type == PrayerType.FARDH }
        val currentFardh = fardhWakts.find { it.isCurrentlyActive } ?: fardhWakts.first()

        val startMs = currentFardh.startTimestampMillis
        val endMs = currentFardh.endTimestampMillis ?: (startMs + 4 * 3600 * 1000L)

        val totalMinutes = ((endMs - startMs) / (60 * 1000L)).toInt().coerceAtLeast(1)
        val remainingMinutes = ((endMs - now) / (60 * 1000L)).toInt().coerceIn(0, totalMinutes)
        val progress = 1f - (remainingMinutes.toFloat() / totalMinutes)

        // Recommended Salah + Wudu + Dhikr rest time
        val recommendedSalahBreak = 15 // 15 mins for prayer rest

        // Each Pomodoro sprint + break takes 30 mins (25m focus + 5m rest)
        val availableTimeForWork = (remainingMinutes - recommendedSalahBreak).coerceAtLeast(0)
        val availablePomodoros = availableTimeForWork / 30

        return WaktPomodoroAnalysis(
            currentWaktName = currentFardh.prayer.displayName,
            currentWaktArabic = currentFardh.prayer.arabicName,
            startTimeFormatted = currentFardh.startTimeFormatted,
            endTimeFormatted = currentFardh.endTimeFormatted ?: "",
            remainingMinutesInWakt = remainingMinutes,
            totalWaktMinutes = totalMinutes,
            waktProgress = progress.coerceIn(0f, 1f),
            recommendedNamazBreakMinutes = recommendedSalahBreak,
            availablePomodorosCount = availablePomodoros
        )
    }

    private fun sunAngleDiff(angle: Double, declination: Double): Double {
        val latRad = degToRad(latitude)
        val decRad = degToRad(declination)
        val num = -sin(degToRad(angle)) - sin(latRad) * sin(decRad)
        val den = cos(latRad) * cos(decRad)
        val cosDiff = num / den
        if (cosDiff > 1.0 || cosDiff < -1.0) return 0.0
        return radToDeg(acos(cosDiff)) / 15.0
    }

    private fun toJulianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun degToRad(deg: Double) = deg * PI / 180.0
    private fun radToDeg(rad: Double) = rad * 180.0 / PI
    private fun fixAngle(angle: Double) = (angle % 360.0 + 360.0) % 360.0
    private fun fixHour(hour: Double) = (hour % 24.0 + 24.0) % 24.0
}

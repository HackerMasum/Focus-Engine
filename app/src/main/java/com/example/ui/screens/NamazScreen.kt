package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.namaz.CityPreset
import com.example.namaz.CityPresets
import com.example.namaz.JuristicMethod
import com.example.namaz.LocationHelper
import com.example.namaz.LocationService
import com.example.namaz.PrayerName
import com.example.namaz.PrayerNotificationReceiver
import com.example.namaz.PrayerTimeCalculator
import com.example.namaz.PrayerType
import com.example.namaz.PrayerWaktItem
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusCoral
import com.example.ui.theme.FocusCyan
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusPrimary
import com.example.ui.viewmodel.ProductivityViewModel
import com.example.ui.viewmodel.TimerMode
import java.util.Date

enum class WaktFilter(val label: String) {
    ALL("All Times"),
    FARDH("5 Fardh Wakts"),
    SUNNAH("Tahajjud & Sunnah"),
    FORBIDDEN("Forbidden Times")
}

@Composable
fun NamazScreen(
    viewModel: ProductivityViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Runtime permission for Notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Coordinates and Location state
    var selectedCityName by remember { mutableStateOf("Dhaka") }
    var selectedCountryName by remember { mutableStateOf("Bangladesh") }
    var latitude by remember { mutableStateOf(23.8103) }
    var longitude by remember { mutableStateOf(90.4125) }
    var timezoneHours by remember { mutableStateOf(6.0) }
    var juristicMethod by remember { mutableStateOf(JuristicMethod.HANAFI) }

    // Custom minute offsets
    var fajrOffset by remember { mutableIntStateOf(0) }
    var dhuhrOffset by remember { mutableIntStateOf(0) }
    var asrOffset by remember { mutableIntStateOf(0) }
    var maghribOffset by remember { mutableIntStateOf(0) }
    var ishaOffset by remember { mutableIntStateOf(0) }

    // Dialog toggles
    var showCityPicker by remember { mutableStateOf(false) }
    var showCustomOffsetsDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(WaktFilter.ALL) }

    // Today's Salah completion checklist
    val salahCompletedToday = remember {
        mutableStateMapOf(
            PrayerName.FAJR.name to false,
            PrayerName.DHUHR.name to false,
            PrayerName.ASR.name to false,
            PrayerName.MAGHRIB.name to false,
            PrayerName.ISHA.name to false
        )
    }

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var isLocatingGps by remember { mutableStateOf(false) }

    fun refreshGpsLocation() {
        isLocatingGps = true
        coroutineScope.launch {
            val detected = LocationService(context).getPreciseGpsLocation()
            isLocatingGps = false
            if (detected != null) {
                selectedCityName = detected.cityName
                selectedCountryName = detected.countryName
                latitude = detected.latitude
                longitude = detected.longitude
                timezoneHours = detected.timezoneHours
                Toast.makeText(context, "GPS Location Updated: ${detected.cityName}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not acquire GPS fix. Please ensure location is enabled.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            refreshGpsLocation()
        } else {
            Toast.makeText(context, "Location permission is required for auto GPS detection.", Toast.LENGTH_SHORT).show()
        }
    }

    val calculator = remember(
        latitude,
        longitude,
        timezoneHours,
        juristicMethod,
        fajrOffset,
        dhuhrOffset,
        asrOffset,
        maghribOffset,
        ishaOffset
    ) {
        PrayerTimeCalculator(
            latitude = latitude,
            longitude = longitude,
            timezone = timezoneHours,
            juristicMethod = juristicMethod,
            fajrOffsetMin = fajrOffset,
            dhuhrOffsetMin = dhuhrOffset,
            asrOffsetMin = asrOffset,
            maghribOffsetMin = maghribOffset,
            ishaOffsetMin = ishaOffset
        )
    }

    val allWakts = remember(calculator) {
        calculator.calculateAllWaktItems(Date())
    }

    val pomodoroAnalysis = remember(calculator) {
        calculator.getWaktPomodoroAnalysis(Date())
    }

    val nextFardh = allWakts.find { it.isNext }

    // Alarms toggle map
    val prayerAlarms = remember {
        mutableStateMapOf(
            PrayerName.FAJR.displayName to true,
            PrayerName.DHUHR.displayName to true,
            PrayerName.ASR.displayName to true,
            PrayerName.MAGHRIB.displayName to true,
            PrayerName.ISHA.displayName to true
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Namaz & Wakt Engine",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$selectedCityName, $selectedCountryName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Auto GPS button
                    IconButton(
                        onClick = {
                            if (!isLocatingGps) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape)
                            .testTag("auto_gps_button")
                    ) {
                        if (isLocatingGps) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Auto GPS",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // City Picker & Custom offsets
                    IconButton(
                        onClick = { showCityPicker = true },
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                            .testTag("city_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Select City",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showCustomOffsetsDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                            .testTag("custom_offsets_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Custom Offsets",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Pomodoro Wakt Alignment Hero Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F3B36), Color(0xFF115E59), Color(0xFF0D9488))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .testTag("pomodoro_namaz_alignment_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🕌", fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ACTIVE WAKT: ${pomodoroAnalysis.currentWaktName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                            Text(
                                text = "${pomodoroAnalysis.remainingMinutesInWakt / 60}h ${pomodoroAnalysis.remainingMinutesInWakt % 60}m left",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${pomodoroAnalysis.startTimeFormatted} ➔ ${pomodoroAnalysis.endTimeFormatted}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress through current Wakt
                    LinearProgressIndicator(
                        progress = { pomodoroAnalysis.waktProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF34D399),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pomodoro Capacity Insight
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🍅 Focus Capacity Before Next Wakt:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD1FAE5)
                                )
                                Text(
                                    text = "${pomodoroAnalysis.availablePomodorosCount} full Pomodoros (25m)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // 1-Tap Namaz Break Button
                            Button(
                                onClick = {
                                    viewModel.selectTimerMode(TimerMode.CUSTOM, 15)
                                    viewModel.startTimer()
                                    Toast.makeText(context, "15m Namaz Break timer started!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FocusEmerald),
                                modifier = Modifier.testTag("start_namaz_break_button")
                            ) {
                                Text("Namaz Break (15m)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Daily Salah Checklist (5 Fardh Prayers)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val count = salahCompletedToday.values.count { it }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Salah Checklist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$count / 5 Completed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (count == 5) FocusEmerald else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(PrayerName.FAJR, PrayerName.DHUHR, PrayerName.ASR, PrayerName.MAGHRIB, PrayerName.ISHA).forEach { prayer ->
                            val isDone = salahCompletedToday[prayer.name] ?: false
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    salahCompletedToday[prayer.name] = !isDone
                                }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDone) FocusEmerald else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(prayer.displayName.take(1), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = prayer.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = if (isDone) FocusEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Filter Chips (All / 5 Fardh / Sunnah / Forbidden)
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(WaktFilter.entries) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Filtered List of Islamic Wakts & Times
        val filteredWakts = when (selectedFilter) {
            WaktFilter.ALL -> allWakts
            WaktFilter.FARDH -> allWakts.filter { it.prayer.type == PrayerType.FARDH }
            WaktFilter.SUNNAH -> allWakts.filter { it.prayer.type == PrayerType.SUNNAH_NAFL }
            WaktFilter.FORBIDDEN -> allWakts.filter { it.prayer.type == PrayerType.FORBIDDEN_TIME }
        }

        items(filteredWakts) { waktItem ->
            val isAlarmEnabled = prayerAlarms[waktItem.prayer.displayName] ?: true

            val cardColor = when {
                waktItem.isCurrentlyActive -> FocusEmerald.copy(alpha = 0.15f)
                waktItem.prayer.type == PrayerType.FORBIDDEN_TIME -> FocusCoral.copy(alpha = 0.10f)
                waktItem.isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wakt_card_${waktItem.prayer.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = when (waktItem.prayer.type) {
                                PrayerType.FARDH -> FocusEmerald.copy(alpha = 0.2f)
                                PrayerType.FORBIDDEN_TIME -> FocusCoral.copy(alpha = 0.2f)
                                PrayerType.SUNNAH_NAFL -> FocusCyan.copy(alpha = 0.2f)
                            },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = waktItem.prayer.iconRes, fontSize = 20.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = waktItem.prayer.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = waktItem.prayer.arabicName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = waktItem.prayer.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1
                            )

                            if (waktItem.endTimeFormatted != null) {
                                Text(
                                    text = "Wakt Window: ${waktItem.startTimeFormatted} ➔ ${waktItem.endTimeFormatted}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (waktItem.isCurrentlyActive) FocusEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (waktItem.isCurrentlyActive) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = waktItem.startTimeFormatted,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                waktItem.isCurrentlyActive -> FocusEmerald
                                waktItem.prayer.type == PrayerType.FORBIDDEN_TIME -> FocusCoral
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )

                        if (waktItem.prayer.type == PrayerType.FARDH) {
                            IconButton(
                                onClick = {
                                    val newState = !isAlarmEnabled
                                    prayerAlarms[waktItem.prayer.displayName] = newState
                                    if (newState) {
                                        PrayerNotificationReceiver.schedulePrayerAlarm(
                                            context,
                                            waktItem.prayer.displayName,
                                            waktItem.startTimeFormatted,
                                            waktItem.startTimestampMillis
                                        )
                                    } else {
                                        PrayerNotificationReceiver.cancelPrayerAlarm(
                                            context,
                                            waktItem.prayer.displayName
                                        )
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAlarmEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                    contentDescription = "Toggle Alarm",
                                    tint = if (isAlarmEnabled) FocusEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // City Selection Dialog
    if (showCityPicker) {
        AlertDialog(
            onDismissRequest = { showCityPicker = false },
            title = { Text("Select City / Region") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(CityPresets.CITIES) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCityName = city.name
                                    selectedCountryName = city.country
                                    latitude = city.latitude
                                    longitude = city.longitude
                                    timezoneHours = city.timezoneHours
                                    showCityPicker = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = city.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = city.country,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selectedCityName == city.name) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Custom Time Offsets Dialog
    if (showCustomOffsetsDialog) {
        AlertDialog(
            onDismissRequest = { showCustomOffsetsDialog = false },
            title = { Text("Custom Prayer Offsets (Minutes)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Fine-tune prayer times (+/- minutes) to match your local mosque Azan/Jamat exactly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OffsetRow("Fajr", fajrOffset) { fajrOffset = it }
                    OffsetRow("Dhuhr", dhuhrOffset) { dhuhrOffset = it }
                    OffsetRow("Asr", asrOffset) { asrOffset = it }
                    OffsetRow("Maghrib", maghribOffset) { maghribOffset = it }
                    OffsetRow("Isha", ishaOffset) { ishaOffset = it }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Asr Juristic Calculation:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row {
                        FilterChip(
                            selected = juristicMethod == JuristicMethod.HANAFI,
                            onClick = { juristicMethod = JuristicMethod.HANAFI },
                            label = { Text("Hanafi") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = juristicMethod == JuristicMethod.STANDARD,
                            onClick = { juristicMethod = JuristicMethod.STANDARD },
                            label = { Text("Shafi/Standard") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCustomOffsetsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun OffsetRow(
    prayerName: String,
    currentOffset: Int,
    onOffsetChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = prayerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onOffsetChange(currentOffset - 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "${if (currentOffset > 0) "+$currentOffset" else currentOffset}m",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            IconButton(
                onClick = { onOffsetChange(currentOffset + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

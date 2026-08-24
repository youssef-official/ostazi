package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GroupEntity
import com.example.data.meetsOnDay
import com.example.data.getTimeSlotForDay
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.ui.components.PremiumIconTile
import com.example.ui.components.PremiumActionChip

val WEEK_DAYS = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")

@Composable
fun WeeklyTimetableScreen(viewModel: MainViewModel) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    fun label(ar: String, en: String) = if (appLanguage == "en") en else ar
    val regularGroups = remember(groups) { groups.filter { it.paymentType != "PER_SESSION" } }
    var selectedDayFilter by remember { mutableStateOf<String?>("الكل") }

    val daysToDisplay = if (selectedDayFilter == null || selectedDayFilter == "الكل") {
        WEEK_DAYS
    } else {
        listOf(selectedDayFilter!!)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumIconTile(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = AppStrings.weeklyTimetable(appLanguage),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = label("عرض مبسط ومباشر لمواعيد كافة المجموعات", "A clear overview of every group's schedule"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        var viewMode by remember { mutableStateOf("ADVANCED") }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PremiumActionChip(
                text = label("جدول تقليدي", "Classic"),
                icon = Icons.Outlined.GridView,
                selected = viewMode == "TRADITIONAL",
                onClick = { viewMode = "TRADITIONAL" },
                modifier = Modifier.weight(1f)
            )
            PremiumActionChip(
                text = label("عرض يومي", "Daily view"),
                icon = Icons.Outlined.ViewAgenda,
                selected = viewMode == "ADVANCED",
                onClick = { viewMode = "ADVANCED" },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        if (viewMode == "ADVANCED") {

        // Days Filter Scrollable Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedDayFilter == "الكل",
                    onClick = { selectedDayFilter = "الكل" },
                    label = { Text(label("الكل", "All"), fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SkyPrimary,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            items(WEEK_DAYS) { day ->
                val isToday = (day == viewModel.todayArabicDayName)
                val isSelected = (selectedDayFilter == day)
                val displayDay = if (appLanguage == "en") mapOf(
                    "السبت" to "Saturday", "الأحد" to "Sunday", "الإثنين" to "Monday",
                    "الثلاثاء" to "Tuesday", "الأربعاء" to "Wednesday", "الخميس" to "Thursday", "الجمعة" to "Friday"
                )[day] ?: day else day
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedDayFilter = day },
                    label = {
                        Text(
                            text = if (isToday) "$displayDay (${label("اليوم", "Today")})" else displayDay,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Timetable Cards List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(daysToDisplay) { dayName ->
                val isToday = (dayName == viewModel.todayArabicDayName)
                val dayGroups = regularGroups.filter { it.meetsOnDay(dayName) }.sortedBy { com.example.ui.util.TimeUtils.parseTime(it.getTimeSlotForDay(dayName)) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = .5f) else MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Day Card Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isToday) Color(0xFF2E7D32) else SkyPrimary,
                                    modifier = Modifier.size(10.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isToday) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "اليوم",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = if (dayGroups.isNotEmpty()) "${dayGroups.size} مجموعات" else "لا توجد حصص",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (dayGroups.isNotEmpty()) SkyPrimary else SkyOnSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (dayGroups.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = " لا توجد حصص مجدولة في هذا اليوم",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                dayGroups.forEach { group ->
                                    GroupScheduleCard(group = group, dayName = dayName)
                                }
                            }
                        }
                    }
                }
            }
        }
        } else {
            // Traditional Timetable Grid (24-Hour Grid starting from 12 AM)
            val standard24Hours = remember {
                listOf(
                    "12:00 ص", "01:00 ص", "02:00 ص", "03:00 ص", "04:00 ص", "05:00 ص",
                    "06:00 ص", "07:00 ص", "08:00 ص", "09:00 ص", "10:00 ص", "11:00 ص",
                    "12:00 م", "01:00 م", "02:00 م", "03:00 م", "04:00 م", "05:00 م",
                    "06:00 م", "07:00 م", "08:00 م", "09:00 م", "10:00 م", "11:00 م"
                )
            }

            val allGroupTimeSlots = remember(groups) {
                groups.flatMap { g ->
                    WEEK_DAYS.mapNotNull { d ->
                        if (g.meetsOnDay(d)) g.getTimeSlotForDay(d).trim().takeIf { it.isNotBlank() } else null
                    }
                }.distinct()
            }

            val displayTimes = remember(allGroupTimeSlots) {
                val extraSlots = allGroupTimeSlots.filter { slot ->
                    standard24Hours.none { hour ->
                        val s = slot.lowercase().replace(" ", "")
                        val h = hour.lowercase().replace(" ", "")
                        s == h || s.contains(h.take(2))
                    }
                }
                standard24Hours + extraSlots
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.background(SkyPrimary).padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("اليوم", modifier = Modifier.width(100.dp), color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    displayTimes.forEach { time ->
                        Text(time, modifier = Modifier.width(110.dp), color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
                
                // Days Rows (Right to Left / RTL Supported)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(WEEK_DAYS.size) { index ->
                        val dayName = WEEK_DAYS[index]
                        Row(
                            modifier = Modifier
                                .background(if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dayName, modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                            
                            displayTimes.forEach { time ->
                                val groupsForTime = regularGroups.filter { grp ->
                                    if (!grp.meetsOnDay(dayName)) return@filter false
                                    val slot = grp.getTimeSlotForDay(dayName).trim()
                                    slot == time || (slot.isNotEmpty() && (
                                        slot.replace(" ", "") == time.replace(" ", "") ||
                                        (slot.contains("ص") == time.contains("ص") && slot.contains("م") == time.contains("م") &&
                                         slot.filter { it.isDigit() } == time.filter { it.isDigit() })
                                    ))
                                }
                                if (groupsForTime.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier.width(110.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        groupsForTime.forEach { grp ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(SkyPrimaryContainer, RoundedCornerShape(6.dp))
                                                    .padding(6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(grp.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SkyPrimary, textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                } else {
                                    Text("-", modifier = Modifier.width(110.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        HorizontalDivider(color = SkyOutline)
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupScheduleCard(group: GroupEntity, dayName: String = "") {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .62f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = group.subject,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (group.groupType.isNotEmpty()) {
                        val typeText = if (group.groupType == "ONLINE") "أونلاين" else "سنتر"
                        Text(
                            text = " • $typeText",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Time Slot Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SkyPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = group.getTimeSlotForDay(dayName),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

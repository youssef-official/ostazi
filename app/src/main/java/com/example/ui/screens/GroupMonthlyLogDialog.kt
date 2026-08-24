package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AttendanceRecordEntity
import com.example.data.ExamRecordEntity
import com.example.data.GroupEntity
import com.example.data.StudentEntity
import com.example.data.meetsOnDay
import com.example.ui.MainViewModel
import com.example.ui.components.GroupAttendancePdfDialog
import com.example.ui.components.PremiumDialogDirectionGuard
import java.text.SimpleDateFormat
import java.util.*

val ARABIC_MONTHS = listOf(
    "يناير" to 0, "فبراير" to 1, "مارس" to 2, "أبريل" to 3,
    "مايو" to 4, "يونيو" to 5, "يوليو" to 6, "أغسطس" to 7,
    "سبتمبر" to 8, "أكتوبر" to 9, "نوفمبر" to 10, "ديسمبر" to 11
)

fun getArabicDayName(calDayOfWeek: Int): String {
    return when (calDayOfWeek) {
        Calendar.SATURDAY -> "السبت"
        Calendar.SUNDAY -> "الأحد"
        Calendar.MONDAY -> "الاثنين"
        Calendar.TUESDAY -> "الثلاثاء"
        Calendar.WEDNESDAY -> "الأربعاء"
        Calendar.THURSDAY -> "الخميس"
        Calendar.FRIDAY -> "الجمعة"
        else -> ""
    }
}

data class SessionDateItem(
    val dayName: String,
    val dateString: String, // "yyyy-MM-dd"
    val displayDate: String // "dd/MM/yyyy"
)

fun calculateGroupSessionsBetweenDates(group: GroupEntity, startDate: Date, endDate: Date): List<SessionDateItem> {
    val result = mutableListOf<SessionDateItem>()
    val cal = Calendar.getInstance().apply {
        time = startDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val endCal = Calendar.getInstance().apply {
        time = endDate
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    val sdfStorage = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    val sdfDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

    while (!cal.after(endCal)) {
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dayName = getArabicDayName(dayOfWeek)
        if (group.meetsOnDay(dayName)) {
            val dateStr = sdfStorage.format(cal.time)
            val dispStr = sdfDisplay.format(cal.time)
            result.add(SessionDateItem(dayName, dateStr, dispStr))
        }
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return result
}

fun calculateGroupSessionsInMonth(group: GroupEntity, year: Int, monthIndex: Int): List<SessionDateItem> {
    val cal = GregorianCalendar(year, monthIndex, 1)
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startCal = GregorianCalendar(year, monthIndex, 1)
    val endCal = GregorianCalendar(year, monthIndex, maxDay)
    return calculateGroupSessionsBetweenDates(group, startCal.time, endCal.time)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMonthlyLogDialog(
    group: GroupEntity,
    students: List<StudentEntity>,
    viewModel: MainViewModel,
    allExams: List<ExamRecordEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sdfStorage = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    val sdfDisplay = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH) }

    // Start & End Date State for the Group Schedule
    var startDate by remember {
        val saved = viewModel.savedLogStartDate.value
        mutableStateOf(
            if (saved > 0) java.util.Date(saved) else Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }.time
        )
    }

    var endDate by remember {
        val saved = viewModel.savedLogEndDate.value
        mutableStateOf(
            if (saved > 0) java.util.Date(saved) else Calendar.getInstance().apply {
                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, maxDay)
            }.time
        )
    }

    LaunchedEffect(startDate, endDate) {
        viewModel.saveLogDateRange(startDate.time, endDate.time)
    }

    val startDateStr = remember(startDate) { sdfStorage.format(startDate) }
    val endDateStr = remember(endDate) { sdfStorage.format(endDate) }
    val startDisplayStr = remember(startDate) { sdfDisplay.format(startDate) }
    val endDisplayStr = remember(endDate) { sdfDisplay.format(endDate) }

    // Attendance Flow for Date Range
    val rangeAttendanceFlow = remember(startDateStr, endDateStr) {
        viewModel.getAttendanceBetweenDatesFlow(startDateStr, endDateStr)
    }
    val rangeAttendanceRecords by rangeAttendanceFlow.collectAsState(initial = emptyList())

    val sessionDates = remember(group, startDate, endDate) {
        calculateGroupSessionsBetweenDates(group, startDate, endDate).sortedBy { it.dateString }
    }

    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()) }

    var selectedDateItem by remember {
        mutableStateOf<SessionDateItem?>(null)
    }

    LaunchedEffect(sessionDates) {
        if (sessionDates.isNotEmpty()) {
            if (selectedDateItem == null || sessionDates.none { it.dateString == selectedDateItem?.dateString }) {
                val match = sessionDates.find { it.dateString == todayDateStr }
                    ?: sessionDates.lastOrNull { it.dateString <= todayDateStr }
                    ?: sessionDates.firstOrNull()
                selectedDateItem = match
            }
        } else {
            selectedDateItem = null
        }
    }

    val selectedDateStr = selectedDateItem?.dateString ?: ""
    val attendanceFlow = remember(selectedDateStr) {
        if (selectedDateStr.isNotBlank()) viewModel.getAttendanceForDateFlow(selectedDateStr)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }
    val attendanceRecords by attendanceFlow.collectAsState(initial = emptyList())

    var showPdfReportDialog by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<StudentEntity?>(null) }

    fun openDatePicker(initialDate: Date, onSelected: (Date) -> Unit) {
        val cal = Calendar.getInstance().apply { time = initialDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                onSelected(picked.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PremiumDialogDirectionGuard()
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Top Header Row with perfectly aligned matching-height action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .height(36.dp)
                                .clickable {
                                    showPdfReportDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.PictureAsPdf,
                                    contentDescription = "كشف حضور PDF",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "PDF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Right Header Title
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "سجل المتابعة الشهري",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedDateItem != null) {
                                "${group.name} • ${selectedDateItem?.dayName} ${selectedDateItem?.displayDate}"
                            } else {
                                "${group.name} • ${group.subject}"
                            },
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Schedule & Calendar Period Card (Allows selecting start & end dates from Calendar)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEDE9FE)
                            ) {
                                Text(
                                    text = "${sessionDates.size} حصة في الفترة",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5B21B6),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "تحديد مواعيد وحصص المجموعة من التقويم",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Start & End Date Pickers Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Start Date Picker Box
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        openDatePicker(startDate) { pickedDate ->
                                            startDate = pickedDate
                                            if (pickedDate.after(endDate)) {
                                                endDate = pickedDate
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("من تاريخ (البداية)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                        Text(startDisplayStr, fontSize = 13.sp, color = Color(0xFF1E1035), fontWeight = FontWeight.Bold)
                                    }
                                    Icon(Icons.Outlined.CalendarToday, contentDescription = "تحديد تاريخ البداية", tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                                }
                            }

                            // End Date Picker Box
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        openDatePicker(endDate) { pickedDate ->
                                            endDate = pickedDate
                                            if (pickedDate.before(startDate)) {
                                                startDate = pickedDate
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("إلى تاريخ (الانتهاء)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                        Text(endDisplayStr, fontSize = 13.sp, color = Color(0xFF1E1035), fontWeight = FontWeight.Bold)
                                    }
                                    Icon(Icons.Outlined.CalendarToday, contentDescription = "تحديد تاريخ الانتهاء", tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Navigation Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Current Month Chip
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val cal = Calendar.getInstance()
                                        cal.set(Calendar.DAY_OF_MONTH, 1)
                                        startDate = cal.time
                                        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                        cal.set(Calendar.DAY_OF_MONTH, maxDay)
                                        endDate = cal.time
                                    }
                            ) {
                                Text(
                                    text = "الشهر الحالي",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E40AF),
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Next Month Chip
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val cal = Calendar.getInstance()
                                        cal.add(Calendar.MONTH, 1)
                                        cal.set(Calendar.DAY_OF_MONTH, 1)
                                        startDate = cal.time
                                        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                        cal.set(Calendar.DAY_OF_MONTH, maxDay)
                                        endDate = cal.time
                                    }
                            ) {
                                Text(
                                    text = "الشهر القادم",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Previous Month Chip
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val cal = Calendar.getInstance()
                                        cal.add(Calendar.MONTH, -1)
                                        cal.set(Calendar.DAY_OF_MONTH, 1)
                                        startDate = cal.time
                                        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                        cal.set(Calendar.DAY_OF_MONTH, maxDay)
                                        endDate = cal.time
                                    }
                            ) {
                                Text(
                                    text = "الشهر السابق",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Grid of Session Dates in that Range
                        if (sessionDates.isEmpty()) {
                            val activeDays = listOfNotNull(group.day1, group.day2, group.day3, group.day4, group.day5, group.day6, group.day7)
                                .filter { it.isNotBlank() && it != "بدون" }
                                .joinToString("، ")
                            Text(
                                text = "لا توجد حصص للمجموعة في الفترة المختارة. تأكد من أن الفترة تتضمن أيام الحصص ($activeDays).",
                                color = Color(0xFFB45309),
                                fontSize = 11.5.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 140.dp)
                            ) {
                                items(sessionDates) { dateItem ->
                                    val isSelected = selectedDateItem?.dateString == dateItem.dateString
                                    val isToday = dateItem.dateString == todayDateStr
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (isToday) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.2.dp, if (isSelected) Color(0xFF2563EB) else if (isToday) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.clickable { selectedDateItem = dateItem }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = dateItem.dayName,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color(0xFF1E40AF) else if (isToday) Color(0xFF166534) else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isToday) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFDCFCE7)
                                                    ) {
                                                        Text(
                                                            text = "اليوم",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFF166534),
                                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = dateItem.displayDate,
                                                fontSize = 10.sp,
                                                color = if (isSelected) Color(0xFF2563EB) else if (isToday) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Batch Actions for Selected Date
                if (selectedDateItem != null && students.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val sIds = students.map { it.id }
                                    viewModel.batchSetAttendance(sIds, selectedDateItem!!.dateString, attendanceStatus = "حضر")
                                    Toast.makeText(context, "تم تحضير جميع الطلاب لهذا اليوم ", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                text = "تحضير الكل ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val sIds = students.map { it.id }
                                    viewModel.batchSetAttendance(sIds, selectedDateItem!!.dateString, homeworkStatus = "كتب")
                                    Toast.makeText(context, "تم تسجيل الواجب للكل ", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                text = "واجب الكل ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E40AF),
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, Color(0xFFE9D5FF)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val sIds = students.map { it.id }
                                    viewModel.batchSetAttendance(sIds, selectedDateItem!!.dateString, interactionStatus = "ممتاز")
                                    Toast.makeText(context, "تم تسجيل تفاعل ممتاز للكل ", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                text = "ممتاز للكل ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6B21A8),
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Students List with 5 Columns
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (students.isEmpty()) {
                        item {
                            Text(
                                text = "لا يوجد طلاب مسجلين في هذه المجموعة.",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(students) { student ->
                            val attRec = attendanceRecords.find { it.studentId == student.id }
                            val exRec = allExams.find { it.studentId == student.id }

                            val attText = attRec?.attendanceStatus ?: "غير مسجل"
                            val hwText = attRec?.homeworkStatus ?: "غير مسجل"
                            val recText = attRec?.recitationGrade?.ifBlank { null } ?: "غير مسجل"
                            val interText = attRec?.interactionStatus ?: "غير مسجل"
                            val examText = exRec?.exam1?.let { "$it/100" } ?: "غير مسجل"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { studentToEdit = student },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = student.fullName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            softWrap = false
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 5 Columns: الحضور | الواجب | التسميع | الامتحانات | التفاعل
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // الحضور
                                        StatusBlock(
                                            modifier = Modifier.weight(1f),
                                            title = "الحضور",
                                            value = attText,
                                            isRegistered = attText != "غير مسجل",
                                            activeColor = when(attText) {
                                                "حضر" -> Color(0xFF10B981)
                                                "غائب" -> Color(0xFFEF4444)
                                                else -> Color(0xFFF59E0B)
                                            }
                                        )

                                        // الواجب
                                        StatusBlock(
                                            modifier = Modifier.weight(1f),
                                            title = "الواجب",
                                            value = hwText,
                                            isRegistered = hwText != "غير مسجل",
                                            activeColor = Color(0xFF0284C7)
                                        )

                                        // التسميع
                                        StatusBlock(
                                            modifier = Modifier.weight(1f),
                                            title = "التسميع",
                                            value = recText,
                                            isRegistered = recText != "غير مسجل",
                                            activeColor = Color(0xFF4F46E5)
                                        )

                                        // الامتحانات
                                        StatusBlock(
                                            modifier = Modifier.weight(1f),
                                            title = "الامتحانات",
                                            value = examText,
                                            isRegistered = examText != "غير مسجل",
                                            activeColor = Color(0xFF9333EA)
                                        )

                                        // التفاعل
                                        StatusBlock(
                                            modifier = Modifier.weight(1f),
                                            title = "التفاعل",
                                            value = interText,
                                            isRegistered = interText != "غير مسجل",
                                            activeColor = Color(0xFF059669)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Edit Student for the Selected Date
    studentToEdit?.let { student ->
        val dateStr = selectedDateItem?.dateString ?: ""
        val currentRec = attendanceRecords.find { it.studentId == student.id }
        var att by remember { mutableStateOf(currentRec?.attendanceStatus ?: "حضر") }
        var hw by remember { mutableStateOf(currentRec?.homeworkStatus ?: "كتب") }
        var inter by remember { mutableStateOf(currentRec?.interactionStatus ?: "ممتاز") }
        var rec by remember { mutableStateOf(currentRec?.recitationGrade ?: "10/10") }
        var note by remember { mutableStateOf(currentRec?.notes ?: "") }

        com.example.ui.components.PremiumAlertDialog(
            onDismissRequest = { studentToEdit = null },
            title = {
                Text(
                    text = "تقييم: ${student.fullName} (${selectedDateItem?.dayName} ${selectedDateItem?.displayDate})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("الحضور:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("حضر", "غائب", "متأخر", "تم الغاء حصة اليوم").forEach { st ->
                            val isSel = att == st
                            val bgColor = if (isSel) {
                                when (st) {
                                    "حضر" -> Color(0xFF10B981)
                                    "غائب" -> Color(0xFFEF4444)
                                    "متأخر" -> Color(0xFFF59E0B)
                                    else -> Color(0xFFDC2626)
                                }
                            } else MaterialTheme.colorScheme.surfaceVariant

                            val txt = when(st) {
                                "حضر" -> "حضر"
                                "متأخر" -> "متأخر"
                                else -> "ملغية"
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = bgColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { att = st }
                            ) {
                                Text(
                                    text = txt,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.2.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Text("الواجب:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("كتب", "لم يكتب", "متأخر", "لا يوجد").forEach { h ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (hw == h) Color(0xFF0284C7) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { hw = h }
                            ) {
                                Text(h, color = if (hw == h) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp), textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Text("التفاعل:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ممتاز", "جيد", "ضعيف").forEach { i ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (inter == i) Color(0xFF059669) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { inter = i }
                            ) {
                                Text(i, color = if (inter == i) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp), textAlign = TextAlign.Center)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = rec,
                        onValueChange = { rec = it },
                        label = { Text("درجة التسميع (مثال: 10/10)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setAttendanceAndHomework(
                            studentId = student.id,
                            date = dateStr,
                            attendanceStatus = att,
                            homeworkStatus = hw,
                            interactionStatus = inter,
                            recitationGrade = rec,
                            notes = note
                        )
                        studentToEdit = null
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToEdit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showPdfReportDialog) {
        GroupAttendancePdfDialog(
            group = group,
            students = students,
            viewModel = viewModel,
            initialStartDate = startDate,
            initialEndDate = endDate,
            onDismiss = { showPdfReportDialog = false }
        )
    }
}

@Composable
fun StatusBlock(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    isRegistered: Boolean,
    activeColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isRegistered) activeColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (isRegistered) activeColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = if (isRegistered) FontWeight.Bold else FontWeight.Normal,
                color = if (isRegistered) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

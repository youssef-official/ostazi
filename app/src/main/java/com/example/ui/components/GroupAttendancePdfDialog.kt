package com.example.ui.components

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AttendanceRecordEntity
import com.example.data.GroupEntity
import com.example.data.StudentEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.ARABIC_MONTHS
import com.example.ui.screens.SessionDateItem
import com.example.ui.screens.calculateGroupSessionsBetweenDates
import com.example.utils.PdfReportGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupAttendancePdfDialog(
    group: GroupEntity,
    students: List<StudentEntity>,
    viewModel: MainViewModel,
    initialStartDate: Date? = null,
    initialEndDate: Date? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sdfDisplay = remember { SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH) }
    val sdfStorage = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }

    // Start & End Date State
    var startDate by remember {
        val saved = viewModel.savedLogStartDate.value
        mutableStateOf(
            initialStartDate ?: if (saved > 0) Date(saved) else Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }.time
        )
    }

    var endDate by remember {
        val saved = viewModel.savedLogEndDate.value
        mutableStateOf(
            initialEndDate ?: if (saved > 0) Date(saved) else Calendar.getInstance().apply {
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

    val periodSubtitle = remember(startDisplayStr, endDisplayStr) {
        "من $startDisplayStr إلى $endDisplayStr"
    }

    // Month Selector States
    var selectedMonthYearCal by remember(startDate) {
        mutableStateOf(Calendar.getInstance().apply { time = startDate })
    }
    var expandedMonthDropdown by remember { mutableStateOf(false) }

    // Dynamic session dates for this group in selected range
    val sessionDates = remember(group, startDate, endDate) {
        calculateGroupSessionsBetweenDates(group, startDate, endDate).sortedBy { it.dateString }
    }

    // Attendance Flow for selected range
    val attendanceFlow = remember(startDateStr, endDateStr) {
        viewModel.getAttendanceBetweenDatesFlow(startDateStr, endDateStr)
    }
    val attendanceRecords by attendanceFlow.collectAsState(initial = emptyList())

    var generatedFile by remember { mutableStateOf<File?>(null) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // Re-generate PDF when parameters change
    LaunchedEffect(group, students, sessionDates, attendanceRecords, periodSubtitle) {
        isGeneratingPdf = true
        generatedFile = PdfReportGenerator.generateGroupAttendancePdf(
            context = context,
            group = group,
            students = students,
            sessionDates = sessionDates,
            attendanceRecords = attendanceRecords,
            periodSubtitle = periodSubtitle
        )
        isGeneratingPdf = false
    }

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
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar for PDF View (Matching Professional Document Viewer)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "تقرير-حضور-${group.name.replace(" ", "-")}.pdf",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$periodSubtitle • ${sessionDates.size} حصة",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Top bar was empty of buttons
                        }
                    }
                }

                // Dedicated Calendar Range Selector Card at the Top
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = " تحديد شهر وتاريخ كشف الحضور:",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEDE9FE)
                            ) {
                                Text(
                                    text = "${sessionDates.size} حصة محددة",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5B21B6),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Month & Year Direct Selector Bar
                        val currentMonthIndex = selectedMonthYearCal.get(Calendar.MONTH)
                        val currentYear = selectedMonthYearCal.get(Calendar.YEAR)
                        val currentMonthName = ARABIC_MONTHS.find { it.second == currentMonthIndex }?.first ?: "الشهر الحالي"

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Month Selector Dropdown Button
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEFF6FF),
                                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                        modifier = Modifier.clickable { expandedMonthDropdown = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Outlined.DateRange, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                            Text(
                                                text = "اختيار الشهر: $currentMonthName $currentYear ▾",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1D4ED8)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = expandedMonthDropdown,
                                        onDismissRequest = { expandedMonthDropdown = false },
                                        modifier = Modifier.heightIn(max = 300.dp)
                                    ) {
                                        ARABIC_MONTHS.forEach { (mName, mIdx) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "$mName $currentYear",
                                                        fontWeight = if (mIdx == currentMonthIndex) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (mIdx == currentMonthIndex) Color(0xFF2563EB) else Color(0xFF0F172A)
                                                    )
                                                },
                                                onClick = {
                                                    val newCal = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, currentYear)
                                                        set(Calendar.MONTH, mIdx)
                                                        set(Calendar.DAY_OF_MONTH, 1)
                                                    }
                                                    startDate = newCal.time
                                                    newCal.set(Calendar.DAY_OF_MONTH, newCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                                                    endDate = newCal.time
                                                    selectedMonthYearCal = newCal
                                                    expandedMonthDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Year Navigation Stepper Buttons
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val newCal = Calendar.getInstance().apply {
                                                time = startDate
                                                add(Calendar.MONTH, -1)
                                                set(Calendar.DAY_OF_MONTH, 1)
                                            }
                                            startDate = newCal.time
                                            newCal.set(Calendar.DAY_OF_MONTH, newCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                                            endDate = newCal.time
                                            selectedMonthYearCal = newCal
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "الشهر السابق", tint = Color(0xFF475569), modifier = Modifier.size(13.dp))
                                    }

                                    Text(
                                        text = "$currentYear",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF334155)
                                    )

                                    IconButton(
                                        onClick = {
                                            val newCal = Calendar.getInstance().apply {
                                                time = startDate
                                                add(Calendar.MONTH, 1)
                                                set(Calendar.DAY_OF_MONTH, 1)
                                            }
                                            startDate = newCal.time
                                            newCal.set(Calendar.DAY_OF_MONTH, newCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                                            endDate = newCal.time
                                            selectedMonthYearCal = newCal
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "الشهر التالي", tint = Color(0xFF475569), modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Calendar Pickers for Start & End Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                color = Color(0xFFF9FAFB),
                                border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("تاريخ البداية (من)", fontSize = 10.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                                        Text(startDisplayStr, fontSize = 13.sp, color = Color(0xFF1E1035), fontWeight = FontWeight.Bold)
                                    }
                                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "تحديد تاريخ البداية", tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
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
                                color = Color(0xFFF9FAFB),
                                border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("تاريخ الانتهاء (إلى)", fontSize = 10.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                                        Text(endDisplayStr, fontSize = 13.sp, color = Color(0xFF1E1035), fontWeight = FontWeight.Bold)
                                    }
                                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "تحديد تاريخ الانتهاء", tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
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
                                color = Color(0xFFEFF6FF),
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
                                color = Color(0xFFF9FAFB),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
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
                                    color = Color(0xFF4B5563),
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Previous Month Chip
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF9FAFB),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
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
                                    color = Color(0xFF4B5563),
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Printable A4 Page View (Real-time synced)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Header banner: Deep Purple
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E1035)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp, horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "تقرير كشف الحضور للمجموعات",
                                        color = Color(0xFFB3A5D4),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "كشف حضور ${group.name}",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${group.subject} • ${students.size} طالب • ${sessionDates.size} أيام مسجلة ($periodSubtitle)",
                                        color = Color(0xFFDDD6FE),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Attendance Table with horizontal scroll if dates exceed screen
                            val horizontalScrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(horizontalScrollState)
                            ) {
                                // Table Header
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEDE9FE)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Number col
                                        Text(
                                            text = "#",
                                            modifier = Modifier.width(30.dp),
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF4C1D95)
                                        )

                                        // Name col
                                        Text(
                                            text = "اسم الطالب",
                                            modifier = Modifier.width(130.dp),
                                            textAlign = TextAlign.Right,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF4C1D95)
                                        )

                                        // Dates columns
                                        val displayDates = sessionDates
                                        displayDates.forEach { dateItem ->
                                            val shortDate = try {
                                                val parts = dateItem.dateString.split("-")
                                                if (parts.size == 3) "${parts[1]}/${parts[2]}" else dateItem.displayDate
                                            } catch (e: Exception) {
                                                dateItem.displayDate
                                            }
                                            Column(
                                                modifier = Modifier.width(54.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = shortDate,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF3730A3)
                                                )
                                                Text(
                                                    text = dateItem.dayName,
                                                    fontSize = 8.5.sp,
                                                    color = Color(0xFF6B7280)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Table Student Rows
                                if (students.isEmpty()) {
                                    Text(
                                        text = "لا يوجد طلاب مسجلين في هذه المجموعة",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        textAlign = TextAlign.Center,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 12.sp
                                    )
                                } else if (sessionDates.isEmpty()) {
                                    Text(
                                        text = "لا توجد حصص محددة للمجموعة في الفترة المختارة. يرجى اختيار فترة تتضمن أيام حصص المجموعة.",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        textAlign = TextAlign.Center,
                                        color = Color(0xFFB45309),
                                        fontSize = 12.sp
                                    )
                                } else {
                                    students.forEachIndexed { index, student ->
                                        val rowBg = if (index % 2 == 1) Color(0xFFF9FAFB) else Color.White
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = rowBg
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 7.dp, horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // #
                                                Text(
                                                    text = "${index + 1}",
                                                    modifier = Modifier.width(30.dp),
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF4B5563)
                                                )

                                                // Student Name
                                                Text(
                                                    text = student.fullName,
                                                    modifier = Modifier.width(130.dp),
                                                    textAlign = TextAlign.Right,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF111827),
                                                    maxLines = 1
                                                )

                                                // Attendance per date
                                                val displayDatesRow = sessionDates
                                                displayDatesRow.forEach { dateItem ->
                                                    val record = attendanceRecords.find {
                                                        it.studentId == student.id && it.date == dateItem.dateString
                                                    }
                                                    val status = record?.attendanceStatus

                                                    Box(
                                                        modifier = Modifier.width(54.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        val cleanStatus = status?.trim() ?: ""
                                                        when (cleanStatus) {
                                                            "حضر", "حاضر", "PRESENT" -> {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = Color(0xFFDCFCE7)
                                                                ) {
                                                                    Text(
                                                                        text = "حاضر",
                                                                        color = Color(0xFF15803D),
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                            "متأخر", "LATE" -> {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = Color(0xFFFEF3C7)
                                                                ) {
                                                                    Text(
                                                                        text = "متأخر",
                                                                        color = Color(0xFFB45309),
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                            "غائب", "غ", "ABSENT" -> {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = Color(0xFFFEE2E2)
                                                                ) {
                                                                    Text(
                                                                        text = "غائب",
                                                                        color = Color(0xFFB91C1C),
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                            "مستأذن", "بعذر", "EXCUSED" -> {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = Color(0xFFEFF6FF)
                                                                ) {
                                                                    Text(
                                                                        text = "بعذر",
                                                                        color = Color(0xFF1D4ED8),
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                            else -> {
                                                                Text(
                                                                    text = "—",
                                                                    color = Color(0xFF9CA3AF),
                                                                    fontSize = 11.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Legend
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = " حاضر / متأخر",
                                    color = Color(0xFF059669),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = " غائب",
                                    color = Color(0xFFDC2626),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "- لم يسجل",
                                    color = Color(0xFF6B7280),
                                    fontSize = 10.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Footer Bottom
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "صفحة 1 من 1",
                                    fontSize = 9.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                                Text(
                                    text = "أستاذي • تقرير كشف حضور جاهز للطباعة والمشاركة",
                                    fontSize = 9.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }
                    }
                }

                // Bottom Action Buttons Row
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Share
                        Button(
                            onClick = {
                                val file = generatedFile ?: PdfReportGenerator.generateGroupAttendancePdf(
                                    context, group, students, sessionDates, attendanceRecords, periodSubtitle
                                )
                                if (file != null) {
                                    PdfReportGenerator.sharePdf(context, file, "كشف حضور ${group.name} ($periodSubtitle)")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Print
                        Button(
                            onClick = {
                                val file = generatedFile ?: PdfReportGenerator.generateGroupAttendancePdf(
                                    context, group, students, sessionDates, attendanceRecords, periodSubtitle
                                )
                                if (file != null) {
                                    PdfReportGenerator.printPdf(context, file, "كشف حضور ${group.name}")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1035))
                        ) {
                            Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Open
                        Button(
                            onClick = {
                                val file = generatedFile ?: PdfReportGenerator.generateGroupAttendancePdf(
                                    context, group, students, sessionDates, attendanceRecords, periodSubtitle
                                )
                                if (file != null) {
                                    PdfReportGenerator.openPdf(context, file)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("فتح", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.request.ImageRequest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.GroupEntity
import com.example.data.StudentEntity
import com.example.data.PaymentRecordEntity
import com.example.data.meetsOnDay
import com.example.data.getTimeSlotForDay
import com.example.ui.MainViewModel
import com.example.ui.components.FinancialWaterBarCard
import com.example.ui.components.DashboardFinancialWaterLineCard
import com.example.ui.components.BarcodeScannerModal
import com.example.ui.theme.*
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.util.Calendar
import java.util.Locale

enum class ClassTimingState {
    ONGOING,    // جارية الآن
    UPCOMING,   // قادمة
    FINISHED    // انتهت
}

data class ClassTimingInfo(
    val state: ClassTimingState,
    val isNextUp: Boolean,
    val timeRemainingText: String,
    val badgeLabel: String,
    val startMinutes: Int,
    val endMinutes: Int
)

fun parseTimeToMinutes(timeSlot: String): Pair<Int, Int>? {
    return try {
        val isPm = timeSlot.contains("م")
        val clean = timeSlot.replace("م", "").replace("ص", "").trim()
        val parts = clean.split(":")
        var hour = parts[0].trim().toInt()
        val min = parts[1].trim().toInt()

        if (isPm && hour < 12) hour += 12
        if (!isPm && timeSlot.contains("ص") && hour == 12) hour = 0

        Pair(hour, min)
    } catch (e: Exception) {
        null
    }
}

fun calculateClassTiming(timeSlot: String, isAssignedNextUp: Boolean): ClassTimingInfo {
    val parsed = parseTimeToMinutes(timeSlot)
    if (parsed == null) {
        return ClassTimingInfo(
            state = ClassTimingState.UPCOMING,
            isNextUp = isAssignedNextUp,
            timeRemainingText = "مجدولة لليوم",
            badgeLabel = if (isAssignedNextUp) "الحصة التالية " else "مجدولة",
            startMinutes = 0,
            endMinutes = 0
        )
    }

    val (hour, min) = parsed
    val startMinutes = hour * 60 + min
    val endMinutes = startMinutes + 60 // Default 60 min session

    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

    return when {
        nowMinutes in startMinutes until endMinutes -> {
            val minsLeft = endMinutes - nowMinutes
            ClassTimingInfo(
                state = ClassTimingState.ONGOING,
                isNextUp = true,
                timeRemainingText = "جارية الآن  (متبقي $minsLeft دقيقة)",
                badgeLabel = "جارية الآن",
                startMinutes = startMinutes,
                endMinutes = endMinutes
            )
        }
        nowMinutes < startMinutes -> {
            val diff = startMinutes - nowMinutes
            val diffText = if (diff < 60) {
                "متبقي على الحصة: $diff دقيقة"
            } else {
                val h = diff / 60
                val m = diff % 60
                when {
                    m == 0 && h == 1 -> "متبقي على الحصة: ساعة واحدة"
                    m == 0 && h == 2 -> "متبقي على الحصة: ساعتان"
                    m == 0 -> "متبقي على الحصة: $h ساعات"
                    h == 1 -> "متبقي على الحصة: ساعة و $m دقيقة"
                    h == 2 -> "متبقي على الحصة: ساعتان و $m دقيقة"
                    else -> "متبقي على الحصة: $h ساعات و $m دقيقة"
                }
            }
            ClassTimingInfo(
                state = ClassTimingState.UPCOMING,
                isNextUp = isAssignedNextUp,
                timeRemainingText = diffText,
                badgeLabel = if (isAssignedNextUp) "الحصة التي عليها الدور " else "قادمة",
                startMinutes = startMinutes,
                endMinutes = endMinutes
            )
        }
        else -> {
            ClassTimingInfo(
                state = ClassTimingState.FINISHED,
                isNextUp = false,
                timeRemainingText = "انتهت حصة اليوم ",
                badgeLabel = "انتهت",
                startMinutes = startMinutes,
                endMinutes = endMinutes
            )
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToGroups: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToTimetable: () -> Unit = {},
    onNavigateToPayments: () -> Unit = {},
    onOpenBackupDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val subscriptionManager = remember { com.example.SubscriptionManager(context) }
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val teacherName by viewModel.teacherName.collectAsStateWithLifecycle()
    val teacherSubject by viewModel.teacherSubject.collectAsStateWithLifecycle()
    val teacherImageUri by viewModel.teacherImageUri.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    fun label(ar: String, en: String) = if (appLanguage == "en") en else ar

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showBarcodeScannerModal by remember { mutableStateOf(false) }
    var groupForQuickAttendance by remember { mutableStateOf<GroupEntity?>(null) }
    var showManualReportDialog by remember { mutableStateOf(false) }

    val attendanceList by viewModel.attendanceForToday.collectAsStateWithLifecycle()

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val scannedData = result.contents
            val studentIdStr = scannedData.removePrefix("STUDENT_")
            val studentId = studentIdStr.toIntOrNull()
            
            if (studentId != null) {
                val student = allStudents.find { it.id == studentId }
                if (student != null) {
                    val group = groups.find { it.id == student.groupId }
                    val currentAttendance = attendanceList.find { it.studentId == student.id }
                    val currentHw = currentAttendance?.homeworkStatus ?: "كتب الواجب"
                    val currentRec = currentAttendance?.recitationStatus ?: "ممتاز"
                    viewModel.setAttendanceAndHomework(
                        studentId = student.id,
                        attendanceStatus = "حضر",
                        homeworkStatus = currentHw,
                        recitationStatus = currentRec
                    )
                    Toast.makeText(
                        context,
                        "تم تسجيل حضور الطالب ${student.fullName} في ${group?.name ?: ""} بنجاح! ",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(context, "لم يتم العثور على الطالب في قاعدة البيانات!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "كود QR غير صالح", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Monthly Financial Statistics for Animated Water Bar
    val cal = remember { Calendar.getInstance() }
    val currentYear = cal.get(Calendar.YEAR)
    val currentMonthIndex = cal.get(Calendar.MONTH)
    val monthYearStr = remember(currentYear, currentMonthIndex) {
        String.format(Locale.ENGLISH, "%04d-%02d", currentYear, currentMonthIndex + 1)
    }
    val paymentsFlow = remember(monthYearStr) {
        viewModel.getPaymentsForMonthFlow(monthYearStr)
    }
    val currentMonthPayments by paymentsFlow.collectAsState(initial = emptyList())
    val currentMonthName = remember(currentMonthIndex) {
        ARABIC_MONTHS.find { it.second == currentMonthIndex }?.first ?: "الشهر الحالي"
    }

    val regularGroups = remember(groups) { groups.filter { it.paymentType != "PER_SESSION" } }
    val regularGroupIds = remember(regularGroups) { regularGroups.map { it.id }.toSet() }
    val regularStudents = remember(allStudents, regularGroupIds) { allStudents.filter { regularGroupIds.contains(it.groupId) } }

    var totalExpected = 0.0
    var totalCollected = 0.0
    var totalRemaining = 0.0

    regularStudents.forEach { student ->
        val grp = groups.find { it.id == student.groupId }
        val fee = grp?.monthlyFee ?: 0.0
        val p = currentMonthPayments.find { it.studentId == student.id }
        when (p?.paymentStatus) {
            "PAID" -> {
                totalExpected += fee
                totalCollected += fee
            }
            "PARTIAL" -> {
                totalExpected += fee
                val rem = p.remainingAmount
                totalCollected += (fee - rem).coerceAtLeast(0.0)
                totalRemaining += rem
            }
            "EXEMPT" -> {
                // Exempted
            }
            else -> { // UNPAID
                totalExpected += fee
                totalRemaining += fee
            }
        }
    }

    // Filter and sort today's groups chronologically by class start time
    val todayGroupsRaw = regularGroups.filter { group ->
        group.meetsOnDay(viewModel.todayArabicDayName)
    }

    val nowCalendar = Calendar.getInstance()
    val currentMinutes = nowCalendar.get(Calendar.HOUR_OF_DAY) * 60 + nowCalendar.get(Calendar.MINUTE)

    val todayGroups = remember(todayGroupsRaw, viewModel.todayArabicDayName, currentMinutes) {
        val mapped = todayGroupsRaw.map { g ->
            val slot = g.getTimeSlotForDay(viewModel.todayArabicDayName)
            val startMin = parseTimeToMinutes(slot)?.let { it.first * 60 + it.second } ?: 9999
            g to startMin
        }
        // Consider group as upcoming/active if its end time (start + 60 mins) is after current time
        val (upcoming, past) = mapped.partition { it.second + 60 > currentMinutes }
        
        (upcoming.sortedBy { it.second } + past.sortedBy { it.second }).map { it.first }
    }

    // Determine the active or next upcoming class
    val ongoingGroup = todayGroups.find { g ->
        val slot = g.getTimeSlotForDay(viewModel.todayArabicDayName)
        parseTimeToMinutes(slot)?.let { (h, m) ->
            val start = h * 60 + m
            val end = start + 60
            currentMinutes in start until end
        } ?: false
    }

    val nextUpcomingGroup = if (ongoingGroup != null) {
        null
    } else {
        todayGroups.find { g ->
            val slot = g.getTimeSlotForDay(viewModel.todayArabicDayName)
            parseTimeToMinutes(slot)?.let { (h, m) ->
                (h * 60 + m) > currentMinutes
            } ?: false
        }
    }

    val nextUpGroupId = ongoingGroup?.id ?: nextUpcomingGroup?.id

    // Sorted todayGroups: active/next first, then others
    val sortedTodayGroups = remember(todayGroups, nextUpGroupId) {
        val (nextUp, others) = todayGroups.partition { it.id == nextUpGroupId }
        nextUp + others
    }

    val todayStudentsCount = regularStudents.count { student ->
        todayGroups.any { group -> group.id == student.groupId }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Teacher Profile Header with Square Avatar & Warm Gradient
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Date Pill Badge Centered with Gradient Accent
                        Surface(
                            shape = RoundedCornerShape(30.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val formattedDate = remember(appLanguage, viewModel.todayDisplayDate) {
                                    if (appLanguage == "en") {
                                        java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.ENGLISH).format(java.util.Date())
                                    } else {
                                        viewModel.todayDisplayDate
                                    }
                                }
                                Text(
                                    text = if (appLanguage == "en") "Today: $formattedDate" else "اليوم: $formattedDate",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                        
                        // Teacher Info Box with Square Avatar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Square Teacher Avatar Image with Gold Accent Ring (Clickable to edit)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(2.dp, Color(0xFFD97706)), // Darker Gold for contrast
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.size(60.dp).clickable { showEditProfileDialog = true }
                                ) {
                                    if (teacherImageUri.isNotBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(teacherImageUri)
                                                .crossfade(true)
                                                .build(),
                                            placeholder = painterResource(id = R.drawable.teacher_standing_book_1787013221869),
                                            error = painterResource(id = R.drawable.teacher_standing_book_1787013221869),
                                            contentDescription = "صورة المعلم",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.teacher_standing_book_1787013221869),
                                            contentDescription = "صورة المعلم",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Teacher Name and Subject
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = teacherName.ifBlank { label("أستاذ المعلم", "Teacher") },
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 18.sp,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Outlined.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFF1E40AF), // Darker for contrast
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFC7D2FE) // Darker for contrast
                                    ) {
                                        Text(
                                            text = "${label("مادة", "Subject")}: ${teacherSubject.ifBlank { label("غير محدد", "Not specified") }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.ExtraBold, // Bold
                                            color = Color(0xFF1E1B4B), // Much darker for contrast
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontSize = 11.5.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // Edit Profile Button
                                IconButton(
                                    onClick = { showEditProfileDialog = true },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .shadow(1.dp, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "تعديل اسم المعلم والمادة",
                                        tint = Color(0xFF1E40AF), // Darker
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trial Expiration Warning Banner (2 days and 1 day before expiration)
        if (!subscriptionManager.isActivated()) {
            val remainingDays = subscriptionManager.getRemainingTrialDays()
            if (remainingDays <= 2) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenBackupDialog() },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (remainingDays <= 1) Color(0xFFFFECEE) else Color(0xFFFFF7ED)
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            if (remainingDays <= 1) Color(0xFFBE123C) else Color(0xFFC2410C)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = if (remainingDays <= 1) Color(0xFFBE123C) else Color(0xFFC2410C),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (remainingDays <= 1) "تنبيه هام: متبقي يوم واحد فقط على انتهاء التجربة المجانية" else "تنبيه: متبقي يومان فقط على انتهاء النسخة المجانية",
                                    fontWeight = FontWeight.ExtraBold, // Bold
                                    fontSize = 13.sp,
                                    color = if (remainingDays <= 1) Color(0xFF9F1239) else Color(0xFF9A3412)
                                )
                                Text(
                                    text = "يرجى الترقية إلى إحدى باقات VIP لتجنب توقف تسجيل البيانات داخل التطبيق.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold, // Bold
                                    color = MaterialTheme.colorScheme.onSurface // Darker
                                )
                            }
                        }
                    }
                }
            }
        }
        // Action Summary Cards: Scan QR Code & Today's Classes (Keeping these as they are useful)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Scan QR Code (مسح QR كود)
                SummaryCard(
                    title = label("مسح سريع", "Quick scan"),
                    value = label("تسجيل فوري", "Instant attendance"),
                    subtitle = label("مسح كود QR للطالب", "Scan the student's QR code"),
                    icon = Icons.Outlined.QrCodeScanner,
                    gradientColors = listOf(Color(0xFFDBEAFE), Color(0xFFBFDBFE)),
                    iconBgColor = Color(0xFF1E40AF), // Darker
                    textColor = Color(0xFF1E3A8A),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showBarcodeScannerModal = true
                    }
                )

                // Card 2: Today's Classes (حصص اليوم)
                val classesCountText = when (todayGroups.size) {
                    0 -> label("لا توجد حصص", "No classes")
                    1 -> label("حصة واحدة اليوم", "One class today")
                    2 -> label("حصتان اليوم", "Two classes today")
                    else -> if (appLanguage == "en") "${todayGroups.size} classes today" else "${todayGroups.size} حصص اليوم"
                }
                SummaryCard(
                    title = viewModel.todayArabicDayName,
                    value = classesCountText,
                    subtitle = label("جدول مواعيد اليوم", "Today's schedule"),
                    icon = Icons.Outlined.EventNote,
                    gradientColors = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)),
                    iconBgColor = Color(0xFFB45309), // Darker
                    textColor = Color(0xFF78350F),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTimetable
                )
            }
        }



        // Financial Animated Water Bar Card (السجل المالي للشهر الحالي)
        item {
            DashboardFinancialWaterLineCard(
                totalCollected = totalCollected,
                totalExpected = totalExpected,
                totalRemaining = totalRemaining,
                monthName = currentMonthName,
                year = currentYear,
                onClick = onNavigateToPayments
            )
        }


        // Today's Schedule Section Header with Dynamic Glow Tag
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = Color(0xFFB45309), // Darker
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (appLanguage == "en") "Today's classes" else "حصص اليوم (${viewModel.todayArabicDayName})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = if (appLanguage == "en") "${todayGroups.size} scheduled" else "${todayGroups.size} حصص مجدولة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold, // Bold
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        // Today's Schedule List with Gold Highlight for Next-up/Ongoing Class
        if (todayGroups.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Celebration,
                                contentDescription = null,
                                tint = Color(0xFF1E40AF), // Darker
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = label("لا توجد حصص مجدولة لليوم", "No classes scheduled today"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label("يمكنك إضافة أو تعديل مواعيد المجموعات من قسم المجموعات.", "Add or edit class times from the Groups section."),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold, // Bold
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(sortedTodayGroups, key = { it.id }) { group ->
                val groupStudents = allStudents.filter { it.groupId == group.id }
                val timeSlotForToday = group.getTimeSlotForDay(viewModel.todayArabicDayName)
                val isNextUp = (group.id == nextUpGroupId)
                val timingInfo = calculateClassTiming(timeSlotForToday, isNextUp)

                TodayGroupCard(
                    group = group,
                    todayDayName = viewModel.todayArabicDayName,
                    timeSlot = timeSlotForToday,
                    studentCount = groupStudents.size,
                    payments = currentMonthPayments,
                    allStudents = allStudents,
                    timingInfo = timingInfo,
                    onTriggerNotification = {
                        viewModel.triggerTestNotification(context, group)
                        Toast.makeText(context, "تم إرسال إشعار التنبيه التجريبي بنجاح! ", Toast.LENGTH_SHORT).show()
                    },
                    onStudentsClick = { groupForQuickAttendance = group }
                )
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }


    if (showEditProfileDialog) {
        TeacherProfileDialog(
            currentName = teacherName,
            currentSubject = teacherSubject,
            currentImageUri = teacherImageUri,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, subject, imageUri ->
                viewModel.saveTeacherProfile(name, subject, imageUri)
                showEditProfileDialog = false
                Toast.makeText(context, "تم حفظ بيانات وصورة المعلم بنجاح! ", Toast.LENGTH_SHORT).show()
            }
        )
    }

    groupForQuickAttendance?.let { group ->
        com.example.ui.components.QuickAttendanceModal(
            group = group,
            viewModel = viewModel,
            onDismiss = { groupForQuickAttendance = null }
        )
    }

    if (showBarcodeScannerModal) {
        BarcodeScannerModal(
            title = "مسح كود QR للطالب",
            onDismiss = { showBarcodeScannerModal = false },
            onScanResult = { scannedData ->
                val studentIdStr = scannedData.removePrefix("STUDENT_")
                val studentId = studentIdStr.toIntOrNull()
                if (studentId != null) {
                    val student = allStudents.find { it.id == studentId }
                    if (student != null) {
                        val group = groups.find { it.id == student.groupId }
                        val currentAttendance = attendanceList.find { it.studentId == student.id }
                        val currentHw = currentAttendance?.homeworkStatus ?: "كتب الواجب"
                        val currentRec = currentAttendance?.recitationStatus ?: "ممتاز"
                        viewModel.setAttendanceAndHomework(
                            studentId = student.id,
                            attendanceStatus = "حضر",
                            homeworkStatus = currentHw,
                            recitationStatus = currentRec
                        )
                        Pair(true, "تم تسجيل حضور: ${student.fullName} (${group?.name ?: ""}) ")
                    } else {
                        Pair(false, "لم يتم العثور على الطالب في قاعدة البيانات! ")
                    }
                } else {
                    Pair(false, "كود QR غير صالح ")
                }
            }
        )
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherProfileDialog(
    currentName: String,
    currentSubject: String,
    currentImageUri: String,
    onDismiss: () -> Unit,
    onSave: (name: String, subject: String, imageUri: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var subject by remember { mutableStateOf(currentSubject) }
    var imageUri by remember { mutableStateOf(currentImageUri) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUri = it.toString() }
    }

    com.example.ui.components.PremiumAlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "تعديل حساب المعلم",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Teacher Avatar Picker Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(3.dp, Color(0xFF2563EB)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(92.dp)
                            .clickable { imagePickerLauncher.launch("image/*") }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (imageUri.isNotBlank()) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "صورة المعلم",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.teacher_standing_book_1787013221869),
                                    contentDescription = "صورة المعلم",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AddAPhoto,
                                    contentDescription = "تغيير الصورة",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Photo Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختر صورة شخصية ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                        }

                        if (imageUri.isNotBlank()) {
                            IconButton(
                                onClick = { imageUri = "" },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "مسح الصورة", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // Teacher Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكريم للمعلم/الأستاذ") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF2563EB)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                // Subject Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "التخصص / المادة الدراسية الأساسية:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DropdownMenuSelector(selected = subject, options = com.example.utils.SUBJECT_LIST) {
                        subject = it
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && subject.isNotBlank()) {
                        onSave(name, subject, imageUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("حفظ البيانات والتعديلات", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إلغاء الأمر", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    iconBgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .65f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.ui.components.PremiumIconTile(
                    icon = icon,
                    contentDescription = null,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TodayGroupCard(
    group: GroupEntity,
    todayDayName: String = "",
    timeSlot: String,
    studentCount: Int,
    payments: List<PaymentRecordEntity>,
    allStudents: List<StudentEntity>,
    timingInfo: ClassTimingInfo,
    onTriggerNotification: () -> Unit,
    onStudentsClick: () -> Unit
) {
    val groupStudents = allStudents.filter { it.groupId == group.id }
    val paidCount = groupStudents.count { student ->
        payments.find { it.studentId == student.id }?.paymentStatus == "PAID"
    }
    val unpaidCount = studentCount - paidCount
    
    val isGold = timingInfo.isNextUp || timingInfo.state == ClassTimingState.ONGOING
    val isOngoing = timingInfo.state == ClassTimingState.ONGOING

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isGold) Modifier.shadow(8.dp, RoundedCornerShape(22.dp), spotColor = Color(0xFFF59E0B))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isGold) MaterialTheme.colorScheme.primary.copy(alpha = .55f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            // Top Status Pill / Banner for Gold Next-Up Class
            if (isGold) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOngoing) Color(0xFFE11D48) else Color(0xFFD97706),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isOngoing) Icons.Outlined.PlayCircleFilled else Icons.Outlined.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOngoing) " جارية الآن - الحصة الحالية" else " الحصة التي عليها الدور (القادمة)",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.5.sp,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Main Info Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Group Type Icon Box
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isGold) Color(0xFFF59E0B) else Color(0xFF2563EB)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (group.groupType) {
                                "ONLINE" -> Icons.Outlined.Language
                                "PRIVATE" -> Icons.Outlined.Person
                                else -> Icons.Outlined.MenuBook
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isGold) Color(0xFF78350F) else MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "مادة: ${group.subject}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isGold) Color(0xFFB45309) else Color(0xFF2563EB),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = " • ${if (group.groupType == "ONLINE") "أونلاين" else if (group.groupType == "PRIVATE") "درس خاص" else "سنتر"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Time Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isGold) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (isGold) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = if (isGold) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeSlot,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isGold) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Remaining Time Countdown Highlight Strip (Very Prominent)
            if (timingInfo.timeRemainingText.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (timingInfo.state) {
                        ClassTimingState.ONGOING -> Color(0xFFFFE4E6)
                        ClassTimingState.UPCOMING -> if (isGold) Color(0xFFFEF9C3) else MaterialTheme.colorScheme.surfaceVariant
                        ClassTimingState.FINISHED -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(
                        1.dp,
                        when (timingInfo.state) {
                            ClassTimingState.ONGOING -> Color(0xFFFB7185)
                            ClassTimingState.UPCOMING -> if (isGold) Color(0xFFFBBF24) else MaterialTheme.colorScheme.outline
                            ClassTimingState.FINISHED -> MaterialTheme.colorScheme.outline
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (timingInfo.state) {
                                ClassTimingState.ONGOING -> Icons.Outlined.HourglassBottom
                                ClassTimingState.UPCOMING -> Icons.Outlined.HourglassTop
                                ClassTimingState.FINISHED -> Icons.Outlined.CheckCircle
                            },
                            contentDescription = null,
                            tint = when (timingInfo.state) {
                                ClassTimingState.ONGOING -> Color(0xFFE11D48)
                                ClassTimingState.UPCOMING -> if (isGold) Color(0xFFB45309) else Color(0xFF2563EB)
                                ClassTimingState.FINISHED -> Color(0xFF16A34A)
                            },
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timingInfo.timeRemainingText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = when (timingInfo.state) {
                                ClassTimingState.ONGOING -> Color(0xFFBE123C)
                                ClassTimingState.UPCOMING -> if (isGold) Color(0xFF92400E) else Color(0xFF1E40AF)
                                ClassTimingState.FINISHED -> Color(0xFF15803D)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(
                color = if (isGold) Color(0xFFFDE68A) else MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Students count & Quick Alert Notification button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onStudentsClick() }.padding(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isGold) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PeopleAlt,
                            contentDescription = null,
                            tint = if (isGold) Color(0xFFB45309) else Color(0xFF2563EB),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "الطلاب: $studentCount طالب",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isGold) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onTriggerNotification,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGold) Color(0xFFD97706) else Color(0xFF2563EB)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "إشعار التنبيه ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

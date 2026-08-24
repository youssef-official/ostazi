package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.*

import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.example.data.GroupEntity
import com.example.data.StudentEntity
import com.example.data.meetsOnDay
import com.example.data.getTimeSlotForDay
import com.example.ui.MainViewModel
import com.example.ui.components.GroupAttendancePdfDialog
import com.example.ui.components.GroupWhatsAppReportsDialog
import com.example.ui.components.StudentDetailDialog
import com.example.ui.components.BarcodeScannerModal
import com.example.ui.components.StudentPdfReportHelper
import com.example.ui.components.PremiumIconTile
import com.example.ui.components.PremiumActionChip
import com.example.ui.components.PremiumAlertDialog
import com.example.ui.components.premiumTextFieldColors
import com.example.ui.theme.*
import com.example.utils.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val attendanceList by viewModel.attendanceForToday.collectAsStateWithLifecycle()
    val allExams by viewModel.allExams.collectAsStateWithLifecycle()
    val paymentsForMonth by viewModel.paymentsForMonth.collectAsStateWithLifecycle()
    val teacherName by viewModel.teacherName.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Center, 1 = Online, 2 = Private
    var showAddDialog by remember { mutableStateOf(false) }
    var newGroupInitialType by remember { mutableStateOf("CENTER") }
    var editingGroup by remember { mutableStateOf<GroupEntity?>(null) }
    var groupToDelete by remember { mutableStateOf<GroupEntity?>(null) }
    var selectedStudentForDetail by remember { mutableStateOf<StudentEntity?>(null) }
    var groupForWhatsAppReports by remember { mutableStateOf<Pair<GroupEntity, Int>?>(null) }
    var groupForMonthlyLog by remember { mutableStateOf<GroupEntity?>(null) }
    var groupForPdfReport by remember { mutableStateOf<GroupEntity?>(null) }
    var currentGroupForScan by remember { mutableStateOf<GroupEntity?>(null) }
    var showGroupBarcodeScanner by remember { mutableStateOf(false) }
    val defaultTeacherSubject by viewModel.teacherSubject.collectAsStateWithLifecycle()

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val scannedData = result.contents
            val studentIdStr = scannedData.removePrefix("STUDENT_")
            val studentId = studentIdStr.toIntOrNull()
            
            if (studentId != null) {
                val student = allStudents.find { it.id == studentId }
                if (student != null && currentGroupForScan != null && student.groupId == currentGroupForScan?.id) {
                    val currentAttendance = attendanceList.find { it.studentId == student.id }
                    val currentHw = currentAttendance?.homeworkStatus ?: "كتب الواجب"
                    val currentRec = currentAttendance?.recitationStatus ?: "ممتاز"
                    viewModel.setAttendanceAndHomework(
                        studentId = student.id,
                        attendanceStatus = "حضر",
                        homeworkStatus = currentHw,
                        recitationStatus = currentRec
                    )
                    Toast.makeText(context, "تم تسجيل حضور ${student.fullName} بنجاح!", Toast.LENGTH_SHORT).show()
                } else if (student != null) {
                    Toast.makeText(context, "الطالب ليس في هذه المجموعة!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "لم يتم العثور على الطالب!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "باركود غير صالح", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val regularGroups = groups.filter { it.paymentType != "PER_SESSION" }
    val centerGroups = regularGroups.filter { it.groupType == "CENTER" || (it.groupType != "ONLINE" && it.groupType != "PRIVATE") }
    val onlineGroups = regularGroups.filter { it.groupType == "ONLINE" }
    val privateGroups = regularGroups.filter { it.groupType == "PRIVATE" }

    val activeList = when (selectedTab) {
        0 -> regularGroups
        1 -> centerGroups
        2 -> onlineGroups
        else -> privateGroups
    }

    Scaffold(
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.groupManagement(appLanguage),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = AppStrings.groupSubtitle(appLanguage),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            newGroupInitialType = when (selectedTab) {
                                2 -> "ONLINE"
                                3 -> "PRIVATE"
                                else -> "CENTER"
                            }
                            showAddDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (appLanguage == "en") "Add group" else "إضافة مجموعة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val categoryTabs = listOf(
                        Triple(0, "${AppStrings.allGroups(appLanguage)}  ${groups.size}", Icons.Outlined.Groups),
                        Triple(1, "${AppStrings.center(appLanguage)}  ${centerGroups.size}", Icons.Outlined.Apartment),
                        Triple(2, "${AppStrings.online(appLanguage)}  ${onlineGroups.size}", Icons.Outlined.Language),
                        Triple(3, "${AppStrings.privateLesson(appLanguage)}  ${privateGroups.size}", Icons.Outlined.PersonOutline)
                    )
                    categoryTabs.forEach { (index, title, icon) ->
                        PremiumActionChip(
                            text = title,
                            icon = icon,
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (activeList.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (selectedTab) {
                                    0 -> Icons.Outlined.Groups
                                    1 -> Icons.Outlined.Apartment
                                    2 -> Icons.Outlined.Laptop
                                    else -> Icons.Outlined.PersonOutline
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = .38f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = when (selectedTab) {
                                    0 -> "لا توجد أي مجموعات مسجلة حتى الآن"
                                    1 -> "لا توجد مجموعات سنتر (أوفلاين) حالياً"
                                    2 -> "لا توجد مجموعات أونلاين حالياً"
                                    else -> "لا توجد مجموعات درس خاص (برايفت) حالياً"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "اضغط على الزر أدناه لإضافة مجموعة جديدة ومواعيدها بكل سهولة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة مجموعة الآن", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(activeList, key = { it.id }) { group ->
                    val groupStudents = allStudents.filter { it.groupId == group.id }
                    GroupCardItem(
                        viewModel = viewModel,
                        appLanguage = appLanguage,
                        group = group,
                        students = groupStudents,
                        attendanceList = attendanceList,
                        allExams = allExams,
                        paymentsForMonth = paymentsForMonth,
                        teacherName = teacherName,
                        onEdit = { editingGroup = group },
                        onDelete = {
                            groupToDelete = group
                        },
                        onStudentClick = { student ->
                            selectedStudentForDetail = student
                        },
                        onAttendanceChange = { studentId, attendance, homework, interaction, recitationGrade, note ->
                            viewModel.setAttendanceAndHomework(
                                studentId = studentId,
                                attendanceStatus = attendance,
                                homeworkStatus = homework,
                                interactionStatus = interaction,
                                recitationGrade = recitationGrade,
                                notes = note
                            )
                        },
                        onBatchAttendance = { studentIds, att, hw, inter, rec ->
                            viewModel.batchSetAttendance(
                                studentIds = studentIds,
                                attendanceStatus = att,
                                homeworkStatus = hw,
                                interactionStatus = inter,
                                recitationGrade = rec
                            )
                        },
                        onSaveNote = { studentId, note ->
                            viewModel.setStudentNote(studentId = studentId, note = note)
                        },
                        onSaveExam = { studentId, ex1, ex2, ex3, ex4, ex5 ->
                            viewModel.saveExams(studentId, ex1, ex2, ex3, ex4, ex5)
                        },
                        onPaymentChange = { studentId, status, remaining ->
                            viewModel.setPaymentStatus(studentId, status, remaining)
                        },
                        onOpenIndividualParentsReport = {
                            groupForWhatsAppReports = group to 0
                        },
                        onOpenGroupBatchReport = {
                            groupForWhatsAppReports = group to 1
                        },
                        onOpenMonthlyLog = {
                            groupForMonthlyLog = group
                        },
                        onOpenPdfReport = {
                            groupForPdfReport = group
                        },
                        onScanBarcodeClick = {
                            currentGroupForScan = group
                            showGroupBarcodeScanner = true
                        }
                    )
                }
            }
        }
    }

    // Add Group Dialog
    if (showAddDialog) {
        GroupFormDialog(
            group = null,
            appLanguage = appLanguage,
            existingGroups = groups,
            initialType = if (newGroupInitialType.isNotBlank()) newGroupInitialType else when (selectedTab) {
                1 -> "CENTER"
                2 -> "ONLINE"
                3 -> "PRIVATE"
                else -> "CENTER"
            },
            defaultSubject = defaultTeacherSubject,
            onDismiss = { showAddDialog = false },
            onSave = { name, subject, d1, d2, d3, t1, t2, t3, fee, groupType, paymentType, whatsappGroupUrl, notes, daysMap ->
                val days = daysMap.keys.toList()
                viewModel.addGroup(
                    name = name, subject = subject,
                    day1 = days.getOrNull(0) ?: "بدون",
                    day2 = days.getOrNull(1),
                    day3 = days.getOrNull(2),
                    day4 = days.getOrNull(3),
                    day5 = days.getOrNull(4),
                    day6 = days.getOrNull(5),
                    day7 = days.getOrNull(6),
                    timeSlot = days.getOrNull(0)?.let { daysMap[it] } ?: "08:00 ص",
                    timeSlot2 = days.getOrNull(1)?.let { daysMap[it] },
                    timeSlot3 = days.getOrNull(2)?.let { daysMap[it] },
                    timeSlot4 = days.getOrNull(3)?.let { daysMap[it] },
                    timeSlot5 = days.getOrNull(4)?.let { daysMap[it] },
                    timeSlot6 = days.getOrNull(5)?.let { daysMap[it] },
                    timeSlot7 = days.getOrNull(6)?.let { daysMap[it] },
                    fee = fee, groupType = groupType, paymentType = paymentType, whatsappGroupUrl = whatsappGroupUrl, notes = notes, context = context
                )
                // If the user was in a specific tab other than "All", switch to the corresponding tab
                if (selectedTab != 0) {
                    selectedTab = when (groupType) {
                        "CENTER" -> 1
                        "ONLINE" -> 2
                        "PRIVATE" -> 3
                        else -> 0
                    }
                }
                showAddDialog = false
                Toast.makeText(context, "تمت إضافة المجموعة بنجاح!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit Group Dialog
    editingGroup?.let { groupToEdit ->
        GroupFormDialog(
            group = groupToEdit,
            appLanguage = appLanguage,
            existingGroups = groups,
            initialType = groupToEdit.groupType,
            defaultSubject = defaultTeacherSubject,
            onDismiss = { editingGroup = null },
            onSave = { name, subject, d1, d2, d3, t1, t2, t3, fee, groupType, paymentType, whatsappGroupUrl, notes, daysMap ->
                val days = daysMap.keys.toList()
                val updated = groupToEdit.copy(
                    name = name,
                    subject = subject,
                    day1 = days.getOrNull(0) ?: "بدون",
                    day2 = days.getOrNull(1),
                    day3 = days.getOrNull(2),
                    day4 = days.getOrNull(3),
                    day5 = days.getOrNull(4),
                    day6 = days.getOrNull(5),
                    day7 = days.getOrNull(6),
                    timeSlot = days.getOrNull(0)?.let { daysMap[it] } ?: "08:00 ص",
                    timeSlot2 = days.getOrNull(1)?.let { daysMap[it] },
                    timeSlot3 = days.getOrNull(2)?.let { daysMap[it] },
                    timeSlot4 = days.getOrNull(3)?.let { daysMap[it] },
                    timeSlot5 = days.getOrNull(4)?.let { daysMap[it] },
                    timeSlot6 = days.getOrNull(5)?.let { daysMap[it] },
                    timeSlot7 = days.getOrNull(6)?.let { daysMap[it] },
                    monthlyFee = fee,
                    groupType = groupType,
                    paymentType = paymentType,
                    whatsappGroupUrl = whatsappGroupUrl,
                    notes = notes
                )
                viewModel.updateGroup(updated, context)
                editingGroup = null
                Toast.makeText(context, "تم تعديل بيانات المجموعة وتحديث التنبيهات!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Group Delete Confirmation Dialog
    groupToDelete?.let { targetGroup ->
        PremiumAlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                    Text(
                        text = "تأكيد حذف المجموعة ",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B),
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف مجموعة '${targetGroup.name}'؟\nسيتم حذف بيانات المجموعة وسجلات الحضور والامتحانات المرتبطة بها.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val g = targetGroup
                        groupToDelete = null
                        viewModel.deleteGroup(g)
                        Toast.makeText(context, "تم حذف المجموعة بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("موافق (حذف)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { groupToDelete = null }) {
                    Text("إلغاء (تراجع)", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Student Popup Detail Modal
    selectedStudentForDetail?.let { student ->
        val grp = groups.find { it.id == student.groupId }
        StudentDetailDialog(
            student = student,
            group = grp,
            viewModel = viewModel,
            onDismiss = { selectedStudentForDetail = null }
        )
    }

    // Group WhatsApp Bulk Reports Dialog
    groupForWhatsAppReports?.let { (grp, tabIndex) ->
        val grpStudents = allStudents.filter { it.groupId == grp.id }
        GroupBulkWhatsAppDialog(
            group = grp,
            students = grpStudents,
            attendanceList = attendanceList,
            allExams = allExams,
            paymentsForMonth = paymentsForMonth,
            teacherName = teacherName,
            initialTab = tabIndex,
            viewModel = viewModel,
            onDismiss = { groupForWhatsAppReports = null }
        )
    }

    // Group Monthly Evaluation & Attendance Log Dialog (Screenshot 2)
    groupForMonthlyLog?.let { grp ->
        val grpStudents = allStudents.filter { it.groupId == grp.id }
        GroupMonthlyLogDialog(
            group = grp,
            students = grpStudents,
            viewModel = viewModel,
            allExams = allExams,
            onDismiss = { groupForMonthlyLog = null }
        )
    }

    // Group Attendance PDF Report Dialog
    groupForPdfReport?.let { grp ->
        val grpStudents = allStudents.filter { it.groupId == grp.id }

        GroupAttendancePdfDialog(
            group = grp,
            students = grpStudents,
            viewModel = viewModel,
            onDismiss = { groupForPdfReport = null }
        )
    }

    if (showGroupBarcodeScanner && currentGroupForScan != null) {
        val grp = currentGroupForScan!!
        BarcodeScannerModal(
            title = "مسح كود QR - ${grp.name}",
            onDismiss = {
                showGroupBarcodeScanner = false
                currentGroupForScan = null
            },
            onScanResult = { scannedData ->
                val studentIdStr = scannedData.removePrefix("STUDENT_")
                val studentId = studentIdStr.toIntOrNull()
                if (studentId != null) {
                    val student = allStudents.find { it.id == studentId }
                    if (student != null && student.groupId == grp.id) {
                        val currentAttendance = attendanceList.find { it.studentId == student.id }
                        val currentHw = currentAttendance?.homeworkStatus ?: "كتب الواجب"
                        val currentRec = currentAttendance?.recitationStatus ?: "ممتاز"
                        viewModel.setAttendanceAndHomework(
                            studentId = student.id,
                            attendanceStatus = "حضر",
                            homeworkStatus = currentHw,
                            recitationStatus = currentRec
                        )
                        Pair(true, "تم تسجيل حضور ${student.fullName} بنجاح! ")
                    } else if (student != null) {
                        Pair(false, "الطالب ${student.fullName} ليس في مجموعة (${grp.name})! ")
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

enum class GroupActiveTab {
    STUDENTS,
    EVALUATION,
    EXAMS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCardItem(
    viewModel: MainViewModel,
    appLanguage: String,
    group: GroupEntity,
    students: List<StudentEntity>,
    attendanceList: List<com.example.data.AttendanceRecordEntity>,
    allExams: List<com.example.data.ExamRecordEntity>,
    paymentsForMonth: List<com.example.data.PaymentRecordEntity>,
    teacherName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStudentClick: (StudentEntity) -> Unit,
    onAttendanceChange: (studentId: Int, attendance: String, homework: String, interaction: String, recitationGrade: String, note: String?) -> Unit,
    onBatchAttendance: (studentIds: List<Int>, att: String?, hw: String?, inter: String?, rec: String?) -> Unit,
    onSaveNote: (studentId: Int, note: String) -> Unit,
    onSaveExam: (studentId: Int, exam1: String?, exam2: String?, exam3: String?, exam4: String?, exam5: String?) -> Unit,
    onPaymentChange: (studentId: Int, status: String, remaining: Double) -> Unit,
    onOpenIndividualParentsReport: () -> Unit = {},
    onOpenGroupBatchReport: () -> Unit = {},
    onOpenMonthlyLog: () -> Unit = {},
    onOpenPdfReport: () -> Unit = {},
    onScanBarcodeClick: () -> Unit = {}
) {
    var activeGroupTab by remember { mutableStateOf<GroupActiveTab?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val totalIncome = students.size * group.monthlyFee.toInt()

    var studentSearchQuery by remember { mutableStateOf("") }
    var selectedEvaluationTab by remember { mutableStateOf(0) } // 0: الكل, 1: الحضور, 2: الغياب, 3: الواجب, 4: التسميع, 5: التفاعل
    var showUnpaidModal by remember { mutableStateOf(false) }

    var studentForExamDialog by remember { mutableStateOf<StudentEntity?>(null) }
    var studentForPaymentDialog by remember { mutableStateOf<StudentEntity?>(null) }
    var paymentDialogMode by remember { mutableStateOf("REMAINING") } // "REMAINING" or "DISCOUNT"
    var studentForNoteDialog by remember { mutableStateOf<StudentEntity?>(null) }
    var studentForReportShare by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    val groupAccent = remember(group.id) {
        listOf(
            Color(0xFF27569A), Color(0xFF16855B), Color(0xFFB7791F),
            Color(0xFF147D84), Color(0xFF6B5A9A), Color(0xFF9B4F62)
        )[kotlin.math.abs(group.id) % 6]
    }

    val collectedAmount = remember(paymentsForMonth, students, group.monthlyFee) {
        val fee = group.monthlyFee
        students.sumOf { student ->
            val payRec = paymentsForMonth.find { it.studentId == student.id }
            when (payRec?.paymentStatus) {
                "PAID" -> fee
                "PARTIAL" -> (fee - payRec.remainingAmount).coerceAtLeast(0.0)
                else -> 0.0
            }
        }.toInt()
    }

    val filteredStudents = remember(students, studentSearchQuery) {
        if (studentSearchQuery.isBlank()) students
        else students.filter {
            it.fullName.contains(studentSearchQuery, ignoreCase = true) ||
            it.studentPhone.contains(studentSearchQuery) ||
            it.parentPhone.contains(studentSearchQuery)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, groupAccent.copy(alpha = .48f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // --- HEADER ROW: Group Title, Subject, Badges & Actions ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Group Info Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = group.name.ifBlank { AppStrings.unnamedGroup(appLanguage) },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Type Tag ("سنتر" / "أونلاين" / "درس خاص")
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = groupAccent.copy(alpha = .12f)
                        ) {
                            Text(
                                text = if (group.groupType == "ONLINE") AppStrings.online(appLanguage) else if (group.groupType == "PRIVATE") AppStrings.privateLesson(appLanguage) else AppStrings.center(appLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = groupAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${AppStrings.subject(appLanguage)}: ${group.subject.ifBlank { AppStrings.general(appLanguage) }}",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )

                        val activeWeeklyDays = listOf(
                            group.day1, group.day2, group.day3,
                            group.day4, group.day5, group.day6, group.day7
                        ).filter { !it.isNullOrBlank() && it != "بدون" }.distinct().size
                        val calculatedSessionsCount = if (activeWeeklyDays > 0) activeWeeklyDays * 4 else 8

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f))
                        ) {
                            Text(
                                text = AppStrings.sessionsMonthly(appLanguage, calculatedSessionsCount),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Edit & Delete Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    com.example.ui.components.PremiumIconAction(Icons.Outlined.Edit, "تعديل المجموعة", onEdit)
                    com.example.ui.components.PremiumIconAction(Icons.Outlined.DeleteOutline, "حذف المجموعة", onDelete, destructive = true)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- STATS SUMMARY BAR (Total Students, Paid, Unpaid) ---
            val paidCount = students.count { student ->
                paymentsForMonth.find { it.studentId == student.id }?.paymentStatus == "PAID"
            }
            val unpaidCount = students.size - paidCount

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(AppStrings.studentCount(appLanguage), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        Text("${students.size}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(AppStrings.paid(appLanguage), fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                        Text("$paidCount", fontSize = 13.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Black)
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showUnpaidModal = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(AppStrings.notPaid(appLanguage), fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        Text("$unpaidCount", fontSize = 13.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Black)
                    }
                }
            }

            if (group.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.StickyNote2, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "ملاحظات: ${group.notes}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- REPORTS & LOGS ACTION BUTTONS (2x2 Grid) ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.2.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clickable { onOpenGroupBatchReport() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إرسال على الجروب دفعة واحدة", color = Color(0xFF15803D), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clickable { onOpenIndividualParentsReport() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إرسال لولي الأمر", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clickable { onOpenMonthlyLog() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("سجل المتابعة الشهري ", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clickable { onOpenPdfReport() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("كشف الحضور PDF ", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- FINANCIAL STATS GRID (2x2) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "الطلاب المسجلين",
                    value = "${students.size} طالب",
                    icon = Icons.Outlined.People,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { activeGroupTab = if (activeGroupTab == GroupActiveTab.EVALUATION) null else GroupActiveTab.EVALUATION }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "سعر الاشتراك",
                    value = "${group.monthlyFee.toInt()} ج.م",
                    icon = Icons.Outlined.Payments,
                    iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconColor = Color(0xFF16A34A)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "الدخل المتوقع",
                    value = "$totalIncome ج.م",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconColor = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "المبلغ المحصل",
                    value = "$collectedAmount ج.م",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconColor = Color(0xFFD97706)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- QUICK ACTIONS NAVIGATION TOOLBAR ---
            Text(
                text = "إجراءات سريعة",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    QuickActionItem(title = "تعديل", icon = Icons.Outlined.Edit, bgColor = MaterialTheme.colorScheme.surfaceVariant, fgColor = MaterialTheme.colorScheme.primary, onClick = onEdit)
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    QuickActionItem(
                        title = "الطلاب",
                        icon = Icons.Outlined.People,
                        bgColor = MaterialTheme.colorScheme.tertiaryContainer,
                        fgColor = Color(0xFFD97706),
                        onClick = { activeGroupTab = if (activeGroupTab == GroupActiveTab.STUDENTS) null else GroupActiveTab.STUDENTS }
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    QuickActionItem(
                        title = "تقييم",
                        icon = Icons.Outlined.Checklist,
                        bgColor = MaterialTheme.colorScheme.primaryContainer,
                        fgColor = MaterialTheme.colorScheme.primary,
                        onClick = { activeGroupTab = if (activeGroupTab == GroupActiveTab.EVALUATION) null else GroupActiveTab.EVALUATION }
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    QuickActionItem(
                        title = "امتحانات",
                        icon = Icons.Outlined.Assignment,
                        bgColor = if (activeGroupTab == GroupActiveTab.EXAMS) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        fgColor = MaterialTheme.colorScheme.primary,
                        onClick = { activeGroupTab = if (activeGroupTab == GroupActiveTab.EXAMS) null else GroupActiveTab.EXAMS }
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    QuickActionItem(title = "مسح سريع", icon = Icons.Outlined.QrCodeScanner, bgColor = MaterialTheme.colorScheme.surfaceVariant, fgColor = MaterialTheme.colorScheme.primary, onClick = onScanBarcodeClick)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SCHEDULE HEADER & TIMES ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text("مواعيد الحصص", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(8.dp))

            val days = listOf(
                group.day1 to group.timeSlot,
                group.day2 to group.timeSlot2,
                group.day3 to group.timeSlot3,
                group.day4 to group.timeSlot4,
                group.day5 to group.timeSlot5,
                group.day6 to group.timeSlot6,
                group.day7 to group.timeSlot7
            ).filter { !it.first.isNullOrBlank() && it.first != "بدون" }.distinctBy { it.first }

            if (days.isEmpty()) {
                Text("مواعيد الحصص غير محددة", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            } else {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    days.forEach { (day, time) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Event,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = day ?: "",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = Color(0xFFDBEAFE),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = time ?: "غير محدد",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Active Tab Content
            AnimatedVisibility(visible = activeGroupTab != null) {
                Column(modifier = Modifier.padding(top = 18.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .65f))
                    Spacer(modifier = Modifier.height(14.dp))

                    when (activeGroupTab) {
                        GroupActiveTab.STUDENTS -> {
                            // ==================== TAB 1: PURE STUDENT LIST ====================
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f))
                                ) {
                                    Text(
                                        text = "${students.size} طالب مسجل",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = "قائمة طلاب المجموعة (${students.size})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Search Bar for Students
                            OutlinedTextField(
                                value = studentSearchQuery,
                                onValueChange = { studentSearchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("ابحث عن طالب بالاسم أو الهاتف...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (studentSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { studentSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Outlined.Close, contentDescription = "مسح", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedBorderColor = Color(0xFFD97706)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (filteredStudents.isEmpty()) {
                                Text(
                                    text = if (students.isEmpty()) "لا يوجد طلاب مسجلين في هذه المجموعة حتى الآن." else "لا يوجد نتائج مطابقة للبحث.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                filteredStudents.forEach { student ->
                                    val paymentRecord = paymentsForMonth.find { it.studentId == student.id }
                                    val payStatus = paymentRecord?.paymentStatus ?: "UNPAID"
                                    val remainingFee = paymentRecord?.remainingAmount ?: 0.0

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Keep the student row focused on the one action relevant here: payment.
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (payStatus == "PAID") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                                                border = BorderStroke(1.dp, if (payStatus == "PAID") MaterialTheme.colorScheme.tertiary.copy(alpha = .35f) else MaterialTheme.colorScheme.error.copy(alpha = .35f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (payStatus == "PAID") Icons.Outlined.CheckCircle else Icons.Outlined.Schedule,
                                                        contentDescription = null,
                                                        tint = if (payStatus == "PAID") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Text(
                                                        text = if (payStatus == "PAID") "تم الدفع" else "لم يدفع",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (payStatus == "PAID") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                            // Student Avatar & Info (Right)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = student.fullName, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = student.fullName.take(1),
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFD97706)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        GroupActiveTab.EVALUATION -> {
                            // ==================== TAB 2: EVALUATION (ATTENDANCE, HOMEWORK, RECITATION, INTERACTION) ====================
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "متابعة وتقييم الطلاب (${students.size})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Search Bar for Students
                            OutlinedTextField(
                                value = studentSearchQuery,
                                onValueChange = { studentSearchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("ابحث عن طالب بالاسم أو الهاتف...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (studentSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { studentSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Outlined.Close, contentDescription = "مسح", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Batch Cancel Session Button
                            Button(
                                onClick = {
                                    onBatchAttendance(students.map { it.id }, "تم الغاء حصة اليوم", "", "", "")
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Outlined.Cancel, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تم إلغاء حصة اليوم للمجموعة ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            // Category Filter Tabs (Exams Removed from Here as Requested!)
                            val evalTabs = listOf("الكل", "الحضور", "الغياب", "الواجب", "التسميع", "التفاعل")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                evalTabs.forEachIndexed { index, tabTitle ->
                                    val isSel = selectedEvaluationTab == index
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedEvaluationTab = index }
                                    ) {
                                        Text(
                                            text = tabTitle,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))



                            if (filteredStudents.isEmpty()) {
                                Text(
                                    text = if (students.isEmpty()) "لا يوجد طلاب مسجلين في هذه المجموعة حتى الآن." else "لا يوجد نتائج مطابقة للبحث.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                filteredStudents.forEach { student ->
                                    val attRecord = attendanceList.find { it.studentId == student.id }
                                            val attStatus = when (attRecord?.attendanceStatus) {
                                                "حضر", "حاضر", "PRESENT" -> "حضر"
                                                "غائب", "لم يحضر", "ABSENT" -> "لم يحضر"
                                                "متأخر" -> "متأخر"
                                                "تم الغاء حصة اليوم", "تم إلغاء الحصة", "إلغاء" -> "إلغاء"
                                                else -> ""
                                            }
                                    val hwStatus = attRecord?.homeworkStatus ?: ""
                                    val interStatus = attRecord?.interactionStatus ?: ""
                                    val recGrade = attRecord?.recitationGrade ?: ""
                                    val studentNote = attRecord?.notes ?: ""
                                    val paymentRecord = paymentsForMonth.find { it.studentId == student.id }
                                    val payStatus = paymentRecord?.paymentStatus ?: "UNPAID"
                                    val remainingFee = paymentRecord?.remainingAmount ?: 0.0

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            // Student Header
                                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Add Note Button
                                                    IconButton(
                                                        onClick = {
                                                            studentForNoteDialog = student
                                                        },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = androidx.compose.material.icons.Icons.Outlined.EditNote,
                                                            contentDescription = "إضافة ملاحظة",
                                                            tint = Color(0xFFF59E0B),
                                                            modifier = Modifier.size(26.dp)
                                                        )
                                                    }

                                                    // Monthly PDF Report Button
                                                    IconButton(
                                                        onClick = {
                                                            scope.launch {
                                                                val cal = java.util.Calendar.getInstance()
                                                                val mName = ARABIC_MONTHS.find { it.second == cal.get(java.util.Calendar.MONTH) }?.first ?: "الشهر الحالي"
                                                                val atts = viewModel.getAttendanceForStudentFlow(student.id).first()
                                                                val exams = viewModel.getExamForStudentFlow(student.id).first()
                                                                StudentPdfReportHelper.generateAndOpenStudentMonthlyReport(
                                                                    context = context,
                                                                    student = student,
                                                                    group = group,
                                                                    attendanceList = atts,
                                                                    examRecord = exams,
                                                                    paymentRecord = null,
                                                                    teacherName = viewModel.teacherName.value,
                                                                    monthName = mName,
                                                                    year = cal.get(java.util.Calendar.YEAR)
                                                                )
                                                            }
                                                        },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.PictureAsPdf,
                                                            contentDescription = "التقرير الشهري PDF",
                                                            tint = Color(0xFFDC2626),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    // WhatsApp Single Report
                                                    IconButton(
                                                        onClick = {
                                                            val formattedDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ar")).format(Date())
                                                            val msg = buildString {
                                                                append("✨ *تقرير المتابعة والتقييم الشامل* ✨\n\n")
                                                                append(" *تقرير المتابعة والتقييم الشامل* \n\n")
                                                                append("ولي أمر الطالب/ة المحترم: *${student.fullName}*\n")
                                                                append("📚 *المجموعة:* ${group.name} (${group.subject})\n")
                                                                append("🗓️ *التاريخ:* $formattedDate\n\n")
                                                                if (attStatus.isNotBlank()) append("📌 *حالة الحضور:* $attStatus\n")
                                                                if (hwStatus.isNotBlank()) append("📝 *أداء الواجب:* $hwStatus\n")
                                                                if (interStatus.isNotBlank()) append("💡 *التفاعل الصفي:* $interStatus\n")
                                                                if (recGrade.isNotBlank()) append("📖 *درجة التسميع:* $recGrade\n")
                                                                append(" *المجموعة:* ${group.name} (${group.subject})\n")
                                                                val payDisplay = when (payStatus) {
                                                                    "PAID" -> "تم سداد المصروفات بالكامل ✔️"
                                                                    else -> "لم يتم سداد المصروفات بعد ⏳"
                                                                }
                                                                append("\n💳 *المصروفات:* $payDisplay\n\n")
                                                                if (teacherName.isNotBlank()) append("أ/ $teacherName")
                                                            }
                                                            val phone = student.parentPhone.ifBlank { student.studentPhone }
                                                            studentForReportShare = Triple(student.fullName, phone, msg)
                                                        },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Icon(Icons.Outlined.Send, contentDescription = "إرسال تقرير الطالب", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                    }
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = student.fullName, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (student.studentPhone.isNotBlank()) {
                                                        Text(text = student.studentPhone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // 1. Attendance (الحضور) with Clear/Reset Option
                                            if (selectedEvaluationTab == 0 || selectedEvaluationTab == 1 || selectedEvaluationTab == 2) {
                                                Text("الحضور:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    listOf("حضر", "لم يحضر", "متأخر").forEach { st ->
                                                        val isSel = attStatus == st
                                                        val bg = when {
                                                            isSel && st == "حضر" -> Color(0xFF10B981)
                                                            isSel && st == "لم يحضر" -> Color(0xFFEF4444)
                                                            isSel && st == "متأخر" -> Color(0xFFF59E0B)
                                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                                        }
                                                        val fg = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(bg)
                                                                .border(1.dp, if (isSel) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                                                .clickable { onAttendanceChange(student.id, st, hwStatus, interStatus, recGrade, studentNote) }
                                                                .padding(vertical = 6.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = when(st) {
                                                                    "حضر" -> "حضر"
                                                                    "لم يحضر" -> "لم يحضر"
                                                                    "متأخر" -> "متأخر"
                                                                    else -> ""
                                                                },
                                                                fontSize = 9.2.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = fg
                                                            )
                                                        }
                                                    }
                                                    // Undo / Clear Attendance Button
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.errorContainer,
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .35f)),
                                                        modifier = Modifier.clickable {
                                                            onAttendanceChange(student.id, "", hwStatus, interStatus, recGrade, studentNote)
                                                            Toast.makeText(context, "تم إلغاء تسجيل الحضور ", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Text("إلغاء ", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }

                                            // 2. Homework (الواجب) with Clear/Reset Option
                                            if (selectedEvaluationTab == 0 || selectedEvaluationTab == 3) {
                                                Text("الواجب:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    listOf("كتب", "لم يكتب", "متأخر", "لا يوجد").forEach { hw ->
                                                        val isSel = hwStatus == hw || (hw == "كتب" && hwStatus == "كتب الواجب") || (hw == "لم يكتب" && hwStatus == "لم يكتب الواجب")
                                                        val bg = when {
                                                            isSel && (hw == "كتب") -> Color(0xFF0284C7)
                                                            isSel && (hw == "لم يكتب") -> Color(0xFFDC2626)
                                                            isSel && (hw == "متأخر") -> Color(0xFFD97706)
                                                            isSel && (hw == "لا يوجد") -> MaterialTheme.colorScheme.onSurfaceVariant
                                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                                        }
                                                        val fg = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(bg)
                                                                .border(1.dp, if (isSel) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                                                .clickable { onAttendanceChange(student.id, attStatus, hw, interStatus, recGrade, studentNote) }
                                                                .padding(vertical = 6.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = when(hw) {
                                                                    "كتب" -> "كتب"
                                                                    "لم يكتب" -> "لم يكتب"
                                                                    "متأخر" -> "متأخر"
                                                                    else -> "لا يوجد"
                                                                },
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = fg
                                                            )
                                                        }
                                                    }
                                                    // Undo / Clear Homework Button
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.errorContainer,
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .35f)),
                                                        modifier = Modifier.clickable {
                                                            onAttendanceChange(student.id, attStatus, "", interStatus, recGrade, studentNote)
                                                            Toast.makeText(context, "تم إلغاء تسجيل الواجب ", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Text("إلغاء ", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }

                                            // 3. Recitation (التسميع) with Clear/Reset Option
                                            if (selectedEvaluationTab == 0 || selectedEvaluationTab == 4) {
                                                Text("التسميع :", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4338CA))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    listOf("10/10", "9/10", "8/10", "5/10", "0/10").forEach { quickGrade ->
                                                        val isSel = recGrade == quickGrade
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                                .border(1.dp, if (isSel) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                                                .clickable { onAttendanceChange(student.id, attStatus, hwStatus, interStatus, quickGrade, studentNote) }
                                                                .padding(vertical = 6.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = quickGrade,
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                    // Undo / Clear Recitation Button
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.errorContainer,
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .35f)),
                                                        modifier = Modifier.clickable {
                                                            onAttendanceChange(student.id, attStatus, hwStatus, interStatus, "", studentNote)
                                                            Toast.makeText(context, "تم إلغاء درجة التسميع ", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Text("إلغاء ", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }

                                            // 4. Interaction (التفاعل) with Clear/Reset Option
                                            if (selectedEvaluationTab == 0 || selectedEvaluationTab == 5) {
                                                Text("التفاعل الصفي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    listOf("ممتاز", "جيد", "ضعيف").forEach { inter ->
                                                        val isSel = interStatus == inter
                                                        val bg = when {
                                                            isSel && inter == "ممتاز" -> Color(0xFF059669)
                                                            isSel && inter == "جيد" -> Color(0xFF0284C7)
                                                            isSel && inter == "ضعيف" -> Color(0xFFEA580C)
                                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                                        }
                                                        val fg = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(bg)
                                                                .border(1.dp, if (isSel) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                                                .clickable { onAttendanceChange(student.id, attStatus, hwStatus, inter, recGrade, studentNote) }
                                                                .padding(vertical = 6.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = when(inter) {
                                                                    "ممتاز" -> "ممتاز"
                                                                    "جيد" -> "جيد"
                                                                    else -> "ضعيف"
                                                                },
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = fg
                                                            )
                                                        }
                                                    }
                                                    // Undo / Clear Interaction Button
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.errorContainer,
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .35f)),
                                                        modifier = Modifier.clickable {
                                                            onAttendanceChange(student.id, attStatus, hwStatus, "", recGrade, studentNote)
                                                            Toast.makeText(context, "تم إلغاء تسجيل التفاعل ", Toast.LENGTH_SHORT).show()
                                                        }
                                                    ) {
                                                        Text("إلغاء ", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp))
                                                    }
                                                }
                                    }
                                }
                            }
                        }
                                }
                            }

                        GroupActiveTab.EXAMS -> {
                            // ==================== TAB 3: DEDICATED EXAMS TAB ====================
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "امتحانات واختبارات (${students.size}) ",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Search Bar for Exams
                            OutlinedTextField(
                                value = studentSearchQuery,
                                onValueChange = { studentSearchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("ابحث عن طالب لرصد درجات امتحانه...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (studentSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { studentSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Outlined.Close, contentDescription = "مسح", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (filteredStudents.isEmpty()) {
                                Text(
                                    text = if (students.isEmpty()) "لا يوجد طلاب مسجلين في هذه المجموعة حتى الآن." else "لا يوجد نتائج مطابقة للبحث.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                filteredStudents.forEach { student ->
                                    val examRecord = allExams.find { it.studentId == student.id }
                                    val hasGrades = examRecord != null && (
                                        !examRecord.exam1.isNullOrBlank() ||
                                        !examRecord.exam2.isNullOrBlank() ||
                                        !examRecord.exam3.isNullOrBlank() ||
                                        !examRecord.exam4.isNullOrBlank() ||
                                        !examRecord.exam5.isNullOrBlank()
                                    )

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    // Grade Entry Button
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.clickable { studentForExamDialog = student }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Outlined.EditNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("رصد الدرجات ", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                    }

                                                    // Parent Exam Report Button
                                                    if (hasGrades) {
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                                            modifier = Modifier.clickable {
                                                                val formattedDate = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale("ar")).format(java.util.Date())
                                                                val msg = buildString {
                                                                    append("✨ *تقرير نتائج الامتحانات والتقييمات* ✨\n\n")
                                                                    append(" *تقرير نتائج الامتحانات والتقييمات* \n\n")
                                                                    append("ولي أمر الطالب/ة المحترم: *${student.fullName}*\n")
                                                                    append("📚 *المجموعة:* ${group.name} (${group.subject})\n")
                                                                    append("🗓️ *التاريخ:* $formattedDate\n\n")
                                                                    append("📊 *درجات الامتحانات (من 100):*\n")
                                                                    if (!examRecord?.exam1.isNullOrBlank()) append("▫️ امتحان 1: ${examRecord?.exam1}/100\n")
                                                                    if (!examRecord?.exam2.isNullOrBlank()) append("▫️ امتحان 2: ${examRecord?.exam2}/100\n")
                                                                    if (!examRecord?.exam3.isNullOrBlank()) append("▫️ امتحان 3: ${examRecord?.exam3}/100\n")
                                                                    if (!examRecord?.exam4.isNullOrBlank()) append("▫️ امتحان 4: ${examRecord?.exam4}/100\n")
                                                                    append(" *المجموعة:* ${group.name} (${group.subject})\n")

                                                                    val scores = listOfNotNull(
                                                                        examRecord?.exam1?.toIntOrNull(),
                                                                        examRecord?.exam2?.toIntOrNull(),
                                                                        examRecord?.exam3?.toIntOrNull(),
                                                                        examRecord?.exam4?.toIntOrNull(),
                                                                        examRecord?.exam5?.toIntOrNull()
                                                                    )
                                                                    if (scores.isNotEmpty()) {
                                                                        val avg = scores.average().toInt()
                                                                        append("\n *المتوسط العام:* $avg%\n")
                                                                    }
                                                                    append("\nمع أطيب تحياتنا وتقديرنا \n")
                                                                    if (teacherName.isNotBlank()) append("أ/ $teacherName")
                                                                }
                                                                val phone = student.parentPhone.ifBlank { student.studentPhone }
                                                                studentForReportShare = Triple(student.fullName, phone, msg)
                                                            }
                                                        ) {
                                                            Text("تقرير لولي الأمر ", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                                                        }

                                                        // Clear Exam Grades Button
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = MaterialTheme.colorScheme.errorContainer,
                                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .35f)),
                                                            modifier = Modifier.clickable {
                                                                onSaveExam(student.id, null, null, null, null, null)
                                                                Toast.makeText(context, "تم إلغاء رصد الامتحانات ", Toast.LENGTH_SHORT).show()
                                                            }
                                                        ) {
                                                            Text("إلغاء الرصد ", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                                                        }
                                                    }
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = student.fullName, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (student.studentPhone.isNotBlank()) {
                                                        Text(text = student.studentPhone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }

                                            if (hasGrades) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (!examRecord?.exam1.isNullOrBlank()) {
                                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFECFDF5), border = BorderStroke(1.dp, Color(0xFFA7F3D0))) {
                                                            Text("امتحان 1: ${examRecord?.exam1}/100", fontSize = 11.sp, color = Color(0xFF065F46), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                                                        }
                                                    }
                                                    if (!examRecord?.exam2.isNullOrBlank()) {
                                                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                                                            Text("امتحان 2: ${examRecord?.exam2}/100", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                                                        }
                                                    }
                                                    if (!examRecord?.exam3.isNullOrBlank()) {
                                                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                                                            Text("امتحان 3: ${examRecord?.exam3}/100", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                                                        }
                                                    }
                                                    if (!examRecord?.exam4.isNullOrBlank()) {
                                                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f))) {
                                                            Text("امتحان 4: ${examRecord?.exam4}/100", fontSize = 11.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                                                        }
                                                    }
                                                    if (!examRecord?.exam5.isNullOrBlank()) {
                                                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .35f))) {
                                                            Text("امتحان 5: ${examRecord?.exam5}/100", fontSize = 11.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                                                        }
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("لم يتم رصد درجات امتحانات هذا الطالب بعد.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        null -> {}
                    }
                }
            }
        }
    }

    // Student Note Dialog ("ملاحظة")
    studentForNoteDialog?.let { student ->
        val currentRec = attendanceList.find { it.studentId == student.id }
        var noteText by remember { mutableStateOf(currentRec?.notes ?: "") }

        val notePresets = listOf(
            "متميز ومتفاعل جداً ",
            "تأخر عن موعد الحصة",
            "يحتاج لمراجعة الدرس السابق ",
        )

        PremiumAlertDialog(
            onDismissRequest = { studentForNoteDialog = null },
            title = {
                Text(
                    text = "تسجيل ملاحظة: ${student.fullName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اختر ملاحظة سريعة أو اكتب ملاحظتك:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    // Quick Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        notePresets.take(3).forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { noteText = preset }
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(4.dp),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        notePresets.drop(3).forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { noteText = preset }
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(4.dp),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("نص الملاحظة") },
                        placeholder = { Text("اكتب ملاحظتك على أداء الطالب هنا...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveNote(student.id, noteText)
                        studentForNoteDialog = null
                        Toast.makeText(context, "تم حفظ الملاحظة بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("حفظ الملاحظة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentForNoteDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Exam Dialog for Entering/Updating Student Exam Scores
    studentForExamDialog?.let { student ->
        com.example.ui.components.StudentExamGradeDialog(
            student = student,
            group = group,
            viewModel = viewModel,
            onDismiss = { studentForExamDialog = null }
        )
    }

    // Payment Remaining / Discount Dialog
    studentForPaymentDialog?.let { student ->
        var amountText by remember { mutableStateOf("") }

        PremiumAlertDialog(
            onDismissRequest = { studentForPaymentDialog = null },
            title = {
                Text(
                    if (paymentDialogMode == "DISCOUNT") "تطبيق خصم للطالب" else "تسجيل المبلغ المتبقي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "الطالب: ${student.fullName}\nقيمة الاشتراك الشهري: ${group.monthlyFee} ج.م",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(if (paymentDialogMode == "DISCOUNT") "المبلغ المطلوب بعد الخصم (ج.م)" else "المبلغ المتبقي على الطالب (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        onPaymentChange(student.id, "PARTIAL", amount)
                        studentForPaymentDialog = null
                        Toast.makeText(context, "تم تسجيل البيانات المالية للطالب بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("تأكيد وحفظ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentForPaymentDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Report Channel Selection Dialog (WhatsApp, WhatsApp Business, Telegram, Messenger, SMS)
    studentForReportShare?.let { (recipientName, phone, msg) ->
        com.example.ui.components.ReportChannelSelectionDialog(
            recipientName = recipientName,
            phoneNumber = phone,
            reportMessage = msg,
            onDismiss = { studentForReportShare = null }
        )
    }

    if (showUnpaidModal) {
        UnpaidStudentsModalDialog(
            group = group,
            students = students,
            paymentsForMonth = paymentsForMonth,
            onDismiss = { showUnpaidModal = false },
            onPaymentChange = onPaymentChange
        )
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, iconBgColor: Color, iconColor: Color, onClick: (() -> Unit)? = null) {
    Surface(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .68f)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionItem(title: String, icon: ImageVector, bgColor: Color, fgColor: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp, horizontal = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .65f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = fgColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}


private fun String?.isNull_or_empty(): Boolean = this == null || this.isBlank() || this == "بدون يوم ثاني"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(time: String, onTimeSelected: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val isEnglish = LocalLayoutDirection.current == LayoutDirection.Ltr
    
    val timeParts = time.split(":")
    val initialHour = if (timeParts.size == 2) {
        val hStr = timeParts[0]
        val minStrWithAmPm = timeParts[1]
        val amPmStr = minStrWithAmPm.split(" ").getOrNull(1) ?: ""
        val h = hStr.toIntOrNull() ?: 12
        when (amPmStr) {
            "م" -> if (h == 12) 12 else h + 12
            "ص" -> if (h == 12) 0 else h
            else -> h
        }
    } else 12
    
    val initialMinute = if (timeParts.size == 2) {
        timeParts[1].split(" ")[0].toIntOrNull() ?: 0
    } else 0
    
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable { showDialog = true },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = time,
                style = LocalTextStyle.current.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = SkyPrimary, modifier = Modifier.size(18.dp))
        }
    }
    
    if (showDialog) {
        PremiumAlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isEnglish) "Choose time" else "اختر الوقت", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    TimePicker(state = state)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    val isPm = state.hour >= 12
                    val displayHour = when {
                        state.hour == 0 -> 12
                        state.hour > 12 -> state.hour - 12
                        else -> state.hour
                    }
                    val amPm = if (isPm) "م" else "ص"
                    val hStr = displayHour.toString().padStart(2, '0')
                    val mStr = state.minute.toString().padStart(2, '0')
                    onTimeSelected("$hStr:$mStr $amPm")
                }) {
                    Text(if (isEnglish) "OK" else "موافق")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(if (isEnglish) "Cancel" else "إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupFormDialog(
    group: GroupEntity?,
    appLanguage: String = "ar",
    existingGroups: List<GroupEntity> = emptyList(),
    initialType: String = "CENTER",
    defaultSubject: String = "اللغة العربية",
    onDismiss: () -> Unit,
    onSave: (name: String, subject: String, day1: String, day2: String?, day3: String?, time1: String, time2: String?, time3: String?, fee: Double, groupType: String, paymentType: String, whatsappGroupUrl: String, notes: String, daysMap: Map<String, String>) -> Unit
) {
    fun label(ar: String, en: String) = if (appLanguage == "en") en else ar
    var name by remember { mutableStateOf(group?.name ?: "") }
    var selectedGroupType by remember { mutableStateOf(group?.groupType ?: initialType) }
    var selectedPaymentType by remember { mutableStateOf(group?.paymentType ?: "MONTHLY") }
    var selectedSubject by remember { mutableStateOf(group?.subject ?: defaultSubject) }
    var whatsappGroupUrl by remember { mutableStateOf(group?.whatsappGroupUrl ?: "") }
    var notes by remember { mutableStateOf(group?.notes ?: "") }
    var conflictMessage by remember { mutableStateOf<String?>(null) }
    var pendingSaveData by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    val allDays = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
    
    var selectedDays by remember { 
        mutableStateOf(
            allDays.filter { day ->
                group != null && group.meetsOnDay(day)
            }.toSet()
        )
    }
    
    var timeForDay by remember { 
        mutableStateOf(
            allDays.associateWith { day ->
                if (group != null && group.meetsOnDay(day)) group.getTimeSlotForDay(day)
                else "08:00 ص"
            }
        )
    }
    
    var feeText by remember { mutableStateOf(group?.monthlyFee?.toInt()?.toString() ?: "150") }

    PremiumAlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PremiumIconTile(
                    icon = if (group == null) Icons.Outlined.GroupAdd else Icons.Outlined.Edit,
                    contentDescription = null
                )
                Column {
                    Text(
                        text = if (group == null) label("إضافة مجموعة جديدة", "Add new group") else label("تعديل بيانات المجموعة", "Edit group"),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp
                    )
                    Text(
                        text = label("حدد نوع المجموعة وأيام الحضور والمواعيد بدقة", "Choose the group type, attendance days and times"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Location Type Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .7f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label("نوع مكان المجموعة", "Group location type"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        // Location Type Chips
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedGroupType == "CENTER",
                                onClick = { selectedGroupType = "CENTER" },
                                label = { Text(label("سنتر", "Center"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            FilterChip(
                                selected = selectedGroupType == "ONLINE",
                                onClick = { selectedGroupType = "ONLINE" },
                                label = { Text(label("أونلاين", "Online"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                            FilterChip(
                                selected = selectedGroupType == "PRIVATE",
                                onClick = { selectedGroupType = "PRIVATE" },
                                label = { Text(label("خاص", "Private"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }

                // Section 2: Group Name & Subject Input Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Class, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label("بيانات المجموعة والمادة", "Group and subject details"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(if (selectedGroupType == "PRIVATE") label("اسم الطالب أو المجموعة", "Student or group name") else label("اسم المجموعة", "Group name")) },
                            placeholder = { Text(label("مثال: مجموعة أ - الصف الثالث الإعدادي", "Example: Group A — Grade 9")) },
                            leadingIcon = { Icon(Icons.Outlined.Groups, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = premiumTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(label("المادة الدراسية الأساسية", "Main subject"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            DropdownMenuSelector(selectedSubject, SUBJECT_LIST) { selectedSubject = it }
                        }
                    }
                }

                // Section 3: Attendance Schedule Days Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .7f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label("أيام الحضور ومواعيد الحصص", "Attendance days and class times"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        allDays.forEach { day ->
                            val isChecked = selectedDays.contains(day)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f) else Color.Transparent,
                                border = BorderStroke(1.dp, if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = .45f) else MaterialTheme.colorScheme.outline.copy(alpha = .45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedDays = if (isChecked) selectedDays - day else selectedDays + day
                                            }
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedDays = if (checked) selectedDays + day else selectedDays - day
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                        )
                                        Text(if (appLanguage == "en") mapOf("السبت" to "Saturday", "الأحد" to "Sunday", "الإثنين" to "Monday", "الثلاثاء" to "Tuesday", "الأربعاء" to "Wednesday", "الخميس" to "Thursday", "الجمعة" to "Friday")[day] ?: day else day, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }

                                    if (isChecked) {
                                        Box(modifier = Modifier.weight(1.2f)) {
                                            TimePickerField(
                                                time = timeForDay[day] ?: "08:00 ص",
                                                onTimeSelected = { time ->
                                                    timeForDay = timeForDay.toMutableMap().apply { put(day, time) }
                                                }
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1.2f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Fees & WhatsApp Link Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AttachMoney, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label("الرسوم وتواصل المجموعة", "Fees and group contact"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        OutlinedTextField(
                            value = feeText,
                            onValueChange = { feeText = it },
                            label = { Text(if (selectedPaymentType == "MONTHLY") label("المبلغ الشهري (ج.م)", "Monthly fee (EGP)") else label("سعر الحصة الواحدة (ج.م)", "Session fee (EGP)")) },
                            leadingIcon = { Icon(Icons.Outlined.Payments, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = premiumTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = whatsappGroupUrl,
                            onValueChange = { whatsappGroupUrl = it },
                            label = { Text(label("رابط جروب الواتساب للمجموعة (اختياري)", "WhatsApp group link (optional)")) },
                            placeholder = { Text("https://chat.whatsapp.com/...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Send,
                                    contentDescription = null,
                                    tint = Color(0xFF25D366)
                                )
                            },
                            colors = premiumTextFieldColors(accent = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(label("ملاحظات خاصة بالمجموعة (اختياري)", "Group notes (optional)")) },
                            placeholder = { Text(label("أدخل أي ملاحظات هامة أو تعليمات للمجموعة...", "Add important notes or instructions...")) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.StickyNote2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = premiumTextFieldColors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val fee = feeText.toDoubleOrNull() ?: 150.0
                    
                    val activeDays = allDays.filter { selectedDays.contains(it) }
                    val daysMap = activeDays.associateWith { timeForDay[it] ?: "08:00 ص" }
                    
                    // Backward compatibility args
                    val d1 = activeDays.getOrNull(0) ?: "بدون"
                    val t1 = if (d1 != "بدون") daysMap[d1] ?: "08:00 ص" else "08:00 ص"
                    val d2 = activeDays.getOrNull(1) ?: "بدون"
                    val t2 = if (d2 != "بدون") daysMap[d2] ?: "08:00 ص" else "08:00 ص"
                    val d3 = activeDays.getOrNull(2) ?: "بدون"
                    val t3 = if (d3 != "بدون") daysMap[d3] ?: "08:00 ص" else "08:00 ص"
                    
                    val saveAction = {
                        onSave(name, selectedSubject, d1, d2, d3, t1, t2, t3, fee, selectedGroupType, selectedPaymentType, whatsappGroupUrl.trim(), notes.trim(), daysMap)
                    }

                    // Check for schedule conflict with another group on the same day and time
                    var conflictFound = false
                    for (other in existingGroups) {
                        if (other.id == group?.id) continue
                        for (day in activeDays) {
                            val chosenTime = daysMap[day]?.trim()
                            if (other.meetsOnDay(day) && other.getTimeSlotForDay(day).trim() == chosenTime) {
                                conflictFound = true
                                conflictMessage = " تنبيه تضارب في المواعيد:\nيوجد بالفعل مجموعة أخرى [${other.name}] في نفس الموعد ($day الساعة $chosenTime).\n\nهل ترغب في المتابعة وتأكيد الحفظ على أي حال؟"
                                pendingSaveData = saveAction
                                break
                            }
                        }
                        if (conflictFound) break
                    }

                    if (!conflictFound) {
                        saveAction()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label("حفظ المجموعة", "Save group"), fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(label("إلغاء", "Cancel"), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )

    // Schedule Conflict Warning Dialog (Approve / Reject)
    conflictMessage?.let { msg ->
        PremiumAlertDialog(
            onDismissRequest = {
                conflictMessage = null
                pendingSaveData = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Color(0xFFD97706))
                    Text(
                        text = "تأكيد تضارب الموعد",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val action = pendingSaveData
                        conflictMessage = null
                        pendingSaveData = null
                        action?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("موافق (حفظ على أي حال)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    conflictMessage = null
                    pendingSaveData = null
                }) {
                    Text("إلغاء (تعديل الموعد)", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun DropdownMenuSelector(selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selected,
                    style = LocalTextStyle.current.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ArrowDropUp else Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = SkyPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 260.dp)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = opt, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        ) 
                    },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun GroupCategoryActionCard(
    title: String,
    subtitle: String,
    badgeColor: Color,
    bgColor: Color,
    buttonLabel: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = badgeColor),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Text(
                    text = buttonLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun UnpaidStudentsModalDialog(
    group: GroupEntity,
    students: List<StudentEntity>,
    paymentsForMonth: List<com.example.data.PaymentRecordEntity>,
    onDismiss: () -> Unit,
    onPaymentChange: (studentId: Int, status: String, remaining: Double) -> Unit
) {
    val unpaidStudents = remember(students, paymentsForMonth) {
        students.filter { student ->
            val payRec = paymentsForMonth.find { it.studentId == student.id }
            payRec?.paymentStatus != "PAID"
        }
    }

    val context = LocalContext.current

    PremiumAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoneyOff,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "الطلاب الذين لم يدفعوا (${unpaidStudents.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B)
                    )
                    Text(
                        text = "مجموعة: ${group.name}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            if (unpaidStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ممتاز! جميع طلاب هذه المجموعة قاموا بدفع الاشتراكات! ",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(unpaidStudents, key = { it.id }) { student ->
                        val payRec = paymentsForMonth.find { it.studentId == student.id }
                        val status = payRec?.paymentStatus ?: "UNPAID"
                        val remaining = payRec?.remainingAmount ?: group.monthlyFee

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.fullName,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (status == "PARTIAL") "متبقي $remaining ج.م" else "لم يدفع (الاشتراك: ${group.monthlyFee.toInt()} ج.م)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB91C1C)
                                    )
                                    if (student.studentPhone.isNotBlank() || student.parentPhone.isNotBlank()) {
                                        Text(
                                            text = "هاتف ولي الأمر: ${student.parentPhone.ifBlank { student.studentPhone }}",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        onPaymentChange(student.id, "PAID", 0.0)
                                        Toast.makeText(context, "تم تسجيل دفع الطالب ${student.fullName}", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = "تم الدفع",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        }
    )
}

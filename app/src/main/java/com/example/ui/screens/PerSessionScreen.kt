package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AttendanceRecordEntity
import com.example.data.ExamRecordEntity
import com.example.data.GroupEntity
import com.example.data.PaymentRecordEntity
import com.example.data.StudentEntity
import com.example.data.meetsOnDay
import com.example.data.getTimeSlotForDay
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.SimpleDateFormat
import java.util.*

private val PurpleSessionPrimary = Color(0xFF7C3AED)
private val PurpleSessionBg = Color(0xFFEDE9FE)

private enum class PerSessionSubTab {
    GROUPS_AND_STUDENTS,
    FINANCIAL_ACCOUNTS
}

private enum class GroupInternalTab {
    STUDENTS,
    EVALUATION,
    EXAMS
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PerSessionScreen(viewModel: MainViewModel) {
    val context = LocalContext.current

    // Observe Database Flows
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val allExams by viewModel.allExams.collectAsStateWithLifecycle()
    val teacherName by viewModel.teacherName.collectAsStateWithLifecycle(initialValue = "")

    // Filter only groups marked as PER_SESSION (الدفع بالحصة)
    val perSessionGroups = remember(groups) {
        groups.filter { it.paymentType == "PER_SESSION" }
    }

    // Top Level Tab State: (مجموعات وطلاب الحصة vs السجل المالي)
    var mainSectionTab by remember { mutableStateOf(PerSessionSubTab.GROUPS_AND_STUDENTS) }

    // Date Picker State for Session Evaluation
    val cal = remember { Calendar.getInstance() }
    val todayDateString = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.time) }
    var selectedDateString by remember { mutableStateOf(todayDateString) }
    val selectedDateDisplay = remember(selectedDateString) {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ar")).format(
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(selectedDateString) ?: cal.time
        )
    }
    var selectedPaymentDate by remember { mutableStateOf<String?>(null) }

    // Month & Year Selector State for Financial Accounts
    var selectedYear by remember { mutableStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonthIndex by remember { mutableStateOf(cal.get(Calendar.MONTH)) }

    val paymentDatePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance()
                newCal.set(Calendar.YEAR, year)
                newCal.set(Calendar.MONTH, month)
                newCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedPaymentDate = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(newCal.time)
                selectedYear = year
                selectedMonthIndex = month
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    val monthYearStr = remember(selectedYear, selectedMonthIndex) {
        String.format(Locale.ENGLISH, "%04d-%02d", selectedYear, selectedMonthIndex + 1)
    }

    // Flows for current Date and Month
    val attendanceFlow = remember(selectedDateString) {
        viewModel.getAttendanceForDateFlow(selectedDateString)
    }
    val attendanceList by attendanceFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val paymentsFlow = remember(monthYearStr) {
        viewModel.getPaymentsForMonthFlow(monthYearStr)
    }
    val paymentsList by paymentsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Dialog States - Group & Student Management
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<GroupEntity?>(null) }
    var groupToDelete by remember { mutableStateOf<GroupEntity?>(null) }

    var groupToAddStudentIn by remember { mutableStateOf<GroupEntity?>(null) }
    var editingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }

    // Dialog States - Student Action Overlays
    var selectedStudentForDetail by remember { mutableStateOf<StudentEntity?>(null) }
    var selectedStudentForBarcode by remember { mutableStateOf<StudentEntity?>(null) }
    var selectedStudentForCard by remember { mutableStateOf<StudentEntity?>(null) }
    var studentForExamDialog by remember { mutableStateOf<StudentEntity?>(null) }

    // Dialog States - Group Action Overlays
    var groupForWhatsAppReports by remember { mutableStateOf<GroupEntity?>(null) }
    var groupForPdfReport by remember { mutableStateOf<GroupEntity?>(null) }
    var groupForMonthlyLog by remember { mutableStateOf<GroupEntity?>(null) }
    var currentGroupForScan by remember { mutableStateOf<GroupEntity?>(null) }
    var showGroupBarcodeScanner by remember { mutableStateOf(false) }

    // Dialog States - Financial
    var showFinancePdfDialog by remember { mutableStateOf(false) }
    var studentForFinancialOptions by remember { mutableStateOf<Pair<StudentEntity, Double>?>(null) }

    // Dialog State - Sharing report messages
    var studentForReportShare by remember { mutableStateOf<Triple<String, String, String>?>(null) } // Name, Phone, Message

    // Financial Filters
    var financeSelectedGroupId by remember { mutableStateOf<Int?>(null) }
    var financePaymentStatusFilter by remember { mutableStateOf("ALL") } // ALL, PAID, PARTIAL, UNPAID, EXEMPT
    var financeSearchQuery by remember { mutableStateOf("") }

    // Barcode scanner launcher
    val barcodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val rawCode = result.contents.trim()
            val cleanCode = rawCode.removePrefix("STU-").removePrefix("STUDENT_").removePrefix("STUDENT-").trim()
            val matchedStudent = allStudents.find {
                it.id.toString() == cleanCode ||
                        it.id.toString() == rawCode ||
                        it.fullName.equals(rawCode, ignoreCase = true)
            }
            if (matchedStudent != null) {
                viewModel.setAttendanceAndHomework(
                    studentId = matchedStudent.id,
                    date = selectedDateString,
                    attendanceStatus = "حاضر",
                    homeworkStatus = "حل",
                    recitationGrade = "10/10",
                    interactionStatus = "ممتاز",
                    notes = ""
                )
                Toast.makeText(context, " تم تسجيل حضور الطالب: ${matchedStudent.fullName}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, " لم يتم العثور على طالب مطابق للرمز: $rawCode", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Header Bar matching Screenshot
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Left action buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { showAddGroupDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = "إضافة مجموعة حصة",
                                tint = PurpleSessionPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (perSessionGroups.isNotEmpty()) {
                                    groupToAddStudentIn = perSessionGroups.first()
                                } else {
                                    showAddGroupDialog = true
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAddAlt1,
                                contentDescription = "إضافة طالب",
                                tint = PurpleSessionPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Right title & ticket icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "نظام الدفع بالحصة واليومي ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "إدارة المجموعات والطلاب والتقييم والمالية",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PurpleSessionBg,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.ConfirmationNumber,
                                    contentDescription = null,
                                    tint = PurpleSessionPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    }
                }

                // Sub-tabs Pills (مجموعات وطلاب الحصة  |  السجل المالي) matching screenshot
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    // Left Tab: السجل المالي
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (mainSectionTab == PerSessionSubTab.FINANCIAL_ACCOUNTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (mainSectionTab == PerSessionSubTab.FINANCIAL_ACCOUNTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                        shadowElevation = if (mainSectionTab == PerSessionSubTab.FINANCIAL_ACCOUNTS) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { mainSectionTab = PerSessionSubTab.FINANCIAL_ACCOUNTS }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "السجل المالي ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (mainSectionTab == PerSessionSubTab.FINANCIAL_ACCOUNTS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Right Tab: مجموعات وطلاب الحصة
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (mainSectionTab == PerSessionSubTab.GROUPS_AND_STUDENTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (mainSectionTab == PerSessionSubTab.GROUPS_AND_STUDENTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                        shadowElevation = if (mainSectionTab == PerSessionSubTab.GROUPS_AND_STUDENTS) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { mainSectionTab = PerSessionSubTab.GROUPS_AND_STUDENTS }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مجموعات وطلاب الحصة ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (mainSectionTab == PerSessionSubTab.GROUPS_AND_STUDENTS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (mainSectionTab) {
                PerSessionSubTab.GROUPS_AND_STUDENTS -> {
                    // =================================================================
                    // PART 1: GROUPS & STUDENTS MANAGEMENT (نظام المجموعات وإضافة الطلاب)
                    // =================================================================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (perSessionGroups.isEmpty()) {
                            // Exact Empty State matching screenshot
                            item {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 36.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Purple Bookmark Icon Card
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = PurpleSessionPrimary,
                                            modifier = Modifier.size(54.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Outlined.Bookmark,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "لا توجد مجموعات أو طلاب حصة حتى الآن",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = Color(0xFF1E1B4B),
                                            textAlign = TextAlign.Center
                                        )

                                        Text(
                                            text = "قم بإنشاء مجموعة جديدة باختيار الأيام ومواعيد عقارب\nالساعة وسعر الحصة وإضافة الطلاب بسهولة.",
                                            fontSize = 13.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 22.sp
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Button(
                                            onClick = { showAddGroupDialog = true },
                                            shape = RoundedCornerShape(24.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PurpleSessionPrimary),
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .height(48.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Add,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "إضافة مجموعة حصة",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            items(perSessionGroups, key = { it.id }) { group ->
                                val groupStudents = remember(allStudents, group.id) {
                                    allStudents.filter { it.groupId == group.id }
                                }

                                PerSessionGroupCard(
                                    group = group,
                                    students = groupStudents,
                                    selectedDate = selectedDateString,
                                    attendanceList = attendanceList,
                                    allExams = allExams,
                                    paymentsList = paymentsList,
                                    teacherName = teacherName,
                                    onEditGroup = { editingGroup = group },
                                    onDeleteGroup = { groupToDelete = group },
                                    onAddStudentClick = { groupToAddStudentIn = group },
                                    onEditStudent = { editingStudent = it },
                                    onDeleteStudent = { studentToDelete = it },
                                    onStudentDetail = { selectedStudentForDetail = it },
                                    onStudentBarcode = { selectedStudentForBarcode = it },
                                    onStudentCard = { selectedStudentForCard = it },
                                    onStudentExam = { studentForExamDialog = it },
                                    onAttendanceChange = { studentId, attStatus, hwStatus, interaction, recitation, notes ->
                                        viewModel.setAttendanceAndHomework(
                                            studentId = studentId,
                                            date = selectedDateString,
                                            attendanceStatus = attStatus,
                                            homeworkStatus = hwStatus,
                                            interactionStatus = interaction,
                                            recitationGrade = recitation,
                                            notes = notes
                                        )
                                    },
                                    onShareStudentReport = { name, phone, msg ->
                                        studentForReportShare = Triple(name, phone, msg)
                                    },
                                    onOpenGroupBatchReport = { groupForWhatsAppReports = group },
                                    onOpenMonthlyLog = { groupForMonthlyLog = group },
                                    onOpenPdfReport = { groupForPdfReport = group },
                                    onScanBarcodeClick = {
                                        currentGroupForScan = group
                                        showGroupBarcodeScanner = true
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(64.dp))
                        }
                    }
                }

                PerSessionSubTab.FINANCIAL_ACCOUNTS -> {
                    // =================================================================
                    // PART 2: FINANCIAL MANAGEMENT (قسم الحسابات والمالية للحصص)
                    // =================================================================
                    PerSessionFinancialView(
                        viewModel = viewModel,
                        groups = perSessionGroups,
                        allStudents = allStudents,
                        paymentsList = paymentsList,
                        monthYearStr = monthYearStr,
                        selectedYear = selectedYear,
                        selectedMonthIndex = selectedMonthIndex,
                        onMonthYearChange = { y, m ->
                            selectedYear = y
                            selectedMonthIndex = m
                        },
                        selectedGroupId = financeSelectedGroupId,
                        onGroupFilterChange = { financeSelectedGroupId = it },
                        selectedStatusFilter = financePaymentStatusFilter,
                        onStatusFilterChange = { financePaymentStatusFilter = it },
                        searchQuery = financeSearchQuery,
                        onSearchQueryChange = { financeSearchQuery = it },
                        teacherName = teacherName,
                        selectedPaymentDate = selectedPaymentDate,
                        onSelectPaymentDate = { paymentDatePickerDialog.show() },
                        onOpenPartialPayment = { student, fee ->
                            studentForFinancialOptions = student to fee
                        },
                        onOpenDiscountPayment = { _, _ -> },
                        onOpenPdfExport = { showFinancePdfDialog = true },
                        onShareReport = { name, phone, msg ->
                            studentForReportShare = Triple(name, phone, msg)
                        }
                    )
                }
            }
        }
    }

    // =========================================================================
    // DIALOGS SECTION
    // =========================================================================

    // 1. Add / Edit Group Dialog
    if (showAddGroupDialog || editingGroup != null) {
        val grp = editingGroup
        GroupFormDialog(
            group = grp,
            existingGroups = groups,
            initialType = "CENTER",
            defaultSubject = "اللغة العربية",
            onDismiss = {
                showAddGroupDialog = false
                editingGroup = null
            },
            onSave = { name, subject, day1, day2, day3, time1, time2, time3, fee, groupType, _, whatsappGroupUrl, notes, _ ->
                if (grp == null) {
                    viewModel.addGroup(
                        name = name,
                        subject = subject,
                        day1 = day1,
                        day2 = day2,
                        day3 = day3,
                        timeSlot = time1,
                        timeSlot2 = time2,
                        timeSlot3 = time3,
                        fee = fee,
                        groupType = groupType,
                        paymentType = "PER_SESSION", // Strictly Per Session
                        whatsappGroupUrl = whatsappGroupUrl,
                        notes = notes,
                        context = context
                    )
                    Toast.makeText(context, "تمت إضافة مجموعة الحصة بنجاح ", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateGroup(
                        group = grp.copy(
                            name = name,
                            subject = subject,
                            day1 = day1,
                            day2 = day2,
                            day3 = day3,
                            timeSlot = time1,
                            timeSlot2 = time2,
                            timeSlot3 = time3,
                            monthlyFee = fee,
                            groupType = groupType,
                            paymentType = "PER_SESSION",
                            whatsappGroupUrl = whatsappGroupUrl,
                            notes = notes
                        ),
                        context = context
                    )
                    Toast.makeText(context, "تم تحديث المجموعة بنجاح", Toast.LENGTH_SHORT).show()
                }
                showAddGroupDialog = false
                editingGroup = null
            }
        )
    }

    // 2. Delete Group Dialog
    groupToDelete?.let { grp ->
        com.example.ui.components.PremiumAlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("حذف المجموعة", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف مجموعة '${grp.name}'؟ سيتم حذف جميع الطلاب والسجلات المرتبطة بها نهائياً.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroup(grp)
                        groupToDelete = null
                        Toast.makeText(context, "تم حذف المجموعة", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 3. Add Student to Specific Group Dialog
    groupToAddStudentIn?.let { grp ->
        StudentFormDialog(
            student = null,
            groups = groups,
            initialGroupId = grp.id,
            onDismiss = { groupToAddStudentIn = null },
            onSave = { name, gId, pPhone, sPhone ->
                viewModel.addStudent(
                    fullName = name,
                    groupId = gId,
                    parentPhone = pPhone,
                    studentPhone = sPhone
                )
                groupToAddStudentIn = null
                Toast.makeText(context, "تمت إضافة الطالب للمجموعة بنجاح ", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 4. Edit Student Dialog
    editingStudent?.let { stu ->
        StudentFormDialog(
            student = stu,
            groups = groups,
            initialGroupId = stu.groupId,
            onDismiss = { editingStudent = null },
            onSave = { name, gId, pPhone, sPhone ->
                viewModel.updateStudent(
                    stu.copy(
                        fullName = name,
                        groupId = gId,
                        parentPhone = pPhone,
                        studentPhone = sPhone
                    )
                )
                editingStudent = null
                Toast.makeText(context, "تم تحديث بيانات الطالب", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 5. Delete Student Dialog
    studentToDelete?.let { stu ->
        com.example.ui.components.PremiumAlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("حذف الطالب", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف الطالب '${stu.fullName}' نهائياً؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudent(stu)
                        studentToDelete = null
                        Toast.makeText(context, "تم حذف الطالب", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 6. Student Detail Dialog
    selectedStudentForDetail?.let { stu ->
        val grp = groups.find { it.id == stu.groupId }
        StudentDetailDialog(
            student = stu,
            group = grp,
            viewModel = viewModel,
            onDismiss = { selectedStudentForDetail = null }
        )
    }

    // 7. Student Barcode QR Dialog
    selectedStudentForBarcode?.let { stu ->
        StudentBarcodeDialog(
            student = stu,
            onDismiss = { selectedStudentForBarcode = null }
        )
    }

    // 8. Student ID Card Dialog
    selectedStudentForCard?.let { stu ->
        StudentCardDialog(
            viewModel = viewModel,
            initialStudent = stu,
            onDismiss = { selectedStudentForCard = null }
        )
    }

    // 9. Student Exam Grades Dialog
    studentForExamDialog?.let { stu ->
        val grp = groups.find { it.id == stu.groupId }
        StudentExamGradeDialog(
            student = stu,
            group = grp,
            viewModel = viewModel,
            onDismiss = { studentForExamDialog = null }
        )
    }

    // 10. Group WhatsApp Reports Dialog
    groupForWhatsAppReports?.let { grp ->
        val grpStudents = allStudents.filter { it.groupId == grp.id }
        GroupWhatsAppReportsDialog(
            group = grp,
            students = grpStudents,
            attendanceList = attendanceList,
            teacherName = teacherName,
            todayDisplayDate = selectedDateDisplay,
            onDismiss = { groupForWhatsAppReports = null }
        )
    }

    // 11. Group Attendance PDF Dialog
    groupForPdfReport?.let { grp ->
        val grpStudents = allStudents.filter { it.groupId == grp.id }
        GroupAttendancePdfDialog(
            group = grp,
            students = grpStudents,
            viewModel = viewModel,
            onDismiss = { groupForPdfReport = null }
        )
    }

    // 12. Group Monthly Log Dialog
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

    // 13. Financial PDF Dialog
    if (showFinancePdfDialog) {
        val relStudents = remember(allStudents, perSessionGroups, financeSelectedGroupId) {
            val gIds = if (financeSelectedGroupId != null) setOf(financeSelectedGroupId!!) else perSessionGroups.map { it.id }.toSet()
            allStudents.filter { gIds.contains(it.groupId) }
        }
        var totalExp = 0.0
        var totalCol = 0.0
        var totalRem = 0.0
        var unpCount = 0

        relStudents.forEach { student ->
            val fee = perSessionGroups.find { it.id == student.groupId }?.monthlyFee ?: 100.0
            totalExp += fee
            val p = paymentsList.find { it.studentId == student.id }
            if (p != null) {
                totalCol += p.paidAmount
                totalRem += p.remainingAmount
                if (p.paymentStatus != "PAID" && p.paymentStatus != "EXEMPT") unpCount++
            } else {
                totalRem += fee
                unpCount++
            }
        }

        val monthName = ARABIC_MONTHS.find { it.second == selectedMonthIndex }?.first ?: "الشهر الحالي"
        val groupTitle = if (financeSelectedGroupId != null) perSessionGroups.find { it.id == financeSelectedGroupId }?.name ?: "المجموعة" else "جميع مجموعات الحصة"

        FinancePdfDialog(
            selectedGroupName = groupTitle,
            monthName = monthName,
            year = selectedYear,
            students = relStudents,
            groups = perSessionGroups,
            paymentsList = paymentsList,
            totalCollected = totalCol,
            totalExpected = totalExp,
            totalRemaining = totalRem,
            unpaidCount = unpCount,
            onDismiss = { showFinancePdfDialog = false }
        )
    }

    // 14. Unified Financial Options Dialog (دفع، لم يدفع، معفي، متبقي، خصم)
    studentForFinancialOptions?.let { (student, fee) ->
        var selectedOption by remember { mutableStateOf("PAID") } // "PAID", "UNPAID", "EXEMPT", "PARTIAL", "DISCOUNT"
        var remainingInput by remember { mutableStateOf("") }
        var discountInput by remember { mutableStateOf("") }

        com.example.ui.components.PremiumAlertDialog(
            onDismissRequest = { studentForFinancialOptions = null },
            title = {
                Text(
                    text = "إدارة الحساب المالي للطالب ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "الطالب: ${student.fullName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "سعر الحصة الأساسي: ${fee.toInt()} ج.م",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "يوم الدفع: ${selectedPaymentDate ?: "اختر اليوم أولاً"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "اختر الحالة المالية للحصة:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Options Grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            Triple("PAID", "دفع كامل ✓", Color(0xFF10B981)),
                            Triple("UNPAID", "لم يدفع", Color(0xFFEF4444)),
                            Triple("EXEMPT", "معفي", Color(0xFF64748B)),
                            Triple("PARTIAL", "متبقي عليه", Color(0xFFF59E0B)),
                            Triple("DISCOUNT", "خصم ", Color(0xFF3B82F6))
                        ).chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { (opt, label, color) ->
                                    val isSel = selectedOption == opt
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSel) color else MaterialTheme.colorScheme.surface)
                                            .border(
                                                1.dp,
                                                if (isSel) Color.Transparent else MaterialTheme.colorScheme.outline,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedOption = opt }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Conditional Inputs
                    if (selectedOption == "PARTIAL") {
                        OutlinedTextField(
                            value = remainingInput,
                            onValueChange = { remainingInput = it },
                            label = { Text("المبلغ المتبقي (ج.م)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    if (selectedOption == "DISCOUNT") {
                        OutlinedTextField(
                            value = discountInput,
                            onValueChange = { discountInput = it },
                            label = { Text("قيمة الخصم (ج.م)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paymentDay = selectedPaymentDate
                            ?: SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
                        when (selectedOption) {
                            "PAID" -> {
                                viewModel.setPaymentStatusWithDiscount(
                                    studentId = student.id,
                                    status = "PAID",
                                    discountAmount = 0.0,
                                    paidAmount = fee,
                                    remainingAmount = 0.0,
                                    paymentDate = paymentDay,
                                    monthYear = monthYearStr
                                )
                            }
                            "UNPAID" -> {
                                viewModel.setPaymentStatusWithDiscount(
                                    studentId = student.id,
                                    status = "UNPAID",
                                    discountAmount = 0.0,
                                    paidAmount = 0.0,
                                    remainingAmount = fee,
                                    paymentDate = paymentDay,
                                    monthYear = monthYearStr
                                )
                            }
                            "EXEMPT" -> {
                                viewModel.setPaymentStatusWithDiscount(
                                    studentId = student.id,
                                    status = "EXEMPT",
                                    discountAmount = 0.0,
                                    paidAmount = 0.0,
                                    remainingAmount = 0.0,
                                    paymentDate = paymentDay,
                                    monthYear = monthYearStr
                                )
                            }
                            "PARTIAL" -> {
                                val remVal = remainingInput.toDoubleOrNull() ?: 0.0
                                val paidVal = (fee - remVal).coerceAtLeast(0.0)
                                viewModel.setPaymentStatusWithDiscount(
                                    studentId = student.id,
                                    status = if (remVal <= 0) "PAID" else "PARTIAL",
                                    discountAmount = 0.0,
                                    paidAmount = paidVal,
                                    remainingAmount = remVal,
                                    paymentDate = paymentDay,
                                    monthYear = monthYearStr
                                )
                            }
                            "DISCOUNT" -> {
                                val discVal = discountInput.toDoubleOrNull() ?: 0.0
                                val paidVal = (fee - discVal).coerceAtLeast(0.0)
                                viewModel.setPaymentStatusWithDiscount(
                                    studentId = student.id,
                                    status = if (paidVal <= 0) "EXEMPT" else "PAID",
                                    discountAmount = discVal,
                                    paidAmount = paidVal,
                                    remainingAmount = 0.0,
                                    paymentDate = paymentDay,
                                    monthYear = monthYearStr
                                )
                            }
                        }
                        studentForFinancialOptions = null
                        Toast.makeText(context, "تم حفظ الحالة المالية بنجاح! ", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("حفظ", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentForFinancialOptions = null }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // 16. Camera Barcode Scanner Modal
    if (showGroupBarcodeScanner) {
        BarcodeScannerModal(
            title = "مسح كود حضور الحصة",
            onDismiss = { showGroupBarcodeScanner = false },
            onScanResult = { rawCode ->
                val cleanCode = rawCode.trim().removePrefix("STU-").removePrefix("STUDENT_").removePrefix("STUDENT-").trim()
                val matched = allStudents.find {
                    it.id.toString() == cleanCode ||
                            it.id.toString() == rawCode ||
                            it.fullName.equals(rawCode.trim(), ignoreCase = true)
                }
                if (matched != null) {
                    viewModel.setAttendanceAndHomework(
                        studentId = matched.id,
                        date = selectedDateString,
                        attendanceStatus = "حاضر",
                        homeworkStatus = "حل",
                        recitationGrade = "10/10",
                        interactionStatus = "ممتاز",
                        notes = ""
                    )
                    Pair(true, "تم تسجيل حضور: ${matched.fullName} ")
                } else {
                    Pair(false, "لم يتم العثور على طالب بالرمز: $rawCode")
                }
            }
        )
    }

    // 17. Report Sharing Dialog
    studentForReportShare?.let { (name, phone, msg) ->
        ReportChannelSelectionDialog(
            recipientName = name,
            phoneNumber = phone,
            reportMessage = msg,
            onDismiss = { studentForReportShare = null }
        )
    }
}

// =============================================================================
// PER-SESSION GROUP CARD (بطاقة المجموعة مع إدارة الطلاب والتقييم)
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PerSessionGroupCard(
    group: GroupEntity,
    students: List<StudentEntity>,
    selectedDate: String,
    attendanceList: List<AttendanceRecordEntity>,
    allExams: List<ExamRecordEntity>,
    paymentsList: List<PaymentRecordEntity>,
    teacherName: String,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onAddStudentClick: () -> Unit,
    onEditStudent: (StudentEntity) -> Unit,
    onDeleteStudent: (StudentEntity) -> Unit,
    onStudentDetail: (StudentEntity) -> Unit,
    onStudentBarcode: (StudentEntity) -> Unit,
    onStudentCard: (StudentEntity) -> Unit,
    onStudentExam: (StudentEntity) -> Unit,
    onAttendanceChange: (studentId: Int, attStatus: String, hwStatus: String, interaction: String, recitation: String, notes: String) -> Unit,
    onShareStudentReport: (name: String, phone: String, msg: String) -> Unit,
    onOpenGroupBatchReport: () -> Unit,
    onOpenMonthlyLog: () -> Unit,
    onOpenPdfReport: () -> Unit,
    onScanBarcodeClick: () -> Unit
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf<GroupInternalTab?>(GroupInternalTab.STUDENTS) }
    var selectedEvaluationFilter by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val groupAccent = remember(group.id) {
        listOf(
            Color(0xFF27569A), Color(0xFF16855B), Color(0xFFB7791F),
            Color(0xFF147D84), Color(0xFF6B5A9A), Color(0xFF9B4F62)
        )[kotlin.math.abs(group.id) % 6]
    }

    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else students.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) ||
                    it.parentPhone.contains(searchQuery) ||
                    it.studentPhone.contains(searchQuery)
        }
    }

    // Financial Stats
    val groupPayments = paymentsList.filter { p -> students.any { it.id == p.studentId } }
    val studentIds = students.mapTo(mutableSetOf()) { it.id }
    val paidStudentIds = groupPayments.asSequence()
        .filter { it.paymentStatus == "PAID" && it.studentId in studentIds }
        .map { it.studentId }
        .toSet()
    val totalPaid = paidStudentIds.size
    val totalNotPaid = (students.size - totalPaid).coerceAtLeast(0)
    val expectedIncome = students.size * group.monthlyFee
    val collectedAmount = groupPayments.filter { it.paymentStatus == "PAID" }.sumOf { it.paidAmount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, groupAccent.copy(alpha = .48f)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Type Badge + Quick Actions
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                // Left: Delete & Edit
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onDeleteGroup, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onEditGroup, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                    }
                }

                // Right: Name + Type
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = if (group.groupType == "ONLINE") "أونلاين" else "سنتر",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = group.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val monthlySessions = listOf(group.day1, group.day2, group.day3, group.day4, group.day5, group.day6, group.day7)
                        .count { !it.isNullOrBlank() && it != "بدون" }
                        .let { (it * 4).coerceAtLeast(1) }
                    Text(
                        text = "$monthlySessions حصة شهرياً",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "المادة: ${group.subject.ifBlank { "غير محدد" }}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Stats Card (Students, Paid, Not Paid)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatColumn(label = "عدد الطلاب", value = "${students.size}", color = MaterialTheme.colorScheme.onSurface)
                    VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.outline)
                    StatColumn(label = "تم الدفع", value = "$totalPaid", color = Color(0xFF16A34A))
                    VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.outline)
                    StatColumn(label = "لم يدفع ", value = "$totalNotPaid", color = Color(0xFFEF4444))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Action Buttons (Parent, Group, Attendance PDF, Monthly Log)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionWideButton(
                        text = "إرسال ولي الأمر",
                        icon = Icons.Outlined.Person,
                        color = Color(0xFFA855F7),
                        bgColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenGroupBatchReport
                    )
                    ActionWideButton(
                        text = "إرسال على الجروب دفعة",
                        icon = Icons.Outlined.Groups,
                        color = Color(0xFF16A34A),
                        bgColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenGroupBatchReport
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionWideButton(
                        text = "سجل المتابعة الشهري",
                        icon = Icons.Outlined.CalendarMonth,
                        color = Color(0xFF3B82F6),
                        bgColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenMonthlyLog
                    )
                    ActionWideButton(
                        text = "كشف الحضور PDF",
                        icon = Icons.Outlined.PictureAsPdf,
                        color = Color(0xFF7C3AED),
                        bgColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenPdfReport
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Grid (Enrolled, Price, Expected, Collected)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoSmallCard(label = "الطلاب المسجلين", value = "${students.size} طالب", icon = Icons.Outlined.Groups, color = Color(0xFF3B82F6), modifier = Modifier.weight(1f))
                    InfoSmallCard(label = "سعر الاشتراك", value = "${group.monthlyFee.toInt()} ج.م", icon = Icons.Outlined.Payments, color = Color(0xFF16A34A), modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoSmallCard(label = "الدخل المتوقع", value = "${expectedIncome.toInt()} ج.م", icon = Icons.Outlined.AccountBalanceWallet, color = Color(0xFF7C3AED), modifier = Modifier.weight(1f))
                    InfoSmallCard(label = "المبلغ المحصل", value = "${collectedAmount.toInt()} ج.م", icon = Icons.Outlined.AccountBalanceWallet, color = Color(0xFFEA580C), modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Circular Actions
            Text("إجراءات سريعة", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularActionItem(icon = Icons.Outlined.Edit, label = "تعديل", color = Color(0xFFEF4444), onClick = onEditGroup)
                CircularActionItem(icon = Icons.Outlined.Person, label = "الطلاب", color = Color(0xFFF97316), onClick = { activeSubTab = GroupInternalTab.STUDENTS })
                CircularActionItem(icon = Icons.Outlined.Rule, label = "تقييم", color = Color(0xFF3B82F6), onClick = { activeSubTab = GroupInternalTab.EVALUATION })
                CircularActionItem(icon = Icons.Outlined.Assignment, label = "امتحانات", color = Color(0xFF8B5CF6), onClick = { activeSubTab = GroupInternalTab.EXAMS })
                CircularActionItem(icon = Icons.Outlined.QrCodeScanner, label = "مسح سريع", color = Color(0xFF10B981), onClick = onScanBarcodeClick)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Session Timings
            Text("مواعيد الحصص", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val days = listOfNotNull(group.day1, group.day2, group.day3, group.day4, group.day5, group.day6, group.day7)
                    .filter { it.isNotBlank() && !it.startsWith("بدون") }
                
                days.forEach { day ->
                    TimingBadge(day = day, time = group.timeSlot)
                }
            }

            // INTERNAL TAB BODY CONTENT
            AnimatedVisibility(visible = activeSubTab != null) {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    when (activeSubTab) {
                        GroupInternalTab.STUDENTS -> {
                            // Students List Implementation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("قائمة طلاب المجموعة (${filteredStudents.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                TextButton(onClick = onAddStudentClick) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF7C3AED))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("إضافة طالب جديد", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                                }
                            }

                            if (students.isNotEmpty()) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("ابحث عن طالب بالاسم أو الهاتف...", fontSize = 11.5.sp) },
                                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    trailingIcon = {
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Outlined.Close, contentDescription = "مسح", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedBorderColor = Color(0xFF7C3AED),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (filteredStudents.isEmpty()) {
                                // Empty View
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    filteredStudents.forEach { student ->
                                        PerSessionStudentItemCard(
                                            student = student,
                                            onEdit = { onEditStudent(student) },
                                            onDelete = { onDeleteStudent(student) },
                                            onDetail = { onStudentDetail(student) },
                                            onBarcode = { onStudentBarcode(student) },
                                            onCard = { onStudentCard(student) }
                                        )
                                    }
                                }
                            }
                        }
                        GroupInternalTab.EVALUATION -> {
                            // Evaluation Implementation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("رصد تقييم الحصة ", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                    Text(
                                        text = " تم إلغاء حصة اليوم للمجموعة",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Evaluation Category Filter Bar
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val evalFilters = listOf("الكل", "الحضور", "الواجب", "التسميع", "التفاعل")
                                items(evalFilters.indices.toList()) { idx ->
                                    val isSelected = selectedEvaluationFilter == idx
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFF3B82F6) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable { selectedEvaluationFilter = idx }
                                    ) {
                                        Text(
                                            text = evalFilters[idx],
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                students.forEach { student ->
                                    val rec = attendanceList.find { it.studentId == student.id }
                                    PerSessionEvaluationItem(
                                        student = student,
                                        group = group,
                                        selectedFilter = selectedEvaluationFilter,
                                        attStatus = when (rec?.attendanceStatus) {
                                            "حضر", "حاضر", "PRESENT" -> "حضر"
                                            "غائب", "لم يحضر", "ABSENT" -> "لم يحضر"
                                            "متأخر" -> "متأخر"
                                            "تم الغاء حصة اليوم", "تم إلغاء الحصة", "إلغاء" -> "إلغاء"
                                            else -> ""
                                        },
                                        hwStatus = when (rec?.homeworkStatus) {
                                            "كتب الواجب", "كتب" -> "كتب"
                                            "لم يكتب الواجب", "لم يكتب" -> "لم يكتب"
                                            "متأخر" -> "متأخر"
                                            "لا يوجد" -> "لا يوجد"
                                            else -> ""
                                        },
                                        interStatus = rec?.interactionStatus ?: "",
                                        recGrade = rec?.recitationGrade ?: "",
                                        note = rec?.notes ?: "",
                                        teacherName = teacherName,
                                        onUpdate = { a, h, i, r, n -> onAttendanceChange(student.id, a, h, i, r, n) },
                                        onShareReport = onShareStudentReport
                                    )
                                }
                            }
                        }
                        GroupInternalTab.EXAMS -> {
                             // Exams View Implementation
                             students.forEach { student ->
                                val exam = allExams.find { it.studentId == student.id }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(student.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text("درجات الاختبارات الشهرية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Button(
                                            onClick = { onStudentExam(student) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                                        ) {
                                            Text("رصد", fontSize = 11.sp)
                                        }
                                    }
                                    }
                                }
                             }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun ActionWideButton(text: String, icon: ImageVector, color: Color, bgColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, color.copy(alpha = .22f)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(7.dp))
                Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun InfoSmallCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CircularActionItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.size(48.dp).clickable(onClick = onClick),
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimingBadge(day: String, time: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(text = time, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
            Text(text = day, fontSize = 9.5.sp, color = Color(0xFF1E40AF))
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(11.dp))
        }
    }
}


// =============================================================================
// STUDENT ITEM CARD INSIDE GROUP (بطاقة الطالب داخل المجموعة)
// =============================================================================

@Composable
private fun PerSessionStudentItemCard(
    student: StudentEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDetail: () -> Unit,
    onBarcode: () -> Unit,
    onCard: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = SkyPrimary, modifier = Modifier.size(20.dp))
                }

                Column {
                    Text(
                        text = student.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = student.parentPhone.ifBlank { "بدون هاتف" },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (student.parentPhone.isNotBlank()) {
                            // Quick Call
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = "اتصال",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier
                                    .size(13.dp)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${student.parentPhone}"))
                                        context.startActivity(intent)
                                    }
                            )
                        }
                    }
                }
            }

            // Quick Student Action Icons
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onDetail, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.Visibility, contentDescription = "تفاصيل", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onCard, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.Badge, contentDescription = "كارت الطالب", tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onBarcode, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.QrCode, contentDescription = "باركود", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = "تعديل", tint = SkyPrimary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
            }
        }
        }
    }
}

// =============================================================================
// PER-SESSION EVALUATION ITEM (رصد تفصيلي للحصة لكل طالب)
// =============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PerSessionEvaluationItem(
    student: StudentEntity,
    group: GroupEntity,
    selectedFilter: Int,
    attStatus: String,
    hwStatus: String,
    interStatus: String,
    recGrade: String,
    note: String,
    teacherName: String,
    onUpdate: (att: String, hw: String, inter: String, rec: String, note: String) -> Unit,
    onShareReport: (name: String, phone: String, msg: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Student Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header action buttons (WhatsApp single report)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                                if (note.isNotBlank()) append("\n📝 *ملاحظة المعلم:* $note\n")
                                append(" *المجموعة:* ${group.name} (${group.subject})\n")
                                if (teacherName.isNotBlank()) append("أ/ $teacherName")
                            }
                            val phone = student.parentPhone.ifBlank { student.studentPhone }
                            onShareReport(student.fullName, phone, msg)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Outlined.Send, contentDescription = "إرسال تقرير الطالب", tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                    }
                }

                // Student details (Name & Phone) on the right
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

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Attendance (الحضور) with Clear/Reset Option
            if (selectedFilter == 0 || selectedFilter == 1) {
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
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val fg = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(1.dp, if (isSel) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { onUpdate(st, hwStatus, interStatus, recGrade, note) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when(st) {
                                    "حضر" -> "حضر"
                                    "متأخر" -> "متأخر"
                                    "لم يحضر" -> "لم يحضر"
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
                            onUpdate("", hwStatus, interStatus, recGrade, note)
                            android.widget.Toast.makeText(context, "تم إلغاء تسجيل الحضور ", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("إلغاء ", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 2. Homework (الواجب) with Clear/Reset Option
            if (selectedFilter == 0 || selectedFilter == 2) {
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
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val fg = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(1.dp, if (isSel) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { onUpdate(attStatus, hw, interStatus, recGrade, note) }
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
                            onUpdate(attStatus, "", interStatus, recGrade, note)
                            android.widget.Toast.makeText(context, "تم إلغاء تسجيل الواجب ", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("إلغاء ", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 3. Recitation (التسميع) with Clear/Reset Option
            if (selectedFilter == 0 || selectedFilter == 3) {
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
                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (isSel) Color.Transparent else Color(0xFFE0E7FF), RoundedCornerShape(8.dp))
                                .clickable { onUpdate(attStatus, hwStatus, interStatus, quickGrade, note) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = quickGrade,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    // Undo / Clear Recitation Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .35f)),
                        modifier = Modifier.clickable {
                            onUpdate(attStatus, hwStatus, interStatus, "", note)
                            android.widget.Toast.makeText(context, "تم إلغاء درجة التسميع ", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("إلغاء ", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 4. Interaction (التفاعل) with Clear/Reset Option
            if (selectedFilter == 0 || selectedFilter == 4) {
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
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val fg = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(1.dp, if (isSel) Color.Transparent else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { onUpdate(attStatus, hwStatus, inter, recGrade, note) }
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
                            onUpdate(attStatus, hwStatus, "", recGrade, note)
                            android.widget.Toast.makeText(context, "تم إلغاء تسجيل التفاعل ", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("إلغاء ", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 5. Notes field at the bottom
            OutlinedTextField(
                value = note,
                onValueChange = { onUpdate(attStatus, hwStatus, interStatus, recGrade, it) },
                placeholder = { Text("أضف ملاحظة خاصة لهذا الطالب...", fontSize = 11.5.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
private fun EvaluationRow(
    label: String,
    options: List<String>,
    currentValue: String,
    onSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { opt ->
                val isSelected = currentValue == opt
                EvaluationButton(
                    text = opt,
                    isSelected = isSelected,
                    onClick = { onSelect(opt) }
                )
            }
            // Clear Button
            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Refresh, contentDescription = "مسح", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun EvaluationButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.onSurface
        text == "حضر" || text == "حل" || text == "ممتاز" || text.contains("10/10") -> Color(0xFFDCFCE7)
        text == "غائب" || text == "لم يحل" || text == "ضعيف" || text.contains("0/10") -> Color(0xFFFEE2E2)
        text == "متأخر" -> Color(0xFFFEF3C7)
        text == "ناقص" || text == "جيد" -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val fgColor = when {
        isSelected -> Color.White
        text == "حضر" || text == "حل" || text == "ممتاز" -> Color(0xFF15803D)
        text == "غائب" || text == "لم يحل" || text == "ضعيف" -> Color(0xFFB91C1C)
        text == "متأخر" -> Color(0xFFB45309)
        text == "ناقص" || text == "جيد" -> Color(0xFF1D4ED8)
        text == "إلغاء" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant) else null
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = fgColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}


// =============================================================================
// PER-SESSION FINANCIAL VIEW (تبويب حسابات ومالية الحصة)
// =============================================================================

@Composable
private fun FinancialActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerSessionFinancialView(
    viewModel: MainViewModel,
    groups: List<GroupEntity>,
    allStudents: List<StudentEntity>,
    paymentsList: List<PaymentRecordEntity>,
    monthYearStr: String,
    selectedYear: Int,
    selectedMonthIndex: Int,
    onMonthYearChange: (y: Int, m: Int) -> Unit,
    selectedGroupId: Int?,
    onGroupFilterChange: (Int?) -> Unit,
    selectedStatusFilter: String,
    onStatusFilterChange: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    teacherName: String,
    selectedPaymentDate: String?,
    onSelectPaymentDate: () -> Unit,
    onOpenPartialPayment: (StudentEntity, Double) -> Unit,
    onOpenDiscountPayment: (StudentEntity, Double) -> Unit,
    onOpenPdfExport: () -> Unit,
    onShareReport: (name: String, phone: String, msg: String) -> Unit
) {
    val context = LocalContext.current
    // Relevant Students based on groups and group filter
    val relevantStudents = remember(allStudents, groups, selectedGroupId) {
        // Resolve students by their group id; if the filtered group flow is still loading,
        // keep the finance screen populated instead of showing a misleading empty state.
        val scoped = if (selectedGroupId != null) {
            allStudents.filter { it.groupId == selectedGroupId }
        } else if (groups.isNotEmpty()) {
            val groupIds = groups.map { it.id }.toSet()
            allStudents.filter { it.groupId in groupIds }
        } else emptyList()
        scoped.ifEmpty { allStudents }
    }

    // Filtered Students matching query and payment status
    val selectedDayPayments = remember(paymentsList, selectedPaymentDate) {
        if (selectedPaymentDate == null) emptyList()
        else paymentsList.filter { it.paymentDate == selectedPaymentDate }
    }

    val filteredStudents = remember(relevantStudents, searchQuery, selectedStatusFilter, selectedDayPayments, selectedPaymentDate) {
        // Students remain visible before a date is chosen; only the payment records
        // stay empty until the teacher selects a day from the calendar.
        relevantStudents.filter { student ->
            val matchesQuery = searchQuery.isBlank() ||
                    student.fullName.contains(searchQuery, ignoreCase = true) ||
                    student.parentPhone.contains(searchQuery)

            val p = selectedDayPayments.find { it.studentId == student.id }
            val status = p?.paymentStatus ?: "UNPAID"

            val matchesStatus = when (selectedStatusFilter) {
                "PAID" -> status == "PAID"
                "PARTIAL" -> status == "PARTIAL"
                "UNPAID" -> status == "UNPAID" || p == null
                "EXEMPT" -> status == "EXEMPT"
                else -> true
            }

            matchesQuery && matchesStatus
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                onClick = onSelectPaymentDate,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "اختر يوم الدفع من التقويم",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = selectedPaymentDate ?: "لم يتم اختيار يوم بعد",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Summary Card with a compact savings-bank treatment for Per Session
        item {
            val selectedStudentIds = relevantStudents.mapTo(mutableSetOf()) { it.id }
            val visiblePayments = selectedDayPayments.filter { it.studentId in selectedStudentIds }
            val totalExp = visiblePayments.sumOf { payment ->
                val student = relevantStudents.find { it.id == payment.studentId }
                groups.find { it.id == student?.groupId }?.monthlyFee ?: 0.0
            }
            val totalCol = visiblePayments.sumOf { it.paidAmount }
            val totalRem = visiblePayments.sumOf { it.remainingAmount.coerceAtLeast(0.0) }
            val monthName = ARABIC_MONTHS.find { it.second == selectedMonthIndex }?.first ?: "الشهر"
            
            FinancialSavingsBankCard(
                totalCollected = totalCol,
                totalExpected = totalExp,
                totalRemaining = totalRem,
                monthName = monthName,
                year = selectedYear,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Quick Pay All & Summary Header matching Image 2 style
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text("السجل المالي ", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                
                Text("العدد: ${filteredStudents.size} طالب", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Search Bar matching Image 2
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("بحث عن طالب بالاسم أو رقم الهاتف...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                singleLine = true
            )
        }

        // Filters matching Image 2
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // ALL Badge
                item {
                    val isSel = selectedStatusFilter == "ALL"
                    FilterBadge(
                        label = "الكل (${filteredStudents.size})",
                        isSelected = isSel,
                        color = MaterialTheme.colorScheme.onSurface,
                        onClick = { onStatusFilterChange("ALL") }
                    )
                }
                // PAID Badge
                item {
                    val isSel = selectedStatusFilter == "PAID"
                    FilterBadge(
                        label = "المسددين",
                        isSelected = isSel,
                        color = Color(0xFF10B981),
                        hasDot = true,
                        onClick = { onStatusFilterChange("PAID") }
                    )
                }
                // UNPAID Badge
                item {
                    val isSel = selectedStatusFilter == "UNPAID"
                    FilterBadge(
                        label = "غير المسددين",
                        isSelected = isSel,
                        color = Color(0xFFEF4444),
                        hasDot = true,
                        onClick = { onStatusFilterChange("UNPAID") }
                    )
                }
                // PARTIAL Badge
                item {
                    val isSel = selectedStatusFilter == "PARTIAL"
                    FilterBadge(
                        label = "متبقي عليهم",
                        isSelected = isSel,
                        color = Color(0xFFF59E0B),
                        hasDot = true,
                        onClick = { onStatusFilterChange("PARTIAL") }
                    )
                }
            }
        }

        // Records List matching Image 2
        if (filteredStudents.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (selectedPaymentDate == null) "اختر يوم الدفع من التقويم لبدء السجل" else "لا يوجد طلاب مطابقون للبحث",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(filteredStudents, key = { it.id }) { student ->
                val grp = groups.find { it.id == student.groupId }
                val sessionFee = grp?.monthlyFee ?: 0.0
                val pRecord = selectedDayPayments.find { it.studentId == student.id }
                val status = pRecord?.paymentStatus ?: "UNPAID"
                val remainingAmount = if (pRecord != null && status != "UNPAID") pRecord.remainingAmount else sessionFee

                Surface(
                    onClick = { onOpenPartialPayment(student, sessionFee) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            // Left Side: Payment Info
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (status == "UNPAID" || status == "PARTIAL") Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (status == "UNPAID" || status == "PARTIAL") {
                                            Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFB91C1C), modifier = Modifier.size(12.dp))
                                        }
                                        Text(
                                            text = if (status == "PAID") "تم السداد " else "مستحق: ${remainingAmount.toInt()} ج.م",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (status == "UNPAID" || status == "PARTIAL") Color(0xFFB91C1C) else Color(0xFF15803D)
                                        )
                                    }
                                }
                                
                                Text(
                                    text = "الاشتراك: ${sessionFee.toInt()} ج.م",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Right Side: Name & Group
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = student.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "المجموعة: ${grp?.name ?: "غير محدد"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "اضغط على الكارت لتسجيل حالة الدفع",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialSavingsBankCard(
    totalCollected: Double,
    totalExpected: Double,
    totalRemaining: Double,
    monthName: String,
    year: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalExpected > 0) (totalCollected / totalExpected).coerceIn(0.0, 1.0) else 0.0
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("ملخص التحصيل المالي", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Text("$monthName $year", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("محصل ${totalCollected.toInt()} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    Text("متبقي ${totalRemaining.toInt()} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun FilterBadge(
    label: String,
    isSelected: Boolean,
    color: Color,
    hasDot: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant),
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (hasDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else color)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

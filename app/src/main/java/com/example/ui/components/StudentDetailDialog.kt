package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AttendanceRecordEntity
import com.example.data.ExamRecordEntity
import com.example.data.GroupEntity
import com.example.data.StudentEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun StudentDetailDialog(
    student: StudentEntity,
    group: GroupEntity?,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val attendanceList by viewModel.getAttendanceForStudentFlow(student.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val examRecord by viewModel.getExamForStudentFlow(student.id)
        .collectAsStateWithLifecycle(initialValue = null)

    val teacherName by viewModel.teacherName.collectAsStateWithLifecycle(initialValue = "")

    // Current attendance & homework status for today
    val todayRecord = attendanceList.find { it.date == viewModel.todayDateString }
    var currentAttendance by remember(todayRecord) { mutableStateOf(todayRecord?.attendanceStatus ?: "حضر") }
    var currentHomework by remember(todayRecord) { mutableStateOf(todayRecord?.homeworkStatus ?: "كتب الواجب") }
    var currentRecitation by remember(todayRecord) { mutableStateOf(todayRecord?.recitationStatus ?: "ممتاز") }

    // Exam states
    var e1Text by remember(examRecord) { mutableStateOf(examRecord?.exam1 ?: "") }
    var e2Text by remember(examRecord) { mutableStateOf(examRecord?.exam2 ?: "") }
    var e3Text by remember(examRecord) { mutableStateOf(examRecord?.exam3 ?: "") }

    // Stats
    val attendedCount = attendanceList.count { it.attendanceStatus == "حضر" || it.attendanceStatus == "متأخر" }
    val absentCount = attendanceList.count { it.attendanceStatus == "غائب" }

    var showStatsDialog by remember { mutableStateOf(false) }
    var showExamGradeDialog by remember { mutableStateOf(false) }
    var showPaymentHistoryDialog by remember { mutableStateOf(false) }
    var reportShareMessage by remember { mutableStateOf<String?>(null) }

    if (showPaymentHistoryDialog) {
        StudentPaymentHistoryDialog(
            student = student,
            group = group,
            viewModel = viewModel,
            onDismiss = { showPaymentHistoryDialog = false }
        )
    }

    if (showExamGradeDialog) {
        StudentExamGradeDialog(
            student = student,
            group = group,
            viewModel = viewModel,
            onDismiss = { showExamGradeDialog = false }
        )
    }

    if (showStatsDialog) {
        /*
        com.example.ui.screens.StudentStatsDialog(
            student = student,
            viewModel = viewModel,
            onDismiss = { showStatsDialog = false }
        )
        */
        showStatsDialog = false // prevent stuck state
    }

    reportShareMessage?.let { msg ->
        val phone = student.parentPhone.ifBlank { student.studentPhone }
        ReportChannelSelectionDialog(
            recipientName = student.fullName,
            phoneNumber = phone,
            reportMessage = msg,
            onDismiss = { reportShareMessage = null }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        PremiumDialogDirectionGuard()
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f)),
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PremiumIconTile(Icons.Outlined.Person, null, modifier = Modifier.size(46.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "المجموعة: ${group?.name ?: "غير محددة"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Today Date Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "تاريخ اليوم:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = viewModel.todayDisplayDate,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weekly Attendance Stats
                Text(
                    text = " إحصائية الحضور والغياب:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .62f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("عدد الحصص الملازمة", style = MaterialTheme.typography.labelSmall, color = StatusPresent)
                            Text("$attendedCount حصة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusPresent)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .62f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("عدد مرات الغياب", style = MaterialTheme.typography.labelSmall, color = StatusAbsent)
                            Text("$absentCount مرة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StatusAbsent)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Attendance Selection
                Text(
                    text = " حالة الحضور اليوم:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("حضر", "غائب", "متأخر", "تم الغاء حصة اليوم").forEach { status ->
                        val selected = currentAttendance == status
                        val bgColor = when {
                            selected && status == "حضر" -> StatusPresent
                            selected && status == "غائب" -> StatusAbsent
                            selected && status == "متأخر" -> StatusLate
                            selected && status == "تم الغاء حصة اليوم" -> Color(0xFFDC2626)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .clickable {
                                    currentAttendance = status
                                    viewModel.setAttendanceAndHomework(
                                        studentId = student.id,
                                        attendanceStatus = currentAttendance,
                                        homeworkStatus = currentHomework,
                                        recitationStatus = currentRecitation
                                    )
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when(status) {
                                    "حضر" -> "حضر"
                                    "غائب" -> "غائب"
                                    "متأخر" -> "متأخر"
                                    else -> "تم إلغاء الحصة"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Homework Selection
                Text(
                    text = " حالة الواجب المنزلي:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("كتب الواجب", "لم يكتب الواجب", "متأخر").forEach { status ->
                        val selected = currentHomework == status
                        val bgColor = when {
                            selected && status == "كتب الواجب" -> StatusPresent
                            selected && status == "لم يكتب الواجب" -> StatusAbsent
                            selected && status == "متأخر" -> StatusLate
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .clickable {
                                    currentHomework = status
                                    viewModel.setAttendanceAndHomework(
                                        studentId = student.id,
                                        attendanceStatus = currentAttendance,
                                        homeworkStatus = currentHomework,
                                        recitationStatus = currentRecitation
                                    )
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when(status) {
                                    "كتب الواجب" -> "كتب الواجب"
                                    "لم يكتب الواجب" -> "لم يكتب الواجب"
                                    else -> "متأخر"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Recitation Selection
                Text(
                    text = " حالة التسميع / الحفظ:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ممتاز", "جيد", "ضعيف").forEach { status ->
                        val selected = currentRecitation == status
                        val bgColor = when {
                            selected && status == "ممتاز" -> StatusPresent
                            selected && status == "جيد" -> StatusLate
                            selected && status == "ضعيف" -> StatusAbsent
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .clickable {
                                    currentRecitation = status
                                    viewModel.setAttendanceAndHomework(
                                        studentId = student.id,
                                        attendanceStatus = currentAttendance,
                                        homeworkStatus = currentHomework,
                                        recitationStatus = currentRecitation
                                    )
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when(status) {
                                    "ممتاز" -> "ممتاز"
                                    "جيد" -> "جيد"
                                    else -> "ضعيف"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Exam Scores Section Button
                Button(
                    onClick = { showExamGradeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Grading, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل درجة امتحان جديد (سجل 50+ امتحان) ", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showStatsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.BarChart, contentDescription = null, tint = SkyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(" عرض كارت إحصائيات الحصص والامتحانات", color = SkyPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showPaymentHistoryDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16A34A))
                ) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = Color(0xFF16A34A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(" سجل كل أيام ومدفوعات الطالب", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Comprehensive PDF Report Button
                OutlinedButton(
                    onClick = {
                        val cal = java.util.Calendar.getInstance()
                        val mName = com.example.ui.screens.ARABIC_MONTHS.find { it.second == cal.get(java.util.Calendar.MONTH) }?.first ?: "الشهر الحالي"
                        StudentPdfReportHelper.generateAndOpenStudentMonthlyReport(
                            context = context,
                            student = student,
                            group = group,
                            attendanceList = attendanceList,
                            examRecord = examRecord,
                            paymentRecord = null,
                            teacherName = teacherName,
                            monthName = mName,
                            year = cal.get(java.util.Calendar.YEAR)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(" تحميل تقرير PDF إحصائي شهري شامل", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Send Report to Parent Button (with platform selection)
                Button(
                    onClick = {
                        val message = buildString {
                            append("✨ *تقرير المتابعة* ✨\n\n")
                            append(" *تقرير المتابعة* \n\n")
                            append("السيد ولي أمر الطالب/ة المحترم:\n")
                            append("👤 *اسم الطالب:* ${student.fullName}\n")
                            if (group != null) append("📚 *المجموعة:* ${group.name}\n")
                            if (teacherName.isNotBlank()) append("👨‍🏫 *المعلم/ة:* $teacherName\n")
                            append(" *اسم الطالب:* ${student.fullName}\n")
                            append("───────────────────\n")
                            append(" *سجل المتابعة اليومية:*\n")
                            append("• *حالة الحضور:* $currentAttendance\n")
                            append("• *أداء الواجب المنزلي:* $currentHomework\n")
                            append("• *حالة التسميع/الحفظ:* $currentRecitation\n")

                            if (e1Text.isNotBlank() || e2Text.isNotBlank() || e3Text.isNotBlank()) {
                                append("\n *نتائج الاختبارات والتقييمات:*\n")
                                if (e1Text.isNotBlank()) append(" - *الاختبار الأول:* $e1Text\n")
                                if (e2Text.isNotBlank()) append(" - *الاختبار الثاني:* $e2Text\n")
                                if (e3Text.isNotBlank()) append(" - *الاختبار الثالث:* $e3Text\n")
                            }

                            append("───────────────────\n")
                            append("مع أطيب التحيات والتقدير \n")
                            if (teacherName.isNotBlank()) append("أ/ $teacherName\n")
                            append("شاكرين ومقدرين حسن تعاونكم ومتابعتكم الدائمة ")
                        }

                        reportShareMessage = message
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال التقرير لولي الأمر ", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

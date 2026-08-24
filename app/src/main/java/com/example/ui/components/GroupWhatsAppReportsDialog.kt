package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AttendanceRecordEntity
import com.example.data.GroupEntity
import com.example.data.StudentEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupWhatsAppReportsDialog(
    group: GroupEntity,
    students: List<StudentEntity>,
    attendanceList: List<AttendanceRecordEntity>,
    teacherName: String,
    todayDisplayDate: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var customNote by remember { mutableStateOf("") }
    var sentStudentIds by remember { mutableStateOf(setOf<Int>()) }
    var previewStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var studentForChannelSelection by remember { mutableStateOf<Pair<StudentEntity, String>?>(null) }
    var selectedFilterTab by remember { mutableStateOf(0) } // 0 = الكل, 1 = الحاضرون, 2 = الغائبون, 3 = لم يُرسل بعد

    // Compute status map for quick access
    val attendanceMap = remember(attendanceList) {
        attendanceList.associateBy { it.studentId }
    }

    val presentCount = students.count { (attendanceMap[it.id]?.attendanceStatus ?: "حضر") == "حضر" }
    val absentCount = students.count { (attendanceMap[it.id]?.attendanceStatus ?: "حضر") == "غائب" }
    val lateCount = students.count { (attendanceMap[it.id]?.attendanceStatus ?: "حضر") == "متأخر" }

    val filteredStudents = remember(students, selectedFilterTab, sentStudentIds, attendanceMap) {
        when (selectedFilterTab) {
            1 -> students.filter { (attendanceMap[it.id]?.attendanceStatus ?: "حضر") == "حضر" }
            2 -> students.filter { (attendanceMap[it.id]?.attendanceStatus ?: "حضر") == "غائب" }
            3 -> students.filter { !sentStudentIds.contains(it.id) }
            else -> students
        }
    }

    // Helper function to build individual message for a student
    fun buildStudentReportMessage(student: StudentEntity): String {
        val attRecord = attendanceMap[student.id]
        val attStatus = attRecord?.attendanceStatus ?: "حضر"
        val hwStatus = attRecord?.homeworkStatus ?: "كتب الواجب"
        val recStatus = attRecord?.recitationStatus ?: "ممتاز"

        return buildString {
            append("✨ *تقرير المتابعة اليومي* ✨\n\n")
            append(" *تقرير المتابعة اليومي* \n\n")
            append("ولي أمر الطالب/ة المحترم: *${student.fullName}*\n")
            append("📚 *المجموعة:* ${group.name} (${group.subject})\n")
            if (teacherName.isNotBlank()) append("👨‍🏫 *المعلم/ة:* $teacherName\n")
            append(" *المجموعة:* ${group.name} (${group.subject})\n")
            append("───────────────────\n")
            append(" *سجل اليوم:*\n")
            append("• *حالة الحضور والغياب:* $attStatus ")
            when (attStatus) {
                "حضر" -> append("🟢")
                "غائب" -> append("🔴")
                "متأخر" -> append("🟡")
                else -> Unit
            }
            append("\n")
            append("• *أداء الواجب المنزلي:* $hwStatus 📝\n")

            if (customNote.isNotBlank()) {
                append("\n *ملاحظة خاصة من المعلم/ة:*\n")
                append(customNote.trim())
                append("\n")
            }

            append("───────────────────\n")
            append("مع أطيب التحيات والتقدير \n")
            if (teacherName.isNotBlank()) append("أ/ $teacherName\n")
            append("شاكرين ومقدرين حسن تعاونكم ومتابعتكم الدائمة ")
        }
    }

    // Function to send report message to a student with channel selection
    fun sendWhatsAppToStudent(student: StudentEntity) {
        val cleanPhone = student.parentPhone.ifBlank { student.studentPhone }.replace(" ", "").replace("-", "").replace("+", "")
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "لا يوجد رقم هاتف مسجل للطالب/ة ${student.fullName}", Toast.LENGTH_SHORT).show()
            return
        }

        val message = buildStudentReportMessage(student)
        studentForChannelSelection = student to message
    }

    // Function to get the next unsent student
    val nextUnsentStudent = remember(students, sentStudentIds) {
        students.firstOrNull { !sentStudentIds.contains(it.id) && it.parentPhone.isNotBlank() }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PremiumDialogDirectionGuard()
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f))
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = " تقارير الواتساب لأولياء الأمور",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = SkyPrimary
                                    )
                                    Text(
                                        text = "${group.name} • ${students.size} طالب/ة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SkyOnSurfaceVariant
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Outlined.Close, contentDescription = "إغلاق", tint = SkyPrimary)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = SkySurface)
                        )
                    },
                    bottomBar = {
                        Surface(
                            shadowElevation = 8.dp,
                            color = SkySurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Big Sequential Send Button
                                    Button(
                                        onClick = {
                                            nextUnsentStudent?.let { sendWhatsAppToStudent(it) }
                                        },
                                        enabled = nextUnsentStudent != null,
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF25D366),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (nextUnsentStudent != null) {
                                            Text(
                                                text = "إرسال لـ: ${nextUnsentStudent.fullName} (${sentStudentIds.size + 1}/${students.size})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Text(
                                                text = " اكتمل إرسال الجميع!",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    // Open Group Link Button
                                    Button(
                                        onClick = {
                                            val groupSummary = buildString {
                                                append("📋 *تقرير كشف حضور وغياب مجموعة: ${group.name}*\n")
                                                append(" *تقرير كشف حضور وغياب مجموعة: ${group.name}*\n")
                                                append("───────────────────\n")
                                                students.forEachIndexed { idx, st ->
                                                    val rec = attendanceMap[st.id]
                                                    val stStatus = rec?.attendanceStatus ?: "حضر"
                                                    val hwStatus = rec?.homeworkStatus ?: "كتب الواجب"
                                                    append("${idx + 1}. *${st.fullName}*: $stStatus ($hwStatus)\n")
                                                }
                                                append("───────────────────\n")
                                                append("إجمالي الطلاب: ${students.size} | حاضر: $presentCount | غائب: $absentCount")
                                            }
                                            ReportSender.openWhatsAppGroupLink(context, group.whatsappGroupUrl, groupSummary)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF075E54),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp)
                                    ) {
                                        Icon(Icons.Outlined.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = " فتح الجروب",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))

                            // Summary Stat Cards
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SkySurface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = " إحصائيات حضور المجموعة اليوم:",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SkyOnSurface
                                        )
                                        Text(
                                            text = todayDisplayDate,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SkyPrimary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        StatBadge(
                                            title = "حاضر",
                                            count = presentCount,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatBadge(
                                            title = "غائب",
                                            count = absentCount,
                                            color = Color(0xFFC62828),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatBadge(
                                            title = "متأخر",
                                            count = lateCount,
                                            color = Color(0xFFF57F17),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatBadge(
                                            title = "تم الإرسال",
                                            count = sentStudentIds.size,
                                            color = SkyPrimary,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                    }

                                    // Copy Entire Group Attendance Summary to Clipboard
                                    OutlinedButton(
                                        onClick = {
                                            val groupSummary = buildString {
                                                append("📋 *تقرير حضور وغياب مجموعة: ${group.name}*\n")
                                                append(" *تقرير حضور وغياب مجموعة: ${group.name}*\n")
                                                append("───────────────────\n")
                                                students.forEachIndexed { idx, st ->
                                                    val rec = attendanceMap[st.id]
                                                    val stStatus = rec?.attendanceStatus ?: "حضر"
                                                    val hwStatus = rec?.homeworkStatus ?: "كتب الواجب"
                                                    append("${idx + 1}. *${st.fullName}*: $stStatus ($hwStatus)\n")
                                                }
                                                append("───────────────────\n")
                                                append("إجمالي الطلاب: ${students.size} | حاضر: $presentCount | غائب: $absentCount")
                                            }
                                            clipboardManager.setText(AnnotatedString(groupSummary))
                                            Toast.makeText(context, "تم نسخ تقرير المجموعة بالكامل إلى الحافظة ", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("نسخ كشف حضور المجموعة بالكامل للحافظة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Custom Note Field Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SkySurface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = " ملاحظة أو تنبيه عام يُرفق مع تقرير كل طالب (اختياري):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = SkyOnSurface
                                    )
                                    OutlinedTextField(
                                        value = customNote,
                                        onValueChange = { customNote = it },
                                        placeholder = { Text("مثال: تنبيه لجميع أولياء الأمور: يوجد اختبار شامل الأسبوع القادم...", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        minLines = 2,
                                        maxLines = 3
                                    )
                                }
                            }
                        }

                        // Filter Tabs
                        item {
                            ScrollableTabRow(
                                selectedTabIndex = selectedFilterTab,
                                edgePadding = 0.dp,
                                containerColor = Color.Transparent,
                                divider = {}
                            ) {
                                Tab(
                                    selected = selectedFilterTab == 0,
                                    onClick = { selectedFilterTab = 0 },
                                    text = { Text("الكل (${students.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                )
                                Tab(
                                    selected = selectedFilterTab == 1,
                                    onClick = { selectedFilterTab = 1 },
                                    text = { Text("الحاضرون  ($presentCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                )
                                Tab(
                                    selected = selectedFilterTab == 2,
                                    onClick = { selectedFilterTab = 2 },
                                    text = { Text("الغائبون  ($absentCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                )
                                Tab(
                                    selected = selectedFilterTab == 3,
                                    onClick = { selectedFilterTab = 3 },
                                    text = { Text("غير مُرسل  (${students.size - sentStudentIds.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                )
                            }
                        }

                        // Student List Items
                        if (filteredStudents.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("لا يوجد طلاب يطابقون التصفية المختارة.", color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                        } else {
                            itemsIndexed(filteredStudents) { index, student ->
                                val isSent = sentStudentIds.contains(student.id)
                                val isNext = nextUnsentStudent?.id == student.id
                                val attRecord = attendanceMap[student.id]
                                val attStatus = attRecord?.attendanceStatus ?: "حضر"
                                val hwStatus = attRecord?.homeworkStatus ?: "كتب الواجب"
                                val recStatus = attRecord?.recitationStatus ?: "ممتاز"

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isNext) 2.dp else 1.dp,
                                            color = if (isNext) Color(0xFF25D366) else SkySurfaceVariant,
                                            shape = RoundedCornerShape(14.dp)
                                        ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isNext) Color(0xFFE8F5E9) else SkySurface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSent) Color(0xFF4CAF50) else SkyPrimaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (isSent) Color.White else SkyPrimary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = student.fullName,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = SkyOnSurface
                                                    )
                                                    Text(
                                                        text = " ولي الأمر: ${if (student.parentPhone.isNotBlank()) student.parentPhone else "غير مسجل"}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = SkyOnSurfaceVariant,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }

                                            // Sent Status Tag
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSent) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                                            ) {
                                                Text(
                                                    text = if (isSent) "تم الإرسال " else "لم يرسل ",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSent) Color(0xFF2E7D32) else Color.Gray,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        // Status Chips Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            StatusChip(
                                                label = attStatus,
                                                icon = when (attStatus) {
                                                    "حضر" -> Icons.Outlined.CheckCircle
                                                    "غائب" -> Icons.Outlined.Cancel
                                                    else -> Icons.Outlined.AccessTime
                                                },
                                                color = when (attStatus) {
                                                    "حضر" -> Color(0xFF2E7D32)
                                                    "غائب" -> Color(0xFFC62828)
                                                    else -> Color(0xFFF57F17)
                                                }
                                            )

                                            StatusChip(
                                                label = hwStatus,
                                                icon = Icons.Outlined.Assignment,
                                                color = SkyPrimary
                                            )

                                            StatusChip(
                                                label = recStatus,
                                                icon = Icons.Outlined.Star,
                                                color = Color(0xFF7B1FA2)
                                            )
                                        }

                                        HorizontalDivider(color = SkySurfaceVariant)

                                        // Action Buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                TextButton(
                                                    onClick = { previewStudent = student },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("معاينة", fontSize = 11.sp)
                                                }

                                                TextButton(
                                                    onClick = {
                                                        val msg = buildStudentReportMessage(student)
                                                        clipboardManager.setText(AnnotatedString(msg))
                                                        Toast.makeText(context, "تم نسخ تقرير ${student.fullName} للحافظة", Toast.LENGTH_SHORT).show()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("نسخ", fontSize = 11.sp)
                                                }
                                            }

                                            // Direct WhatsApp Send Button
                                            Button(
                                                onClick = { sendWhatsAppToStudent(student) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF25D366),
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("إرسال ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            // Preview Message Dialog
            previewStudent?.let { st ->
                PremiumAlertDialog(
                    onDismissRequest = { previewStudent = null },
                    title = {
                        Text(
                            text = " معاينة تقرير ${st.fullName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    },
                    text = {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SkySurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = buildStudentReportMessage(st),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                color = SkyOnSurface
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val s = previewStudent
                                previewStudent = null
                                s?.let { sendWhatsAppToStudent(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Text("إرسال الآن ")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { previewStudent = null }) {
                            Text("إغلاق")
                        }
                    }
                )
            }

            // Channel Selection Dialog (WhatsApp, WhatsApp Business, Telegram, SMS)
            studentForChannelSelection?.let { (st, msg) ->
                val phone = st.parentPhone.ifBlank { st.studentPhone }
                ReportChannelSelectionDialog(
                    recipientName = st.fullName,
                    phoneNumber = phone,
                    reportMessage = msg,
                    onDismiss = {
                        sentStudentIds = sentStudentIds + st.id
                        studentForChannelSelection = null
                    }
                )
            }
        }
    }
}

@Composable
private fun StatBadge(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

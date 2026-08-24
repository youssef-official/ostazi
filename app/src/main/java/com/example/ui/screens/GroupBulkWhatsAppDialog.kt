package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AttendanceRecordEntity
import com.example.data.ExamRecordEntity
import com.example.data.GroupEntity
import com.example.data.PaymentRecordEntity
import com.example.data.StudentEntity
import com.example.ui.components.ReportChannel
import com.example.ui.components.ReportChannelSelectionDialog
import com.example.ui.components.ReportSender
import com.example.ui.components.PremiumDialogDirectionGuard
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupBulkWhatsAppDialog(
    group: GroupEntity,
    students: List<StudentEntity>,
    attendanceList: List<AttendanceRecordEntity>,
    allExams: List<ExamRecordEntity>,
    paymentsForMonth: List<PaymentRecordEntity>,
    teacherName: String,
    initialTab: Int = 0,
    viewModel: com.example.ui.MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val formattedDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ar")).format(Date())
    }

    var selectedTab by remember { mutableStateOf(initialTab) } // 0 = Individual Students, 1 = Full Group Report, 2 = Custom Manual Report
    var selectedChannel by remember { mutableStateOf(ReportChannel.WHATSAPP) }
    var studentForCustomChannelDialog by remember { mutableStateOf<Pair<StudentEntity, String>?>(null) }
    var reportNote by remember { mutableStateOf("") }

    val savedReport by viewModel.savedManualReport.collectAsState()
    var editableManualMessage by remember(savedReport) { mutableStateOf(savedReport) }
    var manualTargetPhone by remember { mutableStateOf("") }
    var selectedManualStudentId by remember { mutableStateOf<Int?>(null) }

    // Rebuild the group message when reportNote or other dependencies change
    val groupSummaryMessage = remember(group, students, attendanceList, allExams, paymentsForMonth, teacherName, reportNote) {
        buildString {
            val groupStudentIds = students.map { it.id }.toSet()
            val presentCount = attendanceList.count { it.studentId in groupStudentIds && (it.attendanceStatus == "حضر" || it.attendanceStatus == "حاضر") }
            append("📊 *تقرير المتابعة الشامل لمجموعة: ${group.name}* 📊\n")
            append("📚 *المادة:* ${group.subject}\n")
            append("🗓️ *التاريخ:* $formattedDate\n")
            append(" *تقرير المتابعة الشامل لمجموعة: ${group.name}* \n")
            if (group.notes.isNotBlank()) {
                append(" *ملاحظات المجموعة:* ${group.notes}\n\n")
            }
            append("═══════════════════════════\n\n")
            students.forEachIndexed { index, student ->
                val attRec = attendanceList.find { it.studentId == student.id }
                val att = attRec?.attendanceStatus ?: ""
                val attDisplay = if (att.isBlank()) "لم يحدد " else att
                val hw = attRec?.homeworkStatus ?: "كتب"
                val inter = attRec?.interactionStatus ?: "ممتاز"
                val rec = attRec?.recitationGrade ?: "10/10"
                val note = attRec?.notes ?: ""
                val exRec = allExams.find { it.studentId == student.id }
                val exam1 = exRec?.exam1

                append("${index + 1}. *${student.fullName}*\n")
                append("   • الحضور: $attDisplay | الواجب: $hw\n")
                append("   • التفاعل: $inter | التسميع: $rec\n")
                if (!exam1.isNullOrBlank()) append("   • الامتحان: $exam1/100\n")
                if (note.isNotBlank()) append("   • ملاحظة: $note\n")
                append("\n")
            }
            append("═══════════════════════════\n")
            if (reportNote.isNotBlank()) {
                append(" *ملاحظة:* $reportNote\n\n")
            }
            if (teacherName.isNotBlank()) {
                append("مع تحيات: أ/ $teacherName ")
            }
        }
    }

    var editableGroupSummaryMessage by remember(groupSummaryMessage) { mutableStateOf(groupSummaryMessage) }

    fun buildSingleStudentMessage(student: StudentEntity, customNote: String): String {
        val attRec = attendanceList.find { it.studentId == student.id }
        val attStatus = attRec?.attendanceStatus ?: ""
        val attDisplay = if (attStatus.isBlank()) "لم يحدد " else attStatus
        val hwStatus = attRec?.homeworkStatus ?: "كتب"
        val interStatus = attRec?.interactionStatus ?: "ممتاز"
        val recGrade = attRec?.recitationGrade ?: "10/10"
        val note = attRec?.notes ?: ""
        val exRec = allExams.find { it.studentId == student.id }
        val payRec = paymentsForMonth.find { it.studentId == student.id }
        val payStatus = payRec?.paymentStatus ?: "UNPAID"
        val remainingFee = payRec?.remainingAmount ?: 0.0

        return buildString {
            append("✨ *تقرير المتابعة والتقييم الشامل* ✨\n\n")
            append(" *تقرير المتابعة والتقييم الشامل* \n\n")
            append("ولي أمر الطالب/ة المحترم: *${student.fullName}*\n")
            append("📚 *المجموعة:* ${group.name} (${group.subject})\n")
            append("🗓️ *التاريخ:* $formattedDate\n\n")
            append("📌 *حالة الحضور:* $attDisplay\n")
            append("📝 *أداء الواجب:* $hwStatus\n")
            append("💡 *التفاعل الصفي:* $interStatus\n")
            append(" *المجموعة:* ${group.name} (${group.subject})\n")
            if (exRec != null && (!exRec.exam1.isNullOrBlank() || !exRec.exam2.isNullOrBlank() || !exRec.exam3.isNullOrBlank())) {
                append("\n *درجات الامتحانات:*\n")
                if (!exRec.exam1.isNullOrBlank()) append("  • امتحان 1: ${exRec.exam1}/100\n")
                if (!exRec.exam2.isNullOrBlank()) append("  • امتحان 2: ${exRec.exam2}/100\n")
                if (!exRec.exam3.isNullOrBlank()) append("  • امتحان 3: ${exRec.exam3}/100\n")
            }
            if (note.isNotBlank()) {
                append("\n *ملاحظة المعلم:* $note\n")
            }
            val payDisplay = when (payStatus) {
                "PAID" -> "تم سداد المصروفات بالكامل ✔️"
                else -> "لم يتم سداد المصروفات بعد ⏳"
            }
            append("\n *المصروفات:* $payDisplay\n\n")
            if (customNote.isNotBlank()) {
                append(" *ملاحظة إضافية:* $customNote\n\n")
            }
            append("مع أطيب التحيات والتقدير \n")
            if (teacherName.isNotBlank()) {
                append("أ/ $teacherName")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PremiumDialogDirectionGuard()
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = when (selectedTab) {
                                0 -> "إرسال تقرير لولي الأمر"
                                1 -> "إرسال تقرير المجموعة"
                                else -> "كتابة وإرسال تقرير يدوي"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "مجموعة: ${group.name} (${students.size} طالب)",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (selectedTab == 0 || selectedTab == 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportNote,
                        onValueChange = { reportNote = it },
                        label = { Text("كتابة ملاحظة للتقرير ", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        placeholder = { Text("اكتب ملاحظة إضافية لتظهر قبل اسم المعلم...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            textAlign = TextAlign.Right
                        ),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF7C3AED),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Segmented Control Row - High Premium look & completely prevents any text clipping!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (selectedTab == 0) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "لولي الأمر",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // Tab 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 1) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Outlined.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (selectedTab == 1) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "الجروب دفعة واحدة",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // Tab 2
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 2) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                            .clickable { selectedTab = 2 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Outlined.Description,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (selectedTab == 2) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تقرير يدوي",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (selectedTab == 2) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (selectedTab == 0) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Default Channel Selection Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "المنصة السريعة:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            reverseLayout = true
                        ) {
                            items(ReportChannel.values()) { channel ->
                                val isSelected = selectedChannel == channel
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) channel.brandColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isSelected) channel.brandColor else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.clickable { selectedChannel = channel }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = channel.iconVector,
                                            contentDescription = null,
                                            tint = if (isSelected) channel.brandColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = channel.title,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) channel.brandColor else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab 0: Individual Student Reports (إرسال لولي الأمر)
                if (selectedTab == 0) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(students) { student ->
                            val singleMsg = buildSingleStudentMessage(student, reportNote)
                            val phone = student.parentPhone.ifBlank { student.studentPhone }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Action buttons for single student
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        // Send button via selected channel
                                        Button(
                                            onClick = {
                                                ReportSender.send(context, selectedChannel, phone, singleMsg)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = selectedChannel.brandColor),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(selectedChannel.iconVector, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "إرسال (${selectedChannel.title})",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        // Choose custom platform button
                                        IconButton(
                                            onClick = {
                                                studentForCustomChannelDialog = student to singleMsg
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.MoreVert,
                                                contentDescription = "اختيار وسيلة أخرى",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Student details
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = student.fullName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = if (phone.isNotBlank()) " $phone" else "بدون هاتف مسجل",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    // Tab 1: Full Group Report (إرسال على الجروب دفعة واحدة)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "معاينة تقرير الجروب الشامل ",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                ) {
                                    OutlinedTextField(
                                        value = editableGroupSummaryMessage,
                                        onValueChange = { editableGroupSummaryMessage = it },
                                        modifier = Modifier.fillMaxSize(),
                                        placeholder = { Text("اكتب رسالة التقرير هنا...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) },
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 17.sp,
                                            textDirection = androidx.compose.ui.text.style.TextDirection.Rtl
                                        ),
                                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF2563EB),
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }

                        // Channels Quick Send Grid (WhatsApp, WA Business, Telegram, SMS)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // WhatsApp
                            Button(
                                onClick = {
                                    ReportSender.openWhatsAppGroupLink(
                                        context,
                                        group.whatsappGroupUrl,
                                        editableGroupSummaryMessage,
                                        "com.whatsapp"
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("واتساب", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, maxLines = 1, softWrap = false)
                            }

                            // WhatsApp Business
                            Button(
                                onClick = {
                                    ReportSender.openWhatsAppGroupLink(
                                        context,
                                        group.whatsappGroupUrl,
                                        editableGroupSummaryMessage,
                                        "com.whatsapp.w4b"
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF075E54)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.BusinessCenter, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("أعمال", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, maxLines = 1, softWrap = false)
                            }

                            // Telegram
                            Button(
                                onClick = {
                                    ReportSender.send(context, ReportChannel.TELEGRAM, "", editableGroupSummaryMessage)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("تليجرام", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, maxLines = 1, softWrap = false)
                            }

                            // SMS
                            Button(
                                onClick = {
                                    ReportSender.send(context, ReportChannel.SMS, "", editableGroupSummaryMessage)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.Sms, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("SMS", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, maxLines = 1, softWrap = false)
                            }
                        }

                        // Copy Button
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Group Report", editableGroupSummaryMessage)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ تقرير المجموعة بنجاح ", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ التقرير بالكامل للحافظة", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                        }
                    }
                } else if (selectedTab == 2) {
                    // Tab 2: Custom Manual Report (تقرير يدوي مخصص)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "تقرير يدوي مخصص ",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "اكتب نص التقرير اليدوي واحفظه للإرسال السريع",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                ) {
                                    OutlinedTextField(
                                        value = editableManualMessage,
                                        onValueChange = { editableManualMessage = it },
                                        modifier = Modifier.fillMaxSize(),
                                        placeholder = { Text("اكتب نص التقرير اليدوي هنا...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) },
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 17.sp,
                                            textDirection = androidx.compose.ui.text.style.TextDirection.Rtl
                                        ),
                                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF0284C7),
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                // Save button
                                Button(
                                    onClick = {
                                        viewModel.saveManualReport(editableManualMessage)
                                        Toast.makeText(context, "تم حفظ نص التقرير بنجاح! ", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    contentPadding = PaddingValues(vertical = 6.dp)
                                ) {
                                    Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حفظ التقرير في الذاكرة ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Target Phone input card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "اختر ولي الأمر",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(students, key = { it.id }) { student ->
                                        val selected = selectedManualStudentId == student.id
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                selectedManualStudentId = student.id
                                                manualTargetPhone = student.parentPhone.ifBlank { student.studentPhone }
                                            },
                                            label = { Text(student.fullName, fontSize = 10.5.sp) },
                                            leadingIcon = if (selected) {
                                                { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(14.dp)) }
                                            } else null
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = manualTargetPhone,
                                    onValueChange = { manualTargetPhone = it },
                                    label = { Text("رقم هاتف المستلم (اختياري) ", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    placeholder = { Text("01xxxxxxxxx", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.End
                                    ),
                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0284C7),
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (editableManualMessage.isBlank()) {
                                                Toast.makeText(context, "اكتب التقرير أولاً", Toast.LENGTH_SHORT).show()
                                            } else {
                                                ReportSender.openWhatsAppGroupLink(
                                                    context,
                                                    group.whatsappGroupUrl,
                                                    editableManualMessage,
                                                    "com.whatsapp"
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Outlined.Groups, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("إرسال للجروب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            if (manualTargetPhone.isBlank()) {
                                                Toast.makeText(context, "اختر الطالب أو اكتب رقم ولي الأمر", Toast.LENGTH_SHORT).show()
                                            } else if (editableManualMessage.isBlank()) {
                                                Toast.makeText(context, "اكتب التقرير أولاً", Toast.LENGTH_SHORT).show()
                                            } else {
                                                ReportSender.send(context, ReportChannel.WHATSAPP, manualTargetPhone, editableManualMessage)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16855B))
                                    ) {
                                        Icon(Icons.Outlined.FamilyRestroom, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("إرسال لولي الأمر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Direct Share Channels
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // WhatsApp
                                    Button(
                                        onClick = {
                                            ReportSender.send(context, ReportChannel.WHATSAPP, manualTargetPhone, editableManualMessage)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("واتساب", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, maxLines = 1, softWrap = false)
                                    }

                                    // WhatsApp Business
                                    Button(
                                        onClick = {
                                            ReportSender.send(context, ReportChannel.WHATSAPP_BUSINESS, manualTargetPhone, editableManualMessage)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Outlined.Business, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("أعمال", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, maxLines = 1, softWrap = false)
                                    }

                                    // Telegram
                                    Button(
                                        onClick = {
                                            ReportSender.send(context, ReportChannel.TELEGRAM, manualTargetPhone, editableManualMessage)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("تليجرام", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, maxLines = 1, softWrap = false)
                                    }

                                    // SMS
                                    Button(
                                        onClick = {
                                            ReportSender.send(context, ReportChannel.SMS, manualTargetPhone, editableManualMessage)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Outlined.Sms, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("SMS", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal when picking custom channel for student
    studentForCustomChannelDialog?.let { (student, msg) ->
        val phone = student.parentPhone.ifBlank { student.studentPhone }
        ReportChannelSelectionDialog(
            recipientName = student.fullName,
            phoneNumber = phone,
            reportMessage = msg,
            onDismiss = { studentForCustomChannelDialog = null }
        )
    }
}

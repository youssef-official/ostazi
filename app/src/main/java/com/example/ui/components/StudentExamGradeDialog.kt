package com.example.ui.components

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GroupEntity
import com.example.data.StudentEntity
import com.example.data.StudentIndividualExamEntity
import com.example.ui.MainViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentExamGradeDialog(
    student: StudentEntity,
    group: GroupEntity?,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSwitchToPayment: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val individualExams by viewModel.getIndividualExamsForStudentFlow(student.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val teacherName by viewModel.teacherName.collectAsStateWithLifecycle(initialValue = "")

    var editingExamId by remember { mutableStateOf<Int?>(null) }
    var examName by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var maxScore by remember { mutableStateOf("100") }
    var examDate by remember { mutableStateOf(viewModel.todayDateString) }
    var examNotes by remember { mutableStateOf("") }

    var reportShareMessage by remember { mutableStateOf<String?>(null) }

    // DatePickerDialog helper
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formattedDate = String.format(Locale.ENGLISH, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            examDate = formattedDate
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

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
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // --- Top Header ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close 'X' Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = student.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "المجموعة: ${group?.name ?: "عامة"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Avatar Circle
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEDE9FE),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFC4B5FD)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val initial = student.fullName.trim().take(1).ifEmpty { "ط" }
                                Text(
                                    text = initial,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7C3AED)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- Header Badge ---
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Assignment,
                                contentDescription = null,
                                tint = Color(0xFF9333EA),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (editingExamId != null) "تعديل امتحان مسجل " else "رصد وتوثيق درجات الامتحانات ",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7E22CE)
                            )
                        }

                        if (editingExamId != null) {
                            TextButton(
                                onClick = {
                                    editingExamId = null
                                    examName = ""
                                    score = ""
                                    maxScore = "100"
                                    examDate = viewModel.todayDateString
                                    examNotes = ""
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("إلغاء التعديل", fontSize = 11.5.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Scrollable Form & History List ---
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // --- Card: Exam Input Form ---
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. Exam Name Field
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "اسم الامتحان / الاختبار:",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    OutlinedTextField(
                                        value = examName,
                                        onValueChange = { examName = it },
                                        placeholder = { Text("مثال: امتحان شهر أكتوبر / اختبار قصير", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF9333EA),
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    // Quick Exam Name Suggestion Chips (All 4 on the exact same single row)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val suggestions = listOf("امتحان شامل", "اختبار قصير", "امتحان الشهر", "تسميع شفوي")
                                        suggestions.forEach { suggestion ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (examName == suggestion) Color(0xFFE9D5FF) else MaterialTheme.colorScheme.surfaceVariant,
                                                border = if (examName == suggestion) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC084FC)) else null,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { examName = suggestion }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 6.dp, horizontal = 2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = suggestion,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (examName == suggestion) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (examName == suggestion) Color(0xFF6B21A8) else MaterialTheme.colorScheme.onSurface,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 2. Scores Row: Student Score + Max Score
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Student Score Field
                                    Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "درجة الطالب:",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        OutlinedTextField(
                                            value = score,
                                            onValueChange = { score = it },
                                            placeholder = { Text("مثال: 95", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF9333EA),
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }

                                    // Max Score Field
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "الدرجة الكلية (من):",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        OutlinedTextField(
                                            value = maxScore,
                                            onValueChange = { maxScore = it },
                                            placeholder = { Text("100", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF9333EA),
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }

                                // Quick Score Shortcuts
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val quickScores = listOf(
                                        "كاملة" to maxScore,
                                        "نصف" to ((maxScore.toDoubleOrNull() ?: 100.0) / 2).toInt().toString(),
                                        "0" to "0",
                                        "غائب" to "غائب"
                                    )
                                    quickScores.forEach { (label, value) ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.clickable { score = value }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                // 3. Exam Date Field (with Interactive Date Picker)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "تاريخ الامتحان:",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    OutlinedTextField(
                                        value = examDate,
                                        onValueChange = { examDate = it },
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { datePickerDialog.show() },
                                        leadingIcon = {
                                            IconButton(onClick = { datePickerDialog.show() }) {
                                                Icon(
                                                    Icons.Outlined.CalendarMonth,
                                                    contentDescription = "اختيار التاريخ",
                                                    tint = Color(0xFF7C3AED),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            TextButton(onClick = { datePickerDialog.show() }) {
                                                Text("تغيير ", fontSize = 11.5.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF9333EA),
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                // 4. Notes Field
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "ملاحظات وتفاصيل الامتحان (اختياري):",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    OutlinedTextField(
                                        value = examNotes,
                                        onValueChange = { examNotes = it },
                                        placeholder = { Text("اكتب ملاحظاتك عن أداء الطالب أو موضوع الامتحان...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        maxLines = 3,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF9333EA),
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // --- Action 1: Save Grade Button ---
                                Button(
                                    onClick = {
                                        val name = examName.ifBlank { "امتحان بدون عنوان" }
                                        val sc = score.ifBlank { "0" }
                                        val mSc = maxScore.ifBlank { "100" }

                                        val currentEditingId = editingExamId
                                        if (currentEditingId != null) {
                                            viewModel.updateIndividualExam(
                                                StudentIndividualExamEntity(
                                                    id = currentEditingId,
                                                    studentId = student.id,
                                                    examName = name,
                                                    score = sc,
                                                    maxScore = mSc,
                                                    date = examDate,
                                                    notes = examNotes
                                                )
                                            )
                                            Toast.makeText(context, "تم تحديث بيانات الامتحان بنجاح! ", Toast.LENGTH_SHORT).show()
                                            editingExamId = null
                                        } else {
                                            viewModel.addIndividualExam(
                                                studentId = student.id,
                                                examName = name,
                                                score = sc,
                                                maxScore = mSc,
                                                date = examDate,
                                                notes = examNotes
                                            )
                                            Toast.makeText(context, "تم حفظ الدرجة في سجل الطالب بنجاح! ", Toast.LENGTH_SHORT).show()
                                        }

                                        examName = ""
                                        score = ""
                                        examNotes = ""
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (editingExamId != null) "تحديث بيانات الامتحان" else "حفظ الدرجة في السجل",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                // --- Action 2: Mark as Absent / No Exam ---
                                OutlinedButton(
                                    onClick = {
                                        val name = examName.ifBlank { "امتحان" }
                                        viewModel.addIndividualExam(
                                            studentId = student.id,
                                            examName = "$name (غائب)",
                                            score = "غائب",
                                            maxScore = maxScore.ifBlank { "100" },
                                            date = examDate,
                                            notes = "لم يحضر الطالب الامتحان"
                                        )
                                        Toast.makeText(context, "تم تسجيل الطالب كـ غائب ", Toast.LENGTH_SHORT).show()
                                        examName = ""
                                        score = ""
                                        examNotes = ""
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = Color(0xFF991B1B)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                                ) {
                                    Text("تسجيل كـ غائب عن الامتحان ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                // --- Action 3: Share Report Via WhatsApp ---
                                OutlinedButton(
                                    onClick = {
                                        val name = examName.ifBlank { "امتحان" }
                                        val sc = score.ifBlank { "لم تُحدد" }
                                        val mSc = maxScore.ifBlank { "100" }
                                        val msg = buildString {
                                            append("📋 *تقرير نتيجة امتحان الطالب* 📋\n\n")
                                            append(" *تقرير نتيجة امتحان الطالب* \n\n")
                                            append("السيد ولي أمر الطالب/ة المحترم:\n")
                                            append("👤 *اسم الطالب:* ${student.fullName}\n")
                                            if (group != null) append("📚 *المجموعة:* ${group.name} (${group.subject})\n")
                                            if (teacherName.isNotBlank()) append("👨‍🏫 *المعلم/ة:* $teacherName\n")
                                            append(" *اسم الطالب:* ${student.fullName}\n")
                                            append("───────────────────\n")
                                            append("📝 *اسم الامتحان:* $name\n")
                                            append("💯 *النتيجة:* $sc من $mSc\n")
                                            append(" *اسم الامتحان:* $name\n")
                                            append("───────────────────\n\n")
                                            append("مع أطيب تمنياتنا بالتوفيق والتفوق الدائم ")
                                        }
                                        reportShareMessage = msg
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = Color(0xFF16A34A)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color(0xFF16A34A)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إرسال تقرير فوري لواتساب ولي الأمر", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF16A34A))
                                }
                            }
                        }
                    }

                    // --- History Section: Recorded Exams ---
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "سجل الامتحانات السابقة (${individualExams.size}) ",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (individualExams.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.clickable {
                                            // Share all exams summary
                                            val msg = buildString {
                                                append(" *سجل نتائج امتحانات الطالب* \n\n")
                                                append("ولي أمر الطالب: *${student.fullName}*\n")
                                                if (group != null) append("المجموعة: ${group.name}\n\n")
                                                append(" *الامتحانات المسجلة:*\n")
                                                individualExams.forEachIndexed { idx, ex ->
                                                    append("${idx + 1}. *${ex.examName}* (${ex.date})\n")
                                                    append("   ▫️ الدرجة: ${ex.score} / ${ex.maxScore}\n")
                                                    append("   ▫ الدرجة: ${ex.score} / ${ex.maxScore}\n")
                                                }
                                                append("\nمع تحيات الأستاذ/ $teacherName ")
                                            }
                                            reportShareMessage = msg
                                        }
                                    ) {
                                        Text(
                                            text = "مشاركة السجل الشامل ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (individualExams.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "لا توجد امتحانات مسجلة لهذا الطالب حتى الآن.\nيمكنك رصد أي امتحان جديد من النموذج أعلاه.",
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Exam Records List Items
                    items(individualExams, key = { it.id }) { exam ->
                        val isAbsent = exam.score.contains("غائب")
                        val scoreNum = exam.score.toDoubleOrNull()
                        val maxNum = exam.maxScore.toDoubleOrNull() ?: 100.0
                        val percentage = if (scoreNum != null && maxNum > 0) (scoreNum / maxNum * 100).toInt() else null

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (editingExamId == exam.id) Color(0xFF9333EA) else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = exam.examName,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (editingExamId == exam.id) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text("جاري التعديل", fontSize = 10.sp, color = Color(0xFF9333EA), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = " ${exam.date}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (exam.notes.isNotBlank()) {
                                        Text(
                                            text = " ${exam.notes}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Score Badge
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            isAbsent -> MaterialTheme.colorScheme.errorContainer
                                            percentage != null && percentage >= 85 -> Color(0xFFDCFCE7)
                                            percentage != null && percentage >= 50 -> MaterialTheme.colorScheme.tertiaryContainer
                                            else -> MaterialTheme.colorScheme.errorContainer
                                        }
                                    ) {
                                        Text(
                                            text = if (isAbsent) "غائب" else "${exam.score} / ${exam.maxScore}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isAbsent -> Color(0xFF991B1B)
                                                percentage != null && percentage >= 85 -> Color(0xFF166534)
                                                percentage != null && percentage >= 50 -> Color(0xFFB45309)
                                                else -> Color(0xFF991B1B)
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // Share Button
                                    IconButton(
                                        onClick = {
                                            val msg = buildString {
                                                append("📋 *نتيجة امتحان للطالب/ة:* ${student.fullName}\n")
                                                if (group != null) append("📚 *المجموعة:* ${group.name}\n")
                                                append("🗓️ *التاريخ:* ${exam.date}\n")
                                                append("📝 *الامتحان:* ${exam.examName}\n")
                                                append("💯 *الدرجة:* ${exam.score} من ${exam.maxScore}\n")
                                                if (exam.notes.isNotBlank()) append("💬 *ملاحظة:* ${exam.notes}\n")
                                                append(" *نتيجة امتحان للطالب/ة:* ${student.fullName}\n")
                                            }
                                            reportShareMessage = msg
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Share,
                                            contentDescription = "مشاركة",
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Edit Button
                                    IconButton(
                                        onClick = {
                                            editingExamId = exam.id
                                            examName = exam.examName
                                            score = exam.score
                                            maxScore = exam.maxScore
                                            examDate = exam.date
                                            examNotes = exam.notes
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = "تعديل",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteIndividualExam(exam)
                                            if (editingExamId == exam.id) {
                                                editingExamId = null
                                                examName = ""
                                                score = ""
                                                examNotes = ""
                                            }
                                            Toast.makeText(context, "تم حذف الامتحان من السجل", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "حذف",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(17.dp)
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
}

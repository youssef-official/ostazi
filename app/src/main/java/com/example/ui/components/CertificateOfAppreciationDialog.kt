package com.example.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.print.PrintHelper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.StudentEntity
import com.example.utils.CertificateGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CertificateOfAppreciationDialog(
    student: StudentEntity,
    subjectName: String,
    initialTeacherName: String,
    initialExamName: String = "",
    initialScore: String = "",
    initialDate: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var teacherName by remember { mutableStateOf(initialTeacherName.ifBlank { "عبدالله الجرايحي" }) }
    var studentName by remember { mutableStateOf(student.fullName) }
    var examTitle by remember { mutableStateOf(if (initialExamName.isNotBlank()) initialExamName else subjectName.ifBlank { "اللغة العربية" }) }
    var scoreText by remember { mutableStateOf(initialScore) }
    var certificateDate by remember { mutableStateOf(initialDate) }
    var customPraise by remember { mutableStateOf("مع تمنياتنا بدوام التفوق والريادة") }

    var isEditingDetails by remember { mutableStateOf(false) }
    var certificateBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    fun refreshCertificate() {
        scope.launch {
            isGenerating = true
            val bmp = withContext(Dispatchers.Default) {
                CertificateGenerator.createCertificateBitmap(
                    context = context,
                    studentName = studentName,
                    teacherName = teacherName,
                    subjectOrExamName = examTitle,
                    scoreText = scoreText,
                    dateText = certificateDate,
                    customPraise = customPraise
                )
            }
            certificateBitmap = bmp
            isGenerating = false
        }
    }

    LaunchedEffect(studentName, teacherName, examTitle, scoreText, certificateDate, customPraise) {
        refreshCertificate()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
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
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "شهادة شكر وتقدير",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(22.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Certificate Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Preview Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = .28f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1600f / 1130f)
                    ) {
                        if (certificateBitmap != null && !isGenerating) {
                            Image(
                                bitmap = certificateBitmap!!.asImageBitmap(),
                                contentDescription = "معاينة شهادة التقدير",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF2563EB), modifier = Modifier.size(36.dp))
                            }
                        }
                    }

                    // Toggle Edit Details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { isEditingDetails = !isEditingDetails }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEditingDetails) "إخفاء بيانات التعديل ▲" else "تعديل نصوص وبيانات الشهادة ▼",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
                    }

                    if (isEditingDetails) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = studentName,
                                    onValueChange = { studentName = it },
                                    label = { Text("اسم الطالب/ة", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                                )
                                OutlinedTextField(
                                    value = teacherName,
                                    onValueChange = { teacherName = it },
                                    label = { Text("اسم الأستاذ/ة", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = examTitle,
                                    onValueChange = { examTitle = it },
                                    label = { Text("المادة أو الاختبار", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                                )
                                OutlinedTextField(
                                    value = scoreText,
                                    onValueChange = { scoreText = it },
                                    label = { Text("الدرجة (اختياري)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    placeholder = { Text("مثال: 50 من 50") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                                )
                            }

                            OutlinedTextField(
                                value = customPraise,
                                onValueChange = { customPraise = it },
                                label = { Text("عبارة تشجيعية ختامية", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary Action: Send to WhatsApp
                    Button(
                        onClick = {
                            val bmp = certificateBitmap
                            if (bmp != null) {
                                val uri = CertificateGenerator.saveBitmapToCache(
                                    context = context,
                                    bitmap = bmp,
                                    filename = "certificate_${student.fullName.replace(" ", "_")}.png"
                                )
                                if (uri != null) {
                                    val phone = student.parentPhone.ifBlank { student.studentPhone }
                                    val caption = " *شهادة شكر وتقدير وتفوق* \n\nالسلام عليكم ورحمة الله وبركاته \nيسر الأستاذ *$teacherName* أن يقدم شهادة التقدير للطالب/ة المتميز/ة *${student.fullName}* تقديراً لأدائه وتفوقه في *$examTitle*.\nمع أطيب التمنيات بدوام التفوق والنجاح الباهر "
                                    CertificateGenerator.shareCertificateImage(
                                        context = context,
                                        imageUri = uri,
                                        phoneNumber = phone,
                                        caption = caption
                                    )
                                } else {
                                    Toast.makeText(context, "تعذر حفظ الشهادة للمشاركة", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إرسال شهادة التقدير لواتساب ولي الأمر",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                    }

                    // Secondary Action Buttons: Share & Print
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val bmp = certificateBitmap
                                if (bmp != null) {
                                    val uri = CertificateGenerator.saveBitmapToCache(
                                        context = context,
                                        bitmap = bmp,
                                        filename = "certificate_${student.fullName.replace(" ", "_")}.png"
                                    )
                                    if (uri != null) {
                                        CertificateGenerator.shareCertificateImage(
                                            context = context,
                                            imageUri = uri,
                                            phoneNumber = "",
                                            caption = " شهادة شكر وتقدير للطالب: ${student.fullName}"
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة الشهادة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val bmp = certificateBitmap
                                if (bmp != null) {
                                    try {
                                        val printHelper = PrintHelper(context).apply {
                                            scaleMode = PrintHelper.SCALE_MODE_FIT
                                        }
                                        printHelper.printBitmap("شهادة_تقدير_${student.fullName}", bmp)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "تعذر تشغيل الطباعة: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569))
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

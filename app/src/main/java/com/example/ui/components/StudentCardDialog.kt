package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GroupEntity
import com.example.data.StudentEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.utils.BarcodeUtils

fun formatCardPhoneDisplay(raw: String): String {
    val clean = raw.trim().replace(" ", "").replace("-", "")
    if (clean.isEmpty()) return ""
    // Standard Egyptian mobile format: 01x-xxxx-xxxx
    return if (clean.length == 11 && (clean.startsWith("010") || clean.startsWith("011") || clean.startsWith("012") || clean.startsWith("015"))) {
        "${clean.substring(0, 3)}-${clean.substring(3, 7)}-${clean.substring(7)}"
    } else {
        clean
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCardDialog(
    viewModel: MainViewModel,
    initialStudent: StudentEntity? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState(initial = emptyList())
    val groups by viewModel.groups.collectAsState(initial = emptyList())
    val teacherProfile by viewModel.teacherProfile.collectAsState()

    var selectedStudent by remember { 
        mutableStateOf(initialStudent ?: students.firstOrNull()) 
    }
    var showStudentSelector by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Front or Back toggle
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(500),
        label = "cardFlip"
    )

    val currentGroup = groups.firstOrNull { it.id == selectedStudent?.groupId }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PremiumDialogDirectionGuard()
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f)),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.92f)
                    .padding(vertical = 16.dp),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = SkyPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Badge,
                                        contentDescription = null,
                                        tint = SkyPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "بطاقة الطالب الذكية ",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SkyOnSurface
                                )
                                Text(
                                    text = "كارت الوجه والظهر مع الباركود والبيانات",
                                    fontSize = 11.sp,
                                    color = SkyOnSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "إغلاق", tint = SkyOnSurface, modifier = Modifier.size(20.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    val student = selectedStudent
                    if (student != null) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Selector for student
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showStudentSelector = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Person, contentDescription = null, tint = SkyPrimary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = student.fullName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = SkyOnSurface
                                            )
                                            Text(
                                                text = "المجموعة: ${currentGroup?.name ?: "غير محدد"}",
                                                fontSize = 11.sp,
                                                color = SkyOnSurfaceVariant
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SkyPrimary.copy(alpha = 0.12f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("تغيير الطالب", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SkyPrimary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = SkyPrimary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Card Front / Back Toggle Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (!isFlipped) SkyPrimary else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { isFlipped = false }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.ContactPage,
                                            contentDescription = null,
                                            tint = if (!isFlipped) Color.White else SkyOnSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "وجه البطاقة (بيانات الطالب)",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (!isFlipped) FontWeight.Bold else FontWeight.Medium,
                                            color = if (!isFlipped) Color.White else SkyOnSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isFlipped) SkyPrimary else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { isFlipped = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.FlipCameraAndroid,
                                            contentDescription = null,
                                            tint = if (isFlipped) Color.White else SkyOnSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ظهر البطاقة (بيانات المعلم)",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isFlipped) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isFlipped) Color.White else SkyOnSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Student Barcode Bitmap
                            val barcodeData = "STUDENT_${student.id}"
                            val qrBitmap = remember(student.id) {
                                BarcodeUtils.generateQrCodeBitmap(barcodeData, 260, 260)
                            }

                            // 3D Card Display Container with Flip Animation
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        rotationY = rotation
                                        cameraDistance = 12f * density
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (rotation <= 90f) {
                                    // FRONT OF CARD
                                    StudentCardFront(
                                        student = student,
                                        group = currentGroup,
                                        qrBitmap = qrBitmap
                                    )
                                } else {
                                    // BACK OF CARD (Mirrored back for readability)
                                    Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                                        StudentCardBack(
                                            teacherName = teacherProfile.name,
                                            teacherSubject = teacherProfile.subject,
                                            group = currentGroup
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = " اضغط على الزر بالأعلى للتبديل بين وجه وظهر البطاقة",
                                fontSize = 11.sp,
                                color = SkyOnSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val groupTypeArabic = when (currentGroup?.groupType) {
                                        "ONLINE" -> "أونلاين"
                                        "PRIVATE" -> "خاص"
                                        else -> "سنتر"
                                    }
                                    val pPhone = formatCardPhoneDisplay(student.parentPhone).ifBlank { "غير مسجل" }
                                    val sPhone = formatCardPhoneDisplay(student.studentPhone).ifBlank { "غير مسجل" }
                                    val shareText = """
                                         *بطاقة طالب ذكية - تطبيق أستاذي*
                                        ═══════════════════
                                        👤 *اسم الطالب:* ${student.fullName}
                                        📚 *المرحلة / المجموعة:* ${currentGroup?.name ?: "المجموعة"}
                                        🏢 *نوع الدرس:* $groupTypeArabic
                                        👨‍🏫 *المعلم:* ${teacherProfile.name}
                                        📖 *المادة:* ${teacherProfile.subject}
                                        📞 *هاتف ولي الأمر:* $pPhone
                                         *اسم الطالب:* ${student.fullName}
                                        ⏰ *المواعيد:* ${currentGroup?.day1 ?: ""} ${currentGroup?.timeSlot ?: ""}
                                         *كود الطالب:* STUDENT_${student.id}
                                    """.trimIndent()
                                    
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "مشاركة بيانات بطاقة الطالب"))
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مشاركة البطاقة ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    StudentCardPdfHelper.generateBatchStudentCardsPdf(
                                        context = context,
                                        students = listOf(student),
                                        groups = listOfNotNull(currentGroup),
                                        teacherName = teacherProfile.name,
                                        teacherSubject = teacherProfile.subject,
                                        groupNameFilter = student.fullName
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SkyPrimary)
                            ) {
                                Icon(Icons.Outlined.Print, contentDescription = null, tint = SkyPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة الكارت ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SkyPrimary)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا يوجد طلاب متاحين حالياً. يرجى إضافة طلاب أولاً.",
                                color = SkyOnSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Student Selector Dialog
                if (showStudentSelector) {
                    PremiumAlertDialog(
                        onDismissRequest = { showStudentSelector = false },
                        title = {
                            Text("اختر الطالب لإنشاء البطاقة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("بحث عن اسم الطالب...") },
                                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                val filteredStudents = students.filter { 
                                    it.fullName.contains(searchQuery, ignoreCase = true)
                                }
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                ) {
                                    items(filteredStudents) { st ->
                                        val stGroup = groups.firstOrNull { it.id == st.groupId }
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedStudent = st
                                                    showStudentSelector = false
                                                }
                                                .padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (st.id == selectedStudent?.id) SkyPrimary.copy(alpha = 0.15f) else Color.Transparent
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Outlined.Person, contentDescription = null, tint = SkyPrimary, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(st.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("المجموعة: ${stGroup?.name ?: "غير محدد"}", fontSize = 11.sp, color = SkyOnSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showStudentSelector = false }) {
                                Text("إلغاء")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StudentCardFront(
    student: StudentEntity,
    group: GroupEntity?,
    qrBitmap: Bitmap?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card Top Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "أستاذي • كارت الطالب الذكي",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Center / Online / Private badge
                val (badgeText, badgeColor, badgeBg) = when (group?.groupType) {
                    "ONLINE" -> Triple("أونلاين", Color(0xFF38BDF8), Color(0xFF0C4A6E))
                    "PRIVATE" -> Triple("خاص", Color(0xFFA855F7), Color(0xFF581C87))
                    else -> Triple("سنتر", Color(0xFF60A5FA), Color(0xFF172554))
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Body: Right Info + Left Barcode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right Details (Student Details)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        Text(
                            text = "اسم الطالب",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = student.fullName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 19.sp
                        )
                    }

                    Column {
                        Text(
                            text = "المرحلة الدراسية / المجموعة",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = group?.name ?: "المجموعة الدراسية",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    val parentFormatted = formatCardPhoneDisplay(student.parentPhone)
                    val studentFormatted = formatCardPhoneDisplay(student.studentPhone)

                    if (parentFormatted.isNotBlank() || studentFormatted.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            if (parentFormatted.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = " ولي الأمر:",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Medium
                                    )
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = "\u202A$parentFormatted\u202C",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF1F5F9)
                                        )
                                    }
                                }
                            }
                            if (studentFormatted.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = " الطالب:",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Medium
                                    )
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = "\u202A$studentFormatted\u202C",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF1F5F9)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = " لم يتم تسجيل هاتف",
                            fontSize = 10.sp,
                            color = Color(0xFFF87171)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Left Side: Barcode / QR Code
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier
                            .size(100.dp)
                            .padding(4.dp)
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Student QR Code",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text("BARCODE", fontSize = 10.sp, color = Color.Black)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ID: ${student.id}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
fun StudentCardBack(
    teacherName: String,
    teacherSubject: String,
    group: GroupEntity?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E1B4B),
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = teacherName,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF8B5CF6).copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "مادة: $teacherSubject",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDDD6FE),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            if (group != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "المواعيد: ${group.day1} ${group.timeSlot}",
                    fontSize = 11.sp,
                    color = Color(0xFFC4B5FD)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(color = Color(0xFF4C1D95).copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth(0.8f))

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = " أستاذي - المنظومة الذكية لإدارة الدروس والطلاب ",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF93C5FD)
            )
        }
    }
}

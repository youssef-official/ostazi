package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GroupEntity
import com.example.data.PaymentRecordEntity
import com.example.data.StudentEntity
import com.example.utils.PdfReportGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinancePdfDialog(
    selectedGroupName: String,
    monthName: String,
    year: Int,
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    paymentsList: List<PaymentRecordEntity>,
    totalCollected: Double,
    totalExpected: Double,
    totalRemaining: Double,
    unpaidCount: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var generatedFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(selectedGroupName, monthName, year, students, groups, paymentsList) {
        generatedFile = PdfReportGenerator.generateFinancePdf(
            context = context,
            selectedGroupName = selectedGroupName,
            monthName = monthName,
            year = year,
            students = students,
            groups = groups,
            paymentsList = paymentsList,
            totalCollected = totalCollected,
            totalExpected = totalExpected,
            totalRemaining = totalRemaining,
            unpaidCount = unpaidCount
        )
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
                // Top App Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
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
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Outlined.ArrowForward, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Column {
                                Text(
                                    text = "السجل-المالي-$monthName-$year.pdf",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$selectedGroupName • جاهز للطباعة والتصدير",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Print Button
                            IconButton(
                                onClick = {
                                    val file = generatedFile ?: PdfReportGenerator.generateFinancePdf(
                                        context, selectedGroupName, monthName, year, students, groups, paymentsList,
                                        totalCollected, totalExpected, totalRemaining, unpaidCount
                                    )
                                    if (file != null) {
                                        PdfReportGenerator.printPdf(context, file, "السجل المالي - $monthName $year")
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.Print, contentDescription = "طباعة", tint = Color(0xFF2563EB))
                            }

                            // Share Button
                            IconButton(
                                onClick = {
                                    val file = generatedFile ?: PdfReportGenerator.generateFinancePdf(
                                        context, selectedGroupName, monthName, year, students, groups, paymentsList,
                                        totalCollected, totalExpected, totalRemaining, unpaidCount
                                    )
                                    if (file != null) {
                                        PdfReportGenerator.sharePdf(context, file, "السجل المالي - $monthName $year")
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = "مشاركة PDF", tint = Color(0xFF059669))
                            }

                            // Open Button
                            IconButton(
                                onClick = {
                                    val file = generatedFile ?: PdfReportGenerator.generateFinancePdf(
                                        context, selectedGroupName, monthName, year, students, groups, paymentsList,
                                        totalCollected, totalExpected, totalRemaining, unpaidCount
                                    )
                                    if (file != null) {
                                        PdfReportGenerator.openPdf(context, file)
                                    }
                                }
                            ) {
                                Icon(Icons.Outlined.PictureAsPdf, contentDescription = "فتح كـ PDF", tint = Color(0xFFDC2626))
                            }
                        }
                    }
                }

                // Printable A4 Page View (Preview matching PDF layout)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Header Banner: Dark Slate Blue
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0F172A)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp, horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "أستاذي • التقرير المالي الشامل",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "السجل المالي والتحصيل",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$selectedGroupName • $monthName $year • إجمالي ${students.size} طالب",
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 4 Stats Summary Cards (Matching Image 2)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // المحصل
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("المحصل", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text("${totalCollected.toInt()} ج.م", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                    }
                                }

                                // إجمالي المتوقع
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("المتوقع", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text("${totalExpected.toInt()} ج.م", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                    }
                                }

                                // المتبقي
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("المتبقي", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text("${totalRemaining.toInt()} ج.م", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                    }
                                }

                                // عليهم مستحقات
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("مستحقين", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text("$unpaidCount طالب", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Table of Financial Records
                            val horizontalScroll = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(horizontalScroll)
                            ) {
                                // Header Row
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("#", modifier = Modifier.width(26.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF334155))
                                        Text("اسم الطالب", modifier = Modifier.width(120.dp), textAlign = TextAlign.Right, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF334155))
                                        Text("المجموعة", modifier = Modifier.width(80.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF334155))
                                        Text("الاشتراك", modifier = Modifier.width(60.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF334155))
                                        Text("المدفوع", modifier = Modifier.width(60.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF334155))
                                        Text("المتبقي", modifier = Modifier.width(60.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF334155))
                                        Text("الحالة", modifier = Modifier.width(75.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF334155))
                                        Text("تاريخ الدفع", modifier = Modifier.width(75.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF334155))
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                if (students.isEmpty()) {
                                    Text(
                                        text = "لا توجد سجلات مالية لهذه التصفية",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        textAlign = TextAlign.Center,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                } else {
                                    students.forEachIndexed { index, student ->
                                        val grp = groups.find { it.id == student.groupId }
                                        val fee = grp?.monthlyFee ?: 0.0
                                        val p = paymentsList.find { it.studentId == student.id }
                                        val status = p?.paymentStatus ?: "UNPAID"
                                        val remaining = when (status) {
                                            "PAID" -> 0.0
                                            "PARTIAL" -> p?.remainingAmount ?: 0.0
                                            "EXEMPT" -> 0.0
                                            else -> fee
                                        }
                                        val paid = when (status) {
                                            "PAID" -> fee
                                            "PARTIAL" -> (fee - remaining).coerceAtLeast(0.0)
                                            "EXEMPT" -> 0.0
                                            else -> 0.0
                                        }

                                        val rowBg = if (index % 2 == 1) Color(0xFFF8FAFC) else Color.White
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = rowBg
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp, horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("${index + 1}", modifier = Modifier.width(26.dp), textAlign = TextAlign.Center, fontSize = 10.sp, color = Color(0xFF475569))
                                                Text(student.fullName, modifier = Modifier.width(120.dp), textAlign = TextAlign.Right, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A), maxLines = 1)
                                                Text(grp?.name ?: "-", modifier = Modifier.width(80.dp), textAlign = TextAlign.Center, fontSize = 10.sp, color = Color(0xFF475569), maxLines = 1)
                                                Text("${fee.toInt()}", modifier = Modifier.width(60.dp), textAlign = TextAlign.Center, fontSize = 10.sp, color = Color(0xFF1E293B))
                                                Text("${paid.toInt()}", modifier = Modifier.width(60.dp), textAlign = TextAlign.Center, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                                Text("${remaining.toInt()}", modifier = Modifier.width(60.dp), textAlign = TextAlign.Center, fontSize = 10.sp, color = if (remaining > 0) Color(0xFFDC2626) else Color(0xFF64748B))

                                                // Status Pill
                                                Box(
                                                    modifier = Modifier.width(75.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    when (status) {
                                                        "PAID" -> Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFDCFCE7)) {
                                                            Text("مدفوع كامل", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                                        }
                                                        "PARTIAL" -> Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFEF3C7)) {
                                                            Text("دفع جزئي", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                                        }
                                                        "EXEMPT" -> Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF3E8FF)) {
                                                            Text("معفي", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7E22CE))
                                                        }
                                                        else -> Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFEE2E2)) {
                                                            Text("غير مسدد", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                                                        }
                                                    }
                                                }

                                                Text(p?.paymentDate?.ifEmpty { null } ?: "-", modifier = Modifier.width(75.dp), textAlign = TextAlign.Center, fontSize = 9.sp, color = Color(0xFF64748B))
                                            }
                                        }
                                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Footer Bottom
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "صفحة 1 من 1",
                                    fontSize = 9.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "أستاذي • تم التصدير بتاريخ ${SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date())}",
                                    fontSize = 9.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                // Bottom Buttons
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
                        Button(
                            onClick = {
                                val file = generatedFile ?: PdfReportGenerator.generateFinancePdf(
                                    context, selectedGroupName, monthName, year, students, groups, paymentsList,
                                    totalCollected, totalExpected, totalRemaining, unpaidCount
                                )
                                if (file != null) {
                                    PdfReportGenerator.sharePdf(context, file, "السجل المالي - $monthName $year")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مشاركة PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val file = generatedFile ?: PdfReportGenerator.generateFinancePdf(
                                    context, selectedGroupName, monthName, year, students, groups, paymentsList,
                                    totalCollected, totalExpected, totalRemaining, unpaidCount
                                )
                                if (file != null) {
                                    PdfReportGenerator.printPdf(context, file, "السجل المالي - $monthName $year")
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("طباعة السجل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

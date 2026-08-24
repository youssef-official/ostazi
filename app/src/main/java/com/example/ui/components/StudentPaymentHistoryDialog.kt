package com.example.ui.components

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GroupEntity
import com.example.data.PaymentRecordEntity
import com.example.data.StudentEntity
import com.example.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentPaymentHistoryDialog(
    student: StudentEntity,
    group: GroupEntity?,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val paymentsList by viewModel.getPaymentsForStudentFlow(student.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val teacherName by viewModel.teacherName.collectAsStateWithLifecycle(initialValue = "")

    val monthlyFee = group?.monthlyFee ?: 0.0

    var showAddOrEditDialog by remember { mutableStateOf<PaymentRecordEntity?>(null) }
    var showNewPaymentDialog by remember { mutableStateOf(false) }
    var showShareChannelDialog by remember { mutableStateOf(false) }

    // Sort payments descending by month/date
    val sortedPayments = remember(paymentsList) {
        paymentsList.sortedWith(compareByDescending<PaymentRecordEntity> { it.monthYear }
            .thenByDescending { it.paymentDate })
    }

    // Totals
    val totalPaid = sortedPayments.filter { it.paymentStatus == "PAID" }.sumOf { monthlyFee } +
            sortedPayments.filter { it.paymentStatus == "PARTIAL" }.sumOf { (monthlyFee - it.remainingAmount).coerceAtLeast(0.0) }
    val totalRemaining = sortedPayments.filter { it.paymentStatus == "PARTIAL" }.sumOf { it.remainingAmount } +
            sortedPayments.filter { it.paymentStatus == "UNPAID" }.sumOf { monthlyFee }
    val paidMonthsCount = sortedPayments.count { it.paymentStatus == "PAID" }
    val exemptMonthsCount = sortedPayments.count { it.paymentStatus == "EXEMPT" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PremiumDialogDirectionGuard()
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "سجل أيام ومدفوعات الطالب ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${student.fullName} • ${group?.name ?: "-"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Summary Financial Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("إجمالي المدفوع", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${totalPaid.toInt()} ج.م", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
                            Text("$paidMonthsCount شهور مسددة", fontSize = 10.sp, color = Color(0xFF16A34A))
                        }

                        VerticalDivider(modifier = Modifier.height(36.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("المتبقي / مستحق", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${totalRemaining.toInt()} ج.م", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (totalRemaining > 0) Color(0xFFDC2626) else Color(0xFF16A34A))
                            Text(if (totalRemaining > 0) "يوجد مستحقات" else "لا توجد متأخرات", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        VerticalDivider(modifier = Modifier.height(36.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("معفي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$exemptMonthsCount شهر", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                            Text("منح دراسية", fontSize = 10.sp, color = Color(0xFF7C3AED))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Bar: Add new payment / date record
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل التواريخ والدفعات (${sortedPayments.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = { showNewPaymentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تسجيل دفعة / تاريخ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Payment History List
                if (sortedPayments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد سجلات دفع مسجلة لهذا الطالب حتى الآن", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedPayments, key = { it.id }) { record ->
                            PaymentHistoryItemCard(
                                record = record,
                                monthlyFee = monthlyFee,
                                onEdit = { showAddOrEditDialog = record }
                            )
                        }
                    }
                }
            }
        }
    }

    // Record / Edit Payment Dialog with DatePicker Calendar
    if (showAddOrEditDialog != null || showNewPaymentDialog) {
        val existingRecord = showAddOrEditDialog
        PaymentRecordDatePickerDialog(
            student = student,
            groupFee = monthlyFee,
            existingRecord = existingRecord,
            isPerSession = group?.paymentType == "PER_SESSION",
            onDismiss = {
                showAddOrEditDialog = null
                showNewPaymentDialog = false
            },
            onSave = { monthYear, status, discount, remaining, paymentDate ->
                viewModel.setPaymentStatusWithDiscount(
                    studentId = student.id,
                    status = status,
                    discountAmount = discount,
                    paidAmount = if (status == "PAID") monthlyFee - discount else (monthlyFee - remaining - discount).coerceAtLeast(0.0),
                    remainingAmount = remaining,
                    paymentDate = paymentDate,
                    monthYear = monthYear
                )
                showAddOrEditDialog = null
                showNewPaymentDialog = false
                Toast.makeText(context, "تم حفظ سجل الدفع بنجاح ", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Share Statement Dialog
    if (showShareChannelDialog) {
        val statementMessage = buildString {
            append("السلام عليكم ورحمة الله وبركاته \n")
            append("تحية طيبة لولي أمر الطالب: *${student.fullName}*\n")
            append("إليكم كشف حساب وسجل مدفوعات الاشتراك:\n\n")
            append("📚 المجموعة: ${group?.name ?: "-"}\n")
            append("💵 قيمة الاشتراك الشهري: ${monthlyFee.toInt()} ج.م\n")
            append(" المجموعة: ${group?.name ?: "-"}\n")
            if (totalRemaining > 0) {
                append(" إجمالي المتبقي: ${totalRemaining.toInt()} ج.م\n")
            } else {
                append(" الحالة: مسدد بالكامل ولا توجد متأخرات\n")
            }
            append("\n *تفاصيل سجل الدفعات بالتواريخ:*\n")
            sortedPayments.forEach { p ->
                val statusText = when (p.paymentStatus) {
                    "PAID" -> "مسدد بالكامل"
                    "PARTIAL" -> "دفع جزئي (متبقي ${p.remainingAmount.toInt()} ج)"
                    "EXEMPT" -> "معفي من المصاريف"
                    else -> "غير مسدد"
                }
                val dateStr = if (p.paymentDate.isNotBlank()) " [تاريخ: ${p.paymentDate}]" else ""
                append("• شهر ${p.monthYear}: $statusText$dateStr\n")
            }
            if (teacherName.isNotBlank()) {
                append("\nمع تحيات: $teacherName ")
            }
        }

        ReportChannelSelectionDialog(
            recipientName = "ولي أمر الطالب: ${student.fullName}",
            phoneNumber = student.parentPhone,
            reportMessage = statementMessage,
            onDismiss = { showShareChannelDialog = false }
        )
    }
}

@Composable
private fun PaymentHistoryItemCard(
    record: PaymentRecordEntity,
    monthlyFee: Double,
    onEdit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Icon Status
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (record.paymentStatus) {
                            "PAID" -> Color(0xFFDCFCE7)
                            "PARTIAL" -> MaterialTheme.colorScheme.tertiaryContainer
                            "EXEMPT" -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (record.paymentStatus) {
                        "PAID" -> Icons.Outlined.Check
                        "PARTIAL" -> Icons.Outlined.AttachMoney
                        "EXEMPT" -> Icons.Outlined.CardGiftcard
                        else -> Icons.Outlined.Close
                    },
                    contentDescription = null,
                    tint = when (record.paymentStatus) {
                        "PAID" -> Color(0xFF15803D)
                        "PARTIAL" -> Color(0xFFB45309)
                        "EXEMPT" -> Color(0xFF7E22CE)
                        else -> Color(0xFFB91C1C)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            // Month and Status Details
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status Badge
                    when (record.paymentStatus) {
                        "PAID" -> Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                            Text("مسدد بالكامل ", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                        "PARTIAL" -> Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text("متبقي: ${record.remainingAmount.toInt()} ج", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        }
                        "EXEMPT" -> Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("معفي ", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7E22CE))
                        }
                        else -> Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.errorContainer) {
                            Text("غير مسدد ", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                        }
                    }

                    Text(
                        text = "شهر ${record.monthYear}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Date of payment
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (record.paymentDate.isNotBlank()) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(12.dp))
                        Text(
                            text = "يوم الدفع: ${record.paymentDate}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "لم يحدد يوم دفع دقيق",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Edit Button
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.EditCalendar, contentDescription = "تعديل", tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
            }
        }
    }
}

/**
 * Dialog to record / edit payment with Calendar DatePicker
 * Options:
 * 1. مسدد (Paid)
 * 2. خصم / متبقي (Discount / Partial)
 * 3. معفي (Exempt)
 * 4. غير مسدد (Unpaid)
 */
@Composable
fun PaymentRecordDatePickerDialog(
    student: StudentEntity,
    groupFee: Double,
    existingRecord: PaymentRecordEntity?,
    isPerSession: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (monthYear: String, status: String, discount: Double, remaining: Double, paymentDate: String) -> Unit
) {
    val context = LocalContext.current
    val cal = remember { Calendar.getInstance() }
    val todayFormatted = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()) }
    val currentMonthYearFormatted = remember { SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date()) }

    var selectedDate by remember {
        mutableStateOf(existingRecord?.paymentDate?.ifBlank { todayFormatted } ?: todayFormatted)
    }

    var selectedMonthYear by remember {
        mutableStateOf(existingRecord?.monthYear ?: currentMonthYearFormatted)
    }

    // Status: "PAID", "DISCOUNT", "REMAINING", "EXEMPT", "UNPAID"
    var paymentStatus by remember {
        mutableStateOf(
            if (existingRecord?.paymentStatus == "PARTIAL") {
                if ((existingRecord.discountAmount) > 0.0) "DISCOUNT" else "REMAINING"
            } else {
                existingRecord?.paymentStatus ?: "PAID"
            }
        )
    }

    var discountAmountText by remember {
        mutableStateOf(if ((existingRecord?.discountAmount ?: 0.0) > 0) existingRecord?.discountAmount?.toInt().toString() else "")
    }

    var remainingAmountText by remember {
        mutableStateOf(if ((existingRecord?.remainingAmount ?: 0.0) > 0) existingRecord?.remainingAmount?.toInt().toString() else "")
    }

    // Calendar DatePicker Launcher
    fun showDatePicker() {
        val curParts = selectedDate.split("-")
        val y = curParts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        val m = (curParts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
        val d = curParts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format(Locale.ENGLISH, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                selectedDate = formatted
                // Auto sync month-year
                selectedMonthYear = String.format(Locale.ENGLISH, "%04d-%02d", year, month + 1)
            },
            y, m, d
        ).show()
    }

    PremiumAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = Color(0xFF2563EB))
                Text("تسجيل سداد وتحديد يوم الدفع ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "الطالب: ${student.fullName} (قيمة الاشتراك: ${groupFee.toInt()} ج.م)",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                // 1. Calendar Day Picker Button
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(" تاريخ الدفع (اختر من التقويم):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تغيير اليوم ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(selectedDate, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E3A8A))
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // 2. Payment Status Options (مسدد, خصم, متبقي, معفي, غير مسدد)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("خيارات حالة الدفع:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // مسدد
                        FilterChip(
                            selected = paymentStatus == "PAID",
                            onClick = { paymentStatus = "PAID" },
                            label = { Text("مسدد ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        // خصم
                        FilterChip(
                            selected = paymentStatus == "DISCOUNT",
                            onClick = { paymentStatus = "DISCOUNT" },
                            label = { Text("خصم ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        // متبقي
                        FilterChip(
                            selected = paymentStatus == "REMAINING",
                            onClick = { paymentStatus = "REMAINING" },
                            label = { Text("متبقي", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // معفي
                        FilterChip(
                            selected = paymentStatus == "EXEMPT",
                            onClick = { paymentStatus = "EXEMPT" },
                            label = { Text("معفي ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        // غير مسدد
                        FilterChip(
                            selected = paymentStatus == "UNPAID",
                            onClick = { paymentStatus = "UNPAID" },
                            label = { Text("غير مسدد ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // If DISCOUNT, show only discount field
                if (paymentStatus == "DISCOUNT") {
                    OutlinedTextField(
                        value = discountAmountText,
                        onValueChange = { discountAmountText = it },
                        label = { Text("مبلغ الخصم (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                // If REMAINING, show only remaining field
                if (paymentStatus == "REMAINING") {
                    OutlinedTextField(
                        value = remainingAmountText,
                        onValueChange = { remainingAmountText = it },
                        label = { Text("المبلغ المتبقي (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val disc = if (paymentStatus == "DISCOUNT") (discountAmountText.toDoubleOrNull() ?: 0.0) else 0.0
                    val rem = if (paymentStatus == "REMAINING") (remainingAmountText.toDoubleOrNull() ?: 0.0) else 0.0
                    val finalStatus = if (paymentStatus == "DISCOUNT" || paymentStatus == "REMAINING") "PARTIAL" else paymentStatus
                    onSave(if (isPerSession) selectedDate else selectedMonthYear, finalStatus, disc, rem, selectedDate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("حفظ الدفعة والتاريخ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

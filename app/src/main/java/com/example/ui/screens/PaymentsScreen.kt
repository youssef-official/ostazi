package com.example.ui.screens
import androidx.compose.ui.text.style.TextOverflow

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GroupEntity
import com.example.data.PaymentRecordEntity
import com.example.data.StudentEntity
import com.example.ui.MainViewModel
import com.example.ui.components.ReportChannelSelectionDialog
import com.example.ui.components.StudentPaymentHistoryDialog
import com.example.ui.components.PaymentRecordDatePickerDialog
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.ui.components.FinancePdfDialog
import com.example.ui.components.FinancialWaterBarCard
import com.example.ui.components.PremiumAlertDialog
import com.example.ui.components.PremiumIconTile
import com.example.ui.components.premiumTextFieldColors
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val teacherName by viewModel.teacherName.collectAsStateWithLifecycle(initialValue = "")
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    val regularGroups = remember(groups) { groups.filter { it.paymentType != "PER_SESSION" } }

    val cal = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonthIndex by remember { mutableStateOf(cal.get(Calendar.MONTH)) }

    val monthYearStr = remember(selectedYear, selectedMonthIndex) {
        String.format(Locale.ENGLISH, "%04d-%02d", selectedYear, selectedMonthIndex + 1)
    }

    val paymentsFlow = remember(monthYearStr) {
        viewModel.getPaymentsForMonthFlow(monthYearStr)
    }
    val paymentsList by paymentsFlow.collectAsState(initial = emptyList())

    val monthName = ARABIC_MONTHS.find { it.second == selectedMonthIndex }?.first ?: "الشهر الحالي"

    // Filters
    var selectedGroupTypeFilter by remember { mutableStateOf("ALL") } // ALL, CENTER, ONLINE, PRIVATE
    var selectedGroupFilterId by remember { mutableStateOf<Int?>(null) } // null = All
    var selectedPaymentStatusFilter by remember { mutableStateOf("ALL") } // ALL, PAID, PARTIAL, EXEMPT, UNPAID
    var searchQuery by remember { mutableStateOf("") }

    // Dropdown expansion states
    var expandedTypeDropdown by remember { mutableStateOf(false) }
    var expandedGroupDropdown by remember { mutableStateOf(false) }
    var expandedMonthDropdown by remember { mutableStateOf(false) }

    // Dialog state for PDF Export
    var showFinancePdfDialog by remember { mutableStateOf(false) }

    // Dialog for Partial Payment edit
    var studentForPartialPayment by remember { mutableStateOf<Pair<StudentEntity, Double>?>(null) }

    // Dialog for Discount Payment edit
    var studentForDiscountPayment by remember { mutableStateOf<Pair<StudentEntity, Double>?>(null) }

    // Filtered Students
    val filteredStudents = remember(allStudents, regularGroups, selectedGroupTypeFilter, selectedGroupFilterId, searchQuery, selectedPaymentStatusFilter, paymentsList) {
        allStudents.filter { student ->
            val grp = regularGroups.find { it.id == student.groupId } ?: return@filter false
            
            // Type filter
            val matchesType = when (selectedGroupTypeFilter) {
                "CENTER" -> grp.groupType == "CENTER" || grp.groupType == "سنتر"
                "ONLINE" -> grp.groupType == "ONLINE" || grp.groupType == "أونلاين"
                "PRIVATE" -> grp.groupType == "PRIVATE" || grp.groupType == "برايفت"
                else -> true
            }

            // Group filter
            val matchesGroup = selectedGroupFilterId == null || student.groupId == selectedGroupFilterId

            // Search query
            val matchesSearch = searchQuery.isBlank() || 
                    student.fullName.contains(searchQuery, ignoreCase = true) || 
                    student.parentPhone.contains(searchQuery)

            // Status filter
            val p = paymentsList.find { it.studentId == student.id }
            val status = p?.paymentStatus ?: "UNPAID"
            val matchesStatus = when (selectedPaymentStatusFilter) {
                "PAID" -> status == "PAID"
                "DISCOUNT" -> status == "PARTIAL" && (p?.discountAmount ?: 0.0) > 0.0
                "REMAINING" -> status == "PARTIAL" && (p?.remainingAmount ?: 0.0) > 0.0
                "EXEMPT" -> status == "EXEMPT"
                "UNPAID" -> status == "UNPAID"
                else -> true
            }

            matchesType && matchesGroup && matchesSearch && matchesStatus
        }
    }

    // Calculations based on current filter or all
    val relevantStudents = remember(allStudents, regularGroups, selectedGroupTypeFilter, selectedGroupFilterId) {
        allStudents.filter { student ->
            val grp = regularGroups.find { it.id == student.groupId } ?: return@filter false
            val matchesType = when (selectedGroupTypeFilter) {
                "CENTER" -> grp.groupType == "CENTER" || grp.groupType == "سنتر"
                "ONLINE" -> grp.groupType == "ONLINE" || grp.groupType == "أونلاين"
                "PRIVATE" -> grp.groupType == "PRIVATE" || grp.groupType == "برايفت"
                else -> true
            }
            val matchesGroup = selectedGroupFilterId == null || student.groupId == selectedGroupFilterId
            matchesType && matchesGroup
        }
    }

    var totalExpected = 0.0
    var totalCollected = 0.0
    var totalRemaining = 0.0
    var unpaidCount = 0

    relevantStudents.forEach { student ->
        val grp = groups.find { it.id == student.groupId }
        val fee = grp?.monthlyFee ?: 0.0
        val p = paymentsList.find { it.studentId == student.id }
        when (p?.paymentStatus) {
            "PAID" -> {
                totalExpected += fee
                totalCollected += fee
            }
            "PARTIAL" -> {
                totalExpected += fee
                val rem = p.remainingAmount
                totalCollected += (fee - rem).coerceAtLeast(0.0)
                totalRemaining += rem
                if (rem > 0) unpaidCount++
            }
            "EXEMPT" -> {
                // Exempted
            }
            else -> { // UNPAID
                totalExpected += fee
                totalRemaining += fee
                unpaidCount++
            }
        }
    }

    val selectedGroupTitle = remember(selectedGroupFilterId, groups, selectedGroupTypeFilter) {
        if (selectedGroupFilterId != null) {
            groups.find { it.id == selectedGroupFilterId }?.name ?: "مجموعة محددة"
        } else {
            when (selectedGroupTypeFilter) {
                "CENTER" -> "مجموعات السنتر"
                "ONLINE" -> "مجموعات الأونلاين"
                "PRIVATE" -> "حصص البرايفت"
                else -> "كل المجموعات والبرايفت"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // --- Financial Summary Card (title and export stay together) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header: Title & PDF Button
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        OutlinedButton(
                            onClick = { showFinancePdfDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Text(
                            text = "السجل المالي",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        }
                    }
                    
                    Text(
                        text = "كل المجموعات والبرايفت • $monthName $selectedYear",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                        textAlign = TextAlign.End
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    val collectedPercentage = if (totalExpected > 0) (totalCollected / totalExpected * 100).toInt() else 0
                    
                    // Inner Content: Stats & Jar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            // Stats (Left)
                            Column(
                                modifier = Modifier.weight(0.6f),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "$collectedPercentage% محصل",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("ملخص التحصيل المالي", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("", fontSize = 16.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                StatRowImageMatch(label = "المبلغ المتبقي", value = "${totalRemaining.toInt()} ج.م", icon = Icons.Outlined.HourglassBottom, color = MaterialTheme.colorScheme.error)
                                StatRowImageMatch(label = "المبلغ المحصل", value = "${totalCollected.toInt()} ج.م", icon = Icons.Outlined.Payments, color = MaterialTheme.colorScheme.tertiary)
                                StatRowImageMatch(label = "المبلغ المتوقع", value = "${totalExpected.toInt()} ج.م", icon = Icons.Outlined.AccountBalanceWallet, color = MaterialTheme.colorScheme.primary)
                                
                                // Progress bars at bottom of stats
                                Row(modifier = Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.weight(0.3f).fillMaxHeight().clip(CircleShape).background(MaterialTheme.colorScheme.outline))
                                    Box(modifier = Modifier.weight(0.7f).fillMaxHeight().clip(CircleShape).background(Color(0xFF0284C7)))
                                }
                            }

                            }
                        }
                    }
                }
            }
        }

        // --- 3. Filters Section ("تصفية حسب" - Matching Image 2) ---
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "تصفية حسب",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Box 3: Period / Month
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الشهر",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedMonthDropdown = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "$monthName $selectedYear",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = expandedMonthDropdown,
                                onDismissRequest = { expandedMonthDropdown = false }
                            ) {
                                ARABIC_MONTHS.forEach { (mName, mIdx) ->
                                    DropdownMenuItem(
                                        text = { Text("$mName $selectedYear", fontSize = 12.sp, fontWeight = if (mIdx == selectedMonthIndex) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            selectedMonthIndex = mIdx
                                            expandedMonthDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Box 2: Specific Group
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(
                            text = "المجموعة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedGroupDropdown = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    val currentGroupName = regularGroups.find { it.id == selectedGroupFilterId }?.name ?: "كل المجموعات"
                                    Text(
                                        text = currentGroupName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = expandedGroupDropdown,
                                onDismissRequest = { expandedGroupDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("كل المجموعات", fontSize = 12.sp, fontWeight = if (selectedGroupFilterId == null) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedGroupFilterId = null
                                        expandedGroupDropdown = false
                                    }
                                )
                                regularGroups.forEach { grp ->
                                    DropdownMenuItem(
                                        text = { Text(grp.name, fontSize = 12.sp, fontWeight = if (selectedGroupFilterId == grp.id) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            selectedGroupFilterId = grp.id
                                            expandedGroupDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Box 1: Group Type Filter
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(
                            text = "النوع",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedTypeDropdown = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = when (selectedGroupTypeFilter) {
                                            "CENTER" -> "السنتر"
                                            "ONLINE" -> "أونلاين"
                                            "PRIVATE" -> "برايفت"
                                            else -> "الكل"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = expandedTypeDropdown,
                                onDismissRequest = { expandedTypeDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("الكل", fontSize = 12.sp, fontWeight = if (selectedGroupTypeFilter == "ALL") FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedGroupTypeFilter = "ALL"
                                        expandedTypeDropdown = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("مجموعات السنتر ", fontSize = 12.sp, fontWeight = if (selectedGroupTypeFilter == "CENTER") FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedGroupTypeFilter = "CENTER"
                                        expandedTypeDropdown = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("مجموعات الأونلاين ", fontSize = 12.sp, fontWeight = if (selectedGroupTypeFilter == "ONLINE") FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedGroupTypeFilter = "ONLINE"
                                        expandedTypeDropdown = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("حصص البرايفت ", fontSize = 12.sp, fontWeight = if (selectedGroupTypeFilter == "PRIVATE") FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedGroupTypeFilter = "PRIVATE"
                                        expandedTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Section Header: "السجل المالي" + Status Tabs + Search ---
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Text(
                        text = "العدد: ${filteredStudents.size} طالب",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "السجل المالي",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث عن طالب بالاسم أو رقم الهاتف...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "مسح")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Status Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusFilters = listOf(
                        "ALL" to "الكل (${relevantStudents.size})",
                        "PAID" to "المسددين ",
                        "UNPAID" to "غير المسددين",
                        "REMAINING" to "متبقي",
                        "EXEMPT" to "معفيين "
                    )
                    statusFilters.forEach { (statusKey, statusLabel) ->
                        val isSelected = selectedPaymentStatusFilter == statusKey
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = .5f) else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.clickable {
                                selectedPaymentStatusFilter = statusKey
                            }
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

            }
        }

        // --- 5. Students Financial List ---
        if (filteredStudents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("لا يوجد طلاب يطابقون خيارات التصفية الحالية", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    }
                }
            }
        } else {
            items(filteredStudents, key = { it.id }) { student ->
                val grp = groups.find { it.id == student.groupId }
                val fee = grp?.monthlyFee ?: 0.0
                val payment = paymentsList.find { it.studentId == student.id }
                val pStatus = payment?.paymentStatus ?: "UNPAID"
                val remaining = when (pStatus) {
                    "PAID" -> 0.0
                    "PARTIAL" -> payment?.remainingAmount ?: 0.0
                    "EXEMPT" -> 0.0
                    else -> fee
                }

                val discount = payment?.discountAmount ?: 0.0

                StudentFinanceCardItem(
                    student = student,
                    group = grp,
                    fee = fee,
                    paymentStatus = pStatus,
                    remainingAmount = remaining,
                    discountAmount = discount,
                    paymentDate = payment?.paymentDate,
                    teacherName = teacherName,
                    monthName = monthName,
                    year = selectedYear,
                    onMarkPaid = {
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
                        viewModel.setPaymentStatus(student.id, "PAID", 0.0, todayStr, monthYearStr)
                        Toast.makeText(context, "تم تسجيل سداد كامل لـ ${student.fullName}", Toast.LENGTH_SHORT).show()
                    },
                    onMarkDiscount = {
                        studentForDiscountPayment = Pair(student, fee)
                    },
                    onMarkPartial = {
                        studentForPartialPayment = Pair(student, fee)
                    },
                    onMarkExempt = {
                        viewModel.setPaymentStatus(student.id, "EXEMPT", 0.0, null, monthYearStr)
                        Toast.makeText(context, "تم إعفاء الطالب من الاشتراك", Toast.LENGTH_SHORT).show()
                    },
                    onMarkUnpaid = {
                        viewModel.setPaymentStatus(student.id, "UNPAID", 0.0, null, monthYearStr)
                        Toast.makeText(context, "تم إلغاء السداد", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // --- Discount Payment Dialog ---
    studentForDiscountPayment?.let { (student, fee) ->
        var discountInputText by remember { mutableStateOf("") }
        val discountVal = discountInputText.toDoubleOrNull() ?: 0.0
        val remainingAfterDiscount = (fee - discountVal).coerceAtLeast(0.0)

        PremiumAlertDialog(
            onDismissRequest = { studentForDiscountPayment = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumIconTile(Icons.Outlined.LocalOffer, null)
                    Column {
                        Text("تسجيل خصم للطالب", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        Text(student.fullName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = discountInputText,
                        onValueChange = { discountInputText = it },
                        label = { Text("قيمة الخصم (ج.م)") },
                        placeholder = { Text("مثال: 50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Payments, contentDescription = null) },
                        colors = premiumTextFieldColors()
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("المبلغ المطلوب بعد الخصم", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 11.sp)
                            Text("${remainingAfterDiscount.toInt()} ج.م", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dVal = discountInputText.toDoubleOrNull() ?: 0.0
                        val rem = (fee - dVal).coerceAtLeast(0.0)
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
                        viewModel.setPaymentStatusWithDiscount(
                            studentId = student.id,
                            status = if (rem == 0.0) "PAID" else "PARTIAL",
                            discountAmount = dVal,
                            paidAmount = 0.0,
                            remainingAmount = rem,
                            paymentDate = todayStr,
                            monthYear = monthYearStr
                        )
                        studentForDiscountPayment = null
                        Toast.makeText(context, "تم تسجيل الخصم بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حفظ الخصم")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentForDiscountPayment = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- Partial Payment Dialog ---
    studentForPartialPayment?.let { (student, fee) ->
        var partialAmountText by remember { mutableStateOf((fee / 2).toInt().toString()) }
        PremiumAlertDialog(
            onDismissRequest = { studentForPartialPayment = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumIconTile(Icons.Outlined.AccountBalanceWallet, null)
                    Column {
                        Text("تسجيل دفع جزئي ومتبقي", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        Text(student.fullName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = partialAmountText,
                        onValueChange = { partialAmountText = it },
                        label = { Text("المبلغ المتبقي على الطالب (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Payments, contentDescription = null) },
                        colors = premiumTextFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val remVal = partialAmountText.toDoubleOrNull() ?: 0.0
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
                        viewModel.setPaymentStatus(student.id, "PARTIAL", remVal, todayStr, monthYearStr)
                        studentForPartialPayment = null
                        Toast.makeText(context, "تم تسجيل الدفع الجزئي بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentForPartialPayment = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- PDF Export Preview Dialog ---
    if (showFinancePdfDialog) {
        FinancePdfDialog(
            selectedGroupName = selectedGroupTitle,
            monthName = monthName,
            year = selectedYear,
            students = relevantStudents,
            groups = groups,
            paymentsList = paymentsList,
            totalCollected = totalCollected,
            totalExpected = totalExpected,
            totalRemaining = totalRemaining,
            unpaidCount = unpaidCount,
            onDismiss = { showFinancePdfDialog = false }
        )
    }
}

/**
 * Glass revenue jar visualization card (زجاجة مالية تفاعلية مع ماء متحرك)
 */
@Composable
fun GlassSavingsJarCard(
    totalCollected: Double,
    totalExpected: Double,
    totalRemaining: Double,
    monthName: String,
    year: Int
) {
    val progress = remember(totalCollected, totalExpected) {
        if (totalExpected > 0) (totalCollected / totalExpected).toFloat().coerceIn(0f, 1f) else 0f
    }
    val percentage = (progress * 100).toInt()

    // Smooth wave animation loop
    val infiniteTransition = rememberInfiniteTransition(label = "waterAnimation")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 2600, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val wavePhaseBack by infiniteTransition.animateFloat(
        initialValue = (Math.PI).toFloat(),
        targetValue = (3f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 3600, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "wavePhaseBack"
    )

    val bubbleAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 2200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "bubbleAnim"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primaryContainer)
                    )
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Text(
                            text = "$percentage% محصل",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF166534),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "زجاجة الإيرادات الشهرية ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Enhanced Glass Money Jar / Premium Bottle (Left)
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(145.dp)
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // 1. Wooden Cork Stopper (Top) - More Detail
                            drawRoundRect(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color(0xFF78350F),
                                        androidx.compose.ui.graphics.Color(0xFFB45309),
                                        androidx.compose.ui.graphics.Color(0xFF78350F)
                                    )
                                ),
                                topLeft = androidx.compose.ui.geometry.Offset(w * 0.38f, 0f),
                                size = androidx.compose.ui.geometry.Size(w * 0.24f, 12f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                            // Jar Lip / Glass Neck Ring - Refined
                            drawRoundRect(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color(0xFF94A3B8),
                                        androidx.compose.ui.graphics.Color(0xFFF1F5F9),
                                        androidx.compose.ui.graphics.Color(0xFF94A3B8)
                                    )
                                ),
                                topLeft = androidx.compose.ui.geometry.Offset(w * 0.30f, 10f),
                                size = androidx.compose.ui.geometry.Size(w * 0.40f, 8f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )

                            // 2. Glass Bottle Outer Body Path - More "Jar" like
                            val bottlePath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w * 0.32f, 18f)
                                lineTo(w * 0.68f, 18f)
                                quadraticTo(w * 0.95f, 30f, w * 0.92f, h - 16f)
                                quadraticTo(w * 0.90f, h, w * 0.50f, h)
                                quadraticTo(w * 0.10f, h, w * 0.08f, h - 16f)
                                quadraticTo(w * 0.05f, 30f, w * 0.32f, 18f)
                                close()
                            }

                            // Glass Ambient Depth
                            drawPath(
                                path = bottlePath,
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color(0x20BAE6FD),
                                        androidx.compose.ui.graphics.Color(0x357DD3FC),
                                        androidx.compose.ui.graphics.Color(0x4538BDF8)
                                    )
                                )
                            )

                            // 3. Animated Liquid Water Waves inside Bottle
                            val maxFillHeight = h - 25f
                            val currentFillHeight = maxFillHeight * progress
                            if (currentFillHeight > 0f) {
                                val baseWaterY = h - currentFillHeight

                                clipPath(bottlePath) {
                                    // A. Back Wave
                                    val wavePathBack = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(0f, h)
                                        lineTo(0f, baseWaterY)
                                        var x = 0f
                                        while (x <= w) {
                                            val waveAmp = (4f * progress).coerceAtMost(6f)
                                            val y = baseWaterY + kotlin.math.sin((x / w * 2.5 * Math.PI + wavePhaseBack).toDouble()).toFloat() * waveAmp
                                            lineTo(x, y)
                                            x += 4f
                                        }
                                        lineTo(w, h)
                                        close()
                                    }
                                    drawPath(
                                        path = wavePathBack,
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                androidx.compose.ui.graphics.Color(0x8838BDF8),
                                                androidx.compose.ui.graphics.Color(0xAA0EA5E9),
                                                androidx.compose.ui.graphics.Color(0xCC0284C7)
                                            ),
                                            startY = baseWaterY - 5f,
                                            endY = h
                                        )
                                    )

                                    // B. Front Wave
                                    val wavePathFront = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(0f, h)
                                        lineTo(0f, baseWaterY)
                                        var x = 0f
                                        while (x <= w) {
                                            val waveAmp = (6f * progress).coerceAtMost(8f)
                                            val y = baseWaterY + kotlin.math.sin((x / w * 2.2 * Math.PI + wavePhase).toDouble()).toFloat() * waveAmp
                                            lineTo(x, y)
                                            x += 4f
                                        }
                                        lineTo(w, h)
                                        close()
                                    }
                                    drawPath(
                                        path = wavePathFront,
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                androidx.compose.ui.graphics.Color(0xDD38BDF8),
                                                androidx.compose.ui.graphics.Color(0xFF0284C7),
                                                androidx.compose.ui.graphics.Color(0xFF0369A1)
                                            ),
                                            startY = baseWaterY - 4f,
                                            endY = h
                                        )
                                    )

                                    // Floating "Coins" (Money Bubbles)
                                    val coin1Y = h - (currentFillHeight * ((bubbleAnim + 0.2f) % 1f))
                                    val coin2Y = h - (currentFillHeight * ((bubbleAnim + 0.7f) % 1f))
                                    
                                    if (coin1Y > baseWaterY) {
                                        drawCircle(
                                            color = androidx.compose.ui.graphics.Color(0xFFFDE047),
                                            radius = 4f,
                                            center = androidx.compose.ui.geometry.Offset(w * 0.4f, coin1Y)
                                        )
                                    }
                                    if (coin2Y > baseWaterY) {
                                        drawCircle(
                                            color = androidx.compose.ui.graphics.Color(0xFFFACC15),
                                            radius = 5f,
                                            center = androidx.compose.ui.geometry.Offset(w * 0.7f, coin2Y)
                                        )
                                    }
                                }
                            }

                            // 4. Glass Surface Gloss & Highlights (Vertical Shine)
                            drawPath(
                                path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(w * 0.15f, 35f)
                                    quadraticTo(w * 0.12f, h / 2, w * 0.18f, h - 25f)
                                },
                                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            
                            // 5. Bottle Outline (Clean Glass Finish)
                            drawPath(
                                path = bottlePath,
                                color = androidx.compose.ui.graphics.Color(0x40FFFFFF),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                            )
                        }
                    }

                        // Center Percentage / Water Icon Badge
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (percentage >= 100) "" else "",
                                fontSize = 16.sp
                            )
                        }

                    // Stats Breakdown & Progress Bar (Right)
                    // Ordering strictly as requested by user:
                    // Bottom: المبلغ المتوقع | Above it: المبلغ المحصل | Above it (Top): المبلغ المتبقي
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // 1. TOP: المبلغ المتبقي (Remaining)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${totalRemaining.toInt()} ج.م",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalRemaining > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                            )
                            Text(
                                text = "المبلغ المتبقي:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // 2. MIDDLE: المبلغ المحصل (Collected)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${totalCollected.toInt()} ج.م",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = " المبلغ المحصل:",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }

                        // 3. BOTTOM: المبلغ المتوقع (Expected)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${totalExpected.toInt()} ج.م",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                            Text(
                                text = " المبلغ المتوقع:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Mini linear progress indicator
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF0284C7),
                            trackColor = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2x2 Stat Card Component (Matching exact styling in Image 2)
 */
@Composable
fun StatRowImageMatch(label: String, value: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(6.dp))
        Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = .12f)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(6.dp).size(16.dp))
        }
    }
}

/**
 * Individual Student Financial Card with Actions
 */
@Composable
fun StudentFinanceCardItem(
    student: StudentEntity,
    group: GroupEntity?,
    fee: Double,
    paymentStatus: String,
    remainingAmount: Double,
    discountAmount: Double = 0.0,
    paymentDate: String?,
    teacherName: String,
    monthName: String,
    year: Int,
    onMarkPaid: () -> Unit,
    onMarkDiscount: () -> Unit,
    onMarkPartial: () -> Unit,
    onMarkExempt: () -> Unit,
    onMarkUnpaid: () -> Unit
) {
    val context = LocalContext.current
    var showActionMenu by remember { mutableStateOf(false) }
    var showReminderChannelDialog by remember { mutableStateOf(false) }

    if (showReminderChannelDialog) {
        val reminderMessage = remember(paymentStatus, student.fullName, fee, paymentDate, remainingAmount, monthName, year) {
            if (paymentStatus == "PAID") {
                "السلام عليكم ورحمة الله وبركاته \nتحية طيبة لولي أمر الطالب المحترم: *${student.fullName}*\nنحيطكم علماً بأنه تم استلام اشتراك شهر $monthName $year بنجاح بقيمة ${fee.toInt()} ج.م.\nتاريخ السداد: ${paymentDate ?: "-"}\nشكراً لحسن تعاونكم وحرصكم الدائم "
            } else if (paymentStatus == "PARTIAL") {
                "السلام عليكم ورحمة الله وبركاته \nتذكير لولي أمر الطالب المحترم: *${student.fullName}*\nنحيطكم علماً بأنه تم سداد جزء من اشتراك شهر $monthName $year والمبلغ المتبقي: ${remainingAmount.toInt()} ج.م.\nشاكرين ومقدرين حسن تعاونكم وحرصكم الدائم "
            } else {
                "السلام عليكم ورحمة الله وبركاته \nتذكير لولي أمر الطالب المحترم: *${student.fullName}*\nنود تذكيركم بموعد سداد اشتراك شهر $monthName $year بقيمة ${fee.toInt()} ج.م.\nشاكرين ومقدرين حسن تعاونكم وحرصكم الدائم "
            }
        }

        ReportChannelSelectionDialog(
            recipientName = "ولي أمر الطالب: ${student.fullName}",
            phoneNumber = student.parentPhone,
            reportMessage = reminderMessage,
            onDismiss = { showReminderChannelDialog = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .68f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Row 1: Student Name + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                if (discountAmount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .32f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(14.dp))
                            Text("خصم: ${discountAmount.toInt()} ج.م (باقي ${remainingAmount.toInt()} ج)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                } else {
                    when (paymentStatus) {
                        "PAID" -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .38f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(14.dp))
                                    Text("مدفوع بالكامل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                            }
                        }
                        "PARTIAL" -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(14.dp))
                                    Text("باقي: ${remainingAmount.toInt()} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                            }
                        }
                        "EXEMPT" -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .32f))
                            ) {
                                Text("معفي من الاشتراك", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .32f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(14.dp))
                                    Text("مستحق: ${fee.toInt()} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }

                // Student Name
                Text(
                    text = student.fullName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle Row: Group Name & Monthly Subscription Fee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الاشتراك الشهري: ${fee.toInt()} ج.م",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "المجموعة: ${group?.name ?: "-"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Payment Date & Expiration Info Banner: "دفع" on Right, "ينتهي" on Left
            val displayPaymentDate = if (!paymentDate.isNullOrBlank()) {
                paymentDate
            } else if (paymentStatus == "PAID" || paymentStatus == "PARTIAL") {
                "01/$monthName/$year"
            } else null

            val expiryDateText = remember(displayPaymentDate, monthName, year) {
                if (displayPaymentDate != null) {
                    try {
                        val parts = displayPaymentDate.split("-", "/")
                        if (parts.size >= 2) {
                            val day = parts.getOrNull(2) ?: parts.getOrNull(0) ?: "01"
                            val m = (parts.getOrNull(1)?.toIntOrNull() ?: 1)
                            val y = (parts.getOrNull(0)?.toIntOrNull() ?: year)
                            val nextM = if (m == 12) 1 else m + 1
                            val nextY = if (m == 12) y + 1 else y
                            String.format(Locale.ENGLISH, "%02d/%02d/%d", day.toIntOrNull() ?: 1, nextM, nextY)
                        } else {
                            "نهاية الشهر"
                        }
                    } catch (_: Exception) {
                        "بعد شهر من الدفع"
                    }
                } else null
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when (paymentStatus) {
                    "PAID" -> MaterialTheme.colorScheme.surfaceVariant
                    "PARTIAL" -> MaterialTheme.colorScheme.tertiaryContainer
                    "EXEMPT" -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.errorContainer
                },
                border = BorderStroke(1.dp, when (paymentStatus) {
                    "PAID" -> Color(0xFFBBF7D0)
                    "PARTIAL" -> Color(0xFFFDE68A)
                    "EXEMPT" -> Color(0xFFE9D5FF)
                    else -> Color(0xFFFECACA)
                }),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (paymentStatus == "PAID") {
                        // Right (First child in RTL layout)
                        Text(
                            text = " دفع: ${displayPaymentDate ?: "-"}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D),
                            maxLines = 1
                        )
                        // Left (Second child in RTL layout)
                        Text(
                            text = "ينتهي: ${expiryDateText ?: "-"}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D),
                            maxLines = 1
                        )
                    } else if (paymentStatus == "PARTIAL") {
                        Text(
                            text = " دفع جزئي: ${displayPaymentDate ?: "-"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            maxLines = 1
                        )
                        Text(
                            text = "ينتهي: ${expiryDateText ?: "-"} (باقي ${remainingAmount.toInt()} ج)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            maxLines = 1
                        )
                    } else if (paymentStatus == "EXEMPT") {
                        Text(
                            text = " طالب معفي رسمياً من المصاريف",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7E22CE),
                            maxLines = 1
                        )
                        Text(
                            text = "منحة دراسية ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7E22CE),
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = " اشتراك شهر $monthName $year",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB91C1C),
                            maxLines = 1
                        )
                        Text(
                            text = " لم يتم السداد",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB91C1C),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .55f))
            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Action Buttons & Reminder Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (paymentStatus != "PAID") {
                    Button(
                        onClick = onMarkPaid,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تسجيل السداد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { showActionMenu = true },
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("خيارات السداد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                com.example.ui.components.PremiumIconAction(
                    icon = Icons.Outlined.NotificationsActive,
                    contentDescription = "تذكير ولي الأمر",
                    onClick = { showReminderChannelDialog = true }
                )
            }

            DropdownMenu(
                expanded = showActionMenu,
                onDismissRequest = { showActionMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("دفع كامل (تم السداد بالكامل)") },
                    leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showActionMenu = false
                        onMarkPaid()
                    }
                )
                DropdownMenuItem(
                    text = { Text("تسجيل خصم للطالب ") },
                    leadingIcon = { Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showActionMenu = false
                        onMarkDiscount()
                    }
                )
                DropdownMenuItem(
                    text = { Text("دفع جزئي / تسجيل مبلغ متبقي") },
                    leadingIcon = { Icon(Icons.Outlined.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showActionMenu = false
                        onMarkPartial()
                    }
                )
                DropdownMenuItem(
                    text = { Text("إعفاء من الاشتراك (منحة / خصم كامل)") },
                    leadingIcon = { Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showActionMenu = false
                        onMarkExempt()
                    }
                )
                DropdownMenuItem(
                    text = { Text("إلغاء السداد (غير مسدد)") },
                    leadingIcon = { Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showActionMenu = false
                        onMarkUnpaid()
                    }
                )
            }
        }
    }
}

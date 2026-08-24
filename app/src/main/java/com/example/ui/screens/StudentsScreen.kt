package com.example.ui.screens
import androidx.compose.ui.text.style.TextOverflow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GroupEntity
import com.example.data.StudentEntity
import com.example.ui.MainViewModel
import com.example.ui.components.StudentDetailDialog
import com.example.ui.components.StudentBarcodeDialog
import com.example.ui.components.StudentCardItem
import com.example.ui.components.PremiumIconTile
import com.example.ui.components.premiumTextFieldColors
import com.example.ui.components.PremiumAlertDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    fun label(ar: String, en: String) = if (appLanguage == "en") en else ar

    val regularGroups = remember(groups) { groups.filter { it.paymentType != "PER_SESSION" } }
    val regularGroupIds = remember(regularGroups) { regularGroups.map { it.id }.toSet() }
    val regularStudents = remember(allStudents, regularGroupIds) { allStudents.filter { regularGroupIds.contains(it.groupId) } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf<Int?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }
    var selectedStudentForDetail by remember { mutableStateOf<StudentEntity?>(null) }
    var selectedStudentForBarcode by remember { mutableStateOf<StudentEntity?>(null) }
    var selectedStudentForExam by remember { mutableStateOf<StudentEntity?>(null) }
    var studentForMessaging by remember { mutableStateOf<StudentEntity?>(null) }

    // Filter students dynamically matching name or phone from first letter
    val trimmedQuery = searchQuery.trim()
    val filteredStudents = regularStudents.filter { student ->
        val matchesQuery = trimmedQuery.isEmpty() ||
                student.fullName.contains(trimmedQuery, ignoreCase = true) ||
                student.fullName.split(" ").any { word -> word.startsWith(trimmedQuery, ignoreCase = true) } ||
                student.parentPhone.contains(trimmedQuery) ||
                student.studentPhone.contains(trimmedQuery)
        val matchesGroup = selectedGroupFilter == null || student.groupId == selectedGroupFilter
        matchesQuery && matchesGroup
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Title + Add Student Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.studentList(appLanguage),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = label("إدارة الطلاب والتواصل السريع", "Manage students and communicate quickly"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            if (regularGroups.isEmpty()) {
                                Toast.makeText(context, "يرجى إضافة مجموعة أولاً قبل إضافة الطلاب", Toast.LENGTH_LONG).show()
                            } else {
                                showAddDialog = true
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label("إضافة طالب", "Add student"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar with explicit Search Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        placeholder = { Text(com.example.ui.theme.AppStrings.searchPlaceholder(appLanguage), fontSize = 12.sp, maxLines = 1) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = label("بحث", "Search"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = "مسح البحث", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val q = searchQuery.trim()
                            if (q.isEmpty()) {
                                Toast.makeText(context, "عرض جميع الطلاب", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "نتائج البحث: ${filteredStudents.size} طالب", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = "بحث", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(com.example.ui.theme.AppStrings.searchBtn(appLanguage), fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }

            // Group Filter Badges (Scrollable horizontally)
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedGroupFilter == null,
                            onClick = { selectedGroupFilter = null },
                            label = { Text("${label("الكل", "All")} (${allStudents.size})", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedGroupFilter == null, borderColor = MaterialTheme.colorScheme.outline, selectedBorderColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    items(groups, key = { it.id }) { grp ->
                        val count = allStudents.count { it.groupId == grp.id }
                        val isSelected = selectedGroupFilter == grp.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGroupFilter = if (isSelected) null else grp.id },
                            label = { Text("${grp.name} ($count)", fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = MaterialTheme.colorScheme.outline, selectedBorderColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Students List
            if (filteredStudents.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.PersonSearch, contentDescription = null, tint = SkyOutline, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(label("لا يوجد طلاب مطابقين للبحث", "No students match your search"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredStudents, key = { it.id }) { student ->
                    val group = groups.find { it.id == student.groupId }
                    StudentCardItem(
                        student = student,
                        group = group,
                        onClick = { selectedStudentForDetail = student },
                        onBarcodeClick = { selectedStudentForBarcode = student },
                        onSmartCardClick = { 
                            // Navigate to smart cards or show dialog? 
                            // The user wants it accessible.
                            selectedStudentForDetail = student 
                        },
                        onSendMessage = { studentForMessaging = student },
                        onCallParent = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${student.parentPhone}"))
                            try { context.startActivity(intent) } catch (e: Exception) { }
                        },
                        onEdit = { editingStudent = student },
                        onDelete = {
                            studentToDelete = student
                        }
                    )
                }
            }
        }
    }

    // Delete Student Confirmation Dialog
    studentToDelete?.let { student ->
        PremiumAlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                    Text(
                        text = "تأكيد حذف الطالب ",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B),
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف الطالب '${student.fullName}'؟\nسيتم حذف جميع سجلات الحضور والامتحانات والماليات الخاصة به نهائياً.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = student
                        studentToDelete = null
                        viewModel.deleteStudent(s)
                        Toast.makeText(context, "تم حذف الطالب بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("موافق (حذف)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { studentToDelete = null }) {
                    Text("إلغاء (تراجع)", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Add Student Dialog
    if (showAddDialog) {
        StudentFormDialog(
            student = null,
            groups = groups,
            initialGroupId = selectedGroupFilter,
            onDismiss = { showAddDialog = false },
            onSave = { name, groupId, parentPhone, studentPhone ->
                viewModel.addStudent(name, groupId, parentPhone, studentPhone)
                showAddDialog = false
                Toast.makeText(context, "تمت إضافة الطالب بنجاح!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit Student Dialog
    editingStudent?.let { studentToEdit ->
        StudentFormDialog(
            student = studentToEdit,
            groups = groups,
            onDismiss = { editingStudent = null },
            onSave = { name, groupId, parentPhone, studentPhone ->
                val updated = studentToEdit.copy(
                    fullName = name,
                    groupId = groupId,
                    parentPhone = parentPhone,
                    studentPhone = studentPhone
                )
                viewModel.updateStudent(updated)
                editingStudent = null
                Toast.makeText(context, "تم تحديث بيانات الطالب بنجاح!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Student Detail Popup Modal
    selectedStudentForDetail?.let { student ->
        val grp = groups.find { it.id == student.groupId }
        StudentDetailDialog(
            student = student,
            group = grp,
            viewModel = viewModel,
            onDismiss = { selectedStudentForDetail = null }
        )
    }

    selectedStudentForBarcode?.let { student ->
        StudentBarcodeDialog(
            student = student,
            onDismiss = { selectedStudentForBarcode = null }
        )
    }

    selectedStudentForExam?.let { student ->
        val grp = groups.find { it.id == student.groupId }
        com.example.ui.components.StudentExamGradeDialog(
            student = student,
            group = grp,
            viewModel = viewModel,
            onDismiss = { selectedStudentForExam = null }
        )
    }

    studentForMessaging?.let { student ->
        val phone = student.parentPhone.ifBlank { student.studentPhone }
        val defaultMsg = "السلام عليكم ورحمة الله وبركاته \nتحية طيبة لولي أمر الطالب/ة المحترم: *${student.fullName}*\nنحيطكم علماً بمتابعة الطالب من خلال منصة أستاذي + التعليمية."
        com.example.ui.components.ReportChannelSelectionDialog(
            recipientName = student.fullName,
            phoneNumber = phone,
            reportMessage = defaultMsg,
            onDismiss = { studentForMessaging = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFormDialog(
    student: StudentEntity?,
    groups: List<GroupEntity>,
    initialGroupId: Int? = null,
    onDismiss: () -> Unit,
    onSave: (fullName: String, groupId: Int, parentPhone: String, studentPhone: String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(student?.fullName ?: "") }
    var selectedGroupId by remember { mutableStateOf(student?.groupId ?: initialGroupId ?: groups.firstOrNull()?.id ?: 0) }
    var parentPhone by remember { mutableStateOf(student?.parentPhone ?: "") }
    var studentPhone by remember { mutableStateOf(student?.studentPhone ?: "") }

    var groupExpanded by remember { mutableStateOf(false) }
    val selectedGroup = groups.find { it.id == selectedGroupId }

    var pendingContactTarget by remember { mutableStateOf<String?>(null) } // "PARENT" or "STUDENT"

    // Pick contact launchers using ACTION_PICK for Phone content URI
    val pickParentContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { contactUri ->
                val num = getPhoneNumberFromUri(context, contactUri)
                if (!num.isNullOrEmpty()) {
                    parentPhone = num
                    Toast.makeText(context, "تم اختيار رقم ولي الأمر: $num", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "تعذر قراءة رقم الهاتف من جهة الاتصال", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val pickStudentContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { contactUri ->
                val num = getPhoneNumberFromUri(context, contactUri)
                if (!num.isNullOrEmpty()) {
                    studentPhone = num
                    Toast.makeText(context, "تم اختيار رقم الطالب: $num", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "تعذر قراءة رقم الهاتف من جهة الاتصال", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            if (pendingContactTarget == "PARENT") {
                pickParentContactLauncher.launch(intent)
            } else if (pendingContactTarget == "STUDENT") {
                pickStudentContactLauncher.launch(intent)
            }
        } else {
            Toast.makeText(context, "تم رفض إذن الوصول لجهات الاتصال", Toast.LENGTH_SHORT).show()
        }
        pendingContactTarget = null
    }

    fun pickContactWithPermission(target: String) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            if (target == "PARENT") {
                pickParentContactLauncher.launch(intent)
            } else {
                pickStudentContactLauncher.launch(intent)
            }
        } else {
            pendingContactTarget = target
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    PremiumAlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PremiumIconTile(
                    icon = if (student == null) Icons.Outlined.PersonAdd else Icons.Outlined.Edit,
                    contentDescription = null
                )
                Column {
                    Text(
                        text = if (student == null) "إضافة طالب جديد " else "تعديل بيانات الطالب ",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "أدخل بيانات الطالب ورقم ولي الأمر بدقة للتواصل السريع",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الطالب ثلاثي") },
                    placeholder = { Text("مثال: أحمد محمد علي") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = premiumTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Select Group Dropdown
                ExposedDropdownMenuBox(
                    expanded = groupExpanded,
                    onExpandedChange = { groupExpanded = !groupExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedGroup?.name ?: "اختر المجموعة",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المجموعة الدراسية") },
                        leadingIcon = { Icon(Icons.Outlined.Groups, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = premiumTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = groupExpanded,
                        onDismissRequest = { groupExpanded = false }
                    ) {
                        groups.forEach { grp ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Class, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(grp.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                },
                                onClick = {
                                    selectedGroupId = grp.id
                                    groupExpanded = false
                                }
                            )
                        }
                    }
                }

                // Parent Phone Input + Contact Picker
                OutlinedTextField(
                    value = parentPhone,
                    onValueChange = { parentPhone = it },
                    label = { Text("رقم هاتف ولي الأمر (واتساب)") },
                    placeholder = { Text("01012345678") },
                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            pickContactWithPermission("PARENT")
                        }) {
                            Icon(Icons.Outlined.Contacts, contentDescription = "استيراد من الهاتف")
                        }
                    },
                    colors = premiumTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Student Phone Input + Contact Picker
                OutlinedTextField(
                    value = studentPhone,
                    onValueChange = { studentPhone = it },
                    label = { Text("رقم هاتف الطالب (اختياري)") },
                    placeholder = { Text("01112345678") },
                    leadingIcon = { Icon(Icons.Outlined.Smartphone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            pickContactWithPermission("STUDENT")
                        }) {
                            Icon(Icons.Outlined.Contacts, contentDescription = "استيراد من الهاتف")
                        }
                    },
                    colors = premiumTextFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || selectedGroupId == 0 || parentPhone.isBlank()) return@Button
                    onSave(name, selectedGroupId, parentPhone, studentPhone)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (student == null) Icons.Outlined.Add else Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (student == null) "حفظ الطالب " else "حفظ التعديلات",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

fun getPhoneNumberFromUri(context: android.content.Context, uri: Uri): String? {
    var rawPhone: String? = null

    try {
        // Attempt 1: Query with explicit Phone.NUMBER projection
        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (idx >= 0) {
                    rawPhone = cursor.getString(idx)
                }
            }
        }

        // Attempt 2: Query with "data1" projection
        if (rawPhone.isNullOrBlank()) {
            context.contentResolver.query(
                uri,
                arrayOf("data1"),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex("data1")
                    if (idx >= 0) {
                        rawPhone = cursor.getString(idx)
                    }
                }
            }
        }

        // Attempt 3: Query with null projection and inspect all columns
        if (rawPhone.isNullOrBlank()) {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (phoneIndex >= 0) {
                        rawPhone = cursor.getString(phoneIndex)
                    }
                    
                    if (rawPhone.isNullOrBlank()) {
                        for (i in 0 until cursor.columnCount) {
                            val valStr = cursor.getString(i)
                            if (!valStr.isNullOrBlank() && valStr.any { it.isDigit() }) {
                                val digitCount = valStr.count { it.isDigit() }
                                if (digitCount >= 6) {
                                    rawPhone = valStr
                                    break
                                }
                            }
                        }
                    }

                    if (rawPhone.isNullOrBlank()) {
                        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        if (idIndex >= 0) {
                            val contactId = cursor.getString(idIndex)
                            if (!contactId.isNullOrEmpty()) {
                                rawPhone = getPhoneByContactId(context, contactId)
                            }
                        }
                    }
                }
            }
        }

        // Attempt 4: Fallback by parsing ContentUris ID
        if (rawPhone.isNullOrBlank()) {
            try {
                val id = android.content.ContentUris.parseId(uri)
                if (id > 0) {
                    rawPhone = getPhoneByContactId(context, id.toString())
                }
            } catch (_: Exception) {}
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (rawPhone.isNullOrBlank()) return null

    var cleaned = rawPhone!!.replace(" ", "").replace("-", "").replace("(", "").replace(")", "").trim()
    if (cleaned.startsWith("+20")) {
        cleaned = "0" + cleaned.substring(3)
    } else if (cleaned.startsWith("+2")) {
        cleaned = "0" + cleaned.substring(2)
    }
    return cleaned
}

private fun getPhoneByContactId(context: android.content.Context, contactId: String): String? {
    var num: String? = null
    try {
        // Try query by CONTACT_ID
        val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId)
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, "data1"),
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (idx >= 0) {
                    num = cursor.getString(idx)
                }
                if (num.isNullOrBlank()) {
                    val idx2 = cursor.getColumnIndex("data1")
                    if (idx2 >= 0) {
                        num = cursor.getString(idx2)
                    }
                }
            }
        }

        // If still empty, try query by _ID
        if (num.isNullOrBlank()) {
            val selection2 = "${ContactsContract.CommonDataKinds.Phone._ID} = ?"
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, "data1"),
                selection2,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (idx >= 0) {
                        num = cursor.getString(idx)
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return num
}

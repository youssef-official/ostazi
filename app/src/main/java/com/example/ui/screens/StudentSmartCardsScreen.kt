package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GroupEntity
import com.example.data.StudentEntity
import com.example.ui.MainViewModel
import com.example.ui.components.StudentCardFront
import com.example.ui.components.StudentCardBack
import com.example.ui.components.StudentCardPdfHelper
import com.example.ui.theme.*
import com.example.utils.BarcodeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSmartCardsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState(initial = emptyList())
    val groups by viewModel.groups.collectAsState(initial = emptyList())
    val teacherProfile by viewModel.teacherProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf<Int?>(null) }
    
    // Group students by groupId and filter
    val filteredStudents = remember(students, groups, searchQuery, selectedGroupId) {
        students.filter { student ->
            (selectedGroupId == null || student.groupId == selectedGroupId) &&
            (searchQuery.isBlank() || student.fullName.contains(searchQuery, ignoreCase = true))
        }
    }

    val groupedStudents = remember(filteredStudents) {
        filteredStudents.groupBy { it.groupId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("بطاقات الطلاب الذكية", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("كروت لجميع الطلاب مقسمة حسب المجموعات", fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        StudentCardPdfHelper.generateBatchStudentCardsPdf(
                            context = context,
                            students = students,
                            groups = groups,
                            teacherName = teacherProfile.name,
                            teacherSubject = teacherProfile.subject,
                            groupNameFilter = "جميع الطلاب"
                        )
                    }) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = "تحميل كل الكروت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = SkyOnSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SkyBackground)
        ) {
            // PDF Export Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val selectedGroupName = groups.find { it.id == selectedGroupId }?.name ?: "الكل"
                        StudentCardPdfHelper.generateBatchStudentCardsPdf(
                            context = context,
                            students = filteredStudents,
                            groups = groups,
                            teacherName = teacherProfile.name,
                            teacherSubject = teacherProfile.subject,
                            groupNameFilter = "مجموعة $selectedGroupName"
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF المجموعة المختارة", fontSize = 10.sp)
                }

                Button(
                    onClick = {
                        StudentCardPdfHelper.generateBatchStudentCardsPdf(
                            context = context,
                            students = students,
                            groups = groups,
                            teacherName = teacherProfile.name,
                            teacherSubject = teacherProfile.subject,
                            groupNameFilter = "كافة الطلاب"
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF كافة الطلاب", fontSize = 10.sp)
                }
            }

            // Group Filter Selection
            ScrollableTabRow(
                selectedTabIndex = if (selectedGroupId == null) 0 else (groups.indexOfFirst { it.id == selectedGroupId } + 1).coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                val allGroupsWithAll = listOf(null) + groups
                allGroupsWithAll.forEach { group ->
                    val isSelected = selectedGroupId == group?.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedGroupId = group?.id },
                        label = { Text(group?.name ?: "الكل", fontSize = 12.sp) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SkyPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("بحث عن طالب لفلترة البطاقات...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            if (filteredStudents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا يوجد طلاب مطابقين للبحث", color = SkyOnSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Sort groups by name or ID
                    val sortedGroupIds = groupedStudents.keys.sortedBy { groupId ->
                        groups.find { it.id == groupId }?.name ?: "بدون مجموعة"
                    }

                    sortedGroupIds.forEach { groupId ->
                        val group = groups.find { it.id == groupId }
                        val studentsInGroup = groupedStudents[groupId] ?: emptyList()

                        item {
                            GroupHeader(groupName = group?.name ?: "طلاب بدون مجموعة")
                        }

                        items(studentsInGroup) { student ->
                            StudentCardItem(
                                student = student,
                                group = group,
                                teacherName = teacherProfile.name,
                                teacherSubject = teacherProfile.subject
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupHeader(groupName: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = SkyPrimary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SkyPrimary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Group, contentDescription = null, tint = SkyPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "مجموعة: $groupName",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = SkyPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                color = SkyPrimary,
                shape = CircleShape
            ) {
                // Number of students in this group item? 
                // Actually let's just keep it simple
            }
        }
    }
}

@Composable
fun StudentCardItem(
    student: StudentEntity,
    group: GroupEntity?,
    teacherName: String,
    teacherSubject: String
) {
    var showBack by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toggle Switch for this specific card
        Row(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showBack = !showBack }
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showBack) "تبديل للوجه " else "تبديل للظهر ",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SkyPrimary
            )
        }

        if (!showBack) {
            val qrBitmap = remember(student.id) {
                BarcodeUtils.generateQrCodeBitmap("STUDENT_${student.id}", 260, 260)
            }
            StudentCardFront(
                student = student,
                group = group,
                qrBitmap = qrBitmap
            )
        } else {
            StudentCardBack(
                teacherName = teacherName,
                teacherSubject = teacherSubject,
                group = group
            )
        }
    }
}

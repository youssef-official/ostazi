package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GroupEntity
import com.example.data.StudentEntity
import com.example.ui.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EvaluationPill(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    activeBgColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeBgColor else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) activeColor else Color(0xFFCBD5E1)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(activeColor)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) activeColor else Color(0xFF64748B)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAttendanceModal(
    group: GroupEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val groupStudents = allStudents.filter { it.groupId == group.id }
    val todayDate = viewModel.todayDateString
    
    // We observe all sessions to find today's session details for each student
    val attendanceList by viewModel.attendanceForToday.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تسجيل الحضور: ${group.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "إغلاق")
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groupStudents, key = { it.id }) { student ->
                    val studentSession = attendanceList.find { it.studentId == student.id }
                    // If no record, we show nothing as selected by default so user can choose manually
                    val attendanceStatus = studentSession?.attendanceStatus ?: ""
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = student.fullName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                modifier = Modifier.weight(1f)
                            )
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                EvaluationPill("حاضر", attendanceStatus == "حاضر" || attendanceStatus == "حضر", Color(0xFF16A34A), Color(0xFFDCFCE7)) {
                                    viewModel.setAttendanceAndHomework(student.id, todayDate, "حاضر", studentSession?.homeworkStatus ?: "كتب")
                                }
                                EvaluationPill("غائب", attendanceStatus == "غائب", Color(0xFFDC2626), MaterialTheme.colorScheme.errorContainer) {
                                    viewModel.setAttendanceAndHomework(student.id, todayDate, "غائب", studentSession?.homeworkStatus ?: "لم يكتب")
                                }
                                EvaluationPill("متأخر", attendanceStatus == "متأخر", Color(0xFFD97706), MaterialTheme.colorScheme.tertiaryContainer) {
                                    viewModel.setAttendanceAndHomework(student.id, todayDate, "متأخر", studentSession?.homeworkStatus ?: "كتب")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

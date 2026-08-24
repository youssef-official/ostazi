cat << 'INNER' > app/src/main/java/com/example/ui/components/QuickAttendanceModal.kt
package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAttendanceModal(
    group: GroupEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val groupStudents = allStudents.filter { it.groupId == group.id }
    val todayDate = viewModel.getTodayDateString()
    
    // We observe all sessions to find today's session details for each student
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
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
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
            Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groupStudents, key = { it.id }) { student ->
                    val studentSession = allSessions.find { it.studentId == student.id && it.date == todayDate }
                    val attendanceStatus = studentSession?.attendanceStatus ?: "حاضر"
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
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
                                EvaluationPill("حاضر", attendanceStatus.contains("حاضر") || attendanceStatus.contains("حضر"), Color(0xFF16A34A), Color(0xFFDCFCE7)) {
                                    viewModel.setAttendanceAndHomework(student.id, todayDate, "حاضر", studentSession?.homeworkStatus ?: "كتب")
                                }
                                EvaluationPill("غائب", attendanceStatus.contains("غائب"), Color(0xFFDC2626), Color(0xFFFEE2E2)) {
                                    viewModel.setAttendanceAndHomework(student.id, todayDate, "غائب", studentSession?.homeworkStatus ?: "لم يكتب")
                                }
                                EvaluationPill("متأخر", attendanceStatus.contains("متأخر"), Color(0xFFD97706), Color(0xFFFEF3C7)) {
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
INNER

sed -i '/if (showBarcodeScannerModal) {/i \
    groupForQuickAttendance?.let { group ->\
        com.example.ui.components.QuickAttendanceModal(\
            group = group,\
            viewModel = viewModel,\
            onDismiss = { groupForQuickAttendance = null }\
        )\
    }\
' app/src/main/java/com/example/ui/screens/DashboardScreen.kt

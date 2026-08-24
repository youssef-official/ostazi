package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GroupEntity
import com.example.data.StudentEntity
import com.example.ui.theme.*

@Composable
fun StudentCardItem(
    student: StudentEntity,
    group: GroupEntity?,
    onClick: () -> Unit,
    onBarcodeClick: () -> Unit,
    onSmartCardClick: () -> Unit,
    onSendMessage: () -> Unit,
    onCallParent: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isOnline = group?.groupType == "ONLINE"
    val isPrivate = group?.groupType == "PRIVATE"
    val groupType = if (isOnline) "أونلاين" else if (isPrivate) "خاص" else "سنتر"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .68f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PremiumIconTile(
                    icon = Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(46.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = student.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = group?.name ?: "غير محددة",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(shape = RoundedCornerShape(7.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                text = groupType,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .55f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PremiumIconAction(Icons.Outlined.Call, "اتصال", onCallParent)
                PremiumIconAction(Icons.Outlined.QrCode2, "رمز الطالب", onBarcodeClick)
                Spacer(Modifier.weight(1f))
                PremiumIconAction(Icons.Outlined.Edit, "تعديل", onEdit)
                PremiumIconAction(Icons.Outlined.DeleteOutline, "حذف", onDelete, destructive = true)
            }
        }
    }
}

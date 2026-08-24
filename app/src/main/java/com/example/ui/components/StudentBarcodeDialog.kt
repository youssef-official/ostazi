package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.StudentEntity
import com.example.utils.BarcodeUtils
import com.example.ui.theme.*

@Composable
fun StudentBarcodeDialog(
    student: StudentEntity,
    onDismiss: () -> Unit
) {
    val bitmap = remember(student.id) {
        BarcodeUtils.generateQRCode("STUDENT_${student.id}", 600)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(onDismissRequest = onDismiss) {
            PremiumDialogDirectionGuard()
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "كود الطالب",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyOnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = student.fullName,
                        fontSize = 16.sp,
                        color = SkyOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Barcode",
                            modifier = Modifier.size(250.dp)
                        )
                    } else {
                        Text(
                            text = "تعذر توليد الباركود",
                            color = Color.Red
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary)
                    ) {
                        Text("إغلاق", color = SkyOnPrimary)
                    }
                }
            }
        }
    }
}

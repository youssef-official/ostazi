package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ActivationCodeGenerator
import com.example.SubscriptionManager
import com.example.ui.theme.*

@Composable
fun ActivationCodeGeneratorDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val subscriptionManager = remember { SubscriptionManager(context) }
    
    var targetPackage by remember { mutableStateOf("TERM") } // "TERM" (100 EGP) or "YEAR" (200 EGP)
    var deviceIdInput by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf("") }
    var generatedList by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    // Auto-fill current device ID as helper option
    val currentDeviceId = remember { subscriptionManager.getDeviceId() }

    PremiumAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "مولّد أكواد التفعيل ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SkyOnSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "قم بتحديد نوع الباقة ومعرف جهاز المعلم (اختياري) لتوليد كود التفعيل الفوري.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyOnSurfaceVariant
                )

                // Package Selector
                Text(
                    text = "اختر الباقة المراد تفعيلها:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SkyOnSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Gold / Term Package
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (targetPackage == "TERM") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            width = if (targetPackage == "TERM") 2.dp else 1.dp,
                            color = if (targetPackage == "TERM") Color(0xFFD97706) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp),
                        onClick = { targetPackage = "TERM" }
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "الذهبية (ترم)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "100 ج.م / 6 شهور",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Diamond / Year Package
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (targetPackage == "YEAR") Color(0xFFCFFAFE) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            width = if (targetPackage == "YEAR") 2.dp else 1.dp,
                            color = if (targetPackage == "YEAR") Color(0xFF0891B2) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp),
                        onClick = { targetPackage = "YEAR" }
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "الماسية (سنة)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0E7490)
                            )
                            Text(
                                text = "200 ج.م / عام كامل",
                                fontSize = 11.sp,
                                color = Color(0xFF0891B2),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Optional Device ID
                OutlinedTextField(
                    value = deviceIdInput,
                    onValueChange = { deviceIdInput = it },
                    label = { Text("معرف الجهاز للعميل (اختياري)") },
                    placeholder = { Text("مثال: DEV-7821-4412") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyPrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { deviceIdInput = currentDeviceId },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Outlined.Devices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("استخدام معرف هذا الجهاز ($currentDeviceId)", fontSize = 11.sp)
                    }
                }

                // Generate Button
                Button(
                    onClick = {
                        val newCode = ActivationCodeGenerator.generateCode(
                            packageType = targetPackage,
                            deviceId = deviceIdInput.takeIf { it.isNotBlank() }
                        )
                        generatedCode = newCode
                        generatedList = listOf(Pair(newCode, if (targetPackage == "YEAR") "عام كامل (200 ج.م)" else "ترم كامل (100 ج.م)")) + generatedList
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Icon(Icons.Outlined.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("توليد كود التفعيل الآن ", fontWeight = FontWeight.Bold)
                }

                // Display Generated Code
                if (generatedCode.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(2.dp, if (targetPackage == "YEAR") Color(0xFF06B6D4) else Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "الكود المُولّد بنجاح:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SkyOnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = generatedCode,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (targetPackage == "YEAR") Color(0xFF0891B2) else Color(0xFFD97706),
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Copy Button
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Activation Code", generatedCode)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "تم نسخ الكود للحافظة!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("نسخ الكود", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Send via WhatsApp
                                Button(
                                    onClick = {
                                        val pkgName = if (targetPackage == "YEAR") "باقة VIP الماسية (عام كامل - 200 ج.م)" else "باقة VIP الذهبية (ترم كامل - 100 ج.م)"
                                        val msg = "كود تفعيل تطبيقك:\n $pkgName\n الكود: $generatedCode\n\nقم بفتح التطبيق > الضغط على رمز VIP > إدخال الكود والضغط على تفعيل."
                                        val uri = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(msg)}")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "تعذر فتح الواتساب", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إرسال واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontWeight = FontWeight.Bold, color = SkyPrimary)
            }
        }
    )
}

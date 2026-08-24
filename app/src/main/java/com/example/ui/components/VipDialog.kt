package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SubscriptionManager
import com.example.ui.theme.*

@Composable
fun VipDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val subscriptionManager = remember { SubscriptionManager(context) }
    val isActivated = subscriptionManager.isActivated()
    val activePkgType = subscriptionManager.getPackageType()
    val deviceId = remember { subscriptionManager.getDeviceId() }

    var activationCode by remember { mutableStateOf("") }
    var showCodeGeneratorDialog by remember { mutableStateOf(false) }

    PremiumAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "باقات الاشتراك VIP",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = SkyOnSurface
                    )
                }

                // Quick Admin Code Generator Icon
                IconButton(
                    onClick = { showCodeGeneratorDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Key,
                        contentDescription = "مولد الأكواد",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Subscription Status Notice if Activated
                if (isActivated) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = subscriptionManager.getSubscriptionTitle(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = subscriptionManager.getExpiryInfo(),
                                    fontSize = 11.sp,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                }

                // 1. Golden Package Card (الذهبية - 100 EGP)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    border = BorderStroke(1.5.dp, Color(0xFFFDE68A)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الذهبية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Outlined.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Price Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "150",
                                fontSize = 15.sp,
                                color = Color(0xFF9CA3AF),
                                textDecoration = TextDecoration.LineThrough,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "100",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "جنيه / ترم كامل (6 شهور)",
                                fontSize = 13.sp,
                                color = Color(0xFF78350F),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Features List
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            VipFeatureItem(text = "6 شهور كاملة", iconColor = Color(0xFF059669))
                            VipFeatureItem(text = "كل المميزات مفتوحة", iconColor = Color(0xFF059669))
                            VipFeatureItem(text = "دعم فني", iconColor = Color(0xFF059669))
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Buy Button Pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable {
                                    val msg = "طلب شراء باقة VIP الذهبية (ترم كامل - 100 ج.م)\nمعرف الجهاز: $deviceId"
                                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=201066860729&text=${Uri.encode(msg)}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "تعذر فتح الواتساب", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "شراء الباقة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF4B5563)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF4B5563),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Diamond Package Card (الماسية - 200 EGP)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFFECFEFF),
                    border = BorderStroke(1.5.dp, Color(0xFF67E8F9)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title Row with Active Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Current Plan / Popular Badge
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isActivated && activePkgType == "YEAR") Color(0xFF059669) else Color(0xFF0891B2),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isActivated && activePkgType == "YEAR") "خطتك الحالية" else "الأكثر توفيراً",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "الماسية",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF0E7490)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Outlined.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color(0xFF0891B2),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Price Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "250",
                                fontSize = 15.sp,
                                color = Color(0xFF9CA3AF),
                                textDecoration = TextDecoration.LineThrough,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "200",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0891B2)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "جنيه / عام دراسي كامل",
                                fontSize = 13.sp,
                                color = Color(0xFF155E75),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Features List
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            VipFeatureItem(text = "12 شهور كاملة", iconColor = Color(0xFF059669))
                            VipFeatureItem(text = "كل المميزات مفتوحة", iconColor = Color(0xFF059669))
                            VipFeatureItem(text = "دعم فني", iconColor = Color(0xFF059669))
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Buy Button Pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable {
                                    val msg = "طلب شراء باقة VIP الماسية (عام كامل - 200 ج.م)\nمعرف الجهاز: $deviceId"
                                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=201066860729&text=${Uri.encode(msg)}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "تعذر فتح الواتساب", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "شراء الباقة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF4B5563)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF4B5563),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Activation with Device Code Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تفعيل الاشتراك بكود الجهاز",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Outlined.Bolt,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "لديك كود تفعيل مخصص لجهازك؟",
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )

                        // Input and Activate Button Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (activationCode.isBlank()) {
                                        Toast.makeText(context, "يرجى كتابة كود التفعيل أولاً", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (subscriptionManager.activatePackage(activationCode)) {
                                        Toast.makeText(context, "تم تفعيل ${subscriptionManager.getSubscriptionTitle()} بنجاح! ", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "كود التفعيل غير صالح، تأكد من الكود وحاول مجدداً", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("تفعيل", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            }

                            OutlinedTextField(
                                value = activationCode,
                                onValueChange = { activationCode = it },
                                placeholder = { Text("أدخل الكود هنا", color = Color(0xFF94A3B8), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0F172A),
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }

                        // Device ID Copy Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Device ID", deviceId)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ معرف الجهاز!", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = SkyPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SkyPrimary)
                            }

                            Text(
                                text = "معرف جهازك: $deviceId",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }

                        // Admin Generator Link
                        TextButton(
                            onClick = { showCodeGeneratorDialog = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Outlined.Key, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD97706))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فتح مولّد أكواد التفعيل ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
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

    if (showCodeGeneratorDialog) {
        ActivationCodeGeneratorDialog(
            onDismiss = { showCodeGeneratorDialog = false }
        )
    }
}

@Composable
private fun VipFeatureItem(
    text: String,
    iconColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF4B5563),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

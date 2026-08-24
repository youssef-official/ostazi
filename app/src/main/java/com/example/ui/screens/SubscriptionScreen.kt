package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import com.example.SubscriptionManager

@Composable
fun SubscriptionScreen(onActivated: () -> Unit) {
    val context = LocalContext.current
    val subscriptionManager = remember { SubscriptionManager(context) }
    var activationCode by remember { mutableStateOf("") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SkyBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = SkyPrimary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "انتهت الفترة المجانية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = SkyOnSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "لقد انتهت فترة التجربة المجانية (شهر). يرجى الاشتراك لمتابعة استخدام التطبيق بجميع ميزاته.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyOnSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SkySurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyPrimaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "باقات الاشتراك",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = SkyPrimary
                        )
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = SkyPrimaryContainer)
                        
                        Text(
                            text = "باقة الترم كامل (الذهبية) بـ 100 ج.م (6 شهور)", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 15.sp,
                            color = Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "باقة العام كامل (الماسية) بـ 200 ج.م (12 شهر)", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 15.sp,
                            color = Color(0xFF0891B2)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LimeContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LimePrimary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "للاشتراك، يرجى التحويل على الرقم:",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = SkyOnSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "01066860729",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = LimePrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "عبر فودافون كاش أو إنستا باي",
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = SkyOnSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val message = "طلب تفعيل باقة اشتراك تطبيق مساعد المعلم"
                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=201066860729&text=${Uri.encode(message)}")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال طلب التفعيل وإيصال التحويل عبر واتساب")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = SkyPrimaryContainer)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = activationCode,
                    onValueChange = { activationCode = it },
                    label = { Text("كود التفعيل") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (subscriptionManager.activatePackage(activationCode)) {
                            Toast.makeText(context, "تم تفعيل البرنامج بنجاح! ", Toast.LENGTH_SHORT).show()
                            onActivated()
                        } else {
                            Toast.makeText(context, "كود التفعيل غير صحيح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("تفعيل البرنامج", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

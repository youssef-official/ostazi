package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.SecurityManager

@Composable
fun SecuritySettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var selectedOption by remember { mutableStateOf(SecurityManager.getSecurityOption(context)) }
    
    // Only show Biometric, PIN, and None
    val allowedOptions = listOf(
        SecurityManager.SecurityOption.NONE,
        SecurityManager.SecurityOption.BIOMETRIC, 
        SecurityManager.SecurityOption.PIN
    )
    
    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "رجوع")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("إعدادات الأمان والحماية", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        }

        Text("اختر طريقة حماية الموارد المالية والبيانات الحساسة:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        allowedOptions.forEach { option ->
            val label = when(option) {
                SecurityManager.SecurityOption.NONE -> "بدون حماية (مفتوح)"
                SecurityManager.SecurityOption.BIOMETRIC -> "بصمة الإصبع (أمان بيومتري)"
                SecurityManager.SecurityOption.PIN -> "رقم سري (PIN Code)"
                else -> option.name
            }
            
            val isSelected = selectedOption == option
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        selectedOption = option
                        SecurityManager.setSecurityOption(context, option)
                    }
                    .padding(vertical = 12.dp)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    label, 
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
        }
        
        if (selectedOption == SecurityManager.SecurityOption.PIN) {
            var pin by remember { mutableStateOf(SecurityManager.getPin(context) ?: "") }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("تعيين الرقم السري للقفل", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 6) pin = it },
                        label = { Text("أدخل رقم سري (4-6 أرقام)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    
                    Button(
                        onClick = { 
                            if (pin.length >= 4) {
                                SecurityManager.setPin(context, pin)
                                Toast.makeText(context, "تم حفظ الرقم السري بنجاح ", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "يجب أن يكون الرقم مكوناً من 4 أرقام على الأقل", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ وتأكيد الرمز ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

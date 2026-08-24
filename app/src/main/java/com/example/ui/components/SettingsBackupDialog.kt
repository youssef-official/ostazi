package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
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
import com.example.notification.NotificationHelper
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val authState by com.example.auth.AuthManager.authState.collectAsState()
    var currentSoundUriStr by remember { mutableStateOf(NotificationHelper.getSelectedSoundUriString(context)) }
    var isNotificationsEnabled by remember { mutableStateOf(NotificationHelper.isNotificationEnabled(context)) }
    var isSoundMuted by remember { mutableStateOf(NotificationHelper.isSoundMuted(context)) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                currentSoundUriStr = uri.toString()
                NotificationHelper.setSelectedSoundUriString(context, uri.toString())
                Toast.makeText(context, "تم اختيار النغمة بنجاح", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Document Creator Launcher (Saves directly to chosen folder in File Manager)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { targetUri ->
            viewModel.exportBackupToUri(context, targetUri) { success, message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // File Picker Launcher for Import Data
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            viewModel.importBackupDataFromUri(context, selectedUri) { success, message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Excel / CSV File Picker Launcher for Students Import
    val importExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            viewModel.importStudentsFromExcel(context, selectedUri) { success, message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    PremiumAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SkyPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = SkyPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "الإعدادات والنسخ الاحتياطي",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = SkyOnSurface
                    )
                    Text(
                        text = "تخصيص التنبيهات، النسخ الاحتياطي ومطور التطبيق",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyOnSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Ringtone & Alarm Sound Settings (نغمة التنبيه عالية الصوت)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SkySurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyPrimaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isNotificationsEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (isNotificationsEnabled) SkyPrimary else Color.Gray,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "تنبيهات الحصص (قبل 10 دقائق)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SkyOnSurface
                                    )
                                    Text(
                                        text = if (isNotificationsEnabled) "التنبيهات مفعلة" else "التنبيهات معطلة",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isNotificationsEnabled) SkyPrimary else Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Switch(
                                checked = isNotificationsEnabled,
                                onCheckedChange = { checked ->
                                    isNotificationsEnabled = checked
                                    NotificationHelper.setNotificationEnabled(context, checked)
                                    if (checked) {
                                        Toast.makeText(context, "تم تشغيل تنبيهات الحصص ", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "تم إيقاف تنبيهات الحصص ", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        HorizontalDivider(color = SkyOutline.copy(alpha = 0.5f))

                        // Mute Sound Switch Option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isSoundMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                                    contentDescription = null,
                                    tint = if (isSoundMuted) Color(0xFFDC2626) else Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "إغلاق نغمة الإشعارات تماماً (صامت )",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SkyOnSurface
                                    )
                                    Text(
                                        text = if (isSoundMuted) "النغمة مكتومة والصوت معطل تماماً" else "الصوت يعمل عند وصول التنبيهات",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSoundMuted) Color(0xFFDC2626) else Color(0xFF059669),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Switch(
                                checked = isSoundMuted,
                                enabled = isNotificationsEnabled,
                                onCheckedChange = { muted ->
                                    isSoundMuted = muted
                                    NotificationHelper.setSoundMuted(context, muted)
                                    if (muted) {
                                        Toast.makeText(context, "تم إغلاق نغمة الإشعارات تماماً ", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "تم تفعيل نغمة الإشعارات ", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        HorizontalDivider(color = SkyOutline.copy(alpha = 0.5f))

                        Text(
                            text = "اختر نغمة التنبيه المرتفعة المناسبة لك لتنبيهك قبل موعد الدرس بـ 10 دقائق:",
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyOnSurfaceVariant,
                            fontSize = 11.sp
                        )

                        // Options Radio/Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM or android.media.RingtoneManager.TYPE_NOTIFICATION or android.media.RingtoneManager.TYPE_RINGTONE)
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    }
                                    ringtonePickerLauncher.launch(intent)
                                },
                                enabled = isNotificationsEnabled && !isSoundMuted,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary)
                            ) {
                                Text("اختر نغمة من ملفات الجهاز ", fontWeight = FontWeight.Bold)
                            }
                            if (currentSoundUriStr != null && !isSoundMuted) {
                                Text(
                                    text = "تم اختيار نغمة مخصصة.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SkyPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Test Sound Button
                        OutlinedButton(
                            onClick = {
                                if (isSoundMuted) {
                                    Toast.makeText(context, " الصوت مكتوم حالياً. قم بإلغاء الكتم أولاً للتجربة.", Toast.LENGTH_SHORT).show()
                                } else {
                                    NotificationHelper.playTestSound(context)
                                    Toast.makeText(context, " جاري تشغيل نغمة التنبيه للتجربة...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = isNotificationsEnabled && !isSoundMuted,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(" تجربة واستماع لنغمة التنبيه الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Section 2: Save Backup to File Manager (تصدير وإمكانية اختيار المجلد)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SkySurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyPrimaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.FolderZip,
                                contentDescription = null,
                                tint = SkyPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = " تصدير وحفظ النسخة الاحتياطية",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SkyOnSurface
                            )
                        }

                        Text(
                            text = "احفظ نسخة كاملة من بيانات الطلاب والمجموعات والدرجات. يمكنك تحديد المجلد الذي تريد الحفظ فيه بنفسك عبر مدير الملفات.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyOnSurfaceVariant,
                            fontSize = 11.sp
                        )

                        // Save to File Manager Button
                        Button(
                            onClick = {
                                val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ENGLISH).format(Date())
                                val fileName = "نسخة_مساعد_المعلم_$dateStamp.json"
                                createDocumentLauncher.launch(fileName)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ في مدير الملفات (اختر المجلد)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Alternative Share / Export Button
                        OutlinedButton(
                            onClick = {
                                viewModel.exportBackupData(context) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة أو إرسال الملف عبر التطبيقات", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Section 3: Import Backup File (استرجاع البيانات)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SkySurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LimeContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.RestorePage,
                                contentDescription = null,
                                tint = LimePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = " استرجاع النسخة الاحتياطية",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SkyOnSurface
                            )
                        }

                        Text(
                            text = "اختر ملف النسخة الاحتياطية (بصيغة JSON) لاستعادة كافة البيانات السابقة بسهولة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SkyOnSurfaceVariant,
                            fontSize = 11.sp
                        )

                        // Import Button
                        Button(
                            onClick = {
                                try {
                                    importLauncher.launch("*/*")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "فشل فتح مدير الملفات", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = LimePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختيار ملف واسترجاع البيانات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Section 4: Excel Import & Export (إكسل)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.TableChart,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = " إدارة ملفات Excel (استيراد وتصدير)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF166534)
                            )
                        }

                        Text(
                            text = "يمكنك تصدير كشوف الطلاب والبيانات بتنسيق Excel CSV أو استيراد قوائم الطلاب مباشرة من ملفات الإكسل.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF15803D),
                            fontSize = 11.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Export Excel Button
                            Button(
                                onClick = {
                                    viewModel.exportStudentsToExcel(context) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصدير إكسل ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }

                            // Import Excel Button
                            OutlinedButton(
                                onClick = {
                                    try {
                                        importExcelLauncher.launch("*/*")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "فشل فتح مدير الملفات", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16A34A)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Outlined.DownloadForOffline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد إكسل ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Section 5: Activation Code Generator (مولد الأكواد)
                var showCodeGenerator by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Key,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = " مولّد أكواد تفعيل VIP",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF92400E)
                            )
                        }

                        Text(
                            text = "توليد أكواد التفعيل الفورية لباقة الترم (100 ج.م) أو باقة العام الكامل (200 ج.م) وإرسالها للعميل مباشرة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB45309),
                            fontSize = 11.sp
                        )

                        Button(
                            onClick = { showCodeGenerator = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فتح مولّد الأكواد ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Section 6: Wipe / Clear All Data (مسح جميع البيانات والسحابة)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteForever,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = " مسح كافة البيانات من السحابة والجهاز",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF991B1B)
                            )
                        }

                        Text(
                            text = "تنبيه: سيؤدي هذا الإجراء إلى حذف جميع المجموعات والطلاب وسجلات الحضور والامتحانات والماليات بشكل نهائي.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB91C1C),
                            fontSize = 11.sp
                        )

                        Button(
                            onClick = { showWipeConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مسح كافة البيانات نهائياً ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showCodeGenerator) {
                    ActivationCodeGeneratorDialog(
                        onDismiss = { showCodeGenerator = false }
                    )
                }

                if (showWipeConfirmDialog) {
                    PremiumAlertDialog(
                        onDismissRequest = { showWipeConfirmDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                                Text(
                                    text = "تأكيد مسح البيانات والسحابة ",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B),
                                    fontSize = 16.sp
                                )
                            }
                        },
                        text = {
                            Text(
                                text = "هل أنت متأكد تماماً من رغبتك في مسح كافة البيانات والسحابة؟\n\nسيتم حذف جميع المجموعات، الطلاب، سجلات الحضور، الدرجات والمدفوعات نهائياً ولن تتمكن من التراجع عن هذا الإجراء إلا إذا كانت لديك نسخة احتياطية سابقة.",
                                color = Color(0xFF374151),
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showWipeConfirmDialog = false
                                    viewModel.wipeAllData { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                            ) {
                                Text("موافق (مسح نهائي)", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showWipeConfirmDialog = false }) {
                                Text("إلغاء (تراجع)", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
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

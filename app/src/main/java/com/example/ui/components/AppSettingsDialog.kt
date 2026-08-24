package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.auth.SecurityManager
import com.example.R
import com.example.notification.NotificationHelper
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val teacherProfile by viewModel.teacherProfile.collectAsState()
    val allStudents by viewModel.allStudents.collectAsState()
    val groups by viewModel.groups.collectAsState()
    fun label(ar: String, en: String) = if (appLanguage == "en") en else ar

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var showStudentCardDialog by remember { mutableStateOf(false) }
    var showAllStudentCards by remember { mutableStateOf(false) }
    var showCertificatePicker by remember { mutableStateOf(false) }
    var certificateStudent by remember { mutableStateOf<com.example.data.StudentEntity?>(null) }
    var showSecuritySettingsDialog by remember { mutableStateOf(false) }
    var showCodeGenerator by remember { mutableStateOf(false) }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    // Excel import launcher
    val importExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            viewModel.importStudentsFromExcel(context, selectedUri) { success, message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides if (appLanguage == "en") LayoutDirection.Ltr else LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PremiumDialogDirectionGuard()
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.94f)
                    .padding(vertical = 12.dp),
                tonalElevation = 0.dp,
                shadowElevation = 18.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .7f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = label("الإعدادات", "Settings"),
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = label("تخصيص وضبط خيارات تطبيق أستاذي", "Customize Ostazi and manage its preferences"),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = label("إغلاق", "Close"), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Scrollable Settings Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ==========================================
                        // SECTION 1: إعدادات التطبيق (App Settings)
                        // ==========================================
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .62f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // 1. تعديل الملف الشخصي
                                SettingsNavRow(
                                    icon = Icons.Outlined.AccountCircle,
                                    premiumIcon = ReferenceNavIconKind.STUDENTS,
                                    title = label("تعديل الملف الشخصي", "Edit teacher profile"),
                                    subtitle = "${teacherProfile.name} • ${teacherProfile.subject}",
                                    iconColor = SkyPrimary,
                                    onClick = { showEditProfileDialog = true }
                                )

                                SettingsRowDivider()

                                // 2. الوضع الليلي
                                val isDark = themeMode == "DARK"
                                SettingsSwitchRow(
                                    icon = if (isDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                                    premiumIcon = ReferenceNavIconKind.THEME,
                                    title = label("الوضع الليلي", "Dark mode"),
                                    subtitle = if (isDark) label("مفعّل (مظهر داكن مريح للعين)", "On — comfortable dark appearance") else label("معطّل (مظهر نهاري فاتح)", "Off — bright light appearance"),
                                    checked = isDark,
                                    iconColor = Color(0xFF6366F1),
                                    onCheckedChange = { viewModel.toggleThemeMode() }
                                )

                                SettingsRowDivider()

                                // 3. اللغة الإنجليزية / لغة التطبيق
                                val isEnglish = appLanguage == "en"
                                SettingsSwitchRow(
                                    icon = Icons.Outlined.Language,
                                    premiumIcon = ReferenceNavIconKind.LANGUAGE,
                                    title = label("اللغة الإنجليزية (English)", "App language: English"),
                                    subtitle = if (isEnglish) "English is active" else "العربية مفعلة",
                                    checked = isEnglish,
                                    iconColor = Color(0xFF0EA5E9),
                                    onCheckedChange = { viewModel.toggleAppLanguage() }
                                )

                                SettingsRowDivider()

                                // 4. إعدادات نغمة الإشعارات
                                SettingsNavRow(
                                    icon = Icons.Outlined.NotificationsActive,
                                    premiumIcon = ReferenceNavIconKind.NOTIFICATION,
                                    title = label("إعدادات نغمة وتنبيهات الإشعارات", "Notification sound and alerts"),
                                    subtitle = label("تخصيص نغمة الحصص وتنبيهات التذكير", "Customize class sounds and reminder alerts"),
                                    iconColor = Color(0xFFF59E0B),
                                    onClick = { showNotificationSettingsDialog = true }
                                )

                                SettingsRowDivider()



                                // 5.5 عرض جميع بطاقات الطلاب
                                SettingsNavRow(
                                    icon = Icons.Outlined.Style,
                                    premiumIcon = ReferenceNavIconKind.CARDS,
                                    title = label("عرض جميع بطاقات الطلاب", "View all student cards"),
                                    subtitle = label("جميع الكروت مقسمة حسب كل مجموعة للطباعة", "Printable cards organized by group"),
                                    iconColor = Color(0xFF6366F1),
                                    badgeText = label("جديد", "New"),
                                    onClick = { showAllStudentCards = true }
                                )

                                SettingsRowDivider()

                                SettingsNavRow(
                                    icon = Icons.Outlined.WorkspacePremium,
                                    premiumIcon = ReferenceNavIconKind.CARDS,
                                    title = label("شهادات التقدير", "Certificates of appreciation"),
                                    subtitle = label("إنشاء وتخصيص وطباعة شهادة فاخرة لأي طالب", "Create, customize and print a premium student certificate"),
                                    iconColor = Color(0xFFD39E36),
                                    onClick = { showCertificatePicker = true }
                                )

                                SettingsRowDivider()

                                // 6. تحديد نوع قفل التطبيق
                                val currentLockOption = SecurityManager.getSecurityOption(context)
                                val lockLabel = when (currentLockOption) {
                                    SecurityManager.SecurityOption.BIOMETRIC -> label("بصمة الإصبع", "Fingerprint")
                                    SecurityManager.SecurityOption.PIN -> label("رقم سري (PIN)", "PIN")
                                    else -> label("بدون حماية (مفتوح)", "No lock")
                                }
                                SettingsNavRow(
                                    icon = Icons.Outlined.Security,
                                    premiumIcon = ReferenceNavIconKind.SECURITY,
                                    title = label("تحديد نوع قفل التطبيق والأمان", "App lock and security"),
                                    subtitle = label("النوع الحالي: $lockLabel (يحمي المالية والدخول)", "Current: $lockLabel — protects access and finance"),
                                    iconColor = Color(0xFF10B981),
                                    onClick = { showSecuritySettingsDialog = true }
                                )

                            }
                        }

                        // ==========================================
                        // SECTION 2: البيانات والنسخ الاحتياطي (Data)
                        // ==========================================
                        SettingsCategoryTitle(title = label("البيانات والنسخ الاحتياطي", "Data and backup"), icon = Icons.Outlined.Storage)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .62f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {



                                // 4. مسح كافة البيانات وتصفير التطبيق
                                SettingsNavRow(
                                    icon = Icons.Outlined.DeleteForever,
                                    premiumIcon = ReferenceNavIconKind.DATA,
                                    title = label("مسح كافة البيانات وتصفير التطبيق", "Erase all app data"),
                                    subtitle = label("حذف جميع المجموعات والطلاب وسجلات الحضور والماليات", "Delete groups, students, attendance and finance records"),
                                    iconColor = Color(0xFFDC2626),
                                    onClick = { showWipeConfirmDialog = true }
                                )
                            }
                        }

                        // ==========================================
                        // SECTION 3: حسابات التواصل (Social Media)
                        // ==========================================
                        SettingsCategoryTitle(title = label("حساباتنا على مواقع التواصل", "Social accounts"), icon = Icons.Outlined.Share)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .62f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label("تابعنا لمعرفة آخر التحديثات والشروحات", "Follow us for updates and tutorials"),
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Facebook
                                    IconButton(onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/share/1ErYaG8y3Q/"))
                                        context.startActivity(intent)
                                    }) {
                                        SocialFacebookIcon()
                                    }
                                    // YouTube
                                    IconButton(onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@abdallah.elgraihy?si=3_bXdAfk91Y_S_WJ"))
                                        context.startActivity(intent)
                                    }) {
                                        SocialYouTubeIcon()
                                    }
                                    // Telegram
                                    IconButton(onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/abdallahelgraihy"))
                                        context.startActivity(intent)
                                    }) {
                                        SocialTelegramIcon()
                                    }
                                    // TikTok
                                    IconButton(onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@abdallahalgraihy?_r=1&_t=ZS-994JM8oyoja"))
                                        context.startActivity(intent)
                                    }) {
                                        SocialTikTokIcon()
                                    }
                                }
                            }
                        }

                        // ==========================================
                        // SECTION 4: حول التطبيق (About App)
                        // Exactly matching the user requirement & Photo
                        // ==========================================
                        SettingsCategoryTitle(title = label("حول التطبيق", "About"), icon = Icons.Outlined.Shield)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .62f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = SkyPrimary,
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Image(
                                            painter = painterResource(R.drawable.ostazy_splash_logo),
                                            contentDescription = "Ostazy",
                                            modifier = Modifier.fillMaxSize().padding(8.dp).clip(CircleShape)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = label("تطبيق أستاذي", "Ostazi"),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SkyPrimary.copy(alpha = 0.12f),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label("الإصدار 2.0.0 (نسخة apk)", "Version 2.0.0 (APK)"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SkyPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = label("تم تطوير هذا التطبيق لمساعدة المعلمين في إدارة أعمالهم اليومية بكفاءة عالية. جميع البيانات تُحفظ على قاعدة بيانات لضمان الخصوصية والعمل بدون إنترنت.", "Ostazi helps teachers manage their daily work efficiently. Data is stored locally for privacy and offline access."),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = label("أستاذي - مساعد المعلم الذكي", "Ostazi — the smart teacher assistant"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SkyPrimary
                                )
                            }
                        }
                    }

                    // Dialog Close Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(label("تم • إغلاق الإعدادات", "Done • Close settings"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // --- Sub-Dialogs ---

        // 1. Edit Teacher Profile Dialog
        if (showEditProfileDialog) {
            EditTeacherProfileDialog(
                currentName = teacherProfile.name,
                currentSubject = teacherProfile.subject,
                onSave = { newName, newSubject ->
                    viewModel.saveTeacherProfile(newName, newSubject)
                    showEditProfileDialog = false
                    Toast.makeText(context, "تم حفظ الملف الشخصي بنجاح", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showEditProfileDialog = false }
            )
        }

        // 2. Notification Sound & Settings Dialog
        if (showNotificationSettingsDialog) {
            NotificationSettingsDialog(
                onDismiss = { showNotificationSettingsDialog = false }
            )
        }

        // 3. Student Card Dialog
        if (showStudentCardDialog) {
            StudentCardDialog(
                viewModel = viewModel,
                onDismiss = { showStudentCardDialog = false }
            )
        }

        // 3.5 All Student Cards Screen
        if (showAllStudentCards) {
            Dialog(
                onDismissRequest = { showAllStudentCards = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                PremiumDialogDirectionGuard()
                Surface(modifier = Modifier.fillMaxSize()) {
                    com.example.ui.screens.StudentSmartCardsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { showAllStudentCards = false }
                    )
                }
            }
        }

        if (showCertificatePicker) {
            PremiumAlertDialog(
                onDismissRequest = { showCertificatePicker = false },
                title = {
                    Text(
                        label("اختيار الطالب للشهادة", "Choose a student"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    if (allStudents.isEmpty()) {
                        Text(label("لا يوجد طلاب مسجلون حالياً", "No students are currently registered"))
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allStudents.forEach { student ->
                                val group = groups.find { it.id == student.groupId }
                                Surface(
                                    onClick = {
                                        certificateStudent = student
                                        showCertificatePicker = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.WorkspacePremium,
                                            contentDescription = null,
                                            tint = Color(0xFFD39E36)
                                        )
                                        Column {
                                            Text(student.fullName, fontWeight = FontWeight.Bold)
                                            Text(
                                                group?.name ?: label("بدون مجموعة", "No group"),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showCertificatePicker = false }) {
                        Text(label("إلغاء", "Cancel"))
                    }
                }
            )
        }

        certificateStudent?.let { student ->
            val group = groups.find { it.id == student.groupId }
            CertificateOfAppreciationDialog(
                student = student,
                subjectName = group?.subject ?: teacherProfile.subject,
                initialTeacherName = teacherProfile.name,
                initialDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                onDismiss = { certificateStudent = null }
            )
        }

        // 4. Security & Lock Type Dialog
        if (showSecuritySettingsDialog) {
            SecurityLockSettingsDialog(
                onDismiss = { showSecuritySettingsDialog = false }
            )
        }

        // 5. VIP Code Generator Dialog
        if (showCodeGenerator) {
            ActivationCodeGeneratorDialog(
                onDismiss = { showCodeGenerator = false }
            )
        }

        // 6. Wipe Data Confirm Dialog
        if (showWipeConfirmDialog) {
            PremiumAlertDialog(
                onDismissRequest = { showWipeConfirmDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                        Text(
                            text = "تأكيد مسح البيانات ",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B),
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Text(
                        text = "هل أنت متأكد تماماً من رغبتك في مسح كافة البيانات؟\n\nسيتم حذف جميع المجموعات، الطلاب، سجلات الحضور، الدرجات والمدفوعات نهائياً ولن تتمكن من التراجع عن هذا الإجراء إلا إذا كانت لديك نسخة احتياطية سابقة.",
                        color = MaterialTheme.colorScheme.onSurface,
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
}

// -------------------------------------------------------------
// HELPER COMPOSABLES FOR SETTINGS UI
// -------------------------------------------------------------

@Composable
fun SettingsCategoryTitle(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(6.dp).size(16.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SettingsNavRow(
    icon: ImageVector,
    premiumIcon: ReferenceNavIconKind? = null,
    title: String,
    subtitle: String,
    iconColor: Color,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, iconColor.copy(alpha = .15f)),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (premiumIcon != null) {
                        ReferenceNavIcon(kind = premiumIcon, color = iconColor, size = 20.dp)
                    } else {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF8B5CF6)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
        Icon(
            imageVector = if (LocalLayoutDirection.current == LayoutDirection.Ltr) Icons.Outlined.ChevronRight else Icons.Outlined.ChevronLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    premiumIcon: ReferenceNavIconKind? = null,
    title: String,
    subtitle: String,
    checked: Boolean,
    iconColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .heightIn(min = 64.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, iconColor.copy(alpha = .15f)),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (premiumIcon != null) {
                        ReferenceNavIcon(kind = premiumIcon, color = iconColor, size = 20.dp)
                    } else {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun SettingsRowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}

// -------------------------------------------------------------
// SUB-DIALOGS (Teacher Profile, Sound, Security)
// -------------------------------------------------------------

@Composable
fun EditTeacherProfileDialog(
    currentName: String,
    currentSubject: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var subject by remember { mutableStateOf(currentSubject) }
    val isEnglish = LocalLayoutDirection.current == LayoutDirection.Ltr
    fun label(ar: String, en: String) = if (isEnglish) en else ar

    PremiumAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = null, tint = SkyPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(label("تعديل بيانات المعلم", "Edit teacher profile"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(label("اسم المعلم", "Teacher name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(label("المادة الدراسية", "Subject")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), subject.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary)
            ) {
                Text(label("حفظ التغييرات", "Save changes"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(label("إلغاء", "Cancel"))
            }
        }
    )
}

@Composable
fun NotificationSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isNotificationsEnabled by remember { mutableStateOf(NotificationHelper.isNotificationEnabled(context)) }
    var isSoundMuted by remember { mutableStateOf(NotificationHelper.isSoundMuted(context)) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                NotificationHelper.setSelectedSoundUriString(context, uri.toString())
                Toast.makeText(context, "تم حفظ النغمة بنجاح ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    PremiumAlertDialog(
        onDismissRequest = {
            NotificationHelper.stopSound()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = SkyPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إعدادات نغمة الإشعارات ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Toggle notifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تفعيل التنبيهات قبل الحصة بـ 10 دقائق", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("يرسل تنبيهاً ذكياً قبل موعد كل مجموعة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isNotificationsEnabled,
                        onCheckedChange = {
                            isNotificationsEnabled = it
                            NotificationHelper.setNotificationEnabled(context, it)
                        }
                    )
                }

                HorizontalDivider()

                // Toggle Mute Sound
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("كتم الصوت (صامت فقط)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("اهتزاز بدون تشغيل نغمة صوتية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isSoundMuted,
                        onCheckedChange = {
                            isSoundMuted = it
                            NotificationHelper.setSoundMuted(context, it)
                            if (it) NotificationHelper.stopSound()
                        }
                    )
                }

                HorizontalDivider()

                // Pick Ringtone Button
                Button(
                    onClick = {
                        val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM or android.media.RingtoneManager.TYPE_NOTIFICATION)
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر نغمة تنبيهات الدروس")
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        }
                        ringtonePickerLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اختيار نغمة مخصصة من الهاتف ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Test Ringtone Button
                OutlinedButton(
                    onClick = {
                        NotificationHelper.playTestSound(context)
                        Toast.makeText(context, "جاري تجربة النغمة...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تجربة النغمة المختارة ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    NotificationHelper.stopSound()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary)
            ) {
                Text("حفظ وإغلاق")
            }
        }
    )
}

@Composable
fun SecurityLockSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedOption by remember { mutableStateOf(SecurityManager.getSecurityOption(context)) }
    var pin by remember { mutableStateOf(SecurityManager.getPin(context) ?: "") }

    val options = listOf(
        Triple(SecurityManager.SecurityOption.NONE, "بدون حماية (مفتوح)", Icons.Outlined.LockOpen),
        Triple(SecurityManager.SecurityOption.BIOMETRIC, "بصمة الإصبع (Biometric)", Icons.Outlined.Fingerprint),
        Triple(SecurityManager.SecurityOption.PIN, "رقم سري (PIN)", Icons.Outlined.Lock)
    )

    PremiumAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Security, contentDescription = null, tint = SkyPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تحديد نوع قفل التطبيق والأمان ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "القفل المحدد سيتم طلبه عند فتح التطبيق وعند محاولة الدخول لقسم المالية  لضمان سرية حساباتك.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                options.forEach { (option, label, icon) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedOption == option) SkyPrimary.copy(alpha = 0.12f) else Color.Transparent,
                        border = BorderStroke(1.dp, if (selectedOption == option) SkyPrimary else Color.LightGray.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedOption = option
                                SecurityManager.setSecurityOption(context, option)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedOption == option,
                                onClick = {
                                    selectedOption = option
                                    SecurityManager.setSecurityOption(context, option)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(icon, contentDescription = null, tint = if (selectedOption == option) SkyPrimary else Color.Gray, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                if (selectedOption == SecurityManager.SecurityOption.PIN) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 5 && it.all { char -> char.isDigit() }) pin = it },
                        label = { Text("أدخل رقم سري مكون من 5 أرقام") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedOption == SecurityManager.SecurityOption.PIN) {
                        if (pin.length == 5) {
                            SecurityManager.setPin(context, pin)
                            SecurityManager.setSecurityOption(context, selectedOption)
                            Toast.makeText(context, "تم تفعيل القفل بالرقم السري بنجاح ", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "يرجى كتابة 5 أرقام للرقم السري", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        SecurityManager.setSecurityOption(context, selectedOption)
                        Toast.makeText(context, "تم حفظ إعدادات القفل بنجاح", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary)
            ) {
                Text("حفظ وتأكيد", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

// -------------------------------------------------------------
// SOCIAL MEDIA ICONS
// -------------------------------------------------------------

@Composable
fun SocialFacebookIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFF1877F2)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val fPath = Path().apply {
                moveTo(w * 0.65f, h * 0.35f)
                lineTo(w * 0.52f, h * 0.35f)
                cubicTo(w * 0.46f, h * 0.35f, w * 0.43f, h * 0.38f, w * 0.43f, h * 0.45f)
                lineTo(w * 0.43f, h * 0.54f)
                lineTo(w * 0.64f, h * 0.54f)
                lineTo(w * 0.61f, h * 0.72f)
                lineTo(w * 0.43f, h * 0.72f)
                lineTo(w * 0.43f, h * 1.0f)
                lineTo(w * 0.25f, h * 1.0f)
                lineTo(w * 0.25f, h * 0.72f)
                lineTo(w * 0.15f, h * 0.72f)
                lineTo(w * 0.15f, h * 0.54f)
                lineTo(w * 0.25f, h * 0.54f)
                lineTo(w * 0.25f, h * 0.42f)
                cubicTo(w * 0.25f, h * 0.25f, w * 0.35f, h * 0.15f, w * 0.52f, h * 0.15f)
                lineTo(w * 0.65f, h * 0.15f)
                close()
            }
            drawPath(fPath, color = Color.White)
        }
    }
}

@Composable
fun SocialYouTubeIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFFFF0000)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.1f, h * 0.25f),
                size = Size(w * 0.8f, h * 0.5f),
                cornerRadius = CornerRadius(w * 0.15f, h * 0.15f)
            )
            val playPath = Path().apply {
                moveTo(w * 0.42f, h * 0.38f)
                lineTo(w * 0.62f, h * 0.5f)
                lineTo(w * 0.42f, h * 0.62f)
                close()
            }
            drawPath(playPath, color = Color(0xFFFF0000))
        }
    }
}

@Composable
fun SocialTelegramIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFF2AABEE)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val planePath = Path().apply {
                moveTo(w * 0.82f, h * 0.18f)
                lineTo(w * 0.15f, h * 0.48f)
                cubicTo(w * 0.1f, h * 0.5f, w * 0.1f, h * 0.56f, w * 0.18f, h * 0.58f)
                lineTo(w * 0.38f, h * 0.65f)
                lineTo(w * 0.65f, h * 0.32f)
                lineTo(w * 0.44f, h * 0.69f)
                lineTo(w * 0.44f, h * 0.82f)
                cubicTo(w * 0.44f, h * 0.87f, w * 0.5f, h * 0.89f, w * 0.54f, h * 0.85f)
                lineTo(w * 0.64f, h * 0.74f)
                lineTo(w * 0.76f, h * 0.83f)
                cubicTo(w * 0.82f, h * 0.87f, w * 0.88f, h * 0.83f, w * 0.89f, h * 0.76f)
                lineTo(w * 0.98f, h * 0.24f)
                cubicTo(w * 1.0f, h * 0.17f, w * 0.9f, h * 0.13f, w * 0.82f, h * 0.18f)
                close()
            }
            drawPath(planePath, color = Color.White)
        }
    }
}

@Composable
fun SocialTikTokIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            
            val path = Path().apply {
                val sx = w / 24f
                val sy = h / 24f
                
                moveTo(19.3f * sx, 8.1f * sy)
                cubicTo(17.6f * sx, 8.0f * sy, 16.1f * sx, 7.3f * sy, 15.0f * sx, 6.2f * sy)
                cubicTo(14.3f * sx, 5.5f * sy, 13.8f * sx, 4.6f * sy, 13.6f * sx, 3.6f * sy)
                lineTo(10.8f * sx, 3.6f * sy)
                lineTo(10.8f * sx, 15.3f * sy)
                cubicTo(10.8f * sx, 16.2f * sy, 10.4f * sx, 17.0f * sy, 9.8f * sx, 17.6f * sy)
                cubicTo(9.2f * sx, 18.2f * sy, 8.3f * sx, 18.6f * sy, 7.4f * sx, 18.6f * sy)
                cubicTo(5.6f * sx, 18.6f * sy, 4.1f * sx, 17.1f * sy, 4.1f * sx, 15.3f * sy)
                cubicTo(4.1f * sx, 13.5f * sy, 5.6f * sx, 12.0f * sy, 7.4f * sx, 12.0f * sy)
                cubicTo(7.9f * sx, 12.0f * sy, 8.4f * sx, 12.1f * sy, 8.8f * sx, 12.3f * sy)
                lineTo(8.8f * sx, 9.4f * sy)
                cubicTo(8.3f * sx, 9.3f * sy, 7.8f * sx, 9.2f * sy, 7.4f * sx, 9.2f * sy)
                cubicTo(4.0f * sx, 9.2f * sy, 1.3f * sx, 11.9f * sy, 1.3f * sx, 15.3f * sy)
                cubicTo(1.3f * sx, 18.7f * sy, 4.0f * sx, 21.4f * sy, 7.4f * sx, 21.4f * sy)
                cubicTo(10.8f * sx, 21.4f * sy, 13.6f * sx, 18.7f * sy, 13.6f * sx, 15.3f * sy)
                lineTo(13.6f * sx, 9.6f * sy)
                cubicTo(15.2f * sx, 10.8f * sy, 17.2f * sx, 11.5f * sy, 19.3f * sx, 11.6f * sy)
                lineTo(19.3f * sx, 8.1f * sy)
                close()
            }
            drawPath(path = path, color = Color.White)
        }
    }
}

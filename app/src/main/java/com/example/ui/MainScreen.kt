package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.BiometricHelper
import com.example.auth.SecurityManager
import com.example.notification.NotificationHelper
import com.example.ui.components.SettingsBackupDialog
import com.example.ui.components.CloudBackupDialog
import com.example.ui.components.AppSettingsDialog
import com.example.ui.components.NotificationSettingsDialog
import com.example.ui.components.ReferenceNavIcon
import com.example.ui.components.ReferenceNavIconKind
import com.example.ui.components.PremiumAlertDialog
import com.example.ui.screens.*
import com.example.ui.theme.*

val CrownVectorIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Crown",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = androidx.compose.ui.graphics.SolidColor(Color(0xFFFFD700))
        ) {
            moveTo(5f, 16f)
            lineTo(3f, 5f)
            lineTo(8.5f, 10f)
            lineTo(12f, 4f)
            lineTo(15.5f, 10f)
            lineTo(21f, 5f)
            lineTo(19f, 16f)
            lineTo(5f, 16f)
            close()
            moveTo(19f, 19f)
            curveTo(19f, 19.55f, 18.55f, 20f, 18f, 20f)
            lineTo(6f, 20f)
            curveTo(5.45f, 20f, 5f, 19.55f, 5f, 19f)
            lineTo(5f, 18f)
            lineTo(19f, 18f)
            lineTo(19f, 19f)
            close()
        }
    }.build()

sealed class BottomTab(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomTab("dashboard", "الرئيسية", Icons.Outlined.Home)
    object Groups : BottomTab("groups", "المجموعات", Icons.Outlined.Groups)
    object Students : BottomTab("students", "الطلاب", Icons.Outlined.Person)
    object Timetable : BottomTab("timetable", "الجدول", Icons.Outlined.CalendarMonth)
    object Payments : BottomTab("payments", "المالية", Icons.Outlined.AccountBalanceWallet)
    object PerSession : BottomTab("per_session", "بالحصة", Icons.Outlined.ConfirmationNumber)
    object Statistics : BottomTab("statistics", "إحصائيات", Icons.Outlined.BarChart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onSignOut: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var currentTab by remember { mutableStateOf<BottomTab>(BottomTab.Dashboard) }
    var targetTabToNavigate by remember { mutableStateOf<BottomTab?>(null) }
    var showCloudBackupDialog by remember { mutableStateOf(false) }
    var showAppSettingsDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var soundMuted by remember { mutableStateOf(NotificationHelper.isSoundMuted(context)) }
    var showVipDialog by remember { mutableStateOf(false) }
    var showSecuritySettings by remember { mutableStateOf(false) }
    var showFinancePinDialog by remember { mutableStateOf(false) }
    var enteredFinancePin by remember { mutableStateOf("") }
    var financePinError by remember { mutableStateOf(false) }

    val handleTabNavigation: (BottomTab) -> Unit = { targetTab ->
        if (targetTab == BottomTab.Payments || targetTab == BottomTab.PerSession) {
            val secOption = SecurityManager.getSecurityOption(context)
            when (secOption) {
                SecurityManager.SecurityOption.BIOMETRIC -> {
                    (context as? androidx.fragment.app.FragmentActivity)?.let { activity ->
                        com.example.auth.BiometricHelper.showBiometricPrompt(
                            activity = activity,
                            title = AppStrings.identityTitle(appLanguage),
                            subtitle = AppStrings.identitySubtitle(appLanguage),
                            negativeButtonText = AppStrings.cancel(appLanguage),
                            onSuccess = {
                                currentTab = targetTab
                            },
                            onError = { err ->
                                android.widget.Toast.makeText(context, "لم يتم تأكيد الهوية", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    } ?: run {
                        currentTab = targetTab
                    }
                }
                SecurityManager.SecurityOption.PIN -> {
                    enteredFinancePin = ""
                    financePinError = false
                    targetTabToNavigate = targetTab
                    showFinancePinDialog = true
                }
                else -> {
                    currentTab = targetTab
                }
            }
        } else {
            currentTab = targetTab
        }
    }

    val layoutDir = if (appLanguage == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(top = 8.dp, bottom = 6.dp, start = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                // 1. King's Crown (تاج الملك - VIP)
                                IconButton(
                                    onClick = { showVipDialog = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = CrownVectorIcon,
                                        contentDescription = "VIP",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showNotificationSettingsDialog = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (soundMuted) Icons.Outlined.VolumeOff else Icons.Outlined.NotificationsActive,
                                        contentDescription = if (appLanguage == "en") "Class reminder sound" else "نغمة تنبيه الحصص",
                                        tint = if (soundMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.setThemeMode(if (themeMode == "DARK") "LIGHT" else "DARK")
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (themeMode == "DARK") Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                        contentDescription = if (appLanguage == "en") "Toggle theme" else "تبديل الوضع الفاتح والداكن",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }



                                // 4. Cloud Backup (السحابة)
                                IconButton(
                                    onClick = { showCloudBackupDialog = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudSync,
                                        contentDescription = AppStrings.cloudSync(appLanguage),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // 5. Settings Gear (الضبط)
                                IconButton(
                                    onClick = { showAppSettingsDialog = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = AppStrings.settings(appLanguage),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            BottomTab.Dashboard,
                            BottomTab.Groups,
                            BottomTab.Students,
                            BottomTab.Timetable,
                            BottomTab.Payments,
                            BottomTab.PerSession
                        )

                        tabs.forEach { tab ->
                            val selected = currentTab.route == tab.route
                            val backgroundColor = Color.Transparent
                            val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = backgroundColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { handleTabNavigation(tab) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp, vertical = 7.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    ReferenceNavIcon(
                                        kind = when (tab) {
                                            BottomTab.Dashboard -> ReferenceNavIconKind.HOME
                                            BottomTab.Groups -> ReferenceNavIconKind.GROUPS
                                            BottomTab.Students -> ReferenceNavIconKind.STUDENTS
                                            BottomTab.Timetable -> ReferenceNavIconKind.CALENDAR
                                            BottomTab.Payments -> ReferenceNavIconKind.FINANCE
                                            else -> ReferenceNavIconKind.SESSION
                                        },
                                        color = contentColor,
                                        size = 23.dp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when (tab) {
                                            BottomTab.Dashboard -> com.example.ui.theme.AppStrings.tabHome(appLanguage)
                                            BottomTab.Groups -> com.example.ui.theme.AppStrings.tabGroups(appLanguage)
                                            BottomTab.Students -> com.example.ui.theme.AppStrings.tabStudents(appLanguage)
                                            BottomTab.Timetable -> com.example.ui.theme.AppStrings.tabTimetable(appLanguage)
                                            BottomTab.Payments -> com.example.ui.theme.AppStrings.tabPayments(appLanguage)
                                            BottomTab.PerSession -> AppStrings.tabSession(appLanguage)
                                            BottomTab.Statistics -> if (appLanguage == "en") "Stats" else "إحصائيات"
                                        },
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = contentColor,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (currentTab) {
                    BottomTab.Dashboard -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToGroups = { handleTabNavigation(BottomTab.Groups) },
                        onNavigateToStudents = { handleTabNavigation(BottomTab.Students) },
                        onNavigateToTimetable = { handleTabNavigation(BottomTab.Timetable) },
                        onNavigateToPayments = { handleTabNavigation(BottomTab.Payments) },
                        onOpenBackupDialog = { showCloudBackupDialog = true }
                    )
                    BottomTab.Groups -> GroupsScreen(viewModel = viewModel)
                    BottomTab.Students -> StudentsScreen(viewModel = viewModel)
                    BottomTab.Timetable -> WeeklyTimetableScreen(viewModel = viewModel)
                    BottomTab.Payments -> PaymentsScreen(viewModel = viewModel)
                    BottomTab.PerSession -> PerSessionScreen(viewModel = viewModel)
                    BottomTab.Statistics -> StatisticsScreen(viewModel = viewModel)
                }
            }
        }

        // Finance PIN Dialog
        if (showFinancePinDialog) {
            val savedPin = SecurityManager.getPin(context) ?: ""
            PremiumAlertDialog(
                onDismissRequest = { showFinancePinDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = SkyPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(AppStrings.financeLock(appLanguage), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = AppStrings.enterFinancePin(appLanguage),
                            fontSize = 12.5.sp,
                            color = SkyOnSurfaceVariant
                        )
                        OutlinedTextField(
                            value = enteredFinancePin,
                            onValueChange = {
                                if (it.length <= 5 && it.all { c -> c.isDigit() }) {
                                    enteredFinancePin = it
                                    financePinError = false
                                    if (it.length == 5) {
                                        if (it == savedPin) {
                                            currentTab = targetTabToNavigate ?: BottomTab.Payments
                                            showFinancePinDialog = false
                                        } else {
                                            financePinError = true
                                        }
                                    }
                                }
                            },
                            label = { Text(AppStrings.pinLabel(appLanguage)) },
                            isError = financePinError,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (financePinError) {
                            Text(
                                text = AppStrings.wrongPin(appLanguage),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (enteredFinancePin == savedPin) {
                                currentTab = targetTabToNavigate ?: BottomTab.Payments
                                showFinancePinDialog = false
                            } else {
                                financePinError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SkyPrimary)
                    ) {
                        Text(AppStrings.confirmOpen(appLanguage))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showFinancePinDialog = false }) {
                        Text(AppStrings.cancel(appLanguage))
                    }
                }
            )
        }

        if (showCloudBackupDialog) {
            CloudBackupDialog(
                viewModel = viewModel,
                onDismiss = { showCloudBackupDialog = false }
            )
        }
        if (showAppSettingsDialog) {
            AppSettingsDialog(
                viewModel = viewModel,
                onDismiss = { showAppSettingsDialog = false }
            )
        }
        if (showNotificationSettingsDialog) {
            NotificationSettingsDialog(
                onDismiss = {
                    soundMuted = NotificationHelper.isSoundMuted(context)
                    showNotificationSettingsDialog = false
                }
            )
        }
        if (showVipDialog) {
            com.example.ui.components.VipDialog(
                onDismiss = { showVipDialog = false }
            )
        }
        if (showSecuritySettings) {
            Dialog(onDismissRequest = { showSecuritySettings = false }) {
                com.example.ui.components.PremiumDialogDirectionGuard()
                Surface(modifier = Modifier.fillMaxSize()) {
                    SecuritySettingsScreen(onNavigateBack = { showSecuritySettings = false })
                }
            }
        }
    }
}

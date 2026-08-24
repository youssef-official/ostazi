package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.auth.AuthManager
import com.example.auth.SecurityManager
import com.example.auth.AuthState
import com.example.auth.BiometricHelper
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            // Create notification channel for class alerts and daily backup
            NotificationHelper.createNotificationChannel(this)
            AlarmScheduler.scheduleDailyBackupReminder(this)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        
        val subscriptionManager = try {
            SubscriptionManager(this)
        } catch (_: Throwable) {
            null
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val appLanguage by viewModel.appLanguage.collectAsState()

            val darkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            val layoutDir = if (appLanguage == "en") androidx.compose.ui.unit.LayoutDirection.Ltr else androidx.compose.ui.unit.LayoutDirection.Rtl

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides layoutDir
            ) {
                MyApplicationTheme(darkTheme = darkTheme) {
                    var showSplash by remember { mutableStateOf(true) }
                    val context = LocalContext.current
                    var securityOption by remember { mutableStateOf(SecurityManager.getSecurityOption(context)) }
                    var isSecurityAuthenticated by remember { mutableStateOf(securityOption == SecurityManager.SecurityOption.NONE) }
                    var showPinEntry by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(securityOption) {
                        if (securityOption == SecurityManager.SecurityOption.BIOMETRIC) {
                            BiometricHelper.showBiometricPrompt(
                                activity = this@MainActivity,
                                onSuccess = { isSecurityAuthenticated = true },
                                onError = { /* Handle error */ }
                            )
                        } else if (securityOption == SecurityManager.SecurityOption.PIN) {
                            showPinEntry = true
                        }
                    }

                    if (showPinEntry) {
                        com.example.ui.components.PremiumAlertDialog(
                            onDismissRequest = { /* Cannot dismiss */ },
                            title = { androidx.compose.material3.Text("أدخل رقم القفل") },
                            text = {
                                var pin by remember { mutableStateOf("") }
                                androidx.compose.material3.OutlinedTextField(
                                    value = pin,
                                    onValueChange = { if (it.length <= 5) pin = it },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                                    singleLine = true
                                )
                                if (pin.length == 5 && pin == SecurityManager.getPin(context)) {
                                    isSecurityAuthenticated = true
                                    showPinEntry = false
                                }
                            },
                            confirmButton = {}
                        )
                    }

                    val isExpired = remember { 
                        mutableStateOf(subscriptionManager?.isFreeTrialExpired() ?: false) 
                    }

                    val isUserAuthenticated = remember {
                        mutableStateOf(
                            try {
                                AuthManager.checkCurrentSession(this@MainActivity)
                            } catch (_: Throwable) {
                                true // Fallback to offline local mode if anything fails
                            }
                        )
                    }

                    val authState by AuthManager.authState.collectAsState()

                    Crossfade(
                        targetState = showSplash,
                        animationSpec = tween(durationMillis = 400),
                        label = "splashCrossfade"
                    ) { isSplash ->
                        if (isSplash) {
                            SplashScreen(
                                onSplashFinished = {
                                    showSplash = false
                                }
                            )
                        } else if (isExpired.value) {
                            SubscriptionScreen(onActivated = { isExpired.value = false })
                        } else if (!isUserAuthenticated.value || authState !is AuthState.LoggedIn) {
                            LoginScreen(
                                onLoginSuccess = {
                                    isUserAuthenticated.value = true
                                }
                            )
                        } else if (!isSecurityAuthenticated) {
                            androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                    androidx.compose.material3.Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                                        contentDescription = "مغلق",
                                        modifier = androidx.compose.ui.Modifier.size(64.dp),
                                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                                    androidx.compose.material3.Text(
                                        "برجاء تأكيد الهوية لفتح التطبيق",
                                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
                                    androidx.compose.material3.Button(onClick = {
                                        if (securityOption == SecurityManager.SecurityOption.BIOMETRIC) {
                                            BiometricHelper.showBiometricPrompt(
                                                activity = this@MainActivity,
                                                onSuccess = { isSecurityAuthenticated = true },
                                                onError = { /* Handle error */ }
                                            )
                                        } else if (securityOption == SecurityManager.SecurityOption.PIN) {
                                            showPinEntry = true
                                        }
                                    }) {
                                        androidx.compose.material3.Text("فتح التطبيق")
                                    }
                                }
                            }
                        } else {
                            MainScreen(
                                viewModel = viewModel,
                                onSignOut = {
                                    AuthManager.signOut(this@MainActivity)
                                    isUserAuthenticated.value = false
                                }
                            )
                        }
                    }
                }
        }
    }
}
}

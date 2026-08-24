package com.example.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class LoggedIn(
        val uid: String,
        val displayName: String,
        val email: String,
        val photoUrl: String,
        val provider: String // "Google", "Facebook", "Guest"
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}

object AuthManager {

    private fun getAuth(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (_: Throwable) {
            null
        }
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun checkCurrentSession(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_auth_session", Context.MODE_PRIVATE)
        val isGuest = prefs.getBoolean("is_guest_mode", true) // Default to true for instant offline access

        val currentUser = try {
            getAuth()?.currentUser
        } catch (_: Throwable) {
            null
        }

        if (currentUser != null) {
            val provider = determineProvider(currentUser)
            _authState.value = AuthState.LoggedIn(
                uid = currentUser.uid,
                displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "معلم متميز",
                email = currentUser.email ?: "",
                photoUrl = currentUser.photoUrl?.toString() ?: "",
                provider = provider
            )
            return true
        } else {
            // Default to guest/offline mode so app opens immediately
            _authState.value = AuthState.LoggedIn(
                uid = "guest_offline_user",
                displayName = "معلم (أستاذي +)",
                email = "offline@local.app",
                photoUrl = "",
                provider = "Guest"
            )
            return true
        }
    }

    suspend fun signInWithGoogle(context: Context, webClientId: String? = null) {
        _authState.value = AuthState.Loading
        val firebaseAuth = getAuth()
        if (firebaseAuth == null) {
            _authState.value = AuthState.Error("خدمة Firebase غير المهيأة. يمكنك متابعة العمل بوضع الأوفلاين المحلي.")
            return
        }
        try {
            val credentialManager = CredentialManager.create(context)
            val clientKey = webClientId.takeIf { !it.isNull_or_blank() } ?: "101234567890-a1b2c3d4e5f6g7h8i9j0.apps.googleusercontent.com"

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientKey)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user

                if (user != null) {
                    saveSessionState(context, isGuest = false)
                    _authState.value = AuthState.LoggedIn(
                        uid = user.uid,
                        displayName = user.displayName ?: user.email?.substringBefore("@") ?: "معلم متميز",
                        email = user.email ?: "",
                        photoUrl = user.photoUrl?.toString() ?: "",
                        provider = "Google"
                    )
                } else {
                    _authState.value = AuthState.Error("فشل العثور على بيانات المستخدم")
                }
            } else {
                _authState.value = AuthState.Error("نوع إذن الدخول غير مطابق")
            }
        } catch (e: GetCredentialException) {
            // Fallback to OAuthProvider for Google if CredentialManager is canceled or unavailable
            signInWithGoogleOAuthProvider(context)
        } catch (e: Exception) {
            // Fallback to OAuthProvider
            signInWithGoogleOAuthProvider(context)
        }
    }

    private suspend fun signInWithGoogleOAuthProvider(context: Context) {
        val firebaseAuth = getAuth()
        if (firebaseAuth == null) {
            _authState.value = AuthState.Error("خدمة Firebase غير مهيأة.")
            return
        }
        try {
            val activity = context as? android.app.Activity
            if (activity == null) {
                _authState.value = AuthState.Error("يتطلب تسجيل الدخول نشاطاً برمجياً فعّالاً")
                return
            }

            val provider = OAuthProvider.newBuilder("google.com")
            val pendingResult = firebaseAuth.pendingAuthResult

            val authResult = if (pendingResult != null) {
                pendingResult.await()
            } else {
                firebaseAuth.startActivityForSignInWithProvider(activity, provider.build()).await()
            }

            val user = authResult.user
            if (user != null) {
                saveSessionState(context, isGuest = false)
                _authState.value = AuthState.LoggedIn(
                    uid = user.uid,
                    displayName = user.displayName ?: user.email?.substringBefore("@") ?: "معلم متميز",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    provider = "Google"
                )
            } else {
                _authState.value = AuthState.Error("تم إلغاء عملية تسجيل الدخول بـ Google")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("خطأ أثناء الدخول بـ Google: ${e.localizedMessage}")
        }
    }

    suspend fun signInWithFacebook(context: Context) {
        _authState.value = AuthState.Loading
        val firebaseAuth = getAuth()
        if (firebaseAuth == null) {
            _authState.value = AuthState.Error("خدمة Firebase غير مهيأة. يمكنك استخدام وضع أوفلاين.")
            return
        }
        try {
            val activity = context as? android.app.Activity
            if (activity == null) {
                _authState.value = AuthState.Error("يتطلب تسجيل الدخول نشاطاً برمجياً فعّالاً")
                return
            }

            val provider = OAuthProvider.newBuilder("facebook.com")
            val pendingResult = firebaseAuth.pendingAuthResult

            val authResult = if (pendingResult != null) {
                pendingResult.await()
            } else {
                firebaseAuth.startActivityForSignInWithProvider(activity, provider.build()).await()
            }

            val user = authResult.user
            if (user != null) {
                saveSessionState(context, isGuest = false)
                _authState.value = AuthState.LoggedIn(
                    uid = user.uid,
                    displayName = user.displayName ?: "مستخدم فيسبوك",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    provider = "Facebook"
                )
            } else {
                _authState.value = AuthState.Error("تم إلغاء عملية تسجيل الدخول بـ Facebook")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("خطأ أثناء الدخول بـ Facebook: ${e.localizedMessage}")
        }
    }

    fun continueAsGuest(context: Context) {
        saveSessionState(context, isGuest = true)
        _authState.value = AuthState.LoggedIn(
            uid = "guest_offline_user",
            displayName = "معلم (وضع أوفلاين)",
            email = "offline@local.app",
            photoUrl = "",
            provider = "Guest"
        )
    }

    fun signOut(context: Context) {
        try {
            getAuth()?.signOut()
        } catch (_: Exception) {}

        val prefs = context.getSharedPreferences("app_auth_session", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        _authState.value = AuthState.Idle
    }

    private fun saveSessionState(context: Context, isGuest: Boolean) {
        val prefs = context.getSharedPreferences("app_auth_session", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_guest_mode", isGuest)
            .putLong("login_timestamp", System.currentTimeMillis())
            .apply()
    }

    private fun determineProvider(user: FirebaseUser): String {
        user.providerData.forEach { profile ->
            when (profile.providerId) {
                GoogleAuthProvider.PROVIDER_ID -> return "Google"
                FacebookAuthProvider.PROVIDER_ID -> return "Facebook"
            }
        }
        return "Firebase Auth"
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
}

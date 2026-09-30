package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class GoogleAuthResult(
    val success: Boolean,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val uid: String? = null,
    val errorMessage: String? = null,
    val isCancelled: Boolean = false,
    val isMissingClientId: Boolean = false,
    val authProvider: String = "google"
)

class GoogleAuthManager(private val context: Context) {

    private val tag = "GoogleAuthManager"
    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.e(tag, "FirebaseAuth unavailable: ${e.message}", e)
            null
        }
    }
    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sam_mikrotic_auth_prefs", Context.MODE_PRIVATE)

    fun getCurrentUser(): FirebaseUser? = try {
        auth?.currentUser
    } catch (e: Throwable) {
        null
    }

    fun getActiveEmail(): String? = try {
        auth?.currentUser?.email
    } catch (e: Throwable) {
        null
    }

    fun getActiveUid(): String? = try {
        auth?.currentUser?.uid
    } catch (e: Throwable) {
        null
    }

    fun isUserLoggedIn(): Boolean = try {
        auth?.currentUser != null
    } catch (e: Throwable) {
        false
    }

    /**
     * Resolves the Web Client ID in the following order:
     * 1. User configured / stored in SharedPreferences
     * 2. R.string.default_web_client_id generated from google-services.json
     */
    fun getSavedWebClientId(): String? {
        val custom = prefs.getString("custom_web_client_id", null)?.trim()
        if (!custom.isNullOrBlank()) return custom

        // Check if google-services plugin generated default_web_client_id
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            val resString = context.getString(resId).trim()
            if (resString.isNotBlank()) return resString
        }

        // Hardcoded fallback since it's confirmed from the user's Firebase console
        return "668455931031-burt9863pi64rshdlmgenejnj27ep0d0.apps.googleusercontent.com"
    }

    fun saveWebClientId(clientId: String) {
        prefs.edit().putString("custom_web_client_id", clientId.trim()).apply()
        Log.i(tag, "Saved custom Web Client ID: ${clientId.trim()}")
    }

    fun clearSavedWebClientId() {
        prefs.edit().remove("custom_web_client_id").apply()
    }

    /**
     * Genuine Google Sign-In using Android Credential Manager and Firebase Authentication.
     * Requires an Activity context to launch the official Google Play Services bottom sheet.
     */
    suspend fun signInWithGoogle(
        activity: Activity? = null,
        customClientId: String? = null
    ): GoogleAuthResult = withContext(Dispatchers.Main) {
        try {
            val resolvedClientId = customClientId?.trim()?.ifBlank { null }
                ?: getSavedWebClientId()

            if (resolvedClientId.isNullOrBlank()) {
                Log.w(tag, "Web Client ID is not configured.")
                return@withContext GoogleAuthResult(
                    success = false,
                    isMissingClientId = true,
                    errorMessage = "معرّف Web Client ID غير متوفر. يرجى توفيره في الرسالة القادمة أو إضافته إلى google-services.json لتفعيل الدخول بنقرة واحدة."
                )
            }

            // 1. Build the official Google Sign-In button option (GetSignInWithGoogleOption)
            // This is specifically designed for explicit button clicks and initiates the native Google account chooser dialog.
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = resolvedClientId)
                .build()

            // 2. Also prepare GetGoogleIdOption as fallback
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(resolvedClientId)
                .setAutoSelectEnabled(false)
                .setFilterByAuthorizedAccounts(false)
                .build()

            val invocationContext: Context = activity ?: context

            // Execute via Credential Manager (trying GetSignInWithGoogleOption first, then fallback)
            val result = try {
                val primaryRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(signInOption)
                    .build()
                credentialManager.getCredential(
                    request = primaryRequest,
                    context = invocationContext
                )
            } catch (noCred: NoCredentialException) {
                Log.w(tag, "GetSignInWithGoogleOption returned NoCredentialException, trying GetGoogleIdOption: ${noCred.message}")
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                credentialManager.getCredential(
                    request = fallbackRequest,
                    context = invocationContext
                )
            }

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName ?: ""
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: ""

                // Authenticate with Firebase Authentication using real Google idToken
                try {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val currentAuth = auth
                    val authResult = currentAuth?.signInWithCredential(firebaseCredential)?.await()
                    val firebaseUser = authResult?.user

                    GoogleAuthResult(
                        success = true,
                        email = firebaseUser?.email ?: email,
                        displayName = firebaseUser?.displayName ?: displayName,
                        photoUrl = firebaseUser?.photoUrl?.toString() ?: photoUrl,
                        uid = firebaseUser?.uid,
                        authProvider = "google.com"
                    )
                } catch (fbEx: Exception) {
                    Log.e(tag, "Firebase sign-in with Google credential failed", fbEx)
                    GoogleAuthResult(
                        success = false,
                        errorMessage = "نجح التحقق من Google ولكن تعذرت المزامنة مع Firebase: ${fbEx.localizedMessage} (تأكد من تفعيل Google في Firebase Console)"
                    )
                }
            } else {
                GoogleAuthResult(
                    success = false,
                    errorMessage = "نوع اعتماد Google غير معتمد أو تعذرت قراءته (${credential.type})"
                )
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(tag, "User cancelled Google sign in")
            GoogleAuthResult(
                success = false,
                isCancelled = true,
                errorMessage = "تم إلغاء عملية تسجيل الدخول من قبل المستخدم"
            )
        } catch (e: NoCredentialException) {
            Log.w(tag, "No Google credentials found on device: ${e.message}")
            GoogleAuthResult(
                success = false,
                errorMessage = "تعذر استرداد حساب Google من خدمات Google Play على جهازك. يرجى التأكد من تسجيل الدخول في حساب Google بالجهاز أو استخدام خيار الدخول المباشر بالبريد وكلمة المرور أدناه."
            )
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.e(tag, "Provider configuration error: ${e.message}")
            GoogleAuthResult(
                success = false,
                isMissingClientId = true,
                errorMessage = "خطأ في تكوين مزود Google: يرجى التحقق من بصمة SHA-1 ومعرف Web Client ID في Firebase Console."
            )
        } catch (e: GetCredentialException) {
            Log.e(tag, "Credential Manager error: ${e.message}")
            val msg = e.localizedMessage ?: "فشل تسجيل الدخول عبر Google"
            GoogleAuthResult(
                success = false,
                errorMessage = if (msg.contains("10") || msg.contains("DEVELOPER_ERROR")) {
                    "خطأ مطور Google (10: DEVELOPER_ERROR): يلزم إضافة بصمة SHA-1 للتطبيق (com.samtecai.sammikrotic) في Firebase Console أسفل إعدادات المشروع."
                } else {
                    "خطأ في خدمات Google Play: $msg"
                }
            )
        } catch (e: Exception) {
            Log.e(tag, "Unexpected Google Auth exception", e)
            GoogleAuthResult(
                success = false,
                errorMessage = "خطأ غير متوقع أثناء تسجيل الدخول: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Real Firebase Authentication with Email and Password
     */
    suspend fun signInWithEmailAndPassword(email: String, pass: String): GoogleAuthResult =
        withContext(Dispatchers.IO) {
            try {
                val cleanEmail = email.trim()
                val cleanPass = pass.trim()
                if (cleanEmail.isBlank() || cleanPass.isBlank()) {
                    return@withContext GoogleAuthResult(
                        success = false,
                        errorMessage = "يرجى كتابة البريد الإلكتروني وكلمة المرور"
                    )
                }
                val currentAuth = auth ?: return@withContext GoogleAuthResult(
                    success = false,
                    errorMessage = "خدمة Firebase غير مهيأة بعد"
                )
                val authResult = currentAuth.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
                val user = authResult.user
                GoogleAuthResult(
                    success = true,
                    email = user?.email ?: cleanEmail,
                    displayName = user?.displayName ?: cleanEmail.substringBefore("@"),
                    uid = user?.uid,
                    authProvider = "password"
                )
            } catch (e: Exception) {
                Log.e(tag, "Firebase email sign-in failed", e)
                val errorDesc = translateFirebaseError(e.message)
                GoogleAuthResult(
                    success = false,
                    errorMessage = errorDesc
                )
            }
        }

    /**
     * Real Firebase Authentication Sign Up with Email and Password
     */
    suspend fun signUpWithEmailAndPassword(
        email: String,
        pass: String,
        displayName: String
    ): GoogleAuthResult = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim()
            val cleanPass = pass.trim()
            val cleanName = displayName.trim().ifBlank { cleanEmail.substringBefore("@") }

            if (cleanEmail.isBlank() || cleanPass.length < 6) {
                return@withContext GoogleAuthResult(
                    success = false,
                    errorMessage = "يجب أن تكون كلمة المرور 6 أحرف أو أكثر والبريد صحيحاً"
                )
            }

            val currentAuth = auth ?: return@withContext GoogleAuthResult(
                success = false,
                errorMessage = "خدمة Firebase غير مهيأة بعد"
            )
            val authResult = currentAuth.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
            val user = authResult.user

            try {
                val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                user?.updateProfile(profileUpdate)?.await()
            } catch (pEx: Exception) {
                Log.w(tag, "Could not update user display name: ${pEx.message}")
            }

            GoogleAuthResult(
                success = true,
                email = user?.email ?: cleanEmail,
                displayName = cleanName,
                uid = user?.uid,
                authProvider = "password"
            )
        } catch (e: Exception) {
            Log.e(tag, "Firebase email sign-up failed", e)
            val errorDesc = translateFirebaseError(e.message)
            GoogleAuthResult(
                success = false,
                errorMessage = errorDesc
            )
        }
    }

    suspend fun sendPasswordReset(email: String): Boolean = withContext(Dispatchers.IO) {
        try {
            auth?.sendPasswordResetEmail(email.trim())?.await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Password reset failed", e)
            false
        }
    }

    suspend fun signOut() {
        try {
            auth?.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Log.i(tag, "Signed out successfully from Firebase and Google Credential Manager")
        } catch (e: Exception) {
            Log.e(tag, "Error signing out", e)
        }
    }

    private fun translateFirebaseError(raw: String?): String {
        if (raw == null) return "خطأ غير معروف في المصادقة"
        return when {
            raw.contains("user-not-found", ignoreCase = true) -> "الحساب غير موجود في Firebase، يمكنك إنشاء حساب جديد"
            raw.contains("wrong-password", ignoreCase = true) -> "كلمة المرور غير صحيحة"
            raw.contains("email-already-in-use", ignoreCase = true) -> "هذا البريد مسجل مسبقاً في Firebase، يرجى تسجيل الدخول"
            raw.contains("invalid-email", ignoreCase = true) -> "صيغة البريد الإلكتروني غير صالحة"
            raw.contains("weak-password", ignoreCase = true) -> "كلمة المرور ضعيفة (يجب أن تتكون من 6 أحرف على الأقل)"
            raw.contains("network", ignoreCase = true) -> "تعذر الاتصال بخوادم Firebase (تحقق من اتصال الإنترنت)"
            raw.contains("operation-not-allowed", ignoreCase = true) -> "طريقة تسجيل الدخول هذه غير مفعلة في Firebase Console (Authentication -> Sign-in method)"
            else -> raw
        }
    }

    fun getPackageName(): String = context.packageName

    companion object {
        const val SHA1_DEBUG = "64:FB:BF:3E:DD:50:75:13:D3:B8:A7:F5:B7:F2:52:23:19:E1:B9:EF"
        const val SHA256_DEBUG = "D5:E9:39:11:E1:C1:FF:A3:A1:D1:DC:5E:18:24:0D:A5:4E:02:A8:21:6E:E0:7D:FD:8B:1A:CA:E6:51:28:A9:46"
        const val DEFAULT_WEB_CLIENT_ID = "668455931031-burt9863pi64rshdlmgenejnj27ep0d0.apps.googleusercontent.com"
    }
}

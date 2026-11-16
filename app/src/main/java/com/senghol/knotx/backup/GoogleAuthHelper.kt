package com.senghol.knotx.backup

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes

object GoogleAuthHelper {

    private const val WEB_CLIENT_ID = "184102817231-t6825enj0gf559v4q41bv7ivs3a7lnbl.apps.googleusercontent.com"
    private const val TAG = "GoogleAuth"

    // ─── SIGN IN via Credential Manager ──────────────────────────────────────

    private fun buildSignInRequest(filterByAuthorizedAccounts: Boolean): GetCredentialRequest {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
    }

    suspend fun signIn(activity: Activity): GoogleIdTokenCredential? {
        val credentialManager = CredentialManager.create(activity)
        return try {
            val result = credentialManager.getCredential(
                activity,
                buildSignInRequest(filterByAuthorizedAccounts = true)
            )
            extractCredential(result.credential)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "First attempt: ${e.message}")
            try {
                val result = credentialManager.getCredential(
                    activity,
                    buildSignInRequest(filterByAuthorizedAccounts = false)
                )
                extractCredential(result.credential)
            } catch (e2: GetCredentialException) {
                Log.e(TAG, "Second attempt: ${e2.message}")
                null
            }
        }
    }

    private fun extractCredential(credential: androidx.credentials.Credential): GoogleIdTokenCredential? {
        return if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) GoogleIdTokenCredential.createFrom(credential.data) else null
    }

    // ─── AUTHORIZE Drive scope ────────────────────────────────────────────────

    fun requestDriveAuthorization(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onAlreadyGranted: () -> Unit
    ) {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_FILE)))
            .build()

        Identity.getAuthorizationClient(activity)
            .authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    launcher.launch(
                        IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build()
                    )
                } else {
                    onAlreadyGranted()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Drive authorization failed: ${e.message}")
                onAlreadyGranted()
            }
    }

    fun handleAuthorizationResult(
        context: Context,
        data: android.content.Intent?
    ): AuthorizationResult? {
        return try {
            Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)
        } catch (e: Exception) {
            Log.e(TAG, "handleAuthorizationResult: ${e.message}")
            null
        }
    }

    // ─── EMAIL STORAGE ────────────────────────────────────────────────────────

    fun saveEmail(context: Context, email: String) {
        context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
            .edit().putString("signed_in_account", email).apply()
    }

    fun getSavedEmail(context: Context): String? {
        return context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
            .getString("signed_in_account", null)
    }

    fun clearEmail(context: Context) {
        context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
            .edit().remove("signed_in_account").apply()
    }

    fun signOut(context: Context, onDone: () -> Unit) {
        clearEmail(context)
        // Credential Manager doesn't have a signOut — just clear local state
        onDone()
    }

    // ─── DRIVE CREDENTIAL ─────────────────────────────────────────────────────

    fun getDriveCredential(context: Context, accountName: String): GoogleAccountCredential {
        return GoogleAccountCredential
            .usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
            .also { it.selectedAccountName = accountName }
    }
}
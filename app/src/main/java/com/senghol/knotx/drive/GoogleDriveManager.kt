package com.senghol.knotx.drive

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GoogleDriveManager {

    var driveApi: DriveApi? = null
        private set

    suspend fun connect(context: Context, account: GoogleSignInAccount) =
        withContext(Dispatchers.IO) {

            // Modern scope string (NO DriveScopes)
            val scope = "oauth2:https://www.googleapis.com/auth/drive.file"

            try {
                val token = GoogleAuthUtil.getToken(
                    context,
                    account.account ?: throw IllegalStateException("GoogleSignInAccount.account is null"),
                    scope

                )

                driveApi = DriveService.create(token)

            } catch (e: UserRecoverableAuthException) {
                throw e  // handle in Activity
            }
        }
}

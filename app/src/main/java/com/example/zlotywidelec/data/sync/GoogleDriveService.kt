package com.example.zlotywidelec.data.sync

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Service to manage Google Drive authentication and API access.
 */
class GoogleDriveService(val context: Context) {

    private val _userAccount = MutableStateFlow<GoogleSignInAccount?>(GoogleSignIn.getLastSignedInAccount(context))
    val userAccount: StateFlow<GoogleSignInAccount?> = _userAccount

    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun updateAccount(account: GoogleSignInAccount?) {
        _userAccount.value = account
    }

    fun getDriveService(): Drive? {
        val account = _userAccount.value ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE)
        ).setSelectedAccount(account.account)

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Zloty Widelec")
            .build()
    }

    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            _userAccount.value = null
            onComplete()
        }
    }
}

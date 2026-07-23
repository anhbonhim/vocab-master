package com.nhimz.vocabmaster.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class AppUser(
    val uid: String,
    val displayName: String?,
    val email: String?
)

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)
    
    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = if (user != null) {
                AppUser(user.uid, user.displayName, user.email)
            } else null
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<AppUser> {
        return try {
            val webClientId = "170306776528-cl98eh785k2s5cto0nmd0uudkjo9lkji.apps.googleusercontent.com" 
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val localCredentialManager = CredentialManager.create(activityContext)
            val result = localCredentialManager.getCredential(activityContext, request)
            handleSignIn(result)
        } catch (e: Exception) {
            Log.e("AuthManager", "Google Sign In Failed", e)
            Result.failure(e)
        }
    }

    private suspend fun handleSignIn(result: GetCredentialResponse): Result<AppUser> {
        val credential = result.credential
        if (credential is GoogleIdTokenCredential) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                
                val user = authResult.user
                return if (user != null) {
                    Result.success(AppUser(user.uid, user.displayName, user.email))
                } else {
                    Result.failure(Exception("Firebase auth returned null user"))
                }
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("AuthManager", "Received an invalid google id token response", e)
                return Result.failure(e)
            }
        } else {
            return Result.failure(Exception("Unexpected type of credential"))
        }
    }

    suspend fun getIdToken(): String? {
        return try {
            val user = auth.currentUser ?: return null
            val result = user.getIdToken(false).await()
            result.token
        } catch (e: Exception) {
            Log.e("AuthManager", "Error getting ID token", e)
            null
        }
    }

    fun signOut() {
        auth.signOut()
    }
}

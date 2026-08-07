package com.raysix.fitns.core.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.raysix.fitns.BuildConfig
import com.raysix.fitns.domain.model.GoogleAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthController @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val isConfigured: Boolean
        get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    private val client by lazy {
        if (!isConfigured) {
            null
        } else {
            GoogleSignIn.getClient(
                context,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .requestEmail()
                    .requestProfile()
                    .build()
            )
        }
    }

    fun createSignInIntent(): Intent? = client?.signInIntent

    fun handleSignInResult(data: Intent?): GoogleAccount? {
        if (!isConfigured || data == null) return null
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            GoogleAccount(
                email = account.email ?: "",
                displayName = account.displayName ?: "",
                photoUrl = account.photoUrl?.toString()
            )
        } catch (e: ApiException) {
            null
        }
    }

    fun signOut() {
        client?.signOut()
    }
}

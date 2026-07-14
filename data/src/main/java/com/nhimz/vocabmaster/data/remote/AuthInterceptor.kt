package com.nhimz.vocabmaster.data.remote

import com.nhimz.vocabmaster.data.auth.AuthManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val authManager: AuthManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Blockingly get the token (OkHttp interceptors run on background threads)
        val token = runBlocking { authManager.getIdToken() }
        
        return if (token != null) {
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        } else {
            // Proceed without token if not logged in
            chain.proceed(originalRequest)
        }
    }
}

package com.example.explorandes.api

import android.content.Context
import android.util.Log
import com.example.explorandes.utils.SessionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    //private const val BASE_URL = "http://10.0.2.2:8080/"
    private const val BASE_URL = "http://172.20.10.6:8080/"

    // Make apiService directly accessible
    lateinit var apiService: ApiService
    lateinit var retrofit: Retrofit
    private lateinit var sessionManager: SessionManager

    fun init(context: Context) {
        Log.d("ApiClient", "Initializing ApiClient")
        sessionManager = SessionManager(context)

        // Add logging interceptor
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Longer timeout for slower connections
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor) // Add logging interceptor
            .addInterceptor(AuthInterceptor(sessionManager))
            .retryOnConnectionFailure(true) // Add retry capability
            .build()

        // Create a custom Gson instance that doesn't require @Expose annotations
        val gson: Gson = GsonBuilder()
            .setLenient() // Be lenient with malformed JSON
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        apiService = retrofit.create(ApiService::class.java)
        Log.d("ApiClient", "ApiClient initialized successfully")
    }

    // Add an interceptor to add Authorization header for authenticated requests
    class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest: Request = chain.request()

            // If there's no token, just proceed with the original request
            val token = sessionManager.getToken() ?: return chain.proceed(originalRequest)

            // Add the authorization header for authenticated requests
            val authorizedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()

            return chain.proceed(authorizedRequest)
        }
    }
}
package com.example.progfront.data.remote

import android.content.Context
import com.example.progfront.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"
    //
    //
    //private const val BASE_URL = "http://192.168.87.1:8080/"

    private var apiService: ApiService? = null

    fun initialize(context: Context) {
        if (apiService == null) {
            val tokenManager = TokenManager(context.applicationContext)

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = AuthInterceptor(tokenManager)

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }
    }

    val instance: ApiService
        get() = apiService ?: throw IllegalStateException(
            "RetrofitClient must be initialized with context before use. Call RetrofitClient.initialize(context) first."
        )
}

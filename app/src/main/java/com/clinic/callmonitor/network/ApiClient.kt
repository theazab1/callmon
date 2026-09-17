package com.clinic.callmonitor.network

import com.clinic.callmonitor.CallMonitorApp
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val authInterceptor = Interceptor { chain ->
        val token = CallMonitorApp.prefs.getString(CallMonitorApp.KEY_API_TOKEN, "") ?: ""
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        chain.proceed(request)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)   // رفع الصوت محتاج وقت أطول
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val service: ApiService by lazy {
        val baseUrl = CallMonitorApp.prefs.getString(CallMonitorApp.KEY_SERVER_URL, "https://callmon.wellio.org/api/")!!
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

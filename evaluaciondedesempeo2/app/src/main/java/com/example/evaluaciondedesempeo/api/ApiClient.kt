package com.example.evaluaciondedesempeo.api

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://192.168.0.117:8000/"
    private var prefs: SharedPreferences? = null

    // Función para inicializar las SharedPreferences una sola vez
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("AUTH_PREFS", Context.MODE_PRIVATE)
        }
    }

    // Interceptor que añade el Token a cada petición automáticamente
    private val authInterceptor = Interceptor { chain ->
        val token = prefs?.getString("TOKEN", null)
        val requestBuilder = chain.request().newBuilder()
        
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        
        val response = chain.proceed(requestBuilder.build())
        
        // Si el servidor responde 401 (No autorizado), significa que el token expiró
        if (response.code == 401) {
            // Aquí se podría implementar una lógica para redirigir al login
        }
        
        response
    }

    private val logger = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor) // <--- Agregamos el interceptor de seguridad
        .addInterceptor(logger)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

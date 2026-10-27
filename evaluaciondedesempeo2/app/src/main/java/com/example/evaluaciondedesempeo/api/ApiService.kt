package com.example.evaluaciondedesempeo.api

import com.example.evaluaciondedesempeo.models.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @POST("api/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/usuarios")
    fun registrarUsuario(@Body request: UsuarioCreate): Call<VerificationResponse>

    @POST("api/verificar")
    fun verificarCuenta(@Body request: VerifyRequest): Call<VerificationResponse>

    @GET("api/empleados")
    fun getEmployees(@Header("Authorization") token: String): Call<List<User>>
    
    @DELETE("api/usuarios/{user_id}")
    fun deleteEmployee(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int
    ): Call<Void>

    @POST("api/evaluaciones")
    fun createEvaluation(
        @Header("Authorization") token: String,
        @Body evaluation: EvaluationRequest
    ): Call<EvaluationResponse>

    @GET("api/evaluaciones/resumen/{usuario_id}")
    fun getSummary360(
        @Header("Authorization") token: String,
        @Path("usuario_id") userId: Int
    ): Call<Summary360>

    @GET("api/reporte/{usuario_id}")
    @Streaming
    fun downloadReport(
        @Header("Authorization") token: String,
        @Path("usuario_id") userId: Int
    ): Call<ResponseBody>
}

data class LoginRequest(val email: String, val password: String)

data class UsuarioCreate(
    val nombre: String,
    val email: String,
    val password: String,
    val rol: String = "EMPLOYEE"
)

data class VerifyRequest(
    val email: String,
    val codigo: String
)

data class VerificationResponse(
    val status: String,
    val message: String,
    val email: String? = null
)

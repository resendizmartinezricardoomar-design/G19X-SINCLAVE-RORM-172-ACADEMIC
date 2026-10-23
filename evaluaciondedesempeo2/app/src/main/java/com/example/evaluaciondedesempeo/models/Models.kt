package com.example.evaluaciondedesempeo.models

data class User(
    val id: Int = -1,
    val nombre: String? = "Usuario",
    val rol: String? = "EMPLOYEE",
    val email: String? = null,
    val score: Float? = 0f
)

data class LoginResponse(
    val access_token: String? = null,
    val user: User? = null
)

data class EvaluationRequest(
    val evaluado_id: Int,
    val evaluador_id: Int,
    val tipo_evaluacion: String,
    val relacion_evaluador: String,
    val obj_cumplimiento: Float,
    val puntualidad_asistencia: Float,
    val calidad_trabajo: Float,
    val productividad: Float,
    val trabajo_equipo: Float,
    val conocimiento_habilidad: Float,
    val comunicacion: Float,
    val adaptabilidad: Float,
    val iniciativa: Float,
    val responsabilidad: Float,
    val actitud_profesional: Float,
    val desarrollo_profesional: Float,
    val comentarios_texto: String
)

data class EvaluationResponse(
    val status: String? = null,
    val message: String? = null
)

data class Summary360(
    val labels: List<String>? = emptyList(),
    val scores: List<Float>? = emptyList(),
    val kpis: Map<String, Float>? = emptyMap(),
    val analisis_ia: AnalysisResult? = null
)

data class AnalysisResult(
    val sentimiento: String? = "Neutral",
    val recomendaciones: List<String>? = emptyList()
)

package com.example.evaluaciondedesempeo.models

data class User(
    val id: Int,
    val nombre: String,
    val rol: String,
    val email: String? = null,
    val alerta: Boolean = false
)

data class LoginResponse(
    val access_token: String? = null, // Hecho opcional por si el backend no lo envía
    val user: User
)

data class EvaluationRequest(
    val evaluado_id: Int,
    val evaluador_id: Int,
    val tipo_evaluacion: String,
    val relacion_evaluador: String,
    
    // Las 12 métricas (0-100)
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
    val evaluacion_id: Int? = null,
    val puntuacion: Float? = null,
    val alerta_critica: Boolean = false,
    // Compatibilidad con versión previa
    val id: Int? = null,
    val evaluado_id: Int? = null,
    val puntuacion_promedio: Float? = null,
    val comentarios_texto: String? = null,
    val fecha: String? = null
)

data class AnalysisRequest(
    val feedback: String,
    val score: Float
)

data class AnalysisResult(
    val sentimiento: String,
    val areas_oportunidad: List<String>,
    val recomendaciones: List<String>,
    val score_analisis: Float
)

data class Summary360(
    val labels: List<String>,
    val scores: List<Float>,
    val kpis: Map<String, Float>? = null,
    val analisis_ia: AnalysisResult? = null
)

package com.example.evaluaciondedesempeo.model

enum class TipoEvaluacion {
    EVAL_90, EVAL_180, EVAL_360
}

data class Competencia(
    val id: String,
    val nombre: String,
    val descripcion: String,
    var calificacion: Float = 0f
)

data class Indicador(
    val id: String,
    val nombre: String,
    val meta: Float,
    var actual: Float
)

data class Objetivo(
    val id: String,
    val descripcion: String,
    var porcentajeCumplimiento: Float
)

data class Evaluacion(
    val id: String,
    val evaluadoId: String,
    val evaluadorId: String,
    val tipo: TipoEvaluacion,
    val competencias: List<Competencia>,
    val indicadores: List<Indicador>,
    val objetivos: List<Objetivo>,
    var retroalimentacionIA: String? = null
)

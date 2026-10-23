package com.example.evaluaciondedesempeo.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evaluaciondedesempeo.model.Evaluacion
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

class EvaluationViewModel : ViewModel() {

    private val _evaluations = MutableLiveData<List<Evaluacion>>(emptyList())
    val evaluations: LiveData<List<Evaluacion>> = _evaluations

    // Nota: El usuario debe proporcionar una API KEY válida de Google AI Studio
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "TU_API_KEY_AQUI"
    )

    fun getAIRecommendations(evaluacion: Evaluacion) {
        viewModelScope.launch {
            val prompt = "Genera recomendaciones de desarrollo profesional para un empleado con los siguientes resultados de evaluación: Competencias: ${evaluacion.competencias}, Indicadores: ${evaluacion.indicadores}. Tipo de evaluación: ${evaluacion.tipo}"
            try {
                val response = generativeModel.generateContent(prompt)
                evaluacion.retroalimentacionIA = response.text
                
                val current = _evaluations.value?.toMutableList() ?: mutableListOf()
                val index = current.indexOfFirst { it.id == evaluacion.id }
                if (index != -1) {
                    current[index] = evaluacion
                    _evaluations.value = current
                }
            } catch (e: Exception) {
                evaluacion.retroalimentacionIA = "Error al generar recomendaciones: ${e.message}"
            }
        }
    }

    fun addEvaluation(evaluacion: Evaluacion) {
        val currentList = _evaluations.value ?: emptyList()
        _evaluations.value = currentList + evaluacion
    }
}

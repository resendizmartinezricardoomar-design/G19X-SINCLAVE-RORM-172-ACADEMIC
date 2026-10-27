package com.example.evaluaciondedesempeo

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.evaluaciondedesempeo.api.ApiClient
import com.example.evaluaciondedesempeo.databinding.ActivityEvaluationBinding
import com.example.evaluaciondedesempeo.databinding.ItemAuditQuestionBinding
import com.example.evaluaciondedesempeo.models.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EvaluationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEvaluationBinding
    private var userId: Int = -1
    private var authToken: String = ""
    private var employeeList: List<User> = emptyList()
    
    private val selectedScores = mutableMapOf<String, Int>()

    private val metrics = listOf(
        Pair("obj_cumplimiento", "1. Objetivos Estratégicos"),
        Pair("puntualidad_asistencia", "2. Puntualidad y Asistencia"),
        Pair("calidad_trabajo", "3. Calidad de Entregables"),
        Pair("productividad", "4. Productividad y Ritmo"),
        Pair("trabajo_equipo", "5. Colaboración en Equipo"),
        Pair("conocimiento_habilidad", "6. Conocimiento Técnico"),
        Pair("comunicacion", "7. Comunicación Asertiva"),
        Pair("adaptabilidad", "8. Adaptabilidad al Cambio"),
        Pair("iniciativa", "9. Iniciativa y Proactividad"),
        Pair("responsabilidad", "10. Responsabilidad"),
        Pair("actitud_profesional", "11. Actitud Profesional"),
        Pair("desarrollo_profesional", "12. Desarrollo y Crecimiento")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityEvaluationBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val prefs = getSharedPreferences("AUTH_PREFS", Context.MODE_PRIVATE)
            userId = intent.getIntExtra("USER_ID", prefs.getInt("USER_ID", -1))
            authToken = prefs.getString("AUTH_TOKEN", "") ?: ""

            setupQuestions()
            loadEmployees()

            binding.btnSubmit.setOnClickListener {
                submitEvaluation()
            }
        } catch (e: Exception) {
            Log.e("INSIGHT_ERROR", "Error al iniciar Evaluación", e)
            finish()
        }
    }

    private fun setupQuestions() {
        metrics.forEach { metric ->
            val itemBinding = ItemAuditQuestionBinding.inflate(LayoutInflater.from(this), binding.llQuestionsContainer, false)
            itemBinding.tvQuestionLabel.text = metric.second
            
            val buttons = listOf(itemBinding.btn1, itemBinding.btn2, itemBinding.btn3, itemBinding.btn4, itemBinding.btn5)
            
            buttons.forEachIndexed { index, button ->
                button.setOnClickListener {
                    val score = index + 1
                    selectedScores[metric.first] = score
                    
                    buttons.forEach { b -> 
                        b.setBackgroundColor(Color.WHITE)
                        b.setTextColor(Color.parseColor("#64748B"))
                    }
                    button.setBackgroundColor(Color.parseColor("#4F46E5"))
                    button.setTextColor(Color.WHITE)
                }
            }
            binding.llQuestionsContainer.addView(itemBinding.root)
        }
    }

    private fun loadEmployees() {
        if (authToken.isEmpty()) return
        val tokenHeader = "Bearer $authToken"
        ApiClient.apiService.getEmployees(tokenHeader).enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful) {
                    employeeList = (response.body() ?: emptyList()).filter { it.id != userId }
                    val names = employeeList.map { it.nombre ?: "Sin nombre" }
                    val adapter = ArrayAdapter(this@EvaluationActivity, android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerEmpleado.adapter = adapter
                }
            }
            override fun onFailure(call: Call<List<User>>, t: Throwable) {}
        })
    }

    private fun submitEvaluation() {
        if (employeeList.isEmpty() || binding.spinnerEmpleado.selectedItemPosition == -1) {
            Toast.makeText(this, "Selecciona a un colaborador", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedScores.size < 12) {
            Toast.makeText(this, "Faltan indicadores por calificar", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val evaluadoId = employeeList[binding.spinnerEmpleado.selectedItemPosition].id
            
            val request = EvaluationRequest(
                evaluado_id = evaluadoId,
                evaluador_id = userId,
                tipo_evaluacion = "Auditoría Premium",
                relacion_evaluador = "Administración",
                obj_cumplimiento = (selectedScores["obj_cumplimiento"] ?: 0).toFloat() * 20,
                puntualidad_asistencia = (selectedScores["puntualidad_asistencia"] ?: 0).toFloat() * 20,
                calidad_trabajo = (selectedScores["calidad_trabajo"] ?: 0).toFloat() * 20,
                productividad = (selectedScores["productividad"] ?: 0).toFloat() * 20,
                trabajo_equipo = (selectedScores["trabajo_equipo"] ?: 0).toFloat() * 20,
                conocimiento_habilidad = (selectedScores["conocimiento_habilidad"] ?: 0).toFloat() * 20,
                comunicacion = (selectedScores["comunicacion"] ?: 0).toFloat() * 20,
                adaptabilidad = (selectedScores["adaptabilidad"] ?: 0).toFloat() * 20,
                iniciativa = (selectedScores["iniciativa"] ?: 0).toFloat() * 20,
                responsabilidad = (selectedScores["responsabilidad"] ?: 0).toFloat() * 20,
                actitud_profesional = (selectedScores["actitud_profesional"] ?: 0).toFloat() * 20,
                desarrollo_profesional = (selectedScores["desarrollo_profesional"] ?: 0).toFloat() * 20,
                comentarios_texto = binding.etComments.text.toString()
            )

            binding.btnSubmit.isEnabled = false
            val tokenHeader = "Bearer $authToken"
            ApiClient.apiService.createEvaluation(tokenHeader, request).enqueue(object : Callback<EvaluationResponse> {
                override fun onResponse(call: Call<EvaluationResponse>, response: Response<EvaluationResponse>) {
                    if (isFinishing || isDestroyed) return
                    if (response.isSuccessful) {
                        Toast.makeText(this@EvaluationActivity, "✅ AUDITORÍA PUBLICADA", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        binding.btnSubmit.isEnabled = true
                    }
                }
                override fun onFailure(call: Call<EvaluationResponse>, t: Throwable) {
                    binding.btnSubmit.isEnabled = true
                }
            })
        } catch (e: Exception) {
            Log.e("INSIGHT_ERROR", "Error al enviar evaluación", e)
        }
    }
}

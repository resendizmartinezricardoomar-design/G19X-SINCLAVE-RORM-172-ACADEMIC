package com.example.evaluaciondedesempeo

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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
        binding = ActivityEvaluationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)

        setupQuestions()
        loadEmployees()

        binding.btnSubmit.setOnClickListener {
            submitEvaluation()
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
                    button.setBackgroundColor(Color.parseColor("#1E3A8A"))
                    button.setTextColor(Color.WHITE)
                }
            }
            binding.llQuestionsContainer.addView(itemBinding.root)
        }
    }

    private fun loadEmployees() {
        ApiClient.apiService.getEmployees().enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (response.isSuccessful) {
                    employeeList = (response.body() ?: emptyList()).filter { it.id != userId }
                    if (employeeList.isEmpty()) {
                        Toast.makeText(this@EvaluationActivity, "No hay empleados registrados para evaluar", Toast.LENGTH_LONG).show()
                    }
                    val names = employeeList.map { it.nombre }
                    val adapter = ArrayAdapter(this@EvaluationActivity, android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerEmpleado.adapter = adapter
                }
            }
            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                Toast.makeText(this@EvaluationActivity, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun submitEvaluation() {
        // VALIDACIÓN 1: Selección de empleado
        if (employeeList.isEmpty() || binding.spinnerEmpleado.selectedItemPosition == -1) {
            Toast.makeText(this, "❌ Error: Primero selecciona a un colaborador", Toast.LENGTH_SHORT).show()
            return
        }

        // VALIDACIÓN 2: Los 12 puntos
        if (selectedScores.size < 12) {
            val faltantes = 12 - selectedScores.size
            Toast.makeText(this, "⚠️ Faltan $faltantes indicadores por calificar", Toast.LENGTH_SHORT).show()
            return
        }

        val evaluadoId = employeeList[binding.spinnerEmpleado.selectedItemPosition].id
        
        val request = EvaluationRequest(
            evaluado_id = evaluadoId,
            evaluador_id = userId,
            tipo_evaluacion = "Auditoría Móvil Premium",
            relacion_evaluador = "Administración",
            obj_cumplimiento = selectedScores["obj_cumplimiento"]!!.toFloat() * 20,
            puntualidad_asistencia = selectedScores["puntualidad_asistencia"]!!.toFloat() * 20,
            calidad_trabajo = selectedScores["calidad_trabajo"]!!.toFloat() * 20,
            productividad = selectedScores["productividad"]!!.toFloat() * 20,
            trabajo_equipo = selectedScores["trabajo_equipo"]!!.toFloat() * 20,
            conocimiento_habilidad = selectedScores["conocimiento_habilidad"]!!.toFloat() * 20,
            comunicacion = selectedScores["comunicacion"]!!.toFloat() * 20,
            adaptabilidad = selectedScores["adaptabilidad"]!!.toFloat() * 20,
            iniciativa = selectedScores["iniciativa"]!!.toFloat() * 20,
            responsabilidad = selectedScores["responsabilidad"]!!.toFloat() * 20,
            actitud_profesional = selectedScores["actitud_profesional"]!!.toFloat() * 20,
            desarrollo_profesional = selectedScores["desarrollo_profesional"]!!.toFloat() * 20,
            comentarios_texto = binding.etComments.text.toString()
        )

        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = "PUBLICANDO..."

        ApiClient.apiService.createEvaluation(request).enqueue(object : Callback<EvaluationResponse> {
            override fun onResponse(call: Call<EvaluationResponse>, response: Response<EvaluationResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@EvaluationActivity, "✅ AUDITORÍA PUBLICADA", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    binding.btnSubmit.isEnabled = true
                    binding.btnSubmit.text = "PUBLICAR AUDITORÍA"
                    Toast.makeText(this@EvaluationActivity, "Error del servidor: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<EvaluationResponse>, t: Throwable) {
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = "PUBLICAR AUDITORÍA"
                Toast.makeText(this@EvaluationActivity, "Fallo de red: Verifique su conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

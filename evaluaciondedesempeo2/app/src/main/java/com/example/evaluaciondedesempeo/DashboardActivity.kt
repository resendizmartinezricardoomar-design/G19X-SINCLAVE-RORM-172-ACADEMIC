package com.example.evaluaciondedesempeo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.evaluaciondedesempeo.api.ApiClient
import com.example.evaluaciondedesempeo.databinding.ActivityDashboardBinding
import com.example.evaluaciondedesempeo.models.Summary360
import com.example.evaluaciondedesempeo.models.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : AppCompatActivity() {
    private var _binding: ActivityDashboardBinding? = null
    private val binding get() = _binding!!
    
    private var userId: Int = -1
    private var userRole: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            _binding = ActivityDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val prefs = getSharedPreferences("AUTH_PREFS", Context.MODE_PRIVATE)
            userId = intent.getIntExtra("USER_ID", prefs.getInt("USER_ID", -1))
            userRole = intent.getStringExtra("USER_ROLE") ?: prefs.getString("USER_ROLE", "EMPLOYEE") ?: "EMPLOYEE"
            val userName = intent.getStringExtra("USER_NAME") ?: prefs.getString("USER_NAME", "Usuario") ?: "Usuario"

            binding.tvUserName.text = "Hola, $userName"

            setupUIByRole()
            setupRecyclerView()

            binding.btnRegisterWorker.setOnClickListener {
                startActivity(Intent(this, RegisterActivity::class.java))
            }

            binding.btnNewEvaluation.setOnClickListener {
                val intent = Intent(this, EvaluationActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }

            binding.btnViewMyResults.setOnClickListener {
                val intent = Intent(this, ResultsActivity::class.java)
                intent.putExtra("USER_ID", userId)
                intent.putExtra("USER_NAME", userName)
                startActivity(intent)
            }

            binding.btnLogout.setOnClickListener {
                getSharedPreferences("AUTH_PREFS", Context.MODE_PRIVATE).edit().clear().apply()
                val loginIntent = Intent(this, LoginActivity::class.java)
                loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(loginIntent)
                finish()
            }
        } catch (e: Exception) {
            Log.e("INSIGHT_FATAL", "Error en Dashboard", e)
        }
    }

    private fun setupUIByRole() {
        try {
            val isAdmin = userRole.equals("ADMIN", ignoreCase = true)
            binding.layoutAdminPanel.visibility = if (isAdmin) View.VISIBLE else View.GONE
            binding.tvSectionTitle.text = if (isAdmin) "GESTIÓN ESTRATÉGICA DE TALENTO" else "MI EQUIPO DE TRABAJO"
        } catch (e: Exception) {}
    }

    private fun setupRecyclerView() {
        try {
            val isAdmin = userRole.equals("ADMIN", ignoreCase = true)
            val adapter = EmployeeAdapter(emptyList(), isAdmin,
                onEvaluateClick = { u ->
                    val intent = Intent(this, EvaluationActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    intent.putExtra("EVALUADO_ID", u.id)
                    startActivity(intent)
                },
                onViewRadarClick = { u ->
                    val intent = Intent(this, ResultsActivity::class.java)
                    intent.putExtra("USER_ID", u.id)
                    intent.putExtra("USER_NAME", u.nombre)
                    startActivity(intent)
                },
                onDeleteClick = { u ->
                    confirmDelete(u)
                }
            )
            binding.rvEmployees.layoutManager = LinearLayoutManager(this)
            binding.rvEmployees.adapter = adapter
        } catch (e: Exception) {}
    }

    private fun confirmDelete(u: User) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar")
            .setMessage("¿Deseas eliminar a ${u.nombre}?")
            .setPositiveButton("SÍ") { _, _ ->
                // El token ya se envía automáticamente por el Interceptor
                ApiClient.apiService.deleteEmployee(u.id).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) loadEmployees()
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {}
                })
            }.setNegativeButton("NO", null).show()
    }

    private fun loadEmployees() {
        // Petición limpia sin parámetros manuales de Token
        ApiClient.apiService.getEmployees().enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (isFinishing || isDestroyed || _binding == null) return
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    val filtered = list.filter { it.id != userId && it.nombre != null }
                    (binding.rvEmployees.adapter as? EmployeeAdapter)?.updateData(filtered)
                }
            }
            override fun onFailure(call: Call<List<User>>, t: Throwable) {}
        })
    }

    private fun loadMySummary() {
        if (userId == -1) return
        ApiClient.apiService.getSummary360(userId).enqueue(object : Callback<Summary360> {
            override fun onResponse(call: Call<Summary360>, response: Response<Summary360>) {
                if (isFinishing || isDestroyed || _binding == null) return
                if (response.isSuccessful && response.body() != null) {
                    val summary = response.body()!!
                    try {
                        val scores = summary.scores?.filterNotNull() ?: emptyList()
                        val avg = if (scores.isNotEmpty()) scores.average() else 0.0
                        binding.tvKpiScore.text = String.format("%.1f", avg)
                        binding.tvKpiEvals.text = "${scores.size}"
                        binding.tvKpiAlerts.text = if (avg < 60 && scores.isNotEmpty()) "01" else "00"
                    } catch (e: Exception) {
                        binding.tvKpiScore.text = "0.0"
                        binding.tvKpiEvals.text = "0"
                    }
                }
            }
            override fun onFailure(call: Call<Summary360>, t: Throwable) {}
        })
    }

    override fun onResume() {
        super.onResume()
        loadEmployees()
        loadMySummary()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

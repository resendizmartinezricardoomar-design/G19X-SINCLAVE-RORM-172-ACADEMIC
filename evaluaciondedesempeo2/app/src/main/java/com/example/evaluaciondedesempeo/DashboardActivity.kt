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
    private lateinit var binding: ActivityDashboardBinding
    private var userId: Int = -1
    private var userRole: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getIntExtra("USER_ID", -1)
        userRole = intent.getStringExtra("USER_ROLE") ?: "EMPLOYEE"
        val userName = intent.getStringExtra("USER_NAME") ?: "Usuario"

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
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupUIByRole() {
        val isAdmin = userRole.equals("ADMIN", ignoreCase = true)
        
        if (isAdmin) {
            binding.layoutAdminPanel.visibility = View.VISIBLE
            binding.btnRegisterWorker.visibility = View.VISIBLE
            binding.btnNewEvaluation.visibility = View.VISIBLE
            binding.tvSectionTitle.text = "GESTIÓN DE TALENTO CLAVE"
        } else {
            binding.layoutAdminPanel.visibility = View.GONE
            binding.btnRegisterWorker.visibility = View.GONE
            binding.btnNewEvaluation.visibility = View.GONE
            binding.tvSectionTitle.text = "MI EQUIPO DE TRABAJO"
        }
        
        binding.btnViewMyResults.visibility = View.VISIBLE
        binding.rvEmployees.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
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
        loadEmployees()
    }

    private fun confirmDelete(u: User) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar")
            .setMessage("¿Eliminar a ${u.nombre}?")
            .setPositiveButton("SÍ") { _, _ ->
                ApiClient.apiService.deleteEmployee(u.id).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) { 
                        if (isFinishing || isDestroyed) return
                        loadEmployees() 
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {}
                })
            }.setNegativeButton("NO", null).show()
    }

    private fun loadEmployees() {
        ApiClient.apiService.getEmployees().enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    // Filtro de seguridad para evitar nulos
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
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body() != null) {
                    val summary = response.body()!!
                    try {
                        // Verificación de lista de scores para evitar nulos y promediar con seguridad
                        val scores = summary.scores?.filterNotNull() ?: emptyList()
                        val avg = if (scores.isNotEmpty()) scores.average() else 0.0
                        
                        binding.tvKpiScore.text = String.format("%.1f", avg)
                        binding.tvKpiEvals.text = "${scores.size}"
                        binding.tvKpiAlerts.text = if (avg < 60 && scores.isNotEmpty()) "01" else "00"
                    } catch (e: Exception) {
                        Log.e("DASHBOARD", "Error en KPIs", e)
                        binding.tvKpiScore.text = "0.0"
                        binding.tvKpiEvals.text = "0"
                    }
                }
            }
            override fun onFailure(call: Call<Summary360>, t: Throwable) {
                // Falla silenciosa para evitar crash por red
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadEmployees()
        loadMySummary()
    }
}

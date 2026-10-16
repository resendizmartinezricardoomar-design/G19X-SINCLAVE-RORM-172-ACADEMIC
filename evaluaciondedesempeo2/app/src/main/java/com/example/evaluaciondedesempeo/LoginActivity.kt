package com.example.evaluaciondedesempeo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.evaluaciondedesempeo.api.ApiClient
import com.example.evaluaciondedesempeo.api.LoginRequest
import com.example.evaluaciondedesempeo.databinding.ActivityLoginBinding
import com.example.evaluaciondedesempeo.models.LoginResponse
import com.example.evaluaciondedesempeo.models.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var selectedUser: User? = null
    private var allUsers: List<User> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        // FORZAR MODO CLARO
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("AUTH_PREFS", Context.MODE_PRIVATE)
        if (prefs.contains("USER_ID")) {
            navigateToDashboard(
                prefs.getInt("USER_ID", -1),
                prefs.getString("USER_NAME", "") ?: "",
                prefs.getString("USER_ROLE", "EMPLOYEE") ?: "EMPLOYEE"
            )
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                performLogin(email, password)
            } else {
                Toast.makeText(this, "Completa los datos", Toast.LENGTH_SHORT).show()
            }
        }

        loadUsersForQuickLogin()

        binding.btnQuickEntry.setOnClickListener {
            selectedUser?.let { user ->
                performLogin(user.email ?: user.nombre, "12345")
            }
        }
    }

    private fun loadUsersForQuickLogin() {
        ApiClient.apiService.getEmployees().enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (response.isSuccessful) {
                    val rawList = response.body() ?: emptyList()
                    
                    // --- FILTRO ESTRICTO: Quitamos al Administrador del Simulador ---
                    allUsers = rawList.filter { 
                        val role = it.rol.uppercase().trim()
                        val email = it.email?.lowercase()?.trim() ?: ""
                        val name = it.nombre.uppercase()
                        
                        role != "ADMIN" && 
                        email != "resendiz@gmail.com" && 
                        !name.contains("RICARDO")
                    }
                    
                    val displayNames = allUsers.map { "${it.nombre} (${it.rol})" }
                    val adapter = ArrayAdapter(this@LoginActivity, android.R.layout.simple_spinner_item, displayNames)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerQuickLogin.adapter = adapter
                    
                    binding.spinnerQuickLogin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                            if (pos < allUsers.size) {
                                selectedUser = allUsers[pos]
                            }
                        }
                        override fun onNothingSelected(p0: AdapterView<*>?) {}
                    }
                }
            }
            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                Log.e("API", "Error: ${t.message}")
            }
        })
    }

    private fun performLogin(email: String, pass: String) {
        binding.btnLogin.isEnabled = false
        ApiClient.apiService.login(LoginRequest(email, pass)).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                binding.btnLogin.isEnabled = true
                if (response.isSuccessful) {
                    response.body()?.user?.let { user ->
                        saveSession(user)
                        navigateToDashboard(user.id, user.nombre, user.rol)
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                binding.btnLogin.isEnabled = true
                Toast.makeText(this@LoginActivity, "Error: El servidor no responde", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveSession(user: User) {
        getSharedPreferences("AUTH_PREFS", Context.MODE_PRIVATE).edit().apply {
            putInt("USER_ID", user.id)
            putString("USER_NAME", user.nombre)
            putString("USER_ROLE", user.rol)
            apply()
        }
    }

    private fun navigateToDashboard(id: Int, name: String, role: String) {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            putExtra("USER_ID", id)
            putExtra("USER_NAME", name)
            putExtra("USER_ROLE", role)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

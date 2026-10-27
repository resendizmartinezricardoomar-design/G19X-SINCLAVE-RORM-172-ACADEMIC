package com.example.evaluaciondedesempeo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.evaluaciondedesempeo.api.ApiClient
import com.example.evaluaciondedesempeo.api.LoginRequest
import com.example.evaluaciondedesempeo.api.VerifyRequest
import com.example.evaluaciondedesempeo.api.VerificationResponse
import com.example.evaluaciondedesempeo.databinding.ActivityLoginBinding
import com.example.evaluaciondedesempeo.models.LoginResponse
import com.example.evaluaciondedesempeo.models.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // Inicializar ApiClient con el contexto para que pueda leer SharedPreferences
        ApiClient.init(this)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("AUTH_PREFS", Context.MODE_PRIVATE)
        if (prefs.contains("USER_ID")) {
            navigateToDashboard(
                prefs.getInt("USER_ID", -1),
                prefs.getString("USER_NAME", "") ?: "",
                prefs.getString("USER_ROLE", "EMPLOYEE") ?: "EMPLOYEE"
            )
            return
        }

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

        binding.btnVerifyAccount.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val code = binding.etVerifyCode.text.toString().trim()
            if (email.isNotEmpty() && code.isNotEmpty()) {
                verifyAccount(email, code)
            } else {
                Toast.makeText(this, "Ingresa email y código", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyAccount(email: String, code: String) {
        ApiClient.apiService.verificarCuenta(VerifyRequest(email, code)).enqueue(object : Callback<VerificationResponse> {
            override fun onResponse(call: Call<VerificationResponse>, response: Response<VerificationResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful) {
                    Toast.makeText(this@LoginActivity, "✅ CUENTA VALIDADA", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@LoginActivity, "❌ Código incorrecto", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<VerificationResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                Toast.makeText(this@LoginActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun performLogin(email: String, pass: String) {
        binding.btnLogin.isEnabled = false
        ApiClient.apiService.login(LoginRequest(email, pass)).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (isFinishing || isDestroyed) return
                binding.btnLogin.isEnabled = true
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    loginResponse?.user?.let { user ->
                        // Guardar sesión incluyendo el access_token bajo la clave "TOKEN"
                        saveSession(user, loginResponse.access_token ?: "")
                        navigateToDashboard(user.id, user.nombre ?: "Usuario", user.rol ?: "EMPLOYEE")
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Error de acceso: Credenciales inválidas", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                binding.btnLogin.isEnabled = true
                Toast.makeText(this@LoginActivity, "Servidor no disponible", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveSession(user: User, token: String) {
        getSharedPreferences("AUTH_PREFS", Context.MODE_PRIVATE).edit().apply {
            putInt("USER_ID", user.id)
            putString("USER_NAME", user.nombre)
            putString("USER_ROLE", user.rol)
            putString("TOKEN", token) // Cambiado de "AUTH_TOKEN" a "TOKEN" según rúbrica
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

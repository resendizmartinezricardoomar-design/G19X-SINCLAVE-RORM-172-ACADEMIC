package com.example.evaluaciondedesempeo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.evaluaciondedesempeo.api.ApiClient
import com.example.evaluaciondedesempeo.api.UsuarioCreate
import com.example.evaluaciondedesempeo.api.VerificationResponse
import com.example.evaluaciondedesempeo.databinding.ActivityRegisterBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {
    private var _binding: ActivityRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            _binding = ActivityRegisterBinding.inflate(layoutInflater)
            setContentView(binding.root)

            binding.btnRegister.setOnClickListener {
                performRegister()
            }

            binding.tvBackToLogin.setOnClickListener {
                finish()
            }
        } catch (e: Exception) {
            Log.e("INSIGHT_ERROR", "Error en RegisterActivity", e)
            finish()
        }
    }

    private fun performRegister() {
        try {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa Nombre, Email y Contraseña", Toast.LENGTH_SHORT).show()
                return
            }

            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "REGISTRANDO..."

            // Por seguridad, el rol ya no se envía, el backend asigna EMPLOYEE automáticamente
            val request = UsuarioCreate(name, email, password)
            ApiClient.apiService.registrarUsuario(request).enqueue(object : Callback<VerificationResponse> {
                override fun onResponse(call: Call<VerificationResponse>, response: Response<VerificationResponse>) {
                    if (isFinishing || isDestroyed || _binding == null) return
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "REGISTRAR"
                    
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "✅ Código enviado con éxito", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@RegisterActivity, VerifyActivity::class.java)
                        intent.putExtra("EMAIL", email)
                        startActivity(intent)
                        finish() 
                    } else {
                        Toast.makeText(this@RegisterActivity, "❌ El correo ya existe", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<VerificationResponse>, t: Throwable) {
                    if (isFinishing || isDestroyed || _binding == null) return
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "REGISTRAR"
                    Toast.makeText(this@RegisterActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Log.e("INSIGHT_ERROR", "Error en performRegister", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

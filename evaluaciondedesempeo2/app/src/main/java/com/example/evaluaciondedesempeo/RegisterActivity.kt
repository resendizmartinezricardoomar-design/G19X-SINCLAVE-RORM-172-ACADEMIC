package com.example.evaluaciondedesempeo

import android.content.Intent
import android.os.Bundle
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
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            performRegister()
        }

        binding.tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun performRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val rolInput = binding.etRol.text.toString().uppercase().trim()
        
        // Validar que el rol sea ADMIN o EMPLOYEE, por defecto EMPLOYEE
        val rol = if (rolInput == "ADMIN") "ADMIN" else "EMPLOYEE"

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa Nombre, Email y Contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "REGISTRANDO..."

        val request = UsuarioCreate(name, email, password, rol)
        ApiClient.apiService.registrarUsuario(request).enqueue(object : Callback<VerificationResponse> {
            override fun onResponse(call: Call<VerificationResponse>, response: Response<VerificationResponse>) {
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "REGISTRAR COLABORADOR"
                
                if (response.isSuccessful) {
                    Toast.makeText(this@RegisterActivity, "✅ Código enviado a su correo", Toast.LENGTH_LONG).show()
                    
                    // Abrir pantalla de verificación pasando el email
                    val intent = Intent(this@RegisterActivity, VerifyActivity::class.java)
                    intent.putExtra("EMAIL", email)
                    startActivity(intent)
                    finish() 
                } else {
                    Toast.makeText(this@RegisterActivity, "❌ Error: El email ya existe o datos inválidos", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<VerificationResponse>, t: Throwable) {
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "REGISTRAR COLABORADOR"
                Toast.makeText(this@RegisterActivity, "Fallo de red: Verifique su servidor", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

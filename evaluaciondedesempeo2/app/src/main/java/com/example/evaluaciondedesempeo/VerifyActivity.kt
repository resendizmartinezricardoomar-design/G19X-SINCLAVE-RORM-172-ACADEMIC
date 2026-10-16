package com.example.evaluaciondedesempeo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.evaluaciondedesempeo.api.ApiClient
import com.example.evaluaciondedesempeo.api.VerificationResponse
import com.example.evaluaciondedesempeo.api.VerifyRequest
import com.example.evaluaciondedesempeo.databinding.ActivityVerifyBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VerifyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyBinding
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        email = intent.getStringExtra("EMAIL")
        
        if (email == null) {
            Toast.makeText(this, "Error: No se recibió el correo", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvVerifyMsg.text = "Hemos enviado un código de 6 dígitos al correo: $email"

        binding.btnVerify.setOnClickListener {
            val code = binding.etCode.text.toString().trim()
            if (code.length == 6) {
                performVerification(code)
            } else {
                Toast.makeText(this, "Ingresa un código de 6 dígitos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvResend.setOnClickListener {
            Toast.makeText(this, "Reenviando código...", Toast.LENGTH_SHORT).show()
            // Podrías implementar una ruta en el backend para reenviar el código si lo deseas
        }
    }

    private fun performVerification(code: String) {
        binding.btnVerify.isEnabled = false
        binding.btnVerify.text = "VERIFICANDO..."

        val request = VerifyRequest(email!!, code)
        ApiClient.apiService.verificarCuenta(request).enqueue(object : Callback<VerificationResponse> {
            override fun onResponse(call: Call<VerificationResponse>, response: Response<VerificationResponse>) {
                binding.btnVerify.isEnabled = true
                binding.btnVerify.text = "VERIFICAR CUENTA"

                if (response.isSuccessful) {
                    Toast.makeText(this@VerifyActivity, "✅ CUENTA ACTIVADA EXITOSAMENTE", Toast.LENGTH_LONG).show()
                    val intent = Intent(this@VerifyActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@VerifyActivity, "❌ Código incorrecto o expirado", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<VerificationResponse>, t: Throwable) {
                binding.btnVerify.isEnabled = true
                binding.btnVerify.text = "VERIFICAR CUENTA"
                Toast.makeText(this@VerifyActivity, "Fallo de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

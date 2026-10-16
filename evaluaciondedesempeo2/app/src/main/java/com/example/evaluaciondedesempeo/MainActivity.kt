package com.example.evaluaciondedesempeo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Redirigir al Login al iniciar la app
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}

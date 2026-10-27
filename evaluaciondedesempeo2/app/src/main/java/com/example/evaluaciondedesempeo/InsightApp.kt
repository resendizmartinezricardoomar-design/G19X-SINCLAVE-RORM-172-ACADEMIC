package com.example.evaluaciondedesempeo

import android.app.Application
import com.example.evaluaciondedesempeo.api.ApiClient

class InsightApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializamos el cliente de red una sola vez para toda la vida de la app
        ApiClient.init(this)
    }
}

package com.example.evaluaciondedesempeo

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.evaluaciondedesempeo.api.ApiClient
import com.example.evaluaciondedesempeo.databinding.ActivityCompareBinding
import com.example.evaluaciondedesempeo.models.Summary360
import com.example.evaluaciondedesempeo.models.User
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CompareActivity : AppCompatActivity() {
    private var _binding: ActivityCompareBinding? = null
    private val binding get() = _binding!!
    
    private var employeeList: List<User> = emptyList()
    private var summaryA: Summary360? = null
    private var summaryB: Summary360? = null
    private var authToken: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            _binding = ActivityCompareBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val prefs = getSharedPreferences("AUTH_PREFS", Context.MODE_PRIVATE)
            authToken = prefs.getString("AUTH_TOKEN", "") ?: ""

            loadEmployees()

            binding.btnClose.setOnClickListener { finish() }

            val listener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (employeeList.isNotEmpty()) updateComparison()
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }

            binding.spinnerUserA.onItemSelectedListener = listener
            binding.spinnerUserB.onItemSelectedListener = listener
        } catch (e: Exception) {
            Log.e("INSIGHT_FATAL", "Error en CompareActivity", e)
            finish()
        }
    }

    private fun loadEmployees() {
        if (authToken.isEmpty()) return
        val tokenHeader = "Bearer $authToken"
        ApiClient.apiService.getEmployees(tokenHeader).enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (isFinishing || isDestroyed || _binding == null) return
                if (response.isSuccessful) {
                    employeeList = response.body()?.filter { it.nombre != null } ?: emptyList()
                    val names = employeeList.map { it.nombre!! }
                    val adapter = ArrayAdapter(this@CompareActivity, android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerUserA.adapter = adapter
                    binding.spinnerUserB.adapter = adapter
                }
            }
            override fun onFailure(call: Call<List<User>>, t: Throwable) {}
        })
    }

    private fun updateComparison() {
        try {
            val posA = binding.spinnerUserA.selectedItemPosition
            val posB = binding.spinnerUserB.selectedItemPosition

            if (employeeList.isEmpty() || posA < 0 || posB < 0 || posA >= employeeList.size || posB >= employeeList.size) return
            if (posA == posB) return

            fetchData(employeeList[posA].id, true)
            fetchData(employeeList[posB].id, false)
        } catch (e: Exception) {
            Log.e("INSIGHT_ERROR", "Error al actualizar comparación", e)
        }
    }

    private fun fetchData(userId: Int, isUserA: Boolean) {
        val tokenHeader = "Bearer $authToken"
        ApiClient.apiService.getSummary360(tokenHeader, userId).enqueue(object : Callback<Summary360> {
            override fun onResponse(call: Call<Summary360>, response: Response<Summary360>) {
                if (isFinishing || isDestroyed || _binding == null) return
                if (response.isSuccessful && response.body() != null) {
                    if (isUserA) summaryA = response.body()
                    else summaryB = response.body()
                    renderChart()
                }
            }
            override fun onFailure(call: Call<Summary360>, t: Throwable) {}
        })
    }

    private fun renderChart() {
        try {
            val sA = summaryA ?: return
            val sB = summaryB ?: return
            
            if (_binding == null) return

            val scoresA = sA.scores?.filterNotNull() ?: emptyList()
            val scoresB = sB.scores?.filterNotNull() ?: emptyList()
            
            if (scoresA.isEmpty() || scoresB.isEmpty()) return

            val entriesA = scoresA.map { RadarEntry(it) }
            val entriesB = scoresB.map { RadarEntry(it) }

            val dataSetA = RadarDataSet(entriesA, "Colaborador A").apply {
                color = Color.parseColor("#6366F1")
                setDrawFilled(true)
                fillAlpha = 40
                lineWidth = 3f
            }

            val dataSetB = RadarDataSet(entriesB, "Colaborador B").apply {
                color = Color.parseColor("#10B981")
                setDrawFilled(true)
                fillAlpha = 40
                lineWidth = 3f
            }

            binding.radarChartCompare.apply {
                data = RadarData(dataSetA, dataSetB)
                description.isEnabled = false
                xAxis.valueFormatter = IndexAxisValueFormatter(sA.labels ?: emptyList())
                xAxis.textColor = Color.parseColor("#94A3B8")
                yAxis.axisMinimum = 0f
                yAxis.axisMaximum = 100f
                yAxis.setDrawLabels(false)
                animateXY(800, 800)
                invalidate()
            }
        } catch (e: Exception) {
            Log.e("INSIGHT_ERROR", "Error al renderizar gráfico", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

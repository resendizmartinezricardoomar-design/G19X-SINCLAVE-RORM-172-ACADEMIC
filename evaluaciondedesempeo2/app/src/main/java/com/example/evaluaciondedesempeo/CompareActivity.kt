package com.example.evaluaciondedesempeo

import android.graphics.Color
import android.os.Bundle
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
    private lateinit var binding: ActivityCompareBinding
    private var employeeList: List<User> = emptyList()
    private var summaryA: Summary360? = null
    private var summaryB: Summary360? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadEmployees()

        binding.btnClose.setOnClickListener { finish() }

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateComparison()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerUserA.onItemSelectedListener = listener
        binding.spinnerUserB.onItemSelectedListener = listener
    }

    private fun loadEmployees() {
        ApiClient.apiService.getEmployees().enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful) {
                    employeeList = response.body() ?: emptyList()
                    val names = employeeList.map { it.nombre ?: "Sin nombre" }
                    val adapter = ArrayAdapter(this@CompareActivity, android.R.layout.simple_spinner_item, names)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerUserA.adapter = adapter
                    binding.spinnerUserB.adapter = adapter
                }
            }
            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                Toast.makeText(this@CompareActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateComparison() {
        val posA = binding.spinnerUserA.selectedItemPosition
        val posB = binding.spinnerUserB.selectedItemPosition

        if (posA < 0 || posB < 0 || employeeList.isEmpty()) return
        if (posA == posB) {
            Toast.makeText(this, "Seleccione dos colaboradores distintos", Toast.LENGTH_SHORT).show()
            return
        }

        fetchData(employeeList[posA].id, true)
        fetchData(employeeList[posB].id, false)
    }

    private fun fetchData(userId: Int, isUserA: Boolean) {
        ApiClient.apiService.getSummary360(userId).enqueue(object : Callback<Summary360> {
            override fun onResponse(call: Call<Summary360>, response: Response<Summary360>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful) {
                    if (isUserA) summaryA = response.body()
                    else summaryB = response.body()
                    renderChart()
                }
            }
            override fun onFailure(call: Call<Summary360>, t: Throwable) {}
        })
    }

    private fun renderChart() {
        val sA = summaryA ?: return
        val sB = summaryB ?: return

        // Corrección: Uso de ?.map y elvis operator para asegurar que no sean nulos
        val scoresA = sA.scores ?: emptyList()
        val scoresB = sB.scores ?: emptyList()
        
        val entriesA = scoresA.map { RadarEntry(it) }
        val entriesB = scoresB.map { RadarEntry(it) }

        if (entriesA.isEmpty() || entriesB.isEmpty()) return

        val nameA = employeeList.getOrNull(binding.spinnerUserA.selectedItemPosition)?.nombre ?: "A"
        val nameB = employeeList.getOrNull(binding.spinnerUserB.selectedItemPosition)?.nombre ?: "B"

        val dataSetA = RadarDataSet(entriesA, nameA)
        dataSetA.apply {
            color = Color.parseColor("#6366F1")
            fillColor = Color.parseColor("#6366F1")
            setDrawFilled(true)
            fillAlpha = 50
            lineWidth = 3f
        }

        val dataSetB = RadarDataSet(entriesB, nameB)
        dataSetB.apply {
            color = Color.parseColor("#10B981")
            fillColor = Color.parseColor("#10B981")
            setDrawFilled(true)
            fillAlpha = 50
            lineWidth = 3f
        }

        binding.radarChartCompare.apply {
            data = RadarData(dataSetA, dataSetB)
            description.isEnabled = false
            // Corrección: Manejo de labels nulos
            xAxis.valueFormatter = IndexAxisValueFormatter(sA.labels ?: emptyList())
            xAxis.textColor = Color.parseColor("#64748B")
            xAxis.textSize = 10f
            
            yAxis.axisMinimum = 0f
            yAxis.axisMaximum = 100f
            yAxis.setDrawLabels(false)

            animateXY(1000, 1000)
            invalidate()
        }
    }
}

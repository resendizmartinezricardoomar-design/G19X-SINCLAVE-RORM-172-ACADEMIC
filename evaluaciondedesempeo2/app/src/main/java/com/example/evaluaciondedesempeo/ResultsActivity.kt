package com.example.evaluaciondedesempeo

import android.content.ContentValues
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.evaluaciondedesempeo.api.ApiClient
import com.example.evaluaciondedesempeo.databinding.ActivityResultsBinding
import com.example.evaluaciondedesempeo.models.Summary360
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class ResultsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultsBinding
    private var currentUserId: Int = -1
    private var currentEmployeeName: String = "Colaborador"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = intent.getIntExtra("USER_ID", -1)
        currentEmployeeName = intent.getStringExtra("USER_NAME") ?: "Colaborador"

        binding.tvEmployeeName.text = currentEmployeeName
        loadRadarData(currentUserId)

        binding.btnClose.setOnClickListener { finish() }
        binding.btnDownloadReport.setOnClickListener { downloadPdfReport(currentUserId) }
    }

    private fun loadRadarData(userId: Int) {
        ApiClient.apiService.getSummary360(userId).enqueue(object : Callback<Summary360> {
            override fun onResponse(call: Call<Summary360>, response: Response<Summary360>) {
                if (response.isSuccessful) {
                    val summary = response.body()
                    summary?.let {
                        setupRadarChart(it.labels, it.scores)
                        displayKPIs(it.kpis)
                        displayAIAnalysis(it)
                    }
                }
            }
            override fun onFailure(call: Call<Summary360>, t: Throwable) {
                Toast.makeText(this@ResultsActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun downloadPdfReport(userId: Int) {
        binding.btnDownloadReport.isEnabled = false
        binding.btnDownloadReport.text = "GENERANDO PDF..."

        ApiClient.apiService.downloadReport(userId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { 
                        val success = savePdfToDownloads(it) 
                        if (success) {
                            Toast.makeText(this@ResultsActivity, "✅ Reporte guardado en Descargas", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(this@ResultsActivity, "❌ Error en el servidor al generar PDF", Toast.LENGTH_SHORT).show()
                }
                binding.btnDownloadReport.isEnabled = true
                binding.btnDownloadReport.text = "GENERAR REPORTE PDF"
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(this@ResultsActivity, "Fallo de red: Verifique conexión", Toast.LENGTH_SHORT).show()
                binding.btnDownloadReport.isEnabled = true
                binding.btnDownloadReport.text = "GENERAR REPORTE PDF"
            }
        })
    }

    private fun savePdfToDownloads(body: ResponseBody): Boolean {
        val fileName = "Reporte_${currentEmployeeName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        return try {
            val inputStream: InputStream = body.byteStream()
            val outputStream: OutputStream?

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = uri?.let { contentResolver.openOutputStream(it) }
            } else {
                val target = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                outputStream = FileOutputStream(target)
            }

            outputStream?.use { output ->
                val buffer = ByteArray(4096)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun displayKPIs(kpis: Map<String, Float>?) {
        kpis?.let {
            it["Eficiencia Operativa"]?.let { s -> 
                binding.progressEficiencia.progress = s.toInt()
                binding.tvEficienciaScore.text = "${s.toInt()}%"
            }
            it["Competencias Blandas"]?.let { s -> 
                binding.progressBlandas.progress = s.toInt()
                binding.tvBlandasScore.text = "${s.toInt()}%"
            }
            it["Potencial y Crecimiento"]?.let { s -> 
                binding.progressPotencial.progress = s.toInt()
                binding.tvPotencialScore.text = "${s.toInt()}%"
            }
        }
    }

    private fun displayAIAnalysis(summary: Summary360) {
        summary.analisis_ia?.let {
            binding.tvSentiment.text = "Sentimiento: ${it.sentimiento}"
            binding.tvRecommendations.text = "Recomendaciones IA:\n${it.recomendaciones.joinToString("\n• ", prefix = "• ")}"
            binding.cardIA.visibility = View.VISIBLE
        }
    }

    private fun setupRadarChart(labels: List<String>, scores: List<Float>) {
        if (labels.isEmpty()) return
        val entries = scores.map { RadarEntry(it) }
        val dataSet = RadarDataSet(entries, "Desempeño")
        dataSet.color = Color.parseColor("#6366F1")
        dataSet.setDrawFilled(true)
        dataSet.fillAlpha = 65
        binding.radarChart.apply {
            data = RadarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            yAxis.axisMinimum = 0f
            yAxis.axisMaximum = 100f
            animateXY(1000, 1000)
            invalidate()
        }
    }
}

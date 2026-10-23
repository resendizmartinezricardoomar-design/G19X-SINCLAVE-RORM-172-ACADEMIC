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
                if (response.isSuccessful && response.body() != null) {
                    val summary = response.body()!!
                    setupRadarChart(summary.labels ?: emptyList(), summary.scores ?: emptyList())
                    displayKPIs(summary.kpis)
                    displayAIAnalysis(summary)
                }
            }
            override fun onFailure(call: Call<Summary360>, t: Throwable) {
                Toast.makeText(this@ResultsActivity, "Fallo de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupRadarChart(labels: List<String>, scores: List<Float>) {
        if (labels.isEmpty() || scores.isEmpty()) {
            binding.radarChart.visibility = View.GONE
            return
        }
        binding.radarChart.visibility = View.VISIBLE
        val entries = scores.map { RadarEntry(it) }
        val dataSet = RadarDataSet(entries, "Desempeño Actual")
        dataSet.color = Color.parseColor("#6366F1")
        dataSet.setDrawFilled(true)
        dataSet.fillAlpha = 70
        dataSet.lineWidth = 2f

        binding.radarChart.apply {
            data = RadarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            yAxis.axisMinimum = 0f
            yAxis.axisMaximum = 100f
            description.isEnabled = false
            legend.isEnabled = false
            animateXY(800, 800)
            invalidate()
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
            binding.tvSentiment.text = "Sentimiento: ${it.sentimiento ?: "Neutral"}"
            val recs = it.recomendaciones ?: emptyList()
            binding.tvRecommendations.text = if (recs.isNotEmpty()) {
                "Recomendaciones IA:\n" + recs.joinToString("\n• ", prefix = "• ")
            } else {
                "Sigue con tu buen desempeño."
            }
            binding.cardIA.visibility = View.VISIBLE
        } ?: run {
            binding.cardIA.visibility = View.GONE
        }
    }

    private fun downloadPdfReport(userId: Int) {
        binding.btnDownloadReport.isEnabled = false
        ApiClient.apiService.downloadReport(userId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful && response.body() != null) {
                    savePdfToDownloads(response.body()!!)
                    Toast.makeText(this@ResultsActivity, "✅ PDF Descargado", Toast.LENGTH_SHORT).show()
                }
                binding.btnDownloadReport.isEnabled = true
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                binding.btnDownloadReport.isEnabled = true
            }
        })
    }

    private fun savePdfToDownloads(body: ResponseBody): Boolean {
        val fileName = "Reporte_${currentEmployeeName.replace(" ", "_")}.pdf"
        return try {
            val inputStream: InputStream = body.byteStream()
            val outputStream: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                outputStream = uri?.let { contentResolver.openOutputStream(it) }
            } else {
                val target = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                outputStream = FileOutputStream(target)
            }
            outputStream?.use { out ->
                val buffer = ByteArray(4096)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) { out.write(buffer, 0, read) }
                out.flush()
                true
            } ?: false
        } catch (e: Exception) { false }
    }
}

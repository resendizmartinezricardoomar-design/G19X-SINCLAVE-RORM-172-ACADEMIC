package com.example.evaluaciondedesempeo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.evaluaciondedesempeo.databinding.ItemEvaluationCompetenciaDetailBinding
import com.example.evaluaciondedesempeo.model.Competencia

class EvaluationDetailCompetenciaAdapter(private val competencias: List<Competencia>) :
    RecyclerView.Adapter<EvaluationDetailCompetenciaAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemEvaluationCompetenciaDetailBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEvaluationCompetenciaDetailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val competencia = competencias[position]
        holder.binding.textCompetenciaNombre.text = competencia.nombre
        holder.binding.textCompetenciaCalif.text = "Calificación: ${competencia.calificacion} / 5.0"
        holder.binding.progressCompetencia.progress = (competencia.calificacion * 10).toInt()
    }

    override fun getItemCount(): Int = competencias.size
}

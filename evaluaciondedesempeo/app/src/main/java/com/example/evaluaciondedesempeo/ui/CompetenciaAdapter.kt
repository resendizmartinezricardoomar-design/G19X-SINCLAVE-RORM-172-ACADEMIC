package com.example.evaluaciondedesempeo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.evaluaciondedesempeo.databinding.ItemCompetenciaInputBinding
import com.example.evaluaciondedesempeo.model.Competencia

class CompetenciaAdapter(private val competencias: List<Competencia>) :
    RecyclerView.Adapter<CompetenciaAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCompetenciaInputBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCompetenciaInputBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val competencia = competencias[position]
        holder.binding.textNombreCompetencia.text = competencia.nombre
        holder.binding.textDescCompetencia.text = competencia.descripcion
        holder.binding.sliderCalificacion.value = competencia.calificacion.takeIf { it > 0 } ?: 3.0f
        
        holder.binding.sliderCalificacion.addOnChangeListener { _, value, _ ->
            competencia.calificacion = value
        }
    }

    override fun getItemCount(): Int = competencias.size
}

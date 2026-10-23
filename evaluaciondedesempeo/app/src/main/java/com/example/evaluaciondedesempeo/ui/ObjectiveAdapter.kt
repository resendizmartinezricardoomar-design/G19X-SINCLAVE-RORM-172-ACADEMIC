package com.example.evaluaciondedesempeo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.evaluaciondedesempeo.databinding.ItemObjectiveInputBinding
import com.example.evaluaciondedesempeo.model.Objetivo

class ObjectiveAdapter(private val objetivos: List<Objetivo>) :
    RecyclerView.Adapter<ObjectiveAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemObjectiveInputBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemObjectiveInputBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val objetivo = objetivos[position]
        holder.binding.textObjectiveDesc.text = objetivo.descripcion
        holder.binding.textPercentage.text = "Cumplimiento: ${objetivo.porcentajeCumplimiento.toInt()}%"
        holder.binding.sliderObjective.value = objetivo.porcentajeCumplimiento

        holder.binding.sliderObjective.addOnChangeListener { _, value, _ ->
            objetivo.copy(porcentajeCumplimiento = value) // Nota: Objetivo es data class, actualizamos la referencia si es necesario o mutamos si fuera var
            // Para simplicidad en este ejemplo, mutaremos el valor si fuera var o usaremos una lista mutable
            holder.binding.textPercentage.text = "Cumplimiento: ${value.toInt()}%"
        }
    }

    override fun getItemCount(): Int = objetivos.size
}

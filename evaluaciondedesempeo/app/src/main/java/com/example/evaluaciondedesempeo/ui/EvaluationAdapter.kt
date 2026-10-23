package com.example.evaluaciondedesempeo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.evaluaciondedesempeo.databinding.ItemEvaluationBinding
import com.example.evaluaciondedesempeo.model.Evaluacion

class EvaluationAdapter : ListAdapter<Evaluacion, EvaluationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEvaluationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemEvaluationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(evaluacion: Evaluacion) {
            binding.textEvalType.text = "Tipo: ${evaluacion.tipo.name}"
            binding.textEvaluado.text = "Evaluado ID: ${evaluacion.evaluadoId}"
            binding.textAiFeedback.text = evaluacion.retroalimentacionIA ?: "Esperando retroalimentación de IA..."
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Evaluacion>() {
        override fun areItemsTheSame(oldItem: Evaluacion, newItem: Evaluacion): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Evaluacion, newItem: Evaluacion): Boolean = oldItem == newItem
    }
}

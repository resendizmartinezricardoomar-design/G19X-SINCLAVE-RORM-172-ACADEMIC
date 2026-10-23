package com.example.evaluaciondedesempeo.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.evaluaciondedesempeo.databinding.ItemIndicatorInputBinding
import com.example.evaluaciondedesempeo.model.Indicador

class IndicatorAdapter(private val indicadores: List<Indicador>) :
    RecyclerView.Adapter<IndicatorAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemIndicatorInputBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIndicatorInputBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val indicador = indicadores[position]
        holder.binding.textIndicatorName.text = indicador.nombre
        holder.binding.textMeta.text = "Meta: ${indicador.meta}"
        holder.binding.editActualValue.setText(indicador.actual.toString())

        holder.binding.editActualValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val value = s.toString().toFloatOrNull() ?: 0f
                indicador.actual = value
            }
        })
    }

    override fun getItemCount(): Int = indicadores.size
}

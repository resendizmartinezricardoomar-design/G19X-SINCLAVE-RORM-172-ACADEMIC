package com.example.evaluaciondedesempeo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.evaluaciondedesempeo.databinding.ItemEmployeeBinding
import com.example.evaluaciondedesempeo.models.User

class EmployeeAdapter(
    private var employees: List<User>,
    private val isAdmin: Boolean,
    private val onEvaluateClick: (User) -> Unit,
    private val onViewRadarClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {

    class EmployeeViewHolder(val binding: ItemEmployeeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val binding = ItemEmployeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EmployeeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        try {
            val employee = employees.getOrNull(position) ?: return
            
            holder.binding.tvEmployeeName.text = employee.nombre ?: "Colaborador"
            holder.binding.tvEmployeeRole.text = employee.rol ?: "GENERAL"
            
            // Seguridad de visibilidad
            val adminVisibility = if (isAdmin) View.VISIBLE else View.GONE
            holder.binding.btnEvaluate.visibility = adminVisibility
            holder.binding.btnDeleteEmployee.visibility = adminVisibility
            
            holder.binding.btnEvaluate.setOnClickListener { onEvaluateClick(employee) }
            holder.binding.btnViewRadar.setOnClickListener { onViewRadarClick(employee) }
            holder.binding.btnDeleteEmployee.setOnClickListener { onDeleteClick(employee) }
            
        } catch (e: Exception) {
            // Si algo falla en un elemento, evitamos que cierre toda la app
        }
    }

    override fun getItemCount(): Int = employees.size

    fun updateData(newEmployees: List<User>) {
        employees = newEmployees
        notifyDataSetChanged()
    }
}

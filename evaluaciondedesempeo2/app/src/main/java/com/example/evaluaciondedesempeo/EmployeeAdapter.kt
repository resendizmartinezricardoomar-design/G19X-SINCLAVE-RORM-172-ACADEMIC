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
        val employee = employees[position]
        
        // Uso de valores por defecto para evitar NullPointerException
        holder.binding.tvEmployeeName.text = employee.nombre ?: "Usuario sin nombre"
        holder.binding.tvEmployeeRole.text = employee.rol ?: "Puesto no asignado"
        
        // Gestión de visibilidad según rol de Administrador
        if (isAdmin) {
            holder.binding.btnEvaluate.visibility = View.VISIBLE
            holder.binding.btnDeleteEmployee.visibility = View.VISIBLE
        } else {
            holder.binding.btnEvaluate.visibility = View.GONE
            holder.binding.btnDeleteEmployee.visibility = View.GONE
        }
        
        holder.binding.btnEvaluate.setOnClickListener { onEvaluateClick(employee) }
        holder.binding.btnViewRadar.setOnClickListener { onViewRadarClick(employee) }
        holder.binding.btnDeleteEmployee.setOnClickListener { onDeleteClick(employee) }
    }

    override fun getItemCount(): Int = employees.size

    fun updateData(newEmployees: List<User>) {
        employees = newEmployees
        notifyDataSetChanged()
    }
}

package com.example.evaluaciondedesempeo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.evaluaciondedesempeo.databinding.FragmentDashboardBinding
import com.example.evaluaciondedesempeo.viewmodel.EvaluationViewModel

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EvaluationViewModel by activityViewModels()
    private val adapter = EvaluationAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerRecentEvaluations.adapter = adapter

        viewModel.evaluations.observe(viewLifecycleOwner) { evaluations ->
            adapter.submitList(evaluations.takeLast(5).reversed())
            
            // Calcular promedio global
            if (evaluations.isNotEmpty()) {
                val totalAvg = evaluations.flatMap { it.competencias }
                    .map { it.calificacion }
                    .average()
                binding.textAverageScore.text = String.format("%.1f", totalAvg)
            } else {
                binding.textAverageScore.text = "0.0"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

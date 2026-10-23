package com.example.evaluaciondedesempeo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.evaluaciondedesempeo.R
import com.example.evaluaciondedesempeo.databinding.FragmentEvaluationsBinding
import com.example.evaluaciondedesempeo.viewmodel.EvaluationViewModel

class EvaluationsFragment : Fragment() {
    private var _binding: FragmentEvaluationsBinding? = null
    private val binding get() = _binding!!

    // Usamos activityViewModels para compartir los datos entre fragmentos
    private val viewModel: EvaluationViewModel by activityViewModels()
    private val adapter = EvaluationAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvaluationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerEvaluations.adapter = adapter

        viewModel.evaluations.observe(viewLifecycleOwner) { evaluations ->
            adapter.submitList(evaluations)
        }

        binding.fabAddEvaluation.setOnClickListener {
            // Navegar al fragmento de creación de nueva evaluación
            findNavController().navigate(R.id.action_navigation_evaluations_to_newEvaluationFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

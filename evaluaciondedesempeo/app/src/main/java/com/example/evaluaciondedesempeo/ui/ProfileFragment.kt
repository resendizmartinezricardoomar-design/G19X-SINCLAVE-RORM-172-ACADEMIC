package com.example.evaluaciondedesempeo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.evaluaciondedesempeo.databinding.FragmentProfileBinding
import com.example.evaluaciondedesempeo.viewmodel.EvaluationViewModel

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EvaluationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observar las evaluaciones para mostrar las recomendaciones de IA más recientes
        viewModel.evaluations.observe(viewLifecycleOwner) { evaluations ->
            val latestFeedback = evaluations.lastOrNull { !it.retroalimentacionIA.isNullOrBlank() }
            if (latestFeedback != null) {
                binding.textRecommendationsIa.text = latestFeedback.retroalimentacionIA
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

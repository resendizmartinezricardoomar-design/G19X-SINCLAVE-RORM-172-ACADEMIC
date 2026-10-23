package com.example.evaluaciondedesempeo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.evaluaciondedesempeo.R
import com.example.evaluaciondedesempeo.databinding.FragmentNewEvaluationBinding
import com.example.evaluaciondedesempeo.model.Competencia
import com.example.evaluaciondedesempeo.model.Evaluacion
import com.example.evaluaciondedesempeo.model.Indicador
import com.example.evaluaciondedesempeo.model.Objetivo
import com.example.evaluaciondedesempeo.model.TipoEvaluacion
import com.example.evaluaciondedesempeo.viewmodel.EvaluationViewModel
import java.util.UUID

class NewEvaluationFragment : Fragment() {

    private var _binding: FragmentNewEvaluationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EvaluationViewModel by activityViewModels()

    // Listas iniciales para la evaluación
    private val listaCompetencias = listOf(
        Competencia(UUID.randomUUID().toString(), "Liderazgo", "Capacidad para guiar equipos.", 3.0f),
        Competencia(UUID.randomUUID().toString(), "Comunicación", "Efectividad al transmitir ideas.", 3.0f),
        Competencia(UUID.randomUUID().toString(), "Trabajo en Equipo", "Colaboración con pares.", 3.0f)
    )

    private val listaObjetivos = listOf(
        Objetivo(UUID.randomUUID().toString(), "Cumplimiento de metas de ventas", 0f),
        Objetivo(UUID.randomUUID().toString(), "Calidad en la entrega de proyectos", 0f)
    )

    private val listaIndicadores = listOf(
        Indicador(UUID.randomUUID().toString(), "Productividad (Tickets/Hora)", 10f, 0f),
        Indicador(UUID.randomUUID().toString(), "Nivel de Satisfacción Cliente", 5f, 0f)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewEvaluationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar adaptadores
        binding.recyclerCompetencias.adapter = CompetenciaAdapter(listaCompetencias)
        binding.recyclerObjetivos.adapter = ObjectiveAdapter(listaObjetivos)
        binding.recyclerIndicadores.adapter = IndicatorAdapter(listaIndicadores)

        binding.btnSaveEvaluation.setOnClickListener {
            val evaluadoId = binding.editEvaluadoId.text.toString()
            if (evaluadoId.isBlank()) {
                binding.editEvaluadoId.error = "Ingresa el nombre del empleado"
                return@setOnClickListener
            }

            val tipo = when (binding.radioGroupType.checkedRadioButtonId) {
                R.id.radio_90 -> TipoEvaluacion.EVAL_90
                R.id.radio_180 -> TipoEvaluacion.EVAL_180
                R.id.radio_360 -> TipoEvaluacion.EVAL_360
                else -> TipoEvaluacion.EVAL_90
            }

            val newEval = Evaluacion(
                id = UUID.randomUUID().toString(),
                evaluadoId = evaluadoId,
                evaluadorId = "Admin",
                tipo = tipo,
                competencias = listaCompetencias,
                indicadores = listaIndicadores,
                objetivos = listaObjetivos
            )

            viewModel.addEvaluation(newEval)
            viewModel.getAIRecommendations(newEval)

            Toast.makeText(context, "Evaluación enviada con éxito", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

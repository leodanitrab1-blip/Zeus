package com.zeus.suite.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.zeus.suite.R

class AIFragment : Fragment() {

    companion object {
        fun newInstance(): AIFragment {
            return AIFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ai, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.cardQuery)?.setOnClickListener {
            showToast("Consultar Datos - En desarrollo")
        }

        view.findViewById<View>(R.id.cardSummary)?.setOnClickListener {
            showToast("Generar Resumen - En desarrollo")
        }

        view.findViewById<View>(R.id.cardAnomaly)?.setOnClickListener {
            showToast("Detectar Anomalias - En desarrollo")
        }

        view.findViewById<View>(R.id.cardReport)?.setOnClickListener {
            showToast("Reporte Ejecutivo - En desarrollo")
        }

        view.findViewById<View>(R.id.cardAutoFill)?.setOnClickListener {
            showToast("Auto-llenado - En desarrollo")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
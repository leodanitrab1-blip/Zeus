package com.zeus.suite.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.zeus.suite.R

class BigDataFragment : Fragment() {

    companion object {
        fun newInstance(): BigDataFragment {
            return BigDataFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bigdata, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.cardCSV)?.setOnClickListener {
            showToast("Abrir CSV - En desarrollo")
        }

        view.findViewById<View>(R.id.cardExcel)?.setOnClickListener {
            showToast("Abrir Excel - En desarrollo")
        }

        view.findViewById<View>(R.id.cardFilter)?.setOnClickListener {
            showToast("Filtrar Datos - En desarrollo")
        }

        view.findViewById<View>(R.id.cardSearch)?.setOnClickListener {
            showToast("Buscar - En desarrollo")
        }

        view.findViewById<View>(R.id.cardChart)?.setOnClickListener {
            showToast("Graficar - En desarrollo")
        }

        view.findViewById<View>(R.id.cardStats)?.setOnClickListener {
            showToast("Estadisticas - En desarrollo")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
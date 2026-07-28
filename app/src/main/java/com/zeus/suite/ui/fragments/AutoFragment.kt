package com.zeus.suite.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.zeus.suite.R

class AutoFragment : Fragment() {

    companion object {
        fun newInstance(): AutoFragment {
            return AutoFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_auto, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.cardExcelToPdf)?.setOnClickListener {
            showToast("Excel a PDF - En desarrollo")
        }

        view.findViewById<View>(R.id.cardCsvToReport)?.setOnClickListener {
            showToast("CSV a Reporte - En desarrollo")
        }

        view.findViewById<View>(R.id.cardBatchPdf)?.setOnClickListener {
            showToast("Lote de PDFs - En desarrollo")
        }

        view.findViewById<View>(R.id.cardInvoice)?.setOnClickListener {
            showToast("Facturas - En desarrollo")
        }

        view.findViewById<View>(R.id.cardStatement)?.setOnClickListener {
            showToast("Estados de Cuenta - En desarrollo")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
package com.zeus.suite.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.zeus.suite.R

class PDFModuleFragment : Fragment() {

    companion object {
        fun newInstance(): PDFModuleFragment {
            return PDFModuleFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pdf, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.cardMergePDF)?.setOnClickListener {
            showToast("Unir PDFs - En desarrollo")
        }

        view.findViewById<View>(R.id.cardSplitPDF)?.setOnClickListener {
            showToast("Dividir PDF - En desarrollo")
        }

        view.findViewById<View>(R.id.cardSignPDF)?.setOnClickListener {
            showToast("Firmar PDF - En desarrollo")
        }

        view.findViewById<View>(R.id.cardCompressPDF)?.setOnClickListener {
            showToast("Comprimir PDF - En desarrollo")
        }

        view.findViewById<View>(R.id.cardConvertPDF)?.setOnClickListener {
            showToast("Convertir PDF - En desarrollo")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
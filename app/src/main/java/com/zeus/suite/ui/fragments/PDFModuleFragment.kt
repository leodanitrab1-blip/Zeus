package com.zeus.suite.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.zeus.suite.R
import com.zeus.suite.pdf.PDFMerger
import com.zeus.suite.utils.FileManager
import java.io.File

class PDFModuleFragment : Fragment() {

    companion object {
        fun newInstance(): PDFModuleFragment {
            return PDFModuleFragment()
        }
    }

    private lateinit var fileManager: FileManager
    private lateinit var pdfMerger: PDFMerger
    private val selectedFiles = mutableListOf<Uri>()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedFiles.clear()
            selectedFiles.addAll(uris)
            showMergeConfirmation(uris)
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
        
        fileManager = FileManager(requireContext())
        pdfMerger = PDFMerger(requireContext())
        
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.cardMergePDF)?.setOnClickListener {
            openFilePicker()
        }

        view.findViewById<View>(R.id.cardSplitPDF)?.setOnClickListener {
            showToast("Dividir PDF - Proximamente")
        }

        view.findViewById<View>(R.id.cardSignPDF)?.setOnClickListener {
            showToast("Firmar PDF - Proximamente")
        }

        view.findViewById<View>(R.id.cardCompressPDF)?.setOnClickListener {
            showToast("Comprimir PDF - Proximamente")
        }

        view.findViewById<View>(R.id.cardConvertPDF)?.setOnClickListener {
            showToast("Convertir PDF - Proximamente")
        }
    }

    private fun openFilePicker() {
        try {
            filePickerLauncher.launch(arrayOf("application/pdf"))
        } catch (e: Exception) {
            showToast("Error al abrir selector de archivos")
        }
    }

    private fun showMergeConfirmation(uris: List<Uri>) {
        val fileNames = uris.map { uri -> getFileName(uri) }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Unir PDFs")
            .setMessage("Se uniran ${uris.size} archivos:\n\n${fileNames.joinToString("\n")}")
            .setPositiveButton("Unir") { _, _ ->
                mergePDFs(uris)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                selectedFiles.clear()
            }
            .show()
    }

    private fun mergePDFs(uris: List<Uri>) {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Uniendo PDFs")
            .setMessage("Procesando archivos...")
            .setCancelable(false)
            .create()
        
        progressDialog.show()

        Thread {
            val outputFileName = "unido_${System.currentTimeMillis()}.pdf"
            val result = pdfMerger.mergePDFs(uris, outputFileName)

            requireActivity().runOnUiThread {
                progressDialog.dismiss()

                if (result != null) {
                    showSuccessDialog(result)
                } else {
                    showToast("Error al unir los PDFs")
                }
            }
        }.start()
    }

    private fun showSuccessDialog(file: File) {
        AlertDialog.Builder(requireContext())
            .setTitle("PDF Unido Exitosamente")
            .setMessage("Archivo guardado como:\n${file.name}\n\nTamaño: ${fileManager.formatFileSize(file.length())}")
            .setPositiveButton("Abrir") { _, _ ->
                openFile(file)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun openFile(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            showToast("No hay aplicacion para abrir PDF")
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "desconocido"
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) name = it.getString(index)
            }
        }
        return name
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
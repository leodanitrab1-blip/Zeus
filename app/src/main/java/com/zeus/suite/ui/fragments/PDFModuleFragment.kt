package com.zeus.suite.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.zeus.suite.R
import com.zeus.suite.pdf.PDFMerger
import com.zeus.suite.pdf.PDFSplitter
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
    private lateinit var pdfSplitter: PDFSplitter
    private val selectedFiles = mutableListOf<Uri>()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedFiles.clear()
            selectedFiles.addAll(uris)
            
            if (uris.size == 1) {
                showSplitOptions(uris[0])
            } else {
                showMergeConfirmation(uris)
            }
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
        pdfSplitter = PDFSplitter(requireContext())
        
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.cardMergePDF)?.setOnClickListener {
            openFilePicker()
        }

        view.findViewById<View>(R.id.cardSplitPDF)?.setOnClickListener {
            openSingleFilePicker()
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

    private fun openSingleFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        singleFileLauncher.launch(intent)
    }

    private val singleFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                showSplitOptions(uri)
            }
        }
    }

    private fun showSplitOptions(uri: Uri) {
        val pageCount = pdfSplitter.getPageCount(uri)
        
        if (pageCount <= 1) {
            showToast("El PDF tiene solo 1 pagina")
            return
        }

        val options = arrayOf(
            "Seleccionar paginas especificas",
            "Dividir en 2 partes",
            "Dividir en 3 partes",
            "Dividir en 4 partes"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Dividir PDF - ${getFileName(uri)}")
            .setMessage("Total de paginas: $pageCount")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showPageSelector(uri, pageCount)
                    1 -> splitInParts(uri, 2)
                    2 -> splitInParts(uri, 3)
                    3 -> splitInParts(uri, 4)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPageSelector(uri: Uri, totalPages: Int) {
        val linearLayout = LinearLayout(requireContext())
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(48, 24, 48, 24)

        val checkBoxes = mutableListOf<CheckBox>()
        
        for (i in 0 until totalPages) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = "Pagina ${i + 1}"
            checkBox.isChecked = true
            linearLayout.addView(checkBox)
            checkBoxes.add(checkBox)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar paginas")
            .setView(linearLayout)
            .setPositiveButton("Dividir") { _, _ ->
                val selectedPages = checkBoxes
                    .mapIndexedNotNull { index, cb -> if (cb.isChecked) index else null }
                
                if (selectedPages.isNotEmpty()) {
                    splitSelectedPages(uri, selectedPages)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun splitSelectedPages(uri: Uri, pages: List<Int>) {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Dividiendo PDF")
            .setMessage("Extrayendo paginas seleccionadas...")
            .setCancelable(false)
            .create()
        
        progressDialog.show()

        Thread {
            val result = pdfSplitter.splitPDF(
                uri,
                pages,
                "dividido_${System.currentTimeMillis()}.pdf"
            )

            requireActivity().runOnUiThread {
                progressDialog.dismiss()

                if (result != null) {
                    showSuccessDialog(result, "PDF dividido exitosamente")
                } else {
                    showToast("Error al dividir el PDF")
                }
            }
        }.start()
    }

    private fun splitInParts(uri: Uri, parts: Int) {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Dividiendo PDF")
            .setMessage("Dividiendo en $parts partes...")
            .setCancelable(false)
            .create()
        
        progressDialog.show()

        Thread {
            val results = pdfSplitter.splitPDFInParts(uri, parts)

            requireActivity().runOnUiThread {
                progressDialog.dismiss()

                if (results.isNotEmpty()) {
                    val message = "Se crearon ${results.size} archivos:\n\n" +
                        results.joinToString("\n") { it.name }
                    
                    AlertDialog.Builder(requireContext())
                        .setTitle("PDF dividido exitosamente")
                        .setMessage(message)
                        .setPositiveButton("Aceptar", null)
                        .show()
                } else {
                    showToast("Error al dividir el PDF")
                }
            }
        }.start()
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
                    showSuccessDialog(result, "PDF unido exitosamente")
                } else {
                    showToast("Error al unir los PDFs")
                }
            }
        }.start()
    }

    private fun showSuccessDialog(file: File, title: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage("Archivo guardado como:\n${file.name}\n\nTamano: ${fileManager.formatFileSize(file.length())}")
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
package com.zeus.suite.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.zeus.suite.R
import com.zeus.suite.pdf.PDFCompressor
import com.zeus.suite.pdf.PDFMerger
import com.zeus.suite.pdf.PDFSigner
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
    private lateinit var pdfSigner: PDFSigner
    private lateinit var pdfCompressor: PDFCompressor
    private var pendingAction: String = ""

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@registerForActivityResult

        when (pendingAction) {
            "merge" -> {
                if (uris.size >= 2) showMergeConfirmation(uris)
                else showToast("Seleccione al menos 2 archivos para unir")
            }
            "split" -> showSplitOptions(uris[0])
            "sign" -> showSignDialog(uris[0])
            "compress" -> showCompressDialog(uris[0])
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
        pdfSigner = PDFSigner(requireContext())
        pdfCompressor = PDFCompressor(requireContext())
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.cardMergePDF)?.setOnClickListener {
            pendingAction = "merge"; openFilePicker()
        }
        view.findViewById<View>(R.id.cardSplitPDF)?.setOnClickListener {
            pendingAction = "split"; openFilePicker()
        }
        view.findViewById<View>(R.id.cardSignPDF)?.setOnClickListener {
            pendingAction = "sign"; openFilePicker()
        }
        view.findViewById<View>(R.id.cardCompressPDF)?.setOnClickListener {
            pendingAction = "compress"; openFilePicker()
        }
        view.findViewById<View>(R.id.cardConvertPDF)?.setOnClickListener {
            showToast("Convertir PDF - Proximamente")
        }
    }

    private fun openFilePicker() {
        try { filePickerLauncher.launch(arrayOf("application/pdf")) }
        catch (e: Exception) { showToast("Error al abrir selector de archivos") }
    }

    private fun showCompressDialog(uri: Uri) {
        val originalSize = getFileSize(uri)
        
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 24, 48, 24)

        val qualityText = TextView(requireContext()).apply {
            text = "Calidad: 50%"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }

        val seekBar = SeekBar(requireContext()).apply {
            max = 90
            progress = 50
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, b: Boolean) {
                    qualityText.text = "Calidad: ${p + 10}%"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        layout.addView(TextView(requireContext()).apply {
            text = "PDF: ${getFileName(uri)}\nTamano: ${fileManager.formatFileSize(originalSize)}"
            textSize = 14f
            setPadding(0, 0, 0, 16)
        })
        layout.addView(qualityText)
        layout.addView(seekBar)

        AlertDialog.Builder(requireContext())
            .setTitle("Comprimir PDF")
            .setView(layout)
            .setPositiveButton("Comprimir") { _, _ ->
                val quality = seekBar.progress + 10
                doCompress(uri, quality)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun doCompress(uri: Uri, quality: Int) {
        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Comprimiendo PDF")
            .setMessage("Reduciendo tamano...")
            .setCancelable(false)
            .create()
        pd.show()

        Thread {
            try {
                val outputFileName = "comprimido_${System.currentTimeMillis()}.pdf"
                val result = pdfCompressor.compressPDF(uri, quality, outputFileName)

                requireActivity().runOnUiThread {
                    pd.dismiss()
                    if (result != null && result.exists() && result.length() > 0) {
                        val originalSize = getFileSize(uri)
                        val compressedSize = result.length()
                        val reduction = if (originalSize > 0) {
                            ((1 - compressedSize.toDouble() / originalSize) * 100).toInt()
                        } else 0

                        AlertDialog.Builder(requireContext())
                            .setTitle("PDF Comprimido")
                            .setMessage(
                                "Original: ${fileManager.formatFileSize(originalSize)}\n" +
                                "Comprimido: ${fileManager.formatFileSize(compressedSize)}\n" +
                                "Reduccion: $reduction%"
                            )
                            .setPositiveButton("Abrir") { _, _ -> openFile(result) }
                            .setNegativeButton("Cerrar", null)
                            .show()
                    } else {
                        showToast("Error al comprimir el PDF")
                    }
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    pd.dismiss()
                    showToast("Error: ${e.message}")
                }
            }
        }.start()
    }

    private fun showSignDialog(uri: Uri) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 24)
        }
        val input = EditText(requireContext()).apply {
            hint = "Ingrese su firma"; inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        layout.addView(TextView(requireContext()).apply {
            text = "PDF: ${getFileName(uri)}"; textSize = 14f; setPadding(0, 0, 0, 16)
        })
        layout.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle("Firmar PDF")
            .setView(layout)
            .setPositiveButton("Firmar") { _, _ ->
                val sig = input.text.toString()
                if (sig.isNotBlank()) signPDF(uri, sig) else showToast("Ingrese una firma")
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun signPDF(uri: Uri, signature: String) {
        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Firmando PDF").setMessage("Agregando firma...").setCancelable(false).create()
        pd.show()
        Thread {
            val result = pdfSigner.signPDF(uri, signature, 0, "firmado_${System.currentTimeMillis()}.pdf")
            requireActivity().runOnUiThread {
                pd.dismiss()
                if (result != null) showSuccessDialog(result, "PDF firmado exitosamente")
                else showToast("Error al firmar el PDF")
            }
        }.start()
    }

    private fun showSplitOptions(uri: Uri) {
        val pageCount = pdfSplitter.getPageCount(uri)
        if (pageCount <= 1) { showToast("El PDF tiene solo 1 pagina"); return }
        AlertDialog.Builder(requireContext())
            .setTitle("Dividir PDF - ${getFileName(uri)}")
            .setMessage("Total de paginas: $pageCount\n\nSeleccione una opcion:")
            .setPositiveButton("Partes iguales") { _, _ -> showDivideInParts(uri, pageCount) }
            .setNeutralButton("Rango") { _, _ -> showRangeSelector(uri, pageCount) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun showDivideInParts(uri: Uri, totalPages: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Dividir en partes iguales")
            .setItems(arrayOf("2", "3", "4", "5", "10")) { _, w ->
                splitInParts(uri, arrayOf("2","3","4","5","10")[w].toInt())
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun showRangeSelector(uri: Uri, totalPages: Int) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 24)
        }
        val fromInput = EditText(requireContext()).apply {
            hint = "Desde pagina (1-$totalPages)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val toInput = EditText(requireContext()).apply {
            hint = "Hasta pagina (1-$totalPages)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(TextView(requireContext()).apply {
            text = "Total de paginas: $totalPages"; textSize = 14f; setPadding(0,0,0,16)
        })
        layout.addView(fromInput); layout.addView(toInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Extraer rango de paginas").setView(layout)
            .setPositiveButton("Extraer") { _, _ ->
                val from = (fromInput.text.toString().toIntOrNull() ?: 1).coerceIn(1, totalPages)
                val to = (toInput.text.toString().toIntOrNull() ?: totalPages).coerceIn(from, totalPages)
                val pages = (from - 1 until to).toList()
                if (pages.isNotEmpty()) splitSelectedPages(uri, pages)
                else showToast("Rango invalido")
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun splitSelectedPages(uri: Uri, pages: List<Int>) {
        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Dividiendo PDF").setMessage("Extrayendo paginas...").setCancelable(false).create()
        pd.show()
        Thread {
            val result = pdfSplitter.splitPDF(uri, pages, "dividido_${System.currentTimeMillis()}.pdf")
            requireActivity().runOnUiThread {
                pd.dismiss()
                if (result != null) showSuccessDialog(result, "PDF dividido exitosamente")
                else showToast("Error al dividir el PDF")
            }
        }.start()
    }

    private fun splitInParts(uri: Uri, parts: Int) {
        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Dividiendo PDF").setMessage("Dividiendo...").setCancelable(false).create()
        pd.show()
        Thread {
            val results = pdfSplitter.splitPDFInParts(uri, parts)
            requireActivity().runOnUiThread {
                pd.dismiss()
                if (results.isNotEmpty()) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("PDF dividido exitosamente")
                        .setMessage("Se crearon ${results.size} archivos:\n\n${results.joinToString("\n") { it.name }}")
                        .setPositiveButton("Aceptar", null).show()
                } else showToast("Error al dividir el PDF")
            }
        }.start()
    }

    private fun showMergeConfirmation(uris: List<Uri>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Unir PDFs")
            .setMessage("Se uniran ${uris.size} archivos:\n\n${uris.joinToString("\n") { getFileName(it) }}")
            .setPositiveButton("Unir") { _, _ -> mergePDFs(uris) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun mergePDFs(uris: List<Uri>) {
        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Uniendo PDFs").setMessage("Procesando...").setCancelable(false).create()
        pd.show()
        Thread {
            val result = pdfMerger.mergePDFs(uris, "unido_${System.currentTimeMillis()}.pdf")
            requireActivity().runOnUiThread {
                pd.dismiss()
                if (result != null) showSuccessDialog(result, "PDF unido exitosamente")
                else showToast("Error al unir los PDFs")
            }
        }.start()
    }

    private fun showSuccessDialog(file: File, title: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage("Archivo: ${file.name}\nTamano: ${fileManager.formatFileSize(file.length())}")
            .setPositiveButton("Abrir") { _, _ -> openFile(file) }
            .setNegativeButton("Cerrar", null).show()
    }

    private fun openFile(file: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(), "${requireContext().packageName}.provider", file
            )
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) { showToast("No hay aplicacion para abrir PDF") }
    }

    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        context?.contentResolver?.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val i = it.getColumnIndex(OpenableColumns.SIZE)
                if (i >= 0) size = it.getLong(i)
            }
        }
        return size
    }

    private fun getFileName(uri: Uri): String {
        var name = "desconocido"
        context?.contentResolver?.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val i = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) name = it.getString(i)
            }
        }
        return name
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
package com.zeus.suite.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.zeus.suite.R
import com.zeus.suite.automation.BatchPDFCreator
import com.zeus.suite.automation.CSVToReport
import com.zeus.suite.automation.ExcelToPDF
import com.zeus.suite.automation.InvoiceAnalyzer
import com.zeus.suite.automation.StatementAnalyzer
import com.zeus.suite.bigdata.CSVHandler
import com.zeus.suite.bigdata.ExcelHandler
import com.zeus.suite.pdf.PDFMerger
import com.zeus.suite.utils.FileManager
import java.text.NumberFormat
import java.util.Locale

class AutoFragment : Fragment() {

    companion object {
        fun newInstance(): AutoFragment = AutoFragment()
    }

    private lateinit var fileManager: FileManager
    private lateinit var csvHandler: CSVHandler
    private lateinit var excelHandler: ExcelHandler
    private lateinit var excelToPDF: ExcelToPDF
    private lateinit var csvToReport: CSVToReport
    private lateinit var batchPDFCreator: BatchPDFCreator
    private lateinit var invoiceAnalyzer: InvoiceAnalyzer
    private lateinit var statementAnalyzer: StatementAnalyzer
    private lateinit var pdfMerger: PDFMerger
    private var pendingAction: String = ""

    private val singleFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            when (pendingAction) {
                "excelToPdf" -> convertExcelToPdf(uri)
                "csvToReport" -> convertCsvToReport(uri)
                "invoice" -> analyzeInvoices(uri)
                "statement" -> analyzeStatements(uri)
            }
        }
    }

    private val multipleFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty() && pendingAction == "batch") {
            createBatchPDFs(uris)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_auto, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fileManager = FileManager(requireContext())
        csvHandler = CSVHandler(requireContext())
        excelHandler = ExcelHandler(requireContext())
        excelToPDF = ExcelToPDF(requireContext())
        csvToReport = CSVToReport(requireContext())
        batchPDFCreator = BatchPDFCreator(requireContext())
        invoiceAnalyzer = InvoiceAnalyzer(requireContext())
        statementAnalyzer = StatementAnalyzer(requireContext())
        pdfMerger = PDFMerger(requireContext())

        view.findViewById<View>(R.id.cardExcelToPdf)?.setOnClickListener {
            pendingAction = "excelToPdf"
            openSingleFilePicker(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel", "*/*"))
        }
        view.findViewById<View>(R.id.cardCsvToReport)?.setOnClickListener {
            pendingAction = "csvToReport"
            openSingleFilePicker(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
        }
        view.findViewById<View>(R.id.cardBatchPdf)?.setOnClickListener {
            pendingAction = "batch"
            try { multipleFileLauncher.launch(arrayOf("application/pdf")) }
            catch (e: Exception) { toast("Error al abrir selector") }
        }
        view.findViewById<View>(R.id.cardInvoice)?.setOnClickListener {
            pendingAction = "invoice"
            openSingleFilePicker(arrayOf("text/csv", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel", "*/*"))
        }
        view.findViewById<View>(R.id.cardStatement)?.setOnClickListener {
            pendingAction = "statement"
            openSingleFilePicker(arrayOf("text/csv", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel", "*/*"))
        }
    }

    private fun openSingleFilePicker(mimeTypes: Array<String>) {
        try { singleFileLauncher.launch(mimeTypes) }
        catch (e: Exception) { toast("Error al abrir selector") }
    }

    // EXCEL A PDF
    private fun convertExcelToPdf(uri: Uri) {
        val pd = progressDialog("Convirtiendo Excel a PDF..."); pd.show()
        Thread {
            val result = excelToPDF.convertExcelToPDF(uri)
            requireActivity().runOnUiThread {
                pd.dismiss()
                if (result != null) showSuccessDialog(result, "Excel convertido a PDF")
                else toast("Error al convertir")
            }
        }.start()
    }

    // CSV A REPORTE
    private fun convertCsvToReport(uri: Uri) {
        AlertDialog.Builder(requireContext())
            .setTitle("CSV a Reporte")
            .setMessage("Seleccione tipo de reporte:")
            .setPositiveButton("Reporte Completo") { _, _ -> generateFullReport(uri) }
            .setNeutralButton("Resumen") { _, _ -> generateSummary(uri) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun generateFullReport(uri: Uri) {
        val pd = progressDialog("Generando reporte..."); pd.show()
        Thread {
            val result = csvToReport.convertCSVToReport(uri)
            requireActivity().runOnUiThread {
                pd.dismiss()
                if (result != null) showTextFileDialog(result, "Reporte generado")
                else toast("Error al generar reporte")
            }
        }.start()
    }

    private fun generateSummary(uri: Uri) {
        val pd = progressDialog("Generando resumen..."); pd.show()
        Thread {
            val result = csvToReport.convertCSVToSummary(uri)
            requireActivity().runOnUiThread {
                pd.dismiss()
                if (result != null) showTextFileDialog(result, "Resumen generado")
                else toast("Error al generar resumen")
            }
        }.start()
    }

    // LOTE DE PDFs
    private fun createBatchPDFs(uris: List<Uri>) {
        if (uris.size < 2) { toast("Seleccione al menos 2 PDFs"); return }

        val fileNames = uris.joinToString("\n") { getFileName(it) }
        AlertDialog.Builder(requireContext())
            .setTitle("Crear lote de PDFs")
            .setMessage("Se procesaran ${uris.size} archivos:\n\n$fileNames")
            .setPositiveButton("Unir todos") { _, _ -> mergeAllPDFs(uris) }
            .setNeutralButton("Procesar individual") { _, _ -> processBatch(uris) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun mergeAllPDFs(uris: List<Uri>) {
        val pd = progressDialog("Uniendo ${uris.size} PDFs..."); pd.show()
        Thread {
            val result = pdfMerger.mergePDFs(uris, "lote_unido_${System.currentTimeMillis()}.pdf")
            requireActivity().runOnUiThread {
                pd.dismiss()
                if (result != null) showSuccessDialog(result, "PDFs unidos exitosamente")
                else toast("Error al unir PDFs")
            }
        }.start()
    }

    private fun processBatch(uris: List<Uri>) {
        val pd = progressDialog("Procesando ${uris.size} archivos..."); pd.show()
        Thread {
            val results = mutableListOf<java.io.File>()
            for (uri in uris) {
                val result = pdfMerger.mergePDFs(listOf(uri), "procesado_${getFileName(uri)}")
                if (result != null) results.add(result)
            }
            requireActivity().runOnUiThread {
                pd.dismiss()
                if (results.isNotEmpty()) {
                    toast("${results.size} archivos procesados en Download/ZeusSuite/")
                } else toast("Error al procesar")
            }
        }.start()
    }

    // FACTURAS
    private fun analyzeInvoices(uri: Uri) {
        val pd = progressDialog("Analizando facturas..."); pd.show()
        Thread {
            val summary = invoiceAnalyzer.analyzeInvoices(uri)
            val reportFile = invoiceAnalyzer.generateMonthlyReport(uri)
            requireActivity().runOnUiThread {
                pd.dismiss()
                if (summary != null) {
                    val f = NumberFormat.getNumberInstance(Locale.getDefault())
                    val sb = StringBuilder()
                    sb.appendLine("=== ANALISIS DE FACTURAS ===")
                    sb.appendLine("Total facturas: ${f.format(summary.totalInvoices)}")
                    sb.appendLine("Monto total: ${"%.2f".format(summary.totalAmount)}")
                    sb.appendLine("Promedio: ${"%.2f".format(summary.averageAmount)}")
                    sb.appendLine("Factura mas alta: ${summary.maxInvoice.first} (${"%.2f".format(summary.maxInvoice.second)})")
                    sb.appendLine("Factura mas baja: ${summary.minInvoice.first} (${"%.2f".format(summary.minInvoice.second)})")
                    sb.appendLine()
                    if (summary.monthlyTotals.isNotEmpty()) {
                        sb.appendLine("--- Totales Mensuales ---")
                        for ((month, total) in summary.monthlyTotals.toList().sortedBy { it.first }) {
                            sb.appendLine("$month: ${"%.2f".format(total)}")
                        }
                    }
                    showTextResult("Analisis de Facturas", sb.toString())
                } else toast("Error al analizar facturas")
            }
        }.start()
    }

    // ESTADOS DE CUENTA
    private fun analyzeStatements(uri: Uri) {
        val pd = progressDialog("Analizando estados de cuenta..."); pd.show()
        Thread {
            val summary = statementAnalyzer.analyzeStatements(uri)
            val reportFile = statementAnalyzer.generateAnalysisReport(uri)
            requireActivity().runOnUiThread {
                pd.dismiss()
                if (summary != null) {
                    val f = NumberFormat.getNumberInstance(Locale.getDefault())
                    val sb = StringBuilder()
                    sb.appendLine("=== ANALISIS DE ESTADOS DE CUENTA ===")
                    sb.appendLine("Transacciones: ${f.format(summary.totalTransactions)}")
                    sb.appendLine("Creditos: +${"%.2f".format(summary.totalCredits)}")
                    sb.appendLine("Debitos: -${"%.2f".format(summary.totalDebits)}")
                    sb.appendLine("Balance neto: ${"%.2f".format(summary.netBalance)}")
                    sb.appendLine("Promedio: ${"%.2f".format(summary.averageTransaction)}")
                    sb.appendLine("Mayor credito: ${"%.2f".format(summary.largestCredit)}")
                    sb.appendLine("Mayor debito: ${"%.2f".format(summary.largestDebit)}")
                    sb.appendLine()
                    if (summary.categoryTotals.isNotEmpty()) {
                        sb.appendLine("--- Totales por Categoria ---")
                        for ((cat, total) in summary.categoryTotals.toList().sortedByDescending { it.second }) {
                            sb.appendLine("$cat: ${"%.2f".format(total)}")
                        }
                    }
                    showTextResult("Analisis de Estados de Cuenta", sb.toString())
                } else toast("Error al analizar")
            }
        }.start()
    }

    private fun progressDialog(msg: String) = AlertDialog.Builder(requireContext())
        .setTitle("Procesando").setMessage(msg).setCancelable(false).create()

    private fun showSuccessDialog(file: java.io.File, title: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage("Archivo: ${file.name}\nTamano: ${fileManager.formatFileSize(file.length())}")
            .setPositiveButton("Abrir") { _, _ -> openFile(file) }
            .setNegativeButton("Cerrar", null).show()
    }

    private fun showTextFileDialog(file: java.io.File, title: String) {
        val content = try { file.readText() } catch (e: Exception) { "Error al leer archivo" }
        showTextResult(title, content)
    }

    private fun showTextResult(title: String, message: String) {
        val sv = ScrollView(requireContext())
        sv.addView(TextView(requireContext()).apply {
            text = message; textSize = 13f; setTextColor(0xFF1A237E.toInt()); setPadding(24, 16, 24, 16)
        })
        AlertDialog.Builder(requireContext()).setTitle(title).setView(sv)
            .setPositiveButton("Copiar") { _, _ ->
                val cb = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cb.setPrimaryClip(android.content.ClipData.newPlainText("Reporte", message))
                toast("Copiado al portapapeles")
            }
            .setNeutralButton("Compartir") { _, _ ->
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, message)
                }, "Compartir"))
            }
            .setNegativeButton("Cerrar", null).show()
    }

    private fun openFile(file: java.io.File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(), "${requireContext().packageName}.provider", file
            )
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) { toast("No hay aplicacion para abrir PDF") }
    }

    private fun getFileName(uri: Uri): String {
        var name = "desconocido"
        context?.contentResolver?.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) { val i = it.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (i >= 0) name = it.getString(i) }
        }
        return name
    }

    private fun toast(msg: String) { Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show() }
}
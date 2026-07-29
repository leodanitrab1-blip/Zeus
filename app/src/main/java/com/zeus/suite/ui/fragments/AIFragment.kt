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
import com.zeus.suite.ai.AIProcessor
import com.zeus.suite.ai.AnomalyDetector
import com.zeus.suite.ai.DataAnalyzer
import com.zeus.suite.ai.ReportGenerator
import com.zeus.suite.bigdata.CSVHandler
import com.zeus.suite.bigdata.ExcelHandler
import com.zeus.suite.pdf.FormFiller
import java.text.NumberFormat
import java.util.Locale

class AIFragment : Fragment() {

    companion object {
        fun newInstance(): AIFragment = AIFragment()
    }

    private lateinit var csvHandler: CSVHandler
    private lateinit var excelHandler: ExcelHandler
    private lateinit var aiProcessor: AIProcessor
    private lateinit var dataAnalyzer: DataAnalyzer
    private lateinit var reportGenerator: ReportGenerator
    private lateinit var anomalyDetector: AnomalyDetector
    private lateinit var formFiller: FormFiller
    private var currentHeaders: List<String> = emptyList()
    private var currentRows: List<List<String>> = emptyList()
    private var currentFileName: String = ""

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) loadData(uri)
    }

    private val templateFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) showFieldMapping(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_ai, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        csvHandler = CSVHandler(requireContext())
        excelHandler = ExcelHandler(requireContext())
        aiProcessor = AIProcessor()
        dataAnalyzer = DataAnalyzer()
        reportGenerator = ReportGenerator()
        anomalyDetector = AnomalyDetector()
        formFiller = FormFiller(requireContext())

        view.findViewById<View>(R.id.cardQuery)?.setOnClickListener {
            if (currentHeaders.isNotEmpty()) showQueryDialog()
            else askToOpenFile("Consultar")
        }
        view.findViewById<View>(R.id.cardSummary)?.setOnClickListener {
            if (currentHeaders.isNotEmpty()) showSummary()
            else askToOpenFile("Resumir")
        }
        view.findViewById<View>(R.id.cardAnomaly)?.setOnClickListener {
            if (currentHeaders.isNotEmpty()) showAnomalies()
            else askToOpenFile("Anomalias")
        }
        view.findViewById<View>(R.id.cardReport)?.setOnClickListener {
            if (currentHeaders.isNotEmpty()) showReport()
            else askToOpenFile("Reporte")
        }
        view.findViewById<View>(R.id.cardAutoFill)?.setOnClickListener {
            if (currentHeaders.isNotEmpty()) showAutoFillOptions()
            else askToOpenFile("Auto-llenado")
        }
    }

    private fun askToOpenFile(action: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(action)
            .setMessage("Debe abrir un archivo CSV o Excel primero")
            .setPositiveButton("Abrir archivo") { _, _ -> openFilePicker() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openFilePicker() {
        try {
            filePickerLauncher.launch(arrayOf("text/csv", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel", "*/*"))
        } catch (e: Exception) { toast("Error al abrir selector") }
    }

    private fun loadData(uri: Uri) {
        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Cargando datos").setMessage("Leyendo archivo...").setCancelable(false).create()
        pd.show()

        Thread {
            val csvData = csvHandler.readCSV(uri, ",", 1000)
            val data = if (csvData != null && csvData.rows.isNotEmpty()) csvData
            else excelHandler.readExcel(uri, 1000)?.let { CSVHandler.CSVData(it.headers, it.rows) }

            requireActivity().runOnUiThread {
                pd.dismiss()
                if (data != null && data.rows.isNotEmpty()) {
                    currentHeaders = data.headers
                    currentRows = data.rows
                    currentFileName = getFileName(uri)
                    toast("Archivo cargado: $currentFileName (${data.rows.size} filas)")
                } else toast("Error al leer el archivo")
            }
        }.start()
    }

    // CONSULTAR
    private fun showQueryDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Ej: Cual fue el cliente que mas compro?"
            inputType = android.text.InputType.TYPE_CLASS_TEXT; minLines = 2
        }
        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 24) }
        layout.addView(TextView(requireContext()).apply {
            text = "Archivo: $currentFileName\nFilas: ${NumberFormat.getNumberInstance(Locale.getDefault()).format(currentRows.size)}"
            textSize = 13f; setTextColor(0xFF1565C0.toInt()); setPadding(0, 0, 0, 12)
        })
        layout.addView(TextView(requireContext()).apply { text = "Pregunte sobre sus datos:"; textSize = 12f; setPadding(0, 0, 0, 8) })
        layout.addView(input)

        AlertDialog.Builder(requireContext()).setTitle("Consultar Datos").setView(layout)
            .setPositiveButton("Preguntar") { _, _ ->
                val q = input.text.toString()
                if (q.isNotBlank()) processQuery(q) else toast("Escriba una pregunta")
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun processQuery(question: String) {
        val pd = AlertDialog.Builder(requireContext()).setTitle("Procesando").setMessage("Analizando...").setCancelable(false).create()
        pd.show()
        Thread {
            val answer = aiProcessor.answerQuestion(question, currentHeaders, currentRows)
            requireActivity().runOnUiThread { pd.dismiss(); showResultDialog("Respuesta", answer) }
        }.start()
    }

    // RESUMEN
    private fun showSummary() {
        val pd = AlertDialog.Builder(requireContext()).setTitle("Procesando").setMessage("Generando resumen...").setCancelable(false).create()
        pd.show()
        Thread {
            val summary = aiProcessor.answerQuestion("resumen", currentHeaders, currentRows)
            requireActivity().runOnUiThread { pd.dismiss(); showResultDialog("Resumen de Datos", summary) }
        }.start()
    }

    // ANOMALIAS
    private fun showAnomalies() {
        val pd = AlertDialog.Builder(requireContext()).setTitle("Procesando").setMessage("Detectando anomalias...").setCancelable(false).create()
        pd.show()
        Thread {
            val anomalies = anomalyDetector.detect(currentHeaders, currentRows)
            val summary = anomalyDetector.getAnomalySummary(anomalies)
            val result = if (anomalies.isNotEmpty()) summary + "\n\n" + anomalies.take(15).joinToString("\n")
            else "No se detectaron anomalias en los datos."
            requireActivity().runOnUiThread { pd.dismiss(); showResultDialog("Deteccion de Anomalias", result) }
        }.start()
    }

    // REPORTE
    private fun showReport() {
        val pd = AlertDialog.Builder(requireContext()).setTitle("Procesando").setMessage("Generando reporte...").setCancelable(false).create()
        pd.show()
        Thread {
            val report = reportGenerator.generateReport(currentHeaders, currentRows)
            requireActivity().runOnUiThread { pd.dismiss(); showResultDialog("Reporte Ejecutivo", report) }
        }.start()
    }

    private fun showResultDialog(title: String, message: String) {
        val sv = ScrollView(requireContext())
        val tv = TextView(requireContext()).apply {
            text = message; textSize = 13f; setTextColor(0xFF1A237E.toInt()); setPadding(24, 16, 24, 16)
        }
        sv.addView(tv)
        AlertDialog.Builder(requireContext()).setTitle(title).setView(sv)
            .setPositiveButton("Copiar") { _, _ ->
                val cb = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cb.setPrimaryClip(android.content.ClipData.newPlainText("Reporte", message))
                toast("Copiado al portapapeles")
            }
            .setNeutralButton("Compartir") { _, _ ->
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, message) }, "Compartir"))
            }
            .setNegativeButton("Cerrar", null).show()
    }

    // AUTO-LLENADO
    private fun showAutoFillOptions() {
        AlertDialog.Builder(requireContext())
            .setTitle("Auto-llenado de Documentos")
            .setMessage("Seleccione un PDF con marcadores ({{campo}}) para rellenar con los datos.\n\nArchivo: $currentFileName\nFilas: ${NumberFormat.getNumberInstance(Locale.getDefault()).format(currentRows.size)}")
            .setPositiveButton("Seleccionar PDF") { _, _ ->
                try { templateFileLauncher.launch(arrayOf("application/pdf")) }
                catch (e: Exception) { toast("Error al abrir selector") }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun showFieldMapping(templateUri: Uri) {
        val placeholders = formFiller.extractPlaceholders(templateUri)

        if (placeholders.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Sin marcadores")
                .setMessage("El PDF no contiene marcadores como {{campo}}, \${campo} o [campo]")
                .setPositiveButton("Entendido", null).show()
            return
        }

        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 16) }
        val mappingSpinners = mutableListOf<Pair<String, Spinner>>()

        for (placeholder in placeholders) {
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 8) }
            val label = TextView(requireContext()).apply {
                text = "$placeholder: "; textSize = 13f; setTextColor(0xFF1565C0.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val spinner = Spinner(requireContext()).apply {
                adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("(ignorar)") + currentHeaders)
            }
            row.addView(label); row.addView(spinner); layout.addView(row)
            mappingSpinners.add(placeholder to spinner)
        }

        val sv = ScrollView(requireContext()); sv.addView(layout)

        AlertDialog.Builder(requireContext())
            .setTitle("Mapeo de Campos (${placeholders.size} encontrados)")
            .setMessage("Asigne columnas a cada marcador:")
            .setView(sv)
            .setPositiveButton("Generar Documentos") { _, _ ->
                val fieldMapping = mutableMapOf<String, String>()
                for ((placeholder, spinner) in mappingSpinners) {
                    val selected = spinner.selectedItem.toString()
                    if (selected != "(ignorar)") fieldMapping[placeholder] = selected
                }
                if (fieldMapping.isNotEmpty()) generateDocuments(templateUri, fieldMapping)
                else toast("Seleccione al menos un campo")
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun generateDocuments(templateUri: Uri, fieldMapping: Map<String, String>) {
        val dataList = currentRows.map { row ->
            val map = mutableMapOf<String, String>()
            for ((index, header) in currentHeaders.withIndex()) {
                map[header] = if (index < row.size) row[index] else ""
            }
            map
        }

        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Generando documentos").setMessage("Creando ${dataList.size} PDFs...").setCancelable(false).create()
        pd.show()

        Thread {
            val results = formFiller.fillFormWithData(templateUri, dataList, fieldMapping)
            requireActivity().runOnUiThread {
                pd.dismiss()
                if (results.isNotEmpty()) showAutoFillResult(results)
                else toast("Error al generar documentos")
            }
        }.start()
    }

    private fun showAutoFillResult(files: List<java.io.File>) {
        val sb = StringBuilder()
        sb.appendLine("=== DOCUMENTOS GENERADOS ===")
        sb.appendLine("Total: ${files.size} PDFs\n")
        for ((index, file) in files.withIndex()) sb.appendLine("${index + 1}. ${file.name}")
        sb.appendLine("\nUbicacion: Download/ZeusSuite/")

        val sv = ScrollView(requireContext())
        sv.addView(TextView(requireContext()).apply {
            text = sb.toString(); textSize = 13f; setTextColor(0xFF1A237E.toInt()); setPadding(24, 16, 24, 16)
        })

        AlertDialog.Builder(requireContext())
            .setTitle("Auto-llenado Completado").setView(sv)
            .setPositiveButton("Abrir carpeta") { _, _ ->
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", files[0])
                    startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf"); flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    })
                } catch (e: Exception) { toast("No hay aplicacion para abrir PDF") }
            }
            .setNegativeButton("Cerrar", null).show()
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
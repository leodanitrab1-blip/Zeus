package com.zeus.suite.ui.fragments

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
    private var currentHeaders: List<String> = emptyList()
    private var currentRows: List<List<String>> = emptyList()
    private var currentFileName: String = ""

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) loadData(uri)
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
            toast("Auto-llenado - Proximamente")
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
        } catch (e: Exception) {
            toast("Error al abrir selector")
        }
    }

    private fun loadData(uri: Uri) {
        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Cargando datos")
            .setMessage("Leyendo archivo...")
            .setCancelable(false)
            .create()
        pd.show()

        Thread {
            val csvData = csvHandler.readCSV(uri, ",", 1000)
            val data = if (csvData != null && csvData.rows.isNotEmpty()) {
                csvData
            } else {
                excelHandler.readExcel(uri, 1000)?.let {
                    CSVHandler.CSVData(it.headers, it.rows)
                }
            }

            requireActivity().runOnUiThread {
                pd.dismiss()
                if (data != null && data.rows.isNotEmpty()) {
                    currentHeaders = data.headers
                    currentRows = data.rows
                    currentFileName = getFileName(uri)
                    toast("Archivo cargado: $currentFileName (${data.rows.size} filas)")
                } else {
                    toast("Error al leer el archivo")
                }
            }
        }.start()
    }

    private fun showQueryDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Ej: Cual fue el cliente que mas compro?"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            minLines = 2
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 24)
        }
        layout.addView(TextView(requireContext()).apply {
            text = "Archivo: $currentFileName\nFilas: ${NumberFormat.getNumberInstance(Locale.getDefault()).format(currentRows.size)}"
            textSize = 13f; setTextColor(0xFF1565C0.toInt()); setPadding(0, 0, 0, 12)
        })
        layout.addView(TextView(requireContext()).apply {
            text = "Pregunte sobre sus datos:"
            textSize = 12f; setPadding(0, 0, 0, 8)
        })
        layout.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle("Consultar Datos")
            .setView(layout)
            .setPositiveButton("Preguntar") { _, _ ->
                val question = input.text.toString()
                if (question.isNotBlank()) processQuery(question)
                else toast("Escriba una pregunta")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun processQuery(question: String) {
        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Procesando")
            .setMessage("Analizando datos...")
            .setCancelable(false)
            .create()
        pd.show()

        Thread {
            val answer = aiProcessor.answerQuestion(question, currentHeaders, currentRows)
            requireActivity().runOnUiThread {
                pd.dismiss()
                showResultDialog("Respuesta", answer)
            }
        }.start()
    }

    private fun showSummary() {
        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Procesando")
            .setMessage("Generando resumen...")
            .setCancelable(false)
            .create()
        pd.show()

        Thread {
            val summary = aiProcessor.answerQuestion("resumen", currentHeaders, currentRows)
            requireActivity().runOnUiThread {
                pd.dismiss()
                showResultDialog("Resumen de Datos", summary)
            }
        }.start()
    }

    private fun showAnomalies() {
        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Procesando")
            .setMessage("Detectando anomalias...")
            .setCancelable(false)
            .create()
        pd.show()

        Thread {
            val anomalies = anomalyDetector.detect(currentHeaders, currentRows)
            val summary = anomalyDetector.getAnomalySummary(anomalies)
            val result = if (anomalies.isNotEmpty()) {
                summary + "\n\n" + anomalies.take(15).joinToString("\n")
            } else {
                "No se detectaron anomalias en los datos."
            }
            requireActivity().runOnUiThread {
                pd.dismiss()
                showResultDialog("Deteccion de Anomalias", result)
            }
        }.start()
    }

    private fun showReport() {
        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Procesando")
            .setMessage("Generando reporte ejecutivo...")
            .setCancelable(false)
            .create()
        pd.show()

        Thread {
            val report = reportGenerator.generateReport(currentHeaders, currentRows)
            requireActivity().runOnUiThread {
                pd.dismiss()
                showResultDialog("Reporte Ejecutivo", report)
            }
        }.start()
    }

    private fun showResultDialog(title: String, message: String) {
        val scrollView = ScrollView(requireContext())
        val textView = TextView(requireContext()).apply {
            text = message; textSize = 13f; setTextColor(0xFF1A237E.toInt())
            setPadding(24, 16, 24, 16)
        }
        scrollView.addView(textView)

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(scrollView)
            .setPositiveButton("Copiar") { _, _ ->
                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Reporte", message))
                toast("Copiado al portapapeles")
            }
            .setNeutralButton("Compartir") { _, _ ->
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, message)
                }
                startActivity(android.content.Intent.createChooser(intent, "Compartir"))
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun getFileName(uri: Uri): String {
        var name = "desconocido"
        context?.contentResolver?.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) { val i = it.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (i >= 0) name = it.getString(i) }
        }
        return name
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
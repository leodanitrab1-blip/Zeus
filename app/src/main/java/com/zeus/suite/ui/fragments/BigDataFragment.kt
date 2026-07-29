package com.zeus.suite.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.zeus.suite.R
import com.zeus.suite.bigdata.CSVHandler
import java.text.NumberFormat
import java.util.Locale

class BigDataFragment : Fragment() {

    companion object {
        fun newInstance(): BigDataFragment = BigDataFragment()
        private const val MAX_DISPLAY_ROWS = 500
    }

    private lateinit var csvHandler: CSVHandler
    private var currentData: CSVHandler.CSVData? = null
    private var currentUri: Uri? = null
    private var pendingAction: String = ""

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            when (pendingAction) {
                "csv" -> {
                    currentUri = uri
                    showLoadingPreview(uri)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_bigdata, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        csvHandler = CSVHandler(requireContext())
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.cardCSV)?.setOnClickListener {
            pendingAction = "csv"
            openFilePicker()
        }
        view.findViewById<View>(R.id.cardExcel)?.setOnClickListener {
            showToast("Abrir Excel - Proximamente")
        }
        view.findViewById<View>(R.id.cardFilter)?.setOnClickListener {
            if (currentData != null) showFilterDialog()
            else showToast("Abra un archivo primero")
        }
        view.findViewById<View>(R.id.cardSearch)?.setOnClickListener {
            if (currentData != null) showSearchDialog()
            else showToast("Abra un archivo primero")
        }
        view.findViewById<View>(R.id.cardChart)?.setOnClickListener {
            showToast("Graficar - Proximamente")
        }
        view.findViewById<View>(R.id.cardStats)?.setOnClickListener {
            if (currentData != null) showStats()
            else showToast("Abra un archivo primero")
        }
    }

    private fun openFilePicker() {
        try {
            filePickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv", "*/*"))
        } catch (e: Exception) {
            showToast("Error al abrir selector de archivos")
        }
    }

    private fun showLoadingPreview(uri: Uri) {
        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Cargando CSV")
            .setMessage("Leyendo archivo...")
            .setCancelable(false)
            .create()
        pd.show()

        Thread {
            val data = csvHandler.readCSV(uri)

            requireActivity().runOnUiThread {
                pd.dismiss()

                if (data != null && data.rows.isNotEmpty()) {
                    currentData = data
                    showDataPreview(getFileName(uri), data)
                } else {
                    showToast("Error al leer el archivo CSV")
                }
            }
        }.start()
    }

    private fun showDataPreview(fileName: String, data: CSVHandler.CSVData) {
        val totalRows = data.rows.size
        val totalCols = data.headers.size
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())

        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Info header
        val infoText = TextView(requireContext()).apply {
            text = "Archivo: $fileName\nFilas: ${formatter.format(totalRows)} | Columnas: $totalCols\nMostrando primeras $MAX_DISPLAY_ROWS filas"
            textSize = 13f
            setTextColor(0xFF1565C0.toInt())
            setPadding(0, 0, 0, 12)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        mainLayout.addView(infoText)

        // Horizontal scroll for wide tables
        val horizontalScroll = HorizontalScrollView(requireContext())
        val verticalScroll = ScrollView(requireContext())

        val tableLayout = TableLayout(requireContext()).apply {
            isShrinkAllColumns = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Header row
        val headerRow = TableRow(requireContext()).apply {
            setBackgroundColor(0xFF1565C0.toInt())
            setPadding(4, 10, 4, 10)
        }
        for (header in data.headers) {
            val tv = createCell(header, 12f, 0xFFFFFFFF.toInt(), true)
            headerRow.addView(tv)
        }
        tableLayout.addView(headerRow)

        // Data rows
        val displayRows = minOf(totalRows, MAX_DISPLAY_ROWS)
        for (i in 0 until displayRows) {
            val row = data.rows[i]
            val tableRow = TableRow(requireContext()).apply {
                setPadding(4, 2, 4, 2)
                if (i % 2 == 0) setBackgroundColor(0xFFF5F9FF.toInt())
                else setBackgroundColor(0xFFFFFFFF.toInt())
            }
            for (cell in row) {
                val tv = createCell(cell, 11f, 0xFF1A237E.toInt(), false)
                tableRow.addView(tv)
            }
            tableLayout.addView(tableRow)
        }

        verticalScroll.addView(tableLayout)
        horizontalScroll.addView(verticalScroll)
        mainLayout.addView(horizontalScroll)

        // Buttons
        val buttonLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 0)
        }

        if (totalRows > MAX_DISPLAY_ROWS) {
            val loadMoreBtn = Button(requireContext()).apply {
                text = "Ver más (${formatter.format(totalRows - MAX_DISPLAY_ROWS)} restantes)"
                setBackgroundColor(0xFF1565C0.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                setOnClickListener { showAllDataDialog(fileName, data) }
            }
            buttonLayout.addView(loadMoreBtn)
        }

        mainLayout.addView(buttonLayout)

        AlertDialog.Builder(requireContext())
            .setTitle("Vista previa CSV")
            .setView(mainLayout)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun showAllDataDialog(fileName: String, data: CSVHandler.CSVData) {
        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Cargando todos los datos")
            .setMessage("Preparando ${NumberFormat.getNumberInstance(Locale.getDefault()).format(data.rows.size)} filas...")
            .setCancelable(false)
            .create()
        pd.show()

        Thread {
            requireActivity().runOnUiThread {
                pd.dismiss()
                showDataPreview(fileName, data)
            }
        }.start()
    }

    private fun createCell(text: String, size: Float, color: Int, bold: Boolean): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            setPadding(16, 8, 16, 8)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            minWidth = 80
            maxWidth = 400
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        }
    }

    private fun showFilterDialog() {
        val data = currentData ?: return
        val columns = data.headers.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por columna")
            .setItems(columns) { _, which ->
                showFilterValueDialog(which, columns[which])
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showFilterValueDialog(colIndex: Int, colName: String) {
        val data = currentData ?: return

        val values = mutableSetOf<String>()
        for (row in data.rows) {
            if (colIndex < row.size) values.add(row[colIndex])
        }
        val uniqueValues = values.take(50).toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar: $colName")
            .setItems(uniqueValues) { _, which ->
                val filtered = data.rows.filter {
                    colIndex < it.size && it[colIndex] == uniqueValues[which]
                }
                val filteredData = CSVHandler.CSVData(data.headers, filtered)
                showDataPreview("${getFileName(currentUri!!)} (filtrado)", filteredData)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSearchDialog() {
        val data = currentData ?: return
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Texto a buscar..."
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Buscar en datos")
            .setView(input)
            .setPositiveButton("Buscar") { _, _ ->
                val query = input.text.toString().lowercase()
                if (query.isNotBlank()) {
                    val results = data.rows.filter { row ->
                        row.any { it.lowercase().contains(query) }
                    }
                    if (results.isNotEmpty()) {
                        val resultData = CSVHandler.CSVData(data.headers, results)
                        showDataPreview("Resultados: $query", resultData)
                    } else {
                        showToast("No se encontraron resultados")
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showStats() {
        val data = currentData ?: return
        val sb = StringBuilder()
        sb.appendLine("=== ESTADISTICAS ===")
        sb.appendLine("Filas: ${NumberFormat.getNumberInstance(Locale.getDefault()).format(data.rows.size)}")
        sb.appendLine("Columnas: ${data.headers.size}")
        sb.appendLine()

        for ((index, header) in data.headers.withIndex()) {
            val uniqueCount = data.rows.mapNotNull { if (index < it.size) it[index] else null }.toSet().size
            val emptyCount = data.rows.count { index >= it.size || it[index].isBlank() }
            sb.appendLine("$header:")
            sb.appendLine("  Valores unicos: ${NumberFormat.getNumberInstance(Locale.getDefault()).format(uniqueCount)}")
            sb.appendLine("  Vacios: ${NumberFormat.getNumberInstance(Locale.getDefault()).format(emptyCount)}")
            sb.appendLine()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Estadisticas del archivo")
            .setMessage(sb.toString())
            .setPositiveButton("Cerrar", null)
            .show()
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
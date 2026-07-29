package com.zeus.suite.ui.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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
        private const val BATCH_SIZE = 500
    }

    private lateinit var csvHandler: CSVHandler
    private var currentUri: Uri? = null
    private var totalRows = 0
    private var currentOffset = 0
    private var headers: List<String> = emptyList()
    private var pendingAction: String = ""
    private var isLoading = false

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && pendingAction == "csv") {
            currentUri = uri
            currentOffset = 0
            totalRows = 0
            headers = emptyList()
            countAndLoad(uri)
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
            pendingAction = "csv"; openFilePicker()
        }
        view.findViewById<View>(R.id.cardExcel)?.setOnClickListener {
            showToast("Abrir Excel - Proximamente")
        }
        view.findViewById<View>(R.id.cardFilter)?.setOnClickListener {
            if (headers.isNotEmpty()) showFilterDialog()
            else showToast("Abra un archivo primero")
        }
        view.findViewById<View>(R.id.cardSearch)?.setOnClickListener {
            if (headers.isNotEmpty()) showSearchDialog()
            else showToast("Abra un archivo primero")
        }
        view.findViewById<View>(R.id.cardChart)?.setOnClickListener {
            showToast("Graficar - Proximamente")
        }
        view.findViewById<View>(R.id.cardStats)?.setOnClickListener {
            if (headers.isNotEmpty()) showStats()
            else showToast("Abra un archivo primero")
        }
    }

    private fun openFilePicker() {
        try {
            filePickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
        } catch (e: Exception) {
            showToast("Error al abrir selector")
        }
    }

    private fun countAndLoad(uri: Uri) {
        if (isLoading) return
        isLoading = true

        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Abriendo archivo")
            .setMessage("Contando filas...")
            .setCancelable(false)
            .create()
        pd.show()

        Thread {
            totalRows = csvHandler.getRowCount(uri)

            requireActivity().runOnUiThread {
                pd.dismiss()
                loadBatch(uri, 0)
                isLoading = false
            }
        }.start()
    }

    private fun loadBatch(uri: Uri, offset: Int) {
        if (isLoading) return
        isLoading = true

        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())

        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Cargando datos")
            .setMessage("Filas ${formatter.format(offset + 1)} - ${formatter.format(offset + BATCH_SIZE)} de ${formatter.format(totalRows)}")
            .setCancelable(false)
            .create()
        pd.show()

        Thread {
            val data = csvHandler.readCSVBatch(uri, offset, BATCH_SIZE)

            requireActivity().runOnUiThread {
                pd.dismiss()
                isLoading = false

                if (data != null && data.rows.isNotEmpty()) {
                    headers = data.headers
                    currentOffset = offset
                    showDataTable(getFileName(uri), data)
                } else if (offset == 0) {
                    showToast("Error al leer el archivo")
                } else {
                    showToast("No hay mas datos")
                }
            }
        }.start()
    }

    private fun showDataTable(fileName: String, data: CSVHandler.CSVData) {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
        val endRow = minOf(currentOffset + data.rows.size, totalRows)

        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Info header
        val infoLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 12)
        }

        val infoText = TextView(requireContext()).apply {
            text = "Archivo: $fileName"
            textSize = 14f
            setTextColor(0xFF1565C0.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        infoLayout.addView(infoText)

        val detailText = TextView(requireContext()).apply {
            text = "Total: ${formatter.format(totalRows)} filas | ${data.headers.size} columnas"
            textSize = 12f
            setTextColor(0xFF5C6BC0.toInt())
        }
        infoLayout.addView(detailText)

        val rangeText = TextView(requireContext()).apply {
            text = "Mostrando: ${formatter.format(currentOffset + 1)} - ${formatter.format(endRow)}"
            textSize = 12f
            setTextColor(0xFF0D47A1.toInt())
        }
        infoLayout.addView(rangeText)

        mainLayout.addView(infoLayout)

        // Navigation buttons on top
        val navLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 8)
        }

        if (currentOffset > 0) {
            val prevBtn = Button(requireContext()).apply {
                text = "<< Anterior ($BATCH_SIZE)"
                setBackgroundColor(0xFF1565C0.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 12f
                setOnClickListener { currentUri?.let { loadBatch(it, maxOf(0, currentOffset - BATCH_SIZE)) } }
            }
            navLayout.addView(prevBtn)
        }

        // Spacer
        if (currentOffset > 0 && currentOffset + BATCH_SIZE < totalRows) {
            val spacer = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(8, 1)
            }
            navLayout.addView(spacer)
        }

        if (currentOffset + BATCH_SIZE < totalRows) {
            val remaining = totalRows - currentOffset - BATCH_SIZE
            val nextBtn = Button(requireContext()).apply {
                text = "Siguiente (${formatter.format(remaining)}) >>"
                setBackgroundColor(0xFF1565C0.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 12f
                setOnClickListener { currentUri?.let { loadBatch(it, currentOffset + BATCH_SIZE) } }
            }
            navLayout.addView(nextBtn)
        }

        if (navLayout.childCount > 0) {
            mainLayout.addView(navLayout)
        }

        // Table
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
            tv.setOnLongClickListener {
                copyToClipboard(header)
                showToast("Copiado: $header")
                true
            }
            headerRow.addView(tv)
        }
        tableLayout.addView(headerRow)

        // Data rows
        for ((index, row) in data.rows.withIndex()) {
            val tableRow = TableRow(requireContext()).apply {
                setPadding(4, 2, 4, 2)
                if (index % 2 == 0) setBackgroundColor(0xFFF5F9FF.toInt())
                else setBackgroundColor(0xFFFFFFFF.toInt())
            }
            for (cell in row) {
                val tv = createCell(cell, 11f, 0xFF1A237E.toInt(), false)
                tv.setOnLongClickListener {
                    copyToClipboard(cell)
                    showToast("Copiado: $cell")
                    true
                }
                tableRow.addView(tv)
            }
            tableLayout.addView(tableRow)
        }

        verticalScroll.addView(tableLayout)
        horizontalScroll.addView(verticalScroll)
        mainLayout.addView(horizontalScroll)

        // Bottom navigation
        val bottomNav = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 0)
        }

        if (currentOffset > 0) {
            val prevBtn2 = Button(requireContext()).apply {
                text = "<< Anterior"
                setBackgroundColor(0xFF1565C0.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 12f
                setOnClickListener { currentUri?.let { loadBatch(it, maxOf(0, currentOffset - BATCH_SIZE)) } }
            }
            bottomNav.addView(prevBtn2)
        }

        if (currentOffset + BATCH_SIZE < totalRows) {
            val nextBtn2 = Button(requireContext()).apply {
                text = "Siguiente >>"
                setBackgroundColor(0xFF1565C0.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 12f
                setOnClickListener { currentUri?.let { loadBatch(it, currentOffset + BATCH_SIZE) } }
            }
            bottomNav.addView(nextBtn2)
        }

        if (bottomNav.childCount > 0) {
            mainLayout.addView(bottomNav)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Datos CSV")
            .setView(mainLayout)
            .setPositiveButton("Cerrar", null)
            .show()
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

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("CSV Data", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun showFilterDialog() {
        val columns = headers.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por columna")
            .setItems(columns) { _, which -> showFilterValueDialog(which, columns[which]) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showFilterValueDialog(colIndex: Int, colName: String) {
        val data = csvHandler.readCSVBatch(currentUri!!, 0, BATCH_SIZE) ?: return
        val values = mutableSetOf<String>()
        for (row in data.rows) {
            if (colIndex < row.size) values.add(row[colIndex])
        }
        val uniqueValues = values.take(50).toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar: $colName")
            .setItems(uniqueValues) { _, which ->
                val filtered = data.rows.filter { colIndex < it.size && it[colIndex] == uniqueValues[which] }
                val filteredData = CSVHandler.CSVData(data.headers, filtered)
                showDataTable("${getFileName(currentUri!!)} (filtrado)", filteredData)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSearchDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Texto a buscar..."
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Buscar en datos")
            .setView(input)
            .setPositiveButton("Buscar") { _, _ ->
                val query = input.text.toString().lowercase()
                if (query.isNotBlank()) {
                    val data = csvHandler.readCSVBatch(currentUri!!, 0, BATCH_SIZE) ?: return@setPositiveButton
                    val results = data.rows.filter { row -> row.any { it.lowercase().contains(query) } }
                    if (results.isNotEmpty()) {
                        val resultData = CSVHandler.CSVData(data.headers, results)
                        showDataTable("Resultados: $query", resultData)
                    } else {
                        showToast("No se encontraron resultados")
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showStats() {
        val data = csvHandler.readCSVBatch(currentUri!!, 0, BATCH_SIZE) ?: return
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
        val sb = StringBuilder()
        sb.appendLine("=== ESTADISTICAS ===")
        sb.appendLine("Total filas: ${formatter.format(totalRows)}")
        sb.appendLine("Columnas: ${data.headers.size}")
        sb.appendLine()

        for ((index, header) in data.headers.withIndex()) {
            val uniqueCount = data.rows.mapNotNull { if (index < it.size) it[index] else null }.toSet().size
            val emptyCount = data.rows.count { index >= it.size || it[index].isBlank() }
            sb.appendLine("$header:")
            sb.appendLine("  Valores unicos: ${formatter.format(uniqueCount)}")
            sb.appendLine("  Vacios: ${formatter.format(emptyCount)}")
            sb.appendLine()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Estadisticas")
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
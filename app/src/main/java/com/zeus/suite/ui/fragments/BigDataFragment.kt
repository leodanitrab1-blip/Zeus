package com.zeus.suite.ui.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
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
import com.zeus.suite.bigdata.ChartGenerator
import com.zeus.suite.bigdata.DataFilter
import com.zeus.suite.bigdata.ExcelHandler
import java.text.NumberFormat
import java.util.Locale

class BigDataFragment : Fragment() {

    companion object {
        fun newInstance(): BigDataFragment = BigDataFragment()
        private const val BATCH_SIZE = 500
    }

    private lateinit var csvHandler: CSVHandler
    private lateinit var excelHandler: ExcelHandler
    private var currentUri: Uri? = null
    private var currentOffset = 0
    private var headers: List<String> = emptyList()
    private var pendingAction: String = ""
    private var hasMoreData = true
    private var isLoading = false
    private var isExcel = false

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            currentUri = uri
            currentOffset = 0
            headers = emptyList()
            hasMoreData = true
            when (pendingAction) {
                "csv" -> { isExcel = false; loadCSV(uri) }
                "excel" -> { isExcel = true; loadExcel(uri) }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_bigdata, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        csvHandler = CSVHandler(requireContext())
        excelHandler = ExcelHandler(requireContext())
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.cardCSV)?.setOnClickListener {
            pendingAction = "csv"
            openFilePicker(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
        }
        view.findViewById<View>(R.id.cardExcel)?.setOnClickListener {
            pendingAction = "excel"
            openFilePicker(arrayOf(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel",
                "*/*"
            ))
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
            if (headers.isNotEmpty()) showChartOptions()
            else showToast("Abra un archivo primero")
        }
        view.findViewById<View>(R.id.cardStats)?.setOnClickListener {
            if (headers.isNotEmpty()) showStats()
            else showToast("Abra un archivo primero")
        }
    }

    private fun openFilePicker(mimeTypes: Array<String>) {
        try { filePickerLauncher.launch(mimeTypes) }
        catch (e: Exception) { showToast("Error al abrir selector") }
    }

    private fun loadCSV(uri: Uri) {
        if (isLoading) return
        isLoading = true
        val pd = createProgressDialog("Cargando CSV...")
        pd.show()

        Thread {
            val data = csvHandler.readCSVBatch(uri, currentOffset, BATCH_SIZE)
            requireActivity().runOnUiThread {
                pd.dismiss(); isLoading = false
                handleDataResult(data)
            }
        }.start()
    }

    private fun loadExcel(uri: Uri) {
        if (isLoading) return
        isLoading = true
        val pd = createProgressDialog("Cargando Excel...")
        pd.show()

        Thread {
            val data = excelHandler.readExcelBatch(uri, currentOffset, BATCH_SIZE)
            requireActivity().runOnUiThread {
                pd.dismiss(); isLoading = false
                handleDataResult(data?.let { CSVHandler.CSVData(it.headers, it.rows) })
            }
        }.start()
    }

    private fun handleDataResult(data: CSVHandler.CSVData?) {
        if (data != null && data.rows.isNotEmpty()) {
            headers = data.headers
            hasMoreData = data.rows.size >= BATCH_SIZE
            showDataTable(getFileName(currentUri!!), data)
        } else if (currentOffset == 0) {
            showToast("Error al leer el archivo o archivo vacio")
        } else {
            hasMoreData = false
            showToast("Fin del archivo")
        }
    }

    private fun createProgressDialog(message: String): AlertDialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Cargando datos")
            .setMessage(message)
            .setCancelable(false)
            .create()
    }

    private fun showDataTable(fileName: String, data: CSVHandler.CSVData) {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
        val endRow = currentOffset + data.rows.size
        val typeLabel = if (isExcel) "EXCEL" else "CSV"

        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(12, 12, 12, 12)
        }

        val infoText = TextView(requireContext()).apply {
            text = "[$typeLabel] $fileName\n${data.headers.size} columnas | Filas ${formatter.format(currentOffset + 1)} - ${formatter.format(endRow)}"
            textSize = 13f; setTextColor(0xFF1565C0.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 0, 0, 8)
        }
        mainLayout.addView(infoText)

        val navLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, 8)
        }

        if (currentOffset > 0) {
            navLayout.addView(createNavButton("<< Anterior") {
                currentOffset = maxOf(0, currentOffset - BATCH_SIZE); reloadData()
            })
        }

        if (hasMoreData) {
            navLayout.addView(createNavButton("Siguiente >>") {
                currentOffset += BATCH_SIZE; reloadData()
            })
        }

        if (navLayout.childCount > 0) mainLayout.addView(navLayout)

        val horizontalScroll = HorizontalScrollView(requireContext())
        val verticalScroll = ScrollView(requireContext())
        val tableLayout = TableLayout(requireContext()).apply { isShrinkAllColumns = false }

        val headerRow = TableRow(requireContext()).apply { setBackgroundColor(0xFF1565C0.toInt()) }
        for (header in data.headers) {
            val tv = createCell(header, 12f, 0xFFFFFFFF.toInt(), true)
            tv.setOnLongClickListener { copyToClipboard(header); showToast("Copiado"); true }
            headerRow.addView(tv)
        }
        tableLayout.addView(headerRow)

        for ((index, row) in data.rows.withIndex()) {
            val tableRow = TableRow(requireContext()).apply {
                if (index % 2 == 0) setBackgroundColor(0xFFF5F9FF.toInt())
                else setBackgroundColor(0xFFFFFFFF.toInt())
            }
            for (cell in row) {
                val tv = createCell(cell, 11f, 0xFF1A237E.toInt(), false)
                tv.setOnLongClickListener { copyToClipboard(cell); showToast("Copiado"); true }
                tableRow.addView(tv)
            }
            tableLayout.addView(tableRow)
        }

        verticalScroll.addView(tableLayout)
        horizontalScroll.addView(verticalScroll)
        mainLayout.addView(horizontalScroll)

        AlertDialog.Builder(requireContext())
            .setTitle("Datos")
            .setView(mainLayout)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun reloadData() {
        currentUri?.let { if (isExcel) loadExcel(it) else loadCSV(it) }
    }

    private fun createNavButton(text: String, onClick: () -> Unit): Button {
        return Button(requireContext()).apply {
            this.text = text
            setBackgroundColor(0xFF1565C0.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 11f
            setOnClickListener { onClick() }
        }
    }

    private fun createCell(text: String, size: Float, color: Int, bold: Boolean): TextView {
        return TextView(requireContext()).apply {
            this.text = text; textSize = size; setTextColor(color)
            setPadding(14, 6, 14, 6); maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            minWidth = 70; maxWidth = 350
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Data", text))
    }

    private fun getBatchData(): CSVHandler.CSVData? {
        return if (isExcel) {
            excelHandler.readExcelBatch(currentUri!!, 0, BATCH_SIZE)?.let {
                CSVHandler.CSVData(it.headers, it.rows)
            }
        } else {
            csvHandler.readCSVBatch(currentUri!!, 0, BATCH_SIZE)
        }
    }

    private fun showFilterDialog() {
        val columns = headers.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por columna")
            .setItems(columns) { _, which -> showOperatorDialog(which) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun showOperatorDialog(colIndex: Int) {
        val operators = DataFilter.FilterOperator.values()
        val labels = operators.map { it.label }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Operador para: ${headers[colIndex]}")
            .setItems(labels) { _, which ->
                val op = operators[which]
                if (op == DataFilter.FilterOperator.IS_EMPTY || op == DataFilter.FilterOperator.IS_NOT_EMPTY) {
                    applyFilter(colIndex, op, "")
                } else {
                    showValueInput(colIndex, op)
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun showValueInput(colIndex: Int, operator: DataFilter.FilterOperator) {
        val data = getBatchData() ?: return
        val uniqueValues = DataFilter().getUniqueValues(data, colIndex).take(30)

        if (uniqueValues.isNotEmpty() && uniqueValues.size <= 30) {
            AlertDialog.Builder(requireContext())
                .setTitle("${headers[colIndex]} ${operator.label}")
                .setItems(uniqueValues.toTypedArray()) { _, which ->
                    applyFilter(colIndex, operator, uniqueValues[which])
                }
                .setNeutralButton("Escribir valor") { _, _ ->
                    showManualInput(colIndex, operator)
                }
                .setNegativeButton("Cancelar", null).show()
        } else {
            showManualInput(colIndex, operator)
        }
    }

    private fun showManualInput(colIndex: Int, operator: DataFilter.FilterOperator) {
        val input = EditText(requireContext()).apply {
            hint = "Ingrese valor..."
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(requireContext())
            .setTitle("${headers[colIndex]} ${operator.label}")
            .setView(input)
            .setPositiveButton("Filtrar") { _, _ ->
                applyFilter(colIndex, operator, input.text.toString())
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun applyFilter(colIndex: Int, operator: DataFilter.FilterOperator, value: String) {
        val data = getBatchData() ?: return
        val dataFilter = DataFilter()
        val condition = DataFilter.FilterCondition(colIndex, operator, value)
        val result = dataFilter.filter(data, listOf(condition))
        if (result.rows.isNotEmpty()) {
            showDataTable("Filtro: ${headers[colIndex]} ${operator.label} $value", result)
        } else {
            showToast("Sin resultados para el filtro")
        }
    }

    private fun showSearchDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Buscar..."; inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Buscar").setView(input)
            .setPositiveButton("Buscar") { _, _ ->
                val q = input.text.toString().lowercase()
                if (q.isNotBlank()) {
                    val data = getBatchData() ?: return@setPositiveButton
                    val results = data.rows.filter { row -> row.any { it.lowercase().contains(q) } }
                    if (results.isNotEmpty()) {
                        showDataTable("Busqueda: $q", CSVHandler.CSVData(data.headers, results))
                    } else showToast("Sin resultados")
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun showChartOptions() {
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccione tipo de grafico")
            .setItems(arrayOf("Grafico de Barras", "Grafico Circular")) { _, which ->
                when (which) {
                    0 -> showColumnSelector("barras")
                    1 -> showColumnSelector("circular")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showColumnSelector(chartType: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccione columna para graficar")
            .setItems(headers.toTypedArray()) { _, which ->
                prepareChart(chartType, which, headers[which])
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun prepareChart(chartType: String, colIndex: Int, colName: String) {
        val data = getBatchData() ?: return
        val chartGenerator = ChartGenerator(requireContext())

        val frequency = mutableMapOf<String, Float>()
        for (row in data.rows) {
            if (colIndex < row.size) {
                val key = row[colIndex].ifBlank { "(vacio)" }
                frequency[key] = (frequency[key] ?: 0f) + 1f
            }
        }

        val sortedFreq = frequency.toList().sortedByDescending { it.second }.take(10)
        val labels = sortedFreq.map { it.first }
        val values = sortedFreq.map { it.second }

        if (labels.isEmpty()) {
            showToast("No hay datos para graficar")
            return
        }

        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Generando grafico...")
            .setMessage("Procesando datos...")
            .setCancelable(false)
            .create()
        pd.show()

        Thread {
            val bitmap = when (chartType) {
                "barras" -> chartGenerator.generateBarChart(labels, values, colName)
                else -> chartGenerator.generatePieChart(labels, values, colName)
            }

            val file = chartGenerator.saveChart(bitmap, "grafico_${System.currentTimeMillis()}")

            requireActivity().runOnUiThread {
                pd.dismiss()
                if (file != null) {
                    showChartDialog(bitmap, file, colName)
                } else {
                    showToast("Error al generar grafico")
                }
            }
        }.start()
    }

    private fun showChartDialog(bitmap: Bitmap, file: java.io.File, title: String) {
        val imageView = ImageView(requireContext()).apply {
            setImageBitmap(bitmap)
            adjustViewBounds = true
            maxHeight = 1200
        }

        val scrollView = ScrollView(requireContext())
        scrollView.addView(imageView)

        AlertDialog.Builder(requireContext())
            .setTitle("Grafico: $title")
            .setView(scrollView)
            .setPositiveButton("Compartir") { _, _ -> shareImage(file) }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun shareImage(file: java.io.File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartir grafico"))
        } catch (e: Exception) {
            showToast("Error al compartir")
        }
    }

    private fun showStats() {
        val data = getBatchData() ?: return
        val sb = StringBuilder()
        sb.appendLine("=== ESTADISTICAS (muestra de ${data.rows.size} filas) ===")
        sb.appendLine("Columnas: ${data.headers.size}")
        sb.appendLine()
        for ((i, h) in data.headers.withIndex()) {
            val unique = data.rows.mapNotNull { if (i < it.size) it[i] else null }.toSet().size
            val empty = data.rows.count { i >= it.size || it[i].isBlank() }
            sb.appendLine("$h: $unique unicos, $empty vacios")
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Estadisticas").setMessage(sb.toString())
            .setPositiveButton("Cerrar", null).show()
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
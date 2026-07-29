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
    private var currentOffset = 0
    private var headers: List<String> = emptyList()
    private var pendingAction: String = ""
    private var hasMoreData = true
    private var isLoading = false

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && pendingAction == "csv") {
            currentUri = uri
            currentOffset = 0
            headers = emptyList()
            hasMoreData = true
            loadBatch(uri, 0)
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

    private fun loadBatch(uri: Uri, offset: Int) {
        if (isLoading) return
        isLoading = true

        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Cargando datos")
            .setMessage("Leyendo archivo...")
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
                    hasMoreData = data.rows.size >= BATCH_SIZE
                    showDataTable(getFileName(uri), data)
                } else if (offset == 0) {
                    showToast("Error al leer el archivo o archivo vacio")
                } else {
                    hasMoreData = false
                    showToast("Fin del archivo")
                }
            }
        }.start()
    }

    private fun showDataTable(fileName: String, data: CSVHandler.CSVData) {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
        val endRow = currentOffset + data.rows.size

        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
        }

        // Info
        val infoText = TextView(requireContext()).apply {
            text = "$fileName\n${data.headers.size} columnas | Filas ${formatter.format(currentOffset + 1)} - ${formatter.format(endRow)}"
            textSize = 13f
            setTextColor(0xFF1565C0.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }
        mainLayout.addView(infoText)

        // Navigation
        val navLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 8)
        }

        if (currentOffset > 0) {
            val prevBtn = Button(requireContext()).apply {
                text = "<< Anterior"
                setBackgroundColor(0xFF1565C0.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 11f
                setOnClickListener {
                    currentUri?.let { loadBatch(it, maxOf(0, currentOffset - BATCH_SIZE)) }
                }
            }
            navLayout.addView(prevBtn)
        }

        if (hasMoreData) {
            val nextBtn = Button(requireContext()).apply {
                text = "Siguiente >>"
                setBackgroundColor(0xFF1565C0.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 11f
                setOnClickListener {
                    currentUri?.let { loadBatch(it, currentOffset + BATCH_SIZE) }
                }
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
        }

        // Headers
        val headerRow = TableRow(requireContext()).apply {
            setBackgroundColor(0xFF1565C0.toInt())
        }
        for (header in data.headers) {
            val tv = createCell(header, 12f, 0xFFFFFFFF.toInt(), true)
            tv.setOnLongClickListener { copyToClipboard(header); showToast("Copiado"); true }
            headerRow.addView(tv)
        }
        tableLayout.addView(headerRow)

        // Rows
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
            setPadding(14, 6, 14, 6)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            minWidth = 70
            maxWidth = 350
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("CSV", text))
    }

    private fun showFilterDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por columna")
            .setItems(headers.toTypedArray()) { _, which -> showFilterValueDialog(which, headers[which]) }
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
                showDataTable("Filtrado por $colName", filteredData)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSearchDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Buscar..."
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Buscar")
            .setView(input)
            .setPositiveButton("Buscar") { _, _ ->
                val q = input.text.toString().lowercase()
                if (q.isNotBlank()) {
                    val data = csvHandler.readCSVBatch(currentUri!!, 0, BATCH_SIZE) ?: return@setPositiveButton
                    val results = data.rows.filter { row -> row.any { it.lowercase().contains(q) } }
                    if (results.isNotEmpty()) {
                        showDataTable("Busqueda: $q", CSVHandler.CSVData(data.headers, results))
                    } else showToast("Sin resultados")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showStats() {
        val data = csvHandler.readCSVBatch(currentUri!!, 0, BATCH_SIZE) ?: return
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
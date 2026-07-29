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
import android.widget.*
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
            currentUri = uri; currentOffset = 0; headers = emptyList(); hasMoreData = true
            when (pendingAction) {
                "csv" -> { isExcel = false; loadCSV(uri) }
                "excel" -> { isExcel = true; loadExcel(uri) }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_bigdata, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        csvHandler = CSVHandler(requireContext())
        excelHandler = ExcelHandler(requireContext())
        view.findViewById<View>(R.id.cardCSV)?.setOnClickListener { pendingAction = "csv"; openFilePicker(arrayOf("text/csv", "text/comma-separated-values", "*/*")) }
        view.findViewById<View>(R.id.cardExcel)?.setOnClickListener { pendingAction = "excel"; openFilePicker(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel", "*/*")) }
        view.findViewById<View>(R.id.cardFilter)?.setOnClickListener { if (headers.isNotEmpty()) showFilterDialog() else toast("Abra un archivo primero") }
        view.findViewById<View>(R.id.cardSearch)?.setOnClickListener { if (headers.isNotEmpty()) showSearchDialog() else toast("Abra un archivo primero") }
        view.findViewById<View>(R.id.cardChart)?.setOnClickListener { if (headers.isNotEmpty()) showChartOptions() else toast("Abra un archivo primero") }
        view.findViewById<View>(R.id.cardStats)?.setOnClickListener { if (headers.isNotEmpty()) showStats() else toast("Abra un archivo primero") }
    }

    private fun openFilePicker(mimeTypes: Array<String>) {
        try { filePickerLauncher.launch(mimeTypes) } catch (e: Exception) { toast("Error al abrir selector") }
    }

    private fun loadCSV(uri: Uri) {
        if (isLoading) return; isLoading = true
        val pd = progressDialog("Cargando CSV..."); pd.show()
        Thread {
            val data = csvHandler.readCSVBatch(uri, currentOffset, BATCH_SIZE)
            requireActivity().runOnUiThread { pd.dismiss(); isLoading = false; handleDataResult(data) }
        }.start()
    }

    private fun loadExcel(uri: Uri) {
        if (isLoading) return; isLoading = true
        val pd = progressDialog("Cargando Excel..."); pd.show()
        Thread {
            val data = excelHandler.readExcelBatch(uri, currentOffset, BATCH_SIZE)
            requireActivity().runOnUiThread { pd.dismiss(); isLoading = false; handleDataResult(data?.let { CSVHandler.CSVData(it.headers, it.rows) }) }
        }.start()
    }

    private fun handleDataResult(data: CSVHandler.CSVData?) {
        if (data != null && data.rows.isNotEmpty()) {
            headers = data.headers; hasMoreData = data.rows.size >= BATCH_SIZE
            showDataTable(getFileName(currentUri!!), data)
        } else if (currentOffset == 0) toast("Error al leer el archivo")
        else { hasMoreData = false; toast("Fin del archivo") }
    }

    private fun progressDialog(msg: String) = AlertDialog.Builder(requireContext()).setTitle("Cargando").setMessage(msg).setCancelable(false).create()

    private fun showDataTable(fileName: String, data: CSVHandler.CSVData) {
        val f = NumberFormat.getNumberInstance(Locale.getDefault())
        val typeLabel = if (isExcel) "EXCEL" else "CSV"
        val mainLayout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 12, 12, 12) }
        mainLayout.addView(TextView(requireContext()).apply {
            text = "[$typeLabel] $fileName\n${data.headers.size} cols | Filas ${f.format(currentOffset + 1)} - ${f.format(currentOffset + data.rows.size)}"
            textSize = 13f; setTextColor(0xFF1565C0.toInt()); setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 0, 0, 8)
        })

        val nav = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, 8) }
        if (currentOffset > 0) nav.addView(btn("<< Anterior") { currentOffset = maxOf(0, currentOffset - BATCH_SIZE); reload() })
        if (hasMoreData) nav.addView(btn("Siguiente >>") { currentOffset += BATCH_SIZE; reload() })
        if (nav.childCount > 0) mainLayout.addView(nav)

        val hScroll = HorizontalScrollView(requireContext())
        val vScroll = ScrollView(requireContext())
        val table = TableLayout(requireContext()).apply { isShrinkAllColumns = false }

        val hr = TableRow(requireContext()).apply { setBackgroundColor(0xFF1565C0.toInt()) }
        for (h in data.headers) {
            val tv = cell(h, 12f, 0xFFFFFFFF.toInt(), true)
            tv.setOnLongClickListener { copyToClipboard(h); toast("Copiado"); true }
            hr.addView(tv)
        }
        table.addView(hr)

        for ((i, row) in data.rows.withIndex()) {
            val tr = TableRow(requireContext()).apply { if (i % 2 == 0) setBackgroundColor(0xFFF5F9FF.toInt()) else setBackgroundColor(0xFFFFFFFF.toInt()) }
            for (c in row) {
                val tv = cell(c, 11f, 0xFF1A237E.toInt(), false)
                tv.setOnLongClickListener { copyToClipboard(c); toast("Copiado"); true }
                tr.addView(tv)
            }
            table.addView(tr)
        }

        vScroll.addView(table); hScroll.addView(vScroll); mainLayout.addView(hScroll)
        AlertDialog.Builder(requireContext()).setTitle("Datos").setView(mainLayout).setPositiveButton("Cerrar", null).show()
    }

    private fun reload() { currentUri?.let { if (isExcel) loadExcel(it) else loadCSV(it) } }

    private fun btn(text: String, onClick: () -> Unit) = Button(requireContext()).apply {
        this.text = text; setBackgroundColor(0xFF1565C0.toInt()); setTextColor(0xFFFFFFFF.toInt()); textSize = 11f; setOnClickListener { onClick() }
    }

    private fun cell(text: String, size: Float, color: Int, bold: Boolean) = TextView(requireContext()).apply {
        this.text = text; textSize = size; setTextColor(color); setPadding(14, 6, 14, 6)
        maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; minWidth = 70; maxWidth = 350
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
    }

    private fun copyToClipboard(text: String) {
        val cb = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("Data", text))
    }

    private fun batchData(): CSVHandler.CSVData? = if (isExcel) excelHandler.readExcelBatch(currentUri!!, 0, BATCH_SIZE)?.let { CSVHandler.CSVData(it.headers, it.rows) } else csvHandler.readCSVBatch(currentUri!!, 0, BATCH_SIZE)

    // FILTRO
    private fun showFilterDialog() {
        AlertDialog.Builder(requireContext()).setTitle("Filtrar por columna").setItems(headers.toTypedArray()) { _, w -> showOperatorDialog(w) }.setNegativeButton("Cancelar", null).show()
    }

    private fun showOperatorDialog(colIndex: Int) {
        val ops = DataFilter.FilterOperator.values()
        AlertDialog.Builder(requireContext()).setTitle("Operador: ${headers[colIndex]}")
            .setItems(ops.map { it.label }.toTypedArray()) { _, w ->
                val op = ops[w]
                if (op == DataFilter.FilterOperator.IS_EMPTY || op == DataFilter.FilterOperator.IS_NOT_EMPTY) applyFilter(colIndex, op, "")
                else showValueInput(colIndex, op)
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun showValueInput(colIndex: Int, op: DataFilter.FilterOperator) {
        val data = batchData() ?: return
        val vals = DataFilter().getUniqueValues(data, colIndex).take(30)
        if (vals.isNotEmpty() && vals.size <= 30) {
            AlertDialog.Builder(requireContext()).setTitle("${headers[colIndex]} ${op.label}")
                .setItems(vals.toTypedArray()) { _, w -> applyFilter(colIndex, op, vals[w]) }
                .setNeutralButton("Escribir valor") { _, _ -> showManualInput(colIndex, op) }
                .setNegativeButton("Cancelar", null).show()
        } else showManualInput(colIndex, op)
    }

    private fun showManualInput(colIndex: Int, op: DataFilter.FilterOperator) {
        val input = EditText(requireContext()).apply { hint = "Valor..."; inputType = android.text.InputType.TYPE_CLASS_TEXT }
        AlertDialog.Builder(requireContext()).setTitle("${headers[colIndex]} ${op.label}").setView(input)
            .setPositiveButton("Filtrar") { _, _ -> applyFilter(colIndex, op, input.text.toString()) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun applyFilter(colIndex: Int, op: DataFilter.FilterOperator, value: String) {
        val data = batchData() ?: return
        val result = DataFilter().filter(data, listOf(DataFilter.FilterCondition(colIndex, op, value)))
        if (result.rows.isNotEmpty()) showDataTable("Filtro: ${headers[colIndex]} ${op.label} $value", result)
        else toast("Sin resultados")
    }

    // BUSQUEDA
    private fun showSearchDialog() {
        val input = EditText(requireContext()).apply { hint = "Buscar..."; inputType = android.text.InputType.TYPE_CLASS_TEXT }
        AlertDialog.Builder(requireContext()).setTitle("Buscar").setView(input)
            .setPositiveButton("Buscar") { _, _ ->
                val q = input.text.toString().lowercase()
                if (q.isNotBlank()) {
                    val data = batchData() ?: return@setPositiveButton
                    val res = data.rows.filter { r -> r.any { it.lowercase().contains(q) } }
                    if (res.isNotEmpty()) showDataTable("Busqueda: $q", CSVHandler.CSVData(data.headers, res))
                    else toast("Sin resultados")
                }
            }.setNegativeButton("Cancelar", null).show()
    }

    // GRAFICOS
    private fun showChartOptions() {
        AlertDialog.Builder(requireContext()).setTitle("Tipo de grafico")
            .setItems(arrayOf("Grafico de Barras", "Grafico Circular")) { _, w ->
                showColumnSelector(if (w == 0) "barras" else "circular")
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun showColumnSelector(chartType: String) {
        AlertDialog.Builder(requireContext()).setTitle("Seleccione columna")
            .setItems(headers.toTypedArray()) { _, w -> prepareChart(chartType, w, headers[w]) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun prepareChart(chartType: String, colIndex: Int, colName: String) {
        val data = batchData() ?: return
        val cg = ChartGenerator(requireContext())
        val freq = mutableMapOf<String, Float>()
        for (row in data.rows) {
            if (colIndex < row.size) {
                val k = row[colIndex].ifBlank { "(vacio)" }
                freq[k] = (freq[k] ?: 0f) + 1f
            }
        }
        val sorted = freq.toList().sortedByDescending { it.second }.take(10)
        val labels = sorted.map { it.first }
        val values = sorted.map { it.second }
        if (labels.isEmpty()) { toast("No hay datos"); return }

        val pd = progressDialog("Generando grafico..."); pd.show()
        Thread {
            val bitmap = if (chartType == "barras") cg.generateBarChart(labels, values, colName)
            else cg.generatePieChart(labels, values, colName)
            val file = cg.saveChart(bitmap, "grafico_${System.currentTimeMillis()}")
            requireActivity().runOnUiThread {
                pd.dismiss()
                if (file != null) showChartDialog(bitmap, file, colName)
                else toast("Error al generar grafico")
            }
        }.start()
    }

    private fun showChartDialog(bitmap: Bitmap, file: java.io.File, title: String) {
        val iv = ImageView(requireContext()).apply { setImageBitmap(bitmap); adjustViewBounds = true; maxHeight = 1200 }
        val sv = ScrollView(requireContext()); sv.addView(iv)
        AlertDialog.Builder(requireContext()).setTitle("Grafico: $title").setView(sv)
            .setPositiveButton("Compartir") { _, _ -> shareImage(file) }
            .setNegativeButton("Cerrar", null).show()
    }

    private fun shareImage(file: java.io.File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            startActivity(Intent.createChooser(intent, "Compartir"))
        } catch (e: Exception) { toast("Error al compartir") }
    }

    // ESTADISTICAS
    private fun showStats() {
        val data = batchData() ?: return
        val sb = StringBuilder()
        sb.appendLine("=== ESTADISTICAS (muestra de ${data.rows.size} filas) ===")
        sb.appendLine("Columnas: ${data.headers.size}\n")
        for ((i, h) in data.headers.withIndex()) {
            val u = data.rows.mapNotNull { if (i < it.size) it[i] else null }.toSet().size
            val e = data.rows.count { i >= it.size || it[i].isBlank() }
            sb.appendLine("$h: $u unicos, $e vacios")
        }
        AlertDialog.Builder(requireContext()).setTitle("Estadisticas").setMessage(sb.toString()).setPositiveButton("Cerrar", null).show()
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
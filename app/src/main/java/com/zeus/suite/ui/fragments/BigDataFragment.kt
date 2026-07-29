package com.zeus.suite.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import java.io.File

class BigDataFragment : Fragment() {

    companion object {
        fun newInstance(): BigDataFragment = BigDataFragment()
    }

    private lateinit var csvHandler: CSVHandler
    private var pendingAction: String = ""

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            when (pendingAction) {
                "csv" -> openCSV(uri)
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
            showToast("Filtrar Datos - Proximamente")
        }
        view.findViewById<View>(R.id.cardSearch)?.setOnClickListener {
            showToast("Buscar - Proximamente")
        }
        view.findViewById<View>(R.id.cardChart)?.setOnClickListener {
            showToast("Graficar - Proximamente")
        }
        view.findViewById<View>(R.id.cardStats)?.setOnClickListener {
            showToast("Estadisticas - Proximamente")
        }
    }

    private fun openFilePicker() {
        try {
            filePickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv"))
        } catch (e: Exception) {
            showToast("Error al abrir selector de archivos")
        }
    }

    private fun openCSV(uri: Uri) {
        val pd = AlertDialog.Builder(requireContext())
            .setTitle("Abriendo CSV")
            .setMessage("Cargando datos...")
            .setCancelable(false)
            .create()
        pd.show()

        Thread {
            val data = csvHandler.readCSV(uri)

            requireActivity().runOnUiThread {
                pd.dismiss()

                if (data != null && data.rows.isNotEmpty()) {
                    showCSVData(getFileName(uri), data.headers, data.rows)
                } else {
                    showToast("Error al leer el archivo CSV")
                }
            }
        }.start()
    }

    private fun showCSVData(fileName: String, headers: List<String>, rows: List<List<String>>) {
        val scrollView = ScrollView(requireContext())
        val tableLayout = TableLayout(requireContext()).apply {
            isShrinkAllColumns = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Header row
        val headerRow = TableRow(requireContext()).apply {
            setBackgroundColor(0xFF1565C0.toInt())
            setPadding(8, 12, 8, 12)
        }
        for (header in headers) {
            val tv = TextView(requireContext()).apply {
                text = header
                textSize = 12f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(12, 8, 12, 8)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            headerRow.addView(tv)
        }
        tableLayout.addView(headerRow)

        // Data rows (max 100)
        val maxRows = minOf(rows.size, 100)
        for (i in 0 until maxRows) {
            val row = rows[i]
            val tableRow = TableRow(requireContext()).apply {
                setPadding(8, 4, 8, 4)
                if (i % 2 == 0) setBackgroundColor(0xFFF0F4FA.toInt())
            }
            for (cell in row) {
                val tv = TextView(requireContext()).apply {
                    text = cell
                    textSize = 11f
                    setTextColor(0xFF1A237E.toInt())
                    setPadding(12, 6, 12, 6)
                    maxLines = 2
                }
                tableRow.addView(tv)
            }
            tableLayout.addView(tableRow)
        }

        scrollView.addView(tableLayout)

        val message = if (rows.size > 100) {
            "Mostrando 100 de ${rows.size} filas"
        } else {
            "${rows.size} filas"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("CSV: $fileName")
            .setMessage("$message | ${headers.size} columnas")
            .setView(scrollView)
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
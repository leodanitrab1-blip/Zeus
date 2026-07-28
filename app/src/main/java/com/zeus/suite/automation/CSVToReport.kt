package com.zeus.suite.automation

import android.content.Context
import android.net.Uri
import com.zeus.suite.ai.ReportGenerator
import com.zeus.suite.bigdata.CSVHandler
import com.zeus.suite.utils.FileManager
import java.io.File

class CSVToReport(private val context: Context) {

    private val fileManager = FileManager(context)
    private val csvHandler = CSVHandler(context)
    private val reportGenerator = ReportGenerator()

    fun convertCSVToReport(uri: Uri): File? {
        val data = csvHandler.readCSV(uri) ?: return null
        val fileName = fileManager.getFileName(uri)
        val baseName = fileName.substringBeforeLast(".")
        val reportFileName = "${baseName}_reporte.txt"
        val outputFile = fileManager.createOutputFile(reportFileName)

        return try {
            val report = reportGenerator.generateReport(data.headers, data.rows)
            outputFile.writeText(report)
            outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            if (outputFile.exists()) {
                outputFile.delete()
            }
            null
        }
    }

    fun convertCSVToSummary(uri: Uri): File? {
        val data = csvHandler.readCSV(uri) ?: return null
        val fileName = fileManager.getFileName(uri)
        val baseName = fileName.substringBeforeLast(".")
        val summaryFileName = "${baseName}_resumen.txt"
        val outputFile = fileManager.createOutputFile(summaryFileName)

        return try {
            val sb = StringBuilder()
            sb.appendLine("=== RESUMEN DE DATOS CSV ===")
            sb.appendLine("Archivo: $fileName")
            sb.appendLine("Filas: ${data.rows.size}")
            sb.appendLine("Columnas: ${data.headers.size}")
            sb.appendLine("Columnas: ${data.headers.joinToString(", ")}")
            sb.appendLine()
            
            for (header in data.headers) {
                val columnIndex = data.headers.indexOf(header)
                val uniqueValues = mutableSetOf<String>()
                
                for (row in data.rows) {
                    if (columnIndex < row.size) {
                        uniqueValues.add(row[columnIndex])
                    }
                }
                
                sb.appendLine("$header: ${uniqueValues.size} valores unicos")
            }

            outputFile.writeText(sb.toString())
            outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            if (outputFile.exists()) {
                outputFile.delete()
            }
            null
        }
    }
}
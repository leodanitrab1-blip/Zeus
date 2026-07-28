package com.zeus.suite.automation

import android.content.Context
import android.net.Uri
import com.zeus.suite.bigdata.ExcelHandler
import com.zeus.suite.utils.FileManager
import java.io.File

class ExcelToPDF(private val context: Context) {

    private val fileManager = FileManager(context)
    private val excelHandler = ExcelHandler(context)

    fun convertExcelToPDF(uri: Uri): File? {
        val data = excelHandler.readExcel(uri) ?: return null
        val fileName = fileManager.getFileName(uri)
        val baseName = fileName.substringBeforeLast(".")
        val pdfFileName = "${baseName}_convertido.pdf"
        val outputFile = fileManager.createOutputFile(pdfFileName)

        return try {
            val sb = StringBuilder()
            sb.appendLine("%PDF-1.4")
            sb.appendLine("1 0 obj")
            sb.appendLine("<< /Type /Catalog /Pages 2 0 R >>")
            sb.appendLine("endobj")
            sb.appendLine("2 0 obj")
            sb.appendLine("<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
            sb.appendLine("endobj")
            sb.appendLine("3 0 obj")
            sb.appendLine("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792]")
            sb.appendLine("   /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>")
            sb.appendLine("endobj")

            val content = buildPDFContent(data.headers, data.rows)
            
            sb.appendLine("4 0 obj")
            sb.appendLine("<< /Length ${content.length} >>")
            sb.appendLine("stream")
            sb.appendLine(content)
            sb.appendLine("endstream")
            sb.appendLine("endobj")
            sb.appendLine("5 0 obj")
            sb.appendLine("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
            sb.appendLine("endobj")
            sb.appendLine("xref")
            sb.appendLine("0 6")
            sb.appendLine("0000000000 65535 f ")
            sb.appendLine("0000000009 00000 n ")
            sb.appendLine("0000000058 00000 n ")
            sb.appendLine("0000000115 00000 n ")
            sb.appendLine("0000000266 00000 n ")
            sb.appendLine("0000000416 00000 n ")
            sb.appendLine("trailer")
            sb.appendLine("<< /Size 6 /Root 1 0 R >>")
            sb.appendLine("startxref")
            sb.appendLine("494")
            sb.appendLine("%%EOF")

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

    private fun buildPDFContent(headers: List<String>, rows: List<List<String>>): String {
        val sb = StringBuilder()
        var yPosition = 720

        sb.appendLine("BT")
        sb.appendLine("/F1 14 Tf")
        sb.appendLine("72 $yPosition Td")
        sb.appendLine("(Datos convertidos desde Excel) Tj")
        sb.appendLine("0 -30 Td")

        sb.appendLine("/F1 10 Tf")
        sb.appendLine("(${headers.joinToString(" | ")}) Tj")
        sb.appendLine("0 -20 Td")

        val maxRows = minOf(rows.size, 50)
        for (i in 0 until maxRows) {
            val row = rows[i]
            val rowText = row.joinToString(" | ").take(80)
            sb.appendLine("($rowText) Tj")
            sb.appendLine("0 -15 Td")
        }

        if (rows.size > 50) {
            sb.appendLine("(... y ${rows.size - 50} filas mas) Tj")
        }

        sb.appendLine("ET")
        return sb.toString()
    }
}
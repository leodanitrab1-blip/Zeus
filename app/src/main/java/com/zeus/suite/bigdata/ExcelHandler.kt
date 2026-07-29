package com.zeus.suite.bigdata

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

class ExcelHandler(private val context: Context) {

    data class ExcelData(
        val sheetName: String,
        val headers: List<String>,
        val rows: List<List<String>>
    )

    fun readExcel(uri: Uri, maxRows: Int = 1000): ExcelData? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()

            // XLSX = ZIP que empieza con PK
            if (bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
                return readXlsx(bytes, maxRows)
            }

            // XLS antiguo o CSV/TSV
            return readAsText(bytes, maxRows)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun readXlsx(bytes: ByteArray, maxRows: Int): ExcelData? {
        return try {
            val zipStream = ZipInputStream(bytes.inputStream())
            var sharedStrings = listOf<String>()
            var sheetData = ""

            var entry = zipStream.nextEntry
            while (entry != null) {
                when {
                    entry.name == "xl/sharedStrings.xml" -> {
                        sharedStrings = parseSharedStrings(zipStream.readBytes())
                    }
                    entry.name.startsWith("xl/worksheets/sheet") && entry.name.endsWith(".xml") -> {
                        sheetData = String(zipStream.readBytes())
                    }
                }
                entry = zipStream.nextEntry
            }
            zipStream.close()

            if (sheetData.isBlank()) return null

            val rows = parseSheetData(sheetData, sharedStrings)
            val displayRows = rows.take(maxRows)

            val headers = if (displayRows.isNotEmpty()) displayRows[0] else emptyList()
            val dataRows = if (displayRows.size > 1) displayRows.drop(1) else emptyList()

            ExcelData("Hoja1", headers, dataRows)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val text = String(bytes)
        val strings = mutableListOf<String>()
        val regex = Regex("<t[^>]*>(.*?)</t>")
        for (match in regex.findAll(text)) {
            strings.add(
                match.groupValues[1]
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
            )
        }
        return strings
    }

    private fun parseSheetData(xml: String, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val rowRegex = Regex("<row[^>]*>(.*?)</row>")
        val cellRegex = Regex("<c[^>]*>(.*?)</c>")
        val valueRegex = Regex("<v>(.*?)</v>")
        val isStringRegex = Regex("t=\"s\"")

        for (rowMatch in rowRegex.findAll(xml)) {
            val rowData = mutableListOf<String>()
            for (cellMatch in cellRegex.findAll(rowMatch.groupValues[1])) {
                val cellXml = cellMatch.groupValues[1]
                val valueMatch = valueRegex.find(cellXml)
                val value = if (valueMatch != null) {
                    val v = valueMatch.groupValues[1]
                    if (isStringRegex.containsMatchIn(cellXml)) {
                        val index = v.toIntOrNull()
                        if (index != null && index < sharedStrings.size) sharedStrings[index] else v
                    } else {
                        v
                    }
                } else ""
                rowData.add(value)
            }
            if (rowData.isNotEmpty()) rows.add(rowData)
        }
        return rows
    }

    private fun readAsText(bytes: ByteArray, maxRows: Int): ExcelData? {
        return try {
            val text = String(bytes)
            val lines = text.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) return null

            val delimiter = if (lines[0].contains("\t")) "\t" else ","
            val headers = lines[0].split(delimiter).map { it.trim().replace("\"", "") }
            val rows = mutableListOf<List<String>>()

            val displayRows = minOf(lines.size - 1, maxRows)
            for (i in 1..displayRows) {
                val row = lines[i].split(delimiter).map { it.trim().replace("\"", "") }
                if (row.isNotEmpty()) rows.add(row)
            }

            ExcelData("Hoja1", headers, rows)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun readExcelBatch(uri: Uri, startRow: Int, batchSize: Int): ExcelData? {
        val data = readExcel(uri, startRow + batchSize) ?: return null
        val batchRows = data.rows.drop(startRow).take(batchSize)
        return ExcelData(data.sheetName, data.headers, batchRows)
    }
}
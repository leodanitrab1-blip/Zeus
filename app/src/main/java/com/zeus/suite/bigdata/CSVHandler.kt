package com.zeus.suite.bigdata

import android.content.Context
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class CSVHandler(private val context: Context) {

    private val fileManager = FileManager(context)

    data class CSVData(
        val headers: List<String>,
        val rows: List<List<String>>
    )

    fun readCSV(uri: Uri, delimiter: String = ",", maxRows: Int = 1000): CSVData? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            val firstLine = reader.readLine() ?: return CSVData(emptyList(), emptyList())
            val headers = parseLine(firstLine, delimiter)
            
            val rows = mutableListOf<List<String>>()
            var line = reader.readLine()
            var count = 0
            
            while (line != null && count < maxRows) {
                if (line.isNotBlank()) {
                    rows.add(parseLine(line, delimiter))
                    count++
                }
                line = reader.readLine()
            }
            
            reader.close()
            CSVData(headers, rows)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getRowCount(uri: Uri): Int {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return 0
            val reader = BufferedReader(InputStreamReader(inputStream))
            var count = 0
            reader.readLine() // Skip header
            while (reader.readLine() != null) count++
            reader.close()
            count
        } catch (e: Exception) {
            0
        }
    }

    fun readCSVBatch(uri: Uri, startRow: Int, batchSize: Int, delimiter: String = ","): CSVData? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            val firstLine = reader.readLine() ?: return null
            val headers = parseLine(firstLine, delimiter)
            
            var line: String?
            var currentRow = 0
            val rows = mutableListOf<List<String>>()
            
            // Saltar hasta startRow
            while (currentRow < startRow) {
                reader.readLine()
                currentRow++
            }
            
            // Leer lote
            var count = 0
            line = reader.readLine()
            while (line != null && count < batchSize) {
                if (line.isNotBlank()) {
                    rows.add(parseLine(line, delimiter))
                    count++
                }
                line = reader.readLine()
                currentRow++
            }
            
            reader.close()
            CSVData(headers, rows)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseLine(line: String, delimiter: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char.toString() == delimiter && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    fun exportToCSV(
        headers: List<String>,
        rows: List<List<String>>,
        fileName: String
    ): File? {
        return try {
            val file = fileManager.createOutputFile("$fileName.csv")
            val sb = StringBuilder()
            sb.appendLine(headers.joinToString(",") { "\"$it\"" })
            for (row in rows) {
                sb.appendLine(row.joinToString(",") { "\"$it\"" })
            }
            file.writeText(sb.toString())
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
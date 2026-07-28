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

    fun readCSV(uri: Uri, delimiter: String = ","): CSVData? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()
            reader.close()

            if (lines.isEmpty()) {
                return CSVData(emptyList(), emptyList())
            }

            val headers = parseLine(lines[0], delimiter)
            val rows = mutableListOf<List<String>>()

            for (i in 1 until lines.size) {
                if (lines[i].isNotBlank()) {
                    val row = parseLine(lines[i], delimiter)
                    rows.add(row)
                }
            }

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

    fun getRowCount(uri: Uri): Int {
        val data = readCSV(uri)
        return data?.rows?.size ?: 0
    }

    fun getColumnCount(uri: Uri): Int {
        val data = readCSV(uri)
        return data?.headers?.size ?: 0
    }

    fun getHeaders(uri: Uri): List<String> {
        val data = readCSV(uri)
        return data?.headers ?: emptyList()
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
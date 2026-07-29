package com.zeus.suite.bigdata

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

class ExcelHandler(private val context: Context) {

    data class ExcelData(
        val sheetName: String,
        val headers: List<String>,
        val rows: List<List<String>>
    )

    fun readExcel(uri: Uri, maxRows: Int = 1000): ExcelData? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()
            reader.close()

            if (lines.size < 2) return null

            val headers = lines[0].split("\t", ",").map { it.trim().replace("\"", "") }
            val rows = mutableListOf<List<String>>()
            
            val displayRows = minOf(lines.size - 1, maxRows)
            for (i in 1..displayRows) {
                val row = lines[i].split("\t", ",").map { it.trim().replace("\"", "") }
                if (row.isNotEmpty()) {
                    rows.add(row)
                }
            }

            ExcelData(
                sheetName = "Hoja1",
                headers = headers,
                rows = rows
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun readExcelBatch(uri: Uri, startRow: Int, batchSize: Int): ExcelData? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            val firstLine = reader.readLine() ?: return null
            val headers = firstLine.split("\t", ",").map { it.trim().replace("\"", "") }
            
            var currentRow = 0
            var line: String?
            val rows = mutableListOf<List<String>>()
            
            while (currentRow < startRow) {
                reader.readLine()
                currentRow++
            }
            
            var count = 0
            line = reader.readLine()
            while (line != null && count < batchSize) {
                if (line.isNotBlank()) {
                    rows.add(line.split("\t", ",").map { it.trim().replace("\"", "") })
                    count++
                }
                line = reader.readLine()
                currentRow++
            }
            
            reader.close()
            ExcelData("Hoja1", headers, rows)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
package com.zeus.suite.bigdata

import android.content.Context
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File

class ExcelHandler(private val context: Context) {

    private val fileManager = FileManager(context)
    private val csvHandler = CSVHandler(context)

    data class ExcelData(
        val sheetName: String,
        val headers: List<String>,
        val rows: List<List<String>>
    )

    fun readExcel(uri: Uri): ExcelData? {
        return try {
            val fileName = fileManager.getFileName(uri)
            val tempFile = fileManager.copyUriToTempFile(uri, fileName) ?: return null

            val content = tempFile.readText()
            val lines = content.lines().filter { it.isNotBlank() }

            if (lines.size < 2) {
                fileManager.deleteTempFile(tempFile)
                return null
            }

            val headers = lines[0].split("\t")
            val rows = mutableListOf<List<String>>()

            for (i in 1 until lines.size) {
                val row = lines[i].split("\t")
                if (row.size == headers.size) {
                    rows.add(row)
                }
            }

            fileManager.deleteTempFile(tempFile)

            ExcelData(
                sheetName = fileName.substringBeforeLast("."),
                headers = headers,
                rows = rows
            )

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getSheetNames(uri: Uri): List<String> {
        val data = readExcel(uri)
        return if (data != null) listOf(data.sheetName) else emptyList()
    }

    fun readExcelAsCSV(uri: Uri): CSVHandler.CSVData? {
        val data = readExcel(uri) ?: return null
        return CSVHandler.CSVData(data.headers, data.rows)
    }

    fun exportToExcel(
        headers: List<String>,
        rows: List<List<String>>,
        fileName: String
    ): File? {
        return try {
            val file = fileManager.createOutputFile("$fileName.xls")
            val sb = StringBuilder()

            sb.appendLine(headers.joinToString("\t"))

            for (row in rows) {
                sb.appendLine(row.joinToString("\t"))
            }

            file.writeText(sb.toString())
            file

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getRowCount(uri: Uri): Int {
        val data = readExcel(uri)
        return data?.rows?.size ?: 0
    }
}
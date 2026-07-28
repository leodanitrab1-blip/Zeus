package com.zeus.suite.automation

import android.content.Context
import com.zeus.suite.bigdata.CSVHandler
import com.zeus.suite.pdf.PDFCreator
import com.zeus.suite.utils.FileManager
import java.io.File

class BatchPDFCreator(private val context: Context) {

    private val fileManager = FileManager(context)
    private val pdfCreator = PDFCreator(context)
    private val csvHandler = CSVHandler(context)

    data class BatchResult(
        val totalFiles: Int,
        val successCount: Int,
        val failedCount: Int,
        val outputFiles: List<File>
    )

    fun createPDFsFromTemplate(
        template: String,
        data: List<Map<String, String>>,
        fileNamePattern: String
    ): BatchResult {
        val outputFiles = mutableListOf<File>()
        var successCount = 0
        var failedCount = 0

        for ((index, item) in data.withIndex()) {
            var content = template
            
            for ((key, value) in item) {
                content = content.replace("{{$key}}", value)
            }

            val fileName = fileNamePattern.replace("{{index}}", (index + 1).toString())
            
            val file = pdfCreator.createSimplePDF(
                fileName = fileName,
                title = "Documento ${index + 1}",
                content = content
            )

            if (file != null) {
                outputFiles.add(file)
                successCount++
            } else {
                failedCount++
            }
        }

        return BatchResult(
            totalFiles = data.size,
            successCount = successCount,
            failedCount = failedCount,
            outputFiles = outputFiles
        )
    }

    fun createPDFsFromCSV(
        csvUri: android.net.Uri,
        titleColumn: String,
        contentColumns: List<String>
    ): BatchResult {
        val data = csvHandler.readCSV(csvUri)
        
        if (data == null || data.rows.isEmpty()) {
            return BatchResult(0, 0, 0, emptyList())
        }

        val titleIndex = data.headers.indexOfFirst {
            it.equals(titleColumn, ignoreCase = true)
        }

        if (titleIndex == -1) {
            return BatchResult(0, 0, 0, emptyList())
        }

        val outputFiles = mutableListOf<File>()
        var successCount = 0
        var failedCount = 0

        for ((index, row) in data.rows.withIndex()) {
            val title = if (titleIndex < row.size) row[titleIndex] else "Documento ${index + 1}"
            
            val content = contentColumns.mapNotNull { colName ->
                val colIndex = data.headers.indexOfFirst {
                    it.equals(colName, ignoreCase = true)
                }
                if (colIndex != -1 && colIndex < row.size) {
                    "$colName: ${row[colIndex]}"
                } else {
                    null
                }
            }.joinToString("\n")

            val file = pdfCreator.createSimplePDF(
                fileName = "doc_${index + 1}",
                title = title,
                content = content
            )

            if (file != null) {
                outputFiles.add(file)
                successCount++
            } else {
                failedCount++
            }
        }

        return BatchResult(
            totalFiles = data.rows.size,
            successCount = successCount,
            failedCount = failedCount,
            outputFiles = outputFiles
        )
    }
}
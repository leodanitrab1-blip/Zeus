package com.zeus.suite.pdf

import android.content.Context
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PDFSplitter(private val context: Context) {

    private val fileManager = FileManager(context)

    fun splitPDF(uri: Uri, pagesPerSplit: Int): List<File> {
        val outputFiles = mutableListOf<File>()
        val fileName = fileManager.getFileName(uri)
        val tempFile = fileManager.copyUriToTempFile(uri, fileName) ?: return outputFiles

        try {
            val totalBytes = tempFile.length()
            
            if (totalBytes <= 0 || pagesPerSplit <= 0) {
                fileManager.deleteTempFile(tempFile)
                return outputFiles
            }

            val bytesPerSplit = totalBytes / pagesPerSplit
            
            if (bytesPerSplit <= 0) {
                fileManager.deleteTempFile(tempFile)
                return outputFiles
            }

            FileInputStream(tempFile).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var currentSplit = 1
                var bytesWritten: Long = 0
                var currentOutput: FileOutputStream? = null

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (currentOutput == null || bytesWritten >= bytesPerSplit) {
                        currentOutput?.close()
                        val splitFileName = "${fileName}_parte${currentSplit}.pdf"
                        val splitFile = fileManager.createOutputFile(splitFileName)
                        currentOutput = FileOutputStream(splitFile)
                        outputFiles.add(splitFile)
                        bytesWritten = 0
                        currentSplit++
                    }
                    currentOutput?.write(buffer, 0, bytesRead)
                    bytesWritten += bytesRead
                }
                currentOutput?.close()
            }

            fileManager.deleteTempFile(tempFile)
            return outputFiles

        } catch (e: Exception) {
            e.printStackTrace()
            outputFiles.forEach { fileManager.deleteTempFile(it) }
            fileManager.deleteTempFile(tempFile)
            return emptyList()
        }
    }
}
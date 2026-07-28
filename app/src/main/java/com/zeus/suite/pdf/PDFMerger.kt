package com.zeus.suite.pdf

import android.content.Context
import android.net.Uri
import com.zeus.suite.utils.FileManager
import java.io.File
import java.io.FileOutputStream

class PDFMerger(private val context: Context) {

    private val fileManager = FileManager(context)

    fun mergePDFs(uris: List<Uri>, outputFileName: String): File? {
        if (uris.size < 2) {
            return null
        }

        val tempFiles = mutableListOf<File>()
        
        for (uri in uris) {
            val fileName = fileManager.getFileName(uri)
            val tempFile = fileManager.copyUriToTempFile(uri, fileName)
            if (tempFile != null) {
                tempFiles.add(tempFile)
            }
        }

        if (tempFiles.size < 2) {
            tempFiles.forEach { fileManager.deleteTempFile(it) }
            return null
        }

        val outputFile = fileManager.createOutputFile(outputFileName)

        try {
            FileOutputStream(outputFile).use { output ->
                tempFiles.forEach { tempFile ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }

            tempFiles.forEach { fileManager.deleteTempFile(it) }
            return outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            tempFiles.forEach { fileManager.deleteTempFile(it) }
            if (outputFile.exists()) {
                outputFile.delete()
            }
            return null
        }
    }
}